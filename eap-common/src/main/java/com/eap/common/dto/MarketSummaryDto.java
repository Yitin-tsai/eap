package com.eap.common.dto;

import lombok.Data;

/**
 * 市場簡要統計 DTO（跨服務通信用）
 * 用於 MatchEngine 向 Order-Service 提供市場簡要數據
 */
@Data
public class MarketSummaryDto {
    /**
     * 最佳買價
     */
    private Integer bestBidPrice;
    
    /**
     * 最佳賣價
     */
    private Integer bestAskPrice;
    
    /**
     * 計算價差
     */
    public Integer getSpread() {
        if (bestBidPrice != null && bestAskPrice != null) {
            return bestAskPrice - bestBidPrice;
        }
        return null;
    }
}
