package com.eap.eap_matchengine.application;

import com.eap.eap_matchengine.domain.event.OrderCreatedEvent;
import com.eap.eap_matchengine.domain.event.OrderMatchedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.eap.common.constants.RabbitMQConstants.*;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Service responsible for matching buy and sell orders in the trading system.
 * Implements the order matching logic and manages the order lifecycle through the order book.
 */
@Service
@RequiredArgsConstructor
public class MatchingEngineService {

  private final RedisOrderBookService orderBookService;
  private final RabbitTemplate rabbitTemplate;

  /**
   * Attempts to match an incoming order with existing orders in the order book.
   * The matching process follows these steps:
   * 1. Checks for matching orders in the opposite order book
   * 2. If no matches found, adds the order to the appropriate order book
   * 3. If matches found, processes them in order:
   *    - Matches the maximum possible quantity
   *    - Updates the quantities of both orders
   *    - Creates and publishes a matched event
   *    - Removes fully matched orders
   *    - Adds remaining quantity back to order book if partially matched
   *
   * @param incomingOrder The new order to be matched
   */
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

      rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_MATCHED_KEY, matchedEvent);
      rabbitTemplate.convertAndSend(ORDER_EXCHANGE, WALLET_MATCHED_KEY, matchedEvent);
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
