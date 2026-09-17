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
- TP1 app: host 8081 -> container 8080.
- TP2 app: host 8082 -> container 8080.
- TP1 Nginx: 81 -> 80.
- TP2 Nginx: 82 -> 80.
- HQ DB 5432, TP1 DB 5433, TP2 DB 5434.
- Redis HQ 6379.

## Current security facts

- `SecurityConfig` is stateless.
- Public: auth/public/health/swagger paths.
- Role system consists of ONLY `ADMIN` and `STAFF`.
- Phân quyền theo Chi nhánh (`Branch-Level Security`) được thực hiện ngay tại lớp Controller/Service, sử dụng `AuthUtils.getBranchIdOrNull()`. Nhân viên chi nhánh A không thể truy vấn hoặc thao tác trên dữ liệu chi nhánh B.
- HQ Analytics dynamically aggregates stock and debt via `stock_movement` and `sales_invoice` rather than copying snapshot tables (`stock_on_hand`, `receivable_debt`).
- Branch master tables are restricted by `V9__branch_db_security.sql` for `erp_user` in Docker runtime.

## Source of truth

When answering questions about current behavior, inspect:

1. Java source.
2. SQL migrations (`V1` to `V9`).
3. Sprints Documentation (`docs/sprints/sprint-1-to-23`).
4. Docker Compose / Nginx.
5. Unit & Integration Tests (cực kỳ đầy đủ, bao gồm E2E flow).
