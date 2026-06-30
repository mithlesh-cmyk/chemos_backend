# PowerShell Quick Test Script for Sale-Purchase Links API
# Replace these with your actual values

$BASE_URL = "http://localhost:8081"
$TOKEN = "YOUR_JWT_TOKEN_HERE"
$SALE_ID = "your-sale-uuid"
$PURCHASE_ID = "your-purchase-uuid"

Write-Host "=== 1. Create Link ===" -ForegroundColor Green
$createBody = @{
    saleId = $SALE_ID
    purchaseId = $PURCHASE_ID
    linkedQuantity = 50.0
} | ConvertTo-Json

Invoke-RestMethod -Uri "$BASE_URL/api/v1/links" -Method POST `
  -Headers @{ 
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $TOKEN"
  } -Body $createBody | ConvertTo-Json -Depth 10
Write-Host ""

Write-Host "=== 2. Get Sale Summary ===" -ForegroundColor Green
Invoke-RestMethod -Uri "$BASE_URL/api/v1/links/sale/$SALE_ID" -Method GET `
  -Headers @{ "Authorization" = "Bearer $TOKEN" } | ConvertTo-Json -Depth 10
Write-Host ""

Write-Host "=== 3. Get Purchase Summary ===" -ForegroundColor Green
Invoke-RestMethod -Uri "$BASE_URL/api/v1/links/purchase/$PURCHASE_ID" -Method GET `
  -Headers @{ "Authorization" = "Bearer $TOKEN" } | ConvertTo-Json -Depth 10
Write-Host ""

Write-Host "=== 4. Update Link (replace LINK_ID) ===" -ForegroundColor Green
$LINK_ID = "link-uuid-here"
$updateBody = @{
    linkedQuantity = 75.0
} | ConvertTo-Json

Invoke-RestMethod -Uri "$BASE_URL/api/v1/links/$LINK_ID" -Method PUT `
  -Headers @{ 
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $TOKEN"
  } -Body $updateBody | ConvertTo-Json -Depth 10
Write-Host ""

Write-Host "=== 5. Delete Link (replace LINK_ID) ===" -ForegroundColor Green
Invoke-RestMethod -Uri "$BASE_URL/api/v1/links/$LINK_ID" -Method DELETE `
  -Headers @{ "Authorization" = "Bearer $TOKEN" }
Write-Host "Link deleted successfully" -ForegroundColor Yellow
