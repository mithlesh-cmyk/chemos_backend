# ChemOS Spring Boot Learning Guide

## 📚 Overview
This document explains all the Spring Boot concepts, patterns, and technologies used in your ChemOS application. As a 3-year experienced developer, this guide will help you understand the architecture and best practices implemented in this codebase.

---

## 🏗️ Application Architecture

### Project Structure
```
src/main/java/chemos/chem_os/
├── ChemOsApplication.java          # Main application entry point
├── auth/                           # Authentication & Authorization module
│   ├── config/                     # Configuration & data seeders
│   ├── controller/                 # Auth REST endpoints
│   ├── dto/                        # Data Transfer Objects for auth
│   ├── model/                      # User, Role, Permission entities
│   ├── repository/                 # Database repositories
│   ├── security/                   # JWT & Security configuration
│   └── service/                    # Business logic for auth
├── controller/                     # Business domain REST controllers
├── dto/                           # Request/Response objects
├── exception/                     # Global exception handling
├── mapper/                        # Entity ↔ DTO converters
├── model/                         # JPA entities (database tables)
├── repository/                    # Data access layer
└── services/                      # Business logic layer
```

---

## 🔧 Core Technologies & Versions

### Technology Stack
- **Spring Boot**: 4.0.6 (Latest version with modern features)
- **Java**: 21 (LTS with modern language features)
- **PostgreSQL**: Production-grade relational database
- **Maven**: Build and dependency management

### Key Dependencies
```xml
<!-- Web & REST APIs -->
spring-boot-starter-webmvc

<!-- Database & ORM -->
spring-boot-starter-data-jpa
postgresql

<!-- Security & JWT -->
spring-boot-starter-security
jjwt-api, jjwt-impl, jjwt-jackson (0.11.5)

<!-- Utilities -->
lombok                    # Reduces boilerplate code
spring-boot-starter-validation
jackson-databind         # JSON serialization
commons-csv              # CSV processing
spring-boot-starter-actuator  # Health checks & monitoring
```

---

## 🎯 Key Concepts You Should Learn

## 1. **Spring Boot Annotations**

### Application-Level Annotations
```java
@SpringBootApplication
// Combines three annotations:
// - @Configuration: Marks class as source of bean definitions
// - @EnableAutoConfiguration: Auto-configures Spring based on dependencies
// - @ComponentScan: Scans for components in current package and sub-packages

@EnableScheduling
// Enables Spring's scheduled task execution capability
// Allows using @Scheduled annotation for background jobs
```

### Component Stereotypes
```java
@RestController
// Combines @Controller + @ResponseBody
// All methods return data (JSON/XML) instead of views
// Used for REST API endpoints

@Service
// Marks a class as a service layer component
// Contains business logic
// Auto-detected during component scanning

@Repository
// Marks a class as a data access layer component
// Provides automatic exception translation for database errors

@Component
// Generic stereotype for any Spring-managed component
// Use when class doesn't fit @Service, @Repository, or @Controller

@Configuration
// Indicates that class declares one or more @Bean methods
// Used for Java-based Spring configuration
```

### Dependency Injection
```java
@RequiredArgsConstructor  // Lombok annotation
// Generates constructor with all 'final' fields
// Enables constructor-based dependency injection (recommended approach)

// Example:
@Service
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyRepository companyRepository;  // Injected via constructor
    private final CompanyMapper companyMapper;
}
```

---

## 2. **REST API Design Patterns**

### Controller Pattern
```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/companies")  // Base path for all endpoints
public class CompanyController {
    
    private final CompanyService companyService;
    
    // POST endpoint - Create new resource
    @PostMapping("/create-company")
    public ResponseEntity<CompanyCreationResponse<CompanySuggestionResposne>> createCompany(
            @RequestBody CreateCompanyRequest companyRequest) {
        // @RequestBody: Converts JSON request body to Java object
        
        CompanySuggestionResposne data = companyService.createCompany(companyRequest);
        
        CompanyCreationResponse<CompanySuggestionResposne> response = 
            new CompanyCreationResponse<>("Company created successfully!", data);
        
        // ResponseEntity: Gives full control over HTTP response (status, headers, body)
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    // GET endpoint - Read resources
    @GetMapping("/search")
    public List<CompanySuggestionResposne> searchCompanies(
            @RequestParam(value = "query", required = false, defaultValue = "") String query) {
        // @RequestParam: Extracts query parameters from URL
        // Example: /search?query=acme
        return companyService.searchCompanies(query);
    }
}
```

### HTTP Methods & Status Codes
- **GET**: Retrieve data (200 OK)
- **POST**: Create new resource (201 CREATED)
- **PUT**: Update entire resource (200 OK)
- **PATCH**: Partial update (200 OK)
- **DELETE**: Remove resource (204 NO CONTENT)

---

## 3. **Security Architecture (JWT)**

### How JWT Authentication Works

```
┌─────────┐                 ┌──────────┐                 ┌──────────┐
│ Client  │                 │  Server  │                 │ Database │
└────┬────┘                 └─────┬────┘                 └─────┬────┘
     │                            │                            │
     │ 1. POST /login             │                            │
     │   {username, password}     │                            │
     ├───────────────────────────>│                            │
     │                            │ 2. Verify credentials      │
     │                            ├───────────────────────────>│
     │                            │<───────────────────────────┤
     │                            │ 3. Generate JWT token      │
     │                            │    (signed with secret)    │
     │ 4. Return JWT token        │                            │
     │<───────────────────────────┤                            │
     │                            │                            │
     │ 5. GET /api/resource       │                            │
     │   Header: Authorization:   │                            │
     │           Bearer <token>   │                            │
     ├───────────────────────────>│                            │
     │                            │ 6. Validate token          │
     │                            │    Extract username/role   │
     │                            │ 7. Load user permissions   │
     │                            ├───────────────────────────>│
     │                            │<───────────────────────────┤
     │                            │ 8. Check authorization     │
     │ 9. Return response         │    (@PreAuthorize)         │
     │<───────────────────────────┤                            │
```

### Key Security Components

#### 1. SecurityConfig.java
```java
@Configuration
@EnableMethodSecurity  // Enables @PreAuthorize annotations
public class SecurityConfig {
    
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())  // Disabled for stateless JWT APIs
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // No sessions
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/login").permitAll()  // Public endpoints
                .anyRequest().authenticated()  // All other endpoints require authentication
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

#### 2. JwtService.java
```java
@Service
public class JwtService {
    
    @Value("${jwt.secret}")
    private String secret;  // Loaded from application.properties
    
    // Creates JWT token with username and role
    public String generateToken(String username, String role) {
        return Jwts.builder()
            .setSubject(username)
            .claim("role", role)  // Custom claim
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 86400000))  // 24 hours
            .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256)
            .compact();
    }
    
    // Validates and parses JWT token
    public boolean isValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

#### 3. JwtAuthFilter.java
```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        
        String authHeader = request.getHeader("Authorization");
        
        // Extract token from "Bearer <token>" format
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            if (jwtService.isValid(token)) {
                String username = jwtService.extractUsername(token);
                
                // Load user and permissions from database
                User user = userRepository.findByUsernameWithPermissions(username).orElse(null);
                
                if (user != null && user.getIsActive()) {
                    // Set authentication in Spring Security context
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

### Authorization with @PreAuthorize
```java
@RestController
@RequestMapping("api/v1/sales")
public class SalesController {
    
    @PreAuthorize("hasAuthority('SALE_CREATE')")
    // Checks if authenticated user has 'SALE_CREATE' permission
    // Returns 403 Forbidden if permission is missing
    @PostMapping("/create/sales_order")
    public ResponseEntity<Sales> salesForm(@RequestBody CreateSaleRequest salesRecord) {
        Sales savedSales = salesService.createSale(salesRecord);
        return ResponseEntity.ok(savedSales);
    }
}
```

---

## 4. **Database & JPA Concepts**

### JPA Entity Mapping
```java
@Data                    // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor       // Lombok: generates no-args constructor (required by JPA)
@AllArgsConstructor      // Lombok: generates constructor with all fields
@Builder(toBuilder = true)  // Lombok: enables builder pattern
@Entity                  // JPA: marks class as database entity
@Table(name = "sales_form", indexes = {
    @Index(name = "idx_sales_form_status_market_status_date", 
           columnList = "status, market_status, date")
})  // Specifies table name and database indexes
public class Sales {
    
    @Id  // Primary key
    @GeneratedValue(strategy = GenerationType.UUID)  // Auto-generate UUID
    @Column(name = "id")
    private String id;
    
    private LocalDate date;  // Column name = field name (snake_case conversion)
    
    @Column(name = "sale_type")  // Custom column name
    private String salesType;
    
    @ManyToOne(fetch = FetchType.LAZY)  // Many sales -> One product
    @JoinColumn(name = "product_id")     // Foreign key column
    private Products product;
    
    @Column(columnDefinition = "TEXT")   // PostgreSQL TEXT type
    private String message;
    
    @CreationTimestamp  // Hibernate: auto-set on insert
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp   // Hibernate: auto-update on modification
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### Relationship Types
```java
// One-to-Many: One company has many sales
@OneToMany(mappedBy = "company", cascade = CascadeType.ALL)
private List<Sales> sales;

// Many-to-One: Many sales belong to one company
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "company_id")
private Company company;

// Many-to-Many: Roles and Permissions
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
    name = "role_permissions",
    joinColumns = @JoinColumn(name = "role_id"),
    inverseJoinColumns = @JoinColumn(name = "permission_id")
)
private Set<Permission> permissions;
```

### Fetch Types
- **EAGER**: Load related entities immediately (default for @ManyToOne, @OneToOne)
- **LAZY**: Load related entities only when accessed (default for @OneToMany, @ManyToMany)
- **Recommendation**: Use LAZY to avoid N+1 query problems

---

## 5. **Spring Data JPA Repositories**

### Basic Repository
```java
public interface CompanyRepository extends JpaRepository<Companies, String> {
    // JpaRepository<Entity, ID Type> provides:
    // - save(), findById(), findAll(), delete(), count(), etc.
    
    // Query methods derived from method name
    Optional<Companies> findBySearchKey(String searchKey);
    Optional<Companies> findByDisplayNameIgnoreCase(String displayName);
}
```

### Custom Queries
```java
public interface CompanyRepository extends JpaRepository<Companies, String> {
    
    @Query(value = """
        SELECT *
        FROM companies
        WHERE
            :prefix = ''
            OR search_key LIKE CONCAT('%', :prefix, '%')
            OR word_similarity(search_key, :prefix) >= 0.20
        ORDER BY
            CASE
                WHEN search_key = :prefix THEN 1
                WHEN search_key LIKE CONCAT(:prefix, '%') THEN 2
                ELSE 3
            END
        LIMIT :limit
        """,
        nativeQuery = true)  // Use native SQL (PostgreSQL-specific)
    List<Companies> findSuggestions(
        @Param("prefix") String prefix,
        @Param("limit") int limit
    );
}
```

### Pagination & Sorting
```java
@GetMapping("/allSales")
public ResponseEntity<Page<Sales>> getAllSales(
        @RequestParam(required = false) String status,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) 
        Pageable pageable) {
    // Pageable enables pagination and sorting
    // Example: /allSales?page=0&size=20&sort=date,desc
    return ResponseEntity.ok(salesService.getAllSales(status, product, pageable));
}
```

---

## 6. **Data Transfer Objects (DTOs)**

### Why Use DTOs?
1. **Decoupling**: Separate API contract from database schema
2. **Security**: Don't expose internal entity structure
3. **Performance**: Send only necessary data
4. **Validation**: Validate incoming data before processing

### Record Pattern (Java 14+)
```java
// Immutable data carrier
// Automatically generates: constructor, getters, equals(), hashCode(), toString()
public record CreateCompanyRequest(
    String companyName
) {}

public record CompanySuggestionResposne(
    String id,
    String displayName
) {}

// With Jackson annotations
public record CreateSaleRequest(
    LocalDate date,
    String salesType,
    
    @JsonAlias({"product", "productId"})  // Accept both field names from JSON
    String productId,
    
    Double quantity,
    Double price
) {}
```

### Response Wrapper Pattern
```java
public record ApiErrorResponse(
    String error,
    String message,
    LocalDateTime timestamp
) {}

public record CompanyCreationResponse<T>(
    String message,
    T data
) {}
```

---

## 7. **Mapper Pattern**

### Why Mappers?
Separates conversion logic from business logic

```java
@Component  // Spring-managed bean
public class CompanyMapper {
    
    // Entity -> DTO
    public CompanySuggestionResposne toResponse(Companies company) {
        return new CompanySuggestionResposne(
            company.getId(),
            company.getDisplayName()
        );
    }
    
    // DTO -> Entity (if needed)
    public Companies toEntity(CreateCompanyRequest request) {
        return Companies.builder()
            .displayName(request.companyName())
            .build();
    }
}
```

### Usage in Service Layer
```java
@Service
@RequiredArgsConstructor
public class CompanyService {
    
    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    
    public List<CompanySuggestionResposne> searchCompanies(String query) {
        return companyRepository
            .findSuggestions(searchKey, 20)
            .stream()
            .map(companyMapper::toResponse)  // Method reference
            .toList();
    }
}
```

---

## 8. **Service Layer Pattern**

### Service Responsibilities
- Business logic
- Transaction management
- Orchestrating multiple repositories
- Data validation
- Error handling

```java
@Service
@RequiredArgsConstructor
public class CompanyService {
    
    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    
    @Transactional  // Wraps method in database transaction
    public CompanySuggestionResposne createCompany(CreateCompanyRequest companyRequest) {
        String displayName = CompanySanitizer.sanitizeDisplayName(companyRequest.companyName());
        String searchKey = CompanySanitizer.createSearchKey(companyRequest.companyName());
        
        // Business validation
        if (companyRepository.findBySearchKey(searchKey).isPresent()) {
            throw new CompnayAlreadyExistsException("A company with this name already exists!");
        }
        
        Companies companies = Companies.builder()
            .displayName(displayName)
            .searchKey(searchKey)
            .build();
        
        Companies savedCompany = companyRepository.save(companies);
        return companyMapper.toResponse(savedCompany);
    }
}
```

### Transaction Management
```java
@Transactional  // Ensures all-or-nothing database operations
// If exception occurs, all changes are rolled back
// Properties:
// - readOnly = true: Optimization for read-only operations
// - isolation: Control concurrent access
// - propagation: Control transaction boundaries
```

---

## 9. **Exception Handling**

### Global Exception Handler
```java
@RestControllerAdvice  // Applies to all @RestController classes
public class GlobalExceptionHandler {
    
    @ExceptionHandler(CompnayAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleCompanyAlreadyExists(
            CompnayAlreadyExistsException ex) {
        
        ApiErrorResponse errorBody = new ApiErrorResponse(
            "Resource Conflict",
            ex.getMessage(),
            LocalDateTime.now()
        );
        
        return new ResponseEntity<>(errorBody, HttpStatus.CONFLICT);  // 409
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex) {
        
        ApiErrorResponse errorBody = new ApiErrorResponse(
            "Bad Request",
            ex.getMessage(),
            LocalDateTime.now()
        );
        
        return new ResponseEntity<>(errorBody, HttpStatus.BAD_REQUEST);  // 400
    }
}
```

### Custom Exceptions
```java
public class CompnayAlreadyExistsException extends RuntimeException {
    public CompnayAlreadyExistsException(String message) {
        super(message);
    }
}
```

---

## 10. **Lombok Library**

### Common Lombok Annotations

```java
@Data
// Generates: getters, setters, toString, equals, hashCode
// Warning: Can cause issues with JPA entities (use @Getter/@Setter instead)

@NoArgsConstructor
// Generates no-argument constructor (required by JPA)

@AllArgsConstructor
// Generates constructor with all fields

@RequiredArgsConstructor
// Generates constructor with 'final' and '@NonNull' fields
// Perfect for dependency injection

@Builder
// Implements builder pattern
Sales sale = Sales.builder()
    .date(LocalDate.now())
    .quantity(100.0)
    .price(1000.0)
    .build();

@Builder(toBuilder = true)
// Enables creating modified copies
Sales updatedSale = originalSale.toBuilder()
    .price(1200.0)
    .build();

@Slf4j
// Creates logger field: private static final Logger log = LoggerFactory.getLogger(...)
log.info("Processing sale: {}", saleId);
log.error("Error occurred", exception);
```

---

## 11. **Configuration & Properties**

### Application Properties
```properties
# application.properties

# Application name
spring.application.name=chem-os

# Server port
server.port=8081

# Database configuration
spring.datasource.url=jdbc:postgresql://host:5432/chemos_dev
spring.datasource.username=chemos
spring.datasource.password=chemos@123

# JPA/Hibernate configuration
spring.jpa.hibernate.ddl-auto=update  # Auto-update schema (use 'validate' in production)
spring.jpa.show-sql=true              # Log SQL queries (disable in production)
spring.jpa.properties.hibernate.format_sql=true

# Jackson (JSON) configuration
spring.jackson.time-zone=Asia/Kolkata

# Custom properties
jwt.secret=your-secret-key-here
```

### Injecting Properties
```java
@Service
public class JwtService {
    
    @Value("${jwt.secret}")  // Injects value from application.properties
    private String secret;
}
```

### Profile-Specific Configuration
```
application.properties          # Common properties
application-dev.properties      # Development environment
application-prod.properties     # Production environment
```

Activate profile:
```bash
java -jar app.jar --spring.profiles.active=prod
```

---

## 12. **CORS Configuration**

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    
    // Allowed origins (frontend URLs)
    config.setAllowedOrigins(List.of(
        "http://localhost:3000",
        "http://localhost:5173"
    ));
    
    // Allowed HTTP methods
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    
    // Allowed headers
    config.setAllowedHeaders(List.of("*"));
    
    // Expose headers to browser
    config.setExposedHeaders(List.of("Authorization"));
    
    // Allow cookies
    config.setAllowCredentials(true);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);  // Apply to all endpoints
    return source;
}
```

---

## 13. **Jackson (JSON Processing)**

### Configuration
```java
@Bean
public ObjectMapper objectMapper() {
    return new ObjectMapper()
        .registerModule(new JavaTimeModule())  // Support Java 8 date/time
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);  // ISO-8601 format
}
```

### Annotations
```java
public record CreateSaleRequest(
    @JsonAlias({"product", "productId"})  // Accept multiple field names
    String productId,
    
    @JsonProperty("sale_date")  // Map to different JSON field name
    LocalDate date,
    
    @JsonIgnore  // Exclude from JSON serialization
    String internalField
) {}
```

---

## 14. **Validation**

### Bean Validation Annotations
```java
public record CreateUserRequest(
    @NotNull(message = "Username is required")
    @Size(min = 3, max = 50)
    String username,
    
    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8)
    String password,
    
    @Email(message = "Invalid email format")
    String email,
    
    @Pattern(regexp = "^[A-Z]{2,10}$")
    String roleId
) {}
```

### Controller Validation
```java
@PostMapping("/users")
public ResponseEntity<UserResponse> createUser(
        @Valid @RequestBody CreateUserRequest request) {
    // @Valid triggers validation
    // Returns 400 Bad Request if validation fails
    return ResponseEntity.ok(authService.createUser(request));
}
```

---

## 15. **Actuator (Monitoring)**

### Dependency
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Endpoints
```
GET /actuator/health    # Application health status
GET /actuator/info      # Application information
GET /actuator/metrics   # Application metrics
```

### Configuration
```properties
# application.properties
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
```

---

## 16. **Modern Java Features Used**

### Records (Java 14+)
```java
// Immutable data carriers
public record UserResponse(String username, String email, String role) {}
```

### Text Blocks (Java 13+)
```java
@Query(value = """
    SELECT *
    FROM companies
    WHERE name LIKE :query
    """,
    nativeQuery = true)
```

### var Keyword (Java 10+)
```java
var companies = companyRepository.findAll();  // Type inference
```

### Stream API
```java
return companyRepository.findAll()
    .stream()
    .filter(c -> c.isActive())
    .map(companyMapper::toResponse)
    .toList();  // Java 16+
```

---

## 📝 Key Patterns & Best Practices in This Codebase

### 1. **Layered Architecture**
```
Controller → Service → Repository → Database
```
- **Controller**: HTTP handling, request/response mapping
- **Service**: Business logic, transactions
- **Repository**: Data access
- **Clear separation of concerns**

### 2. **Constructor Injection**
```java
@RequiredArgsConstructor  // Generates constructor for final fields
private final CompanyService companyService;  // Immutable dependency
```
Why? Testable, immutable, required dependencies are clear.

### 3. **DTO Pattern**
- Never expose entities directly in APIs
- Use DTOs for request/response
- Use mappers for conversion

### 4. **Builder Pattern**
```java
Companies company = Companies.builder()
    .displayName("Acme Corp")
    .searchKey("acmecorp")
    .build();
```

### 5. **Optional Usage**
```java
Optional<Company> company = companyRepository.findById(id);
company.ifPresent(c -> log.info("Found: {}", c));
Company c = company.orElseThrow(() -> new NotFoundException("Company not found"));
```

### 6. **RESTful API Design**
- Proper HTTP methods (GET, POST, PUT, PATCH, DELETE)
- Meaningful status codes (200, 201, 400, 401, 403, 404, 409, 500)
- Version in URL (`/api/v1/`)
- Resource-based URLs (`/companies`, `/sales`)

---

## 🎓 Learning Roadmap for You

### Phase 1: Core Spring Boot (Week 1-2)
1. ✅ Spring Boot basics (auto-configuration, starter dependencies)
2. ✅ Dependency Injection & IoC container
3. ✅ Component scanning & stereotypes
4. ✅ Configuration management (@Value, properties files)

### Phase 2: Web & REST (Week 3-4)
1. ✅ REST controllers & request mappings
2. ✅ Request/Response handling
3. ✅ Exception handling (@RestControllerAdvice)
4. ✅ Validation (@Valid, Bean Validation)

### Phase 3: Data Access (Week 5-6)
1. ✅ JPA entities & mappings
2. ✅ Spring Data repositories
3. ✅ Query methods & custom queries
4. ✅ Transactions (@Transactional)
5. ✅ Pagination & sorting

### Phase 4: Security (Week 7-8)
1. ✅ Spring Security architecture
2. ✅ JWT authentication flow
3. ✅ Authorization (@PreAuthorize)
4. ✅ Role-based access control (RBAC)

### Phase 5: Advanced Topics (Week 9-10)
1. ⏳ Logging & monitoring (Actuator, Slf4j)
2. ⏳ Testing (Unit tests, Integration tests)
3. ⏳ Caching (Spring Cache)
4. ⏳ Async processing (@Async)
5. ⏳ Scheduled tasks (@Scheduled)

---

## 📖 Recommended Resources

### Official Documentation
1. **Spring Boot Reference**: https://docs.spring.io/spring-boot/docs/current/reference/html/
2. **Spring Data JPA**: https://docs.spring.io/spring-data/jpa/docs/current/reference/html/
3. **Spring Security**: https://docs.spring.io/spring-security/reference/

### Books
1. **"Spring Boot in Action"** by Craig Walls
2. **"Spring Microservices in Action"** by John Carnell

### Courses
1. **Udemy**: "Spring Boot 3, Spring 6 & Hibernate" by Chad Darby
2. **Baeldung**: Spring Boot tutorials (https://www.baeldung.com/spring-boot)

### Practice Projects
1. Build a simple CRUD REST API
2. Add JWT authentication
3. Implement role-based authorization
4. Add pagination and filtering
5. Write unit and integration tests

---

## 🔍 Understanding Your Specific Code

### Role & Permission System
Your app uses a sophisticated RBAC system:
1. **User** has one **Role**
2. **Role** has multiple **Permissions**
3. **Role** can inherit from parent role
4. **Super roles** (like ADMIN) get all permissions automatically
5. JWT stores username + role
6. On each request, permissions are loaded from DB
7. @PreAuthorize checks permissions

### Data Flow Example: Creating a Sale
```
1. Client sends POST /api/v1/sales/create/sales_order
   ↓
2. JwtAuthFilter validates JWT, loads user permissions
   ↓
3. @PreAuthorize("hasAuthority('SALE_CREATE')") checks permission
   ↓
4. SalesController.salesForm() receives request
   ↓
5. SalesService.createSale() validates and processes
   ↓
6. SalesRepository.save() persists to database
   ↓
7. SalesMapper converts entity to DTO
   ↓
8. Controller returns ResponseEntity with 200 OK
```

---

## 🚀 Next Steps

1. **Read this guide thoroughly** - Don't rush, understand each concept
2. **Debug the application** - Set breakpoints and see the flow
3. **Modify existing features** - Change validations, add fields
4. **Create a new feature** - Follow the patterns you see
5. **Write tests** - Understand the code better by testing it
6. **Ask questions** - When something is unclear, research or ask

---

## 💡 Common Questions Answered

### Why use @RequiredArgsConstructor instead of @Autowired?
- Constructor injection is recommended over field injection
- Immutable dependencies (final fields)
- Easier to test (can create objects without Spring)
- Clear required dependencies

### Why DTOs instead of entities?
- Security: Don't expose internal structure
- Flexibility: API contract independent of database
- Performance: Send only necessary data
- Backward compatibility: Can change DB without breaking API

### Why Lazy vs Eager fetching?
- LAZY: Avoids N+1 query problem, loads on-demand
- EAGER: Loads immediately, can cause performance issues
- Rule: Use LAZY by default, fetch explicitly when needed

### Why @Transactional?
- Ensures data consistency
- Automatic rollback on exceptions
- Manages database connections
- Required for lazy loading outside of persistence context

### What's the difference between @RestController and @Controller?
- @Controller returns views (HTML)
- @RestController = @Controller + @ResponseBody
- @RestController returns data (JSON/XML) directly

---

## 📌 Important Notes

1. **Never commit secrets** - Use environment variables for passwords/keys
2. **Use HTTPS in production** - Protect JWT tokens in transit
3. **Validate all inputs** - Use @Valid and custom validators
4. **Handle exceptions properly** - Use @RestControllerAdvice
5. **Write tests** - Ensure code quality and prevent regressions
6. **Use pagination** - Avoid loading large datasets
7. **Index database columns** - Used in WHERE, ORDER BY clauses
8. **Use @Transactional carefully** - Can impact performance
9. **Monitor your application** - Use Actuator endpoints
10. **Follow REST conventions** - Consistent API design

---

**Good luck with your learning journey! 🎉**

_This document is based on your ChemOS codebase. Refer to it whenever you need to understand a pattern or concept._
