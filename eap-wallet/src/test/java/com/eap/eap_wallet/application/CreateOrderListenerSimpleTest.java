package com.eap.eap_wallet.application;

import com.eap.eap_wallet.configuration.repository.WalletRepository;
import com.eap.eap_wallet.domain.entity.WalletEntity;
import com.eap.eap_wallet.domain.event.OrderCreateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 簡化的 RabbitMQ 整合測試
 * 直接測試 CreateOrderListener 的方法，而不依賴真實的 RabbitMQ 訊息傳遞
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver", 
    "spring.datasource.username=sa",
    "spring.datasource.password=password",
    "spring.liquibase.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.rabbitmq.listener.simple.auto-startup=false",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"
})
class CreateOrderListenerSimpleTest {

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private WalletRepository walletRepository;

    @Autowired
    private CreateOrderListener createOrderListener;

    private UUID testUserId;
    private UUID testOrderId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testOrderId = UUID.randomUUID();
    }

    @Test
    void testOrderCreateEventProcessing_WithSufficientBalance() {
        // Given
        WalletEntity walletEntity = WalletEntity.builder()
                .id(1L)
                .userId(testUserId)
                .availableAmount(100)
                .lockedAmount(0)
                .updateTime(LocalDateTime.now())
                .build();

        when(walletRepository.findByUserId(testUserId)).thenReturn(walletEntity);

        OrderCreateEvent orderCreateEvent = OrderCreateEvent.builder()
                .orderId(testOrderId)
                .userId(testUserId)
                .price(1000)
                .amount(50)
                .orderType("BUY")
                .createdAt(LocalDateTime.now())
                .build();

        // When - 直接調用 listener 方法進行測試
        createOrderListener.onOrderCreate(orderCreateEvent);

        // Then
        verify(walletRepository).findByUserId(testUserId);
        // 驗證 RabbitTemplate 被調用來發送 OrderCreatedEvent
        verify(rabbitTemplate, times(1)).convertAndSend(eq("order.exchange"), eq("order.created"), any(Object.class));
    }

    @Test
    void testOrderCreateEventProcessing_WithInsufficientBalance() {
        // Given
        WalletEntity walletEntity = WalletEntity.builder()
                .id(1L)
                .userId(testUserId)
                .availableAmount(30) // 不足的餘額
                .lockedAmount(0)
                .updateTime(LocalDateTime.now())
                .build();

        when(walletRepository.findByUserId(testUserId)).thenReturn(walletEntity);

        OrderCreateEvent orderCreateEvent = OrderCreateEvent.builder()
                .orderId(testOrderId)
                .userId(testUserId)
                .price(1000)
                .amount(50) // 超過可用餘額
                .orderType("BUY")
                .createdAt(LocalDateTime.now())
                .build();

        // When & Then
        try {
            createOrderListener.onOrderCreate(orderCreateEvent);
            // 如果沒有拋出異常，測試應該失敗
            org.junit.jupiter.api.Assertions.fail("Expected ReturnException to be thrown");
        } catch (Exception e) {
            // 預期的異常
            org.junit.jupiter.api.Assertions.assertTrue(
                e.getMessage().contains("訂單數量超過可用餘額")
            );
        }

        verify(walletRepository).findByUserId(testUserId);
        // 驗證沒有發送 OrderCreatedEvent
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void testDirectListenerCall_WithSellOrder() {
        // Given
        WalletEntity walletEntity = WalletEntity.builder()
                .id(1L)
                .userId(testUserId)
                .availableAmount(200)
                .lockedAmount(0)
                .updateTime(LocalDateTime.now())
                .build();

        when(walletRepository.findByUserId(testUserId)).thenReturn(walletEntity);

        OrderCreateEvent orderCreateEvent = OrderCreateEvent.builder()
                .orderId(testOrderId)
                .userId(testUserId)
                .price(1500)
                .amount(75)
                .orderType("SELL")
                .createdAt(LocalDateTime.now())
                .build();

        // When
        createOrderListener.onOrderCreate(orderCreateEvent);

        // Then
        verify(walletRepository).findByUserId(testUserId);
        // 驗證 RabbitTemplate 被調用來發送 OrderCreatedEvent
        verify(rabbitTemplate, times(1)).convertAndSend(eq("order.exchange"), eq("order.created"), any(Object.class));
    }
}
