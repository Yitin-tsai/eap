package com.eap.eap_matchengine.application;

import com.eap.eap_matchengine.domain.event.OrderCreatedEvent;
import com.eap.eap_matchengine.domain.event.OrderMatchedEvent;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import java.util.Optional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchingEngineService {

  private final RedisOrderBookService orderBookService;
  private final RabbitTemplate rabbitTemplate;

  public void tryMatch() {
    Optional<OrderCreatedEvent> buyOpt = orderBookService.peekTopBuyOrder();
    Optional<OrderCreatedEvent> sellOpt = orderBookService.peekTopSellOrder();

    if (buyOpt.isEmpty() || sellOpt.isEmpty()) return;

    OrderCreatedEvent buy = buyOpt.get();
    OrderCreatedEvent sell = sellOpt.get();

    if (buy.getPrice() < sell.getPrice()) return; 

    int matchedAmount = Math.min(buy.getQuantity(), sell.getQuantity());
    int matchedPrice = sell.getPrice(); 

    OrderMatchedEvent matchedEvent =
        OrderMatchedEvent.builder()
            .buyerId(buy.getUserId())
            .sellerId(sell.getUserId())
            .amount(matchedAmount)
            .price(matchedPrice)
            .matchedAt(LocalDateTime.now())
            .build();

   
    rabbitTemplate.convertAndSend("order.exchange", "order.matched", matchedEvent);

    
    orderBookService.removeOrder(buy);
    orderBookService.removeOrder(sell);
  }
}
