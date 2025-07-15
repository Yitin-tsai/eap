package com.eap.eap_wallet.domain.event;


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
public class OrderCreateEvent  {
  private UUID orderId;
  private UUID userId;
  private Integer price;
  private Integer amount;
  private String orderType; 
  private LocalDateTime createdAt;
}
