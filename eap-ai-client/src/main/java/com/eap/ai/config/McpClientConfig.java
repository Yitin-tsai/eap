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

@Configuration  
public class McpClientConfig {

    @Value("${eap.mcp.base-url:http://localhost:8083}")
    private String baseUrl;
    
    @Value("${eap.mcp.sse-path:/mcp/sse}")
    private String ssePath;
    
    @Value("${eap.mcp.message-path:/mcp/message}")
    private String messagePath;
    
    @Value("${eap.mcp.timeout-seconds:60}")
    private int timeoutSeconds;

    @Bean(destroyMethod = "close")
    public McpSyncClient mcpSyncClient(ObjectMapper objectMapper) {
        String sseUrl = baseUrl + ssePath;
        String msgUrl = baseUrl + messagePath;
        
        System.out.println("=== MCP Client Configuration ===");
        System.out.println("SSE URL: " + sseUrl);
        System.out.println("Message URL: " + msgUrl);
        System.out.println("================================");

        var transport = HttpClientSseClientTransport.builder(baseUrl)
            .sseEndpoint(ssePath)
            .connectTimeout(Duration.ofSeconds(timeoutSeconds))
            .build();
            
        return McpClient.sync(transport)
            .requestTimeout(Duration.ofSeconds(timeoutSeconds))
            .initializationTimeout(Duration.ofSeconds(timeoutSeconds))
            .clientInfo(new McpSchema.Implementation("EAP AI Client", "0.1.0"))
            .build();
    }
}
