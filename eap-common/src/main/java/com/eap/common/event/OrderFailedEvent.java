package com.eap.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderFailedEvent {
    private UUID orderId;
    private UUID userId;
    private String reason;
    private String failureType; // INSUFFICIENT_BALANCE, INSUFFICIENT_AMOUNT, WALLET_NOT_FOUND, etc.

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime failedAt;
}
