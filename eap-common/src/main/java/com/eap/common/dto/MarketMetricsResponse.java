package com.eap.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketMetricsResponse {
    
    @JsonProperty("marketSummary")
    private MarketSummaryDto marketSummary;
    
    @JsonProperty("orderBook")
    private OrderBookResponseDto orderBook;
    
    @JsonProperty("metrics")
    private Map<String, Object> metrics;
    
    @JsonProperty("orderBookDepth")
    private int orderBookDepth;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("message")
    private String message;
    
    public static MarketMetricsResponse success(MarketSummaryDto marketSummary, 
                                              OrderBookResponseDto orderBook,
                                              Map<String, Object> metrics,
                                              int depth) {
        MarketMetricsResponse response = new MarketMetricsResponse();
        response.setMarketSummary(marketSummary);
        response.setOrderBook(orderBook);
        response.setMetrics(metrics);
        response.setOrderBookDepth(depth);
        response.setTimestamp(LocalDateTime.now());
        response.setSuccess(true);
        response.setMessage("獲取市場指標成功");
        return response;
    }
    
    public static MarketMetricsResponse failure(String message) {
        MarketMetricsResponse response = new MarketMetricsResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}
