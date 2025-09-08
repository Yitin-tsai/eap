#!/bin/bash

echo "=== 創建賣單測試撮合 ==="

echo "1. 用戶2下賣單 (價格99, 數量200) - 應該與用戶1的買單(價格100)撮合"
curl -X POST http://localhost:8080/eap-order/bid/sell \
  -H "Content-Type: application/json" \
  -d '{
    "sellPrice": 99,
    "amount": 200,
    "seller": "450e8400-e29b-41d4-a716-446655440001"
  }'
echo ""

echo "2. 等待2秒讓撮合處理完成..."
sleep 2

echo "3. 用戶2下另一個賣單 (價格102, 數量400) - 不會撮合，會加入訂單簿"
curl -X POST http://localhost:8080/eap-order/bid/sell \
  -H "Content-Type: application/json" \
  -d '{
    "sellPrice": 102,
    "amount": 400,
    "seller": "450e8400-e29b-41d4-a716-446655440001"
  }'
echo ""

echo "4. 等待2秒讓處理完成..."
sleep 2

echo "=== 賣單創建完成 ==="
