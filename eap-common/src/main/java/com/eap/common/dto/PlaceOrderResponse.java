package com.eap.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderResponse {
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("acceptedAt")
    private LocalDateTime acceptedAt;
    
    @JsonProperty("side")
    private String side;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("price")
    private BigDecimal price;
    
    @JsonProperty("qty")
    private BigDecimal qty;
    
    @JsonProperty("symbol")
    private String symbol;
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("message")
    private String message;
    
    public static PlaceOrderResponse success(String orderId, String side, String type, 
                                           BigDecimal price, BigDecimal qty, String symbol) {
        PlaceOrderResponse response = new PlaceOrderResponse();
        response.setOrderId(orderId);
        response.setStatus("PENDING");
        response.setAcceptedAt(LocalDateTime.now());
        response.setSide(side);
        response.setType(type);
        response.setPrice(price);
        response.setQty(qty);
        response.setSymbol(symbol);
        response.setSuccess(true);
        response.setMessage("訂單提交成功");
        return response;
    }
    
    public static PlaceOrderResponse failure(String message) {
        PlaceOrderResponse response = new PlaceOrderResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
