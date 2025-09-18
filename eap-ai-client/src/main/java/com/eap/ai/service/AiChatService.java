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
        你是 EAP 電力交易助手。
        
        重要：當用戶詢問你的功能時，請直接回答，不需要調用任何工具！
        
        你的主要功能：
        • 協助註冊新用戶和查詢用戶錢包
        • 協助下單、取消訂單、查詢用戶訂單  
        • 獲取市場數據：訂單簿、市場指標
        • 檢查用戶是否存在
        
        只有在用戶要求執行具體操作時才使用工具：
        • registerUser - 註冊新用戶（參數：{"userId": "字串"}）
        • checkUserExists - 檢查用戶是否存在（參數：{"userId": "字串"}）
        • getUserWallet - 查詢用戶錢包（參數：{"userId": "字串"}）
        • placeOrder - 下訂單（參數：{"userId":"字串","side":"BUY/SELL","price":"數字","quantity":"數字"}）
        • getUserOrders - 查詢用戶訂單（參數：{"userId": "字串"}）
        • cancelOrder - 取消訂單（參數：{"orderId": "字串"}）
        • getOrderBook - 獲取訂單簿（參數：{"depth": 數字} 或 {}）
        • getMarketMetrics - 獲取市場指標（參數：{}）
        
        回應格式：
        • 需要調用工具時：{"action":"工具名稱","arguments":{...}}
        • 最終回答時：{"final_answer":"你的回答"}
        • 只能輸出有效 JSON，不可有其他文字
        • 工具名稱必須完全符合上述清單
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
            String modelResponse = chatModel.call(initialPrompt).trim();
            log.debug("模型初步回應: {}", modelResponse);

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

            String finalResponse = chatModel.call(followUpPrompt).trim();
            log.debug("模型最終回應: {}", finalResponse);

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
            String testResponse = chatModel.call("ping");
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
