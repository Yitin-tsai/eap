package com.eap.eap_order.domain.entity;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder.In;

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
public class Order {

  private OrderType type; // 'BUY' or 'SELL'

  public enum OrderType {
    BUY,
    SELL
  }


}
