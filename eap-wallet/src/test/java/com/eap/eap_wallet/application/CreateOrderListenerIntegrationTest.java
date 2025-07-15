package com.eap.eap_wallet.application;

import com.eap.eap_wallet.configuration.repository.WalletRepository;
import com.eap.eap_wallet.domain.entity.WalletEntity;
import com.eap.eap_wallet.domain.event.OrderCreateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 使用 Testcontainers 的 RabbitMQ 集成測試
 * 自動啟動和管理 Docker 容器，無需手動啟動 Docker
 */
@SpringBootTest
@Testcontainers
class CreateOrderListenerIntegrationTest {

    @Container
    static RabbitMQContainer rabbitContainer = new RabbitMQContainer("rabbitmq:3.12-management")
            .withStartupTimeout(Duration.ofMinutes(3));

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withStartupTimeout(Duration.ofMinutes(2))
            .withInitScript("init-test-db.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // RabbitMQ properties
        registry.add("spring.rabbitmq.host", rabbitContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitContainer::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitContainer::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitContainer::getAdminPassword);
        
        // PostgreSQL properties
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        
        // Liquibase 設定 - 使用正確的 changelog 檔案
        registry.add("spring.liquibase.enabled", () -> "true");
        registry.add("spring.liquibase.change-log", () -> "classpath:db/changelog/db.changelog-master.xml");
        registry.add("spring.liquibase.default-schema", () -> "wallet_service");
        registry.add("spring.liquibase.liquibase-schema", () -> "wallet_service");
        registry.add("spring.liquibase.drop-first", () -> "false");
        
        // JPA 設定
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        
        // Debug logging
        registry.add("logging.level.org.springframework.amqp", () -> "DEBUG");
        registry.add("logging.level.com.eap.eap_wallet", () -> "DEBUG");
        registry.add("logging.level.liquibase", () -> "DEBUG");
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @MockitoBean
    private WalletRepository walletRepository;

    private UUID testUserId;
    private UUID testOrderId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testOrderId = UUID.randomUUID();

        // 確保隊列存在
        rabbitAdmin.declareQueue(new Queue("order.create.queue", true));
        rabbitAdmin.declareQueue(new Queue("order.created.queue", true));
        rabbitAdmin.declareExchange(new TopicExchange("order.exchange"));
    }

    @Test
    void testCompleteOrderFlow_WithSufficientBalance() {
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

        // When - 發送真實的 RabbitMQ 訊息
        rabbitTemplate.convertAndSend("order.exchange", "order.create", orderCreateEvent);

        // Then - 等待異步處理完成
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(walletRepository, atLeastOnce()).findByUserId(testUserId);
        });

        // 驗證 order.created 訊息被發送 (檢查隊列)
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Object message = rabbitTemplate.receiveAndConvert("order.created.queue");
            assertNotNull(message, "Should have received order.created message");
        });
    }

    @Test
    void testCompleteOrderFlow_WithInsufficientBalance() {
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

        // When - 發送真實的 RabbitMQ 訊息
        rabbitTemplate.convertAndSend("order.exchange", "order.create", orderCreateEvent);

        // Then - 等待處理完成
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(walletRepository, atLeastOnce()).findByUserId(testUserId);
        });

        // 確保沒有 order.created 訊息被發送
        await().during(3, TimeUnit.SECONDS).untilAsserted(() -> {
            Object message = rabbitTemplate.receiveAndConvert("order.created.queue");
            assertNull(message, "Should not have received order.created message when balance insufficient");
        });
    }

    @Test
    void testRabbitMQConnection() {
        // 簡單的連接測試
        assertNotNull(rabbitTemplate, "RabbitTemplate should be available");
        assertNotNull(rabbitAdmin, "RabbitAdmin should be available");
        
        // 測試基本的訊息發送
        String testMessage = "test-message";
        rabbitTemplate.convertAndSend("order.exchange", "test.routing.key", testMessage);
        
        // 如果沒有拋出異常，表示連接正常
        assertTrue(true, "RabbitMQ connection is working");
    }
}
