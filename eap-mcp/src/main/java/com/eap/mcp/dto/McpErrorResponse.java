package com.eap.mcp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 統一的錯誤回應格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpErrorResponse {
    private String code;
    private String message;
    private Object details;
    
    public static McpErrorResponse of(String code, String message) {
        return McpErrorResponse.builder()
                .code(code)
                .message(message)
                .build();
    }
    
    public static McpErrorResponse of(String code, String message, Object details) {
        return McpErrorResponse.builder()
                .code(code)
                .message(message)
                .details(details)
                .build();
    }
}
