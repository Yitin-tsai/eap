package com.eap.eap_matchengine.application;

import com.eap.eap_matchengine.domain.event.OrderCreatedEvent;
import com.eap.eap_matchengine.domain.event.OrderMatchedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchingEngineService {

  private final RedisOrderBookService orderBookService;

  private final RabbitTemplate rabbitTemplate;

  public void tryMatch(OrderCreatedEvent incomingOrder) {
    List<OrderCreatedEvent> matchableOrders = orderBookService.getMatchableOrders(incomingOrder);

    if (matchableOrders.isEmpty()) {
      try {
        orderBookService.addOrder(incomingOrder);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
      return;
    }

    for (OrderCreatedEvent matchOrder : matchableOrders) {

      if (incomingOrder.getQuantity() == 0) {
        break;
      }

      int matchedAmount = Math.min(incomingOrder.getQuantity(), matchOrder.getQuantity());

      incomingOrder.setQuantity(incomingOrder.getQuantity() - matchedAmount);
      matchOrder.setQuantity(matchOrder.getQuantity() - matchedAmount);

      OrderMatchedEvent matchedEvent = OrderMatchedEvent.builder()
          .buyerId(incomingOrder.getType().equalsIgnoreCase("BUY") ? incomingOrder.getUserId() : matchOrder.getUserId())
          .sellerId(
              incomingOrder.getType().equalsIgnoreCase("SELL") ? incomingOrder.getUserId() : matchOrder.getUserId())
          .price(matchOrder.getPrice())
          .amount(matchedAmount)
          .matchedAt(LocalDateTime.now())
          .orderType(incomingOrder.getType())
          .build();

      rabbitTemplate.convertAndSend("order.exchange", "order.matched", matchedEvent);

      if (matchOrder.getQuantity() == 0) {
        orderBookService.removeOrder(matchOrder);
      }
    }

    if (incomingOrder.getQuantity() > 0) {
      try {
        orderBookService.addOrder(incomingOrder);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    }
  }
}
