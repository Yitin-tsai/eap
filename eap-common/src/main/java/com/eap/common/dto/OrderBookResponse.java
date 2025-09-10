package com.eap.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderBookResponse {
    
    @JsonProperty("orderBook")
    private OrderBookResponseDto orderBook;
    
    @JsonProperty("depth")
    private int depth;
    
    @JsonProperty("symbol")
    private String symbol;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("message")
    private String message;
    
    public static OrderBookResponse success(OrderBookResponseDto orderBook, int depth, String symbol) {
        OrderBookResponse response = new OrderBookResponse();
        response.setOrderBook(orderBook);
        response.setDepth(depth);
        response.setSymbol(symbol != null ? symbol : "ELC");
        response.setTimestamp(LocalDateTime.now());
        response.setSuccess(true);
        response.setMessage("獲取訂單簿成功");
        return response;
    }
    
    public static OrderBookResponse failure(String message) {
        OrderBookResponse response = new OrderBookResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}
