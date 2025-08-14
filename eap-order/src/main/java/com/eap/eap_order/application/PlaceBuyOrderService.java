package com.eap.eap_order.application;

import com.eap.common.event.OrderCreatedEvent;
import com.eap.eap_order.application.OutBound.EapWallet;
import com.eap.eap_order.controller.dto.req.PlaceBuyOrderReq;
import com.eap.eap_order.domain.entity.Order.OrderType;
import com.eap.common.event.OrderCreateEvent;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.eap.common.constants.RabbitMQConstants.*;

@Service
@Slf4j
public class PlaceBuyOrderService {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private EapWallet eapWallet;

    public void execute(PlaceBuyOrderReq request) {

        OrderCreatedEvent event =
                OrderCreatedEvent.builder()
                        .orderId(UUID.randomUUID())
                        .userId(request.getBidder())
                        .price(request.getBidPrice())
                        .quantity(request.getAmount())
                        .type(OrderType.BUY.name())
                        .createdAt(LocalDateTime.now())
                        .build();
        log.info("Creating buy order: {}", event);
        if (eapWallet.checkWallet(event)) {
            rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_CREATED_KEY, event);
            log.info("Buy order created and event published: {}", event);
        } else {
            log.error("wallet check failed for user: {}", request.getBidder());
        }
    }
}
