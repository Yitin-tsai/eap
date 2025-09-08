# EAP Order Service API 說明文檔

## 概述
本文檔說明 eap-order 服務提供的所有 REST API 端點，供 MCP Server 呼叫使用。

## 基礎資訊
- **Base URL**: `http://localhost:8081`
- **Content-Type**: `application/json`
- **認證**: 使用 `ID_TOKEN` header（可選）
- **交易追蹤**: 使用 `txnSeq` header（可選）

## API 端點

### 1. 下買單
**POST** `/bid/buy`

下達買入訂單

#### 請求標頭
- `ID_TOKEN`: 用戶認證令牌（可選）
- `txnSeq`: 交易序號（可選）

#### 請求體
```json
{
  "userId": "string",
  "amount": "decimal",
  "price": "decimal",
  "symbol": "string"
}
```

#### 回應
```json
{
  "orderId": "uuid",
  "status": "PENDING_WALLET_CHECK",
  "message": "訂單已提交，正在檢查餘額..."
}
```

### 2. 下賣單
**POST** `/bid/sell`

下達賣出訂單

#### 請求標頭
- `ID_TOKEN`: 用戶認證令牌（可選）
- `txnSeq`: 交易序號（可選）

#### 請求體
```json
{
  "userId": "string",
  "amount": "decimal",
  "price": "decimal",
  "symbol": "string"
}
```

#### 回應
- 狀態碼：200 OK（成功）

### 3. 查詢用戶所有訂單
**GET** `/bid/user-orders/all`

查詢用戶的所有訂單（包含待處理和已成交）

#### 請求標頭
- `ID_TOKEN`: 用戶認證令牌（可選）
- `txnSeq`: 交易序號（可選）

#### 回應
```json
{
  "orders": [
    {
      "orderId": "uuid",
      "userId": "string",
      "side": "BUY|SELL",
      "amount": "decimal",
      "price": "decimal",
      "symbol": "string",
      "status": "string",
      "createdAt": "datetime",
      "updatedAt": "datetime"
    }
  ]
}
```

### 4. 查詢待處理訂單
**GET** `/bid/user-orders/pending`

查詢用戶在平台上待處理的訂單

#### 請求標頭
- `ID_TOKEN`: 用戶認證令牌（可選）
- `txnSeq`: 交易序號（可選）

#### 回應格式同上

### 5. 查詢已成交訂單
**GET** `/bid/user-orders/matched`

查詢用戶已成交的訂單

#### 請求標頭
- `ID_TOKEN`: 用戶認證令牌（可選）
- `txnSeq`: 交易序號（可選）

#### 回應格式同上

### 6. 取消訂單
**POST** `/bid/user-orders/cancel`

取消用戶的未完成訂單

#### 請求體
```json
{
  "orderId": "uuid"
}
```

#### 回應
- 狀態碼：200 OK（成功）

## Match Engine API（通過 order-service 代理）

### 1. 獲取訂單簿
**GET** `/v1/order/orderbook?depth=10`

獲取市場訂單簿數據

#### 查詢參數
- `depth`: 訂單簿深度（預設10層）

#### 回應
```json
{
  "symbol": "ELC",
  "bids": [
    {
      "price": "decimal",
      "quantity": "decimal",
      "orderCount": "integer"
    }
  ],
  "asks": [
    {
      "price": "decimal", 
      "quantity": "decimal",
      "orderCount": "integer"
    }
  ],
  "timestamp": "datetime"
}
```

### 2. 獲取市場摘要
**GET** `/v1/order/market/summary`

獲取市場基本統計資訊

#### 回應
```json
{
  "symbol": "ELC",
  "bestBid": "decimal",
  "bestAsk": "decimal",
  "spread": "decimal",
  "lastPrice": "decimal",
  "volume24h": "decimal",
  "high24h": "decimal",
  "low24h": "decimal",
  "timestamp": "datetime"
}
```

### 3. 查詢用戶訂單
**GET** `/v1/order/query?userId=xxx`

查詢用戶在撮合引擎中的訂單

#### 查詢參數
- `userId`: 用戶ID

#### 回應
```json
[
  {
    "orderId": "uuid",
    "userId": "string",
    "side": "BUY|SELL",
    "amount": "decimal",
    "price": "decimal",
    "symbol": "string",
    "status": "string",
    "createdAt": "datetime"
  }
]
```

## 錯誤處理

### 錯誤回應格式
```json
{
  "code": "ERROR_CODE",
  "message": "錯誤訊息",
  "details": {},
  "timestamp": "datetime"
}
```

### 常見錯誤碼
- `INSUFFICIENT_BALANCE`: 餘額不足
- `INVALID_PRICE`: 無效價格
- `INVALID_AMOUNT`: 無效數量
- `ORDER_NOT_FOUND`: 訂單不存在
- `ORDER_ALREADY_PROCESSED`: 訂單已處理
- `VALIDATION_ERROR`: 參數驗證錯誤
- `INTERNAL_ERROR`: 內部伺服器錯誤
- `SERVICE_UNAVAILABLE`: 服務不可用

## 使用建議

### 1. 錯誤重試
- 對於網路錯誤，建議指數退避重試
- 對於業務錯誤（如餘額不足），不應重試
- 最大重試次數建議設為 3 次

### 2. 超時設定
- 下單操作：5 秒超時
- 查詢操作：3 秒超時
- 取消操作：5 秒超時

### 3. 頻率限制
- 建議每用戶每分鐘最多 60 次 API 呼叫
- 下單操作每用戶每分鐘最多 10 次

### 4. 最佳實踐
- 使用 `txnSeq` 追蹤交易流程
- 定期查詢訂單狀態確認執行結果
- 在下單前先查詢餘額和市場狀態
- 設定合理的價格和數量範圍驗證
