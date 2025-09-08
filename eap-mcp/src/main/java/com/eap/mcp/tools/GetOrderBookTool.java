package com.eap.mcp.tools;

import com.eap.mcp.client.OrderServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 獲取訂單簿工具
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetOrderBookTool {

    private final OrderServiceClient orderServiceClient;
    private final ObjectMapper objectMapper;

    public Object execute(Map<String, Object> parameters) {
        try {
            String symbol = (String) parameters.getOrDefault("symbol", "ELC");
            Integer depth = (Integer) parameters.getOrDefault("depth", 10);

            log.info("Getting order book for symbol: {}, depth: {}", symbol, depth);

            ResponseEntity<Map<String, Object>> response = orderServiceClient.getOrderBook(depth);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                return Map.of(
                    "code", "ERROR",
                    "message", "Failed to get order book",
                    "details", Map.of("status", response.getStatusCode())
                );
            }
        } catch (Exception e) {
            log.error("Error getting order book", e);
            return Map.of(
                "code", "INTERNAL_ERROR",
                "message", "Internal server error: " + e.getMessage()
            );
        }
    }

    public String getName() {
        return "getOrderBook";
    }

    public String getDescription() {
        return "獲取訂單簿數據，包含買賣盤資訊";
    }

    public Map<String, Object> getSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "symbol", Map.of(
                    "type", "string",
                    "description", "交易標的代碼，預設為ELC",
                    "default", "ELC"
                ),
                "depth", Map.of(
                    "type", "integer",
                    "description", "訂單簿深度，預設為10層",
                    "default", 10,
                    "minimum", 1,
                    "maximum", 50
                )
            )
        );
    }
}
