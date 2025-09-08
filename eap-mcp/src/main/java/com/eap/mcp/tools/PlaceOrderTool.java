package com.eap.mcp.tools;

import com.eap.mcp.client.OrderServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 下單工具
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlaceOrderTool {

    private final OrderServiceClient orderServiceClient;

    public Object execute(Map<String, Object> parameters) {
        try {
            // 驗證必需參數
            String[] requiredParams = {"userId", "side", "price", "qty"};
            for (String param : requiredParams) {
                if (!parameters.containsKey(param) || parameters.get(param) == null) {
                    return Map.of(
                        "code", "INVALID_PARAMETER",
                        "message", "Missing required parameter: " + param
                    );
                }
            }

            String userId = parameters.get("userId").toString();
            String side = parameters.get("side").toString();
            String price = parameters.get("price").toString();
            String qty = parameters.get("qty").toString();

            log.info("Placing order: userId={}, side={}, price={}, qty={}", 
                    userId, side, price, qty);

            // 驗證 side 參數
            if (!side.equals("BUY") && !side.equals("SELL")) {
                return Map.of(
                    "code", "INVALID_PARAMETER",
                    "message", "side must be either 'BUY' or 'SELL'"
                );
            }

            ResponseEntity<Map<String, Object>> response = orderServiceClient.placeOrder(parameters);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                return Map.of(
                    "code", "ERROR",
                    "message", "Failed to place order",
                    "details", Map.of("status", response.getStatusCode())
                );
            }
        } catch (Exception e) {
            log.error("Error placing order", e);
            return Map.of(
                "code", "INTERNAL_ERROR",
                "message", "Internal server error: " + e.getMessage()
            );
        }
    }

    public String getName() {
        return "placeOrder";
    }

    public String getDescription() {
        return "下單交易，支持買入和賣出訂單";
    }

    public Map<String, Object> getSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "userId", Map.of(
                    "type", "string",
                    "description", "用戶ID（必需）"
                ),
                "side", Map.of(
                    "type", "string",
                    "description", "訂單方向（必需）",
                    "enum", new String[]{"BUY", "SELL"}
                ),
                "price", Map.of(
                    "type", "string",
                    "description", "價格（必需）",
                    "pattern", "^\\d+(\\.\\d+)?$"
                ),
                "qty", Map.of(
                    "type", "string",
                    "description", "數量（必需）",
                    "pattern", "^\\d+(\\.\\d+)?$"
                ),
                "symbol", Map.of(
                    "type", "string",
                    "description", "交易標的代碼（可選，預設為ELC）",
                    "default", "ELC"
                )
            ),
            "required", new String[]{"userId", "side", "price", "qty"}
        );
    }
}
