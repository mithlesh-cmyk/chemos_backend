# 📚 Spring Boot Learning Materials - ChemOS Project

Welcome! This repository contains comprehensive learning materials based on your **ChemOS** Spring Boot application. These documents are designed for a 3-year experienced developer who wants to deeply understand Spring Boot concepts and patterns.

---

## 📖 Documentation Overview

### 1. **[LEARNING_GUIDE.md](./LEARNING_GUIDE.md)** - Main Reference Guide
**📘 Comprehensive conceptual guide covering all core concepts**

- Spring Boot architecture & structure
- All annotations explained with examples
- REST API design patterns
- JWT authentication flow (detailed)
- Database & JPA concepts
- Spring Data repositories
- DTOs & Mappers pattern
- Service layer & transactions
- Exception handling
- Lombok usage
- Configuration & properties
- Security architecture
- Modern Java features (Records, Text Blocks, Streams)
- Best practices & patterns

**When to read**: Start here for theory and concepts. Read sections as you need them.

---

### 2. **[QUICK_REFERENCE.md](./QUICK_REFERENCE.md)** - Cheat Sheet
**⚡ Quick lookup guide for syntax and patterns**

- Annotation quick reference
- HTTP status codes
- Common code patterns (Controller, Service, Repository, Entity)
- Security patterns
- Database query methods
- Lombok patterns
- Testing patterns
- Performance tips
- Maven commands
- Common mistakes to avoid

**When to use**: Keep this open while coding for quick lookups.

---

### 3. **[ADVANCED_CONCEPTS.md](./ADVANCED_CONCEPTS.md)** - Deep Dive
**🎯 In-depth explanation of complex patterns in your codebase**

- Role-Based Access Control (RBAC) with permission hierarchy
- JWT authentication complete lifecycle
- PostgreSQL advanced text search (similarity, fuzzy matching)
- Builder pattern with Lombok
- DTO pattern with Java Records
- Transaction behavior & propagation
- Pagination & performance
- Lazy loading & N+1 problem
- Method security with @PreAuthorize
- Exception flow & status codes

**When to read**: After understanding basics, dive deep into these advanced topics.

---

### 4. **[HANDS_ON_LEARNING.md](./HANDS_ON_LEARNING.md)** - Practical Exercises
**🛠️ 5-week structured learning path with exercises**

**Week 1**: Understanding existing code
- Run application, explore database
- Trace requests, test JWT
- Debug filters

**Week 2**: Data access & JPA
- Custom queries, pagination
- Entity relationships, lazy loading

**Week 3**: Security & authorization
- Permissions, row-level security
- Exception handling

**Week 4**: Advanced topics
- Transactions, concurrency
- Unit & integration testing

**Week 5**: Build complete feature
- Audit logging
- Full inventory module

**When to use**: Follow this week-by-week to build hands-on skills.

---

## 🗺️ Learning Roadmap

### Phase 1: Foundation (Week 1-2) 
**Goal**: Understand what exists and how it works

1. Read **LEARNING_GUIDE.md** sections:
   - Spring Boot Annotations
   - REST API Design
   - Database & JPA
   
2. Follow **HANDS_ON_LEARNING.md** Week 1-2:
   - Run application
   - Test JWT authentication
   - Explore database
   - Create simple endpoints

3. Use **QUICK_REFERENCE.md** for lookups

---

### Phase 2: Security & Data (Week 3-4)
**Goal**: Master authentication, authorization, and data access

1. Read **LEARNING_GUIDE.md** sections:
   - Security Architecture (JWT)
   - Authorization with @PreAuthorize
   
2. Read **ADVANCED_CONCEPTS.md** sections:
   - RBAC with permission hierarchy
   - JWT authentication flow
   - Pagination & N+1 problem
   
3. Follow **HANDS_ON_LEARNING.md** Week 3:
   - Test permissions
   - Implement row-level security
   - Add custom exceptions

---

### Phase 3: Advanced Patterns (Week 5-6)
**Goal**: Understand complex patterns and best practices

1. Read **ADVANCED_CONCEPTS.md**:
   - All sections in detail
   
2. Follow **HANDS_ON_LEARNING.md** Week 4:
   - Test transactions
   - Write unit tests
   - Optimize queries

---

### Phase 4: Build & Master (Week 7-10)
**Goal**: Build complete features independently

1. Follow **HANDS_ON_LEARNING.md** Week 5:
   - Build audit logging
   - Create inventory module
   
2. Complete additional challenges

---

## 🎯 Learning by Task Type

### "I want to understand..."

| Topic | Read This |
|-------|-----------|
| **Annotations** | LEARNING_GUIDE.md → Section 1 |
| **JWT Authentication** | ADVANCED_CONCEPTS.md → Section 2 |
| **RBAC & Permissions** | ADVANCED_CONCEPTS.md → Section 1 |
| **Database Queries** | LEARNING_GUIDE.md → Section 5 |
| **Transactions** | ADVANCED_CONCEPTS.md → Section 6 |
| **Exception Handling** | LEARNING_GUIDE.md → Section 9 |
| **Testing** | QUICK_REFERENCE.md → Testing Patterns |

### "I want to build..."

| Feature | Follow This |
|---------|-------------|
| **New REST endpoint** | HANDS_ON_LEARNING.md → Exercise 6 |
| **Custom query** | HANDS_ON_LEARNING.md → Exercise 10 |
| **New entity & relationships** | HANDS_ON_LEARNING.md → Exercise 12 |
| **Permission-protected feature** | HANDS_ON_LEARNING.md → Exercise 14 |
| **Complete module** | HANDS_ON_LEARNING.md → Exercise 24 |

### "I'm stuck on..."

| Issue | Check This |
|-------|-----------|
| **Annotation not working** | QUICK_REFERENCE.md → Annotations |
| **Query returning wrong data** | ADVANCED_CONCEPTS.md → Section 8 (N+1) |
| **403 Forbidden error** | ADVANCED_CONCEPTS.md → Section 1 (RBAC) |
| **Transaction not rolling back** | ADVANCED_CONCEPTS.md → Section 6 |
| **N+1 query problem** | ADVANCED_CONCEPTS.md → Section 8 |

---

## 📋 Quick Start Guide

### Step 1: First Day
1. Read this README completely
2. Read LEARNING_GUIDE.md → Overview & Architecture
3. Run the application following HANDS_ON_LEARNING.md → Exercise 1
4. Explore the database following Exercise 2

### Step 2: First Week
1. Follow HANDS_ON_LEARNING.md Week 1 exercises
2. Keep QUICK_REFERENCE.md open for lookups
3. Read relevant sections of LEARNING_GUIDE.md as needed

### Step 3: Going Forward
- Follow the 5-week plan in HANDS_ON_LEARNING.md
- Deep dive into ADVANCED_CONCEPTS.md topics
- Use QUICK_REFERENCE.md for daily coding

---

## 🔑 Key Concepts You Must Master

Based on your codebase, prioritize these topics:

### High Priority (Master First)
1. ✅ **Spring Boot Annotations** (@RestController, @Service, @Autowired)
2. ✅ **REST API Patterns** (Controllers, DTOs, ResponseEntity)
3. ✅ **JPA Entities** (@Entity, @ManyToOne, relationships)
4. ✅ **Spring Data Repositories** (JpaRepository, query methods)
5. ✅ **JWT Authentication** (Token generation, validation, filters)
6. ✅ **@PreAuthorize** (Permission-based security)

### Medium Priority (Master Next)
7. ⏳ **Transactions** (@Transactional, rollback behavior)
8. ⏳ **Exception Handling** (@RestControllerAdvice, custom exceptions)
9. ⏳ **Pagination** (Page, Pageable)
10. ⏳ **Lombok** (@Builder, @Data, @RequiredArgsConstructor)
11. ⏳ **DTOs & Mappers** (Records, entity-DTO conversion)

### Advanced (Master Later)
12. ⏳ **RBAC Architecture** (Role hierarchy, permission inheritance)
13. ⏳ **Lazy Loading** (N+1 problem, JOIN FETCH)
14. ⏳ **Query Optimization** (Indexes, EXPLAIN ANALYZE)
15. ⏳ **Testing** (Unit tests, Integration tests)

---

## 🛠️ Your Codebase Structure

```
ChemOS Application
│
├── Authentication & Security
│   ├── JWT token generation & validation
│   ├── User/Role/Permission management
│   ├── Permission inheritance & restrictions
│   └── Method-level security (@PreAuthorize)
│
├── Business Domain
│   ├── Companies (with fuzzy search)
│   ├── Sales (with pagination & filters)
│   ├── Purchases
│   ├── Products
│   ├── Ports & Countries
│   └── Vessel Stock Stats
│
├── Supporting Features
│   ├── Audit logging
│   ├── Market status tracking
│   ├── Sale-Purchase linking
│   └── CSV imports/exports
│
└── Technical Stack
    ├── Spring Boot 4.0.6
    ├── Java 21
    ├── PostgreSQL (with pg_trgm for fuzzy search)
    ├── JWT (jjwt 0.11.5)
    └── Lombok (code generation)
```

---

## 🎓 Certification Path (Self-Study)

Track your progress:

### Beginner (Weeks 1-2)
- [ ] Can run and debug the application
- [ ] Understand basic annotations
- [ ] Can create simple REST endpoints
- [ ] Understand JWT flow
- [ ] Can write basic queries

### Intermediate (Weeks 3-4)
- [ ] Can implement security features
- [ ] Understand transaction behavior
- [ ] Can write complex queries
- [ ] Can handle exceptions properly
- [ ] Can add pagination

### Advanced (Weeks 5-8)
- [ ] Can build complete features
- [ ] Understand RBAC architecture
- [ ] Can optimize queries
- [ ] Can write tests
- [ ] Can debug complex issues

### Expert (Weeks 9-12)
- [ ] Can architect new modules
- [ ] Can refactor legacy code
- [ ] Can review pull requests
- [ ] Can mentor juniors
- [ ] Understand all patterns in codebase

---

## 💡 Tips for Effective Learning

### Do ✅
1. **Debug, don't guess** - Set breakpoints and trace execution
2. **Make it fail** - Break things intentionally to understand behavior
3. **Type, don't copy** - Muscle memory matters
4. **Read logs** - They tell you what's happening
5. **Experiment** - Try different approaches
6. **Ask questions** - Research when stuck

### Don't ❌
1. **Rush through** - Take time to understand
2. **Skip exercises** - Hands-on practice is crucial
3. **Memorize** - Focus on understanding concepts
4. **Give up** - Debugging is learning
5. **Work in isolation** - Ask for help when needed

---

## 🔍 How to Use These Docs

### For Daily Development
1. Keep **QUICK_REFERENCE.md** open
2. Look up patterns as needed
3. Copy-paste and adapt examples

### For Learning New Concepts
1. Read relevant section in **LEARNING_GUIDE.md**
2. Read deeper in **ADVANCED_CONCEPTS.md**
3. Practice with **HANDS_ON_LEARNING.md**

### For Problem Solving
1. Search all docs (Ctrl+F)
2. Check **ADVANCED_CONCEPTS.md** for complex issues
3. Review **QUICK_REFERENCE.md** for syntax

### For Building Features
1. Find similar example in **QUICK_REFERENCE.md**
2. Follow pattern from **HANDS_ON_LEARNING.md**
3. Apply best practices from **LEARNING_GUIDE.md**

---

## 📚 Additional Resources

### Official Documentation
- [Spring Boot Docs](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security Docs](https://docs.spring.io/spring-security/reference/)
- [Spring Data JPA Docs](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)

### Recommended Reading
- "Spring in Action" by Craig Walls
- "Spring Security in Action" by Laurentiu Spilca
- Baeldung.com tutorials

### Tools
- **IntelliJ IDEA** - Best IDE for Spring Boot
- **Postman** - API testing
- **DBeaver** - Database management
- **Git** - Version control

---

## ❓ FAQ

**Q: Where should I start?**
A: Start with LEARNING_GUIDE.md → Overview, then follow HANDS_ON_LEARNING.md Week 1.

**Q: I don't understand JWT authentication**
A: Read ADVANCED_CONCEPTS.md → Section 2 (JWT) with detailed flow diagram.

**Q: How do I create a new REST endpoint?**
A: Follow HANDS_ON_LEARNING.md → Exercise 6.

**Q: What's the N+1 problem?**
A: Read ADVANCED_CONCEPTS.md → Section 8 (Lazy Loading).

**Q: How do permissions work in this app?**
A: Read ADVANCED_CONCEPTS.md → Section 1 (RBAC).

**Q: I want to add a new entity**
A: Follow HANDS_ON_LEARNING.md → Exercise 12.

**Q: How do I test my code?**
A: Follow HANDS_ON_LEARNING.md → Exercises 19-20.

---

## 📞 Getting Help

When stuck:

1. **Search these docs** - Use Ctrl+F to find topics
2. **Check logs** - Enable DEBUG logging
3. **Google the error** - Include "Spring Boot" in search
4. **Read Stack Overflow** - Usually has answers
5. **Check Spring docs** - Official documentation
6. **Ask colleagues** - Share what you've tried

---

## ✨ Your Journey Starts Here

You have everything you need to master Spring Boot:
- ✅ Comprehensive theory (LEARNING_GUIDE.md)
- ✅ Quick reference (QUICK_REFERENCE.md)
- ✅ Deep dives (ADVANCED_CONCEPTS.md)
- ✅ Practical exercises (HANDS_ON_LEARNING.md)

**Start with Week 1 of HANDS_ON_LEARNING.md and work your way through!**

---

## 📈 Track Your Progress

| Week | Topic | Status |
|------|-------|--------|
| 1 | Project Setup & JWT | ⏳ |
| 2 | Data Access & JPA | ⏳ |
| 3 | Security & Authorization | ⏳ |
| 4 | Advanced Topics | ⏳ |
| 5 | Build Complete Feature | ⏳ |

---

**Good luck! You've got this! 🚀**

*Remember: The best developers are not those who know everything, but those who know how to learn and find answers.*
