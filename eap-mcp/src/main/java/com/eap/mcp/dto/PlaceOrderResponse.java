package com.eap.mcp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 下單回應 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderResponse {
    private String orderId;         // 訂單ID
    private String status;          // 訂單狀態
    private LocalDateTime acceptedAt; // 接受時間
}
