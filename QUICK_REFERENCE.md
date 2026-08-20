# Spring Boot Quick Reference Cheat Sheet

## 🔖 Annotations Quick Reference

### Class-Level Annotations
```java
@SpringBootApplication          // Main application class
@RestController                 // REST API controller
@Service                        // Business logic service
@Repository                     // Data access layer
@Component                      // Generic Spring bean
@Configuration                  // Configuration class
@RequiredArgsConstructor        // Lombok: constructor injection
@Data                          // Lombok: getters, setters, toString, equals, hashCode
@Builder                       // Lombok: builder pattern
@Entity                        // JPA entity (database table)
@Table(name = "table_name")    // Custom table name
```

### Method-Level Annotations
```java
@GetMapping("/path")           // HTTP GET endpoint
@PostMapping("/path")          // HTTP POST endpoint
@PutMapping("/path")           // HTTP PUT endpoint
@PatchMapping("/path")         // HTTP PATCH endpoint
@DeleteMapping("/path")        // HTTP DELETE endpoint
@RequestMapping("/base")       // Base path for controller
@Transactional                 // Database transaction
@PreAuthorize("hasAuthority('PERM')") // Authorization check
@Bean                          // Declare Spring bean
@Scheduled(cron = "0 0 * * * *")      // Scheduled task
```

### Parameter Annotations
```java
@RequestBody                   // HTTP request body → Java object
@RequestParam                  // Query parameter (?key=value)
@PathVariable                  // URL path variable (/users/{id})
@Valid                         // Trigger validation
@PageableDefault              // Pagination defaults
```

### Field-Level Annotations
```java
// JPA
@Id                           // Primary key
@GeneratedValue(strategy = GenerationType.UUID)
@Column(name = "column_name")
@ManyToOne(fetch = FetchType.LAZY)
@OneToMany(mappedBy = "field")
@JoinColumn(name = "fk_column")
@CreationTimestamp            // Auto-set creation time
@UpdateTimestamp              // Auto-set update time

// Validation
@NotNull                      // Cannot be null
@NotBlank                     // Cannot be null/empty/whitespace
@Size(min = 3, max = 50)     // String/collection size
@Email                        // Email format validation
@Pattern(regexp = "...")      // Regex validation

// Configuration
@Value("${property.name}")    // Inject from properties file
```

---

## 🌐 HTTP Status Codes
```
200 OK                  - Successful GET, PUT, PATCH
201 CREATED            - Successful POST (resource created)
204 NO CONTENT         - Successful DELETE
400 BAD REQUEST        - Invalid input
401 UNAUTHORIZED       - Not authenticated
403 FORBIDDEN          - Not authorized (insufficient permissions)
404 NOT FOUND          - Resource not found
409 CONFLICT           - Resource already exists
500 INTERNAL ERROR     - Server error
```

---

## 📝 Common Code Patterns

### Controller Pattern
```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/resource")
public class ResourceController {
    
    private final ResourceService resourceService;
    
    @GetMapping
    public ResponseEntity<List<ResourceDto>> getAll() {
        return ResponseEntity.ok(resourceService.findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ResourceDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(resourceService.findById(id));
    }
    
    @PostMapping
    public ResponseEntity<ResourceDto> create(@Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(resourceService.create(request));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ResourceDto> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateRequest request) {
        return ResponseEntity.ok(resourceService.update(id, request));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        resourceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### Service Pattern
```java
@Service
@RequiredArgsConstructor
public class ResourceService {
    
    private final ResourceRepository resourceRepository;
    private final ResourceMapper resourceMapper;
    
    @Transactional(readOnly = true)
    public List<ResourceDto> findAll() {
        return resourceRepository.findAll()
            .stream()
            .map(resourceMapper::toDto)
            .toList();
    }
    
    @Transactional
    public ResourceDto create(CreateRequest request) {
        Resource resource = resourceMapper.toEntity(request);
        Resource saved = resourceRepository.save(resource);
        return resourceMapper.toDto(saved);
    }
    
    @Transactional
    public ResourceDto update(String id, UpdateRequest request) {
        Resource resource = resourceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Resource not found"));
        
        // Update fields
        resource.setName(request.name());
        resource.setStatus(request.status());
        
        return resourceMapper.toDto(resource);
    }
}
```

### Repository Pattern
```java
public interface ResourceRepository extends JpaRepository<Resource, String> {
    
    // Query method - derived from method name
    List<Resource> findByStatus(String status);
    List<Resource> findByNameContainingIgnoreCase(String name);
    Optional<Resource> findByEmail(String email);
    boolean existsByEmail(String email);
    
    // Custom JPQL query
    @Query("SELECT r FROM Resource r WHERE r.status = :status AND r.createdAt >= :date")
    List<Resource> findRecentByStatus(@Param("status") String status, 
                                      @Param("date") LocalDateTime date);
    
    // Native SQL query
    @Query(value = "SELECT * FROM resources WHERE status = ?1", nativeQuery = true)
    List<Resource> findByStatusNative(String status);
}
```

### Entity Pattern
```java
@Entity
@Table(name = "resources")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Resource {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(unique = true)
    private String email;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### DTO Pattern (Record)
```java
// Request DTOs
public record CreateResourceRequest(
    @NotBlank String name,
    @Email String email,
    String categoryId
) {}

public record UpdateResourceRequest(
    @NotBlank String name,
    String status
) {}

// Response DTOs
public record ResourceResponse(
    String id,
    String name,
    String email,
    String categoryName,
    LocalDateTime createdAt
) {}

// Generic API responses
public record ApiResponse<T>(
    boolean success,
    String message,
    T data
) {}

public record ApiError(
    String error,
    String message,
    LocalDateTime timestamp
) {}
```

### Mapper Pattern
```java
@Component
public class ResourceMapper {
    
    public ResourceResponse toDto(Resource resource) {
        return new ResourceResponse(
            resource.getId(),
            resource.getName(),
            resource.getEmail(),
            resource.getCategory() != null ? resource.getCategory().getName() : null,
            resource.getCreatedAt()
        );
    }
    
    public Resource toEntity(CreateResourceRequest request) {
        return Resource.builder()
            .name(request.name())
            .email(request.email())
            .build();
    }
}
```

### Exception Handling
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
        ApiError error = new ApiError(
            "NOT_FOUND",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex) {
        ApiError error = new ApiError(
            "BAD_REQUEST",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        ApiError error = new ApiError(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

// Custom exceptions
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
```

---

## 🔐 Security Patterns

### JWT Configuration
```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### Authorization in Controllers
```java
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    
    @PreAuthorize("hasAuthority('USER_MANAGEMENT')")
    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        // Only users with USER_MANAGEMENT permission can access
        return ResponseEntity.ok(userService.createUser(request));
    }
    
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @GetMapping("/reports")
    public ResponseEntity<Report> getReports() {
        // Users with ADMIN OR MANAGER can access
        return ResponseEntity.ok(reportService.getReports());
    }
}
```

---

## 📊 Database Query Methods

### Derived Query Methods
```java
// Method name conventions
findBy...           // SELECT
countBy...          // COUNT
deleteBy...         // DELETE
existsBy...         // EXISTS check

// Keywords
And, Or, Between, LessThan, GreaterThan, After, Before
Like, StartingWith, EndingWith, Containing
OrderBy, IgnoreCase, Top, First

// Examples
findByName(String name)
findByNameAndEmail(String name, String email)
findByAgeBetween(int start, int end)
findByNameContainingIgnoreCase(String name)
findByCreatedAtAfter(LocalDateTime date)
findTop10ByOrderByCreatedAtDesc()
countByStatus(String status)
existsByEmail(String email)
```

### Pagination & Sorting
```java
// Repository
Page<Resource> findByStatus(String status, Pageable pageable);

// Service
public Page<ResourceDto> findByStatus(String status, Pageable pageable) {
    return resourceRepository.findByStatus(status, pageable)
        .map(resourceMapper::toDto);
}

// Controller
@GetMapping
public ResponseEntity<Page<ResourceDto>> getAll(
        @RequestParam(required = false) String status,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) 
        Pageable pageable) {
    return ResponseEntity.ok(resourceService.findByStatus(status, pageable));
}

// Request: /api/resources?status=ACTIVE&page=0&size=10&sort=name,asc
```

---

## 🛠️ Common Lombok Patterns

```java
// Entity with Lombok
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString(exclude = {"lazyField"})
@EqualsAndHashCode(of = "id")
public class MyEntity {
    @Id
    private String id;
    private String name;
}

// Service with Lombok
@Service
@RequiredArgsConstructor
@Slf4j
public class MyService {
    private final MyRepository repository;
    
    public void doSomething() {
        log.info("Processing...");
        log.error("Error occurred", exception);
    }
}
```

---

## 🧪 Testing Patterns

### Unit Test
```java
@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {
    
    @Mock
    private ResourceRepository resourceRepository;
    
    @Mock
    private ResourceMapper resourceMapper;
    
    @InjectMocks
    private ResourceService resourceService;
    
    @Test
    void shouldCreateResource() {
        // Given
        CreateResourceRequest request = new CreateResourceRequest("Test", "test@example.com", null);
        Resource resource = Resource.builder().name("Test").build();
        when(resourceMapper.toEntity(request)).thenReturn(resource);
        when(resourceRepository.save(resource)).thenReturn(resource);
        
        // When
        resourceService.create(request);
        
        // Then
        verify(resourceRepository).save(resource);
    }
}
```

### Integration Test
```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ResourceControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void shouldCreateResource() throws Exception {
        CreateResourceRequest request = new CreateResourceRequest("Test", "test@example.com", null);
        
        mockMvc.perform(post("/api/v1/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test"));
    }
}
```

---

## ⚡ Performance Tips

1. **Use LAZY fetching** for @ManyToOne, @OneToMany
2. **Add database indexes** on columns used in WHERE, ORDER BY
3. **Use pagination** for large datasets
4. **Use @Transactional(readOnly = true)** for read operations
5. **Avoid N+1 queries** - use JOIN FETCH
6. **Use projections** instead of full entities when possible
7. **Cache frequently accessed data** with @Cacheable
8. **Use connection pooling** (HikariCP - default in Spring Boot)

---

## 🔍 Debugging Tips

```java
// Enable SQL logging
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.use_sql_comments=true

// Enable debug logging
logging.level.org.springframework.security=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

// Use Lombok @Slf4j
@Slf4j
@Service
public class MyService {
    public void process() {
        log.debug("Processing started");
        log.info("Processing item: {}", item);
        log.error("Error occurred", exception);
    }
}
```

---

## 📦 Maven Commands

```bash
mvn clean install          # Build project
mvn spring-boot:run        # Run application
mvn test                   # Run tests
mvn clean package          # Create JAR file
mvn dependency:tree        # Show dependencies
```

---

## 🚀 Application Properties Common Settings

```properties
# Server
server.port=8080
server.servlet.context-path=/api

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
spring.datasource.username=user
spring.datasource.password=pass

# JPA
spring.jpa.hibernate.ddl-auto=update  # create, update, validate, none
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Jackson
spring.jackson.time-zone=UTC
spring.jackson.date-format=yyyy-MM-dd HH:mm:ss

# Logging
logging.level.root=INFO
logging.level.com.myapp=DEBUG

# Actuator
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always

# File Upload
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

---

## 🎯 Common Mistakes to Avoid

1. ❌ Using @Data on JPA entities (circular toString)
2. ❌ Forgetting @Transactional on service methods
3. ❌ Using EAGER fetching everywhere
4. ❌ Not validating input (@Valid)
5. ❌ Exposing entities in controllers (use DTOs)
6. ❌ Hardcoding credentials (use environment variables)
7. ❌ Not handling exceptions properly
8. ❌ Not using pagination for large datasets
9. ❌ Not adding database indexes
10. ❌ Using @Autowired field injection (use constructor)

---

**📖 For detailed explanations, see LEARNING_GUIDE.md**
