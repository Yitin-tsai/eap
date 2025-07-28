package com.eap.eap_order.application;

import com.eap.eap_order.configuration.repository.MathedOrderRepository;
import com.eap.eap_order.domain.entity.MatchOrderEntity;
import com.eap.common.event.OrderMatchedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import static com.eap.common.constants.RabbitMQConstants.*;

@Component
public class MatchEventListener {

  @Autowired private MathedOrderRepository matchOrderRepository;

  @RabbitListener(queues = ORDER_MATCHED_QUEUE)
  public void handleOrderMatched(OrderMatchedEvent event) {
    System.out.println("Received OrderMatchedEvent: " + event);

    MatchOrderEntity matchOrder =
        MatchOrderEntity.builder()
            .buyerUuid(event.getBuyerId())
            .sellerUuid(event.getSellerId())
            .price(event.getPrice())
            .amount(event.getAmount())
            .updateTime(event.getMatchedAt())
            .build();

    matchOrderRepository.save(matchOrder);
  }
}
