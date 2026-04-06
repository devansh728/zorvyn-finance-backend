## Data Architecture Strategy

### 1 Transaction Management
*   **Isolation Level:** `READ_COMMITTED` (Postgres default) is sufficient for this scope to prevent dirty reads.
*   **Propagation:**
    *   Business Mutations: `REQUIRED` (default).
    *   Audit Logging: `REQUIRES_NEW` (Critical! Ensures audit logs persist even if the parent business transaction rolls back).

### 2 Handling Financial Precision
*   **Rule:** Never use `Double` or `Float`.
*   **Implementation:** Java `BigDecimal` maps to Postgres `NUMERIC(15,2)`.
*   **Rounding:** All divisions (for percentages) use `RoundingMode.HALF_UP`.

### 3 Concurrency Control
*   **Scenario:** Two admins open the same record to edit. Admin A saves first.
*   **Without Control:** Admin B saves second, overwriting A's changes (Lost Update).
*   **Implementation:**
    *   1. Entity has `@Version int version`.
    *   2. Admin B sends `PATCH` with `version=0`.
    *   3. DB currently has `version=1` (updated by Admin A).
    *   4. Hibernate throws `OptimisticLockException`.
    *   5. Service catches exception and returns `409 Conflict`.
