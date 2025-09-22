package com.eap.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;


/**
 * AI 聊天服務：
 * - 透過本地 Ollama 模型理解問題
 * - 依照模型輸出的 JSON 指示呼叫 MCP 工具
 * - 將工具結果餵回模型生成最終回答
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

        private static final String SYSTEM_PROMPT = """
                    你是 EAP 的交易執行代理 (Tool-first Executor)。

                    目標：直接以機器可執行的 JSON 輸出對應的工具呼叫（actions 陣列），並且不得包含任何額外的文字、解釋或 Markdown。輸出必須為純粹的 JSON 物件 (application/json)，嚴格遵守下列契約。

                    嚴格契約（請務必遵守）：
                    1) 僅輸出一個 JSON 物件，不能有任何文字說明或 code fence（例如 ```）。
                    2) 請總是使用 "actions" (陣列) 包裝要執行的工具調用；即使只有一個 action 也要放在陣列內。
                    3) 欄位名稱大小寫必須精準：mode, plan, actions, action, arguments, final_answer, executeConfirmed。
                    4) 數值型參數 price 與 qty 必須以字串回傳，例如 "price":"10.0"、"qty":"100"。
                    5) 若檢查到格式錯誤或缺少必要欄位，回傳 {"final_answer":"錯誤: 說明"} 作為唯一輸出。
                    6) 不要回傳自然語言計畫（plan 說明可以存在於 plan 欄位但不要以人類語句包裝）；若無需執行任何工具，請回傳空的 actions: []。

                    支援的工具與精確 arguments：
                    - placeOrder -> arguments: {"userId":"string","side":"BUY|SELL","price":"string","qty":"string","symbol":"string"}
                    - registerUser -> arguments: {"userId":"string"}
                    - checkUserExists -> arguments: {"userId":"string"}
                    - getUserWallet -> arguments: {"userId":"string"}
                    - getUserOrders -> arguments: {"userId":"string"}
                    - cancelOrder -> arguments: {"orderId":"string"}
                    - getOrderBook -> arguments: {} or {"depth": number}
                    - getMarketMetrics -> arguments: {}

                    最小可執行範例（一個 action）：
                    {"mode":"execute","executeConfirmed":false,"plan":[],"actions":[{"action":"getOrderBook","arguments":{}}],"final_answer":""}

                    多步驟範例：
                    {
                        "mode":"execute",
                        "executeConfirmed":false,
                        "plan":[{"step":1,"name":"讀取訂單簿","tools":["getOrderBook"]}],
                        "actions":[
                            {"action":"getOrderBook","arguments":{}},
                            {"action":"placeOrder","arguments":{"userId":"550e8400-e29b-41d4-a716-446655440000","side":"BUY","price":"100","qty":"100","symbol":"ELC"}}
                        ],
                        "final_answer":"請確認是否要執行上述下單動作 (executeConfirmed=true 表示同意執行)。"
                    }

                    注意：
                    - 若想要系統僅模擬，請回傳 "mode":"simulate" 並在 actions 中包含要模擬的步驟；系統將不會實際執行 state-changing 工具。
                    - 若要實際執行 state-changing 工具 (如 placeOrder)，請將 "executeConfirmed":true 放在回傳的 JSON 中來表示你同意系統執行。若缺少該欄位或為 false，系統將只模擬或跳過真正會改變狀態的操作。
                    - 絕對不要輸出任何非 JSON 內容，否則後端會將其視為人類可讀回答並不會自動執行工具。
                    """;

    private final OllamaChatModel chatModel;
    private final McpToolClient mcpToolClient;
    private final ObjectMapper objectMapper;
    // set to true to enable verbose per-chunk streaming deltas (may be very noisy)
    private static final boolean LOG_STREAM_DELTAS = false;

    /**
     * 處理用戶聊天請求，必要時呼叫 MCP 工具。
     */
    public String chat(String userMessage) {
        try {
            log.info("收到用戶訊息: {}", userMessage);

            String initialPrompt = SYSTEM_PROMPT + "\n\n使用者提問：" + userMessage;
            log.info("[AI] prompt size={} chars", initialPrompt.length());
            StringBuilder buf = new StringBuilder();
            long t0 = System.currentTimeMillis();

            chatModel.stream(initialPrompt)
                .doOnSubscribe(s -> log.info("[AI] start streaming initial response..."))
                .doOnNext(chunk -> {
                    // Spring AI Ollama stream may emit String chunks; handle as-is
                    String text = chunk == null ? "" : chunk;
                    buf.append(text);
                    if (LOG_STREAM_DELTAS) {
                        log.debug("[AI][delta] {}", text);
                    }
                })
                .doOnError(e -> log.error("[AI] stream error (initial)", e))
                .doOnComplete(() -> log.info("[AI] initial stream complete, total={} chars", buf.length()))
                .blockLast();

            String modelResponse = buf.toString().trim();
            long t1 = System.currentTimeMillis();
            log.debug("模型初步回應(assembled) ({} ms): {}", (t1 - t0), modelResponse);

            // Try to parse as either a single-action JSON or a multi-step plan with actions[]
            ParsedPlan parsedPlan = extractPlanFromModelOutput(modelResponse);

            if (parsedPlan == null) {
                // Not a machine-actionable response; return final answer or raw text
                return extractFinalAnswer(modelResponse, null);
            }

            log.info("解析到模型計畫: mode={} actions={}", parsedPlan.mode, parsedPlan.actions == null ? 0 : parsedPlan.actions.size());

            // If simulate mode, do not call execute tools; instead call tools that are read-only to gather data for the final answer
            if ("simulate".equalsIgnoreCase(parsedPlan.mode)) {
                // execute all read-only tools (getOrderBook/getMarketMetrics/getUserWallet/getUserOrders) and embed results
                ObjectNode aggregated = objectMapper.createObjectNode();
                if (parsedPlan.actions != null) {
                    for (ObjectNode a : parsedPlan.actions) {
                        String act = a.path("action").asText();
                        ObjectNode args = (ObjectNode) a.path("arguments");
                        if (isReadOnlyTool(act)) {
                            JsonNode res = safeCallTool(act, args);
                            aggregated.set(act, res == null ? objectMapper.nullNode() : res);
                        }
                    }
                }

                // Ask the model to produce a final_answer based on simulated tool outputs
                String followUpPrompt = SYSTEM_PROMPT + "\n\n使用者提問：" + userMessage +
                    "\n以下為我模擬（simulate）執行後蒐集到的工具結果（JSON）：\n" + aggregated.toPrettyString() +
                    "\n要求：請仔細閱讀上方 JSON 格式的工具輸出，解析出重要的市場資訊（例如：最佳買賣價格 / 總買/賣量 / 是否存在明顯價格差距 / 建議的觀察或下單策略）。\n" +
                    "輸出格式限制：嚴格回傳一個 JSON 物件，格式為 {\"final_answer\":\"...\"}。final_answer 的內容請使用自然中文，簡潔明確地總結市場狀況，並且不要直接回傳或包裹原始 JSON（不要出現像 \"工具回傳結果\":{...} 這類字樣）。\n" +
                    "若要提出後續可執行的 action，請在 final_answer 中以自然語句建議，而不是在 JSON 的其他欄位輸出原始 actions。";

                String finalResponse = streamModelAndAssemble(followUpPrompt, "followup");
                return extractFinalAnswer(finalResponse, aggregated);
            }

            // mode == execute: we will attempt to execute actions[] sequentially but only for allowed tools
            ObjectNode executionResults = objectMapper.createObjectNode();
            if (parsedPlan.actions != null) {
                for (ObjectNode a : parsedPlan.actions) {
                    String act = a.path("action").asText();
                    ObjectNode args = (ObjectNode) a.path("arguments");

                    // Normalize and validate args (price/qty -> strings)
                    normalizeArguments(args);

                    if (!isAllowedTool(act)) {
                        executionResults.put(act, "ERROR: tool not allowed or unknown");
                        continue;
                    }

                    // For safety: skip state-changing ops unless explicitly execute-mode and client confirms
                    if (isStateChangingTool(act) && !parsedPlan.executeConfirmed) {
                        executionResults.put(act, "SKIPPED: requires explicit confirmation to execute");
                        continue;
                    }

                    JsonNode res = safeCallTool(act, args);
                    executionResults.set(act, res == null ? objectMapper.nullNode() : res);
                    log.info("工具 {} 呼叫完成 (結果大小={} chars)", act, res == null ? 0 : res.toString().length());
                }
            }

            // ask model to summarize based on execution results
            String followUpPrompt = SYSTEM_PROMPT + "\n\n使用者提問：" + userMessage +
                "\n已執行工具結果如下（JSON）：\n" + executionResults.toPrettyString() +
                "\n要求：請解析上方 JSON 工具輸出，並以簡潔的中文總結目前市場狀況（例如：最佳買賣價、總買賣量、是否有大量掛單、任何對交易者重要的異常或風險）。\n" +
                "輸出格式限制：嚴格回傳一個 JSON 物件，格式為 {\"final_answer\":\"...\"}。final_answer 請只包含中文自然語句；不要把原始 JSON 或工具回傳透過 \"工具回傳結果\" 再次輸出。\n" +
                "若你建議進一步執行的動作，可以在 final_answer 內以自然語句提出建議（例如：建議下多單、觀察價格至 X 等），但不要在其他欄位產生原始 action JSON。";

            String finalResponse = streamModelAndAssemble(followUpPrompt, "followup");
            return extractFinalAnswer(finalResponse, executionResults);

        } catch (Exception e) {
            log.error("處理聊天請求失敗", e);
            return "抱歉，處理您的請求時發生錯誤：" + e.getMessage();
        }
    }

    /**
     * 取得系統狀態摘要。
     */
    public String getSystemStatus() {
        boolean modelHealthy = isModelAvailable();
        boolean mcpHealthy = mcpToolClient.isHealthy();

        return String.format("""
            🤖 EAP AI 助手狀態

            🧠 模型: %s
            🕸️ MCP 服務: %s

            我可以協助：
            • 註冊用戶與查詢錢包
            • 送出/取消訂單、查詢撮合
            • 取得市場指標與訂單簿

            請輸入指令開始互動。
            """,
            modelHealthy ? "Llama (本地) ✅" : "模型不可用 ❌",
            mcpHealthy ? "連線正常" : "無法連線");
    }

    /**
     * 檢查 Ollama 模型是否可用。
     */
    public boolean isModelAvailable() {
        try {
            // use a short streaming check to detect availability without waiting for full sync call
            StringBuilder buf = new StringBuilder();
            chatModel.stream("ping")
                .doOnNext(chunk -> {
                    String text = chunk == null ? "" : chunk;
                    buf.append(text);
                })
                .blockLast();

            String testResponse = buf.toString();
            return testResponse != null && !testResponse.trim().isEmpty();
        } catch (Exception e) {
            log.warn("AI 模型不可用", e);
            return false;
        }
    }



    private String extractFinalAnswer(String modelOutput, JsonNode toolResultFallback) {
        // If the model returned a JSON object containing final_answer, return that.
        try {
            JsonNode root = objectMapper.readTree(modelOutput);
            JsonNode finalAnswer = root.path("final_answer");
            if (!finalAnswer.isMissingNode()) {
                return finalAnswer.asText();
            }
        } catch (JsonProcessingException e) {
            // ignore — we'll return raw modelOutput below
        }

        // Otherwise, return the raw model output exactly as received (no heuristics/fallbacks).
        return modelOutput == null ? "" : modelOutput;
    }


    /*
     * Extract a machine-actionable plan from the model output.
     * Handles cases where the model wraps JSON inside markdown code fences ```json ... ```
     */
    private ParsedPlan extractPlanFromModelOutput(String modelOutput) {
        if (modelOutput == null || modelOutput.isBlank()) return null;

        // try direct parse first
        try {
            JsonNode root = objectMapper.readTree(modelOutput);
            return parsedPlanFromJson(root);
        } catch (JsonProcessingException ignore) {
        }

        // try to extract code fence content (```json ... ``` or ``` ... ```)
        String extracted = extractFirstJsonBlock(modelOutput);
        if (extracted != null) {
            try {
                JsonNode root = objectMapper.readTree(extracted);
                return parsedPlanFromJson(root);
            } catch (JsonProcessingException e) {
                log.debug("提取的區塊仍非 JSON: {}", extracted);
            }
        }

        // try to find any {...} substring that is valid JSON
        int start = modelOutput.indexOf('{');
        while (start >= 0) {
            int end = modelOutput.lastIndexOf('}');
            if (end > start) {
                String candidate = modelOutput.substring(start, end + 1);
                try {
                    JsonNode root = objectMapper.readTree(candidate);
                    return parsedPlanFromJson(root);
                } catch (JsonProcessingException e) {
                    // try next occurrence
                }
            }
            start = modelOutput.indexOf('{', start + 1);
        }

        return null;
    }

    private ParsedPlan parsedPlanFromJson(JsonNode root) {
        if (root == null || root.isMissingNode()) return null;

        String mode = root.path("mode").asText("execute");
        boolean executeConfirmed = root.path("executeConfirmed").asBoolean(false);
        java.util.List<ObjectNode> actions = null;
        JsonNode actionsNode = root.path("actions");
        if (actionsNode.isArray()) {
            actions = new java.util.ArrayList<>();
            for (JsonNode n : actionsNode) {
                if (n.isObject()) {
                    actions.add((ObjectNode) n);
                }
            }
        }

        return new ParsedPlan(mode, executeConfirmed, actions);
    }

    private String extractFirstJsonBlock(String text) {
        if (text == null) return null;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("```(?:json)?\\s*(\\{.*?\\})\\s*```", java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find()) return m.group(1);
        return null;
    }

    private String streamModelAndAssemble(String prompt, String tag) {
        StringBuilder buf = new StringBuilder();
        chatModel.stream(prompt)
            .doOnSubscribe(s -> log.info("[AI] start streaming {} response...", tag))
            .doOnNext(chunk -> {
                String text = chunk == null ? "" : chunk;
                buf.append(text);
                if (LOG_STREAM_DELTAS) {
                    log.debug("[AI][delta][{}] {}", tag, text);
                }
            })
            .doOnError(e -> log.error("[AI] stream error ({})", tag, e))
            .doOnComplete(() -> log.info("[AI] {} stream complete, total={} chars", tag, buf.length()))
            .blockLast();

        return buf.toString().trim();
    }

    private void normalizeArguments(ObjectNode args) {
        if (args == null) return;
        // ensure price and qty are strings
        if (args.has("price") && !args.get("price").isTextual()) {
            args.put("price", args.get("price").asText());
        }
        if (args.has("qty") && !args.get("qty").isTextual()) {
            args.put("qty", args.get("qty").asText());
        }
        // also accept `quantity` alias and normalize to qty
        if (args.has("quantity") && !args.has("qty")) {
            args.put("qty", args.get("quantity").asText());
            args.remove("quantity");
        }
    }

    private boolean isReadOnlyTool(String name) {
        return "getOrderBook".equals(name) || "getMarketMetrics".equals(name) || "getUserWallet".equals(name) || "getUserOrders".equals(name) || "checkUserExists".equals(name);
    }

    private boolean isStateChangingTool(String name) {
        return "placeOrder".equals(name) || "cancelOrder".equals(name) || "registerUser".equals(name);
    }

    private boolean isAllowedTool(String name) {
        // whitelist tools
        return isReadOnlyTool(name) || isStateChangingTool(name) || "placeOrder".equals(name) || "registerUser".equals(name) || "cancelOrder".equals(name) || "checkUserExists".equals(name);
    }

    private JsonNode safeCallTool(String action, ObjectNode args) {
        try {
            // defensive: ensure args is non-null
            if (args == null) args = objectMapper.createObjectNode();
            // tool client may throw; catch and return an error node
            JsonNode res = mcpToolClient.callTool(action, args);
            return res == null ? objectMapper.nullNode() : res;
        } catch (Exception e) {
            log.error("呼叫工具 {} 失敗", action, e);
            ObjectNode err = objectMapper.createObjectNode();
            err.put("error", e.getMessage());
            return err;
        }
    }

    private static final class ParsedPlan {
        final String mode;
        final boolean executeConfirmed;
        final java.util.List<ObjectNode> actions;

        ParsedPlan(String mode, boolean executeConfirmed, java.util.List<ObjectNode> actions) {
            this.mode = mode == null ? "execute" : mode;
            this.executeConfirmed = executeConfirmed;
            this.actions = actions;
        }
    }
}
