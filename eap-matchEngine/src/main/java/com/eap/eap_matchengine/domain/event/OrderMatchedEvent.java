package com.eap.eap_matchengine.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderMatchedEvent {
  private UUID buyerId;
  private UUID sellerId;
  private int price;
  private int amount;
  private Integer matchId;
  private LocalDateTime matchedAt;
}
