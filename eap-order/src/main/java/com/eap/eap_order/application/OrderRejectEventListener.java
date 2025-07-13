package com.eap.eap_order.application;

import com.eap.eap_order.configuration.repository.OrderRepository;
import com.eap.eap_order.domain.entity.OrderEntity;
import com.eap.eap_order.domain.event.OrderRejectedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderRejectEventListener {

  @Autowired private OrderRepository orderRepository;

  @RabbitListener(queues = "order.rejected.queue")
  public void handleOrderRejected(OrderRejectedEvent event) {
    System.out.println("Received OrderRejectedEvent: " + event);

    orderRepository
        .findById(event.getOrderId().longValue())
        .ifPresent(
            order -> {
              order.setStatus(OrderEntity.OrderStatus.REJECTED);
              orderRepository.save(order);
            });
  }
}
