package com.eap.mcp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 下單請求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequest {
    private String side;           // BUY/SELL
    private String type;           // LIMIT/MARKET
    private BigDecimal price;      // 價格
    private BigDecimal qty;        // 數量
    private String symbol;         // 交易標的
    private String userId;         // 用戶ID
    private String tif;            // Time In Force (GTC, IOC, FOK)
    private String clientOrderId;  // 客戶端訂單ID
}
