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
* **Developers** implementing the persistence layer and business logic should read **Section 3** (UML domain model and functional requirements), **Section 4** (external interfaces), and **Section 5** (non-functional requirements).

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
  * Each user (regardless of role) belongs to exactly one department at any given time (administrators are assigned to a dedicated department, e.g., FinOps).
  * An employee requires at most one active license per SaaS subscription at a time.
  * Monthly department budgets follow calendar months (`YYYY-MM`) and automatically inherit the department's default monthly limit unless explicitly overridden by an Administrator.

### 2.6 Apportioning of Requirements (Implementation Scope)

While the system is designed to cover the complete internal SaaS governance workflow, only the core functional subset required for the semester project will be implemented. Individual requirements in Section 3 are tagged `[CP1]` or `[CP2]` to indicate when each capability is delivered:

* **Checkpoint 1 — Core Domain & License Request Workflow (`[CP1]`):**
  * Persistence of the core domain model (users and roles, departments, monthly budgets, subscriptions, license requests, and active licenses).
  * Requesting a subscription (including duplicate-request prevention), direct license acquisition by a Department Manager, and transactional request approval/rejection within the manager's department—covering seat availability checks, `CZK` price conversion (initially with fixed/local rates), and monthly budget validation.
  * Automated tests verifying the core business rules and failure scenarios.

* **Checkpoint 2 — Complete Application (`[CP2]`):**
  * Full management of subscriptions, departments, and monthly budget overrides, including global subscription cancellation.
  * License release by employees, license revocation by managers, transactional employee transfers between departments, and employee offboarding.
  * Live exchange-rate integration (ČNB / Frankfurter API) and current-month foreign-currency cost revaluation.
  * Multi-criteria search and filtering, user authentication with role-based access control, the REST API, and the web user interface.

* **Conceptual Only (Not Implemented):**
  * E-mail/chat notifications for pending approvals or budget warnings and downloadable monthly expense reports (PDF/CSV) are considered part of a full enterprise product vision, but are excluded from implementation within this course.

## 3. Domain Model & Functional Requirements

### 3.1 Domain Model (UML Class Diagram)

The domain model represents SaaS products, users and their roles, license requests, assigned licenses, departments, and monthly budgets. Its main design decisions are:

| Design decision | Rationale |
|---|---|
| Users & roles | `User` is a single entity storing authentication data (`email`, `passwordHash`, `active`), personal details, department membership, and a `Role` enum (`EMPLOYEE`, `DEPARTMENT_MANAGER`, `ADMIN`) instead of an entity inheritance hierarchy. This simplifies role changes, avoids multi-table `JOIN` overhead, and integrates cleanly with Spring Security. `BaseEntity` (`@MappedSuperclass`) provides the shared identifier across domain entities. |
| Product categories | `Product` and `Category` have a many-to-many (M:N) relationship, allowing a product to belong to multiple categories and a category to contain multiple products. |
| Monthly budgets | `Department` has a one-to-many (1:N) relationship with `DepartmentMonthlyBudget`. Each budget records a limit and the amount spent for a specific month and year; budget accounting is performed in CZK. |
| Requests vs. assignments | `LicenseRequest` records an employee's request, its status, department, and reviewing manager. `License` separately records the actual assigned seat, its status, assignment/release timestamps, and `lockedPrice` in CZK. This preserves assignment history independently of the approval workflow. |
| Value objects and statuses | `Price` and `Email` are value objects; `Currency`, `LicenseRequestStatus`, and `LicenseStatus` represent controlled sets of values. |

The model also includes 1:N relationships between a department and its users, a user and their license requests, and a user and their assigned licenses. A product's available seat count is derived from its seat limit and active assignments rather than stored as a separate mutable value.

### 3.2 Functional Requirements

The checkpoint column indicates the planned implementation stage: **[CP1]** for the first checkpoint and **[CP2]** for functionality deferred to the second checkpoint.

| ID | Priority | Stage | Requirement |
|---|---|---|---|
| REQ-1 | High | [CP1] | An employee can submit a license request for a product, specifying an optional message. The request is initially pending and belongs to the employee's department. |
| REQ-2 | High | [CP1] | The system prevents an employee from submitting a duplicate pending request or requesting a product for which they already hold an active license. |
| REQ-3 | High | [CP1] | A department manager can acquire a license for themselves without a separate manager approval, subject to the same seat-capacity and department-budget checks. |
| REQ-4 | High | [CP1] | A manager can approve a pending request from their own department. Within one transaction, the system verifies seat availability and the applicable monthly budget, determines the price in CZK, creates the assigned license, records its locked CZK price, and updates budget spending. If any check fails, no partial changes are committed. |
| REQ-5 | High | [CP1] | A manager can reject a pending request from their own department. The request status and reviewer are recorded, without reserving a seat or increasing budget spending. |
| REQ-6 | High | [CP2] | An admin can create, update, deactivate, and browse SaaS products, including their categories, per-seat prices, billing currencies, and maximum seat counts. |
| REQ-7 | High | [CP2] | An admin can manage departments and their monthly spending limits; authorized users can inspect the budget limit, amount spent, and remaining budget for their department. |
| REQ-8 | High | [CP2] | Authorized users can release or revoke assigned licenses. The system updates the license status, frees the occupied seat, and adjusts the applicable department budget while retaining license history. |
| REQ-9 | High | [CP2] | An admin can transfer an employee to another department. Costs of the employee's active licenses are transferred between the departments' relevant monthly budgets atomically, provided the destination budget can cover them. |
| REQ-10 | High | [CP2] | An admin can deactivate an employee during offboarding. Their active licenses are revoked or released according to the business rules, and budget allocations are updated without physically deleting historical data. |
| REQ-11 | Medium | [CP2] | The system can retrieve EUR/CZK and USD/CZK exchange rates from an external REST provider, such as ČNB or Frankfurter. During CP1, a local exchange-rate provider is used instead. |
| REQ-12 | Medium | [CP2] | Authorized users can trigger a recalculation of current monthly subscription costs using updated exchange rates. Previously locked assignment prices and historical records remain available for audit. |
| REQ-13 | Medium | [CP2] | Authorized users can search and filter products, license requests, and assigned licenses by relevant attributes such as department, currency, request/license status, price range, and available seats. |
| REQ-14 | High | [CP2] | The application exposes REST endpoints and supports email/password authentication and role-based authorization for employees, department managers, and admins. Managers may only access approval and budget-management operations for their own department. |


## 4. External Interface Requirements

- **User Interfaces:** In Checkpoint 2, the system provides a desktop web interface tailored to the three roles (`Employee`, `DepartmentManager`, `Admin`) for managing subscription catalogs, license requests, active seats, and monthly department budgets.
- **Software & Communications Interfaces:** The backend exposes a JSON-based REST API over HTTP/HTTPS (Checkpoint 2) and integrates with an external REST exchange-rate service (ČNB or Frankfurter / ECB API) to convert `EUR` and `USD` subscription prices into `CZK`.
- **Service Decoupling:** The exchange-rate provider is accessed through an application-level `ExchangeRateService` interface so that a deterministic local implementation can be used in Checkpoint 1 and automated tests.

---

## 5. Non-Functional Requirements

### 5.1 Performance Requirements

- Catalog queries and multi-criteria filtering results (by department, currency, status, price range, and seat availability) shall return within 1 second for 95% of requests under normal load.
- The system shall support at least 200 concurrent users without performance degradation, handling peak administrative periods such as start-of-month onboarding waves, monthly budget resets, and end-of-month FinOps cost revaluations.
- Available seat capacity and remaining monthly budget balances shown to users shall be no more than 2 seconds out of date and must immediately reflect committed transactions during approval checks.

### 5.2 Safety Requirements

All state-changing financial and license operations (request approval, manager self-acquisition, license release/revocation, department transfer, and employee offboarding) execute within ACID database transactions. If any business rule check or external exchange-rate lookup fails, the transaction rolls back completely so no partial seat allocation or inconsistent department budget spending is persisted. Historical records are protected against accidental destruction: deactivated employees use soft-deletion (`active = false`) and ended licenses transition to `RELEASED` or `REVOKED` rather than being physically deleted from the database.

### 5.3 Security Requirements

- User authentication uses email and password credentials; passwords are never stored in plain text and are persisted only as cryptographic hashes.
- Role-based access control (`Employee`, `DepartmentManager`, `Admin`) is enforced on the server side for every protected service method and REST endpoint; inactive users (`active = false`) cannot access protected functionality.
- Department isolation is strictly enforced: a `DepartmentManager` can review requests, revoke active licenses, and monitor budget utilization solely within their own department.
- Employee personal data (names, corporate emails, department assignments) is handled in accordance with GDPR principles, and all license approvals, rejections, assignments, and revocations record timestamps and the responsible actor for internal FinOps auditability.

### 5.4 Software Quality Attributes

- **Correctness & Data Integrity:** Active seat allocations for a product must never exceed its `maxSeatsCount`, each department has at most one monthly budget record per `(year, month)`, and a license stores its `lockedPrice` in `CZK` upon assignment so subsequent exchange-rate fluctuations do not silently overwrite historical expenditure.
- **Reliability:** Failures or timeouts of the external exchange-rate API must never leave a license request partially approved; rate-dependent operations either obtain a valid rate or fail cleanly without committing changes.
- **Testability:** External currency conversion is isolated behind an injectable `ExchangeRateService` interface, enabling deterministic offline testing in Checkpoint 1 and repeatable integration tests.
- **Maintainability:** The backend follows a layered Spring Boot architecture separating JPA domain entities, DAO repositories, and transactional service logic, allowing persistence and web layers to evolve cleanly between Checkpoint 1 and Checkpoint 2.
- **Usability:** Core workflows—submitting a request, approving department requests, and tracking monthly `CZK` budget consumption—are designed to be clear and consistent across the REST API and web interface.


