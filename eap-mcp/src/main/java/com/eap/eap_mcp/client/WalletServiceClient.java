package com.eap.mcp.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class WalletServiceClient {

    private final RestTemplate restTemplate;
    private final String walletServiceUrl;

    public WalletServiceClient(RestTemplate restTemplate, 
                              @Value("${services.wallet.url:http://localhost:8081/eap-wallet}") String walletServiceUrl) {
        this.restTemplate = restTemplate;
        this.walletServiceUrl = walletServiceUrl;
    }

    /**
     * 註冊新用戶並創建錢包
     * @return 用戶註冊響應包含用戶 ID
     */
    public UserRegistrationResponse registerUser() {
        try {
            String url = walletServiceUrl + "/v1/wallet/register";
            log.info("調用錢包服務註冊用戶: {}", url);
            
            return restTemplate.postForObject(url, null, UserRegistrationResponse.class);
            
        } catch (Exception e) {
            log.error("調用錢包服務註冊用戶失敗", e);
            throw new RuntimeException("用戶註冊失敗: " + e.getMessage());
        }
    }

    /**
     * 查詢用戶錢包狀態
     * @param userId 用戶 ID
     * @return 錢包狀態響應
     */
    public WalletStatusResponse getWalletStatus(UUID userId) {
        try {
            String url = walletServiceUrl + "/v1/wallet/status/" + userId;
            log.info("查詢用戶錢包狀態: {}", url);
            
            return restTemplate.getForObject(url, WalletStatusResponse.class);
            
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("用戶錢包不存在: userId={}", userId);
            return null;
        } catch (Exception e) {
            log.error("查詢用戶錢包狀態失敗: userId={}", userId, e);
            throw new RuntimeException("查詢錢包狀態失敗: " + e.getMessage());
        }
    }

    /**
     * 檢查用戶是否存在
     * @param userId 用戶 ID
     * @return 是否存在
     */
    public boolean checkUserExists(UUID userId) {
        try {
            String url = walletServiceUrl + "/v1/wallet/exists/" + userId;
            log.info("檢查用戶是否存在: {}", url);
            
            Boolean exists = restTemplate.getForObject(url, Boolean.class);
            return exists != null && exists;
            
        } catch (Exception e) {
            log.error("檢查用戶存在性失敗: userId={}", userId, e);
            return false;
        }
    }

    // DTO 類
    public static class UserRegistrationResponse {
        private UUID userId;
        private String message;
        private boolean success;

        // Getters and Setters
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
    }

    public static class WalletStatusResponse {
        private UUID userId;
        private Integer availableAmount;
        private Integer lockedAmount;
        private Integer availableCurrency;
        private Integer lockedCurrency;
        private String createdAt;
        private String updatedAt;
        private boolean success;
        private String message;

        // Getters and Setters
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        
        public Integer getAvailableAmount() { return availableAmount; }
        public void setAvailableAmount(Integer availableAmount) { this.availableAmount = availableAmount; }
        
        public Integer getLockedAmount() { return lockedAmount; }
        public void setLockedAmount(Integer lockedAmount) { this.lockedAmount = lockedAmount; }
        
        public Integer getAvailableCurrency() { return availableCurrency; }
        public void setAvailableCurrency(Integer availableCurrency) { this.availableCurrency = availableCurrency; }
        
        public Integer getLockedCurrency() { return lockedCurrency; }
        public void setLockedCurrency(Integer lockedCurrency) { this.lockedCurrency = lockedCurrency; }
        
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
