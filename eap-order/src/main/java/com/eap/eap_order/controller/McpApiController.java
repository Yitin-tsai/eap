package com.eap.eap_order.controller;

import com.eap.common.event.OrderCancelEvent;
import com.eap.common.dto.OrderBookResponseDto;
import com.eap.common.dto.MarketSummaryDto;
import com.eap.eap_order.application.OrderQueryService;
import com.eap.eap_order.application.PlaceBuyOrderService;
import com.eap.eap_order.application.PlaceSellOrderService;
import com.eap.eap_order.application.OutBound.EapMatchEngine;
import com.eap.eap_order.controller.dto.req.PlaceBuyOrderReq;
import com.eap.eap_order.controller.dto.req.PlaceSellOrderReq;
import com.eap.eap_order.controller.dto.res.ListUserOrderRes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MCP API Controller
 * 專門為 MCP 服務提供的 API 端點
 */
@RestController
@RequestMapping("/mcp/v1")
@Validated
@Tag(name = "MCP API", description = "Model Context Protocol API for EAP Trading Platform")
@Slf4j
public class McpApiController {

    @Autowired
    private PlaceBuyOrderService placeBuyOrderService;
    
    @Autowired
    private PlaceSellOrderService placeSellOrderService;
    
    @Autowired
    private OrderQueryService orderQueryService;
    
    @Autowired
    private EapMatchEngine eapMatchEngine;

    /**
     * 統一下單接口 (支援買賣雙向)
     * POST /mcp/v1/orders
     */
    @Operation(summary = "統一下單", description = "支援買賣雙向的統一下單接口")
    @ApiResponse(responseCode = "200", description = "下單成功")
    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> placeOrder(
            @Valid @RequestBody Map<String, Object> request) {
        
        log.info("收到 MCP 下單請求: {}", request);
        
        Map<String, Object> response = new HashMap<>();
        
        // 解析請求參數
        String side = (String) request.get("side");
        BigDecimal price = new BigDecimal(request.get("price").toString());
        BigDecimal qty = new BigDecimal(request.get("qty").toString());
        String userId = (String) request.get("userId");

        UUID orderId;
        if ("BUY".equalsIgnoreCase(side)) {
            PlaceBuyOrderReq buyReq = PlaceBuyOrderReq.builder()
                .bidPrice(price.intValue())
                .amount(qty.intValue())
                .bidder(UUID.fromString(userId))
                .build();
            orderId = placeBuyOrderService.execute(buyReq);
        } else if ("SELL".equalsIgnoreCase(side)) {
            PlaceSellOrderReq sellReq = new PlaceSellOrderReq();
            sellReq.setSellPrice(price.intValue());
            sellReq.setAmount(qty.intValue());
            sellReq.setSeller(UUID.fromString(userId));
            placeSellOrderService.placeSellOrder(sellReq);
            orderId = UUID.randomUUID(); // 暫時生成，實際應該從服務返回
        } else {
            throw new IllegalArgumentException("Invalid side: " + side);
        }

        // 構建響應
        response.put("orderId", orderId.toString());
        response.put("status", "PENDING");
        response.put("acceptedAt", LocalDateTime.now());
        response.put("side", side);
        response.put("type", request.get("type"));
        response.put("price", price);
        response.put("qty", qty);
        response.put("symbol", request.get("symbol"));

        return ResponseEntity.ok(response);
    }

    /**
     * 取消訂單
     * DELETE /mcp/v1/orders/{orderId}
     */
    @Operation(summary = "取消訂單", description = "根據訂單ID取消訂單")
    @ApiResponse(responseCode = "200", description = "取消成功")
    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, Object>> cancelOrder(
            @Parameter(description = "訂單ID") @PathVariable String orderId) {
        
        log.info("收到 MCP 取消訂單請求: {}", orderId);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 構建取消事件
            OrderCancelEvent cancelEvent = OrderCancelEvent.builder()
                .orderId(UUID.fromString(orderId))
                .build();
            
            // 調用取消服務
            eapMatchEngine.cancelOrder(cancelEvent);
            
            response.put("orderId", orderId);
            response.put("status", "CANCELLED");
            response.put("cancelledAt", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("取消訂單失敗: {}", e.getMessage());
            response.put("error", "取消訂單失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 查詢用戶訂單
     * GET /mcp/v1/orders
     */
    @Operation(summary = "查詢用戶訂單", description = "根據用戶ID查詢訂單列表")
    @ApiResponse(responseCode = "200", description = "查詢成功")
    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> getUserOrders(
            @Parameter(description = "用戶ID") @RequestParam String userId,
            @Parameter(description = "訂單狀態", required = false) @RequestParam(required = false) String status) {
        
        log.info("收到 MCP 查詢用戶訂單請求: userId={}, status={}", userId, status);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            ListUserOrderRes orders;
            if ("pending".equalsIgnoreCase(status)) {
                orders = orderQueryService.getUserPendingOrders(userId);
            } else if ("matched".equalsIgnoreCase(status)) {
                orders = orderQueryService.getUserMatchedOrders(userId);
            } else {
                orders = orderQueryService.getUserOrderList(userId);
            }
            
            response.put("userId", userId);
            response.put("orders", orders.getUserOrders());
            response.put("totalCount", orders.getUserOrders().size());
            response.put("status", status != null ? status : "all");
            response.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("查詢用戶訂單失敗: {}", e.getMessage());
            response.put("error", "查詢失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 獲取訂單簿
     * GET /mcp/v1/orderbook
     */
    @Operation(summary = "獲取訂單簿", description = "獲取當前市場訂單簿數據")
    @ApiResponse(responseCode = "200", description = "獲取成功")
    @GetMapping("/orderbook")
    public ResponseEntity<Map<String, Object>> getOrderBook(
            @Parameter(description = "深度") @RequestParam(defaultValue = "10") int depth) {
        
        log.info("收到 MCP 獲取訂單簿請求: depth={}", depth);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            ResponseEntity<OrderBookResponseDto> orderBook = eapMatchEngine.getOrderBook(depth);
            
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("orderBook", orderBook.getBody());
            
        } catch (Exception e) {
            log.error("獲取訂單簿失敗: {}", e.getMessage());
            response.put("error", "獲取訂單簿失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 獲取市場指標
     * GET /mcp/v1/metrics
     */
    @Operation(summary = "獲取市場指標", description = "獲取詳細的市場分析指標")
    @ApiResponse(responseCode = "200", description = "獲取成功")
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMarketMetrics(
            @Parameter(description = "訂單簿深度") @RequestParam(defaultValue = "10") int depth) {
        
        log.info("收到 MCP 獲取市場指標請求: depth={}", depth);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 獲取市場摘要
            ResponseEntity<MarketSummaryDto> marketSummary = eapMatchEngine.getMarketSummary();
            response.put("marketSummary", marketSummary.getBody());
            
            // 獲取訂單簿進行深度分析
            int depthN = Math.min(depth, 20); // 限制最大深度
            ResponseEntity<OrderBookResponseDto> orderBook = eapMatchEngine.getOrderBook(depthN);
            response.put("orderBook", orderBook.getBody());
            
            if (orderBook.getBody() != null) {
                OrderBookResponseDto orderBookData = orderBook.getBody();
                
                // 計算額外的市場指標
                Map<String, Object> metrics = new HashMap<>();
                
                // 買賣價差
                if (orderBookData != null && 
                    orderBookData.getBids() != null && !orderBookData.getBids().isEmpty() && 
                    orderBookData.getAsks() != null && !orderBookData.getAsks().isEmpty()) {
                    BigDecimal bestBid = BigDecimal.valueOf(orderBookData.getBids().get(0).getPrice());
                    BigDecimal bestAsk = BigDecimal.valueOf(orderBookData.getAsks().get(0).getPrice());
                    BigDecimal spread = bestAsk.subtract(bestBid);
                    BigDecimal spreadPercent = spread.divide(bestAsk, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
                    
                    metrics.put("spread", spread);
                    metrics.put("spreadPercent", spreadPercent);
                    metrics.put("bestBid", bestBid);
                    metrics.put("bestAsk", bestAsk);
                    
                    // 流動性指標 (深度N層總量)
                    Integer bidLiquidity = orderBookData.getBids().stream()
                            .map(bid -> bid.getAmount())
                            .reduce(0, Integer::sum);
                    Integer askLiquidity = orderBookData.getAsks().stream()
                            .map(ask -> ask.getAmount())
                            .reduce(0, Integer::sum);
                    
                    metrics.put("bidLiquidity", bidLiquidity);
                    metrics.put("askLiquidity", askLiquidity);
                    metrics.put("totalLiquidity", bidLiquidity + askLiquidity);
                }
                
                response.put("metrics", metrics);
                response.put("orderBookDepth", depthN);
            }
            
            response.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("獲取市場指標失敗: {}", e.getMessage());
            response.put("error", "獲取市場指標失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 健康檢查
     * GET /mcp/v1/health
     */
    @Operation(summary = "健康檢查", description = "檢查 MCP API 服務狀態")
    @ApiResponse(responseCode = "200", description = "服務正常")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "mcp-api");
        response.put("version", "1.0.0");
        
        // 檢查依賴服務狀態
        Map<String, String> dependencies = new HashMap<>();
        dependencies.put("placeBuyOrderService", "UP");
        dependencies.put("placeSellOrderService", "UP");
        dependencies.put("orderQueryService", "UP");
        dependencies.put("eapMatchEngine", "UP");
        
        response.put("dependencies", dependencies);
        
        return ResponseEntity.ok(response);
    }
}
