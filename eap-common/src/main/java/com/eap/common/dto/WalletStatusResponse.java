package com.eap.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletStatusResponse {
    
    @JsonProperty("userId")
    private UUID userId;
    
    @JsonProperty("availableAmount")
    private Integer availableAmount;
    
    @JsonProperty("lockedAmount")
    private Integer lockedAmount;
    
    @JsonProperty("availableCurrency")
    private Integer availableCurrency;
    
    @JsonProperty("lockedCurrency")
    private Integer lockedCurrency;
    
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
    
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("message")
    private String message;
    
    public static WalletStatusResponse success(UUID userId, Integer availableAmount, Integer lockedAmount, 
                                             Integer availableCurrency, Integer lockedCurrency, 
                                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        WalletStatusResponse response = new WalletStatusResponse();
        response.setUserId(userId);
        response.setAvailableAmount(availableAmount);
        response.setLockedAmount(lockedAmount);
        response.setAvailableCurrency(availableCurrency);
        response.setLockedCurrency(lockedCurrency);
        response.setCreatedAt(createdAt);
        response.setUpdatedAt(updatedAt);
        response.setSuccess(true);
        response.setMessage("錢包查詢成功");
        return response;
    }
    
    public static WalletStatusResponse notFound(UUID userId) {
        WalletStatusResponse response = new WalletStatusResponse();
        response.setUserId(userId);
        response.setSuccess(false);
        response.setMessage("找不到指定用戶的錢包");
        return response;
    }
}
