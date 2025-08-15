package com.eap.eap_wallet.application;

import com.eap.eap_wallet.configuration.ReturnException;
import com.eap.eap_wallet.configuration.repository.WalletRepository;
import com.eap.eap_wallet.domain.entity.WalletEntity;
import com.eap.common.event.OrderCreateEvent;
import com.eap.common.event.OrderCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrderListenerTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private CreateOrderListener createOrderListener;

    private UUID testUserId;
    private UUID testOrderId;
    private LocalDateTime testCreatedAt;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testOrderId = UUID.randomUUID();
        testCreatedAt = LocalDateTime.now();
    }

    @Test
    void testOnOrderCreate_WhenWalletHasSufficientBalance_ShouldPublishOrderCreatedEvent() {
        // Given
        OrderCreateEvent orderCreateEvent = OrderCreateEvent.builder()
                .orderId(testOrderId)
                .userId(testUserId)
                .price(1000)
                .amount(50)
                .orderType("BUY")
                .createdAt(testCreatedAt)
                .build();

        WalletEntity walletEntity = WalletEntity.builder()
                .id(1L)
                .userId(testUserId)
                .availableAmount(100)
                .availableCurrency(1000000000)
                .lockedCurrency(0)
                .lockedAmount(0)
                .updateTime(LocalDateTime.now())
                .build();

        when(walletRepository.findByUserId(testUserId)).thenReturn(walletEntity);

        // When
        createOrderListener.onOrderCreate(orderCreateEvent);

        // Then
        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("order.exchange"), eq("order.created"), eventCaptor.capture());

        OrderCreatedEvent capturedEvent = eventCaptor.getValue();
        assertEquals(testOrderId, capturedEvent.getOrderId());
        assertEquals(testUserId, capturedEvent.getUserId());
        assertEquals(1000, capturedEvent.getPrice());
        assertEquals(50, capturedEvent.getAmmount());
        assertEquals("BUY", capturedEvent.getOrderType());
        assertEquals(testCreatedAt, capturedEvent.getCreatedAt());
    }

    @Test
    void testOnOrderCreate_WhenWalletHasInsufficientBalance_ShouldThrowReturnException() {
        // Given
        OrderCreateEvent orderCreateEvent = OrderCreateEvent.builder()
                .orderId(testOrderId)
                .userId(testUserId)
                .price(1000)
                .amount(150) // 超過可用餘額
                .orderType("BUY")
                .createdAt(testCreatedAt)
                .build();

        WalletEntity walletEntity = WalletEntity.builder()
                .id(1L)
                .userId(testUserId)
                .availableAmount(100)
                .availableCurrency(1)
                .lockedAmount(0)
                .updateTime(LocalDateTime.now())
                .build();

        when(walletRepository.findByUserId(testUserId)).thenReturn(walletEntity);

        // When & Then
        ReturnException exception = assertThrows(ReturnException.class, 
            () -> createOrderListener.onOrderCreate(orderCreateEvent));


        // 驗證沒有發送 OrderCreatedEvent
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(OrderCreatedEvent.class));
    }

  
}
