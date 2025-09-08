package com.eap.mcp.tools;

import com.eap.mcp.client.OrderServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 獲取市場指標工具
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetMetricsTool {

    private final OrderServiceClient orderServiceClient;
    private final ObjectMapper objectMapper;

    public Object execute(Map<String, Object> parameters) {
        try {
            String symbol = (String) parameters.getOrDefault("symbol", "ELC");
            String window = (String) parameters.getOrDefault("window", "1h");
            Integer depthN = (Integer) parameters.getOrDefault("depthN", 5);

            log.info("Getting metrics for symbol: {}, window: {}, depthN: {}", 
                    symbol, window, depthN);

            // 使用 order-service 的 MCP API 獲取市場指標
            ResponseEntity<Map<String, Object>> metricsResponse = orderServiceClient.getMarketMetrics(depthN);
            
            if (metricsResponse.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> result = metricsResponse.getBody();
                
                // 添加請求的參數信息
                result.put("requestedSymbol", symbol);
                result.put("requestedWindow", window);
                result.put("requestedDepth", depthN);
                
                return result;
            } else {
                return Map.of(
                    "code", "ERROR",
                    "message", "Failed to get metrics data",
                    "details", Map.of("status", metricsResponse.getStatusCode())
                );
            }
        } catch (Exception e) {
            log.error("Error getting metrics", e);
            return Map.of(
                "code", "INTERNAL_ERROR",
                "message", "Internal server error: " + e.getMessage()
            );
        }
    }

    public String getName() {
        return "metrics";
    }

    public String getDescription() {
        return "獲取市場關鍵指標，包含價差、成交量、波動率等";
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
                "window", Map.of(
                    "type", "string",
                    "description", "時間窗口（如1h, 1d, 1w）",
                    "default", "1h",
                    "enum", new String[]{"5m", "15m", "1h", "4h", "1d", "1w"}
                ),
                "depthN", Map.of(
                    "type", "integer",
                    "description", "訂單簿深度分析層數，預設5層",
                    "default", 5,
                    "minimum", 1,
                    "maximum", 20
                )
            )
        );
    }
}
