# Software Requirements Specification (SRS)
## Team Subscription & Department Budget Tracker

---

## 1. Introduction

### 1.1 Purpose
The purpose of this document is to define the requirements for the **Team Subscription & Department Budget Tracker**, a system for central management of corporate SaaS subscriptions, license seats, and monthly department budgets within an IT company. It is designed to prevent uncontrolled license purchases and monthly budget overspending across teams, streamline software access requests for employees and department managers, and provide FinOps administrators with automated cost conversion from foreign currencies (`EUR`, `USD`) into the company's base currency (`CZK`).

### 1.2 Document Conventions
Requirements in this document are uniquely numbered (`REQ-1`, `REQ-2`, …) and tagged with a priority (`High`, `Medium`, `Low`) as well as an implementation milestone tag (`[CP1]` for Checkpoint 1 core, `[CP2]` for the complete Checkpoint 2 application) to clearly distinguish the implemented scope.

Common abbreviations used throughout this document:

| Abbreviation | Meaning |
| :--- | :--- |
| **API** | Application Programming Interface |
| **CP1 / CP2** | Checkpoint 1 / Checkpoint 2 (course milestones) |
| **CZK / EUR / USD** | Czech Koruna / Euro / United States Dollar |
| **DB** | Database |
| **FinOps** | Financial Operations |
| **REST** | Representational State Transfer |
| **SaaS** | Software as a Service |
| **SRS** | Software Requirements Specification |
| **UML** | Unified Modeling Language |
| **ČNB** | Czech National Bank (*Česká národní banka*) |

### 1.3 Intended Audience and Reading Suggestions
This document is intended for the developer building and testing the system, the course instructors reviewing the architecture and scope for Checkpoint 1 and Checkpoint 2, and business stakeholders (FinOps administrators, department managers, and employees).

* **Reviewers and stakeholders** evaluating the project scope and user roles can focus on **Sections 1 and 2**.
* **Developers** implementing the persistence layer and business logic should read **Section 3** (UML domain model and functional requirements) and **Section 4** (external interfaces and non-functional requirements).

### 1.4 Product Scope
The **Team Subscription & Department Budget Tracker** provides centralized tracking and governance of paid software tools and cloud services (such as JetBrains, GitHub, Figma, and AWS). Specifically, the system:

* Maintains a global catalog of SaaS subscriptions with per-seat pricing, billing currency (`CZK`, `EUR`, `USD`), and maximum purchased seat capacity.
* Supports a role-based access request workflow where **Employees** request or return license seats, **Department Managers** approve or reject requests within their team's monthly budget, and **FinOps Administrators** configure subscriptions, seat limits, department budgets, and employee department transfers.
* Executes license assignment, revocation, subscription cancellation, and employee department transfers **transactionally**, guaranteeing seat-limit consistency and accurate `CZK` budget accounting.
* Integrates with an external REST exchange-rate service (e.g., ČNB or Frankfurter / ECB API) to convert foreign-currency seat costs to `CZK`.
* Provides multi-criteria filtering and expenditure auditing across departments, currencies, request statuses, price ranges, and available seat capacity.

**Scope boundaries (what the system will NOT do):** The system serves purely as an internal governance and cost-tracking platform. It does **not** execute real financial payments or process invoices, and does **not** automatically provision or deactivate user accounts in external third-party SaaS tools (e.g., via vendor APIs or SCIM/SSO). In the final release (Checkpoint 2), functionality is exposed via a secured REST API and a graphical web interface, whereas Checkpoint 1 delivers the core domain model, persistence layer, and transactional business logic.

### 1.5 References
* ISO/IEC/IEEE 29148:2018, *Systems and software engineering — Life cycle processes — Requirements engineering*
* ČVUT FEL EAR Course Guidelines — *Checkpoint 1 & Checkpoint 2 Requirements*
* Czech National Bank (ČNB) Daily Exchange Rates / Frankfurter (ECB) REST API Documentation

---

## 2. Overall Description

### 2.1 Product Perspective
**TeamPass** (Team Subscription & Department Budget Tracker) is a standalone, greenfield web application designed for internal SaaS license and budget governance. It does not replace an existing legacy billing or identity system; instead, it centralizes tracking currently scattered across spreadsheets.

At the architectural level, the system consists of:
* A **Spring Boot backend** with a relational SQL database (accessed via JPA/Hibernate) encapsulating domain entities, DAO repositories, and transactional business logic.
* A **secured REST API** consumed by a **Web GUI frontend** (planned for Checkpoint 2).
* An **external REST integration** with a public currency exchange-rate provider (ČNB or Frankfurter / ECB API) to retrieve `EUR`/`CZK` and `USD`/`CZK` exchange rates.

### 2.2 Product Functions
The main capabilities of the system include:
* **Subscription Catalog & Seat Management:** Maintain global SaaS products with per-seat cost, billing currency (`CZK`, `EUR`, `USD`), and maximum purchased seat count.
* **License Request Workflow:** Employees request a subscription for themselves (limited to at most one active license or pending request per product; re-requesting is permitted after rejection or revocation). Department Managers inherit Employee permissions and may directly acquire an available license for themselves without an approval step, provided seat capacity and department budget allow it.
* **Transactional Request Approval & Rejection:** Department Managers approve or reject pending requests strictly within their own department. Upon approval, the system atomically checks seat availability, converts the per-seat cost into `CZK`, verifies and updates the department's monthly budget, and issues an active license assignment with the `CZK` price locked for accurate historical reporting.
* **License Release & Subscription Cancellation:** Active licenses can be released by the owning Employee or revoked by their Department Manager; Administrators can cancel an entire subscription globally. In all cases, occupied seats are freed and allocated `CZK` funds are returned to the affected department budgets.
* **Monthly Budget Tracking & Cost Revaluation:** Each department defines a default monthly budget limit (`CZK`) that automatically applies to new months without manual re-entry, while tracking per-month limits and expenditures (`limit` and `spent` per department and month). Current-month foreign-currency expenses can be revalued using updated exchange rates without modifying historical months.
* **Employee Transfers & Offboarding:** Administrators transfer employees between departments transactionally. If the target department lacks sufficient monthly budget for the employee's active licenses, the transfer fails strictly and rolls back. When an Administrator removes/deactivates an employee from a department (offboarding), all active licenses are automatically released (freeing seats and refunding the department budget) and any pending requests are cancelled.
* **Multi-Criteria Filtering & Audit:** Search and filter subscriptions, active licenses, and requests by department, currency, status, price range, and available seat capacity.

### 2.3 User Characteristics
Every user must authenticate and pass role-based authorization before performing any operation in TeamPass. The system defines three user roles:

| Role | Description & Responsibilities | Access Boundaries |
| :--- | :--- | :--- |
| **Employee** | Regular department member using company-provided SaaS tools. | Can submit license requests for themselves, cancel their own pending requests, release their own active licenses, and view their own licenses/requests. Cannot view or modify other users' data. |
| **Department Manager** | Team lead responsible for license allocation and monthly budget compliance in one department. | Inherits all `Employee` permissions (and can directly claim licenses for themselves subject to seat and budget checks). Can approve/reject pending requests and revoke active licenses **only within their own department**, and monitor their department's budget and seat usage. |
| **Administrator (FinOps)** | Central finance/IT administrator managing global resources and company structure. | Manages the global subscription catalog and seat limits, configures department monthly budgets, triggers current-month cost revaluation, transfers employees between departments, and offboards employees by removing them from departments. |

### 2.4 Constraints
* **Out of Scope (Business Boundaries):**
  * The system does **not** execute real financial payments, process credit cards, or generate/parse vendor invoices.
  * The system does **not** automatically provision, modify, or delete user accounts in external third-party SaaS platforms (e.g., JetBrains, GitHub, Figma, AWS).
* **Financial & Currency Constraints:**
  * The company's base accounting currency is always `CZK`; subscriptions may only be billed in `CZK`, `EUR`, or `USD`.
  * Budgets are tracked strictly as monthly `CZK` limits without VAT calculation, volume discounts, or complex multi-tier pricing plans.
* **Technical Constraints:**
  * Local application authentication and role-based authorization only (no corporate SSO / LDAP / external identity provider).
  * Delivered as a web application (REST API + Web GUI in CP2); no dedicated mobile client or offline mode.
  * Implemented in Java 25 with Spring Boot, JPA/Hibernate, and Maven.

### 2.5 Assumptions and Dependencies
* **External API Dependency:** Live exchange-rate conversion and cost revaluation depend on network availability and the uptime of the ČNB or Frankfurter REST API (in CP1, an injected `ExchangeRateService` stub/interface is used so the core logic remains decoupled and testable offline).
* **Organizational Assumptions:**
  * Each active `Employee` and `DepartmentManager` belongs to at most one department at any given time.
  * An employee requires at most one active license per SaaS subscription at a time.
  * Monthly department budgets follow calendar months (`YYYY-MM`) and automatically inherit the department's default monthly limit unless explicitly overridden by an Administrator.

### 2.6 Implementation Scope (Checkpoint 1 vs. Checkpoint 2)

| Milestone | Planned Scope |
| :--- | :--- |
| **Checkpoint 1 (CP1)** | Complete JPA domain model (entities, inheritance / `@MappedSuperclass`, `1:N` and `M:N` associations), DAO layer, core transactional business logic via Dependency Injection (request creation with duplicate checks & manager self-acquisition, transactional approval/rejection with seat-limit check, `CZK` conversion via `ExchangeRateService` stub, and monthly budget validation/update), and non-trivial unit/integration tests verifying business rules and rollbacks. |
| **Checkpoint 2 (CP2)** | Full CRUD & global subscription cancellation, license release/revocation, transactional employee transfer & offboarding, live ČNB/Frankfurter REST API integration and current-month cost revaluation, advanced multi-parameter filtering (`Criteria API` + at least two additional JPA techniques), Spring Security (authentication & role-based authorization), REST controllers + Postman test scenarios, and a Web GUI frontend. |

## 3. Domain Model & Functional Requirements
<!-- TODO: UML Class Diagram + Functional Requirements (REQ-1, REQ-2, ...) -->

## 4. External Interfaces & Non-Functional Requirements
<!-- TODO: External Exchange Rate API, Security, Transactions & Data Integrity -->
