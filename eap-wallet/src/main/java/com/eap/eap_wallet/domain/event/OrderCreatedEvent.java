package com.eap.eap_wallet.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;


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
