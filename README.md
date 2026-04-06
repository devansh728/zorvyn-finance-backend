# Finance Data Processing & Access Control Backend

> A secure, scalable backend system for financial dashboard management with role-based access control, transaction tracking, and real-time analytics.

---

## 📋 Project Overview

This is a **production-ready RESTful API** built for managing financial records with enterprise-grade security and audit capabilities. The system supports multi-role access control (Viewer, Analyst, Admin), tracks every financial transaction with precision, and provides real-time analytics for business insights.

Built as a **Modular Monolith** to balance simplicity with maintainability, this architecture allows clean separation of concerns while avoiding the operational complexity of microservices.

**What makes this different?**
- Financial precision using `DECIMAL(15,2)` — never floating-point
- Optimistic locking prevents concurrent update conflicts
- Soft deletes preserve audit trails
- Token blacklisting enables immediate session revocation
- Database-level aggregations for analytics (no in-memory processing)

---

## 🛠 Tech Stack

| Component | Technology | Why? |
|-----------|------------|------|
| **Framework** | Spring Boot 3.4 | Industry-standard, rich security ecosystem |
| **Language** | Java 21 | LTS with modern features (records, sealed classes) |
| **Database** | PostgreSQL 16 | ACID compliance, JSONB support, partial indexes |
| **Migrations** | Flyway | Version-controlled schema evolution |
| **Security** | Spring Security + JWT | Stateless auth with role hierarchy |
| **Caching** | Caffeine | In-process cache, zero infrastructure |
| **Rate Limiting** | Bucket4j | Brute-force protection (30/min public, 200/min auth) |
| **API Docs** | SpringDoc OpenAPI 3 | Auto-generated Swagger UI |
| **Testing** | JUnit 5 + Testcontainers | Real PostgreSQL in tests (no H2 mocks) |
| **Validation** | Jakarta Bean Validation | Declarative request validation |

---

## 🏗 Architecture: Modular Monolith

```
┌─────────────────────────────────────────────┐
│           Single Spring Boot JAR            │
├─────────────────────────────────────────────┤
│  ┌────────┐  ┌────────┐  ┌────────┐        │
│  │  Auth  │  │  User  │  │Category│        │
│  └───┬────┘  └───┬────┘  └───┬────┘        │
│      │           │            │             │
│      └───────┬───┴────┬───────┘             │
│              │        │                     │
│         ┌────▼────┐  ┌▼──────┐             │
│         │ Record  │  │Analytic│             │
│         └────┬────┘  └────────┘             │
│              │                              │
│         ┌────▼────┐                         │
│         │  Audit  │ (Cross-cutting)         │
│         └─────────┘                         │
└─────────────────────────────────────────────┘
```

**Key Principles:**
- Each module has its own `controller → service → repository` stack
- Modules communicate via **public service interfaces**, never repositories
- Strict dependency rules prevent circular dependencies
- Future-proof: modules can be extracted to microservices if needed

---

## ⚡ Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose

### 1. Clone & Setup Environment
```bash
git clone https://github.com/devansh728/zorvyn-finance-backend.git
cd finance-backend
cp .env.example .env
or 
# Edit directly in application-dev.yml or application-prod.yml
# Edit .env with your JWT secret (generate with: openssl rand -hex 64)
```

### 2. Start Database
```bash
docker-compose up -d
# Wait for health check: docker-compose ps
```

### 3. Run Application
```bash
./gradlew bootRun -x test
```

**Application ready at:** `http://localhost:8080/api/v1`  
**Swagger UI:** `http://localhost:8080/api/v1/swagger-ui.html`

---

## 🔐 Default Credentials

**⚠️ For development only!**

```
Email:    admin@zorvyn.com
Password: Password1!
Role:     ADMIN
```

**First Steps:**
1. Login via `POST /api/v1/auth/login` to get JWT tokens
2. Click "Authorize" in Swagger UI and paste the `accessToken`
3. Explore protected endpoints

---

## 🌍 Environment Variables

Create a `.env` file (never commit this!) from `.env.example`:

```bash
# Database
DB_USERNAME=finance_user
DB_PASSWORD=your_secure_password

# JWT (Must be 512 bits for HS512)
JWT_SECRET=your_64_char_minimum_secret_here_use_openssl_rand_hex_64
JWT_ACCESS_TOKEN_EXPIRY_MS=900000    # 15 minutes
JWT_REFRESH_TOKEN_EXPIRY_MS=604800000 # 7 days

# CORS (Comma-separated origins)
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200

# Rate Limiting
RATE_LIMIT_PUBLIC_RPM=30   # Requests/min for login/register
RATE_LIMIT_AUTH_RPM=200    # Requests/min for authenticated users
```

---

## 🧪 Running Tests

### Unit and Integration Tests (Uses Testcontainers)
```bash
./gradlew clean test
```

**Why Testcontainers?**  
Tests run against a **real PostgreSQL container**, ensuring:
- Schema migrations work correctly
- SQL queries match production behavior
- No H2 compatibility issues

**Test Coverage:** 87%+ (JaCoCo report in `target/site/jacoco/`)

---

## 📁 Project Structure

```
src/main/java/com/financeapp/
├── FinanceBackendApplication.java       # Main entry point
├── common/                              # Shared utilities
│   ├── dto/                             # ApiResponse<T> envelope
│   ├── exception/                       # Custom exceptions + GlobalHandler
│   ├── util/                            # SecurityContextUtil
│   └── config/                          # JPA Auditing, OpenAPI
├── security/                            # Security layer
│   ├── jwt/                             # Token generation/validation
│   ├── config/                          # SecurityFilterChain, CORS, RBAC
│   └── ratelimit/                       # Bucket4j filters
├── auth/                                # Authentication
│   ├── controller/  AuthController      # Login, refresh, logout
│   ├── service/     AuthServiceImpl     # Business logic
│   └── entity/      RefreshToken        # DB-backed tokens
├── user/                                # User management
│   ├── controller/  UserController      # CRUD + role/status changes
│   ├── service/     UserServiceImpl     # Last-admin protection
│   └── entity/      User, UserRole
├── category/                            # Transaction categories
│   ├── controller/  CategoryController
│   ├── service/     CategoryServiceImpl # Delete guard (prevents orphans)
│   └── entity/      Category
├── record/                              # Financial records
│   ├── controller/  FinancialRecordController
│   ├── service/     FinancialRecordServiceImpl  # Optimistic locking
│   ├── specification/ FinancialRecordSpec       # Dynamic filters
│   └── entity/      FinancialRecord
├── analytics/                           # Dashboard aggregations
│   ├── controller/  AnalyticsController
│   └── service/     AnalyticsServiceImpl # Cached, read-only
└── audit/                               # Audit trail
    ├── controller/  AuditLogController
    ├── service/     AuditServiceImpl
    └── entity/      AuditLog
```

**Layer Boundaries:**
- `Controller`: HTTP mapping, validation, RBAC (`@PreAuthorize`)
- `Service`: Business logic, transactions, audit logging
- `Repository`: Database queries, aggregations
- `Entity`: JPA mappings, constraints

---

## ✨ Key Features

### 1. **Multi-Role Access Control (RBAC)**
- **VIEWER:** Read-only dashboard access
- **ANALYST:** View records + analytics
- **ADMIN:** Full CRUD + user management

Role hierarchy: `ADMIN > ANALYST > VIEWER`

### 2. **Financial Record Management**
- Create, update, soft-delete transactions
- Category-based classification (Income/Expense)
- **Optimistic locking** prevents concurrent update conflicts
- **Soft deletes** preserve audit history

### 3. **Real-Time Analytics**
- Total income/expense/net balance
- Category-wise breakdowns with percentages
- Time-series trends (daily/weekly/monthly)
- **Cached for 5 minutes** (auto-invalidated on mutations)

### 4. **Audit Trail**
- Every mutation logged with:
  - **Who** performed the action
  - **What** changed (old → new values in JSONB)
  - **When** it happened
  - **Where** (IP address)
- Immutable, append-only log

### 5. **Secure Authentication**
- **JWT access tokens** (15 min) with fingerprinting (UserAgent hash)
- **Refresh tokens** (7 days) stored in DB with SHA-256 hashing
- **Immediate revocation** via token blacklist
- **Account lockout** after 5 failed login attempts

---

## 🔒 Security Features

### Defense-in-Depth Layers

1. **Rate Limiting**
   - Public endpoints: 30 req/min per IP (brute-force protection)
   - Authenticated: 200 req/min per userId

2. **Token Fingerprinting**
   - JWT contains `SHA-256(UserAgent)`
   - Prevents cross-device token theft

3. **Token Blacklist**
   - In-memory ConcurrentHashMap with JTI
   - Enables immediate session termination

4. **Optimistic Locking**
   - `@Version` field on mutable entities
   - Prevents lost updates in concurrent scenarios

5. **Soft Deletes**
   - Financial records never physically deleted
   - Audit trail integrity preserved

6. **IDOR Prevention**
   - `created_by` always extracted from JWT
   - Never accepted from request body

7. **Security Headers**
   - `X-Content-Type-Options: nosniff`
   - `X-Frame-Options: DENY`
   - `Strict-Transport-Security`

---

## 📌 Assumptions

1. **Currency:** All amounts are in **USD** (single-currency system)
2. **Timezone:** All timestamps stored in **UTC** (TIMESTAMPTZ)
3. **User Scope:** Financial records are **organizational**, not per-user isolated
4. **Public Registration:** Disabled by design (admin-only user creation)
5. **Analytics Access:** All roles can view aggregated data (no per-user filtering)

---

## ⚖️ Tradeoffs

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Architecture** | Modular Monolith | Simpler than microservices, allows future extraction |
| **Pagination** | Offset-based | Simpler than cursor-based; sufficient for <1M rows |
| **Caching** | Caffeine (in-memory) | Zero infrastructure; upgrade to Redis if multi-instance |
| **Token Storage** | SHA-256 (not BCrypt) | BCrypt is non-deterministic, can't index for lookups |
| **Soft Delete** | Mandatory for records | Finance data must never be lost (audit/compliance) |
| **Analytics** | Database aggregations | Pushes compute to DB layer (optimized C code) |

---

## 🚀 Future Enhancements

- [ ] **Multi-currency support** (currency field + conversion rates)
- [ ] **Cursor-based pagination** (for tables >1M rows)
- [ ] **Redis caching** (for distributed deployments)
- [ ] **Async audit logging** (via event queue for high throughput)
- [ ] **CSV/PDF export** endpoints for reports
- [ ] **WebSocket subscriptions** for real-time dashboard updates
- [ ] **Read replicas** (route analytics queries to replica DB)
- [ ] **Grafana dashboards** (expose Prometheus metrics)

---

## 📚 API Documentation

**Interactive Swagger UI:** [http://localhost:8080/api/v1/swagger-ui.html](http://localhost:8080/api/v1/swagger-ui.html)

**Sample Workflow:**
```bash
# 1. Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@zorvyn.com","password":"Password1!"}'

# Response: { "accessToken": "eyJhbG...", "refreshToken": "abc123..." }

# 2. Create Record (replace <TOKEN> with accessToken)
curl -X POST http://localhost:8080/api/v1/records \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 1500.00,
    "type": "INCOME",
    "categoryId": "c1000000-0000-0000-0000-000000000001",
    "transactionDate": "2025-01-15T10:30:00Z",
    "notes": "Freelance project payment"
  }'

# 3. Get Analytics
curl http://localhost:8080/api/v1/analytics/summary \
  -H "Authorization: Bearer <TOKEN>"
```

---

## 📞 Summary

This design emphasizes Correctness over complexity. In the FinTech domain, an architecture that prevents lost updates, maintains precision, and audits every action is superior to one that merely scales. The Modular Monolith approach ensures the system remains maintainable and testable while delivering robust performance for the target scale.

---

**Built with precision. Secured by design. Ready for production.**