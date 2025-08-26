package com.eap.eap_order.configuration.repository;

import com.eap.eap_order.domain.entity.MatchOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MathedOrderRepository extends JpaRepository<MatchOrderEntity, Long> {
    
    /**
     * 查詢用戶作為買方或賣方的所有成交記錄
     */
    @Query("SELECT m FROM MatchOrderEntity m WHERE m.buyerUuid = :userId OR m.sellerUuid = :userId ORDER BY m.updateTime DESC")
    List<MatchOrderEntity> findByUserUuid(@Param("userId") UUID userId);
}
