## Low-Level Design (LLD) - OOP & Patterns

### 1 Domain Modeling (Entity Layer)

We apply **Rich Domain Model** principles where entities encapsulate state and behavior, rather than being mere data buckets.

**Key Patterns:**
1.  **Optimistic Offline Lock:** The `version` field on mutable entities (`User`, `FinancialRecord`) prevents concurrent update anomalies.
2.  **Soft Delete:** The `deleted_at` field allows "removal" while preserving referential integrity for audits.
3.  **Value Objects:** `Money` (Amount+Currency logic) and `TransactionType` are treated as immutable value types (Java Enums or records).

**Entity Responsibility:**
*   `User`: Encapsulates authentication state (locked, active) and role.
*   `FinancialRecord`: Enforces consistency (e.g., ensuring the amount is positive). *Note: Validation logic often sits in the service or via Bean Validation, but the Entity protects its invariants.*

### 2 Service Layer Design (Application Logic)

The Service Layer defines the **Application Boundary**. It orchestrates domain objects and infrastructure services.

**Design Patterns Applied:**

**A. Specification Pattern (Dynamic Filtering)**
*   **Problem:** `GET /records` accepts 5+ optional filter parameters.
*   **Anti-Pattern:** String concatenation in SQL queries ("WHERE 1=1...").
*   **Solution:** `FinancialRecordSpec` implements JPA `Specification`. It constructs type-safe predicates dynamically.
    *   *Benefit:* Compile-time safety, reusability, clean code.

**B. Strategy Pattern (Trend Intervals)**
*   **Problem:** Analytics trends need different SQL grouping (Daily, Weekly, Monthly).
*   **Solution:** `TrendCalculationStrategy` interface with concrete implementations `DailyTrend`, `MonthlyTrend`.
    *   *Benefit:* Open/Closed Principle. Adding a new interval (e.g., Quarterly) requires a new class, not modifying existing logic.

**C. Facade Pattern (Analytics Service)**
*   **Problem:** Analytics requires complex aggregation logic, but the Controller should remain thin.
*   **Solution:** `AnalyticsService` acts as a facade. It coordinates:
    1.  Fetching raw aggregation data from `FinancialRecordRepository`.
    2.  Calculating derived values (percentages) in memory.
    3.  Formatting response DTOs.
    *   *Benefit:* Decouples controller from repository complexity and calculation logic.

### 3 Security Layer Design

Security is implemented as a **Pipes and Filters** architecture within Spring Security.

**Filter Chain Order (Crucial):**
1.  **TraceIdFilter:** Generates correlation ID. Must run first to tag all subsequent logs.
2.  **RateLimitFilter:** Protects the application from overload *before* expensive auth verification occurs.
3.  **JwtAuthenticationFilter:** Parses token, verifies signature, sets `SecurityContext`.

**Token Blacklist Strategy (Performance Optimization):**
*   **Constraint:** Checking the DB for revoked tokens on every request is too slow (O(1) DB call per request).
*   **Solution:** In-memory `ConcurrentHashMap` (Bloom Filter alternative).
*   **Trade-off:** Slight memory usage (approx 100KB for 10k tokens) for massive latency reduction. The `@Scheduled` cleanup ensures memory doesn't leak.

---
