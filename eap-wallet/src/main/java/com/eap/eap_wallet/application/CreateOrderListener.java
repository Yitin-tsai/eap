package com.eap.eap_wallet.application;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.eap.eap_wallet.configuration.ReturnException;
import com.eap.eap_wallet.configuration.repository.WalletRepository;
import com.eap.eap_wallet.domain.entity.WalletEntity;
import com.eap.common.event.OrderCreateEvent;
import com.eap.common.event.OrderCreatedEvent;
import static com.eap.common.constants.RabbitMQConstants.*;

import lombok.extern.slf4j.Slf4j;

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
            throw new ReturnException("訂單金額超過可用餘額: " + event.getUserId());
        }

        if(!isWalletEnoughForSell(event)) {
            log.warn("訂單可用電量不足: " + event.getUserId());
            throw new ReturnException("訂單可用電量不足: " + event.getUserId());
        }

        lockAsset(event);

        OrderCreatedEvent orderCreatedEvent = OrderCreatedEvent.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .price(event.getPrice())
                .quantity(event.getAmount())
                .type(event.getOrderType())
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

    private boolean isWalletEnoughForSell(OrderCreateEvent event) {

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
}
