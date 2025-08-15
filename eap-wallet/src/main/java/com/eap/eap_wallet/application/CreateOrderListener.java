package com.eap.eap_wallet.application;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.eap.eap_wallet.configuration.repository.WalletRepository;
import com.eap.eap_wallet.domain.entity.WalletEntity;
import com.eap.common.event.OrderCreateEvent;
import com.eap.common.event.OrderCreatedEvent;
import com.eap.common.event.OrderFailedEvent;
import static com.eap.common.constants.RabbitMQConstants.*;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Component
@Slf4j
public class CreateOrderListener {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = ORDER_CREATE_QUEUE)
    public void onOrderCreate(OrderCreateEvent event) {
        if (!isWalletEnough(event)) {
            log.warn("訂單金額超過可用餘額: " + event.getUserId());
            // 發送餘額不足通知
            sendOrderFailedEvent(event, "餘額不足");
            return;
        }

        if(!isWalletAmountEnoughForSell(event)) {
            log.warn("訂單可用電量不足: " + event.getUserId());
            // 發送電量不足通知
            sendOrderFailedEvent(event, "可用電量不足");
            return;
        }

        lockAsset(event);

        OrderCreatedEvent orderCreatedEvent = OrderCreatedEvent.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .price(event.getPrice())
                .ammount(event.getAmount())
                .orderType(event.getOrderType())
                .createdAt(event.getCreatedAt())
                .build();
        rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_CREATED_KEY, orderCreatedEvent);

    }

    private boolean isWalletEnough(OrderCreateEvent event) {

        WalletEntity wallet = walletRepository.findByUserId(event.getUserId());
        if (wallet == null) {
            log.warn("找不到使用者錢包: " + event.getUserId());
            return false;
        }
        if (event.getOrderType() == "BUY" && event.getAmount() * event.getPrice() > wallet.getAvailableCurrency()) {
            log.warn("訂單總金額超過可用餘額: " + event.getUserId());
            return false;
        }

        return true;
    }

    private boolean isWalletAmountEnoughForSell(OrderCreateEvent event) {

        WalletEntity wallet = walletRepository.findByUserId(event.getUserId());
        if (wallet == null) {
            log.warn("找不到使用者錢包: " + event.getUserId());
            return false;

        }
        if (event.getOrderType() == "SELL" && event.getAmount() > wallet.getAvailableAmount()) {
            log.warn("訂單總電量超過可供應電量: " + event.getUserId());
            return false;

        }
        return true;
    }

    private void lockAsset(OrderCreateEvent event) {
        WalletEntity wallet = walletRepository.findByUserId(event.getUserId());

        if ("BUY".equals(event.getOrderType())) {
            int lockCurrency = event.getPrice() * event.getAmount();
            wallet.setAvailableCurrency(wallet.getAvailableCurrency() - lockCurrency);
            wallet.setLockedCurrency(wallet.getLockedCurrency() + lockCurrency);
        } else if ("SELL".equals(event.getOrderType())) {
            int lockAmount = event.getAmount();
            wallet.setAvailableAmount(wallet.getAvailableAmount() - lockAmount);
            wallet.setLockedAmount(wallet.getLockedAmount() + lockAmount);
        }

        walletRepository.save(wallet);
        log.info("🔒 資產鎖定完成，用戶: {}", event.getUserId());
    }

    private void sendOrderFailedEvent(OrderCreateEvent originalEvent, String reason) {
        String failureType = reason.contains("餘額") ? "INSUFFICIENT_BALANCE" :
                           reason.contains("電量") ? "INSUFFICIENT_AMOUNT" : "WALLET_NOT_FOUND";

        OrderFailedEvent failedEvent = OrderFailedEvent.builder()
                .orderId(originalEvent.getOrderId())
                .userId(originalEvent.getUserId())
                .reason(reason)
                .failureType(failureType)
                .failedAt(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_FAILED_KEY, failedEvent);
        log.info("已發送訂單失敗通知: {} - {}", originalEvent.getOrderId(), reason);
    }
}
