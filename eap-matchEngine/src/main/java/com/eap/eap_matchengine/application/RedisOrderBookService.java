package com.eap.eap_matchengine.application;

import com.eap.eap_matchengine.domain.event.OrderCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.Set;
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
    double score =
        event.getType().equalsIgnoreCase("BUY")
            ? -event.getPrice()
            : event.getPrice(); 

    String value = new ObjectMapper().writeValueAsString(event);
    redisTemplate.opsForZSet().add(key, value, score);
  }

  public Optional<OrderCreatedEvent> peekTopBuyOrder() {
    return getTopOrder(BUY_ORDERBOOK_KEY, true);
  }

  public Optional<OrderCreatedEvent> peekTopSellOrder() {
    return getTopOrder(SELL_ORDERBOOK_KEY, false);
  }

  private Optional<OrderCreatedEvent> getTopOrder(String key, boolean reverse) {
    Set<String> results =
        reverse
            ? redisTemplate.opsForZSet().reverseRange(key, 0, 0)
            : redisTemplate.opsForZSet().range(key, 0, 0);

    if (results == null || results.isEmpty()) return Optional.empty();

    try {
      return Optional.of(
          new ObjectMapper().readValue(results.iterator().next(), OrderCreatedEvent.class));
    } catch (Exception e) {
      e.printStackTrace();
      return Optional.empty();
    }
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
}
