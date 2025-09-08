# LLM 平台 MCP 集成指南

## 🤖 Claude Desktop 集成

### 配置文件位置
- **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

### 配置內容
```json
{
  "mcpServers": {
    "eap-trading": {
      "command": "python3",
      "args": ["/path/to/eap/eap-mcp/scripts/mcp_bridge.py"],
      "env": {
        "EAP_MCP_URL": "http://localhost:8083",
        "EAP_MCP_TIMEOUT": "30"
      }
    }
  }
}
```

## 🦾 OpenAI GPTs 集成

### Actions 配置
```yaml
openapi: 3.0.1
info:
  title: EAP Trading MCP API
  description: 電力交易平台 MCP 工具接口
  version: 1.0.0
servers:
  - url: http://localhost:8083
paths:
  /mcp/tools/getOrderBook/call:
    post:
      operationId: getOrderBook
      summary: 獲取訂單簿數據
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                arguments:
                  type: object
                  properties:
                    depth:
                      type: integer
                      default: 10
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                type: object
                
  /mcp/tools/placeOrder/call:
    post:
      operationId: placeOrder
      summary: 下單交易
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                arguments:
                  type: object
                  properties:
                    userId:
                      type: string
                      format: uuid
                    side:
                      type: string
                      enum: [BUY, SELL]
                    price:
                      type: string
                    qty:
                      type: string
      responses:
        '200':
          description: 成功
```

## 🧠 Other LLM Integrations

### LangChain 集成
```python
from langchain.tools import Tool
from langchain.agents import initialize_agent
import requests

def create_eap_tools():
    def call_mcp_tool(tool_name: str, **kwargs):
        response = requests.post(
            f"http://localhost:8083/mcp/tools/{tool_name}/call",
            json={"arguments": kwargs}
        )
        return response.json()
    
    tools = [
        Tool(
            name="get_order_book",
            description="獲取訂單簿數據",
            func=lambda **kwargs: call_mcp_tool("getOrderBook", **kwargs)
        ),
        Tool(
            name="place_order", 
            description="下單交易",
            func=lambda **kwargs: call_mcp_tool("placeOrder", **kwargs)
        ),
        Tool(
            name="get_market_metrics",
            description="獲取市場指標", 
            func=lambda **kwargs: call_mcp_tool("getMetrics", **kwargs)
        )
    ]
    
    return tools

# 使用示例
tools = create_eap_tools()
agent = initialize_agent(tools, llm, agent_type="zero-shot-react-description")
```

### LlamaIndex 集成
```python
from llama_index.tools import FunctionTool
import requests

def get_order_book(depth: int = 10):
    """獲取訂單簿數據"""
    response = requests.post(
        "http://localhost:8083/mcp/tools/getOrderBook/call",
        json={"arguments": {"depth": depth}}
    )
    return response.json()

def place_order(user_id: str, side: str, price: str, qty: str):
    """下單交易"""
    response = requests.post(
        "http://localhost:8083/mcp/tools/placeOrder/call", 
        json={"arguments": {
            "userId": user_id,
            "side": side, 
            "price": price,
            "qty": qty
        }}
    )
    return response.json()

# 創建工具
tools = [
    FunctionTool.from_defaults(fn=get_order_book),
    FunctionTool.from_defaults(fn=place_order)
]
```

## 🔐 Production 注意事項

### 安全性
- 實現 API Key 認證
- 使用 HTTPS
- 添加 IP 白名單
- 實現 Rate Limiting

### 監控
- 添加請求日誌
- 設置告警
- 監控 API 使用量
- 性能指標收集

### 示例生產配置
```yaml
# docker-compose.yml
version: '3.8'
services:
  eap-mcp:
    image: eap-mcp:latest
    ports:
      - "8083:8083"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - EAP_MCP_API_KEY=${EAP_MCP_API_KEY}
      - EAP_MCP_ALLOWED_ORIGINS=${EAP_MCP_ALLOWED_ORIGINS}
    volumes:
      - ./config:/app/config
    networks:
      - eap-network
```
