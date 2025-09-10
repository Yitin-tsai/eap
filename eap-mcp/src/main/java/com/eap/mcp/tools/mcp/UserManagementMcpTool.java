package com.eap.mcp.tools.mcp;

import com.eap.mcp.client.WalletServiceClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
public class UserManagementMcpTool {

    @Autowired
    private WalletServiceClient walletServiceClient;

    /**
     * 註冊新用戶並創建錢包
     * 每個新用戶將獲得 10000 電力和 10000 金額的初始餘額
     */
    @Tool(name = "registerUser", description = "註冊新用戶並創建錢包，新用戶將獲得 10000 電力和 10000 金額的初始餘額")
    public Map<String, Object> registerUser() {
        try {
            WalletServiceClient.UserRegistrationResponse response = walletServiceClient.registerUser();
            
            if (response != null && response.isSuccess()) {
                UUID userId = response.getUserId();
                log.info("用戶註冊成功: userId={}", userId);
                
                return Map.of(
                    "success", true,
                    "userId", userId.toString(),
                    "message", "用戶註冊成功！新用戶獲得 10,000 電力 + 10,000 金額的初始餘額",
                    "availableAmount", 10000,
                    "availableCurrency", 10000,
                    "timestamp", System.currentTimeMillis()
                );
            } else {
                String errorMsg = response != null ? response.getMessage() : "未知錯誤";
                log.error("用戶註冊失敗: {}", errorMsg);
                return Map.of(
                    "success", false,
                    "error", "用戶註冊失敗: " + errorMsg,
                    "timestamp", System.currentTimeMillis()
                );
            }
            
        } catch (Exception e) {
            log.error("用戶註冊過程中發生異常", e);
            return Map.of(
                "success", false,
                "error", "用戶註冊失敗: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
        }
    }

    /**
     * 查詢用戶錢包狀態
     */
    @Tool(name = "getUserWallet", description = "查詢指定用戶的錢包狀態，包括可用餘額和鎖定餘額")
    public Map<String, Object> getUserWallet(@ToolParam(description = "用戶ID (UUID格式)", required = true) String userId) {
        try {
            UUID userUuid = UUID.fromString(userId);
            WalletServiceClient.WalletStatusResponse wallet = walletServiceClient.getWalletStatus(userUuid);
            
            if (wallet == null) {
                return Map.of(
                    "success", false,
                    "error", "找不到指定的用戶錢包，請確認用戶ID是否正確",
                    "userId", userId,
                    "timestamp", System.currentTimeMillis()
                );
            }
            
            return Map.of(
                "success", true,
                "userId", wallet.getUserId().toString(),
                "availableAmount", wallet.getAvailableAmount(),
                "lockedAmount", wallet.getLockedAmount(),
                "availableCurrency", wallet.getAvailableCurrency(),
                "lockedCurrency", wallet.getLockedCurrency(),
                "updatedAt", wallet.getUpdatedAt(),
                "timestamp", System.currentTimeMillis()
            );
            
        } catch (IllegalArgumentException e) {
            return Map.of(
                "success", false,
                "error", "無效的用戶ID格式",
                "userId", userId,
                "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            log.error("查詢用戶錢包失敗", e);
            return Map.of(
                "success", false,
                "error", "查詢錢包狀態失敗: " + e.getMessage(),
                "userId", userId,
                "timestamp", System.currentTimeMillis()
            );
        }
    }

    /**
     * 檢查用戶是否存在
     */
    @Tool(name = "checkUserExists", description = "檢查指定的用戶ID是否存在於系統中")
    public Map<String, Object> checkUserExists(@ToolParam(description = "用戶ID (UUID格式)", required = true) String userId) {
        try {
            UUID userUuid = UUID.fromString(userId);
            boolean exists = walletServiceClient.checkUserExists(userUuid);
            
            return Map.of(
                "success", true,
                "userId", userId,
                "exists", exists,
                "message", exists ? "用戶存在於系統中" : "用戶不存在，您可能需要先註冊",
                "timestamp", System.currentTimeMillis()
            );
            
        } catch (IllegalArgumentException e) {
            return Map.of(
                "success", false,
                "error", "無效的用戶ID格式",
                "userId", userId,
                "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            log.error("檢查用戶存在性失敗", e);
            return Map.of(
                "success", false,
                "error", "檢查用戶存在性失敗: " + e.getMessage(),
                "userId", userId,
                "timestamp", System.currentTimeMillis()
            );
        }
    }
}
