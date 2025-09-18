# ⚡ 電力交易平台（EAP: Electricity Auction Platform）

本專案是一個模擬「電力交易所」的後端系統，靈感來自虛擬幣交易所，重點在於 **高頻率即時掛單與撮合**。  
採用 **微服務架構 + 事件驅動設計**，並整合 LLM 進行市場模擬。  
目標是練習 Spring Boot 生態系、事件驅動設計、契約測試與容器化部署。  

---

## 📦 功能模組

### order-service
- 提供掛單與查詢 API  
- 將 REST 請求轉換為 **OrderCreateEvent** 並送入事件流  
- 維護本地訂單狀態（PENDING → CREATED → MATCHED）

### wallet-service
- 接收 **OrderCreateEvent**，驗證與鎖定資產  
- 發送 **OrderCreatedEvent**，表示訂單已通過資產核定，可進入撮合  
- 在成交事件後進行最終結算  

### match-engine
- 接收 **OrderCreatedEvent**，嘗試撮合  
- 成交後發送 **OrderMatchedEvent**（給 order-service 做狀態更新）與 **WalletMatchedEvent**（給 wallet-service 做結算）  
- 使用 Redis 建立訂單簿（ZSET + Hash/Set 索引），支援高併發撮合與查詢  

### eap-mcp
- 提供 MCP 介面，讓 **LLM** 或外部工具可以與系統互動  
- 用於模擬市場情境與自動化測試  

### LLM 模擬層
- 作為外部 Agent，透過 MCP 發送掛單請求  
- 驗證整體事件流與市場互動行為  

---

## 🔄 訂單事件流

每筆訂單透過事件推進狀態：  

1. **OrderCreateEvent**：使用者掛單，狀態 → *PENDING*  
2. **OrderCreatedEvent**：Wallet 核定完成，狀態 → *CREATED*  
3. **OrderMatchedEvent**：撮合完成，狀態 → *MATCHED*，並觸發結算  

事件流讓狀態演進清晰可追蹤，服務間保持解耦。

---

## 🛠 技術棧

- **Spring Boot 全家桶**：微服務框架  
- **Spring Web + OpenAPI Generator**：API-first，生成 Controller 與 DTO  
- **Spring AMQP + RabbitMQ**：事件驅動骨幹  
- **Redis**：撮合訂單簿與快取  
- **Spring Cloud Contract**：事件契約測試（Wallet → Match → Order）  
- **Testcontainers**：整合測試與模擬外部依賴  
- **MCP + LLM**：市場模擬與自動化壓測  

---

## 🧪 測試策略

- **單元測試**：JUnit5 + Mockito  
- **契約測試**：Spring Cloud Contract（驗證事件格式）  
- **整合測試**：Testcontainers（Postgres、RabbitMQ、Redis）  
- **模擬測試**：透過 MCP + LLM 自動下單與觀察事件流  

---

## 📂 專案結構

```plaintext
eap-main
 ├── eap-common         # 共用模組（DTO, 常數, 工具）
 ├── eap-order          # order-service
 ├── eap-wallet         # wallet-service
 ├── eap-matchEngine    # match-engine
 ├── eap-mcp            # MCP 介面模組
 └── docs               # 技術文件與設計稿
