package com.eap.eap_order.application;

import com.eap.eap_order.controller.dto.req.PlaceSellOrderReq;
import com.eap.eap_order.domain.entity.OrderEntity.OrderType;
import com.eap.common.event.OrderCreateEvent;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import static com.eap.common.constants.RabbitMQConstants.*;

@Service
public class PlaceSellOrderService {

  @Autowired private RabbitTemplate rabbitTemplate;

  public void placeSellOrder(PlaceSellOrderReq req) {

    try {
  

      OrderCreateEvent event =
          OrderCreateEvent.builder()
              .orderId(UUID.randomUUID())
              .userId(req.getSeller())
              .price(req.getSellPrice())
              .amount(req.getAmount())
              .orderType(OrderType.SELL.name())
              .createdAt(LocalDateTime.now())
              .build();

      // Publish the event to RabbitMQ
      rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_CREATE_KEY, event);
    } catch (Exception e) {
      // Handle exception, e.g., log it or rethrow it
      System.err.println("Error saving sell order: " + e.getMessage());
    }
  }
}
