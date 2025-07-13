package com.eap.eap_order.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRejectedEvent {
  private Integer orderId;
  private UUID userId;
  private String reason; // e.g., "INSUFFICIENT_FUNDS"
  private LocalDateTime rejectedAt;
}
