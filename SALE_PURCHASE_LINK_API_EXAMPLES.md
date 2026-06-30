# Sale-Purchase Link API - Testing Guide

## Authentication
All endpoints require JWT authentication. Replace `YOUR_JWT_TOKEN` with your actual token.

```bash
# Get your JWT token first by logging in
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "your_username",
    "password": "your_password"
  }'
```

---

## 1. Create a Link (Link a PO to a Sale)

**Scenario:** Sale requires 140MT, PO has 100MT available. Link 100MT from this PO to this sale.

```bash
curl -X POST http://localhost:8081/api/v1/links \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "saleId": "SALE-UUID-HERE",
    "purchaseId": "PURCHASE-UUID-HERE",
    "linkedQuantity": 100.0
  }'
```

**Response (200 OK):**
```json
{
  "id": "link-uuid-123",
  "saleId": "sale-uuid-456",
  "purchaseId": "purchase-uuid-789",
  "linkedQuantity": 100.0,
  "purchaseOriginalQuantity": 100.0,
  "purchaseAvailableQuantity": 0.0,
  "saleTotalRequired": 140.0,
  "saleRemainingQuantity": 40.0,
  "createdAt": "2026-06-30T10:15:30",
  "updatedAt": "2026-06-30T10:15:30"
}
```

**What the response means:**
- ✅ Link created successfully
- The PO had 100MT, now has 0MT available (fully committed)
- The sale needs 140MT total, still needs 40MT more (add another PO link)

---

## 2. Create a Second Link (Add another PO to fulfill remaining requirement)

**Scenario:** Sale still needs 40MT. Add a second PO with 80MT, but only commit 40MT.

```bash
curl -X POST http://localhost:8081/api/v1/links \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "saleId": "SALE-UUID-HERE",
    "purchaseId": "ANOTHER-PURCHASE-UUID",
    "linkedQuantity": 40.0
  }'
```

**Response (200 OK):**
```json
{
  "id": "link-uuid-124",
  "saleId": "sale-uuid-456",
  "purchaseId": "purchase-uuid-999",
  "linkedQuantity": 40.0,
  "purchaseOriginalQuantity": 80.0,
  "purchaseAvailableQuantity": 40.0,
  "saleTotalRequired": 140.0,
  "saleRemainingQuantity": 0.0,
  "createdAt": "2026-06-30T10:20:15",
  "updatedAt": "2026-06-30T10:20:15"
}
```

**What happened:**
- ✅ Sale is now fully fulfilled (0MT remaining)
- This PO had 80MT, committed 40MT, still has 40MT available for other sales

---

## 3. Update a Link (Change committed quantity)

**Scenario:** Salesperson wants to increase the quantity from the second PO from 40MT to 50MT.

```bash
curl -X PUT http://localhost:8081/api/v1/links/link-uuid-124 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "linkedQuantity": 50.0
  }'
```

**Response (200 OK):**
```json
{
  "id": "link-uuid-124",
  "saleId": "sale-uuid-456",
  "purchaseId": "purchase-uuid-999",
  "linkedQuantity": 50.0,
  "purchaseOriginalQuantity": 80.0,
  "purchaseAvailableQuantity": 30.0,
  "saleTotalRequired": 140.0,
  "saleRemainingQuantity": 0.0,
  "createdAt": "2026-06-30T10:20:15",
  "updatedAt": "2026-06-30T11:05:00"
}
```

**Note:** The sale is now over-fulfilled by 10MT (you'll need to adjust the first link or let the business logic handle it).

---

## 4. Delete a Link (Remove PO from Sale)

**Scenario:** Remove the first PO link. This releases 100MT back to the PO and increases sale's remaining requirement by 100MT.

```bash
curl -X DELETE http://localhost:8081/api/v1/links/link-uuid-123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response (204 No Content):**
```
(Empty body - link deleted successfully)
```

---

## 5. Get Sale Summary (View all POs linked to a Sale)

**Scenario:** See all the POs linked to a particular sale and how much is still needed.

```bash
curl -X GET http://localhost:8081/api/v1/links/sale/SALE-UUID-HERE \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response (200 OK):**
```json
{
  "saleId": "sale-uuid-456",
  "totalRequired": 140.0,
  "totalLinked": 100.0,
  "remaining": 40.0,
  "links": [
    {
      "linkId": "link-uuid-123",
      "purchaseId": "purchase-uuid-789",
      "linkedQuantity": 60.0,
      "purchaseOriginalQuantity": 100.0,
      "purchaseAvailableQuantity": 40.0
    },
    {
      "linkId": "link-uuid-125",
      "purchaseId": "purchase-uuid-888",
      "linkedQuantity": 40.0,
      "purchaseOriginalQuantity": 50.0,
      "purchaseAvailableQuantity": 10.0
    }
  ]
}
```

**What this shows:**
- Sale needs 140MT total
- Currently has 100MT linked from 2 POs
- Still needs 40MT more
- First PO: committed 60MT, has 40MT available for other sales
- Second PO: committed 40MT, has 10MT available for other sales

---

## 6. Get Purchase Summary (View all Sales linked to a PO)

**Scenario:** See how a PO's quantity is distributed across multiple sales.

```bash
curl -X GET http://localhost:8081/api/v1/links/purchase/PURCHASE-UUID-HERE \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response (200 OK):**
```json
{
  "purchaseId": "purchase-uuid-789",
  "originalQuantity": 100.0,
  "totalLinked": 80.0,
  "availableQuantity": 20.0,
  "links": [
    {
      "linkId": "link-uuid-123",
      "saleId": "sale-uuid-456",
      "linkedQuantity": 50.0,
      "saleTotalRequired": 140.0,
      "saleRemainingQuantity": 40.0
    },
    {
      "linkId": "link-uuid-126",
      "saleId": "sale-uuid-777",
      "linkedQuantity": 30.0,
      "saleTotalRequired": 80.0,
      "saleRemainingQuantity": 0.0
    }
  ]
}
```

**What this shows:**
- This PO has 100MT total
- 80MT is committed across 2 sales
- 20MT still available for new sales
- Sale 1: took 50MT, still needs 40MT more from other POs
- Sale 2: took 30MT, is fully fulfilled (0MT remaining)

---

## Error Scenarios

### A. Duplicate Link
```bash
# Try to create the same link twice
curl -X POST http://localhost:8081/api/v1/links \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "saleId": "sale-uuid-456",
    "purchaseId": "purchase-uuid-789",
    "linkedQuantity": 50.0
  }'
```

**Response (409 Conflict):**
```json
{
  "timestamp": "2026-06-30T10:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "A link already exists between this sale and purchase. Use PUT to update it.",
  "path": "/api/v1/links"
}
```

### B. Quantity Exceeds PO Available
```bash
# Try to commit more than the PO has available
curl -X POST http://localhost:8081/api/v1/links \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "saleId": "sale-uuid-456",
    "purchaseId": "purchase-uuid-789",
    "linkedQuantity": 150.0
  }'
```

**Response (400 Bad Request):**
```json
{
  "timestamp": "2026-06-30T10:35:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Linked quantity (150.00) exceeds available PO quantity (20.00). The PO has 80.00 already committed to other sales.",
  "path": "/api/v1/links"
}
```

### C. Quantity Exceeds Sale Remaining
```bash
# Try to commit more than the sale still needs
curl -X POST http://localhost:8081/api/v1/links \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "saleId": "sale-uuid-456",
    "purchaseId": "purchase-uuid-999",
    "linkedQuantity": 60.0
  }'
```

**Response (400 Bad Request):**
```json
{
  "timestamp": "2026-06-30T10:40:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Linked quantity (60.00) exceeds remaining sale requirement (40.00). The sale already has 100.00 linked from other POs.",
  "path": "/api/v1/links"
}
```

### D. Invalid/Zero Quantity
```bash
curl -X POST http://localhost:8081/api/v1/links \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "saleId": "sale-uuid-456",
    "purchaseId": "purchase-uuid-789",
    "linkedQuantity": 0
  }'
```

**Response (400 Bad Request):**
```json
{
  "timestamp": "2026-06-30T10:45:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Linked quantity must be a positive number",
  "path": "/api/v1/links"
}
```

### E. Sale Not Found
```bash
curl -X POST http://localhost:8081/api/v1/links \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "saleId": "non-existent-sale-id",
    "purchaseId": "purchase-uuid-789",
    "linkedQuantity": 50.0
  }'
```

**Response (404 Not Found):**
```json
{
  "timestamp": "2026-06-30T10:50:00",
  "status": 404,
  "error": "Not Found",
  "message": "Sale not found with id: non-existent-sale-id",
  "path": "/api/v1/links"
}
```

---

## Complete Workflow Example

### Step 1: Get all unlinked sales
```bash
curl -X GET "http://localhost:8081/api/v1/sales/allSales?status=CONFIRMED" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Step 2: Get available purchases (with available quantity)
```bash
curl -X GET "http://localhost:8081/api/v1/purchase/allPurchase?status=CONFIRMED" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Step 3: For each PO, check how much is already committed
```bash
curl -X GET "http://localhost:8081/api/v1/links/purchase/PURCHASE-UUID" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Step 4: Create link
```bash
curl -X POST http://localhost:8081/api/v1/links \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "saleId": "sale-uuid",
    "purchaseId": "purchase-uuid",
    "linkedQuantity": 50.0
  }'
```

### Step 5: Verify the sale summary
```bash
curl -X GET "http://localhost:8081/api/v1/links/sale/SALE-UUID" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Required Permissions

| Endpoint | Permission Required | Notes |
|---|---|---|
| `POST /api/v1/links` | `SALE_EDIT` | Create link |
| `PUT /api/v1/links/{id}` | `SALE_EDIT` | Update link |
| `DELETE /api/v1/links/{id}` | `SALE_EDIT` | Delete link |
| `GET /api/v1/links/sale/{saleId}` | `SALE_VIEW` | View sale summary |
| `GET /api/v1/links/purchase/{purchaseId}` | `PURCHASE_VIEW` | View purchase summary |

**Note:** Your JWT token must include these authorities/permissions for the API calls to succeed. If you get a `403 Forbidden` error, check that your user's role has these permissions enabled.

---

## Frontend Integration Tips

### 1. Building a "Link PO to Sale" UI

**Recommended flow:**
1. Display sale details (total requirement, already linked, remaining)
2. Show available POs with their available quantities
3. Let user enter quantity (validate on client side: must be ≤ min(PO available, sale remaining))
4. Call `POST /api/v1/links`
5. Refresh both sale and PO summaries

### 2. Showing Sale Fulfillment Progress

```javascript
// Pseudo-code for React/Vue
const saleSummary = await fetch(`/api/v1/links/sale/${saleId}`);
const fulfillmentPercent = (saleSummary.totalLinked / saleSummary.totalRequired) * 100;

// Show progress bar
<ProgressBar 
  value={fulfillmentPercent} 
  label={`${saleSummary.totalLinked}MT / ${saleSummary.totalRequired}MT`}
/>
```

### 3. Showing PO Utilization

```javascript
const poSummary = await fetch(`/api/v1/links/purchase/${purchaseId}`);
const utilizationPercent = (poSummary.totalLinked / poSummary.originalQuantity) * 100;

// Color code: green if < 80%, yellow if 80-95%, red if > 95%
```

---

## Database Table Structure (Auto-created)

When you start the application, Hibernate will auto-create this table:

```sql
CREATE TABLE sale_purchase_links (
    id VARCHAR(36) PRIMARY KEY,
    sale_id VARCHAR(36) NOT NULL,
    purchase_id VARCHAR(36) NOT NULL,
    linked_quantity DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_sale_purchase UNIQUE (sale_id, purchase_id)
);
```

**Key points:**
- `UNIQUE (sale_id, purchase_id)` prevents duplicate links
- Foreign keys are NOT enforced at DB level (Spring manages relationships)
- Quantities are stored as `DOUBLE` (matches Java `Double` type)
