package com.eap.common.event;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class OrderCreatedEvent {
  private UUID orderId;
  private String type; // BUY or SELL
  private Integer price;
  private Integer quantity;
  private UUID userId;
  private LocalDateTime createdAt;
}
