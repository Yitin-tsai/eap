package com.eap.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelOrderRequest {
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("reason")
    private String reason;
    
    // 驗證方法
    public boolean isValid() {
        return orderId != null && !orderId.trim().isEmpty();
    }
    
    public String getValidationError() {
        if (orderId == null || orderId.trim().isEmpty()) {
            return "訂單ID不能為空";
        }
        return null;
    }
}
