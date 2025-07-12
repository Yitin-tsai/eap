package com.eap.eap_order.application;


import com.eap.eap_order.controller.dto.req.PlaceSellOrderReq;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.eap.eap_order.configuration.repository.OrderRepository;
import com.eap.eap_order.domain.entity.OrderEntity;
import com.eap.eap_order.domain.entity.OrderEntity.OrderType;


@Service
public class PlaceSellOrderService {

  @Autowired private OrderRepository OrderRepository;

  public void placeSellOrder(PlaceSellOrderReq req) {
    try {
      OrderRepository.save(
          OrderEntity.builder()
              .price(req.getSellPrice())
              .amount(req.getAmount())
              .type(OrderType.SELL)
              .userUuid(java.util.UUID.fromString(req.getSeller()))
              .updateTime(LocalDateTime.now())
              .build());
    } catch (Exception e) {
      // Handle exception, e.g., log it or rethrow it
      System.err.println("Error saving sell order: " + e.getMessage());
    }
  }
}
