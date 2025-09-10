package com.eap.mcp.tools.mcp;

import com.eap.mcp.client.WalletServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
@Description("用戶註冊和錢包管理工具")
public class UserManagementMcpTool {

    @Autowired
    private WalletServiceClient walletServiceClient;

    /**
     * 註冊新用戶並創建錢包
     * 每個新用戶將獲得 10000 電力和 10000 金額的初始餘額
     */
    @org.springframework.ai.model.function.FunctionCallback.Tool(
        name = "registerUser", 
        description = "註冊新用戶並創建錢包，新用戶將獲得 10000 電力和 10000 金額的初始餘額"
    )
    public Function<Void, String> registerUser() {
        return (input) -> {
            try {
                WalletServiceClient.UserRegistrationResponse response = walletServiceClient.registerUser();
                
                if (response != null && response.isSuccess()) {
                    UUID userId = response.getUserId();
                    log.info("用戶註冊成功: userId={}", userId);
                    
                    return String.format(
                        "用戶註冊成功！\n" +
                        "用戶ID: %s\n" +
                        "初始餘額: 10,000 電力 + 10,000 金額\n" +
                        "您現在可以開始交易了！",
                        userId
                    );
                } else {
                    String errorMsg = response != null ? response.getMessage() : "未知錯誤";
                    log.error("用戶註冊失敗: {}", errorMsg);
                    return "用戶註冊失敗: " + errorMsg;
                }
                
            } catch (Exception e) {
                log.error("用戶註冊過程中發生異常", e);
                return "用戶註冊失敗: " + e.getMessage();
            }
        };
    }

    /**
     * 查詢用戶錢包狀態
     */
    @org.springframework.ai.model.function.FunctionCallback.Tool(
        name = "getUserWallet", 
        description = "查詢指定用戶的錢包狀態，包括可用餘額和鎖定餘額"
    )
    public Function<GetUserWalletRequest, String> getUserWallet() {
        return (request) -> {
            try {
                UUID userId = UUID.fromString(request.userId);
                WalletServiceClient.WalletStatusResponse wallet = walletServiceClient.getWalletStatus(userId);
                
                if (wallet == null) {
                    return "錯誤: 找不到指定的用戶錢包，請確認用戶ID是否正確";
                }
                
                return String.format(
                    "用戶錢包狀態:\n" +
                    "用戶ID: %s\n" +
                    "可用電力: %,d\n" +
                    "鎖定電力: %,d\n" +
                    "可用金額: %,d\n" +
                    "鎖定金額: %,d\n" +
                    "最後更新: %s",
                    wallet.getUserId(),
                    wallet.getAvailableAmount(),
                    wallet.getLockedAmount(),
                    wallet.getAvailableCurrency(),
                    wallet.getLockedCurrency(),
                    wallet.getUpdatedAt()
                );
                
            } catch (IllegalArgumentException e) {
                return "錯誤: 無效的用戶ID格式";
            } catch (Exception e) {
                log.error("查詢用戶錢包失敗", e);
                return "查詢錢包狀態失敗: " + e.getMessage();
            }
        };
    }

    /**
     * 檢查用戶是否存在
     */
    @org.springframework.ai.model.function.FunctionCallback.Tool(
        name = "checkUserExists", 
        description = "檢查指定的用戶ID是否存在於系統中"
    )
    public Function<CheckUserExistsRequest, String> checkUserExists() {
        return (request) -> {
            try {
                UUID userId = UUID.fromString(request.userId);
                boolean exists = walletServiceClient.checkUserExists(userId);
                
                if (exists) {
                    return String.format("用戶 %s 存在於系統中", userId);
                } else {
                    return String.format("用戶 %s 不存在，您可能需要先註冊", userId);
                }
                
            } catch (IllegalArgumentException e) {
                return "錯誤: 無效的用戶ID格式";
            } catch (Exception e) {
                log.error("檢查用戶存在性失敗", e);
                return "檢查用戶存在性失敗: " + e.getMessage();
            }
        };
    }

    // 請求 DTO 類
    public static class GetUserWalletRequest {
        @Description("用戶ID (UUID格式)")
        public String userId;
    }

    public static class CheckUserExistsRequest {
        @Description("用戶ID (UUID格式)")
        public String userId;
    }
}
