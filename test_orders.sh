#!/bin/bash

echo "=== 測試訂單創建 ==="

echo "1. 用戶1下買單 (價格98, 數量500)"
curl -X POST http://localhost:8080/eap-order/bid/buy \
  -H "Content-Type: application/json" \
  -d '{
    "bidPrice": 98,
    "amount": 500,
    "bidder": "550e8400-e29b-41d4-a716-446655440000"
  }'
echo ""

echo "2. 用戶1下買單 (價格100, 數量300)"
curl -X POST http://localhost:8080/eap-order/bid/buy \
  -H "Content-Type: application/json" \
  -d '{
    "bidPrice": 100,
    "amount": 300,
    "bidder": "550e8400-e29b-41d4-a716-446655440000"
  }'
echo ""

echo "3. 用戶2下賣單 (價格102, 數量400)"
curl -X POST http://localhost:8080/eap-order/bid/sell \
  -H "Content-Type: application/json" \
  -d '{
    "sellPrice": 102,
    "amount": 400,
    "seller": "450e8400-e29b-41d4-a716-446655440001"
  }'
echo ""

echo "4. 用戶2下賣單 (價格99, 數量200) - 應該會撮合"
curl -X POST http://localhost:8080/eap-order/bid/sell \
  -H "Content-Type: application/json" \
  -d '{
    "sellPrice": 99,
    "amount": 200,
    "seller": "450e8400-e29b-41d4-a716-446655440001"
  }'
echo ""

echo "5. 用戶1再下一個買單 (價格95, 數量600)"
curl -X POST http://localhost:8080/eap-order/bid/buy \
  -H "Content-Type: application/json" \
  -d '{
    "bidPrice": 95,
    "amount": 600,
    "bidder": "550e8400-e29b-41d4-a716-446655440000"
  }'
echo ""

echo "=== 測試完成 ==="
