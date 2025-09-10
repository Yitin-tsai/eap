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
 * MCP 交易工具
 * 使用 Spring AI MCP 註解
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TradingMcpTool {

    private final OrderServiceClient orderServiceClient;

    @Tool(name = "placeOrder", description = "下單交易，支持買入和賣出訂單")
    public Map<String, Object> placeOrder(
        @ToolParam(description = "用戶ID，必須是有效的UUID格式", required = true) String userId,
        @ToolParam(description = "訂單方向：BUY 或 SELL", required = true) String side,
        @ToolParam(description = "訂單價格", required = true) String price,
        @ToolParam(description = "訂單數量", required = true) String qty,
        @ToolParam(description = "交易標的代碼", required = false) String symbol
    ) {
        try {
            // 設置默認值
            if (symbol == null || symbol.isEmpty()) {
                symbol = "ELC";
            }
            
            // 驗證參數
            if (!side.equals("BUY") && !side.equals("SELL")) {
                return Map.of(
                    "success", false,
                    "error", "side 參數必須是 'BUY' 或 'SELL'"
                );
            }

            log.info("下單請求: userId={}, side={}, price={}, qty={}, symbol={}", 
                    userId, side, price, qty, symbol);

            Map<String, Object> orderRequest = Map.of(
                "userId", userId,
                "side", side,
                "price", price,
                "qty", qty,
                "symbol", symbol
            );

            ResponseEntity<Map<String, Object>> response = orderServiceClient.placeOrder(orderRequest);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Map.of(
                    "success", true,
                    "data", response.getBody(),
                    "timestamp", System.currentTimeMillis(),
                    "operation", "placeOrder"
                );
            } else {
                return Map.of(
                    "success", false,
                    "error", "訂單執行失敗",
                    "statusCode", response.getStatusCode().value()
                );
            }

        } catch (Exception e) {
            log.error("訂單執行失敗", e);
            return Map.of(
                "success", false,
                "error", "訂單執行失敗: " + e.getMessage()
            );
        }
    }

    @Tool(name = "getUserOrders", description = "查詢用戶的所有交易訂單")
    public Map<String, Object> getUserOrders(
        @ToolParam(description = "用戶ID，必須是有效的UUID格式", required = true) String userId
    ) {
        try {
            log.info("獲取用戶訂單，用戶: {}", userId);
            
            ResponseEntity<Map<String, Object>> response = orderServiceClient.getUserOrders(userId, null);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Map.of(
                    "success", true,
                    "data", response.getBody(),
                    "timestamp", System.currentTimeMillis(),
                    "userId", userId
                );
            } else {
                return Map.of(
                    "success", false,
                    "error", "無法獲取用戶訂單",
                    "statusCode", response.getStatusCode().value()
                );
            }
            
        } catch (Exception e) {
            log.error("獲取用戶訂單失敗", e);
            return Map.of(
                "success", false,
                "error", "獲取用戶訂單失敗: " + e.getMessage()
            );
        }
    }
}
