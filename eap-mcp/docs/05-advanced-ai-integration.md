# 05 - 進階：AI（LLM）整合、Prompt 與安全考量

本文討論如何把 LLM 與 MCP 工具鏈結，並示範安全與可靠性的實作細節（prompt 設計、工具回傳的結構化策略）。

## 高階架構

- AI client（eap-ai-client）向 LLM (Ollama / OpenAI / 其他) 發出 prompt，模型回傳「工具規劃（actions）」或純文字結果。
- AI client 解析模型輸出，並根據解析的 action 呼叫 MCP 的工具（透過 `McpToolClient.callTool(toolName, args)`）。

專案檔案：
- `eap-ai-client/src/main/java/com/eap/ai/service/AiChatService.java`：負責 prompt 組裝、解析模型回覆、呼叫 MCP tools。

## Prompt 設計要點

- 優先要求模型回傳結構化 JSON（可直接執行）：
  - 範例格式：

```json
{"actions":[{"action":"getOrderBook","arguments":{"depth":20}}],"final_answer":""}
```

- 為了容錯，也可允許模型回傳純文字結果（例如查詢表格）；系統會優先嘗試 parse JSON，如果解析失敗就把原文回傳給使用者。

## 呼叫流程與保護機制

- 在呼叫任何會改變狀態的 tool（例如 `placeOrder`、`runSimulation`）前，執行最低限度的參數驗證（例如 userId, side, price, qty）。
- 若需要，在 AI client 層加入額外的安全檢查，例如：
  - 黑白名單限制可呼叫的 tool
  - 交易金額上限（防止模型下超大單）
  - 在測試環境允許 auto-register；在生產環境應關閉

## 日誌與審計

- 所有由模型產生並執行的 action 應該被紀錄（requestId、原始模型輸出、normalized args、MCP 回傳），以利事後稽核。
- 建議在 `AiChatService` 中持久化一份操作事件或把記錄流送到外部的審計服務。

## 常見挑戰與建議

- 模型不遵守輸出規範：採取容錯的解析策略（JSON、code-fence、brace matching）。若仍失敗，把原文回傳並記錄以便 prompt 調整。
- ChatClient（Ollama）自動配置問題：避免同時提供自定義 `OllamaApi` bean 與期望的 auto-config，必要時提供一個 adapter `ChatClient` bean。
- 測試策略：對 AI client 做端到端的模擬測試，並使用 deterministic seed 減少 LLM 回覆波動。

---

結語

以上五篇從基礎到進階，涵蓋 `eap-mcp` 的建置、Tool 實作、測試與 AI 整合。你可以直接把這些 .md 發佈到技術部落格或比賽投稿，若要我為每篇補上更多實作程式碼範例或圖片/流程圖，我可以繼續補強。  