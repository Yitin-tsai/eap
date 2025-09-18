package com.eap.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

/**
 * 啟動時檢查本地模型與 MCP 服務狀態。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class McpConnectionService implements CommandLineRunner {

    private final OllamaChatModel chatModel;
    private final McpToolClient mcpToolClient;

    @Override
    public void run(String... args) {
        log.info("=== EAP AI Client 啟動檢查 ===");
        checkOllamaConnection();
        checkMcpConnection();
        log.info("=== 啟動檢查完成 ===");
    }

    private void checkOllamaConnection() {
        try {
            log.info("正在檢查 Ollama 連接...");
            String response = chatModel.call("Hello");

            if (response != null && !response.trim().isEmpty()) {
                log.info("✅ Ollama 連線成功 (回應長度 {} 字符)", response.length());
            } else {
                log.warn("⚠️ Ollama 回應為空，請確認模型是否載入完成");
            }

        } catch (Exception e) {
            log.error("❌ Ollama 連線失敗: {}", e.getMessage());
            log.warn("請確認 Ollama 服務已啟動並已載入模型 (ollama serve + ollama pull llama3.1)");
        }
    }

    private void checkMcpConnection() {
        try {
            log.info("正在檢查 MCP 工具列表...");
            JsonNode toolArray = mcpToolClient.listTools();

            if (toolArray.isArray() && toolArray.size() > 0) {
                log.info("✅ MCP 連線成功，取得 {} 個工具", toolArray.size());
                toolArray.forEach(tool -> log.info("  • {} - {}",
                    tool.path("name").asText(),
                    tool.path("description").asText("")));
            } else {
                log.warn("⚠️ MCP 服務可達，但工具列表為空");
            }

        } catch (Exception e) {
            log.error("❌ MCP 連線失敗: {}", e.getMessage());
            log.warn("請確認 eap-mcp 服務運行於 http://localhost:8083");
        }
    }
}
