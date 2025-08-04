package com.eap.eap_matchengine.application;

import com.eap.common.event.OrderCreatedEvent;
import com.eap.common.event.OrderMatchedEvent;
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
    boolean isBuy = incomingOrder.getType().equalsIgnoreCase("BUY");
    while (incomingOrder.getQuantity() > 0) {
      OrderCreatedEvent matchOrder = orderBookService.getAndRemoveBestMatchOrderLua(isBuy, incomingOrder.getPrice());
      if (matchOrder == null) {
        // 沒有可撮合對手單，將剩餘訂單加回 orderbook
        try {
          orderBookService.addOrder(incomingOrder);
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }
        break;
      }
      int matchedAmount = Math.min(incomingOrder.getQuantity(), matchOrder.getQuantity());
      incomingOrder.setQuantity(incomingOrder.getQuantity() - matchedAmount);
      matchOrder.setQuantity(matchOrder.getQuantity() - matchedAmount);
      OrderMatchedEvent matchedEvent = OrderMatchedEvent.builder()
          .buyerId(isBuy ? incomingOrder.getUserId() : matchOrder.getUserId())
          .sellerId(isBuy ? matchOrder.getUserId() : incomingOrder.getUserId())
          .price(matchOrder.getPrice())
          .amount(matchedAmount)
          .matchedAt(LocalDateTime.now())
          .orderType(incomingOrder.getType())
          .build();
      rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_MATCHED_KEY, matchedEvent);
      rabbitTemplate.convertAndSend(ORDER_EXCHANGE, WALLET_MATCHED_KEY, matchedEvent);
      if (matchOrder.getQuantity() > 0) {
        // 對手單部分成交，剩餘部分加回 orderbook
        try {
          orderBookService.addOrder(matchOrder);
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }
      } else {
        orderBookService.removeOrder(matchOrder);
      }
    }
  }
}
