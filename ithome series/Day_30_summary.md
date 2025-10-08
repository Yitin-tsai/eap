# Day 30｜總結與感謝：eap-ai-client × eap-mcp 系列，我如何把 AI 變成可控的執行力

這 29 天，我把「能聽懂人話」的 LLM，和「一定做對事」的 MCP 工具層接起來。透過 `eap-ai-client`（LLM 編排器）與 `eap-mcp`（工具提供者），我讓對話不只產生答案，還能在執行上可控、可測、可擴充。

## TL;DR

- 我用 **LLM 做規劃、MCP 做執行**，把「理解」與「落地」拆開；`AiChatService` 站在中間做治理與風控。
- 要求模型優先輸出 **JSON 計畫（Plan）** → 嚴格解析 → 參數正規化與必要欄位檢查 → 才放行到 MCP 工具。
- 成果：從「可以對話」→「可以執行」→「執行可控可測」。後續我會把觀測、風控與成本延遲治理做深。


## 全局流程（純文字版，所有 Markdown 可讀）

```text
使用者/CLI -> AiChatController -> AiChatService
                   |                 |
                   |                 +--> ChatClient/ChatModel
                   |                       (模型回覆：JSON 計畫 / 純文字)
                   |                 |
                   |                 +--> parsePlanStrict + execution gate
                   |                         - 正規化：price/qty 去千分位、side/symbol 大寫
                   |                         - 必要欄位檢查／使用者存在性檢查與自動註冊
                   |                 |
                   |                 +--> McpToolClient -> eap-mcp 工具端點
                   |                                     <- 工具結果
                   |
回傳聚合結果 <-----+
```

## 關鍵心得（我踩過、也跨過的）

- **Plan 優先**：以 JSON 計畫把自然語言的不確定性壓到工具層之下；解析要容錯（直出/` ```json`/混雜）。
- **參數正規化**：金額/數量以字串、移除千分位；`BUY/SELL`、`ELC` 等字面統一。
- **唯讀 vs. 改變狀態**：下單/撤單/模擬等有副作用的工具必經 gate；查詢工具允許安全預設。
- **工具介面先定穩**：MCP schema 穩住，Prompt 才能快速迭代；換 provider 不會牽動後端。
- **降級路徑**：模型丟純文字也能回結果；但只有 JSON 計畫會觸發實際執行。


## 技術主線（關鍵主題雲）

> wallet, order, order.created, spring, order.create, order.create.queue, order.exchange, order_create_queue, orderid, matched, ordercreatedevent, placebuyorderservice.execute

## 系列索引（29 天門牌）
- **Day 3｜08** — _wallet, order.create, order.create.queue, order.exchange, order, order.created, order_create_queue, spring_
- **Day 9｜11** — _orderid, order, matched, wallet, ordercreatedevent, placebuyorderservice.execute, order_created_queue, order.created_
- **Day 29｜29** — _mcp, aichatservice, eap-ai-client, chatmodel, chatclientautoconfig, mcptoolclient, spring, mcpclientconfig_
- **Day 4｜01** — _spring, redis, wallet, spring-boot, order-service, wallet-service, match-engine, eap-mcp_
- **Day 5｜02** — _spring, openapi, contract, openapi.yaml, spring-contract, openapi-generator-cli, postbidbuyorder, openapigenerator_
- **Day 6｜03** — _spring, rabbitmq, rabbittemplate, ordercreateevent, placebuyorderservice, amqp, wallet-service, order-service_
- **Day 7｜04** — _wallet, ordercreateevent, rabbitmq, spring, rabbitlistener, order, availablecurrency, availableamount_
- **Day 8｜05** — _wallet, order, redis, ordercreateevent, ordercreatedevent, matchengine, rabbitmq, ordermatchedevent_
- **Day 9｜06** — _spring, contract, createorderlistener, basecontracttest, order.exchange, rabbitmq, wallet-service, build.gradle_
- **Day 10｜07** — _wallet, rabbitmq, ordercreateevent, spring, placebuyorderreq, amqp, order_exchange, order_create_routing_key_
- **Day 11｜09** — _wallet, available, ordercreatedevent, ordercreateevent, order, iswalletenough, iswalletenoughforsell_
- **Day 12｜10** — _order.created, wallet, order.exchange, ordercreatedevent, orderid, match, engine, orderbook_
- **Day 13｜12** — _matchengine, order.created, order.matched, wallet.matched, wallet, order-service, wallet-service, spring_
- **Day 14｜13** — _orderid, orderbook, redis, ordercreatedevent, orders_
- **Day 15｜14** — _redis, orderid, order_
- **Day 16｜15** — _order, redis, addorder, removeorder, ordercancelevent_
- **Day 17｜16** — _order.created.queue, redis, k8s, rabbitmq, order.created, order.created., ordermatchedevent, order_
- **Day 18｜17** — _websocket, orderbook, order-service_
- **Day 19｜18** — _orderbook, feign, order.matched, feignclient_
- **Day 20｜19** — _mcp, mcpapicontroller, orders, spring, websocket, eap-order, placeorder, cancelorder_
- **Day 21｜01 - Gradle 與相依套件（Dependencies）** — _mcp, spring, build.gradle, eap-mcp, feign, org.springframework.boot, metrics, gradle_
- **Day 22｜02 - MCP 設定與初始化（Configuration）** — _mcp, feign, client, wallet, order, mcptoolconfig.java, eap-mcp, spring_
- **Day 23｜03 - 撰寫 MCP Tools：從簡單查詢到狀態變更** — _client, mcp, failure, eap-mcp, main, orderbookmcptool, orderserviceclient, marketmetricsmcptool_
- **Day 24｜23** — _placeorderresponse, simulation, simulationrequest, simulationservice, ordersperstep, simulationresult, simulationfill, mcp_
- **Day 25｜04 — Simulation 工具（Code excerpts & explanations）** — _simulation, simulationservice, runsimulation, simulationrequest, simulationresult, mcp, simulationmcptool, lastsimulationresult_
- **Day 26｜25** — _mcp, spring, ai-agent, feign, aichatservice, simulation, contract, gradle_
- **Day 27｜01 - 建置 (Build) — 建立與編譯 ai-service** — _chatclient, mcp, eap-ai-client, spring, client, build.gradle, main, ai-service_
- **Day 28｜02 - 設定 (Configuration) — 將 LLM 與 MCP 連起來** — _chatclient, client, mcp, chatmodel, spring, ollamaapi, chatclientautoconfig, ollama_
- **Day 29｜03 - 實作 (Implementation) — AiChatService、McpToolClient 與執行流程** — _aichatservice, mcptoolclient, getorderbook, mcp, getmarketmetrics, placeorder, cancelorder, runsimulation_

