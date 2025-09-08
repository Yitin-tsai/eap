package com.eap.mcp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP Tool Response DTO
 * 用於封裝 MCP 工具調用的響應結果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpToolResponse {
    
    private boolean success;
    private String message;
    private Object data;
    private String error;
    
    public static McpToolResponse success(String message, Object data) {
        return new McpToolResponse(true, message, data, null);
    }
    
    public static McpToolResponse success(Object data) {
        return new McpToolResponse(true, "操作成功", data, null);
    }
    
    public static McpToolResponse error(String error) {
        return new McpToolResponse(false, null, null, error);
    }
    
    public static McpToolResponse error(String message, String error) {
        return new McpToolResponse(false, message, null, error);
    }
}
