
# System Design Document: Finance Data Processing & Access Control Backend

---

## 1. Requirements Engineering

### 1.1 Functional Requirements (FRs)

**FR-01: Identity & Access Management (IAM)**
*   **FR-01.1:** The system shall support role-based access control (RBAC) with three distinct roles: `VIEWER`, `ANALYST`, `ADMIN`.
*   **FR-01.2:** The system shall authenticate users via stateless JWT Access Tokens (15-min TTL) and stateful Refresh Tokens (7-day TTL) stored securely in the database.
*   **FR-01.3:** The system must support immediate session revocation (blacklisting) upon user deactivation or logout.
*   **FR-01.4:** The system must protect against brute-force attacks via account lockout mechanisms (5 failed attempts).

**FR-02: Financial Record Management**
*   **FR-02.1:** Authorized users (ADMIN) shall perform CRUD operations on financial records.
*   **FR-02.2:** The system must ensure data integrity: amounts must be positive, dates valid, and record types must match their assigned category types.
*   **FR-02.3:** The system must preserve history; record deletions shall be "soft deletes" (marking as archived), not physical removal.
*   **FR-02.4:** The system must handle concurrent modifications via Optimistic Locking to prevent lost updates.

**FR-03: Analytics & Reporting**
*   **FR-03.1:** The system shall provide aggregated insights (Total Income/Expense, Net Balance) calculated dynamically from the database.
*   **FR-03.2:** The system shall support time-series aggregation (Daily, Weekly, Monthly trends).
*   **FR-03.3:** Analytics queries must exclude soft-deleted records automatically.

**FR-04: Auditability**
*   **FR-04.1:** The system shall log every sensitive mutation (Create, Update, Delete, Role/Status Change) into an immutable audit trail.
*   **FR-04.2:** Audit logs must capture the "Who" (user ID), "When" (timestamp), "What" (entity change), and "Where" (IP address).

---

### 1.2 Non-Functional Requirements (NFRs)

**NFR-01: Reliability & Data Integrity (Critical)**
*   **NFR-01.1:** The system must guarantee ACID compliance for all financial transactions.
*   **NFR-01.2:** Currency precision must be maintained using fixed-point arithmetic (`DECIMAL(15,2)`). Floating-point arithmetic is strictly forbidden for financial data.
*   **NFR-01.3:** The database schema must enforce constraints (Foreign Keys, Checks) as a defense layer against application-layer bugs.

**NFR-02: Security**
*   **NFR-02.1:** All endpoints (except login/refresh) require authentication.
*   **NFR-02.2:** The system must prevent IDOR (Insecure Direct Object References) by binding record creation to the authenticated user's context.
*   **NFR-02.3:** Passwords must be hashed using BCrypt (cost factor 12). Refresh tokens must be hashed using SHA-256 for indexable lookups.
*   **NFR-02.4:** The system must implement Rate Limiting to protect against DoS and brute-force attacks.

**NFR-03: Performance**
*   **NFR-03.1:** API response times for CRUD operations must be < 200ms (p95).
*   **NFR-03.2:** Analytics aggregation queries must execute via database-level aggregation (`GROUP BY`), not in-memory processing, to ensure scalability.
*   **NFR-03.3:** Database indexes must cover all frequent query patterns (filtering by date, user, category).

**NFR-04: Maintainability**
*   **NFR-04.1:** The architecture must be a **Modular Monolith** with strict package boundaries to allow potential future extraction into microservices.
*   **NFR-04.2:** No circular dependencies between business modules.

---
