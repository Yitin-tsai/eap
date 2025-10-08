# Day 30｜總結與感謝

這 29 天，我不是只做 AI。我把一個事件驅動的交易系統從**基礎架構**、**服務介面與資料一致性**、**佇列與撮合**、**市場推送**一路做起，最後再把 **LLM × MCP** 疊上去。這篇把「全系列」做個回顧。

## TL;DR
- **基礎先行**：清楚的服務邊界、事件流、金錢/存貨一致性與 Redis Order Book，讓後面的每一步都站得穩。  
- **平台化補強**：WebSocket 推送、契約與整合測試、可維護的設定與打包流程，讓系統能被複用與運營。  
- **AI × MCP 壓頂**：用 Plan（JSON）把不確定性關進籠子，工具層負責確定執行，AiChatService 做風險閘門。

---

## 30 天地圖（主題 × 範圍）
- **Day 01–04：系統設計與服務邊界** — Order / Wallet / Match Engine / Market Feed / eap-mcp / eap-ai-client 的定位與耦合策略。  
- **Day 05–10：事件驅動與資料一致性** — RabbitMQ 拓樸、命名規約、Wallet 先行的保證金/庫存檢查與 `order.created` 的下游一致性。  
- **Day 11–14：Order Book 與撮合基本功** — Redis ZSET 結構、TopBid/TopAsk 的取得、插入/刪除/更新的穩定性。  
- **Day 15–18：市場資料推送** — 以 WebSocket/SSE 發送 order book 與市況快照，降低 REST 輪詢壓力。   
- **Day 19–29：AI 編排與 MCP 工具** — SYSTEM_PROMPT、Plan 解析、參數正規化、工具呼叫與降級策略。

> 實作上，我刻意把「能跑就好」與「能被治理」綁在一起：**事件命名、參數型別、錯誤碼語意** 都先講清楚，再寫程式。

---

## 一、系統基礎（Day 01–14）

### 1) 服務邊界與資料流
- **Order Service** 負責接單、產生 `order.create`；  
- **Wallet Service** 監聽 `order.create`，檢查/保留額度（或庫存），成功才發 `order.created`；  
- **Match Engine** 監聽 `order.created`，進入 order book 撮合，成交再發 `order.matched`；  
- **Market Feed** 依事件/時間推送 order book 快照。  
- `eap-mcp` 提供「工具 API」，`eap-ai-client` 做「編排與風控」。

### 2) RabbitMQ 拓樸（最小可行但可治理）
- **Exchange**：`order.exchange`（Topic）  
- **Routing Key**：`order.create` → Wallet；`order.created` → 撮合；`order.matched` → 對帳/通知  
- **命名規約**：`{資源}.{動作}`，Queue 加 `.queue`，避免語意漂移。

### 3) 一致性策略：Wallet 先行
- 建單**先檢查資金/庫存、先保留**，再讓撮合入場；把「做不到的訂單」擋在前面。  
- 每個事件都可以重放；以 `TXNSEQ`（或等效幂等鍵）避免重複扣款。

### 4) Order Book with Redis ZSET
- 每商品 × (BUY/SELL) 一個 ZSET：score=價格，member=orderId(+時間)。  
- **Top-N 讀取高效**、**插入/刪除 O(logN)**，適合現在的規模；必要時以 Lua 做小範圍原子更新。

### 5) API 契約與測試
- **OpenAPI** 先定義欄位/單位/錯誤語意；**Contract/Integration** 驗證通道能跑起來。  
- 減少「彼此以為對方懂」的灰色地帶。

---

## 二、平台化與可運維（Day 15–18）

### 1) 市場推送通道
- WebSocket/SSE 提供 order book / 市場指標的即時觀測；**拉少一點、推多一點**，避開高頻輪詢。

### 2) 對外呼叫一致化
- 以 Feign/WebClient 收斂對外呼叫：**超時、重試、錯誤轉換** 集中治理；不讓每個服務各自一套。

### 3) 設定與打包
- 使用 `@ConfigurationProperties` 收斂關鍵設定（包含 MCP 端點），啟動即做校驗；  
- 建置以 Gradle 為主，包出可直接跑的 fat jar 與容器映像。

---

## 三、AI 編排與工具化（Day 19–29）

### 1) 角色責任劃分
- **LLM**：負責「說應該做什麼」（Plan）；  
- **AiChatService**：**解析 + 風控 + 參數正規化**；  
- **MCP 工具**：負責「真的去做」。

### 2) Plan 與降級
- SYSTEM_PROMPT 要求輸出 **JSON 計畫（actions/arguments）**；  
- `parsePlanStrict` 容錯（純 JSON / ```json / 混雜文字）；  
- **只有 JSON Plan 會觸發狀態改變**，純文字視為查詢解讀。

### 3) 參數正規化與必要欄位檢查
- `price/qty` 去千分位、以字串處理；`BUY/SELL`、商品代碼大寫；  
- `placeOrder/cancelOrder/runSimulation` 缺參直接退回不執行。

### 4) 工具呼叫與錯誤隔離
- `McpToolClient` 統一呼叫入口，單一 action 失敗只影響自己；結果以 `data/error` 清楚回報。

---

## 四、跨篇章的關鍵決策（Trade-offs）
- **簡單命名 > 巧妙縮寫**：事件/Queue 名稱可讀，日後維護靠它。  
- **「現在可行」>「未來也許更快」**：先用 Redis ZSET 支撐當前吞吐；觀測到瓶頸再演進。  
- **契約先行 > 界面猜測**：OpenAPI/Contract 減少跨組誤解。  
- **統一呼叫層 > 分散自定義**：Feign/WebClient 集中治理超時/重試/錯誤碼。  
- **Plan 優先 > 直接下指令**：只信 JSON 計畫，避免 LLM 描述性「幻覺」觸發副作用。  
- **降級策略 > 全有或全無**：Plan 失敗就回原文，不硬執行；查詢類容許安全預設。

---

## 五、里程碑（我驗收的東西）
- `order.create → wallet pass → order.created → match → order.matched` 全線走通。  
- Redis Order Book 可查 Top-N，撮合流程能產生日誌與事件。  
- WebSocket/SSE 能把 order book 與市況快照推給外部。  
- OpenAPI + Contract/Integration 保證主要通道「別人照著文件就能跑起來」。  
- AiChatService 能把 Plan 解析、過 Gate、呼叫 MCP執行下單、查詢。

---

## 六、給未來的我（後續路線）
- **可觀測補完**：工具呼叫耗時、成功率、錯誤碼分佈；把 Prompt 版本與 Plan 失敗率納管。  
- **成本/延遲治理**：便宜模型規劃 → 需要時升級強模型，建立 SLO 與保護欄。  
- **風控與二次確認**：高金額/劇烈價差的下單加上閥值與雙重確認（含人工許可模式）。  
- **模擬更強**：`runSimulation` 支援多策略混合、事件腳本、KPI 報表輸出。

---

## 結語
如果要用一句話收尾：**先把系統做穩，再把 AI 疊上來。**  
我用事件驅動與清楚邊界打底，靠契約與測試把路鋪平，最後以 Plan/Gate/Tools 讓 AI 真正「可控地執行」。接下來，我會把觀測、風控與成本/延遲治理做深，讓這套系統在更真實的壓力下，也能沉著運行。