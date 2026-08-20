# Hands-On Learning Path for ChemOS

This guide provides practical exercises to master the concepts in your codebase. Complete these in order.

---

## 📅 Week 1: Understanding the Existing Code

### Day 1-2: Project Setup & Exploration

#### Exercise 1: Run the Application
```bash
# 1. Start the application
mvn spring-boot:run

# 2. Test the health endpoint
curl http://localhost:8081/actuator/health

# 3. Check the logs - identify:
#    - When Spring Boot starts
#    - When database connection is established
#    - What auto-configurations are applied
```

**Learning Goals**: Understand Spring Boot startup, auto-configuration

#### Exercise 2: Database Exploration
```sql
-- Connect to your PostgreSQL database and run these queries

-- 1. List all tables
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public';

-- 2. Understand the role hierarchy
SELECT r1.name as role, r2.name as parent_role
FROM roles r1
LEFT JOIN roles r2 ON r1.parent_role_id = r2.id;

-- 3. See permission distribution
SELECT r.name, COUNT(rp.permission_id) as permission_count
FROM roles r
LEFT JOIN role_permissions rp ON r.id = rp.role_id
GROUP BY r.name;

-- 4. Check user-role mappings
SELECT u.username, r.name as role, u.is_active
FROM users u
JOIN roles r ON u.role_id = r.id;
```

**Learning Goals**: Understand database schema, RBAC structure

#### Exercise 3: Trace a Request
```bash
# Set debug logging in application.properties
logging.level.chemos.chem_os=DEBUG
logging.level.org.springframework.security=DEBUG

# Make a login request
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Trace in logs:
# 1. Request received
# 2. UserDetailsService loading user
# 3. Password verification
# 4. JWT token generation
# 5. Response sent
```

**Learning Goals**: Understand request lifecycle, security filter chain

---

### Day 3-4: JWT Authentication

#### Exercise 4: Test JWT Flow

1. **Login and Get Token**
```bash
TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | jq -r '.token')

echo $TOKEN
```

2. **Decode JWT** (Use https://jwt.io)
   - Copy your token
   - Paste in jwt.io
   - Identify: header, payload, signature
   - Find: username, role, expiration

3. **Make Authenticated Request**
```bash
curl -X GET http://localhost:8081/api/v1/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

4. **Test Invalid Token**
```bash
curl -X GET http://localhost:8081/api/v1/auth/me \
  -H "Authorization: Bearer invalid_token"

# Expected: 401 Unauthorized
```

5. **Test Expired Token**
   - Change JWT expiry to 1 minute in `JwtService`
   - Get token, wait 2 minutes, use it
   - Expected: 401 Unauthorized

**Learning Goals**: JWT structure, token validation, expiry handling

#### Exercise 5: Debug JWT Filter

**Task**: Add debug logging to `JwtAuthFilter`

```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {
    
    // Add these log statements
    System.out.println("=== JWT Filter Start ===");
    System.out.println("Request URI: " + request.getRequestURI());
    
    String authHeader = request.getHeader("Authorization");
    System.out.println("Auth Header: " + authHeader);
    
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        System.out.println("No valid Authorization header");
        filterChain.doFilter(request, response);
        return;
    }
    
    String token = authHeader.substring(7);
    System.out.println("Token extracted: " + token.substring(0, 20) + "...");
    
    if (!jwtService.isValid(token)) {
        System.out.println("Token is invalid");
        filterChain.doFilter(request, response);
        return;
    }
    
    String username = jwtService.extractUsername(token);
    System.out.println("Username from token: " + username);
    
    // ... rest of the code
    
    System.out.println("=== JWT Filter End ===");
    filterChain.doFilter(request, response);
}
```

**Make a request and observe logs**

**Learning Goals**: Filter execution order, request processing

---

### Day 5-7: Controllers & Services

#### Exercise 6: Create Your First Endpoint

**Task**: Add a simple status endpoint to `CompanyController`

```java
@GetMapping("/count")
public ResponseEntity<Long> getCompanyCount() {
    long count = companyService.getCount();
    return ResponseEntity.ok(count);
}
```

```java
// In CompanyService
public long getCount() {
    return companyRepository.count();
}
```

**Test it**:
```bash
curl -X GET http://localhost:8081/api/v1/companies/count \
  -H "Authorization: Bearer $TOKEN"
```

**Learning Goals**: Create REST endpoint, service method

#### Exercise 7: Add Request Validation

**Task**: Enhance `CreateCompanyRequest` with validation

```java
public record CreateCompanyRequest(
    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    String companyName
) {}
```

**Test with invalid data**:
```bash
# Empty name
curl -X POST http://localhost:8081/api/v1/companies/create-company \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"companyName":""}'

# Expected: 400 Bad Request with validation error
```

**Learning Goals**: Bean Validation, error responses

#### Exercise 8: Add a Filter Endpoint

**Task**: Add filtering to company search

```java
@GetMapping("/filter")
public ResponseEntity<List<CompanySuggestionResposne>> filterCompanies(
        @RequestParam(required = false) String query,
        @RequestParam(required = false, defaultValue = "20") int limit) {
    return ResponseEntity.ok(companyService.searchCompanies(query, limit));
}
```

**Update service**:
```java
public List<CompanySuggestionResposne> searchCompanies(String query, int limit) {
    String searchKey = (query == null || query.trim().isEmpty())
            ? ""
            : CompanySanitizer.createSearchKey(query.trim());
    
    return companyRepository
            .findSuggestions(searchKey, limit)
            .stream()
            .map(companyMapper::toResponse)
            .toList();
}
```

**Learning Goals**: Query parameters, default values

---

## 📅 Week 2: Data Access & JPA

### Day 8-10: Repositories & Queries

#### Exercise 9: Create Custom Query Methods

**Task**: Add query methods to `SalesRepository`

```java
public interface SalesRepository extends JpaRepository<Sales, String> {
    
    // 1. Find by status
    List<Sales> findByStatus(String status);
    
    // 2. Find by date range
    List<Sales> findByDateBetween(LocalDate startDate, LocalDate endDate);
    
    // 3. Find by product and status
    List<Sales> findByProduct_IdAndStatus(String productId, String status);
    
    // 4. Count by status
    long countByStatus(String status);
    
    // 5. Find recent sales
    List<Sales> findTop10ByOrderByCreatedAtDesc();
}
```

**Create test endpoint**:
```java
@GetMapping("/by-status/{status}")
public ResponseEntity<List<Sales>> getSalesByStatus(@PathVariable String status) {
    return ResponseEntity.ok(salesRepository.findByStatus(status));
}
```

**Learning Goals**: Derived query methods, method naming conventions

#### Exercise 10: Write Custom JPQL Query

**Task**: Add a custom query to find total sales value by product

```java
@Query("""
    SELECT s.product.name, SUM(s.quantity * s.price) 
    FROM Sales s 
    WHERE s.status = 'CONFIRMED'
    GROUP BY s.product.name
    ORDER BY SUM(s.quantity * s.price) DESC
    """)
List<Object[]> findTotalSalesValueByProduct();

// Or use DTO projection
@Query("""
    SELECT new com.example.ProductSalesDto(
        s.product.name, 
        SUM(s.quantity * s.price)
    )
    FROM Sales s 
    WHERE s.status = 'CONFIRMED'
    GROUP BY s.product.name
    ORDER BY SUM(s.quantity * s.price) DESC
    """)
List<ProductSalesDto> findProductSalesSummary();
```

**Learning Goals**: JPQL, aggregations, projections

#### Exercise 11: Implement Pagination

**Task**: Add pagination to sales endpoint

```java
@GetMapping("/all")
public ResponseEntity<Page<SalesDto>> getAllSales(
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable) {
    Page<Sales> salesPage = salesRepository.findAll(pageable);
    Page<SalesDto> dtoPage = salesPage.map(salesMapper::toDto);
    return ResponseEntity.ok(dtoPage);
}
```

**Test pagination**:
```bash
# First page
curl "http://localhost:8081/api/v1/sales/all?page=0&size=5"

# Second page
curl "http://localhost:8081/api/v1/sales/all?page=1&size=5"

# Sort by price
curl "http://localhost:8081/api/v1/sales/all?page=0&size=5&sort=price,desc"

# Multiple sort fields
curl "http://localhost:8081/api/v1/sales/all?sort=status,asc&sort=date,desc"
```

**Learning Goals**: Pagination, sorting, Page object

---

### Day 11-12: Entities & Relationships

#### Exercise 12: Add a New Entity

**Task**: Create a `Category` entity and link it to `Product`

1. **Create Entity**
```java
@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @OneToMany(mappedBy = "category")
    private List<Products> products = new ArrayList<>();
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

2. **Update Product Entity**
```java
@Entity
@Table(name = "products")
public class Products {
    // ... existing fields
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
}
```

3. **Create Repository**
```java
public interface CategoryRepository extends JpaRepository<Category, String> {
    Optional<Category> findByName(String name);
}
```

4. **Create REST endpoints** (Controller, Service, DTOs)

**Learning Goals**: Entity relationships, migrations, CRUD operations

#### Exercise 13: Understand Lazy Loading

**Task**: Observe N+1 problem

```java
// Add this endpoint to SalesController
@GetMapping("/debug-lazy")
public ResponseEntity<List<String>> debugLazy() {
    List<Sales> sales = salesRepository.findAll(); // 1 query
    
    List<String> productNames = new ArrayList<>();
    for (Sales sale : sales) {
        productNames.add(sale.getProduct().getName()); // N queries!
    }
    
    return ResponseEntity.ok(productNames);
}
```

**Enable SQL logging**:
```properties
spring.jpa.show-sql=true
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

**Call endpoint and count queries in logs**

**Fix with JOIN FETCH**:
```java
@Query("SELECT s FROM Sales s JOIN FETCH s.product")
List<Sales> findAllWithProduct();
```

**Learning Goals**: Lazy loading, N+1 problem, JOIN FETCH

---

## 📅 Week 3: Security & Authorization

### Day 13-15: Role-Based Access Control

#### Exercise 14: Create a New Permission

1. **Add permission to database**
```sql
INSERT INTO permissions (id, code, description) 
VALUES ('perm_test', 'TEST_FEATURE', 'Access test feature');

-- Assign to a role
INSERT INTO role_permissions (role_id, permission_id)
VALUES ('sales_manager', 'perm_test');
```

2. **Create protected endpoint**
```java
@PreAuthorize("hasAuthority('TEST_FEATURE')")
@GetMapping("/test-feature")
public ResponseEntity<String> testFeature() {
    return ResponseEntity.ok("You have TEST_FEATURE permission!");
}
```

3. **Test with different users**
```bash
# User with permission - should work
curl -X GET http://localhost:8081/api/v1/test-feature \
  -H "Authorization: Bearer $TOKEN_MANAGER"

# User without permission - should fail (403)
curl -X GET http://localhost:8081/api/v1/test-feature \
  -H "Authorization: Bearer $TOKEN_REGULAR"
```

**Learning Goals**: Permission management, @PreAuthorize

#### Exercise 15: Implement Row-Level Security

**Task**: Users can only view their own sales

1. **Add createdBy field to Sales**
```java
@Entity
public class Sales {
    // ... existing fields
    
    @Column(name = "created_by")
    private String createdBy;
}
```

2. **Set createdBy on creation**
```java
@Service
public class SalesService {
    
    private final CurrentUserService currentUserService;
    
    @Transactional
    public Sales createSale(CreateSaleRequest request) {
        String currentUsername = currentUserService.getCurrentUsername();
        
        Sales sale = Sales.builder()
            // ... other fields
            .createdBy(currentUsername)
            .build();
        
        return salesRepository.save(sale);
    }
}
```

3. **Filter by user**
```java
@Service
public class SecurityService {
    
    public boolean canAccessSale(String saleId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        // Admin can access all
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"))) {
            return true;
        }
        
        // Others can only access their own
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new NotFoundException("Sale not found"));
        
        return sale.getCreatedBy().equals(username);
    }
}

// In controller
@PreAuthorize("@securityService.canAccessSale(#id)")
@GetMapping("/{id}")
public ResponseEntity<Sales> getSale(@PathVariable String id) {
    return ResponseEntity.ok(salesService.findById(id));
}
```

**Learning Goals**: Row-level security, custom security logic

---

### Day 16-17: Exception Handling

#### Exercise 16: Add Custom Exceptions

**Task**: Create domain-specific exceptions

```java
// Custom exception
public class SaleNotFoundException extends RuntimeException {
    public SaleNotFoundException(String saleId) {
        super("Sale not found with ID: " + saleId);
    }
}

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String product, double requested, double available) {
        super(String.format("Insufficient stock for %s. Requested: %.2f, Available: %.2f", 
            product, requested, available));
    }
}

// Exception handler
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(SaleNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleSaleNotFound(SaleNotFoundException ex) {
        ApiErrorResponse error = new ApiErrorResponse(
            "NOT_FOUND",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientStock(InsufficientStockException ex) {
        ApiErrorResponse error = new ApiErrorResponse(
            "INSUFFICIENT_STOCK",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
}
```

**Use in service**:
```java
public Sales findById(String id) {
    return salesRepository.findById(id)
        .orElseThrow(() -> new SaleNotFoundException(id));
}
```

**Learning Goals**: Custom exceptions, global error handling

---

## 📅 Week 4: Advanced Topics

### Day 18-20: Transactions & Concurrency

#### Exercise 17: Test Transaction Rollback

**Task**: Verify transaction behavior

```java
@Service
public class OrderService {
    
    private final SalesRepository salesRepository;
    private final StockRepository stockRepository;
    
    @Transactional
    public void processOrder(String saleId) {
        Sales sale = salesRepository.findById(saleId)
            .orElseThrow(() -> new NotFoundException("Sale not found"));
        
        // Update sale status
        sale.setStatus("CONFIRMED");
        
        // Update stock (simulated error)
        Stock stock = stockRepository.findByProduct(sale.getProduct())
            .orElseThrow(() -> new NotFoundException("Stock not found"));
        
        if (stock.getQuantity() < sale.getQuantity()) {
            throw new InsufficientStockException("Not enough stock");
        }
        
        stock.setQuantity(stock.getQuantity() - sale.getQuantity());
        
        // If exception occurs here, BOTH sale and stock updates are rolled back
        
        if (sale.getQuantity() > 1000) {
            throw new RuntimeException("Order too large");
        }
    }
}
```

**Test**:
1. Create sale with quantity > 1000
2. Call processOrder
3. Check database - sale status should be unchanged (rollback worked)

**Learning Goals**: Transaction boundaries, rollback behavior

#### Exercise 18: Optimistic Locking

**Task**: Handle concurrent updates

```java
@Entity
public class Sales {
    // ... existing fields
    
    @Version
    private Long version;
}
```

**Test concurrent updates**:
```java
@Test
void testOptimisticLocking() {
    // Thread 1 loads sale
    Sales sale1 = salesRepository.findById("123").get();
    
    // Thread 2 loads same sale
    Sales sale2 = salesRepository.findById("123").get();
    
    // Thread 1 updates
    sale1.setPrice(1000.0);
    salesRepository.save(sale1); // Success
    
    // Thread 2 updates (version mismatch)
    sale2.setPrice(2000.0);
    salesRepository.save(sale2); // Throws OptimisticLockException
}
```

**Learning Goals**: Optimistic locking, concurrency control

---

### Day 21-23: Testing

#### Exercise 19: Write Unit Tests

**Task**: Test service layer

```java
@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {
    
    @Mock
    private CompanyRepository companyRepository;
    
    @Mock
    private CompanyMapper companyMapper;
    
    @InjectMocks
    private CompanyService companyService;
    
    @Test
    void shouldCreateCompany() {
        // Given
        CreateCompanyRequest request = new CreateCompanyRequest("Acme Corp");
        Companies company = Companies.builder()
            .displayName("Acme Corp")
            .searchKey("acmecorp")
            .build();
        Companies savedCompany = Companies.builder()
            .id("123")
            .displayName("Acme Corp")
            .searchKey("acmecorp")
            .build();
        CompanySuggestionResposne response = new CompanySuggestionResposne("123", "Acme Corp");
        
        when(companyRepository.findBySearchKey("acmecorp")).thenReturn(Optional.empty());
        when(companyRepository.save(any(Companies.class))).thenReturn(savedCompany);
        when(companyMapper.toResponse(savedCompany)).thenReturn(response);
        
        // When
        CompanySuggestionResposne result = companyService.createCompany(request);
        
        // Then
        assertNotNull(result);
        assertEquals("Acme Corp", result.displayName());
        verify(companyRepository).save(any(Companies.class));
    }
    
    @Test
    void shouldThrowExceptionWhenCompanyExists() {
        // Given
        CreateCompanyRequest request = new CreateCompanyRequest("Acme Corp");
        Companies existingCompany = Companies.builder().build();
        when(companyRepository.findBySearchKey("acmecorp"))
            .thenReturn(Optional.of(existingCompany));
        
        // When & Then
        assertThrows(CompanyAlreadyExistsException.class, 
            () -> companyService.createCompany(request));
    }
}
```

**Learning Goals**: Mocking, unit testing, verification

#### Exercise 20: Write Integration Tests

**Task**: Test controller with real database

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CompanyControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private CompanyRepository companyRepository;
    
    @Test
    @WithMockUser(authorities = {"COMPANY_CREATE"})
    void shouldCreateCompany() throws Exception {
        CreateCompanyRequest request = new CreateCompanyRequest("Test Company");
        
        mockMvc.perform(post("/api/v1/companies/create-company")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Company created successfully!"))
                .andExpect(jsonPath("$.data.displayName").value("Test Company"));
        
        // Verify in database
        Optional<Companies> saved = companyRepository.findByDisplayNameIgnoreCase("Test Company");
        assertTrue(saved.isPresent());
    }
    
    @Test
    @WithMockUser(authorities = {"COMPANY_CREATE"})
    void shouldReturn409WhenCompanyExists() throws Exception {
        // Create company first
        Companies existing = Companies.builder()
            .displayName("Existing Company")
            .searchKey("existingcompany")
            .build();
        companyRepository.save(existing);
        
        // Try to create duplicate
        CreateCompanyRequest request = new CreateCompanyRequest("Existing Company");
        
        mockMvc.perform(post("/api/v1/companies/create-company")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Resource Conflict"));
    }
}
```

**Learning Goals**: Integration testing, MockMvc, database verification

---

### Day 24-25: Performance & Optimization

#### Exercise 21: Add Database Indexes

**Task**: Analyze and optimize queries

1. **Enable query logging**
```properties
spring.jpa.properties.hibernate.generate_statistics=true
logging.level.org.hibernate.stat=DEBUG
```

2. **Identify slow queries** from logs

3. **Add indexes**
```sql
-- Add index on frequently queried column
CREATE INDEX idx_sales_status ON sales(status);
CREATE INDEX idx_sales_date ON sales(date);
CREATE INDEX idx_sales_status_date ON sales(status, date);

-- Analyze query plan
EXPLAIN ANALYZE 
SELECT * FROM sales WHERE status = 'CONFIRMED' AND date >= '2024-01-01';
```

4. **Add JPA indexes**
```java
@Entity
@Table(name = "sales", indexes = {
    @Index(name = "idx_sales_status", columnList = "status"),
    @Index(name = "idx_sales_date", columnList = "date"),
    @Index(name = "idx_sales_status_date", columnList = "status, date")
})
public class Sales { ... }
```

**Learning Goals**: Query optimization, indexing strategy

#### Exercise 22: Implement Caching

**Task**: Cache frequently accessed data

1. **Add dependency**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

2. **Enable caching**
```java
@SpringBootApplication
@EnableCaching
public class ChemOsApplication { ... }
```

3. **Cache method results**
```java
@Service
public class ProductService {
    
    @Cacheable(value = "products", key = "#id")
    public ProductDto findById(String id) {
        // This will be cached
        return productRepository.findById(id)
            .map(productMapper::toDto)
            .orElseThrow(() -> new NotFoundException("Product not found"));
    }
    
    @CacheEvict(value = "products", key = "#id")
    public ProductDto update(String id, UpdateProductRequest request) {
        // This will invalidate cache
        // ...
    }
    
    @CacheEvict(value = "products", allEntries = true)
    public void deleteAll() {
        // Clear entire cache
    }
}
```

**Learning Goals**: Caching strategies, cache invalidation

---

## 📅 Week 5: Real-World Features

### Day 26-30: Build Complete Feature

#### Exercise 23: Implement Audit Log

**Task**: Track all user actions

1. **Create AuditLog entity** (already exists in your codebase)

2. **Create AuditService**
```java
@Service
@RequiredArgsConstructor
public class AuditService {
    
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;
    
    public void logAction(String action, String entity, String entityId, String details) {
        String username = currentUserService.getCurrentUsername();
        
        AuditLog log = AuditLog.builder()
            .username(username)
            .action(action)
            .entity(entity)
            .entityId(entityId)
            .details(details)
            .timestamp(LocalDateTime.now())
            .build();
        
        auditLogRepository.save(log);
    }
}
```

3. **Integrate in services**
```java
@Service
public class SalesService {
    
    private final AuditService auditService;
    
    @Transactional
    public Sales confirmSale(String id) {
        Sales sale = findById(id);
        sale.setStatus("CONFIRMED");
        
        auditService.logAction(
            "CONFIRM_SALE",
            "SALE",
            id,
            "Sale confirmed by user"
        );
        
        return sale;
    }
}
```

4. **Create audit log viewer**
```java
@GetMapping("/audit")
@PreAuthorize("hasAuthority('AUDIT_VIEW')")
public ResponseEntity<Page<AuditLog>> getAuditLogs(
        @RequestParam(required = false) String username,
        @RequestParam(required = false) String action,
        Pageable pageable) {
    return ResponseEntity.ok(auditService.findLogs(username, action, pageable));
}
```

**Learning Goals**: Audit logging, compliance, traceability

---

## 🎓 Final Project: Build Complete Module

### Exercise 24: Inventory Management Module

**Task**: Build a complete inventory tracking feature

**Requirements**:
1. Entities: Warehouse, Stock, StockMovement
2. Relationships: Warehouse → Stocks → Product
3. Features:
   - Add/remove stock
   - Transfer between warehouses
   - Stock movement history
   - Low stock alerts
   - Stock valuation report
4. Security: Role-based access
5. Validation: Prevent negative stock
6. Audit: Log all movements
7. Testing: Unit + Integration tests

**Deliverables**:
- [ ] Database schema & migrations
- [ ] JPA entities with relationships
- [ ] Repositories with custom queries
- [ ] Service layer with business logic
- [ ] REST controllers with proper status codes
- [ ] DTOs for all requests/responses
- [ ] Exception handling
- [ ] Permission-based security
- [ ] Audit logging
- [ ] Unit tests (>80% coverage)
- [ ] Integration tests
- [ ] API documentation

---

## 📚 Additional Challenges

### Challenge 1: Implement File Upload
- CSV import for bulk product creation
- Excel export for sales reports
- Image upload for products

### Challenge 2: Add Async Processing
- Email notifications on sale confirmation
- Scheduled job for daily reports
- Background task for data cleanup

### Challenge 3: Add Validation Rules
- Custom validators
- Cross-field validation
- Business rule validation

### Challenge 4: Implement Search
- Full-text search across entities
- Filter builder pattern
- Advanced query DSL

### Challenge 5: Add Metrics & Monitoring
- Custom Actuator endpoints
- Prometheus metrics
- Application health checks

---

## ✅ Checklist: What You Should Know

After completing this path, you should be able to:

### Spring Boot Fundamentals
- [ ] Explain auto-configuration
- [ ] Use dependency injection effectively
- [ ] Configure application properties
- [ ] Use profiles (dev/prod)

### REST APIs
- [ ] Design RESTful endpoints
- [ ] Handle requests/responses
- [ ] Implement validation
- [ ] Handle exceptions globally

### Data Access
- [ ] Create JPA entities
- [ ] Define relationships (@ManyToOne, @OneToMany)
- [ ] Write custom queries (JPQL, native SQL)
- [ ] Implement pagination
- [ ] Avoid N+1 queries

### Security
- [ ] Implement JWT authentication
- [ ] Configure Spring Security
- [ ] Use @PreAuthorize
- [ ] Implement RBAC
- [ ] Handle authentication errors

### Best Practices
- [ ] Use DTOs for API contracts
- [ ] Apply builder pattern
- [ ] Write transactional code
- [ ] Add proper indexes
- [ ] Handle concurrency

### Testing
- [ ] Write unit tests with Mockito
- [ ] Write integration tests
- [ ] Test security
- [ ] Test exception handling

---

## 📖 Resources for Deep Dive

### Documentation
1. Spring Boot Reference: https://docs.spring.io/spring-boot/docs/current/reference/html/
2. Spring Data JPA: https://docs.spring.io/spring-data/jpa/docs/current/reference/html/
3. Spring Security: https://docs.spring.io/spring-security/reference/

### Practice
1. Refactoring Guru (Design Patterns): https://refactoring.guru/
2. Baeldung (Tutorials): https://www.baeldung.com/
3. PostgreSQL Documentation: https://www.postgresql.org/docs/

### Tools
1. Postman: API testing
2. DBeaver: Database management
3. IntelliJ IDEA: IDE debugging
4. Git: Version control

---

**🚀 You've got this! Start with Week 1 and work your way through.**

**Remember**: Understanding beats memorization. Debug, experiment, break things, fix them!
