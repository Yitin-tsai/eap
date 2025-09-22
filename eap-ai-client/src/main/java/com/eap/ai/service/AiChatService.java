package com.eap.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.stereotype.Service;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiChatService {

    private final ChatClient chatClient;
    private final McpToolClient mcpToolClient;
    private final ObjectMapper objectMapper;

    // 僅用於執行階段分類（最小必要硬編碼）
    private static final Set<String> READ_ONLY = Set.of(
            "getOrderBook", "getMarketMetrics", "getUserWallet", "getUserOrders", "checkUserExists");
    private static final Set<String> STATE_CHANGING = Set.of(
            "placeOrder", "cancelOrder", "registerUser");

    /** 單一主提示（請依需要微調文案即可，邏輯不在程式碼里） */
    private static final String SYSTEM_PROMPT = """
            你是 EAP 電力交易平台的「工具執行規劃器」。請輸出**一個且只有一個**可執行 JSON 物件（Content-Type: application/json），不得出現任何多餘文字或 ``` 標記；系統將直接解析並執行。

            【固定輸出格式】
            {
              "mode": "execute" | "simulate",
              "actions": [ { "action": "<toolName>", "arguments": {…} }, ... ],
              "final_answer": ""
            }

            【嚴格規則】
            - 只能輸出上述**單一** JSON 物件，不能有解釋或 Markdown。
            - 欄位大小寫固定：mode, actions, action, arguments, final_answer。
            - 參數 price、qty 一律以**字串**回傳；嚴禁千分位（"7000" ✓ / "7,000" ✗）。
            - side 只能是 "BUY" 或 "SELL"（大寫）。
            - 若缺少必要參數：
              - 查詢類（唯讀）可使用安全預設值直接執行；
              - 會改變狀態的工具請改以 "mode":"simulate" 輸出，避免實際執行。
            - 若無法產出合法規劃，輸出：{"final_answer":"錯誤: 規劃無效或缺參數"}

            【可用工具與參數】
            - getOrderBook -> arguments: {} | {"depth": number}
            - getMarketMetrics -> arguments: {}
            - getUserWallet -> {"userId":"string"}
            - getUserOrders -> {"userId":"string"}
            - placeOrder -> {"userId":"string","side":"BUY|SELL","price":"string","qty":"string","symbol":"string"}
            - cancelOrder -> {"orderId":"string"}
            - registerUser -> {"userId":"string"}

            【語義對應建議】
            - 「訂單簿/買賣單/order book/五檔/十檔/深度」→ getOrderBook（若文本含「前N檔」，則 depth=N）
            - 「市場/市況/行情/指標/metrics」→ getMarketMetrics
            - 「模擬/下單/成交」→ placeOrder/cancelOrder；若未明確允許執行，請以 simulate 模式輸出

            【最小範例】
            {"mode":"execute","actions":[{"action":"getOrderBook","arguments":{"depth":20}}],"final_answer":""}
            """;

    // ===== 公開入口：單輪規劃 → 執行 → 回傳結果 =====
    public String chat(String userMessage) {
        try {
            log.info("收到用戶訊息: {}", userMessage);

            String prompt = SYSTEM_PROMPT + "\n使用者提問：" + userMessage;
            String modelOut = chatClient.prompt(prompt).call().content();

            Plan plan = parsePlanStrict(modelOut);
            if (plan == null || plan.actions().isEmpty()) {
                // 若模型違規，直接把原文回去，便於你觀察調 prompt
                return modelOut == null ? "錯誤：未取得模型回應" : modelOut;
            }

            ObjectNode execRes = executePlan(plan);
            return execRes.toPrettyString();

        } catch (Exception e) {
            log.error("處理失敗", e);
            return "處理請求時發生錯誤：" + e.getMessage();
        }
    }

    // ===== 規劃解析（容錯：直 parse → ```json 區塊 → 大括號配對）=====
    private record Plan(String mode, List<ObjectNode> actions) {
    }

    private Plan parsePlanStrict(String text) {
        if (text == null || text.isBlank())
            return null;

        Plan p = tryParseAsPlan(text);
        if (p != null)
            return p;

        var fence = Pattern.compile("```(?:json)?\\s*(\\{[\\s\\S]*?\\})\\s*```",
                Pattern.CASE_INSENSITIVE);
        var m1 = fence.matcher(text);
        if (m1.find()) {
            p = tryParseAsPlan(m1.group(1));
            if (p != null)
                return p;
        }

        int s = text.indexOf('{');
        while (s >= 0) {
            int depth = 0;
            for (int i = s; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '{')
                    depth++;
                else if (c == '}' && --depth == 0) {
                    String cand = text.substring(s, i + 1);
                    p = tryParseAsPlan(cand);
                    if (p != null)
                        return p;
                    break;
                }
            }
            s = text.indexOf('{', s + 1);
        }
        return null;
    }

    private Plan tryParseAsPlan(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String mode = root.path("mode").asText("execute");
            JsonNode arr = root.path("actions");
            if (!arr.isArray() || arr.size() == 0)
                return null;
            List<ObjectNode> acts = new ArrayList<>();
            for (JsonNode n : arr)
                if (n.isObject())
                    acts.add((ObjectNode) n);
            return new Plan(mode, acts);
        } catch (Exception ignore) {
            return null;
        }
    }

    // ===== 執行器（唯讀直接執行；狀態改變在 simulate 模式時跳過）=====
    private ObjectNode executePlan(Plan plan) {
        ObjectNode results = objectMapper.createObjectNode();
        boolean simulate = "simulate".equalsIgnoreCase(plan.mode());

        for (ObjectNode a : plan.actions()) {
            String name = a.path("action").asText();
            ObjectNode args = a.has("arguments") && a.get("arguments").isObject()
                    ? (ObjectNode) a.get("arguments")
                    : objectMapper.createObjectNode();

            normalizeArgs(args);

            boolean isReadOnly = READ_ONLY.contains(name);
            boolean isStateChanging = STATE_CHANGING.contains(name);

            if (isReadOnly) {
                results.set(name, safeCall(name, args));
                continue;
            }

            if (isStateChanging) {
                if (simulate) {
                    results.put(name, "SKIPPED: simulate 模式不執行狀態變更");
                } else {
                    results.set(name, safeCall(name, args));
                }
                continue;
            }

            // 不在白名單，直接標示
            results.put(name, "ERROR: unsupported tool");
        }
        return results;
    }

    private JsonNode safeCall(String tool, ObjectNode args) {
        try {
            JsonNode res = mcpToolClient.callTool(tool, args);
            return res == null ? objectMapper.nullNode() : res;
        } catch (Exception e) {
            ObjectNode err = objectMapper.createObjectNode();
            err.put("error", e.getMessage());
            return err;
        }
    }

    // ===== 參數正規化：移除千分位、全部字串化、枚舉/代碼大寫 =====
    private void normalizeArgs(ObjectNode args) {
        if (args == null)
            return;

        if (args.has("price")) {
            args.put("price", args.get("price").asText().replace(",", "").trim());
        }
        if (args.has("qty")) {
            args.put("qty", args.get("qty").asText().replace(",", "").trim());
        }
        if (args.has("userId") && !args.get("userId").isTextual()) {
            args.put("userId", args.get("userId").asText());
        }
        if (args.has("side") && args.get("side").isTextual()) {
            String s = args.get("side").asText();
            if ("buy".equalsIgnoreCase(s))
                args.put("side", "BUY");
            if ("sell".equalsIgnoreCase(s))
                args.put("side", "SELL");
        }
        if (args.has("symbol") && args.get("symbol").isTextual()) {
            args.put("symbol", args.get("symbol").asText().toUpperCase());
        }
    }
}
