package com.eap.mcp.tools;

import com.eap.mcp.client.OrderServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 取消訂單工具
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CancelOrderTool {

    private final OrderServiceClient orderServiceClient;

    public Object execute(Map<String, Object> parameters) {
        try {
            if (!parameters.containsKey("orderId") || parameters.get("orderId") == null) {
                return Map.of(
                    "code", "INVALID_PARAMETER",
                    "message", "Missing required parameter: orderId"
                );
            }

            String orderId = parameters.get("orderId").toString();

            log.info("Cancelling order: orderId={}", orderId);

            ResponseEntity<Map<String, Object>> response = orderServiceClient.cancelOrder(orderId);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                return Map.of(
                    "code", "ERROR",
                    "message", "Failed to cancel order",
                    "details", Map.of("status", response.getStatusCode())
                );
            }
        } catch (Exception e) {
            log.error("Error cancelling order", e);
            return Map.of(
                "code", "INTERNAL_ERROR",
                "message", "Internal server error: " + e.getMessage()
            );
        }
    }

    public String getName() {
        return "cancelOrder";
    }

    public String getDescription() {
        return "取消指定的訂單";
    }

    public Map<String, Object> getSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "orderId", Map.of(
                    "type", "string",
                    "description", "要取消的訂單ID（必需）"
                )
            ),
            "required", new String[]{"orderId"}
        );
    }
}
