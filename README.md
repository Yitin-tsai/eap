# 電力交易平台（EAP: Electricity Auction Platform）

本專案為一套模擬電力交易所的後端系統，實作高頻率的即時掛單／買賣撮合機制，設計概念來自虛擬幣交易所，並採用微服務架構與事件驅動設計進行模組拆分與整合。適合練習 Spring Boot 技術棧、OpenAPI 驅動開發流程、非同步事件處理、契約測試與容器化部署。

---

## 📦 專案目標與功能模組

本平台核心功能包括：

- 電力買賣雙邊掛單 API（掛單、查詢、撮合歷史等）
- 撮合引擎（根據價格優先、時間優先的規則撮合訂單）
- 錢包資產管理（查詢餘額、鎖定資產、扣款）
- 使用 RabbitMQ 建立事件驅動流程（例如「訂單成交」後觸發資產扣款）
- 基於 OpenAPI 的 API-first 開發流程，支援跨服務 DTO 契約共用與測試生成

---

## 🛠 技術棧與設計考量

### Spring Boot

作為微服務核心框架，統整 REST API 開發、依賴注入、配置管理等。使用 Spring Boot 的主要原因為快速建構可獨立部署的服務。

---

### Spring Web

- 提供 RESTful API 開發能力
- 搭配 OpenAPI Generator 自動產生 Controller Interface
- 採 Interface-first + 實作分離，提高測試與維護彈性

---

### Spring Validation (`jakarta.validation`)

- 對傳入的 DTO 進行參數檢查與型別驗證
- 降低錯誤處理的重複程式碼

---

### Spring Security（後續擴充）

- 預留用於使用者身份驗證與權限管理
- 實作 OAuth2 或 JWT 的認證機制

---

### Spring Cloud Contract（規劃中）

- 透過 OpenAPI 產生 DTO 與契約測試 stub
- 讓 Wallet Service 等微服務能根據約定進行整合測試，避免版本對不上導致的錯誤

---

### Spring AMQP + RabbitMQ（事件驅動核心）

- 各微服務透過事件串連，不直接相依
- 例如：`order-service` 撮合成功後發送 `order.match` 事件 → `wallet-service` 訂閱事件進行資產鎖定與扣款
- 解耦微服務、提升擴展性與容錯能力

---

## 🧪 測試策略

- 使用 JUnit5 與 Mockito 撰寫單元測試
- 整合測試規劃以 Testcontainers 或本地 Cloud SQL 為基礎
- API 整合驗證可透過 Postman 或 Spring MockMvc 執行

---

## 📂 專案結構概覽（範例）

