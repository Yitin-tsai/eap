package com.eap.eap_wallet.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallets", schema = "wallet_service")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;
    
    @Column(name = "available_amount", nullable = false)
    @Builder.Default
    private Integer availableAmount = 0;
    
    @Column(name = "locked_amount", nullable = false)
    @Builder.Default
    private Integer lockedAmount = 0;
    
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @Column(name = "available_currency" , nullable = false)
    private Integer availableCurrency;

    @Column(name = "locked_currency", nullable = false)
    private Integer lockedCurrency;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updateTime = LocalDateTime.now();
    }
}
