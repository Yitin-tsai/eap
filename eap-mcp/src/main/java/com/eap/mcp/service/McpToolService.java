package com.eap.mcp.service;

import com.eap.mcp.tools.GetOrderBookTool;
import com.eap.mcp.tools.GetTradesTool;
import com.eap.mcp.tools.GetMetricsTool;
import com.eap.mcp.tools.PlaceOrderTool;
import com.eap.mcp.tools.CancelOrderTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 工具管理服務
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class McpToolService {

    private final GetOrderBookTool getOrderBookTool;
    private final GetTradesTool getTradesTool;
    private final GetMetricsTool getMetricsTool;
    private final PlaceOrderTool placeOrderTool;
    private final CancelOrderTool cancelOrderTool;

    /**
     * 獲取所有可用工具的列表
     */
    public List<Map<String, Object>> listTools() {
        return List.of(
            createToolInfo(getOrderBookTool.getName(), getOrderBookTool.getDescription(), getOrderBookTool.getSchema()),
            createToolInfo(getTradesTool.getName(), getTradesTool.getDescription(), getTradesTool.getSchema()),
            createToolInfo(getMetricsTool.getName(), getMetricsTool.getDescription(), getMetricsTool.getSchema()),
            createToolInfo(placeOrderTool.getName(), placeOrderTool.getDescription(), placeOrderTool.getSchema()),
            createToolInfo(cancelOrderTool.getName(), cancelOrderTool.getDescription(), cancelOrderTool.getSchema())
        );
    }

    /**
     * 執行指定的工具
     */
    public Object executeTool(String toolName, Map<String, Object> parameters) {
        log.info("Executing tool: {} with parameters: {}", toolName, parameters);
        
        try {
            switch (toolName) {
                case "getOrderBook":
                    return getOrderBookTool.execute(parameters);
                case "getUserOrders":
                    return getTradesTool.execute(parameters);
                case "getMetrics":
                    return getMetricsTool.execute(parameters);
                case "placeOrder":
                    return placeOrderTool.execute(parameters);
                case "cancelOrder":
                    return cancelOrderTool.execute(parameters);
                default:
                    return createErrorResponse("TOOL_NOT_FOUND", "Unknown tool: " + toolName);
            }
        } catch (Exception e) {
            log.error("Error executing tool: " + toolName, e);
            return createErrorResponse("EXECUTION_ERROR", "Error executing tool: " + e.getMessage());
        }
    }

    private Map<String, Object> createToolInfo(String name, String description, Map<String, Object> schema) {
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", name);
        tool.put("description", description);
        tool.put("inputSchema", schema);
        return tool;
    }

    private Map<String, Object> createErrorResponse(String code, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}
