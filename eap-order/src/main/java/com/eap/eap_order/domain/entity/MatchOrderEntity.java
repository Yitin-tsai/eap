package com.eap.eap_order.domain.entity;

import jakarta.persistence.*;
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
@Entity
@Table(name = "match_history", schema = "order_service")
public class MatchOrderEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Integer price;

  @Column(nullable = false)
  private Integer amount;

  @Column(name = "buyer_uuid", nullable = false)
  private UUID buyerUuid;

  @Column(name = "seller_uuid", nullable = false)
  private UUID sellerUuid;

  @Column(name = "update_time", nullable = false)
  private LocalDateTime updateTime;
}
