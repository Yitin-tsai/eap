package com.eap.eap_wallet.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 測試用的 WalletEntity，不使用 schema
 */
@Entity
@Table(name = "wallets")  // 移除 schema 配置，供測試使用
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestWalletEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;
    
    @Column(name = "available_amount", nullable = false)
    private Integer availableAmount;
    
    @Column(name = "locked_amount", nullable = false)
    private Integer lockedAmount;
    
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
    
    @PrePersist
    protected void onCreate() {
        updateTime = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
