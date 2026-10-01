# AI Context - ERP v5 (Post-Refactoring 23-Sprint)

## Identity

Hệ thống ERP cho cửa hàng VLXD & thiết bị thông minh nhà.

## Architecture

- **Stack**: Java 21 + Spring Boot 3.4.0 + Hibernate 6.
- **Pattern**: Modular Monolith (Strict Bounded Contexts).
- **Deployment**: Same backend artifact -> 1 HQ + N Branch instances.
- **Database**: PostgreSQL DB riêng cho từng instance, đồng bộ bằng **PostgreSQL Logical Replication hai chiều**:
  - HQ publish Master Data -> Branch subscribe.
  - Branch publish Transaction Data -> HQ subscribe.
  - **Ownership Invariant**: HQ cấm API ghi transaction, Branch cấm API ghi master.
- **Cache**: Redis chỉ tại HQ.
- **Auth**: HQ signs JWT RS256 (sử dụng private key external mount vào thư mục `secrets/`). Branch verifies locally (chỉ mount public key).
- **Primary Keys**: Khóa chính Master Data = `Long`, Khóa chính Transaction (Invoice, Receipt, StockMovement) = `UUID`.

## Active business modules

- `identity`: Xác thực, phân quyền (Role/Permission).
- `branch`: Thông tin chi nhánh.
- `catalog`: Sản phẩm (Product), Danh mục (Category), Nhà cung cấp (Supplier).
- `crm`: Khách hàng (Customer) và Công nợ (ReceivableDebt, ReceivableDebtMovement).
- `order`: Đơn bán hàng (SalesInvoice) và Đơn khách trả (GoodsReturn).
- `inventory`: Tồn kho (StockOnHand, StockMovement), Phiếu nhập (InboundReceipt), Tính giá vốn (CostLayer).
- `analytics`: Báo cáo, thống kê đa chi nhánh (tại HQ).
- `common`: Các Utility, AOP, Event System, Cấu hình chung.

## Removed modules (Do NOT describe as active)

- Customer Order
- Procurement / Purchase Order
- Payable Debt
- Stock Transfer
- Outbound Receipt
- RFQ

## Business Core Flows

1. **Inbound Receipt**: Nhập kho -> Tăng `StockOnHand` + Ghi nhận `CostLayer` (FIFO).
2. **Sales Invoice Confirm**: Xác nhận bán hàng -> Giảm `StockOnHand` (tiêu thụ `CostLayer`) + Tăng `ReceivableDebt` (Movement: `SALE` [+]).
3. **Goods Return Confirm**: Khách trả hàng -> Tăng `StockOnHand` (cộng lại kho) + Giảm `ReceivableDebt` (Movement: `RETURN` [-]).
4. **Customer Payment**: Thu tiền nợ -> Giảm `ReceivableDebt` (Movement: `PAYMENT` [-]).
*Quy tắc chuẩn: `SUM(Movement Amount) = Current Debt`*.

## Codebase Strict Rules

- **No Cross-Module Repository Injection**: Các module không được tự ý import `*Repository` của module khác. Toàn bộ giao tiếp liên module phải thông qua **Facade Pattern** (e.g. `CrmFacade`, `OrderFacade`, `CatalogFacade`).
- **No Cross-Module @ManyToOne**: Domain Entity không giữ Object Reference của Context khác (VD: `GoodsReturn` dùng `UUID customerId`, không dùng `Customer customer`). Tránh rò rỉ (Domain Leakage).
- **No Entity in API Responses**: Mọi dữ liệu trả về qua Controller phải được bọc trong `*ResponseDto`.
- **Strict Idempotency**: Tất cả các hành động state-changing (POST, PUT) phải đi kèm Header `Idempotency-Key` và annotation `@IdempotencyProtected`.
- **Anti N+1 Queries**: Xử lý dữ liệu hàng loạt thông qua Batch Fetching (`findAllById`) hoặc `@EntityGraph`. Không dùng loop để chọc vào database.

## Docker & Environments

- ERP frontend: host 80.
- HQ app: host 8080 -> container 8080.
- HQ app: 127.0.0.1:8080 -> 8080. Branch apps: no host port (reach them through branch nginx).
- TP1 Nginx: 81 -> 80. TP2 Nginx: 82 -> 80 (compose profile `tp2`, toggle with `scripts/branch.sh on|off tp2`).
- DBs on 127.0.0.1 only: HQ 5432, TP1 5433, TP2 5434. Redis: no host port.

## Current security facts

- `SecurityConfig` is stateless.
- Public: auth/public/health/swagger paths.
- Role system consists of ONLY `ADMIN` and `STAFF`.
- Phân quyền theo Chi nhánh (`Branch-Level Security`) được thực hiện ngay tại lớp Controller/Service, sử dụng `AuthUtils.getBranchIdOrNull()`. Nhân viên chi nhánh A không thể truy vấn hoặc thao tác trên dữ liệu chi nhánh B.
- HQ Analytics dynamically aggregates some metrics, but snapshot tables (`stock_on_hand`, `receivable_debt`) are replicated from Branch to HQ with a row filter (`branch_id = X`) to support reconciliation and reporting.
- Apps connect as `erp_app` (non-superuser). On branch DBs `db/migration-branch/R__branch_db_security.sql` makes every table read-only for `erp_app` except branch-owned tables. Flyway runs as `erp_user` (owner).
- Supplier is shared master data (no `branch_id`): created only at HQ by ADMIN, replicated to every branch; staff see all active suppliers.
- Flyway has no seed data. HQ-only reference data (branches TP1/TP2, roles, admin/staff accounts) is in `db/migration-hq/R__reference_data.sql`; master data reaches branches via replication with `copy_data = true`. Demo data: `scripts/seed-demo.py` (via API).

## Source of truth

When answering questions about current behavior, inspect:

1. Java source.
2. SQL migrations (`db/migration` V1..V8, `db/migration-hq`, `db/migration-branch`).
3. Architecture docs (`docs/architecture/*`, `docs/testing/TEST_STRATEGY.md`). Old sprint notes are archived in `docs/to_remove/` (history only, not source of truth).
4. Docker Compose / Nginx.
5. Unit & Integration Tests (cực kỳ đầy đủ, bao gồm E2E flow).
