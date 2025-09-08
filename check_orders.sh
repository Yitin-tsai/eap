#!/bin/bash

echo "=== 查詢用戶訂單狀態 ==="

echo "1. 查詢用戶1的所有訂單 (550e8400-e29b-41d4-a716-446655440000)"
curl -X GET "http://localhost:8080/eap-order/bid/user-orders/all" \
  -H "ID_TOKEN: 550e8400-e29b-41d4-a716-446655440000"
echo ""
echo ""

echo "2. 查詢用戶1的待處理訂單"
curl -X GET "http://localhost:8080/eap-order/bid/user-orders/pending" \
  -H "ID_TOKEN: 550e8400-e29b-41d4-a716-446655440000"
echo ""
echo ""

echo "3. 查詢用戶2的所有訂單 (450e8400-e29b-41d4-a716-446655440001)"
curl -X GET "http://localhost:8080/eap-order/bid/user-orders/all" \
  -H "ID_TOKEN: 450e8400-e29b-41d4-a716-446655440001"
echo ""
echo ""

echo "4. 查詢用戶2的待處理訂單"
curl -X GET "http://localhost:8080/eap-order/bid/user-orders/pending" \
  -H "ID_TOKEN: 450e8400-e29b-41d4-a716-446655440001"
echo ""
echo ""

echo "5. 查詢用戶1的已成交訂單"
curl -X GET "http://localhost:8080/eap-order/bid/user-orders/matched" \
  -H "ID_TOKEN: 550e8400-e29b-41d4-a716-446655440000"
echo ""
echo ""

echo "6. 查詢用戶2的已成交訂單"
curl -X GET "http://localhost:8080/eap-order/bid/user-orders/matched" \
  -H "ID_TOKEN: 450e8400-e29b-41d4-a716-446655440001"
echo ""

echo "=== 查詢完成 ==="
