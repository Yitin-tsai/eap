package com.eap.eap_wallet.application;

import com.eap.eap_wallet.configuration.repository.WalletRepository;
import com.eap.eap_wallet.domain.entity.WalletEntity;
import com.eap.eap_wallet.domain.event.OrderCreateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Spring Cloud Contract 的基礎測試類
 * 這個類提供了 Contract 測試所需的 Spring 上下文和真實的服務
 */
@SpringBootTest
@AutoConfigureMessageVerifier
@DirtiesContext
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:contracttest",
    "spring.datasource.driver-class-name=org.h2.Driver", 
    "spring.datasource.username=sa",
    "spring.datasource.password=password",
    "spring.liquibase.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
public abstract class CreateOrderListenerContractBase {

    @Autowired
    protected CreateOrderListener createOrderListener;

    @Autowired
    protected WalletRepository walletRepository;

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    protected UUID testUserId;
    protected UUID testOrderId;

    @BeforeEach
    public void setup() {
        testUserId = UUID.fromString("12345678-1234-1234-1234-123456789012");
        testOrderId = UUID.fromString("87654321-4321-4321-4321-210987654321");
        
        // 設置測試數據
        setupTestWallet();
    }

    private void setupTestWallet() {
        // 清理現有數據
        walletRepository.deleteAll();
        
        // 創建測試錢包數據
        WalletEntity testWallet = WalletEntity.builder()
                .userId(testUserId)
                .availableAmount(1000)  // 足夠的餘額
                .lockedAmount(0)
                .updateTime(LocalDateTime.now())
                .build();
        
        walletRepository.save(testWallet);
    }

    /**
     * 觸發訂單創建事件的方法
     * Contract 會調用這個方法來觸發事件
     */
    public void triggerOrderCreateEvent() {
        OrderCreateEvent orderCreateEvent = OrderCreateEvent.builder()
                .orderId(testOrderId)
                .userId(testUserId)
                .price(100)
                .amount(50)
                .orderType("BUY")
                .createdAt(LocalDateTime.now())
                .build();

        // 直接調用 listener 來處理事件
        createOrderListener.onOrderCreate(orderCreateEvent);
    }

    /**
     * 觸發餘額不足的訂單創建事件
     */
    public void triggerInsufficientBalanceEvent() {
        OrderCreateEvent orderCreateEvent = OrderCreateEvent.builder()
                .orderId(testOrderId)
                .userId(testUserId)
                .price(100)
                .amount(2000)  // 超過可用餘額
                .orderType("BUY")
                .createdAt(LocalDateTime.now())
                .build();

        // 這會拋出異常
        createOrderListener.onOrderCreate(orderCreateEvent);
    }
}
