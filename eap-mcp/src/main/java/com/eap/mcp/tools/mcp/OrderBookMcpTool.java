package com.eap.mcp.tools.mcp;

import com.eap.mcp.client.OrderServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MCP 訂單簿工具
 * 使用 Spring AI @Tool 註解
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderBookMcpTool {

    private final OrderServiceClient orderServiceClient;

    @Tool(name = "getOrderBook", description = "獲取電力交易訂單簿數據，包含買賣盤資訊")
    public Map<String, Object> getOrderBook(
        @ToolParam(description = "訂單簿深度，預設為10層", required = false) Integer depth
    ) {
        try {
            // 設置默認值
            if (depth == null) {
                depth = 10;
            }
            
            log.info("獲取訂單簿，深度: {}", depth);
            
            ResponseEntity<Map<String, Object>> response = orderServiceClient.getOrderBook(depth);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Map.of(
                    "success", true,
                    "data", response.getBody(),
                    "timestamp", System.currentTimeMillis()
                );
            } else {
                return Map.of(
                    "success", false,
                    "error", "無法獲取訂單簿數據",
                    "statusCode", response.getStatusCode().value()
                );
            }
            
        } catch (Exception e) {
            log.error("獲取訂單簿失敗", e);
            return Map.of(
                "success", false,
                "error", "獲取訂單簿失敗: " + e.getMessage()
            );
        }
    }
}