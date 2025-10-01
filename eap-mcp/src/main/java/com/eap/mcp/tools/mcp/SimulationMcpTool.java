package com.eap.mcp.tools.mcp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.eap.mcp.simulation.SimulationRequest;
import com.eap.mcp.simulation.SimulationResult;
import com.eap.mcp.simulation.SimulationService;

/**
 * MCP 工具：模擬服務
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SimulationMcpTool {

    private final SimulationService simulationService;

    @Tool(name = "runSimulation", description = "執行交易模擬；若 executeReal=true 則會呼叫實際下單")
    public SimulationResult runSimulation(
            @ToolParam(description = "JSON 格式的模擬請求 (SimulationRequest)", required = true) SimulationRequest req
    ) {
        log.info("runSimulation: {}", req);
        return simulationService.runSimulation(req);
    }

    @Tool(name = "exportReport", description = "匯出最近一次模擬的報表 (目前會直接回傳 SimulationResult)")
    public SimulationResult exportReport(@ToolParam(description = "Simulation id / placeholder", required = false) String id) {
        // Return the most recent simulation result if available
        SimulationResult last = simulationService.getLastSimulationResult();
        if (last != null) {
            return last;
        }
        SimulationResult r = new SimulationResult();
        r.setSteps(0);
        r.setSymbol("N/A");
        r.getEvents().add("no simulation run yet");
        return r;
    }
}
