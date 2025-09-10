package com.eap.eap_wallet.controller;

import com.eap.common.event.OrderCreatedEvent;
import com.eap.eap_wallet.application.UserRegistrationService;
import com.eap.eap_wallet.application.WalletCheckService;
import com.eap.eap_wallet.domain.dto.UserRegistrationResponse;
import com.eap.eap_wallet.domain.dto.WalletStatusResponse;
import com.eap.eap_wallet.domain.entity.WalletEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/wallet")
public class WalletController {

    @Autowired
    WalletCheckService walletCheckService;

    @Autowired
    UserRegistrationService userRegistrationService;

    @PostMapping("/check")
    public boolean checkWallet(@RequestBody OrderCreatedEvent event) {
        return walletCheckService.checkWallet(event);
    }

    /**
     * 用戶註冊 - 創建新錢包
     * @return 包含新用戶 ID 的響應
     */
    @PostMapping("/register")
    public ResponseEntity<UserRegistrationResponse> registerUser() {
        try {
            UUID newUserId = userRegistrationService.createNewUserWallet();
            UserRegistrationResponse response = UserRegistrationResponse.success(newUserId);
            log.info("用戶註冊成功: {}", newUserId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("用戶註冊失敗", e);
            UserRegistrationResponse response = UserRegistrationResponse.failure("註冊失敗: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 根據用戶 ID 查詢錢包狀態
     * @param userId 用戶 UUID
     * @return 錢包狀態信息
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<WalletStatusResponse> getWalletStatus(@PathVariable("userId") UUID userId) {
        try {
            WalletEntity wallet = userRegistrationService.getWalletByUserId(userId);
            
            if (wallet == null) {
                WalletStatusResponse response = WalletStatusResponse.notFound(userId);
                return ResponseEntity.notFound().build();
            }
            
            WalletStatusResponse response = WalletStatusResponse.fromEntity(wallet);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("查詢錢包狀態失敗: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 檢查用戶是否存在
     * @param userId 用戶 UUID
     * @return 是否存在
     */
    @GetMapping("/exists/{userId}")
    public ResponseEntity<Boolean> checkUserExists(@PathVariable("userId") UUID userId) {
        try {
            boolean exists = userRegistrationService.userExists(userId);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            log.error("檢查用戶存在性失敗: userId={}", userId, e);
            return ResponseEntity.internalServerError().body(false);
        }
    }
}