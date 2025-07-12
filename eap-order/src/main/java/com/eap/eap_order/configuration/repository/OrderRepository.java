package com.eap.eap_order.configuration.repository;

import com.eap.eap_order.domain.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
  // 可自訂查詢方法

}
