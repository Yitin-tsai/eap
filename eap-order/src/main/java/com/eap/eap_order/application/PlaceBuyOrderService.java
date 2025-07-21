package com.eap.eap_order.application;

import com.eap.eap_order.controller.dto.req.PlaceBuyOrderReq;
import com.eap.eap_order.domain.entity.OrderEntity.OrderType;
import com.eap.eap_order.domain.event.OrderCreateEvent;

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

  @Autowired private RabbitTemplate rabbitTemplate;

  public void execute(PlaceBuyOrderReq request) {

    try {
     
      OrderCreateEvent event =
          OrderCreateEvent.builder()
              .orderId(UUID.randomUUID())
              .userId(request.getBidder())
              .price(request.getBidPrice())
              .amount(request.getAmount())
              .orderType(OrderType.BUY.name())
              .createdAt(LocalDateTime.now())
              .build();

      // Publish the event to RabbitMQ
      rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_CREATE_KEY, event);
      log.info("Buy order creat and event published: {}", event);
    } catch (Exception e) {
      
      System.err.println("Error saving buy order: " + e.getMessage());
    }
  }
}
