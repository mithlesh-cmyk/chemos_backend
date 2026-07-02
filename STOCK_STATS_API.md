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
    "dischargePort": "KAOHSIUNG",
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
        "inventoryDays": 11,
        "companyFrom": "Our Company Ltd"
      }
    ]
  }
]
```

`product`/`dischargePort` are uppercased and trimmed (grouping is normalized internally), so display them as-is or re-case on the frontend if a specific casing is wanted.

`dischargePort` is the purchase's **discharge port** (`purchases.discharge_ports`) — the port the vessel unloads at and where the stock physically sits. This is deliberately not the purchase's `port` column, which is the *load* (origin) port; grouping by load port would scatter one physical stock lot across whatever port it was bought from instead of where it actually is.

`vesselInventory` — one entry per physical-stock record (i.e. per vessel/purchase) contributing to this product+dischargePort, sourced from the `physical_stocks` table joined to its purchase:
- `vesselName` — the purchase's vessel name.
- `eta` — the date the physical stock record was last updated (`physical_stocks.updated_at`), used as the stock's "arrival" date.
- `inventoryDays` — `today − eta` in days (business day, `Asia/Kolkata`). Sorted ascending by `eta`. Empty array if no physical stock is recorded for that product+dischargePort.
- `companyFrom` — the purchase's `company_from` (the selling/supplying company this stock lot was bought from).

**Why does `vesselInventory` sometimes have more than one entry?** Each entry is one *confirmed purchase* (one physical-stock row), not one product. If two different vessels both brought the same product into the same discharge port — e.g. two separate confirmed purchases of `MIXED XYLENE (SOLVENT)` arriving at `YEOSU PORT`, one via `MARITIME AMITY` and one via `STENA PROGRESSIVE` — that's two rows in `physical_stocks`, so `vesselInventory` has two objects. The parent object's `physicalStockOpening`/`totalStock` etc. are already the *sum* across all of them; `vesselInventory` is the itemized breakdown of what makes up that sum.

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
    "dischargePort": "KAOHSIUNG",
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

---

## How it works internally

Everything lives in `VesselStockStatsService` (`src/main/java/chemos/chem_os/services/VesselStockStatsService.java`). All three endpoints are read-only views built from the same core computation, `computeGroupStats()` — nothing is precomputed or cached except the one snapshot table described below.

### The group key

Every number in this API is a sum over some (vessel, product, discharge port) bucket. That triple is captured as a private record inside the service:

```java
private record GroupKey(String vesselName, String product, String dischargePort) {}
```

This is the atomic grouping unit. `/api/v1/stock-stats` returns one row per `GroupKey`. The other two endpoints just aggregate `GroupKey` rows further (see below).

Keys are normalized before grouping — uppercased and trimmed (`UPPER(TRIM(...))`) — both in the JPQL queries and in the `normalize()` helper used for the `summary` filters. This means `"Toluene"`, `" TOLUENE "`, and `"TOLUENE"` all land in the same bucket, so inconsistent data entry doesn't silently create duplicate rows.

### `computeGroupStats()` — the compute function

This is the one function that produces every `GroupKey` row. It works by pulling four independent aggregates, each already grouped by `GroupKey` at the SQL/JPQL level, then combining them in Java:

| Source map | Repository call | What it sums |
|---|---|---|
| `physicalOpeningByGroup` | `physicalStockRepository.sumPhysicalStockOpeningByGroup()` | `physical_stocks.physical_stock` for all CONFIRMED purchases, per group |
| `physicalSoldByGroup` | `salesRepository.sumReadyMarketSoldByGroup(today)` | Today's CONFIRMED "Ready Market" sales quantity, per group |
| `incomingNewByGroup` | `purchaseRepository.sumIncomingNewByGroup(today)` | Today's CONFIRMED "Incoming" purchases quantity, per group |
| `incomingSoldByGroup` | `salesRepository.sumIncomingSoldByGroup(today)` | Today's CONFIRMED "Incoming" sales quantity, per group |

Each of these is a JPA `@Query` that returns a flat `List<VesselStockGroupAggregate>` (just `vesselName`, `product`, `dischargePort`, `total`), which `toMap()` folds into a `Map<GroupKey, Double>`.

The union of all four maps' keys (`allGroups`, a `LinkedHashSet` so iteration order is stable/deterministic) is the full set of groups that exist *today*, even if a group has stock but no sales, or sales but no stock. For each group, the four numbers are combined with plain arithmetic — no aggregation happens outside SQL, only combination:

```
physicalUnsoldClosing = physicalStockOpening − physicalSold
incomingUnsoldClosing = incomingUnsoldOpening + incomingUnsoldNew − incomingSold
totalStock             = physicalUnsoldClosing + incomingUnsoldClosing
```

`incomingUnsoldOpening` is the only field that doesn't come from today's live tables — see the snapshot section below.

### Why the discharge port, not the load port

`Purchase` has two port columns: `port` (where the goods were loaded/bought — column `port`) and `dischargePort` (where the vessel unloads — column `discharge_ports`). All the grouping queries join on `p.dischargePort.displayName`, because "where is this stock physically sitting" is a discharge-port question, not a load-port question. `Sales` only has a single `port` column, which already represents the sale's destination, so no equivalent split exists there.

### The three endpoints, in terms of `GroupKey`

- **`GET /stock-stats`** — returns the `GroupKey` rows as-is (`getStats()` → `computeGroupStats()`, no further aggregation). This is the finest-grained view; useful for debugging a specific vessel/product/port combination.
- **`GET /stock-stats/summary`** — filters the `GroupKey` rows by optional `vesselName`/`product`, then sums 4 fields across whatever survives the filter (`getSummary()`). No grouping, just a filtered total.
- **`GET /stock-stats/by-product`** — re-groups the `GroupKey` rows by a coarser key, `ProductPortKey(product, dischargePort)` (i.e. drops `vesselName` from the key), and sums every numeric field across all vessels that share a product+port (`getProductBreakdown()`, via `Collectors.groupingBy`). This is why a single row here can represent multiple vessels: it's the sum of every `GroupKey` row that shares that product+port.

  Separately (not from `computeGroupStats()`), it also fetches `physicalStockRepository.findVesselInventoryRows()` — one raw row per physical-stock record, each carrying its own vessel/eta/company — and groups *those* by the same `ProductPortKey`. That's the `vesselInventory` array: the itemized list of individual purchase lots that were summed together to produce the row's totals. This is also the direct answer to "why does `vesselInventory` sometimes have 2+ objects" — it's not a bug or a merge artifact, it's one entry per underlying confirmed purchase/physical-stock record; a product+port combination fed by two different vessel deliveries will always show two entries there.

### The nightly snapshot (why `incomingUnsoldOpening` isn't computed live)

"Incoming" stock rolls day-to-day: today's closing balance must become tomorrow's opening balance, and that can't be recomputed from live tables at query time without arbitrarily picking a reference date. So a `@Scheduled` job (`runNightlySnapshot()`, cron `0 30 0 * * *` Asia/Kolkata — 12:30 AM IST) locks in each group's incoming numbers once per day into the `incoming_unsold_snapshots` table (`IncomingUnsoldSnapshot` entity, unique on `snapshot_date + vessel_name + product + port`).

At query time, `resolveIncomingOpening(key, today)` looks up the most recent snapshot *before* today for that `GroupKey` and uses its `incomingUnsoldClosing` as today's opening (defaulting to `0.0` if none exists — e.g. a brand-new group). This is why the docs note intraday numbers are "still correct... they just won't be locked in until the job runs" — the live computation always works, the snapshot just avoids recomputing history from scratch and gives each day a durable, auditable opening balance.

### Data flow, end to end

```
physical_stocks + purchases  ─┐
sales (Ready Market)          ├─▶ 4 grouped SQL aggregates ─▶ computeGroupStats() ─▶ List<GroupKey row>
purchases (Incoming)          │        (per GroupKey)              │
sales (Incoming)              │                                    │
incoming_unsold_snapshots ────┘ (yesterday's closing, via          ├─▶ /stock-stats            (as-is)
                                  resolveIncomingOpening)           ├─▶ /stock-stats/summary    (filter + sum)
                                                                     └─▶ /stock-stats/by-product (re-group by product+port
                                                                                                    + join vesselInventory)
```
