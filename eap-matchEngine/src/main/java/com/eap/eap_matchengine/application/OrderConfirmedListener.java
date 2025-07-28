package com.eap.eap_matchengine.application;


import com.eap.common.event.OrderCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderConfirmedListener {

  private final MatchingEngineService matchingEngineService;

  @RabbitListener(queues = "order.created.queue")
  public void handleConfirmedOrder(OrderCreatedEvent event) throws JsonProcessingException {
    System.out.println("Confirmed order received: " + event);

    matchingEngineService.tryMatch(event);
  }
}
