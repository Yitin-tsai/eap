# EAP MCP Server

EAP 電力交易平台的 Model Context Protocol (MCP) 服務器，為 LLM 提供標準化的交易工具接口。

## 功能概述

### 核心功能
- **標準化工具接口**: 提供符合 MCP 規範的工具定義和執行接口
- **市場數據查詢**: 獲取實時訂單簿、交易記錄和市場指標
- **訂單管理**: 支持下單、取消、查詢等操作（Phase 2）
- **風險控制**: 內建頻率限制和錯誤處理機制
- **審計追蹤**: 完整的操作日誌和性能監控

### 架構設計
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   LLM Client    │───▶│   EAP MCP       │───▶│  Order Service  │
│   (Claude/GPT)  │    │   Server        │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │  Match Engine   │
                       │                 │
                       └─────────────────┘
```

## 快速開始

### 前置需求
- Java 17+
- Redis (用於頻率限制)
- eap-order 服務運行在 localhost:8081
- eap-matchEngine 服務運行在 localhost:8082

### 啟動服務

1. **構建項目**
```bash
./gradlew build
```

2. **啟動服務**
```bash
./gradlew bootRun
```

服務將在 http://localhost:8083 啟動

### 驗證部署

```bash
# 檢查服務狀態
curl http://localhost:8083/mcp/health

# 獲取服務資訊
curl http://localhost:8083/mcp/info

# 查看可用工具
curl http://localhost:8083/mcp/tools
```

## API 文檔

### MCP 標準端點

#### 獲取服務器資訊
```http
GET /mcp/info
```

#### 列出所有工具
```http
GET /mcp/tools
```

#### 執行工具
```http
POST /mcp/tools/{toolName}/call
Content-Type: application/json

{
  "arguments": {
    "symbol": "ELC",
    "depth": 10
  }
}
```

### 可用工具 (Phase 1)

#### 1. getOrderBook
獲取訂單簿數據

**參數**:
- `symbol` (string, 可選): 交易標的，預設 "ELC"
- `depth` (integer, 可選): 深度層數，預設 10

**回應**: 包含買賣盤資訊的訂單簿數據

#### 2. getTrades
獲取交易記錄

**參數**:
- `symbol` (string, 可選): 交易標的，預設 "ELC"
- `since` (string, 可選): 開始時間 (ISO 8601)
- `cursor` (string, 可選): 分頁游標
- `limit` (integer, 可選): 記錄數量，預設 50

**回應**: 交易記錄列表

#### 3. metrics
獲取市場指標

**參數**:
- `symbol` (string, 可選): 交易標的，預設 "ELC"
- `window` (string, 可選): 時間窗口，預設 "1h"
- `depthN` (integer, 可選): 深度分析層數，預設 5

**回應**: 包含價差、成交量、波動率等指標

## 開發指南

### 項目結構
```
src/main/java/com/eap/mcp/
├── EapMcpApplication.java          # 主應用程式
├── client/                         # Feign 客戶端
│   ├── OrderServiceClient.java     # Order Service 客戶端
│   └── MatchEngineClient.java      # Match Engine 客戶端
├── config/                         # 配置類
│   └── McpConfig.java              # MCP 配置
├── controller/                     # REST 控制器
│   └── McpController.java          # MCP API 控制器
├── dto/                           # 資料傳輸對象
│   ├── McpErrorResponse.java       # 錯誤回應格式
│   ├── PlaceOrderRequest.java      # 下單請求
│   └── PlaceOrderResponse.java     # 下單回應
├── service/                       # 業務服務
│   └── McpToolService.java        # 工具管理服務
└── tools/                         # MCP 工具實現
    ├── GetOrderBookTool.java       # 訂單簿工具
    ├── GetTradesTool.java          # 交易記錄工具
    └── GetMetricsTool.java         # 指標工具
```

### 配置文件

#### application.yml
```yaml
server:
  port: 8083

eap:
  order-service:
    base-url: http://localhost:8081
  match-engine:
    base-url: http://localhost:8082

mcp:
  rate-limit:
    enabled: true
    requests-per-minute: 60
  audit:
    enabled: true
```

### 添加新工具

1. **創建工具類**
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class NewTool {
    public Object execute(Map<String, Object> parameters) {
        // 工具邏輯
    }
    
    public String getName() { return "toolName"; }
    public String getDescription() { return "工具描述"; }
    public Map<String, Object> getSchema() { 
        // JSON Schema
    }
}
```

2. **註冊到 McpToolService**
```java
case "toolName":
    return newTool.execute(parameters);
```

## Phase 2 規劃

### 交易工具
- `placeOrder`: 下單功能
- `cancelOrder`: 取消訂單
- `getOrder`: 查詢訂單詳情
- `listOrders`: 查詢訂單列表

### 模擬功能
- `loadScenario`: 載入交易場景
- `spawnAgents`: 生成交易代理
- `runSimulation`: 執行模擬
- `computeLoss`: 計算損失
- `sweep`: 參數掃描

### 報告功能
- `exportReport`: 匯出分析報告

## 監控和運維

### 健康檢查
```bash
curl http://localhost:8083/actuator/health
```

### 指標收集
```bash
curl http://localhost:8083/actuator/metrics
curl http://localhost:8083/actuator/prometheus
```

### 日誌級別
- 開發環境: DEBUG
- 生產環境: INFO

## 安全考量

### 頻率限制
- 每分鐘最多 60 次請求
- 突發容量: 10 次請求

### 錯誤處理
- 統一的錯誤格式
- 不暴露內部實現細節
- 完整的錯誤追蹤

### 審計日誌
- 記錄所有工具呼叫
- 包含請求參數和執行結果
- 性能指標收集

## 故障排除

### 常見問題

1. **連接 order-service 失敗**
   - 檢查 order-service 是否運行
   - 確認 base-url 配置正確

2. **Redis 連接錯誤**
   - 檢查 Redis 服務狀態
   - 確認連接參數配置

3. **工具執行失敗**
   - 查看詳細錯誤日誌
   - 驗證輸入參數格式

### 日誌位置
- 應用日誌: stdout
- 錯誤日誌: stderr
- 審計日誌: Redis/檔案系統

## 貢獻指南

1. Fork 項目
2. 創建功能分支
3. 提交變更
4. 推送到分支
5. 創建 Pull Request

## 授權

本項目採用 MIT 授權條款。
