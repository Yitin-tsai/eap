package com.eap.eap_matchengine;

import com.eap.eap_matchengine.application.MatchingEngineService;
import com.eap.eap_matchengine.application.RedisOrderBookService;
import com.eap.common.event.OrderCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(classes = { MatchingEngineService.class, BaseContractTest.TestConfiguration.class })
@AutoConfigureMessageVerifier
@ActiveProfiles("test")
public class BaseContractTest {

    @Configuration
    static class TestConfiguration {
        @Bean
        @Primary
        public ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Autowired
    private MatchingEngineService matchingEngineService;

    @MockitoBean
    private RedisOrderBookService redisOrderBookService;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    public void setup() {
        // 準備測試資料：模擬已存在的買單
        OrderCreatedEvent existingBuyOrder = OrderCreatedEvent.builder()
                .orderId(UUID.fromString("e052f2bb-8bb9-4f0f-9c0c-385a2b16c50b"))
                .userId(UUID.fromString("456f2bb8-8bb9-4f0f-9c0c-385a2b16c50b"))
                .price(100)
                .ammount(10)
                .orderType("BUY")
                .createdAt(LocalDateTime.parse("2025-07-18T09:59:00"))
                .build();

        // 當收到賣單時，返回匹配的買單
        Mockito.when(redisOrderBookService.getMatchableOrders(any(OrderCreatedEvent.class)))
                .thenReturn(List.of(existingBuyOrder));

        Mockito.doNothing().when(rabbitTemplate).convertAndSend(Mockito.any(String.class),
                Mockito.any(String.class),
                Mockito.any(Object.class));
    }

    public void sendOrderCreatedEvent() {
        // 建立賣單事件
        OrderCreatedEvent sellOrder = OrderCreatedEvent.builder()
                .orderId(UUID.fromString("e052f2bb-8bb9-4f0f-9c0c-385a2b16c50a"))
                .userId(UUID.fromString("123f2bb8-8bb9-4f0f-9c0c-385a2b16c50a"))
                .price(100)
                .ammount(10)
                .orderType("SELL")
                .createdAt(LocalDateTime.parse("2025-07-18T10:00:00"))
                .build();
        // 觸發訂單匹配
        matchingEngineService.tryMatch(sellOrder);
    }
}
