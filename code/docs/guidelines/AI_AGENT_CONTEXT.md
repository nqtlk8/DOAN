# ERP System for Construction Materials & Smart Home Store (VLXD & TTNT)
**Agent Context Document** - Quick Reference for AI Agents

## 1. Project Identity
- **Architecture:** Modular Monolith (single deployable, multiple instances).
- **Deployment:** 1 HQ instance + N Branch instances (each with own PostgreSQL DB).
- **Tech Stack:**
  - **Backend:** Java 21, Spring Boot 3.4.0, PostgreSQL 15, Redis 7 (HQ only).
  - **Frontend:** Next.js monorepo (`public-site`, `hq-admin`, `branch-erp`).
  - **Security:** JWT RS256 (HQ signs, Branches verify).
  - **Data Access:** Spring Data JPA, Hibernate, Flyway.
  - **Testing & API:** JUnit 5, Mockito, H2, SpringDoc OpenAPI.

## 2. Architecture & Deployment
- **Profiles:** `hq` (HQ instance) and `branch` (Branch instance). Both use the same JAR.
- **HQ Instance:** Contains private key (signs JWT), uses Redis, handles master data writes, runs `AuthController`.
- **Branch Instance:** Contains public key (verifies JWT), NO Redis, NO master data write endpoints, NO `AuthController`.
- **Data Sync:** PostgreSQL Logical Replication (Hub-and-Spoke).
  - *HQ → Branch:* Master data (product, category, customer, etc.) is READ-ONLY at branches.
  - *Branch → HQ:* Transactional data (sales, inventory) for reporting.
- **Access Control:** `@ConditionalOnProperty(name = "instance.role", havingValue = "HQ")` gates master-data-write beans.

## 3. Bounded Contexts

| Module | Package | Key DB Tables | Instance |
|---|---|---|---|
| **Identity** | `identity` | `user_account`, `role`, `permission`, `role_permission`, `user_branch_role` | HQ only (write) |
| **Catalog** | `catalog` | `product`, `category`, `price_list` | HQ write, All read |
| **CRM** | `crm` | `customer` (HQ master), `receivable_debt`, `customer_product_price` | Mixed |
| **Sales** | `order` | `sales_invoice`, `goods_return` (+ line tables) | Branch |
| **Inventory** | `inventory` | `stock_on_hand`, `inbound_receipt` | Branch |
| **Analytics** | `analytics` | `dim_date`, `fact_sales` | HQ |
| **Branch** | `branch` | `branch` | HQ (should be) |
| **Common** | `common` | `audit_log`, `idempotency_record` | All |

## 4. Database Quick Reference
- **HQ Master Tables (Replicated as Read-Only):** `product`, `category`, `price_list`, `unit_of_measure`, `customer`, `branch`, `user_account`, `role`, `permission`, `role_permission`, `user_branch_role`.
- **Branch-Local Tables:** `stock_on_hand`, `inbound_receipt`, `sales_invoice`, `goods_return`, `receivable_debt`, `customer_product_price`, `idempotency_record`, `audit_log`.
- **Design Decisions:** UUID primary keys everywhere. `stock_on_hand` allows negative quantities. `@Version` for optimistic locking. Soft deletes via `isDeleted`.

## 5. Security Model
- **Roles:** `STAFF` (operates at exactly 1 branch), `ADMIN` (operates at HQ only).
- **JWT Claims:** `subject` (username), `role`, `branchId` (null for ADMIN), `tokenId`, `type` (access/refresh).
- **Authentication Flow:** Login at HQ → Receive Tokens + Branch URL → Redirect.
- **3-Layer Branch Defense:** Nginx IP whitelist, `@ConditionalOnProperty` excluding write beans, PostgreSQL `app_user` with SELECT-only privileges on master tables.
- **Token Management:** Access (30m), Refresh (7d). Revocation via Redis denylist at HQ.

## 6. Key Conventions & Patterns
- **Standard API Response:** `ApiResponse<T>` with `{ success, data, message, errors }`.
- **Draft → Confirm Pattern:** Transactional entities use this flow. `confirm()` uses `@IdempotencyProtected` and `@Retryable` (Optimistic Locking).
- **BaseEntity:** `UUID id`, `Integer version`, `LocalDateTime createdAt/updatedAt`, `UUID createdBy/updatedBy`, `boolean isDeleted`.
- **AOP Annotations:**
  - `@BranchScoped`: Validates branchId in JWT, blocks HQ users from branch endpoints.
  - `@IdempotencyProtected`: Prevents duplicate requests using `Idempotency-Key` header.
  - `@Auditable`: Logs entity changes.
  - `@LogExecutionTime`: Measures method execution time.

## 7. API Endpoints Summary

| Method | Path | Auth | Instance | Notes |
|---|---|---|---|---|
| POST | `/api/v1/auth/login` | Public | HQ | Login |
| POST | `/api/v1/auth/refresh` (or `/revoke`) | Public | HQ | Token management |
| GET | `/api/v1/public/catalog/products` | Public | All | Public products |
| CRUD | `/api/v1/catalog/products` | Varies | Varies | HQ writes, All read |
| POST | `/api/v1/sales-invoices/{id}/confirm` | STAFF | Branch | `@BranchScoped`, `@IdempotencyProtected` |
| POST | `/api/v1/goods-returns/{id}/confirm` | STAFF | Branch | `@BranchScoped`, `@IdempotencyProtected` |
| POST | `/api/v1/inventory/inbound/{id}/confirm` | STAFF | Branch | `@BranchScoped`, `@IdempotencyProtected` |
| GET | `/api/v1/customer-prices/{custId}/product/{prodId}`| STAFF | Branch | `@BranchScoped` |
| GET | `/api/v1/analytics/dashboard` | Auth'd | HQ | Dashboard metrics |

## 8. Current State & Known Issues (Sprint 6, Aug 2026)
- **Completed:** N-instance architecture, JWT RS256, Catalog (EAV + JSONB), Logical Replication, Inventory core, Sales + Idempotency, Procurement, Customer Order, Phase 2, Frontend monorepo, OpenAPI.
- **Simplified Architecture (v5):** Removed Procurement, Customer Order, Outbound Receipt, RFQ. Kept Sales, Inbound, Goods Return, CRM, Catalog, and Identity.
- **Known Issues:**
  - Docker compose credentials mismatch (`erp_user` vs `app_user`).
  - `PreAuthorize` annotations use wrong authority format.
  - `getUserId()` returns random UUID in some controllers.
  - `BranchController` and `DashboardController` lack auth restrictions.
  - Missing `@EnableJpaAuditing` and JaCoCo coverage configuration.

## 9. Configuration Files
| File | Purpose |
|---|---|
| `application.yml` | Base config: app name, default profile=hq, port=8080, log level |
| `application-hq.yml` | HQ: datasource, Flyway user, Redis, JWT private+public key, expiration |
| `application-branch.yml`| Branch: datasource, Flyway locations, Redis excluded, JWT public key |
| `docker-compose.yml` | 3 Postgres + Redis + 3 backend + 2 nginx + frontend |
| `V1__init_schema.sql`| Full DDL: ~17 tables across 6 modules |
