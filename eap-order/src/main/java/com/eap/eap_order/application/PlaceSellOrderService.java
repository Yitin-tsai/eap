package com.eap.eap_order.application;

import com.eap.eap_order.configuration.repository.OrderRepository;
import com.eap.eap_order.controller.dto.req.PlaceSellOrderReq;
import com.eap.eap_order.domain.entity.OrderEntity;
import com.eap.eap_order.domain.entity.OrderEntity.OrderType;
import com.eap.eap_order.domain.event.OrderCreateEvent;
import java.time.LocalDateTime;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlaceSellOrderService {

  @Autowired private OrderRepository OrderRepository;
  @Autowired private RabbitTemplate rabbitTemplate;

  public void placeSellOrder(PlaceSellOrderReq req) {

    try {
      OrderEntity saved =
          OrderEntity.builder()
              .price(req.getSellPrice())
              .amount(req.getAmount())
              .type(OrderType.SELL)
              .userUuid(java.util.UUID.fromString(req.getSeller()))
              .updateTime(LocalDateTime.now())
              .build();
      OrderRepository.save(saved);

      OrderCreateEvent event =
          OrderCreateEvent.builder()
              .userId(saved.getUserUuid())
              .price(saved.getPrice())
              .amount(saved.getAmount())
              .orderType(saved.getType().name())
              .createdAt(LocalDateTime.now())
              .build();

      // Publish the event to RabbitMQ
      rabbitTemplate.convertAndSend("order.exchange", "order.creat", event);
      ;
    } catch (Exception e) {
      // Handle exception, e.g., log it or rethrow it
      System.err.println("Error saving sell order: " + e.getMessage());
    }
  }
}
