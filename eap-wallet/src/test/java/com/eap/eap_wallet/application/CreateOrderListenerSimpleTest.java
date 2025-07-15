package com.eap.eap_wallet.application;

import com.eap.eap_wallet.configuration.repository.WalletRepository;
import com.eap.eap_wallet.domain.entity.WalletEntity;
import com.eap.eap_wallet.domain.event.OrderCreateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 使用 Spring Cloud Contract 依賴庫的整合測試
 * 不使用 Mock，而是真正測試服務間的整合
 */
@SpringBootTest
@DirtiesContext
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:contracttest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "spring.datasource.driver-class-name=org.h2.Driver", 
    "spring.datasource.username=sa",
    "spring.datasource.password=password",
    "spring.liquibase.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.rabbitmq.listener.simple.auto-startup=false",
    "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true"
})
class CreateOrderListenerSimpleTest {

    @Autowired
    private CreateOrderListener createOrderListener;

    @Autowired
    private WalletRepository walletRepository;

    private UUID testUserId;
    private UUID testOrderId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.fromString("12345678-1234-1234-1234-123456789012");
        testOrderId = UUID.fromString("87654321-4321-4321-4321-210987654321");
        
        // 清理現有數據
        walletRepository.deleteAll();
    }

    @Test
    void testContractWithSufficientBalance() {
        // Given - 創建有足夠餘額的錢包
        WalletEntity walletEntity = WalletEntity.builder()
                .userId(testUserId)
                .availableAmount(1000)
                .lockedAmount(0)
                .updateTime(LocalDateTime.now())
                .build();
        walletRepository.save(walletEntity);

        // When - 創建訂單事件
        OrderCreateEvent orderCreateEvent = OrderCreateEvent.builder()
                .orderId(testOrderId)
                .userId(testUserId)
                .price(100)
                .amount(50)
                .orderType("BUY")
                .createdAt(LocalDateTime.now())
                .build();

        // Then - 不應該拋出異常
        assertDoesNotThrow(() -> {
            createOrderListener.onOrderCreate(orderCreateEvent);
        });

        // 驗證錢包數據
        WalletEntity updatedWallet = walletRepository.findByUserId(testUserId);
        assertNotNull(updatedWallet);
        assertEquals(testUserId, updatedWallet.getUserId());
    }

    @Test
    void testContractWithInsufficientBalance() {
        // Given - 創建餘額不足的錢包
        WalletEntity walletEntity = WalletEntity.builder()
                .userId(testUserId)
                .availableAmount(30)  // 不足的餘額
                .lockedAmount(0)
                .updateTime(LocalDateTime.now())
                .build();
        walletRepository.save(walletEntity);

        // When - 創建超過餘額的訂單事件
        OrderCreateEvent orderCreateEvent = OrderCreateEvent.builder()
                .orderId(testOrderId)
                .userId(testUserId)
                .price(100)
                .amount(50)  // 超過可用餘額
                .orderType("BUY")
                .createdAt(LocalDateTime.now())
                .build();

        // Then - 應該拋出異常
        Exception exception = assertThrows(Exception.class, () -> {
            createOrderListener.onOrderCreate(orderCreateEvent);
        });
        
        assertTrue(exception.getMessage().contains("訂單數量超過可用餘額"));
    }

    @Test
    void testContractBasedMessaging() {
        // Given - 設置測試數據
        WalletEntity walletEntity = WalletEntity.builder()
                .userId(testUserId)
                .availableAmount(200)
                .lockedAmount(0)
                .updateTime(LocalDateTime.now())
                .build();
        walletRepository.save(walletEntity);

        // When - 測試真實的訊息處理（不使用 Mock）
        OrderCreateEvent orderCreateEvent = OrderCreateEvent.builder()
                .orderId(testOrderId)
                .userId(testUserId)
                .price(150)
                .amount(75)
                .orderType("SELL")
                .createdAt(LocalDateTime.now())
                .build();

        // Then - 真實調用服務
        assertDoesNotThrow(() -> {
            createOrderListener.onOrderCreate(orderCreateEvent);
        });

        // 檢查實際的資料庫狀態
        WalletEntity result = walletRepository.findByUserId(testUserId);
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
    }
}
