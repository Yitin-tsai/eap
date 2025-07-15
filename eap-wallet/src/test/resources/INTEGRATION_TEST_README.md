# RabbitMQ 集成測試使用指南

## CreateOrderListenerIntegrationTest

這是一個**真正的 RabbitMQ 集成測試**，測試完整的訊息流程：
1. 發送 `OrderCreateEvent` 到 RabbitMQ
2. `CreateOrderListener` 接收並處理訊息
3. 驗證是否正確發送 `OrderCreatedEvent`

## 運行前準備

### 1. 啟動 RabbitMQ 服務

#### 使用 Docker (推薦)
```bash
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3.12-management
```

#### 或使用 Docker Compose
```yaml
services:
  rabbitmq:
    image: rabbitmq:3.12-management
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
```

#### 本地安裝
```bash
# macOS
brew install rabbitmq
brew services start rabbitmq

# Ubuntu
sudo apt-get install rabbitmq-server
sudo systemctl start rabbitmq-server
```

### 2. 驗證 RabbitMQ 正在運行

- 管理界面: http://localhost:15672 (guest/guest)
- 或檢查連接: `telnet localhost 5672`

## 運行測試

### 單獨運行集成測試
```bash
./gradlew test --tests CreateOrderListenerIntegrationTest
```

### 運行所有測試
```bash
./gradlew test
```

## 測試場景

### 1. `testCompleteOrderFlow_WithSufficientBalance`
- ✅ 模擬餘額充足的情況
- 📤 發送 `OrderCreateEvent` 到 RabbitMQ
- ⏳ 等待異步處理
- ✅ 驗證 `OrderCreatedEvent` 被發送

### 2. `testCompleteOrderFlow_WithInsufficientBalance`
- ❌ 模擬餘額不足的情況
- 📤 發送 `OrderCreateEvent` 到 RabbitMQ
- ⏳ 等待異步處理
- ❌ 確認沒有發送 `OrderCreatedEvent`

### 3. `testRabbitMQConnection`
- 🔗 基本連接測試
- 📤 測試訊息發送功能

## 故障排除

### 測試失敗 - 連接錯誤
```
ApplicationContext failure threshold (1) exceeded
```
**解決方案**: 確保 RabbitMQ 正在運行並監聽 5672 端口

### 測試超時
```
await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {...})
```
**解決方案**: 
1. 檢查 RabbitMQ 日誌
2. 增加等待時間
3. 檢查隊列是否正確創建

### 查看 RabbitMQ 隊列
```bash
# 使用 rabbitmqctl
docker exec rabbitmq rabbitmqctl list_queues

# 或訪問管理界面
open http://localhost:15672
```

## 測試配置

測試會自動創建以下 RabbitMQ 組件：
- **Exchange**: `order.exchange` (topic)
- **Queue**: `order.create.queue`
- **Queue**: `order.created.queue`
- **Binding**: `order.create` → `order.create.queue`
- **Binding**: `order.created` → `order.created.queue`

## 與其他測試的區別

| 測試類型 | 外部依賴 | 訊息傳遞 | 適用場景 |
|---------|----------|----------|----------|
| **Unit Test** | ❌ 無 | ❌ Mock | 開發時快速反饋 |
| **Simple Test** | ❌ 無 | ❌ 直接調用 | CI/CD 流水線 |
| **Integration Test** | ✅ RabbitMQ | ✅ 真實訊息 | 部署前驗證 |

這個集成測試確保了你的應用在真實 RabbitMQ 環境下的完整功能！
