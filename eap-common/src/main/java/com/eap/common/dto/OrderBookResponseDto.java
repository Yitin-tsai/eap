package com.eap.common.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 訂單簿響應 DTO（跨服務通信用）
 * 用於 MatchEngine 向 Order-Service 提供訂單簿數據
 */
@Data
@Builder
public class OrderBookResponseDto {
    
    /**
     * 買盤數據 (價格從高到低)
     */
    private List<OrderBookLevel> bids;
    
    /**
     * 賣盤數據 (價格從低到高)
     */
    private List<OrderBookLevel> asks;
    
    @Data
    @Builder
    public static class OrderBookLevel {
        /**
         * 價格
         */
        private Integer price;
        
        /**
         * 該價格的總數量
         */
        private Integer quantity;
        
        /**
         * 該價格的訂單數量
         */
        private Integer orderCount;
    }
}
