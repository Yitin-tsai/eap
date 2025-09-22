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

    /**
     * 處理用戶聊天請求，必要時呼叫 MCP 工具。
     */
    public String chat(String userMessage) {
        try {
            log.info("收到用戶訊息: {}", userMessage);

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

            Optional<ToolInvocation> maybeTool = parseToolInvocation(modelResponse);
            if (maybeTool.isEmpty()) {
                return extractFinalAnswer(modelResponse, null);
            }

            ToolInvocation invocation = maybeTool.get();
            JsonNode toolResult = mcpToolClient.callTool(invocation.action(), invocation.arguments());
            log.info("工具 {} 呼叫完成", invocation.action());

            String followUpPrompt = SYSTEM_PROMPT + "\n\n使用者提問：" + userMessage +
                "\n工具 " + invocation.action() + " 回傳的 JSON 結果如下：\n" + toolResult.toPrettyString() +
                "\n請根據結果產出最終回答，僅以 {\"final_answer\":\"...\"} JSON 格式回覆。";

            // stream follow-up
            StringBuilder buf2 = new StringBuilder();
            long t2 = System.currentTimeMillis();
            chatModel.stream(followUpPrompt)
                .doOnSubscribe(s -> log.info("[AI] start streaming follow-up response..."))
                .doOnNext(chunk -> {
                    String text = chunk == null ? "" : chunk;
                    buf2.append(text);
                    log.debug("[AI][delta][followup] {}", text);
                })
                .doOnError(e -> log.error("[AI] stream error (followup)", e))
                .doOnComplete(() -> log.info("[AI] follow-up stream complete, total={} chars", buf2.length()))
                .blockLast();

            String finalResponse = buf2.toString().trim();
            long t3 = System.currentTimeMillis();
            log.debug("模型最終回應(assembled) ({} ms): {}", (t3 - t2), finalResponse);

            return extractFinalAnswer(finalResponse, toolResult);

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
}
