# ChemOS - Project Architecture & Database Documentation

## Table of Contents
1. [Project Overview](#project-overview)
2. [Technology Stack](#technology-stack)
3. [Architecture Overview](#architecture-overview)
4. [Database Schema](#database-schema)
5. [Table Relationships](#table-relationships)
6. [Security & Authentication](#security--authentication)
7. [Module Breakdown](#module-breakdown)

---

## Project Overview

**ChemOS** is a comprehensive Chemical Operations Management System designed for managing chemical trading operations including purchases, sales, inventory tracking, P&L calculations, and financial analytics. The system supports role-based access control with fine-grained permissions and two-factor authentication.

### Key Features
- **Purchase Order Management** - Track imports, local purchases, and vessel-based procurement
- **Sales Order Management** - Manage sales, inquiries, and customer orders
- **Inventory Tracking** - Physical stock management with vessel-wise tracking
- **Sale-Purchase Linking** - Link sales to purchases for P&L calculation
- **Financial Analytics** - Cost tracking, revenue tracking, and P&L analysis
- **Audit Logging** - Complete audit trail of all operations
- **RBAC with 2FA** - Role-based permissions with two-factor authentication
- **CSV Import/Export** - Bulk operations for stock, costs, and revenue data

---

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.x
- **Language**: Java 17+
- **Database**: PostgreSQL (AWS RDS)
- **ORM**: JPA/Hibernate
- **Security**: Spring Security + JWT
- **2FA**: TOTP (Time-based One-Time Password)

### Infrastructure
- **Cloud**: AWS (RDS PostgreSQL)
- **Server Port**: 8081
- **Timezone**: Asia/Kolkata

### Key Dependencies
- Lombok - Reduce boilerplate code
- Jakarta Validation - Request validation
- Jackson - JSON processing
- Spring Data JPA - Database operations

---

## Architecture Overview

### Layer Architecture

```
┌─────────────────────────────────────────────────┐
│           Presentation Layer                    │
│  (REST Controllers - API Endpoints)             │
├─────────────────────────────────────────────────┤
│           Security Layer                        │
│  (JWT Auth, 2FA, Permission Checks)            │
├─────────────────────────────────────────────────┤
│           Service Layer                         │
│  (Business Logic, Validation, Audit)           │
├─────────────────────────────────────────────────┤
│           Repository Layer                      │
│  (JPA Repositories - Data Access)              │
├─────────────────────────────────────────────────┤
│           Database Layer                        │
│  (PostgreSQL - AWS RDS)                        │
└─────────────────────────────────────────────────┘
```

### Package Structure

```
chemos.chem_os/
├── auth/                           # Authentication & Authorization Module
│   ├── config/                     # Security configuration
│   ├── controller/                 # Auth endpoints
│   ├── dto/                        # Auth DTOs
│   ├── model/                      # User, Role, Permission entities
│   ├── repository/                 # Auth repositories
│   ├── security/                   # JWT, filters, handlers
│   └── service/                    # Auth business logic
│
├── controller/                     # Main business controllers
│   ├── AuditController            # Audit log access
│   ├── CompanyController          # Company management
│   ├── CostCsvController          # Cost data CSV operations
│   ├── CountryController          # Country master data
│   ├── MarketStatusController     # Market status options
│   ├── PaymentTermController      # Payment terms master
│   ├── PlController               # P&L CSV operations
│   ├── PortController             # Port master data
│   ├── PortTransitDaysController  # Transit days between ports
│   ├── ProductController          # Product master data
│   ├── PurchaseController         # Purchase order CRUD
│   ├── RevenueCsvController       # Revenue CSV operations
│   ├── SalePurchaseLinkController # Link sales to purchases
│   ├── SalesController            # Sales order CRUD
│   ├── SalespersonController      # Salesperson master
│   └── VesselStockStatsController # Stock statistics & analytics
│
├── dto/                            # Data Transfer Objects
├── exception/                      # Custom exceptions
├── mapper/                         # Entity-DTO mappers
├── model/                          # JPA entities (database tables)
├── repository/                     # Spring Data JPA repositories
└── services/                       # Business logic services
```

### Request Flow

```
Client Request (with JWT Token)
        ↓
Security Filter (JWT validation, 2FA check)
        ↓
Controller (Request mapping, validation)
        ↓
Service Layer (Business logic, permission checks)
        ↓
Audit Service (Log operation)
        ↓
Repository (Database operations)
        ↓
Database (PostgreSQL)
        ↓
Response (JSON)
```

---

## Database Schema

### Overview
The database consists of **30+ tables** organized into functional modules:
- **Authentication & Security** (6 tables)
- **Master Data** (8 tables)
- **Transactional Data** (7 tables)
- **Analytics & Tracking** (6 tables)
- **Audit & Logs** (3 tables)

---

## Detailed Table Documentation

### 1. Authentication & Security Module

#### **users**
Stores user accounts with credentials and role assignments.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique user identifier |
| username | VARCHAR | UNIQUE, NOT NULL | Login username |
| password | VARCHAR | NOT NULL | BCrypt hashed password |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | Account active status |
| name | VARCHAR | NULL | User's full name |
| email | VARCHAR | NULL | User's email address |
| role_id | VARCHAR | FK → roles.id, NOT NULL | Assigned role |

**Purpose**: Core user authentication and account management.

**Relationships**:
- Many-to-One with `roles` (one user has one role)
- One-to-One with `two_factor_credentials` (optional 2FA)
- One-to-Many with `backup_codes` (for 2FA recovery)
- One-to-Many with `user_permission_restrictions` (permission overrides)

---

#### **roles**
Defines hierarchical roles with permission inheritance.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | VARCHAR | PK | Role identifier (e.g., 'admin', 'pur_man') |
| name | VARCHAR | UNIQUE, NOT NULL | Role name (e.g., 'ADMIN') |
| display_name | VARCHAR | NULL | Display name (e.g., 'Administrator') |
| is_super_role | BOOLEAN | NOT NULL, DEFAULT FALSE | Bypass permission checks |
| parent_role_id | VARCHAR | FK → roles.id, NULL | Parent for inheritance |
| restrict_to_own_records | BOOLEAN | NOT NULL, DEFAULT FALSE | Row-level security flag |

**Purpose**: Define organizational roles with permission inheritance.

**Role Hierarchy Example**:
```
ADMIN (super_role=true)
  ├── PURCHASE_MANAGER
  │     └── PURCHASE_EXECUTIVE (restrict_to_own_records=true)
  └── SALES_MANAGER
        └── SALES_EXECUTIVE (restrict_to_own_records=true)
```

**Relationships**:
- Self-referencing (parent_role_id → roles.id)
- Many-to-Many with `permissions` through `role_permissions`

---

#### **permissions**
Granular permissions for different operations.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | VARCHAR | PK | Permission ID (e.g., 'sale_view') |
| permission_code | VARCHAR | UNIQUE, NOT NULL | Code (e.g., 'SALE_VIEW') |
| display_name | VARCHAR | NOT NULL | Human-readable name |
| module | VARCHAR | NOT NULL | Module grouping (SALES, PURCHASES, etc.) |

**Permission Types**:
- **PURCHASE_VIEW** - View purchase orders
- **PURCHASE_CREATE** - Create purchase orders
- **PURCHASE_EDIT** - Edit purchase orders
- **PURCHASE_APPROVE** - Confirm/cancel purchases
- **SALE_VIEW** - View sales orders
- **SALE_CREATE** - Create sales orders
- **SALE_EDIT** - Edit sales orders
- **SALE_APPROVE** - Confirm/cancel sales
- **USER_MANAGEMENT** - Manage users and roles
- **STOCK_STATS_VIEW** - View stock statistics

**Relationships**:
- Many-to-Many with `roles` through `role_permissions`

---

#### **role_permissions** (Join Table)
Links roles to permissions.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| role_id | VARCHAR | FK → roles.id | Role identifier |
| permission_id | VARCHAR | FK → permissions.id | Permission identifier |

**Purpose**: Many-to-many relationship between roles and permissions.

---

#### **user_permission_restrictions**
Override individual user permissions beyond their role.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique restriction ID |
| user_id | UUID | FK → users.id | User being restricted |
| permission_id | VARCHAR | FK → permissions.id | Permission being overridden |
| allowed | BOOLEAN | NOT NULL | True = grant, False = deny |

**Purpose**: Allow/deny specific permissions for individual users without changing their role.

**Use Case**: A manager delegates SALE_EDIT to a specific executive temporarily.

---

#### **two_factor_credentials**
TOTP-based two-factor authentication credentials.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique credential ID |
| user_id | UUID | FK → users.id, UNIQUE | User account |
| encrypted_secret | TEXT | NULL | AES-encrypted TOTP secret |
| enabled | BOOLEAN | NOT NULL, DEFAULT FALSE | 2FA enabled flag |
| enrolled_at | TIMESTAMP | NULL | Enrollment completion time |
| last_verified_at | TIMESTAMP | NULL | Last successful verification |
| failed_attempts | INTEGER | NOT NULL, DEFAULT 0 | Failed login attempts |
| locked_until | TIMESTAMP | NULL | Lockout expiration time |
| last_used_code | VARCHAR | NULL | Prevent code reuse |
| last_code_used_at | TIMESTAMP | NULL | Code usage timestamp |

**Purpose**: Secure TOTP-based 2FA with lockout protection.

**Security Features**:
- AES-256-GCM encrypted secrets
- Code reuse prevention
- Automatic lockout after 5 failed attempts
- 15-minute lockout duration

---

#### **backup_codes**
One-time backup codes for 2FA recovery.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique code ID |
| user_id | UUID | FK → users.id | User account |
| code_hash | VARCHAR | NOT NULL | BCrypt hashed backup code |
| used | BOOLEAN | NOT NULL, DEFAULT FALSE | Used flag |
| used_at | TIMESTAMP | NULL | Usage timestamp |

**Purpose**: Allow users to login when they lose their authenticator device.

**Note**: Each user gets 10 backup codes during 2FA enrollment.

---

### 2. Master Data Module

#### **companies**
Company directory for buyers, sellers, and partners.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | VARCHAR | PK | Auto-generated company ID |
| display_name | VARCHAR | NULL | Full company name |
| search_key | VARCHAR | NULL | Normalized name for search |
| created_at | DATE | NULL | Creation date |

**Purpose**: Centralized company registry for purchase/sale parties.

**Usage**:
- Purchase: `company_to` (buyer), `company_from` (seller)
- Sales: `company_to` (customer), `company_from` (our company)

---

#### **products**
Chemical products catalog.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | VARCHAR | PK | Product identifier |
| name | VARCHAR | NOT NULL | Product name (e.g., 'Sulphuric Acid') |
| hs_code | VARCHAR | NULL | Harmonized System code for customs |
| cas_no | VARCHAR | NULL | Chemical Abstracts Service number |

**Purpose**: Master list of traded chemical products.

**Referenced By**:
- `purchases.product`
- `sales.product_id`

---

#### **countries**
Country master data.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | VARCHAR | PK | Country code |
| name | VARCHAR | NOT NULL | Country name |
| code | VARCHAR | NULL | ISO country code |

**Purpose**: Origin country for products in purchase orders.

**Referenced By**:
- `purchases.origin`

---

#### **ports**
Seaports and discharge locations.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | VARCHAR | PK | Auto-generated port ID |
| display_name | VARCHAR | NOT NULL | Port name |
| search_key | VARCHAR | UNIQUE, NOT NULL | Normalized search key |
| locode | VARCHAR | NULL | UN/LOCODE identifier |
| is_indian | BOOLEAN | NOT NULL, DEFAULT TRUE | Indian port flag |
| created_at | TIMESTAMP | NULL | Creation timestamp |
| updated_at | TIMESTAMP | NULL | Update timestamp |

**Purpose**: Port master for import/export operations.

**Referenced By**:
- `purchases.port` (loading port)
- `purchases.discharge_ports` (discharge port)
- `sales.port` (delivery port)
- `port_transit_days` (from/to ports)

---

#### **port_transit_days**
Shipping transit times between ports.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique entry ID |
| from_port_id | VARCHAR | FK → ports.id, NOT NULL | Origin port |
| to_port_id | VARCHAR | FK → ports.id, NOT NULL | Destination port |
| days | INTEGER | NOT NULL | Transit days |

**Purpose**: Calculate ETAs based on ETDs and port pairs.

**Use Case**: If ETD from Shanghai is June 1, and transit to Mumbai is 15 days, ETA is June 16.

---

#### **market_status**
Market status options for pricing.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | VARCHAR | PK | Status identifier |
| name | VARCHAR | NOT NULL | Status name (FIRM, SOFT, etc.) |

**Purpose**: Indicate pricing firmness in purchase/sale orders.

**Common Values**: FIRM, SOFT, INDICATIVE, PENDING

---

#### **payment_terms**
Standard payment terms.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | VARCHAR | PK | Term identifier |
| display_name | VARCHAR | NOT NULL | Term description |
| is_active | BOOLEAN | NOT NULL | Active flag |

**Purpose**: Standardized payment terms for purchases.

**Examples**: 'LC 30 days', 'LC 60 days', 'Advance Payment', 'CAD'

---

#### **statuses**
Universal status values for orders.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | VARCHAR | PK | Status identifier |
| name | VARCHAR | UNIQUE, NOT NULL | Status name |

**Status Lifecycle**:
```
PENDING → CONFIRMED → CANCELLED
    ↓
UNCONFIRMED (can go back to CONFIRMED)
```

**Purpose**: Track purchase and sales order states.

---

#### **salespersons**
Salesperson registry.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | VARCHAR | PK | Auto-generated ID |
| display_name | VARCHAR | NOT NULL | Salesperson name |
| search_key | VARCHAR | NULL | Normalized search key |
| created_at | TIMESTAMP | NULL | Creation timestamp |

**Purpose**: Track which salesperson handled each deal.

**Referenced By**:
- `sales.sales_person`

---

### 3. Transactional Data Module

#### **purchases**
Purchase orders (imports and local procurement).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | VARCHAR | PK | Auto-generated purchase ID (PO-XXX) |
| purchase_type | VARCHAR | NULL | IMPORT, LOCAL |
| company_to | VARCHAR | NULL | Buying company |
| company_from | VARCHAR | NULL | Selling company |
| product | VARCHAR | FK → products.id | Product being purchased |
| vessel_name | VARCHAR | NULL | Vessel/carrier name |
| shipment | VARCHAR | NULL | FCL, LCL, BULK |
| quantity | DOUBLE | NULL | Quantity in MT (metric tons) |
| price_fc | DECIMAL(19,4) | NULL | Price in foreign currency |
| currency | VARCHAR | NULL | Currency code |
| offer_usd | DECIMAL(19,4) | NULL | Price in USD |
| exchange_rate | DECIMAL(19,4) | NULL | USD to INR exchange rate |
| price_inr | DECIMAL(19,4) | NULL | Price in INR |
| delivery_term | VARCHAR | NULL | CIF, FOB, CFR, etc. |
| payment_days | INTEGER | NULL | Payment terms in days |
| port | VARCHAR | FK → ports.id | Loading port |
| market_price | DECIMAL(19,4) | NULL | Current market price |
| market_status | VARCHAR | NULL | FIRM, SOFT, etc. |
| replacement_cost | DECIMAL(19,4) | NULL | Replacement cost estimate |
| make | VARCHAR | NULL | Manufacturer/grade |
| packaging | VARCHAR | NULL | Packaging type |
| origin | VARCHAR | FK → countries.id | Country of origin |
| expense | DECIMAL(19,4) | NULL | Additional expenses |
| custom_duty | DECIMAL(19,4) | NULL | Customs duty percentage |
| sws | DECIMAL(19,4) | NULL | Social Welfare Surcharge |
| additional_charge | DECIMAL(19,4) | NULL | Additional charges |
| other_expense | DECIMAL(19,4) | NULL | Other expenses |
| discharge_ports | VARCHAR | FK → ports.id | Discharge port |
| price_type | VARCHAR | NULL | FIXED, VARIABLE |
| payment_term | VARCHAR | FK → payment_terms.id | Payment term |
| etd | DATE | NULL | Expected Time of Departure |
| eta | DATE | NULL | Expected Time of Arrival |
| status | VARCHAR | FK → statuses.id, NOT NULL | Order status |
| created_at | TIMESTAMP | NOT NULL | Creation timestamp |
| updated_at | TIMESTAMP | NULL | Last update timestamp |
| created_by | VARCHAR | NULL | Creator username |
| updated_by | VARCHAR | NULL | Last updater username |
| confirmed_at | TIMESTAMP | NULL | Confirmation timestamp |
| quantity_received | DOUBLE | NULL | Actual received quantity |
| pay_due_date | DATE | NULL | Payment due date |

**Indexes**:
- `idx_purchases_status_market_status_created_at` on (status, market_status, created_at)

**Purpose**: Track all purchase orders from suppliers.

**Business Rules**:
1. Only PENDING orders can be edited
2. CONFIRMED orders create inventory
3. Quantity received updates physical stock

**Referenced By**:
- `sale_purchase_links.purchase_id`
- `physical_stocks.purchase_id`

---

#### **sales**
Sales orders (customer orders and inquiries).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Auto-generated sales ID (SO-XXX) |
| date | DATE | NULL | Order date |
| updated_at | TIMESTAMP | NULL | Last update timestamp |
| sale_type | VARCHAR | NULL | BUY, SELL, INQUIRY |
| company_to | VARCHAR | NULL | Customer company |
| company_from | VARCHAR | NULL | Our company |
| product_id | VARCHAR | FK → products.id | Product being sold |
| quantity | DOUBLE | NULL | Quantity in MT |
| price | DOUBLE | NULL | Sale price per MT |
| payment_term | VARCHAR | NULL | Payment terms |
| delivery_term | VARCHAR | NULL | Delivery terms (CFR, FOB, etc.) |
| port | VARCHAR | FK → ports.id | Delivery port |
| market_price | DOUBLE | NULL | Current market price |
| market_status | VARCHAR | NULL | FIRM, SOFT, etc. |
| storage_days | INTEGER | NULL | Storage period allowed |
| make | VARCHAR | NULL | Product grade/manufacturer |
| packaging | VARCHAR | NULL | Packaging type |
| origin | VARCHAR | NULL | Country of origin |
| transit_tolerance | VARCHAR | NULL | Weight tolerance (e.g., '5%') |
| message | TEXT | NULL | Additional instructions |
| vessel_name | VARCHAR | NULL | Vessel name |
| remarks | TEXT | NULL | Internal remarks |
| sales_person | VARCHAR | FK → salespersons.id | Salesperson |
| broker_name | VARCHAR | NULL | Broker name (if any) |
| status | VARCHAR | FK → statuses.id, NOT NULL | Order status |
| created_at | TIMESTAMP | NOT NULL | Creation timestamp |
| created_by | VARCHAR | NULL | Creator username |
| updated_by | VARCHAR | NULL | Last updater username |
| confirmed_at | TIMESTAMP | NULL | Confirmation timestamp |
| lifted_qty | DOUBLE | NULL | Quantity actually lifted |
| remaining_qty | DOUBLE | NULL | Remaining quantity to fulfill |

**Indexes**:
- `idx_sales_form_status_market_status_date` on (status, market_status, date)

**Purpose**: Track all sales orders to customers.

**Business Rules**:
1. `remaining_qty` = `quantity` - `lifted_qty`
2. Linked quantities update `lifted_qty` and `remaining_qty`
3. Only CONFIRMED sales contribute to revenue

**Referenced By**:
- `sale_purchase_links.sale_id`

---

#### **sale_purchase_links**
Links sales orders to purchase orders for P&L tracking.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique link ID |
| sale_id | VARCHAR | FK → sales.id, NOT NULL | Sales order |
| purchase_id | VARCHAR | FK → purchases.id, NOT NULL | Purchase order |
| created_by_username | VARCHAR | NOT NULL | Creator username |
| updated_by | VARCHAR | NULL | Last updater |
| linked_quantity | DOUBLE | NOT NULL | Quantity allocated (MT) |
| is_negative | BOOLEAN | NOT NULL, DEFAULT FALSE | Over-commitment flag |
| created_at | TIMESTAMP | NOT NULL | Creation timestamp |
| updated_at | TIMESTAMP | NULL | Update timestamp |

**Unique Constraint**: `uq_sale_purchase` on (sale_id, purchase_id)

**Purpose**: Allocate purchase inventory to sales orders for P&L calculation.

**Business Rules**:
1. `linked_quantity` cannot exceed purchase quantity
2. If over-committed, `is_negative` = TRUE
3. P&L = (sale price × linked_quantity) - (purchase cost × linked_quantity)

**P&L Calculation Example**:
```
Sale: 100 MT @ $400/MT = $40,000
Purchase: 100 MT @ $350/MT = $35,000
Linked Quantity: 100 MT
P&L = $40,000 - $35,000 = $5,000 profit
```

**Referenced By**:
- `sale_purchase_link_negative_history`

---

#### **sale_purchase_link_negative_history**
Tracks when links went negative (over-committed).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique history entry ID |
| link_id | VARCHAR | FK → sale_purchase_links.id | Link being tracked |
| detected_at | TIMESTAMP | NOT NULL | When negativity was detected |
| negative_quantity | DOUBLE | NULL | Amount over-committed |

**Purpose**: Audit trail for inventory over-commitment issues.

---

#### **physical_stocks**
Current physical stock levels for confirmed purchases.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique stock entry ID |
| purchase_id | VARCHAR | FK → purchases.id, UNIQUE | Purchase order |
| physical_stock | DOUBLE | NULL | Current stock level (MT) |
| updated_at | TIMESTAMP | NOT NULL | Last stock update time |
| updated_by | VARCHAR | NULL | User who updated stock |
| previous_stock | DOUBLE | NULL | Previous stock level |

**Purpose**: Track real-time physical inventory for each purchase.

**Business Logic**:
```
Initial Stock = quantity_received (from purchase)
Available Stock = physical_stock - Σ(linked_quantities)
```

**Use Case**: When physical stock is updated via CSV import, system tracks changes.

---

### 4. Analytics & Tracking Module

#### **cost_csv_uploads**
Tracks cost data CSV imports.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PK | Auto-increment upload ID |
| uploaded_by | VARCHAR | NULL | Username who uploaded |
| uploaded_at | TIMESTAMP | NOT NULL | Upload timestamp |
| row_count | INTEGER | NULL | Number of rows imported |

**Purpose**: Track cost data import sessions.

**Referenced By**:
- `cost_csv_entries.upload_id`

---

#### **cost_csv_entries**
Individual cost entries from CSV imports.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PK | Entry ID |
| upload_id | BIGINT | FK → cost_csv_uploads.id | Upload session |
| date | DATE | NULL | Cost date |
| product | VARCHAR | NULL | Product name |
| cost_amount | DECIMAL | NULL | Cost amount |
| currency | VARCHAR | NULL | Currency (INR, USD, etc.) |

**Purpose**: Store daily cost data for financial reporting.

**Use Case**: Daily operational costs, overheads, logistics costs.

---

#### **revenue_csv_uploads**
Tracks revenue data CSV imports.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PK | Auto-increment upload ID |
| uploaded_by | VARCHAR | NULL | Username who uploaded |
| uploaded_at | TIMESTAMP | NOT NULL | Upload timestamp |
| row_count | INTEGER | NULL | Number of rows imported |

**Purpose**: Track revenue data import sessions.

**Referenced By**:
- `revenue_csv_entries.upload_id`

---

#### **revenue_csv_entries**
Individual revenue entries from CSV imports.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PK | Entry ID |
| upload_id | BIGINT | FK → revenue_csv_uploads.id | Upload session |
| date | DATE | NULL | Revenue date |
| product | VARCHAR | NULL | Product name |
| revenue_amount | DECIMAL | NULL | Revenue amount |
| currency | VARCHAR | NULL | Currency |

**Purpose**: Store daily revenue data for financial reporting.

---

#### **pl_uploads**
P&L statement CSV uploads.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PK | Upload ID |
| uploaded_by | VARCHAR | NULL | Username |
| uploaded_at | TIMESTAMP | NOT NULL | Upload timestamp |
| row_count | INTEGER | NULL | Rows imported |

**Purpose**: Track P&L statement imports.

**Referenced By**:
- `pl_entries.upload_id`

---

#### **pl_entries**
P&L statement line items.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PK | Entry ID |
| upload_id | BIGINT | FK → pl_uploads.id | Upload session |
| date | DATE | NULL | P&L date |
| account_name | VARCHAR | NULL | Account/category name |
| debit_amount | DECIMAL | NULL | Debit amount |
| credit_amount | DECIMAL | NULL | Credit amount |
| net_amount | DECIMAL | NULL | Net P&L (credit - debit) |

**Purpose**: Store P&L statement data for financial analysis.

---

### 5. Audit & Logs Module

#### **audit_logs**
Complete audit trail of all system operations.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique log entry ID |
| action | VARCHAR | NOT NULL | Action performed (CREATE, UPDATE, DELETE, etc.) |
| entity_type | VARCHAR | NOT NULL | Entity type (PURCHASE, SALE, USER, etc.) |
| entity_id | VARCHAR | NULL | ID of affected entity |
| performed_by | VARCHAR | NOT NULL | Username of actor |
| performed_by_name | VARCHAR | NULL | Full name of actor |
| performed_by_role | VARCHAR | NULL | Role of actor |
| data_before | TEXT | NULL | JSON snapshot before change |
| data_after | TEXT | NULL | JSON snapshot after change |
| performed_at | TIMESTAMP | NOT NULL | When action occurred |

**Purpose**: Complete audit trail for compliance and debugging.

**Audited Actions**:
- CREATE: New record creation
- UPDATE: Record modifications
- DELETE: Record deletion
- CONFIRM: Order confirmation
- CANCEL: Order cancellation
- IMPORT: CSV data imports

**Use Cases**:
- Compliance reporting
- Change tracking
- Debugging data issues
- User activity monitoring

---

## Table Relationships

### Entity Relationship Diagram (Simplified)

```
┌──────────────┐         ┌──────────────┐
│    users     │────────>│    roles     │
└──────────────┘         └──────────────┘
      │                        │
      │                        │ Many-to-Many
      │                        ▼
      │                  ┌──────────────┐
      │                  │ permissions  │
      │                  └──────────────┘
      │
      ├───────> two_factor_credentials
      ├───────> backup_codes
      └───────> user_permission_restrictions

┌──────────────┐         ┌──────────────┐
│  purchases   │────────>│  products    │
└──────────────┘         └──────────────┘
      │                        ▲
      ├───────> ports          │
      ├───────> countries      │
      ├───────> payment_terms  │
      ├───────> statuses       │
      │                        │
      └───────> physical_stocks

┌──────────────┐         ┌──────────────┐
│    sales     │────────>│  products    │
└──────────────┘         └──────────────┘
      │                        
      ├───────> ports
      ├───────> salespersons
      └───────> statuses

┌──────────────┐         ┌──────────────┐
│ sale_purchase│────────>│  purchases   │
│    links     │         └──────────────┘
└──────────────┘
      │                  ┌──────────────┐
      └─────────────────>│    sales     │
                         └──────────────┘
```

### Key Relationships Summary

| Parent Table | Child Table | Relationship | Description |
|--------------|-------------|--------------|-------------|
| roles | users | One-to-Many | Each user has one role |
| roles | role_permissions | One-to-Many | Roles have multiple permissions |
| permissions | role_permissions | One-to-Many | Permissions assigned to multiple roles |
| users | two_factor_credentials | One-to-One | Optional 2FA for users |
| users | backup_codes | One-to-Many | Multiple backup codes per user |
| products | purchases | One-to-Many | Product used in purchases |
| products | sales | One-to-Many | Product used in sales |
| ports | purchases | One-to-Many | Port for loading/discharge |
| ports | sales | One-to-Many | Port for delivery |
| ports | port_transit_days | Many-to-Many | Transit times between ports |
| countries | purchases | One-to-Many | Origin country |
| statuses | purchases | One-to-Many | Order status |
| statuses | sales | One-to-Many | Order status |
| purchases | physical_stocks | One-to-One | Stock level per purchase |
| purchases | sale_purchase_links | One-to-Many | Allocate to sales |
| sales | sale_purchase_links | One-to-Many | Linked to purchases |
| salespersons | sales | One-to-Many | Salesperson handling deal |

---

## Security & Authentication

### Authentication Flow

#### Standard Login (No 2FA)
```
1. User sends username + password to /api/v1/auth/login
2. System validates credentials
3. System returns JWT access token
4. Client includes token in all subsequent requests
```

#### 2FA Login Flow
```
1. User sends username + password to /api/v1/auth/login
2. System validates credentials
3. System returns pre-auth token (not full access)
4. Client sends TOTP code with pre-auth token to /api/v1/auth/2fa/login/verify
5. System validates TOTP code
6. System returns JWT access token
```

#### 2FA Enrollment Flow
```
1. User logs in (gets pre-auth token)
2. Client calls /api/v1/auth/2fa/enroll/init with pre-auth token
3. System generates TOTP secret, returns QR code
4. User scans QR code in authenticator app
5. Client sends TOTP code to /api/v1/auth/2fa/enroll/confirm
6. System validates code, returns access token + backup codes
7. User saves backup codes securely
```

### Permission Inheritance

```
Example: Sales Executive Role

Direct Permissions:
  - SALE_VIEW
  - SALE_CREATE

Parent Role (Sales Manager) Permissions:
  - SALE_VIEW
  - SALE_CREATE
  - SALE_EDIT
  - SALE_APPROVE

Effective Permissions for Sales Executive:
  - SALE_VIEW (from self)
  - SALE_CREATE (from self)
  - SALE_EDIT (inherited from parent)
  - SALE_APPROVE (inherited from parent)
```

### Row-Level Security

For roles with `restrict_to_own_records = true`:
- Users can only view/edit records they created
- Applied via `created_by` field in services
- Enforced in PurchaseService and SalesService

```java
// Example: Sales Executive can only see their own sales
if (currentUser.isRowScoped()) {
    return salesRepository.findByCreatedBy(currentUser.getUsername());
} else {
    return salesRepository.findAll();
}
```

---

## Module Breakdown

### Purchase Module
**Purpose**: Manage import and local purchase orders.

**Key Features**:
- Create/Edit/Confirm/Cancel purchase orders
- Track ETD/ETA and vessel information
- Calculate landed costs (custom duty, SWS, expenses)
- Physical stock management via CSV import
- Receipt tracking (quantity received vs ordered)

**Permissions**: PURCHASE_VIEW, PURCHASE_CREATE, PURCHASE_EDIT, PURCHASE_APPROVE

**Key Tables**: purchases, physical_stocks

---

### Sales Module
**Purpose**: Manage customer sales orders and inquiries.

**Key Features**:
- Create/Edit/Confirm/Cancel sales orders
- Support BUY/SELL/INQUIRY types
- Track lifted vs remaining quantities
- CSV import/export
- Filter by product, company, port, date range

**Permissions**: SALE_VIEW, SALE_CREATE, SALE_EDIT, SALE_APPROVE

**Key Tables**: sales, salespersons

---

### Linking Module
**Purpose**: Link sales to purchases for P&L calculation.

**Key Features**:
- Allocate purchase quantities to sales
- Calculate P&L per link
- Track over-committed inventory (negative links)
- Historical tracking of negative P&L
- Summary views (sale-wise, purchase-wise)

**Permissions**: SALE_VIEW, SALE_EDIT

**Key Tables**: sale_purchase_links, sale_purchase_link_negative_history

**P&L Calculation**:
```
For each link:
  Sale Revenue = linked_quantity × sale_price
  Purchase Cost = linked_quantity × purchase_cost
  Gross P&L = Sale Revenue - Purchase Cost
  
Total P&L = Σ(Gross P&L for all links)
```

---

### Stock Statistics Module
**Purpose**: Real-time inventory analytics and vessel tracking.

**Key Features**:
- Vessel-wise stock breakdown
- Product-wise stock summary
- Financial summary (value of stock)
- Historical stock data
- Available vs committed quantities

**Permissions**: STOCK_STATS_VIEW

**Calculations**:
```
Available Stock = physical_stock - Σ(linked_quantities)
Stock Value = physical_stock × purchase_price
```

---

### Financial Tracking Module
**Purpose**: Cost and revenue tracking via CSV imports.

**Key Features**:
- Daily cost import (CostCsvController)
- Daily revenue import (RevenueCsvController)
- P&L statement import (PlController)
- Upload history and session tracking
- Total calculations

**Key Tables**: 
- cost_csv_uploads, cost_csv_entries
- revenue_csv_uploads, revenue_csv_entries
- pl_uploads, pl_entries

---

### Audit Module
**Purpose**: Complete audit trail of all operations.

**Key Features**:
- Automatic logging of CREATE/UPDATE/DELETE
- Before/after snapshots in JSON
- Filter by entity type, user, date
- User and role tracking

**Permissions**: USER_MANAGEMENT (to view logs)

**Key Tables**: audit_logs

---

### Master Data Module
**Purpose**: Manage reference data.

**Endpoints**:
- Companies (search, create)
- Products (search)
- Ports (search, create)
- Countries (search)
- Market Status (list)
- Payment Terms (list)
- Port Transit Days (CRUD)
- Salespersons (search, create)

**Key Tables**: companies, products, ports, countries, market_status, payment_terms, port_transit_days, salespersons

---

## Data Flow Examples

### Example 1: Create Purchase Order → Confirm → Link to Sale

```
Step 1: Create Purchase Order
  POST /api/v1/purchase/create/purchase_order
  {
    "product": "Sulphuric Acid",
    "quantity": 500,
    "offer_usd": 350,
    "status": "PENDING"
  }
  → Creates record in purchases table with status = PENDING

Step 2: Confirm Purchase Order
  PATCH /api/v1/purchase/{id}/confirm
  → Updates status = CONFIRMED
  → Creates entry in physical_stocks (physical_stock = quantity)
  → Logs action in audit_logs

Step 3: Create Sales Order
  POST /api/v1/sales/create/sales_order
  {
    "product": "Sulphuric Acid",
    "quantity": 200,
    "price": 400,
    "status": "CONFIRMED"
  }
  → Creates record in sales table

Step 4: Link Sale to Purchase
  POST /api/v1/links
  {
    "saleId": "SO-001",
    "purchaseId": "PO-001",
    "linkedQuantity": 200
  }
  → Creates record in sale_purchase_links
  → Updates sales.lifted_qty = 200
  → Updates sales.remaining_qty = 0
  → P&L = (400 - 350) × 200 = $10,000 profit
```

### Example 2: Physical Stock Update via CSV

```
Step 1: Export Current Stock
  GET /api/v1/purchase/export-physical-stock
  → Returns CSV with current physical_stock values

Step 2: User Updates CSV Offline
  PO-001, 450 (was 500, sold 50 MT)
  PO-002, 300 (unchanged)

Step 3: Import Updated Stock
  POST /api/v1/purchase/import-physical-stock
  → Reads CSV file
  → For each row:
      - Store previous_stock
      - Update physical_stock
      - Update updated_by and updated_at
  → Creates entry in cost_csv_uploads (session tracking)
  → Returns import summary
```

### Example 3: 2FA Enrollment

```
Step 1: Login
  POST /api/v1/auth/login
  { "username": "john", "password": "secret" }
  → Returns pre-auth token (if 2FA not enrolled)

Step 2: Initialize Enrollment
  POST /api/v1/auth/2fa/enroll/init
  Header: Authorization: Bearer {preAuthToken}
  → Generates TOTP secret
  → Encrypts and stores in two_factor_credentials
  → Returns QR code URI

Step 3: User Scans QR in Google Authenticator

Step 4: Confirm Enrollment
  POST /api/v1/auth/2fa/enroll/confirm
  { "code": "123456" }
  → Validates TOTP code
  → Sets enabled = true
  → Generates 10 backup codes
  → Returns full access token + backup codes

Future Logins:
  POST /api/v1/auth/login
  → Returns pre-auth token
  
  POST /api/v1/auth/2fa/login/verify
  { "code": "654321" }
  → Returns full access token
```

---

## Indexes and Performance

### Critical Indexes

1. **purchases.idx_purchases_status_market_status_created_at**
   - Covers common filters: status, market_status, created_at
   - Speeds up dashboard queries and reports

2. **sales.idx_sales_form_status_market_status_date**
   - Covers status, market_status, date filters
   - Essential for sales reports

3. **sale_purchase_links.uq_sale_purchase**
   - Prevents duplicate links
   - Ensures data integrity

4. **physical_stocks.purchase_id UNIQUE**
   - One stock entry per purchase
   - Fast lookup for availability checks

### Query Optimization Tips

- Use pagination for large result sets
- Filter by status and date range to reduce dataset
- Leverage lazy loading for relationships
- Use projection (DTO) instead of full entities for lists

---

## Environment Configuration

### Database Connection
```properties
spring.datasource.url=jdbc:postgresql://crystal-analytics-db.c30ye0yuuri7.ap-south-1.rds.amazonaws.com:5432/chemos_dev
spring.datasource.username=chemos
spring.datasource.password=chemos@123
```

### JPA Settings
```properties
spring.jpa.hibernate.ddl-auto=update  # Auto-update schema
spring.jpa.show-sql=true              # Log SQL queries
spring.jpa.properties.hibernate.format_sql=true
```

### Security Settings
```properties
jwt.secret=auSTLV3ixygyT5jrwq3EExuqNSPdUVGZQgX87YrjGE6
totp.issuer=ChemOS
totp.lockout.max-attempts=5
totp.lockout.duration-minutes=15
```

### Application Settings
```properties
server.port=8081
spring.jackson.time-zone=Asia/Kolkata
```

---

## Backup and Recovery

### Database Backup Strategy
1. **Automated RDS Snapshots** - Daily at 2:00 AM IST
2. **Manual Snapshots** - Before major releases
3. **Point-in-Time Recovery** - Last 7 days

### Critical Tables (Priority for Backup)
1. purchases
2. sales
3. sale_purchase_links
4. physical_stocks
5. users, roles, permissions
6. audit_logs

---

## Future Enhancements

### Planned Features
1. **Multi-currency Support** - Track purchases in multiple currencies
2. **Advanced Analytics** - Dashboards with charts and trends
3. **Email Notifications** - Order confirmations, alerts
4. **Document Management** - Attach invoices, BOLs, contracts
5. **API Rate Limiting** - Prevent abuse
6. **Bulk Operations** - Batch confirm/cancel orders
7. **Export Reports** - PDF/Excel reports
8. **Approval Workflows** - Multi-level approvals for large orders

---

## Support and Maintenance

### Monitoring
- Application logs via Spring Boot Actuator
- Database slow query logs
- Failed login attempts tracking (2FA lockouts)

### Regular Maintenance Tasks
1. Clean up old audit logs (> 1 year)
2. Archive cancelled/old orders
3. Review permission assignments
4. Update master data (ports, payment terms)
5. Backup code regeneration for 2FA users

---

## Glossary

| Term | Definition |
|------|------------|
| MT | Metric Tons - unit for quantity |
| CIF | Cost, Insurance, and Freight - delivery term |
| FOB | Free On Board - delivery term |
| CFR | Cost and Freight - delivery term |
| ETA | Expected Time of Arrival |
| ETD | Expected Time of Departure |
| TOTP | Time-based One-Time Password (2FA) |
| JWT | JSON Web Token (authentication) |
| P&L | Profit & Loss |
| LC | Letter of Credit - payment method |
| FCL | Full Container Load |
| LCL | Less than Container Load |
| HS Code | Harmonized System Code (customs) |
| CAS No | Chemical Abstracts Service Number |
| LOCODE | UN Location Code (ports) |

---

**Document Version**: 1.0  
**Last Updated**: 2026-08-21  
**Maintained By**: ChemOS Development Team
