package com.eap.eap_matchengine.application;

import com.eap.eap_matchengine.domain.event.OrderCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis-based implementation of an order book service for managing buy and sell orders.
 * Uses Redis Sorted Sets (ZSet) to maintain order books with price-based sorting.
 * Provides functionality for adding, removing, and matching orders.
 */
@Service
@RequiredArgsConstructor
public class RedisOrderBookService {

  private final String BUY_ORDERBOOK_KEY = "orderbook:buy";
  private final String SELL_ORDERBOOK_KEY = "orderbook:sell";
  private final RedisTemplate<String, String> redisTemplate;

  /**
   * Adds a new order to the appropriate order book (buy/sell).
   * Orders are stored in Redis ZSet with price as the score for sorting.
   *
   * @param event The order event to be added
   * @throws JsonProcessingException if the order cannot be serialized to JSON
   */
  public void addOrder(OrderCreatedEvent event) throws JsonProcessingException {
    String key = event.getType().equalsIgnoreCase("BUY") ? BUY_ORDERBOOK_KEY : SELL_ORDERBOOK_KEY;

    String value = new ObjectMapper().writeValueAsString(event);
    redisTemplate.opsForZSet().add(key, value, event.getPrice());
  }

  /**
   * Removes an order from its corresponding order book.
   * 
   * @param event The order event to be removed
   */
  public void removeOrder(OrderCreatedEvent event) {
    String key = event.getType().equalsIgnoreCase("BUY") ? BUY_ORDERBOOK_KEY : SELL_ORDERBOOK_KEY;
    try {
      String value = new ObjectMapper().writeValueAsString(event);
      redisTemplate.opsForZSet().remove(key, value);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Retrieves matchable orders for an incoming order based on price matching rules:
   * - For buy orders: finds sell orders with prices less than or equal to the buy price
   * - For sell orders: finds buy orders with prices greater than or equal to the sell price
   *
   * @param incomingOrder The order to find matches for
   * @return List of matching orders sorted by best price (lowest for sells, highest for buys)
   */
  public List<OrderCreatedEvent> getMatchableOrders(OrderCreatedEvent incomingOrder) {
    boolean isBuy = incomingOrder.getType().equalsIgnoreCase("BUY");
    String oppositeKey = isBuy ? SELL_ORDERBOOK_KEY : BUY_ORDERBOOK_KEY;

   
    Set<String> results;
    if (isBuy) {
      
      results = redisTemplate.opsForZSet().rangeByScore(oppositeKey, 0, incomingOrder.getPrice());
    } else {
      
      results = redisTemplate.opsForZSet().reverseRangeByScore(oppositeKey, incomingOrder.getPrice(), Double.POSITIVE_INFINITY);
    }

    if (results == null || results.isEmpty()) {
      return List.of();
    }

    try {
      ObjectMapper mapper = new ObjectMapper();
      return results.stream()
          .map(str -> {
            try {
              return mapper.readValue(str, OrderCreatedEvent.class);
            } catch (Exception e) {
              e.printStackTrace();
              return null;
            }
          })
          .filter(event -> event != null)
          .collect(Collectors.toList());
    } catch (Exception e) {
      e.printStackTrace();
      return List.of();
    }
  }

}
