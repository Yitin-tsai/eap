package com.eap.mcp.config;

import com.eap.mcp.tools.mcp.MarketMetricsMcpTool;
import com.eap.mcp.tools.mcp.OrderBookMcpTool;
import com.eap.mcp.tools.mcp.TradingMcpTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP 工具配置類
 * 將 @Tool 註解的工具類註冊為 ToolCallbackProvider
 */
@Configuration
public class McpToolConfig {

    @Bean
    public ToolCallbackProvider mcpTools(
            MarketMetricsMcpTool metrics,
            OrderBookMcpTool orderBook,
            TradingMcpTool trading) {

        return MethodToolCallbackProvider
                .builder()
                .toolObjects(metrics, orderBook, trading)
                .build();
    }
}
