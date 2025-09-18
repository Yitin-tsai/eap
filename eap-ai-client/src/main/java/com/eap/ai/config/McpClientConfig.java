package com.eap.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 建立使用 SSE 傳輸的 MCP 同步客戶端。
 */
@Configuration
public class McpClientConfig {

    @Bean(destroyMethod = "close")
    public McpSyncClient mcpSyncClient(
        @Value("${eap.mcp.base-url:http://localhost:8083}") String baseUrl,
        @Value("${eap.mcp.base-path:/mcp}") String basePath,
        @Value("${eap.mcp.sse-path:/sse}") String ssePath,
        @Value("${eap.mcp.timeout-seconds:30}") long timeoutSeconds,
        ObjectMapper objectMapper
    ) {
        Duration timeout = Duration.ofSeconds(timeoutSeconds);

        String finalBaseUri = trimTrailingSlash(baseUrl);
        String sseEndpoint = joinPaths(basePath, ssePath);

        HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(finalBaseUri)
            .sseEndpoint(sseEndpoint)
            .objectMapper(objectMapper)
            .connectTimeout(timeout)
            .build();

        return McpClient.sync(transport)
            .requestTimeout(timeout)
            .initializationTimeout(timeout)
            .clientInfo(new McpSchema.Implementation("EAP AI Client", "0.1.0"))
            .build();
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String joinPaths(String basePath, String subPath) {
        String base = normalizePath(basePath);
        String sub = normalizePath(subPath);
        if ("/".equals(base)) {
            return sub;
        }
        if ("/".equals(sub)) {
            return base;
        }
        return base + sub;
    }

}
