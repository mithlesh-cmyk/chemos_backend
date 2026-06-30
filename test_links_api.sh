#!/bin/bash
# Quick Test Script for Sale-Purchase Links API
# Replace these with your actual values

BASE_URL="http://localhost:8081"
TOKEN="YOUR_JWT_TOKEN_HERE"
SALE_ID="your-sale-uuid"
PURCHASE_ID="your-purchase-uuid"

echo "=== 1. Create Link ==="
curl -X POST $BASE_URL/api/v1/links \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"saleId\": \"$SALE_ID\",
    \"purchaseId\": \"$PURCHASE_ID\",
    \"linkedQuantity\": 50.0
  }"
echo -e "\n"

echo "=== 2. Get Sale Summary ==="
curl -X GET $BASE_URL/api/v1/links/sale/$SALE_ID \
  -H "Authorization: Bearer $TOKEN"
echo -e "\n"

echo "=== 3. Get Purchase Summary ==="
curl -X GET $BASE_URL/api/v1/links/purchase/$PURCHASE_ID \
  -H "Authorization: Bearer $TOKEN"
echo -e "\n"

echo "=== 4. Update Link (replace LINK_ID) ==="
LINK_ID="link-uuid-here"
curl -X PUT $BASE_URL/api/v1/links/$LINK_ID \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"linkedQuantity\": 75.0
  }"
echo -e "\n"

echo "=== 5. Delete Link (replace LINK_ID) ==="
curl -X DELETE $BASE_URL/api/v1/links/$LINK_ID \
  -H "Authorization: Bearer $TOKEN"
echo -e "\n"
