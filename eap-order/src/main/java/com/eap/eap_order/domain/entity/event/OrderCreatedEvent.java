package com.eap.eap_order.domain.entity.event;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;


@Data
@Builder

public class OrderCreatedEvent {
  private String orderId;
  private String type; // BUY or SELL
  private Integer price;
  private Integer quantity;
  private String userId;
  private LocalDateTime createdAt;
}
