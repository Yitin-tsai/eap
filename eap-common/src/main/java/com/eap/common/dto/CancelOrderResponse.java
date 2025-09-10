package com.eap.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderResponse {
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("cancelledAt")
    private LocalDateTime cancelledAt;
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("message")
    private String message;
    
    public static CancelOrderResponse success(String orderId) {
        CancelOrderResponse response = new CancelOrderResponse();
        response.setOrderId(orderId);
        response.setStatus("CANCELLED");
        response.setCancelledAt(LocalDateTime.now());
        response.setSuccess(true);
        response.setMessage("訂單取消成功");
        return response;
    }
    
    public static CancelOrderResponse failure(String orderId, String message) {
        CancelOrderResponse response = new CancelOrderResponse();
        response.setOrderId(orderId);
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
