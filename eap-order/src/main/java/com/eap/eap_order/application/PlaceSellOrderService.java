package com.eap.eap_order.application;

import com.eap.common.event.OrderCreatedEvent;
import com.eap.eap_order.application.OutBound.EapWallet;
import com.eap.eap_order.controller.dto.req.PlaceSellOrderReq;
import com.eap.eap_order.domain.entity.OrderEntity.OrderType;
import com.eap.common.event.OrderCreateEvent;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.eap.common.constants.RabbitMQConstants.*;

@Service
@Slf4j
public class PlaceSellOrderService {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    EapWallet eapWallet;

    public void placeSellOrder(PlaceSellOrderReq request) {

        OrderCreatedEvent event =
                OrderCreatedEvent.builder()
                        .orderId(UUID.randomUUID())
                        .userId(request.getSeller())
                        .price(request.getSellPrice())
                        .quantity(request.getAmount())
                        .type(OrderType.SELL.name())
                        .createdAt(LocalDateTime.now())
                        .build();
        log.info("Creating sell order: {}", event);
        if (eapWallet.checkWallet(event)) {
            {
                rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_CREATED_KEY, event);
                log.info("Buy order creat and event published: {}", event);
            }
            log.error("wallet check failed for user: {}", request.getSeller());

        }
    }
}
