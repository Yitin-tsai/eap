package com.eap.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * AI 聊天服務：
 * - 透過本地 Ollama 模型理解問題
 * - 依照模型輸出的 JSON 指示呼叫 MCP 工具
 * - 將工具結果餵回模型生成最終回答
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

     private static final String SYSTEM_PROMPT = """
          你是 EAP 電力交易助手（Planner + Executor 模式）。

          目標：能夠規劃並執行多步驟的市場模擬或實際交易操作，並以機器可執行的 JSON 格式輸出所有決策與 action 列表。

          嚴格規則（請務必遵守）：
          1) 僅輸出 JSON（可以先輸出一個 plan 欄位說明步驟，再緊接著輸出 actions 陣列）。
          2) price 與 qty 必須以字串回傳（例如 "10.0"、"100"）。
          3) 欄位名稱需精確匹配（大小寫敏感）：userId、side、price、qty、symbol。
          4) 若欄位缺失或型別不正確，回傳標準錯誤物件 {"final_answer":"錯誤: 說明"}。

          支援的工具（名稱與 arguments）：
          - placeOrder: {"userId":"string","side":"BUY|SELL","price":"string","qty":"string","symbol":"string"}
          - registerUser: {"userId":"string"}
          - checkUserExists: {"userId":"string"}
          - getUserWallet: {"userId":"string"}
          - getUserOrders: {"userId":"string"}
          - cancelOrder: {"orderId":"string"}
          - getOrderBook: {"depth": number} 或 {}
          - getMarketMetrics: {}

          Multi-step JSON schema（機器可執行）：
          {
            "mode": "simulate" | "execute",
            "plan": [{"step":1,"name":"短說明","tools":["getUserWallet","getOrderBook"]}],
            "actions": [
               {"action":"getUserWallet","arguments":{"userId":"..."}},
               {"action":"getOrderBook","arguments":{}},
               {"action":"placeOrder","arguments":{"userId":"...","side":"BUY","price":"10.0","qty":"100","symbol":"ELC"}}
            ],
            "final_answer":"簡短總結（可選）"
          }

          使用範例（模擬）：
          {
            "mode":"simulate",
            "plan":[{"step":1,"name":"查三位用戶錢包","tools":["getUserWallet"]},{"step":2,"name":"取得訂單簿","tools":["getOrderBook"]},{"step":3,"name":"計算並下單","tools":["placeOrder"]}],
            "actions":[
              {"action":"getUserWallet","arguments":{"userId":"550e8400-e29b-41d4-a716-446655440000"}},
              {"action":"getUserWallet","arguments":{"userId":"a731325b-641c-488c-ae53-64a88ad3d525"}},
              {"action":"getUserWallet","arguments":{"userId":"896fe72c-6099-405d-bb73-c76d60258f0b"}},
              {"action":"getOrderBook","arguments":{}},
              {"action":"placeOrder","arguments":{"userId":"550e8400-e29b-41d4-a716-446655440000","side":"BUY","price":"100","qty":"100","symbol":"ELC"}}
            ],
            "final_answer":"模擬完成，請確認是否執行。"
          }

          錯誤示例（不可）：
          {"action":"placeOrder","arguments":{"userId":"...","side":"BUY","price":10.0,"quantity":100}}

          注意：
          - 當 mode="simulate" 時，僅模擬，不會實際呼叫下單 API；當 mode="execute" 時，請在呼叫前確認（可由 human 確認或 safe-mode 參數）。
          - 若需要一次下多張訂單，請將多個 placeOrder 放入 actions[]，並按執行順序排列。
          - 若無法遵守契約，回傳 {"final_answer":"錯誤: 欄位或格式錯誤說明"}。
          """;

    private final OllamaChatModel chatModel;
    private final McpToolClient mcpToolClient;
    private final ObjectMapper objectMapper;
    // 儲存上一次模型產生的計畫（簡單記憶，單一實例）
    private volatile JsonNode lastPlan = null;

    /**
     * 處理用戶聊天請求，必要時呼叫 MCP 工具。
     */
    public String chat(String userMessage) {
        try {
            log.info("收到用戶訊息: {}", userMessage);

            // 如果使用者只下達「執行」之類的短命令，且我們有上一次的計畫，則直接執行該計畫
            boolean isExecuteCmd = userMessage != null && (userMessage.contains("執行") || userMessage.toLowerCase().contains("execute"));
            if (isExecuteCmd && lastPlan != null) {
                log.info("偵測到執行命令，使用上次計畫進行執行");
                JsonNode rootJson = lastPlan.deepCopy();
                // 強制 mode=execute
                if (rootJson.isObject()) {
                    ((ObjectNode) rootJson).put("mode", "execute");
                }
                return executeActionsAndFollowup(rootJson, userMessage);
            }

            String initialPrompt = SYSTEM_PROMPT + "\n\n使用者提問：" + userMessage;
            log.info("[AI] prompt size={} chars", initialPrompt.length());
            StringBuilder buf = new StringBuilder();
            long t0 = System.currentTimeMillis();

            chatModel.stream(initialPrompt)
                .doOnSubscribe(s -> log.info("[AI] start streaming initial response..."))
                .doOnNext(chunk -> {
                    // Spring AI Ollama stream may emit String chunks; handle as-is
                    String text = chunk == null ? "" : chunk;
                    buf.append(text);
                    log.debug("[AI][delta] {}", text);
                })
                .doOnError(e -> log.error("[AI] stream error (initial)", e))
                .doOnComplete(() -> log.info("[AI] initial stream complete, total={} chars", buf.length()))
                .blockLast();

            String modelResponse = buf.toString().trim();
            long t1 = System.currentTimeMillis();
            log.debug("模型初步回應(assembled) ({} ms): {}", (t1 - t0), modelResponse);

            // 嘗試從模型回應中抽出一或多個 JSON 物件
            List<JsonNode> extracted = extractJsonObjects(modelResponse);
            JsonNode rootJson = null;
            if (!extracted.isEmpty()) {
                if (extracted.size() == 1) {
                    rootJson = extracted.get(0);
                } else {
                    // 如果模型回傳多個獨立 JSON 片段，嘗試把它們組成 actions 陣列
                    ArrayNode actionsArr = objectMapper.createArrayNode();
                    for (JsonNode n : extracted) {
                        // 若每個片段本身就是一個 action 物件，直接加入；否則包成 action
                        if (n.has("action")) {
                            actionsArr.add(n);
                        } else {
                            ObjectNode wrapper = objectMapper.createObjectNode();
                            wrapper.set("arguments", n);
                            wrapper.put("action", "unknown");
                            actionsArr.add(wrapper);
                        }
                    }
                    ObjectNode wrapperRoot = objectMapper.createObjectNode();
                    wrapperRoot.put("mode", "simulate");
                    wrapperRoot.set("actions", actionsArr);
                    rootJson = wrapperRoot;
                }
            }

            if (rootJson == null) {
                return extractFinalAnswer(modelResponse, null);
            }

            // 如果模型輸出包含 actions[]，先記錄成 lastPlan
            if (rootJson.has("actions")) {
                lastPlan = rootJson.deepCopy();
            }

            // 若 mode=execute，立即執行 actions
            String mode = rootJson.path("mode").asText("simulate");
            if ("execute".equalsIgnoreCase(mode)) {
                return executeActionsAndFollowup(rootJson, userMessage);
            }

            // 否則回傳模型給出的模擬結果
            return extractFinalAnswer(modelResponse, null);

        } catch (Exception e) {
            log.error("處理聊天請求失敗", e);
            return "抱歉，處理您的請求時發生錯誤：" + e.getMessage();
        }
    }

    /**
     * 取得系統狀態摘要。
     */
    public String getSystemStatus() {
        boolean modelHealthy = isModelAvailable();
        boolean mcpHealthy = mcpToolClient.isHealthy();

        return String.format("""
            🤖 EAP AI 助手狀態

            🧠 模型: %s
            🕸️ MCP 服務: %s

            我可以協助：
            • 註冊用戶與查詢錢包
            • 送出/取消訂單、查詢撮合
            • 取得市場指標與訂單簿

            請輸入指令開始互動。
            """,
            modelHealthy ? "Llama (本地) ✅" : "模型不可用 ❌",
            mcpHealthy ? "連線正常" : "無法連線");
    }

    /**
     * 檢查 Ollama 模型是否可用。
     */
    public boolean isModelAvailable() {
        try {
            // use a short streaming check to detect availability without waiting for full sync call
            StringBuilder buf = new StringBuilder();
            chatModel.stream("ping")
                .doOnNext(chunk -> {
                    String text = chunk == null ? "" : chunk;
                    buf.append(text);
                })
                .blockLast();

            String testResponse = buf.toString();
            return testResponse != null && !testResponse.trim().isEmpty();
        } catch (Exception e) {
            log.warn("AI 模型不可用", e);
            return false;
        }
    }

    private Optional<ToolInvocation> parseToolInvocation(String modelOutput) {
        try {
            JsonNode root = objectMapper.readTree(modelOutput);
            JsonNode actionNode = root.path("action");
            if (actionNode.isMissingNode() || actionNode.asText().isBlank()) {
                return Optional.empty();
            }

            JsonNode arguments = root.path("arguments");
            if (!arguments.isObject()) {
                arguments = objectMapper.createObjectNode();
            }

            return Optional.of(new ToolInvocation(actionNode.asText(), (ObjectNode) arguments));

        } catch (JsonProcessingException e) {
            log.debug("模型輸出不是 JSON，視為最終回答: {}", modelOutput);
            return Optional.empty();
        }
    }

    private String extractFinalAnswer(String modelOutput, JsonNode toolResultFallback) {
        try {
            JsonNode root = objectMapper.readTree(modelOutput);
            JsonNode finalAnswer = root.path("final_answer");
            if (!finalAnswer.isMissingNode()) {
                return finalAnswer.asText();
            }
        } catch (JsonProcessingException e) {
            log.debug("最終輸出非 JSON，直接回傳原文");
        }

        if (toolResultFallback != null && !toolResultFallback.isEmpty()) {
            return "工具回傳結果：\n" + toolResultFallback.toPrettyString();
        }

        return modelOutput;
    }

    private record ToolInvocation(String action, ObjectNode arguments) {
    }

    // 從一段文字中抽出所有 JSON 物件（包括被 code fence 包裹的 JSON）
    private List<JsonNode> extractJsonObjects(String text) {
        List<JsonNode> result = new ArrayList<>();
        if (text == null || text.isBlank()) return result;

        // 1) 嘗試找到所有 ```...``` code fence 中的內容
        int idx = 0;
        while (true) {
            int start = text.indexOf("```", idx);
            if (start == -1) break;
            int end = text.indexOf("```", start + 3);
            if (end == -1) break;
            String inner = text.substring(start + 3, end).trim();
            try {
                JsonNode node = objectMapper.readTree(inner);
                result.add(node);
            } catch (Exception ignored) {
            }
            idx = end + 3;
        }

        // 2) 嘗試直接 parse 可能的 JSON 片段（包括多個獨立的 JSON 物件）
        // 我們簡單用正則或 search for '{'..'}' 匹配，這裡用 naive 方法：嘗試從每個 '{' 開始解析到能 parse
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != '{') continue;
            for (int j = i + 1; j <= text.length(); j++) {
                if (text.charAt(j - 1) != '}') continue;
                String cand = text.substring(i, j);
                try {
                    JsonNode n = objectMapper.readTree(cand);
                    result.add(n);
                    i = j - 1; // advance
                    break;
                } catch (Exception ex) {
                    // ignore, keep extending
                }
            }
        }

        return result;
    }

    // 執行 actions[] 並把結果回傳與餵給模型 follow-up
    private String executeActionsAndFollowup(JsonNode rootJson, String userMessage) {
        ArrayNode actions = (ArrayNode) rootJson.path("actions");
        ArrayNode results = objectMapper.createArrayNode();

        for (JsonNode act : actions) {
            String actionName = act.path("action").asText(null);
            JsonNode args = act.path("arguments");
            if (actionName == null || actionName.isBlank()) {
                ObjectNode err = objectMapper.createObjectNode();
                err.put("error", "missing action name");
                results.add(err);
                continue;
            }

            try {
                ObjectNode argsObj = args.isObject() ? (ObjectNode) args : objectMapper.createObjectNode();
                JsonNode toolRes = mcpToolClient.callTool(actionName, argsObj);
                ObjectNode ok = objectMapper.createObjectNode();
                ok.put("action", actionName);
                ok.set("result", toolRes == null ? objectMapper.nullNode() : toolRes);
                ok.put("status", "ok");
                results.add(ok);
                log.info("工具 {} 呼叫完成", actionName);
            } catch (Exception ex) {
                ObjectNode err = objectMapper.createObjectNode();
                err.put("action", actionName);
                err.put("status", "error");
                err.put("message", ex.getMessage());
                results.add(err);
                log.error("呼叫工具 {} 時發生錯誤", actionName, ex);
            }
        }

        // 把執行結果餵回模型，讓模型產生 final_answer
        String followUpPrompt = SYSTEM_PROMPT + "\n\n使用者提問：" + userMessage +
            "\n工具 actions[] 執行結果：\n" + results.toPrettyString() +
            "\n請根據結果產出最終回答，僅以 {\"final_answer\":\"...\"} JSON 格式回覆。";

        StringBuilder assembled = new StringBuilder();
        chatModel.stream(followUpPrompt)
            .doOnSubscribe(s -> log.info("[AI] start streaming follow-up response..."))
            .doOnNext(chunk -> {
                String text = chunk == null ? "" : chunk;
                assembled.append(text);
                log.debug("[AI][delta][followup] {}", text);
            })
            .doOnError(e -> log.error("[AI] stream error (followup)", e))
            .doOnComplete(() -> log.info("[AI] follow-up stream complete, total={} chars", assembled.length()))
            .blockLast();

        String finalResp = assembled.toString().trim();
        return extractFinalAnswer(finalResp, results);
    }
}
