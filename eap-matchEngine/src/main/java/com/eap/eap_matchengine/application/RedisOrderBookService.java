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

@Service
@RequiredArgsConstructor
public class RedisOrderBookService {

  private final String BUY_ORDERBOOK_KEY = "orderbook:buy";
  private final String SELL_ORDERBOOK_KEY = "orderbook:sell";

  private final RedisTemplate<String, String> redisTemplate;

  public void addOrder(OrderCreatedEvent event) throws JsonProcessingException {
    String key = event.getType().equalsIgnoreCase("BUY") ? BUY_ORDERBOOK_KEY : SELL_ORDERBOOK_KEY;

    String value = new ObjectMapper().writeValueAsString(event);
    redisTemplate.opsForZSet().add(key, value, event.getPrice());
  }

  public void removeOrder(OrderCreatedEvent event) {
    String key = event.getType().equalsIgnoreCase("BUY") ? BUY_ORDERBOOK_KEY : SELL_ORDERBOOK_KEY;
    try {
      String value = new ObjectMapper().writeValueAsString(event);
      redisTemplate.opsForZSet().remove(key, value);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public List<OrderCreatedEvent> getMatchableOrders(OrderCreatedEvent incomingOrder) {
    boolean isBuy = incomingOrder.getType().equalsIgnoreCase("BUY");
    String oppositeKey = isBuy ? SELL_ORDERBOOK_KEY : BUY_ORDERBOOK_KEY;

    // 使用 Redis ZSet 的 score 範圍查詢來獲取匹配的訂單
    Set<String> results;
    if (isBuy) {
      // 買單：獲取所有價格小於等於買價的賣單，按價格從低到高排序
      results = redisTemplate.opsForZSet().rangeByScore(oppositeKey, 0, incomingOrder.getPrice());
    } else {
      // 賣單：獲取所有價格大於等於賣價的買單，按價格從高到低排序
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
