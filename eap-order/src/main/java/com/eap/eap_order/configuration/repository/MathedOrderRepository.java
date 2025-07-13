package com.eap.eap_order.configuration.repository;

import com.eap.eap_order.domain.entity.MatchOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MathedOrderRepository extends JpaRepository<MatchOrderEntity, Long> {}
