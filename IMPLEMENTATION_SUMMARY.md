# Sale-Purchase Link Implementation - Summary

## What Does "Permissions/Authorities" Mean?

In your Spring Boot app, `@PreAuthorize("hasAuthority('SALE_EDIT')")` is a **security gate** that checks:
> "Does the logged-in user have the 'SALE_EDIT' permission?"

**How I implemented it:**
```java
// When creating/updating/deleting links:
@PreAuthorize("hasAuthority('SALE_EDIT')")   // ← Uses existing permission

// When viewing sale summary:
@PreAuthorize("hasAuthority('SALE_VIEW')")

// When viewing purchase summary:
@PreAuthorize("hasAuthority('PURCHASE_VIEW')")
```

**Why these permissions?**
- Salespeople who can **edit sales** should be able to **link POs to sales** → `SALE_EDIT`
- Anyone who can **view sales** can **see which POs are linked** → `SALE_VIEW`
- Anyone who can **view purchases** can **see how PO quantity is distributed** → `PURCHASE_VIEW`

**Alternative approach (if you want dedicated permissions):**
You could create NEW permissions like `LINK_CREATE`, `LINK_VIEW`, `LINK_DELETE` in your auth system, but that requires updating your User/Role tables. I went with **reusing existing permissions** for simplicity.

---

## How the System Works

### Data Flow

```
┌─────────────────────────────────────────────────────────────────┐
│  PURCHASES TABLE                  SALES_FORM TABLE              │
│  ┌───────────┐                    ┌───────────┐                │
│  │ PO-1      │                    │ SALE-1    │                │
│  │ Qty: 100MT│                    │ Qty: 140MT│                │
│  └─────┬─────┘                    └─────┬─────┘                │
│        │                                │                       │
│        │         ┌────────────────┐     │                       │
│        └────────►│  LINK-A        │◄────┘                       │
│                  │  Qty: 100MT    │                             │
│                  └────────────────┘                             │
│                                                                  │
│  Result after Link-A:                                           │
│  - PO-1 available: 100 - 100 = 0MT                             │
│  - SALE-1 remaining: 140 - 100 = 40MT                          │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  Now add another PO to fulfill the remaining 40MT               │
│                                                                  │
│  ┌───────────┐                    ┌───────────┐                │
│  │ PO-2      │                    │ SALE-1    │                │
│  │ Qty: 80MT │                    │ Qty: 140MT│                │
│  └─────┬─────┘                    └─────┬─────┘                │
│        │                                │                       │
│        │         ┌────────────────┐     │                       │
│        └────────►│  LINK-B        │◄────┘                       │
│                  │  Qty: 40MT     │                             │
│                  └────────────────┘                             │
│                                                                  │
│  Result after Link-B:                                           │
│  - PO-2 available: 80 - 40 = 40MT (can be used for other sales)│
│  - SALE-1 remaining: 140 - 140 = 0MT (fully fulfilled!)        │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  A single PO can fulfill multiple sales                         │
│                                                                  │
│  ┌───────────┐         ┌────────────────┐         ┌──────────┐ │
│  │ PO-2      │────────►│  LINK-B        │────────►│ SALE-1   │ │
│  │ Qty: 80MT │         │  Qty: 40MT     │         │ Qty: 140 │ │
│  └───────────┘         └────────────────┘         └──────────┘ │
│       │                                                          │
│       │                ┌────────────────┐         ┌──────────┐ │
│       └───────────────►│  LINK-C        │────────►│ SALE-2   │ │
│                        │  Qty: 30MT     │         │ Qty: 80  │ │
│                        └────────────────┘         └──────────┘ │
│                                                                  │
│  Result:                                                        │
│  - PO-2 committed: 40 + 30 = 70MT                              │
│  - PO-2 available: 80 - 70 = 10MT (still available!)           │
└─────────────────────────────────────────────────────────────────┘
```

---

## Files Created (9 files)

### Backend Code (Java)
1. **`SalePurchaseLink.java`** (Entity)  
   → Maps to `sale_purchase_links` table (auto-created by Hibernate)

2. **`SalePurchaseLinkRepository.java`** (JPA Repository)  
   → Has aggregate queries: `sumLinkedQuantityByPurchaseId()`, `sumLinkedQuantityBySaleId()`

3. **`SalePurchaseLinkService.java`** (Business Logic)  
   → All validation & quantity calculations

4. **`SalePurchaseLinkController.java`** (REST API)  
   → 5 endpoints (create, update, delete, get sale summary, get purchase summary)

### DTOs (5 records)
5. **`CreateSalePurchaseLinkRequest.java`** → `{ saleId, purchaseId, linkedQuantity }`
6. **`UpdateSalePurchaseLinkRequest.java`** → `{ linkedQuantity }`
7. **`SalePurchaseLinkResponse.java`** → Single link with computed quantities
8. **`SaleLinkSummaryResponse.java`** → Sale view (total needed, total linked, remaining)
9. **`PurchaseLinkSummaryResponse.java`** → PO view (total, committed, available)

---

## API Endpoints (5 endpoints)

| # | Method | URL | Permission | Purpose |
|---|---|---|---|---|
| 1 | POST | `/api/v1/links` | `SALE_EDIT` | Create a new link |
| 2 | PUT | `/api/v1/links/{id}` | `SALE_EDIT` | Update linked quantity |
| 3 | DELETE | `/api/v1/links/{id}` | `SALE_EDIT` | Remove a link |
| 4 | GET | `/api/v1/links/sale/{saleId}` | `SALE_VIEW` | Get sale summary with all its PO links |
| 5 | GET | `/api/v1/links/purchase/{purchaseId}` | `PURCHASE_VIEW` | Get PO summary with all its sale links |

---

## Testing Resources (4 files for frontend team)

### 1. **`SALE_PURCHASE_LINK_API_EXAMPLES.md`** (Comprehensive guide)
   - Detailed explanations
   - CURL examples for all 5 endpoints
   - Error scenario examples
   - Frontend integration tips

### 2. **`Sale_Purchase_Links.postman_collection.json`** (Postman Collection)
   - Import into Postman for instant testing
   - Pre-configured requests
   - Variables for easy customization

### 3. **`test_links_api.sh`** (Bash script for Linux/Mac)
   - Quick test script
   - Just update variables and run

### 4. **`test_links_api.ps1`** (PowerShell script for Windows)
   - Same as above, but for Windows

---

## Quick Example

### Scenario: Sale needs 140MT, link two POs to fulfill it

**Step 1: Create first link (PO-1 has 100MT)**
```bash
curl -X POST http://localhost:8081/api/v1/links \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "saleId": "sale-uuid-123",
    "purchaseId": "po-uuid-456",
    "linkedQuantity": 100.0
  }'
```

**Response:**
```json
{
  "id": "link-uuid-aaa",
  "linkedQuantity": 100.0,
  "purchaseAvailableQuantity": 0.0,    ← PO fully used
  "saleRemainingQuantity": 40.0,       ← Sale still needs 40MT
  ...
}
```

**Step 2: Create second link (PO-2 has 80MT, only need 40MT)**
```bash
curl -X POST http://localhost:8081/api/v1/links \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "saleId": "sale-uuid-123",
    "purchaseId": "po-uuid-789",
    "linkedQuantity": 40.0
  }'
```

**Response:**
```json
{
  "id": "link-uuid-bbb",
  "linkedQuantity": 40.0,
  "purchaseAvailableQuantity": 40.0,   ← PO-2 still has 40MT for other sales
  "saleRemainingQuantity": 0.0,        ← Sale fully fulfilled! ✅
  ...
}
```

**Step 3: View complete sale summary**
```bash
curl -X GET http://localhost:8081/api/v1/links/sale/sale-uuid-123 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Response:**
```json
{
  "saleId": "sale-uuid-123",
  "totalRequired": 140.0,
  "totalLinked": 140.0,      ← Fully linked
  "remaining": 0.0,          ← Nothing left to fulfill
  "links": [
    {
      "linkId": "link-uuid-aaa",
      "purchaseId": "po-uuid-456",
      "linkedQuantity": 100.0,
      "purchaseOriginalQuantity": 100.0,
      "purchaseAvailableQuantity": 0.0
    },
    {
      "linkId": "link-uuid-bbb",
      "purchaseId": "po-uuid-789",
      "linkedQuantity": 40.0,
      "purchaseOriginalQuantity": 80.0,
      "purchaseAvailableQuantity": 40.0
    }
  ]
}
```

---

## Error Prevention

The service automatically validates:

✅ **Duplicate prevention** → Can't link same PO to same sale twice  
✅ **Over-commitment prevention** → Can't commit more than PO has available  
✅ **Over-fulfillment prevention** → Can't exceed sale's remaining requirement  
✅ **Zero/negative prevention** → Linked quantity must be positive  

**Example error message:**
```json
{
  "status": 400,
  "message": "Linked quantity (150.00) exceeds available PO quantity (20.00). The PO has 80.00 already committed to other sales."
}
```

---

## To Start Testing

1. **Start your Spring Boot app:**
   ```bash
   .\mvnw.cmd spring-boot:run
   ```

2. **Login to get JWT token:**
   ```bash
   curl -X POST http://localhost:8081/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username": "your_user", "password": "your_pass"}'
   ```

3. **Get some sale and purchase IDs:**
   ```bash
   # Get sales
   curl -X GET "http://localhost:8081/api/v1/sales/allSales" \
     -H "Authorization: Bearer YOUR_TOKEN"
   
   # Get purchases
   curl -X GET "http://localhost:8081/api/v1/purchase/allPurchase" \
     -H "Authorization: Bearer YOUR_TOKEN"
   ```

4. **Create your first link** using the CURL examples above!

---

## Questions?

- **Q: Do I need to manually create the database table?**  
  A: No! Hibernate auto-creates `sale_purchase_links` table on startup.

- **Q: What happens if I delete a link?**  
  A: The committed quantity is released back to both the PO and sale.

- **Q: Can I link the same PO to the same sale twice?**  
  A: No, the system prevents duplicates. Use PUT to update the existing link.

- **Q: What if my user doesn't have SALE_EDIT permission?**  
  A: You'll get `403 Forbidden`. Check your user's role/permissions in the auth system.

---

✅ **Implementation Complete**  
✅ **Compilation Successful (109 source files)**  
✅ **No Errors**  
✅ **Ready for Testing**
