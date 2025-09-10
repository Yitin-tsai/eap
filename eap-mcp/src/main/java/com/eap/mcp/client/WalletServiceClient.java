package com.eap.mcp.client;

import com.eap.common.dto.UserRegistrationResponse;
import com.eap.common.dto.WalletStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.UUID;

@Slf4j
@Component
public class WalletServiceClient {

    private final RestTemplate restTemplate;
    private final String walletServiceUrl;

    public WalletServiceClient(RestTemplate restTemplate, 
                              @Value("${eap.wallet.base-url:http://localhost:8081/eap-wallet}") String walletServiceUrl) {
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
}
