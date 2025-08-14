package com.eap.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderMatchedEvent {
  private UUID buyerId;
  private UUID sellerId;
  private int price;
  private int amount;
  private Integer matchId;
  private LocalDateTime matchedAt;
  private String orderType; // BUY or SELL
}

