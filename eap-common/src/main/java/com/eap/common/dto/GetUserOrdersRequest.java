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
public class GetUserOrdersRequest {
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("status")
    private String status; // pending, matched, cancelled, all
    
    @JsonProperty("limit")
    @Builder.Default
    private Integer limit = 50;
    
    @JsonProperty("offset")
    @Builder.Default
    private Integer offset = 0;
    
    // 便利方法
    public boolean isPendingOnly() {
        return "pending".equalsIgnoreCase(status);
    }
    
    public boolean isMatchedOnly() {
        return "matched".equalsIgnoreCase(status);
    }
    
    public boolean isCancelledOnly() {
        return "cancelled".equalsIgnoreCase(status);
    }
    
    public boolean isAll() {
        return status == null || "all".equalsIgnoreCase(status);
    }
    
    // 驗證方法
    public boolean isValid() {
        return userId != null && !userId.trim().isEmpty() &&
               limit != null && limit > 0 && limit <= 100 &&
               offset != null && offset >= 0;
    }
    
    public String getValidationError() {
        if (userId == null || userId.trim().isEmpty()) {
            return "用戶ID不能為空";
        }
        if (limit == null || limit <= 0 || limit > 100) {
            return "查詢數量必須在1-100之間";
        }
        if (offset == null || offset < 0) {
            return "偏移量不能為負數";
        }
        return null;
    }
}
