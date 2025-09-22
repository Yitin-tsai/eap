package com.eap.ai.config;

import com.eap.ai.config.properties.EapMcpProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpClientConfig {

    private final EapMcpProperties props;

    public McpClientConfig(EapMcpProperties props) {
        this.props = props;
    }

    @Bean(destroyMethod = "close")
    public McpSyncClient mcpSyncClient(ObjectMapper objectMapper) {
        String sseUrl = props.getSseUrl();
        String msgUrl = props.getMessageUrl();

        System.out.println("=== MCP Client Configuration ===");
        System.out.println("SSE URL: " + sseUrl);
        System.out.println("Message URL: " + msgUrl);
        System.out.println("================================");

        var transport = HttpClientSseClientTransport.builder(props.getBaseUrl())
            .sseEndpoint(props.getSsePath())
            .connectTimeout(props.getTimeoutDuration())
            .build();

        return McpClient.sync(transport)
            .requestTimeout(props.getTimeoutDuration())
            .initializationTimeout(props.getTimeoutDuration())
            .clientInfo(new McpSchema.Implementation("EAP AI Client", "0.1.0"))
            .build();
    }
}
