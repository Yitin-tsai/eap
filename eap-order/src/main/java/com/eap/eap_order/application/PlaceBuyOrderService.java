package com.eap.eap_order.application;

import com.eap.eap_order.configuration.repository.OrderRepository;
import com.eap.eap_order.controller.dto.req.PlaceBuyOrderReq;
import com.eap.eap_order.domain.entity.OrderEntity;
import com.eap.eap_order.domain.entity.OrderEntity.OrderType;
import com.eap.eap_order.domain.entity.event.OrderCreatedEvent;
import java.time.LocalDateTime;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

public class PlaceBuyOrderService {

  @Autowired private OrderRepository OrderRepository;
  @Autowired private RabbitTemplate rabbitTemplate;

  public void execute(PlaceBuyOrderReq request) {

    try {
      OrderEntity saved =  OrderRepository.save(
          OrderEntity.builder()
              .price(request.getBidPrice())
              .amount(request.getAmount())
              .userUuid(java.util.UUID.fromString(request.getBidder()))
              .updateTime(LocalDateTime.now())
              .type(OrderType.BUY)
              .build());

     OrderCreatedEvent event = OrderCreatedEvent.builder()
          .orderId(saved.getId().toString())
          .type(saved.getType().name())
          .price(saved.getPrice())
          .quantity(saved.getAmount())
          .userId(saved.getUserUuid().toString())
          .createdAt(saved.getUpdateTime())
          .build();

      // Publish the event to RabbitMQ
       rabbitTemplate.convertAndSend("order.exchange", "order.created", event);;
    } catch (Exception e) {
      // Handle exception, e.g., log it or rethrow it
      System.err.println("Error saving buy order: " + e.getMessage());
    }


  }
}
