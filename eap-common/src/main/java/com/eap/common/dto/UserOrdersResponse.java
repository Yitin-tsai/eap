package com.eap.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOrdersResponse {
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("orders")
    private List<Object> orders;
    
    @JsonProperty("totalCount")
    private int totalCount;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("message")
    private String message;
    
    public static UserOrdersResponse success(String userId, List<Object> orders, String status) {
        UserOrdersResponse response = new UserOrdersResponse();
        response.setUserId(userId);
        response.setOrders(orders);
        response.setTotalCount(orders.size());
        response.setStatus(status != null ? status : "all");
        response.setTimestamp(LocalDateTime.now());
        response.setSuccess(true);
        response.setMessage("查詢訂單成功");
        return response;
    }
    
    public static UserOrdersResponse failure(String userId, String message) {
        UserOrdersResponse response = new UserOrdersResponse();
        response.setUserId(userId);
        response.setSuccess(false);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}
