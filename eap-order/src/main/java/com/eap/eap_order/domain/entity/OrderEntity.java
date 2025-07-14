package com.eap.eap_order.domain.entity;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder.In;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders", schema = "order_service")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 10)
  private OrderType type; // 'BUY' or 'SELL'

  @Column(name = "price", nullable = false)
  private Integer price;

  @Column(name = "amount", nullable = false)
  private Integer amount;

  @Column(name = "user_uuid", nullable = false)
  private UUID userUuid;

  @Column(name = "update_time", nullable = false)
  private LocalDateTime updateTime;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private OrderStatus status;

  public enum OrderType {
    BUY,
    SELL
  }

  public enum OrderStatus {
    PENDING_ASSET_CHECK,
    CONFIRMED,
    REJECTED
  }
}
