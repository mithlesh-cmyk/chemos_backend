# Vessel Stock Stats API — Frontend Integration Guide

Read-only endpoints for the Inventory Command Centre dashboard: the 4 summary cards and the item breakdown table.

Base URL: `http://<host>:8081/api/v1/stock-stats`

## Authentication

All endpoints require a Bearer token from the existing login endpoint.

```
POST /api/v1/auth/login
Content-Type: application/json

{ "username": "...", "password": "..." }
```

Response includes a `token` field. Send it on every request:

```
Authorization: Bearer <token>
```

The logged-in user's role must have the `STOCK_STATS_VIEW` permission, or these endpoints return `403 Forbidden`.

## Field glossary (shared across all 3 endpoints)

| Field | Meaning |
|---|---|
| `physicalStockOpening` | Physical warehouse stock currently on hand (from the physical stock import) |
| `physicalSold` | Physical ("Ready Market") stock sold today |
| `physicalUnsoldClosing` | `physicalStockOpening − physicalSold` |
| `incomingUnsoldOpening` | Incoming unsold balance carried forward from yesterday's closing |
| `incomingUnsoldNew` | New "Incoming" purchases booked today |
| `incomingSold` | "Incoming" stock sold today |
| `incomingUnsoldClosing` | `incomingUnsoldOpening + incomingUnsoldNew − incomingSold` |
| `totalStock` | `physicalUnsoldClosing + incomingUnsoldClosing` |

All values are in MT (metric tons), as `Double`. "Today" is the business day in `Asia/Kolkata`.

---

## 1. `GET /api/v1/stock-stats/summary`

**Use for:** the 4 top cards (TOTAL STOCK, PHYSICAL UNSOLD CLOSING, INCOMING UNSOLD, INCOMING SOLD).

Query params (both optional):

| Param | Type | Description |
|---|---|---|
| `vesselName` | string | Filter to a single vessel. Case/whitespace-insensitive. |
| `product` | string | Filter to a single product. Case/whitespace-insensitive. |

No params → totals across everything.

**Request**
```
GET /api/v1/stock-stats/summary
GET /api/v1/stock-stats/summary?product=TOLUENE
GET /api/v1/stock-stats/summary?vesselName=SEA FALCON&product=TOLUENE
```

**Response** `200 OK`
```json
{
  "totalStock": 150.0,
  "physicalUnsoldClosing": 150.0,
  "incomingUnsoldClosing": 0.0,
  "incomingSold": 0.0
}
```

If nothing matches the filter, all fields are `0.0` (not an error).

---

## 2. `GET /api/v1/stock-stats/by-product`

**Use for:** the item breakdown table — one row per (product, port) combination.

No query params.

**Request**
```
GET /api/v1/stock-stats/by-product
```

**Response** `200 OK`
```json
[
  {
    "product": "TOLUENE",
    "port": "KAOHSIUNG",
    "physicalStockOpening": 150.0,
    "physicalSold": 0.0,
    "physicalUnsoldClosing": 150.0,
    "incomingUnsoldOpening": 0.0,
    "incomingUnsoldNew": 0.0,
    "incomingSold": 0.0,
    "incomingUnsoldClosing": 0.0,
    "totalStock": 150.0,
    "vesselInventory": [
      {
        "vesselName": "SEA FALCON",
        "eta": "2026-06-20",
        "inventoryDays": 11
      }
    ]
  }
]
```

`product`/`port` are uppercased and trimmed (grouping is normalized internally), so display them as-is or re-case on the frontend if a specific casing is wanted.

`vesselInventory` — one entry per physical-stock record (i.e. per vessel/purchase) contributing to this product+port, sourced from the `physical_stocks` table joined to its purchase:
- `vesselName` — the purchase's vessel name.
- `eta` — the date the physical stock record was last updated (`physical_stocks.updated_at`), used as the stock's "arrival" date.
- `inventoryDays` — `today − eta` in days (business day, `Asia/Kolkata`). Sorted ascending by `eta`. Empty array if no physical stock is recorded for that product+port.

---

## 3. `GET /api/v1/stock-stats`

**Use for:** per-vessel breakdown (debugging/audit — drill-down if you ever need vessel-level detail instead of product-level).

No query params.

**Request**
```
GET /api/v1/stock-stats
```

**Response** `200 OK`
```json
[
  {
    "vesselName": "SEA FALCON",
    "product": "TOLUENE",
    "port": "KAOHSIUNG",
    "physicalStockOpening": 150.0,
    "physicalSold": 0.0,
    "physicalUnsoldClosing": 150.0,
    "incomingUnsoldOpening": 0.0,
    "incomingUnsoldNew": 0.0,
    "incomingSold": 0.0,
    "incomingUnsoldClosing": 0.0,
    "totalStock": 150.0
  }
]
```

---

## Notes

- All three endpoints are computed from the same underlying data, so `summary`'s totals always equal the sum of `by-product`'s rows, which always equal the sum of the vessel-level `/stock-stats` rows.
- Only `CONFIRMED` purchases/sales are counted (draft/unconfirmed entries don't affect these numbers).
- `incomingUnsoldOpening`/`incomingUnsoldClosing` roll over at midnight IST via a nightly job — numbers are still correct intraday even before that job runs, they just won't be "locked in" as tomorrow's opening until it does.
- Errors: `401 Unauthorized` (missing/invalid token), `403 Forbidden` (valid token, missing `STOCK_STATS_VIEW` permission).
