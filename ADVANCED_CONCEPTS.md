# Advanced Concepts in ChemOS - Deep Dive

This document explains the more advanced or potentially confusing patterns in your codebase that you should master.

---

## 🎯 1. Role-Based Access Control (RBAC) with Permission Hierarchy

### How It Works in Your Application

Your application implements a **sophisticated multi-level permission system**:

```
Super Role (ADMIN)
    ↓
Parent Role (e.g., PURCHASE_MANAGER)
    ↓
Child Role (e.g., PURCHASE_ASSISTANT)
    ↓
User with Permission Restrictions
```

### Database Schema
```sql
-- Users table
users (id, username, password, role_id, is_active)

-- Roles table
roles (id, name, display_name, is_super_role, parent_role_id)

-- Permissions table  
permissions (id, code, description)

-- Role-Permission mapping
role_permissions (role_id, permission_id)

-- User-specific restrictions
user_permission_restrictions (user_id, permission_id, is_revoked)
```

### Permission Resolution Algorithm

The `PermissionResolverService` uses this logic:

```java
1. Is user's role a SUPER ROLE (like ADMIN)?
   → YES: Return ALL permission codes from database
   → NO: Continue to step 2

2. Get permissions directly assigned to user's role

3. Get permissions from parent role (if exists)
   - Skip super roles during traversal
   - Only one level of inheritance

4. Apply user-specific restrictions
   - Remove any permissions that user has revoked

5. Return final permission set
```

### Example Flow

```java
// User "john" has role "PURCHASE_MANAGER"
// PURCHASE_MANAGER has parent role "PURCHASE_ASSISTANT"
// User has restriction: revoked "PURCHASE_DELETE"

User: john
Role: PURCHASE_MANAGER (not super)
Parent Role: PURCHASE_ASSISTANT

Permissions:
✅ Own role: [PURCHASE_CREATE, PURCHASE_EDIT, PURCHASE_DELETE, PURCHASE_APPROVE]
✅ Parent role: [PURCHASE_VIEW, PURCHASE_EXPORT]
❌ User restrictions: [PURCHASE_DELETE]

Final permissions for john:
[PURCHASE_CREATE, PURCHASE_EDIT, PURCHASE_APPROVE, PURCHASE_VIEW, PURCHASE_EXPORT]
```

### Why This Design?

1. **Super Role**: Admin gets ALL permissions without maintaining mappings
2. **Inheritance**: Reduces duplication (managers get assistant permissions automatically)
3. **User Restrictions**: Fine-grained control per user
4. **Audit Trail**: Every action maps to a real permission code (even for admins)

### Code Implementation

```java
@Service
@RequiredArgsConstructor
public class PermissionResolverService {
    
    private final PermissionRepository permissionRepository;
    
    public Set<String> resolve(User user) {
        Role role = user.getRole();
        
        // Super role: return ALL permission codes
        if (role.isSuperRole()) {
            return permissionRepository.findAll()
                .stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());
        }
        
        Set<String> effectivePermissions = new HashSet<>();
        
        // Add own permissions
        effectivePermissions.addAll(
            role.getPermissions().stream()
                .map(Permission::getCode)
                .toList()
        );
        
        // Add parent permissions (if exists)
        Role parent = role.getParentRole();
        if (parent != null && !parent.isSuperRole()) {
            effectivePermissions.addAll(
                parent.getPermissions().stream()
                    .map(Permission::getCode)
                    .toList()
            );
        }
        
        // Apply user restrictions
        Set<String> revokedPermissions = user.getRestrictions().stream()
            .filter(UserPermissionRestriction::isRevoked)
            .map(r -> r.getPermission().getCode())
            .collect(Collectors.toSet());
        
        effectivePermissions.removeAll(revokedPermissions);
        
        return effectivePermissions;
    }
}
```

---

## 🔐 2. JWT Authentication Flow - Detailed

### Complete Request Lifecycle

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. CLIENT REQUEST                                                │
│ GET /api/v1/sales/12345                                         │
│ Headers:                                                         │
│   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. JwtAuthFilter.doFilterInternal()                             │
│    - Extract "Bearer " prefix                                    │
│    - Validate token signature                                    │
│    - Extract username from token                                 │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. Load User from Database                                       │
│    User user = userRepository.findByUsernameWithPermissions(...)│
│    Check: Is user active?                                        │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. Resolve User Permissions                                      │
│    Set<String> permissions = permissionResolverService.resolve()│
│    Convert to GrantedAuthority objects                          │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. Set SecurityContext                                           │
│    UsernamePasswordAuthenticationToken auth = new ...           │
│    SecurityContextHolder.getContext().setAuthentication(auth)   │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 6. Controller Method Execution                                   │
│    @PreAuthorize("hasAuthority('SALE_VIEW')")                   │
│    - Spring Security checks if 'SALE_VIEW' in authorities       │
│    - If YES: execute method                                      │
│    - If NO: throw AccessDeniedException (403)                   │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 7. Response                                                      │
│    - Success: 200 OK with data                                   │
│    - Auth failed: 401 Unauthorized                              │
│    - Permission denied: 403 Forbidden                           │
└─────────────────────────────────────────────────────────────────┘
```

### Key Points

1. **Stateless**: No session stored on server (JWT contains all info)
2. **Filter Runs First**: Before Spring Security's authentication
3. **SecurityContext**: Thread-local storage for current user
4. **Fresh Permissions**: Loaded from DB on every request (not from token)
5. **Token Expiry**: 24 hours (configurable)

### Why Load Permissions on Every Request?

**Question**: Token already has role, why query DB again?

**Answer**:
1. Permission changes take effect immediately (no need to re-login)
2. User deactivation works instantly
3. Role hierarchy changes apply immediately
4. User-specific restrictions are enforced

Trade-off: DB query per request vs. real-time permission updates

**Optimization Option**: Use Redis cache for user permissions

---

## 🔄 3. Repository Query Optimization with PostgreSQL

### Advanced Text Search Query

Your `CompanyRepository.findSuggestions()` uses sophisticated PostgreSQL features:

```sql
SELECT *
FROM companies
WHERE
    :prefix = ''  -- Empty query returns all
    OR search_key LIKE CONCAT('%', :prefix, '%')  -- Pattern match
    OR word_similarity(search_key, :prefix) >= 0.20  -- Fuzzy match
    OR similarity(search_key, :prefix) >= 0.20
ORDER BY
    CASE
        WHEN search_key = :prefix THEN 1           -- Exact match (highest priority)
        WHEN search_key LIKE CONCAT(:prefix, '%') THEN 2    -- Starts with
        WHEN search_key LIKE CONCAT('% ', :prefix, '%') THEN 3  -- Word starts with
        WHEN search_key LIKE CONCAT('%(', :prefix, '%') THEN 4  -- Acronym in brackets
        WHEN search_key LIKE CONCAT('%', :prefix, '%') THEN 5   -- Contains
        ELSE 6                                      -- Fuzzy match (lowest priority)
    END,
    LENGTH(search_key) ASC,  -- Shorter names first
    GREATEST(
        word_similarity(search_key, :prefix),
        similarity(search_key, :prefix)
    ) DESC  -- Higher similarity first
LIMIT :limit
```

### What This Does

**Example**: User types "acc"

Results in order:
1. "acc" (exact match)
2. "accenture" (starts with)
3. "blue acc limited" (word starts with)
4. "company (acc)" (acronym in brackets)
5. "account services" (contains)
6. "acme corp" (fuzzy match - similarity score)

### PostgreSQL Extensions Required

```sql
-- Enable trigram extension for similarity functions
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

### Why This Approach?

- **User-friendly**: Handles typos and partial matches
- **Relevant results**: Smart ordering puts best matches first
- **Fast**: Uses GIN index on search_key column
- **Scalable**: Limit prevents loading too many results

### Database Index for Performance

```sql
CREATE INDEX idx_companies_search_key_trgm 
ON companies 
USING GIN (search_key gin_trgm_ops);
```

---

## 📦 4. Builder Pattern with Lombok

### What is Builder Pattern?

Instead of constructors with many parameters:

```java
// ❌ Hard to read, easy to mess up parameter order
Sales sale = new Sales(
    null,                    // id
    LocalDate.now(),        // date
    null,                   // updatedAt
    "DIRECT",               // salesType
    "Company A",            // companyTo
    "Company B",            // companyFrom
    product,                // product
    100.0,                  // quantity
    50000.0,                // price
    "NET_30",               // payment
    "FOB",                  // deliveryTerm
    port,                   // port
    52000.0,                // marketPrice
    "ABOVE",                // marketStatus
    // ... 10 more parameters
);
```

Use builder pattern:

```java
// ✅ Clear, readable, maintainable
Sales sale = Sales.builder()
    .date(LocalDate.now())
    .salesType("DIRECT")
    .companyTo("Company A")
    .companyFrom("Company B")
    .product(product)
    .quantity(100.0)
    .price(50000.0)
    .payment("NET_30")
    .deliveryTerm("FOB")
    .port(port)
    .marketPrice(52000.0)
    .marketStatus("ABOVE")
    .build();
```

### toBuilder = true

```java
@Builder(toBuilder = true)
public class Sales { ... }

// Create modified copy
Sales originalSale = getSale();
Sales confirmedSale = originalSale.toBuilder()
    .status("CONFIRMED")
    .updatedAt(LocalDateTime.now())
    .build();

// originalSale is unchanged (immutability-like behavior)
```

### Use Cases

1. **Creating entities**: Clear parameter names
2. **Testing**: Easy to create test data
3. **Partial updates**: Copy and modify specific fields
4. **Fluent API**: Chain method calls

---

## 🎭 5. DTO Pattern with Java Records

### Traditional Class vs Record

```java
// ❌ Old way - verbose
public class UserResponse {
    private String username;
    private String email;
    private String role;
    
    public UserResponse(String username, String email, String role) {
        this.username = username;
        this.email = email;
        this.role = role;
    }
    
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    
    @Override
    public boolean equals(Object o) { /* ... */ }
    @Override
    public int hashCode() { /* ... */ }
    @Override
    public String toString() { /* ... */ }
}

// ✅ Modern way - concise
public record UserResponse(
    String username,
    String email,
    String role
) {}
```

### Record Features

```java
public record CreateSaleRequest(
    LocalDate date,
    String salesType,
    
    @JsonAlias({"product", "productId"})  // Accept both field names
    String productId,
    
    @NotNull @Size(min = 1, max = 50)     // Validation works on records
    String companyTo,
    
    Double quantity
) {
    // Can add custom methods
    public boolean isBulkOrder() {
        return quantity != null && quantity > 1000;
    }
    
    // Compact constructor - validation
    public CreateSaleRequest {
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
    }
}
```

### When to Use Records

✅ **Use for**:
- DTOs (request/response)
- Configuration objects
- Value objects
- Immutable data

❌ **Don't use for**:
- JPA entities (need setters for lazy loading)
- Classes that need inheritance (records are final)
- Mutable objects

---

## 🔄 6. Transactional Behavior

### What @Transactional Does

```java
@Transactional
public void transferStock(String fromWarehouse, String toWarehouse, double quantity) {
    // 1. Start database transaction
    
    Stock fromStock = stockRepository.findByWarehouse(fromWarehouse);
    fromStock.setQuantity(fromStock.getQuantity() - quantity);
    
    Stock toStock = stockRepository.findByWarehouse(toWarehouse);
    toStock.setQuantity(toStock.getQuantity() + quantity);
    
    // If any exception occurs here, both updates are rolled back
    
    auditService.logTransfer(fromWarehouse, toWarehouse, quantity);
    
    // 2. Commit transaction (all changes applied together)
}
```

### Transaction Propagation

```java
@Transactional(propagation = Propagation.REQUIRED)  // Default
// Join existing transaction or create new one

@Transactional(propagation = Propagation.REQUIRES_NEW)
// Always create new transaction (suspend existing)

@Transactional(propagation = Propagation.MANDATORY)
// Must be called within existing transaction (throw exception if not)

@Transactional(propagation = Propagation.SUPPORTS)
// Join if exists, execute without transaction if not
```

### Read-Only Optimization

```java
@Transactional(readOnly = true)
public List<Sales> getAllSales() {
    // Hints to database: no writes will occur
    // Optimizations:
    // - Skip flushing Hibernate session
    // - Use read replicas in master-slave setup
    // - Reduce locking overhead
    return salesRepository.findAll();
}
```

### Common Pitfall

```java
@Service
public class SalesService {
    
    @Autowired
    private SalesRepository salesRepository;
    
    // ❌ Transaction doesn't work - self-invocation
    public void publicMethod() {
        this.transactionalMethod();  // Spring AOP proxy bypassed
    }
    
    @Transactional
    private void transactionalMethod() {
        // Transaction not started!
    }
}

// ✅ Fix: Make method public and call from another service
@Transactional
public void transactionalMethod() {
    // Transaction works
}
```

---

## 📄 7. Pagination & Performance

### Page vs List

```java
// ❌ Bad: Loads ALL records into memory
@GetMapping("/sales")
public List<Sales> getAllSales() {
    return salesRepository.findAll();  // Could be 1 million records!
}

// ✅ Good: Loads one page at a time
@GetMapping("/sales")
public Page<Sales> getAllSales(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable) {
    return salesRepository.findAll(pageable);
}
```

### Page Object Structure

```json
{
  "content": [ /* array of items */ ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": { "sorted": true, "unsorted": false }
  },
  "totalElements": 1500,
  "totalPages": 75,
  "last": false,
  "first": true,
  "number": 0,
  "size": 20,
  "numberOfElements": 20
}
```

### Custom Pagination

```java
public Page<SalesDto> getFilteredSales(SalesFilterRequest filters, Pageable pageable) {
    // Get page of entities
    Page<Sales> salesPage = salesRepository.findByFilters(filters, pageable);
    
    // Transform to DTOs (preserves pagination metadata)
    return salesPage.map(salesMapper::toDto);
}
```

### Client Usage

```
GET /api/v1/sales?page=0&size=50&sort=date,desc&sort=price,asc
```

---

## 🔥 8. Lazy Loading and N+1 Problem

### The N+1 Problem

```java
// Repository query
List<Sales> sales = salesRepository.findAll();  // 1 query

// Controller
for (Sales sale : sales) {
    System.out.println(sale.getProduct().getName());  // N queries (one per sale!)
}

// Total queries: 1 + N (if N=100 sales, that's 101 queries!)
```

### Solution 1: JOIN FETCH

```java
@Query("SELECT s FROM Sales s JOIN FETCH s.product WHERE s.status = :status")
List<Sales> findByStatusWithProduct(@Param("status") String status);

// Single query with JOIN
```

### Solution 2: Entity Graph

```java
@EntityGraph(attributePaths = {"product", "port", "salesPerson"})
@Query("SELECT s FROM Sales s WHERE s.status = :status")
List<Sales> findByStatusWithRelations(@Param("status") String status);
```

### Solution 3: DTO Projection

```java
@Query("""
    SELECT new com.example.SalesDto(
        s.id, s.date, s.quantity, p.name, port.name
    )
    FROM Sales s
    JOIN s.product p
    JOIN s.port port
    WHERE s.status = :status
    """)
List<SalesDto> findSalesSummary(@Param("status") String status);

// Fetches only needed columns, no lazy loading issues
```

---

## 🎯 9. Method Security with @PreAuthorize

### SpEL Expressions

```java
@PreAuthorize("hasAuthority('ADMIN')")
// Check single authority

@PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
// Check multiple authorities (OR)

@PreAuthorize("hasAuthority('SALE_VIEW') and hasAuthority('SALE_EDIT')")
// Multiple checks (AND)

@PreAuthorize("#username == authentication.name")
// Check if parameter matches current user
public UserDto getUser(@PathVariable String username) { ... }

@PreAuthorize("@securityService.canAccessSale(#saleId)")
// Custom bean method check
public Sales getSale(@PathVariable String saleId) { ... }
```

### Custom Security Service

```java
@Service
public class SecurityService {
    
    public boolean canAccessSale(String saleId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        // Custom business logic
        Sale sale = saleRepository.findById(saleId);
        return sale.getCreatedBy().equals(username) || 
               auth.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"));
    }
}
```

---

## 🚦 10. Exception Flow & HTTP Status Codes

### Your Exception Handling Flow

```
┌─────────────────────────────────────────────────────────────┐
│ Controller Method Execution                                  │
│ service.createCompany(request)                              │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ Service Layer                                                │
│ if (exists) {                                               │
│     throw new CompanyAlreadyExistsException("...");         │
│ }                                                           │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ GlobalExceptionHandler catches exception                     │
│ @ExceptionHandler(CompanyAlreadyExistsException.class)      │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ Creates ApiErrorResponse                                     │
│ Returns ResponseEntity with HTTP 409 CONFLICT               │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ Client receives JSON:                                        │
│ {                                                           │
│   "error": "Resource Conflict",                            │
│   "message": "A company with this name already exists!",   │
│   "timestamp": "2024-01-15T10:30:00"                       │
│ }                                                           │
│ Status: 409                                                 │
└─────────────────────────────────────────────────────────────┘
```

### When to Use Which Status Code

```
200 OK              → Successful GET, PUT, PATCH
201 CREATED         → Successful POST (resource created)
204 NO CONTENT      → Successful DELETE (no response body)
400 BAD REQUEST     → Invalid input (validation failed)
401 UNAUTHORIZED    → Not authenticated (no token / invalid token)
403 FORBIDDEN       → Authenticated but no permission
404 NOT FOUND       → Resource doesn't exist
409 CONFLICT        → Resource already exists / version conflict
422 UNPROCESSABLE   → Semantic validation failed
500 INTERNAL ERROR  → Server error / unexpected exception
```

---

## 🧩 Summary: Key Takeaways

1. **RBAC**: Understand role hierarchy, permission inheritance, and super roles
2. **JWT**: Stateless auth, permissions loaded fresh on each request
3. **PostgreSQL**: Advanced text search with similarity functions
4. **Builder Pattern**: Clean object construction with Lombok
5. **Records**: Modern, immutable DTOs
6. **Transactions**: ACID guarantees, propagation, read-only optimization
7. **Pagination**: Always paginate large datasets
8. **Lazy Loading**: Beware N+1 problem, use JOIN FETCH
9. **@PreAuthorize**: Method-level security with SpEL
10. **Exceptions**: Global handling with appropriate HTTP status codes

---

**Next Steps**:
1. Debug the application and trace these flows
2. Modify existing features using these patterns
3. Create a new feature from scratch
4. Read Spring Security documentation
5. Study PostgreSQL full-text search

**Practice Makes Perfect** 🚀
