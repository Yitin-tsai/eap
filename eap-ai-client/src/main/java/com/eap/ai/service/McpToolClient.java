package com.eap.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 負責與 eap-mcp 的 REST 介面互動。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class McpToolClient {

    private final McpSyncClient mcpSyncClient;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * 列出 MCP 可用工具。
     */
    public JsonNode listTools() {
        try {
            ensureInitialized();
            McpSchema.ListToolsResult result = mcpSyncClient.listTools();
            if (result == null || result.tools() == null) {
                return objectMapper.createArrayNode();
            }
            return objectMapper.valueToTree(result.tools());
        } catch (Exception e) {
            log.error("取得 MCP 工具列表失敗", e);
            return objectMapper.createObjectNode();
        }
    }

    /**
     * 呼叫指定 MCP 工具。
     */
    public JsonNode callTool(String toolName, JsonNode arguments) {
        try {
            ensureInitialized();
            Map<String, Object> argumentMap = (arguments == null || arguments.isNull())
                ? Collections.emptyMap()
                : objectMapper.convertValue(arguments, new TypeReference<Map<String, Object>>() {});

            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(toolName, argumentMap);
            McpSchema.CallToolResult result = mcpSyncClient.callTool(request);
            if (result == null) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.valueToTree(result);
        } catch (Exception e) {
            log.error("呼叫 MCP 工具 {} 失敗", toolName, e);
            return objectMapper.createObjectNode();
        }
    }

    /**
     * 檢查 MCP 健康狀態。
     */
    public boolean isHealthy() {
        try {
            ensureInitialized();
            mcpSyncClient.ping();
            return true;
        } catch (Exception e) {
            log.warn("MCP 健康檢查失敗: {}", e.getMessage());
            return false;
        }
    }

    private void ensureInitialized() {
        if (initialized.get()) {
            return;
        }
        synchronized (initialized) {
            if (initialized.get()) {
                return;
            }
            try {
                McpSchema.InitializeResult result = mcpSyncClient.initialize();
                log.info("MCP 初始化完成，伺服器版本: {}", result != null ? result.serverInfo().version() : "unknown");
                initialized.set(true);
            } catch (Exception e) {
                log.error("MCP 初始化失敗", e);
                throw new IllegalStateException("MCP initialization failed", e);
            }
        }
    }
}
