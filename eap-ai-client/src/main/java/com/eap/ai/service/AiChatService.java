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

    private static final Set<String> READ_ONLY = Set.of(
        "getOrderBook", "getMarketMetrics", "getUserWallet", "getUserOrders", "checkUserExists", "exportReport");
    private static final Set<String> STATE_CHANGING = Set.of(
        "placeOrder", "cancelOrder", "registerUser", "runSimulation");

    /** 主提示：允許 JSON 規劃或純文字工具執行結果 */
    private static final String SYSTEM_PROMPT = """
            你是 EAP 電力交易平台的「工具執行規劃器」。首選輸出格式為可執行的 JSON 物件；但為了實務彈性，系統也接受直接以純文字（或簡短表格/清單）回傳工具執行結果。當你能產出結構化規劃時，請仍優先輸出 JSON（見下方格式）；若情境更適合直接回傳工具結果（例如查詢後的表格或文字說明），也可直接輸出純文字結果。

            【首選 - 結構化輸出格式 (機器可直接執行)】
            {
                "actions": [ { "action": "<toolName>", "arguments": {…} }, ... ],
                "final_answer": ""
            }

            【放寬規則 — 若回傳 JSON，請遵守下列要點】
            - 若輸出 JSON，請僅輸出單一 JSON 物件，勿夾帶額外文字或 ``` 標記。
            - 欄位大小寫固定：actions, action, arguments, final_answer。
            - 參數 price、qty 建議以**字串**回傳；嚴禁千分位（"7000" ✓ / "7,000" ✗）。
            - side 建議為 "BUY" 或 "SELL"（大寫）。
            - 若缺少必要參數：
                - 查詢類（唯讀）可使用安全預設值直接執行；
                - 會改變狀態的工具請直接提供完整參數，否則回傳錯誤。
            - 若無法產出合法規劃，輸出：{"final_answer":"錯誤: 規劃無效或缺參數"}

            【次選 - 純文字工具結果】
            - 當你直接回傳工具執行結果（非 JSON 規劃）時，請以簡潔清楚的文字、表格或列點呈現，並在可能處提供對應的工具名稱與主要參數，例：
              "getOrderBook(depth=20) -> 表格: ..." 或
              "getMarketMetrics -> price=7000, spread=5"
            - 系統會嘗試從文字中擷取足夠資訊以供紀錄與顯示，但不會自動將自然語言轉為下單指令。

          【可用工具與參數】
          - getOrderBook -> arguments: {} | {"depth": number}
          - getMarketMetrics -> arguments: {}
          - getUserWallet -> {"userId":"string"}
          - getUserOrders -> {"userId":"string"}
          - placeOrder -> {"userId":"string","side":"BUY|SELL","price":"string","qty":"string","symbol":"string"}
          - cancelOrder -> {"orderId":"string"}
          - registerUser -> {}
          - runSimulation -> {"strategy":"string","symbol":"string","steps":number,
              "userId":"string","threshold":number,"qty":number,"priceStrategy":"topBid|mid|topAsk",
              "sides":"BUY|SELL|BOTH","ordersPerStep":number}
          - exportReport -> {"id":"string"}  // returns most recent SimulationResult when id omitted

            【語義對應建議】
            - 「訂單簿/買賣單/order book/五檔/十檔/深度」→ getOrderBook（若文本含「前N檔」，則 depth=N）
            - 「市場/市況/行情/指標/metrics」→ getMarketMetrics
            - 「下單/成交/取消」→ placeOrder/cancelOrder

            【最小範例 (JSON 規劃)】
            {"actions":[{"action":"getOrderBook","arguments":{"depth":20}}],"final_answer":""}
            """;

    // ===== 公開入口：單輪規劃 → 執行 → 回傳結果 =====
    public String chat(String userMessage) {
        try {
            log.info("收到用戶訊息: {}", userMessage);

            String prompt = SYSTEM_PROMPT + "\n使用者提問：" + userMessage;
            String modelOut = chatClient.prompt(prompt).call().content();
            log.info("模型回應: {}", modelOut);
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
    private record Plan(List<ObjectNode> actions) {
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
            JsonNode arr = root.path("actions");
            if (!arr.isArray() || arr.size() == 0)
                return null;
            List<ObjectNode> acts = new ArrayList<>();
            for (JsonNode n : arr)
                if (n.isObject())
                    acts.add((ObjectNode) n);
            return new Plan(acts);
        } catch (Exception ignore) {
            return null;
        }
    }

    // ===== 執行器（唯讀直接執行；狀態改變直接執行但需驗證必要參數） =====
    private ObjectNode executePlan(Plan plan) {
        ObjectNode results = objectMapper.createObjectNode();

        for (ObjectNode a : plan.actions()) {
            String name = a.path("action").asText();
            ObjectNode args = a.has("arguments") && a.get("arguments").isObject()
                    ? (ObjectNode) a.get("arguments")
                    : objectMapper.createObjectNode();

            normalizeArgs(args);

            boolean isReadOnly = READ_ONLY.contains(name);
            boolean isStateChanging = STATE_CHANGING.contains(name);

            // basic required-parameter validation
            String missing = validateRequiredParams(name, args);
            if (missing != null) {
                ObjectNode err = objectMapper.createObjectNode();
                err.put("error", "missing parameter: " + missing);
                results.set(name, err);
                continue;
            }

            if (isReadOnly || isStateChanging) {
                log.info("Calling MCP tool {} with args {}", name, args.toString());
                results.set(name, safeCall(name, args));
                continue;
            }

            // 不在白名單，直接標示
            results.put(name, "ERROR: unsupported tool");
        }
        return results;
    }

    private String validateRequiredParams(String name, ObjectNode args) {
        if ("placeOrder".equals(name)) {
            if (!args.hasNonNull("userId"))
                return "userId";
            if (!args.hasNonNull("side"))
                return "side";
            if (!args.hasNonNull("price"))
                return "price";
            if (!args.hasNonNull("qty"))
                return "qty";
            if (!args.hasNonNull("symbol"))
                return "symbol";
        }
        if ("cancelOrder".equals(name)) {
            if (!args.hasNonNull("orderId"))
                return "orderId";
        }
        if ("getUserWallet".equals(name) || "getUserOrders".equals(name)) {
            if (!args.hasNonNull("userId"))
                return "userId";
        }
        if ("runSimulation".equals(name)) {
            // always require userId for simulations and verify existence via checkUserExists
            if (!args.hasNonNull("userId") || args.get("userId").asText().isBlank())
                return "userId";

            try {
                ObjectNode check = objectMapper.createObjectNode();
                check.put("userId", args.get("userId").asText());
                JsonNode res = mcpToolClient.callTool("checkUserExists", check);

                boolean exists = false;
                if (res != null) {
                    if (res.isBoolean())
                        exists = res.asBoolean();
                    else if (res.isObject() && res.has("exists"))
                        exists = res.path("exists").asBoolean();
                }

                if (!exists) {
                    // attempt to auto-register a user
                    try {
                        log.info("userId {} not found — attempting to register new user", args.get("userId").asText());
                        ObjectNode empty = objectMapper.createObjectNode();
                        JsonNode reg = mcpToolClient.callTool("registerUser", empty);
                        if (reg != null && reg.isObject() && reg.path("success").asBoolean(false)) {
                            String newId = reg.path("userId").asText(null);
                            if (newId != null && !newId.isBlank()) {
                                args.put("userId", newId);
                                log.info("auto-registered userId={}", newId);
                                // success — proceed
                            } else {
                                return "userId (registration succeeded but missing id)";
                            }
                        } else {
                            String msg = reg != null && reg.has("message") ? reg.path("message").asText() : "registration failed";
                            return "userId (not found and registration failed: " + msg + ")";
                        }
                    } catch (Exception rex) {
                        log.error("auto-register failed", rex);
                        return "userId (not found and registration attempt failed: " + rex.getMessage() + ")";
                    }
                }
            } catch (Exception e) {
                log.warn("checkUserExists failed for {}: {}", args.get("userId").asText(), e.getMessage());
                // don't block on transient check failures
            }
        }
        // registerUser requires no params
        return null;
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
