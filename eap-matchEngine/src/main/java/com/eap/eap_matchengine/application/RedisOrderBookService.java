package com.eap.eap_matchengine.application;

import com.eap.common.event.OrderCancelEvent;
import com.eap.common.event.OrderCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
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
    private final ObjectMapper objectMapper; // 注入Spring配置的ObjectMapper

    /**
     * Adds a new order to the appropriate order book (buy/sell).
     * Orders are stored in Redis ZSet with price as the score for sorting.
     *
     * @param event The order event to be added
     * @throws JsonProcessingException if the order cannot be serialized to JSON
     */
    public void addOrder(OrderCreatedEvent event) throws JsonProcessingException {
        String key = event.getType().equalsIgnoreCase("BUY") ? BUY_ORDERBOOK_KEY : SELL_ORDERBOOK_KEY;
        // 1. 存入 orderbook ZSet，value 為 orderId
        redisTemplate.opsForZSet().add(key, event.getOrderId().toString(), event.getPrice());
        // 2. 存入 orderId 對應內容
        String orderIdKey = "order:" + event.getOrderId();
        String orderJson = objectMapper.writeValueAsString(event);
        redisTemplate.opsForValue().set(orderIdKey, orderJson);
        // 3. 存入 user 對應的 set
        String userOrdersKey = "user:" + event.getUserId() + ":orders";
        redisTemplate.opsForSet().add(userOrdersKey, event.getOrderId().toString());
    }

    /**
     * Removes an order from its corresponding order book.
     *
     * @param event The order event to be removed
     */
    public void removeOrder(OrderCreatedEvent event) {
        String key = event.getType().equalsIgnoreCase("BUY") ? BUY_ORDERBOOK_KEY : SELL_ORDERBOOK_KEY;
        redisTemplate.opsForZSet().remove(key, event.getOrderId().toString());
        String orderIdKey = "order:" + event.getOrderId();
        redisTemplate.delete(orderIdKey);
        String userOrdersKey = "user:" + event.getUserId() + ":orders";
        redisTemplate.opsForSet().remove(userOrdersKey, event.getOrderId().toString());
    }


    public boolean cancelOrder(OrderCancelEvent event) {
        String orderIdKey = "order:" + event.getOrderId();
        String orderJson = redisTemplate.opsForValue().get(orderIdKey);
        if (orderJson != null) {
            try {
                OrderCreatedEvent order = objectMapper.readValue(orderJson, OrderCreatedEvent.class);
                String bookKey = order.getType().equalsIgnoreCase("BUY") ? BUY_ORDERBOOK_KEY : SELL_ORDERBOOK_KEY;
                // 從 ZSet 中移除 orderId
                boolean removed = redisTemplate.opsForZSet().remove(bookKey, event.getOrderId().toString()) > 0;
                // 從 ID 映射中移除
                if (removed) {
                    redisTemplate.delete(orderIdKey);
                    String userOrdersKey = "user:" + order.getUserId() + ":orders";
                    redisTemplate.opsForSet().remove(userOrdersKey, event.getOrderId().toString());
                }
                return removed;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public List<OrderCreatedEvent> getOrderByUserId(UUID userId) {
        String userOrdersKey = "user:" + userId + ":orders";
        Set<String> orderIds = redisTemplate.opsForSet().members(userOrdersKey);
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }
        return orderIds.stream()
                .map(orderId -> {
                    String orderJson = redisTemplate.opsForValue().get("order:" + orderId);
                    if (orderJson != null) {
                        try {
                            return objectMapper.readValue(orderJson, OrderCreatedEvent.class);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return null;
                })
                .filter(o -> o != null)
                .collect(Collectors.toList());
    }

    /**
     * 使用 Lua script 原子性取得並移除一筆最優先的對手單 orderId
     *
     * @param isBuy 是否為買單
     * @param price 撮合價格
     * @return 對手單的 OrderCreatedEvent，若無則回傳 null
     */
    public OrderCreatedEvent getAndRemoveBestMatchOrderLua(boolean isBuy, int price) {
        String zsetKey = isBuy ? SELL_ORDERBOOK_KEY : BUY_ORDERBOOK_KEY;

        final String buyLua =
                "local r = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1], 'LIMIT', 0, 1); " +
        "if #r > 0 then redis.call('ZREM', KEYS[1], r[1]); return r[1]; else return nil; end";

        final String sellLua =
                "local r = redis.call('ZREVRANGEBYSCORE', KEYS[1], '+inf', ARGV[1], 'LIMIT', 0, 1); " +
        "if #r > 0 then redis.call('ZREM', KEYS[1], r[1]); return r[1]; else return nil; end";




        final String lua = isBuy ? buyLua : sellLua;
        String priceArg = Integer.toString(price);

        String orderId = redisTemplate.execute((RedisCallback<String>) (connection) -> {
            Object res = connection.eval(
                    lua.getBytes(),
                    ReturnType.VALUE,
                    1,
                    zsetKey.getBytes(),
                    priceArg.getBytes()
            );
            return res != null ? new String((byte[]) res) : null;
        });
        if (orderId == null) return null;
        String orderJson = redisTemplate.opsForValue().get("order:" + orderId);
        if (orderJson == null) return null;
        try {
            return objectMapper.readValue(orderJson, OrderCreatedEvent.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
            return results.stream()
                    .map(str -> {
                        try {
                            return objectMapper.readValue(str, OrderCreatedEvent.class);
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
