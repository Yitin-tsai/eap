package com.eap.mcp.controller;

import com.eap.mcp.service.McpToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MCP API 控制器
 * 提供標準的 Model Context Protocol 接口
 */
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@Slf4j
public class McpController {

    private final McpToolService mcpToolService;

    /**
     * 獲取 MCP 服務器資訊
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServerInfo() {
        Map<String, Object> info = Map.of(
            "name", "EAP Trading MCP Server",
            "version", "1.0.0",
            "description", "Model Context Protocol Server for EAP Electricity Trading Platform",
            "protocolVersion", "2024-11-05",
            "capabilities", Map.of(
                "tools", true,
                "resources", true,
                "prompts", true,
                "logging", true
            )
        );
        return ResponseEntity.ok(info);
    }

    /**
     * 獲取所有可用工具列表
     */
    @GetMapping("/tools")
    public ResponseEntity<Map<String, Object>> listTools() {
        List<Map<String, Object>> tools = mcpToolService.listTools();
        return ResponseEntity.ok(Map.of("tools", tools));
    }

    /**
     * 執行指定工具
     */
    @PostMapping("/tools/{toolName}/call")
    public ResponseEntity<Map<String, Object>> callTool(
            @PathVariable String toolName,
            @RequestBody Map<String, Object> request) {
        
        log.info("Tool call request: {} with body: {}", toolName, request);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) request.getOrDefault("arguments", Map.of());
        
        Object result = mcpToolService.executeTool(toolName, arguments);
        
        return ResponseEntity.ok(Map.of(
            "content", List.of(Map.of(
                "type", "text",
                "text", result.toString()
            )),
            "isError", false
        ));
    }

    /**
     * 獲取資源列表（未來實現）
     */
    @GetMapping("/resources")
    public ResponseEntity<Map<String, Object>> listResources() {
        return ResponseEntity.ok(Map.of(
            "resources", List.of(
                Map.of(
                    "uri", "docs://kpi-spec.md",
                    "name", "KPI Specification",
                    "description", "每個 KPI 的定義、單位和解讀說明",
                    "mimeType", "text/markdown"
                ),
                Map.of(
                    "uri", "docs://api-contract.md", 
                    "name", "API Contract",
                    "description", "order-service 端點說明文檔",
                    "mimeType", "text/markdown"
                )
            )
        ));
    }

    /**
     * 獲取提示模板列表（未來實現）
     */
    @GetMapping("/prompts")
    public ResponseEntity<Map<String, Object>> listPrompts() {
        return ResponseEntity.ok(Map.of(
            "prompts", List.of(
                Map.of(
                    "name", "analyze-scenario",
                    "description", "分析市場場景並生成交易報告",
                    "arguments", List.of(
                        Map.of("name", "symbol", "description", "交易標的", "required", true),
                        Map.of("name", "timeRange", "description", "時間範圍", "required", false)
                    )
                )
            )
        ));
    }

    /**
     * 健康檢查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "timestamp", System.currentTimeMillis(),
            "uptime", System.currentTimeMillis() // 簡化版本
        ));
    }
}
