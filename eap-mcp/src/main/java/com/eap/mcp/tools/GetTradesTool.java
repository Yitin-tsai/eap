package com.eap.mcp.tools;

import com.eap.mcp.client.OrderServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 獲取用戶訂單工具
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetTradesTool {

    private final OrderServiceClient orderServiceClient;
    private final ObjectMapper objectMapper;

    public Object execute(Map<String, Object> parameters) {
        try {
            String userId = (String) parameters.get("userId");
            String status = (String) parameters.get("status");
            Integer limit = (Integer) parameters.getOrDefault("limit", 50);

            log.info("Getting user orders for userId: {}, status: {}, limit: {}", 
                    userId, status, limit);

            if (userId == null) {
                return Map.of(
                    "code", "INVALID_PARAMETER",
                    "message", "userId is required"
                );
            }

            // 使用 order-service 的 MCP API 獲取用戶訂單
            ResponseEntity<Map<String, Object>> response = orderServiceClient.getUserOrders(userId, status);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> result = response.getBody();
                result.put("limit", limit);
                return result;
            } else {
                return Map.of(
                    "code", "ERROR",
                    "message", "Failed to get user orders",
                    "details", Map.of("status", response.getStatusCode())
                );
            }
        } catch (Exception e) {
            log.error("Error getting user orders", e);
            return Map.of(
                "code", "INTERNAL_ERROR",
                "message", "Internal server error: " + e.getMessage()
            );
        }
    }

    public String getName() {
        return "getUserOrders";
    }

    public String getDescription() {
        return "獲取用戶訂單記錄，可指定狀態過濾條件";
    }

    public Map<String, Object> getSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "userId", Map.of(
                    "type", "string",
                    "description", "用戶ID（必需）"
                ),
                "status", Map.of(
                    "type", "string",
                    "description", "訂單狀態過濾（可選）",
                    "enum", new String[]{"PENDING", "PARTIAL", "FILLED", "CANCELLED"}
                ),
                "limit", Map.of(
                    "type", "integer",
                    "description", "返回記錄數量限制，預設50",
                    "default", 50,
                    "minimum", 1,
                    "maximum", 1000
                )
            ),
            "required", new String[]{"userId"}
        );
    }
}
