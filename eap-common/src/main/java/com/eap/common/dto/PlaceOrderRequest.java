package com.eap.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceOrderRequest {
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("side")
    private String side;
    
    @JsonProperty("price")
    private BigDecimal price;
    
    @JsonProperty("qty")
    private BigDecimal qty;
    
    @JsonProperty("type")
    @Builder.Default
    private String type = "LIMIT";
    
    @JsonProperty("symbol")
    @Builder.Default
    private String symbol = "ELC";
    
    // 便利方法
    public boolean isBuy() {
        return "BUY".equalsIgnoreCase(side);
    }
    
    public boolean isSell() {
        return "SELL".equalsIgnoreCase(side);
    }
    
    public int getPriceAsInt() {
        return price.intValue();
    }
    
    public int getQtyAsInt() {
        return qty.intValue();
    }
    
    // 驗證方法
    public boolean isValid() {
        return userId != null && !userId.trim().isEmpty() &&
               side != null && (isBuy() || isSell()) &&
               price != null && price.compareTo(BigDecimal.ZERO) > 0 &&
               qty != null && qty.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public String getValidationError() {
        if (userId == null || userId.trim().isEmpty()) {
            return "用戶ID不能為空";
        }
        if (side == null || (!isBuy() && !isSell())) {
            return "訂單方向必須是 BUY 或 SELL";
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return "價格必須大於0";
        }
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            return "數量必須大於0";
        }
        return null;
    }
}
