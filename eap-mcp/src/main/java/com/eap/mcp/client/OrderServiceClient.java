package com.eap.mcp.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Order Service Client for MCP
 * 調用 order-service 的 MCP API 端點
 */
@FeignClient(name = "order-service", url = "${eap.order-service.base-url}")
public interface OrderServiceClient {

    /**
     * 健康檢查
     */
    @GetMapping("/eap-order/mcp/v1/health")
    ResponseEntity<Map<String, Object>> health();

    /**
     * 統一下單
     */
    @PostMapping("/eap-order/mcp/v1/orders")
    ResponseEntity<Map<String, Object>> placeOrder(@RequestBody Map<String, Object> orderRequest);

    /**
     * 取消訂單
     */
    @DeleteMapping("/eap-order/mcp/v1/orders/{orderId}")
    ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable String orderId);

    /**
     * 查詢用戶訂單
     */
    @GetMapping("/eap-order/mcp/v1/orders")
    ResponseEntity<Map<String, Object>> getUserOrders(
            @RequestParam String userId,
            @RequestParam(required = false) String status);

    /**
     * 獲取訂單簿
     */
    @GetMapping("/eap-order/mcp/v1/orderbook")
    ResponseEntity<Map<String, Object>> getOrderBook(@RequestParam(defaultValue = "10") int depth);

    /**
     * 獲取市場指標
     */
    @GetMapping("/eap-order/mcp/v1/metrics")
    ResponseEntity<Map<String, Object>> getMarketMetrics(@RequestParam(defaultValue = "10") int depth);
}
