# System Architecture - v5 (Post-Refactoring)

## 1. Phạm vi
Hệ thống là ERP cho cửa hàng vật liệu xây dựng và thiết bị thông minh nhà, vận hành theo mô hình **1 HQ + N Branch**. Backend sử dụng một codebase Java/Spring Boot và được triển khai thành nhiều instance.

Trong phiên bản hiện tại, backend tập trung vào các domain chính (Bounded Contexts):
- `identity`: Quản lý tài khoản, phân quyền (Role/Permission).
- `branch`: Thông tin cấu trúc các chi nhánh.
- `catalog`: Sản phẩm (Product), danh mục, nhà cung cấp.
- `crm`: Khách hàng, quản lý công nợ (ReceivableDebt).
- `order`: Đơn bán hàng (SalesInvoice), Đơn trả hàng (GoodsReturn).
- `inventory`: Tồn kho (StockOnHand, StockMovement), Nhập kho (InboundReceipt), Tính giá vốn FIFO (CostLayer).
- `analytics`: Báo cáo tập trung.
- `common`: Các utilities, security, logging, AOP (Idempotency).

## 2. Nguyên tắc kiến trúc

### 2.1. Modular Monolith & Bounded Contexts (DDD)
Các module nghiệp vụ nằm trong cùng một Spring Boot application nhưng được thiết kế với **sự cô lập nghiêm ngặt**:
- **Không inject Repository chéo**: Các module giao tiếp với nhau duy nhất thông qua các interface **Facade** (`CrmFacade`, `OrderFacade`, `CatalogFacade`, `BranchFacade`, `InventoryFacade`).
- **Không Entity Leakage**: Các Domain Entity ở module này không được trỏ ngoại (`@ManyToOne`) sang Domain Entity của module khác. Toàn bộ tham chiếu ngoại lai đều dùng Native ID (`UUID`, `Long`).
- **Data Transfer Objects (DTO)**: Toàn bộ API Controller trả về DTO thay vì Domain Entity để bảo vệ cấu trúc nội bộ.

### 2.2. Phân quyền (Role System)
- Role giới hạn ở `ADMIN` và `STAFF`.
- Phân quyền theo Chi nhánh (Branch-Level Security) thực hiện qua `AuthUtils.getBranchIdOrNull()` để đảm bảo dữ liệu nhánh nào chỉ nhánh đó thấy.

### 2.3. N-instance deployment & Replication
Cùng một backend artifact (file .jar) được triển khai thành:
- 01 HQ instance (quản trị tập trung, caching Redis).
- N Branch instances (xử lý giao dịch cục bộ).

Trao đổi dữ liệu thông qua **PostgreSQL Logical Replication**:
- HQ -> Branch: Master Data (Product, Category, Branch, User).
- Branch -> HQ: Transaction Data (Invoice, Receipt, Movement).

### 2.4. Idempotency & Financial Consistency
- **Idempotency**: API tạo mới / cập nhật đều có `@IdempotencyProtected` (ngăn cản người dùng double-click sinh nhiều hoá đơn/giao dịch).
- **Draft & Confirm**: Nghiệp vụ Inbound, Sales, và Goods Return đều trải qua 2 bước:
  1. `Draft`: Tạo bản nháp, lấy ID.
  2. `Confirm`: Xác nhận bản nháp -> Chốt dữ liệu, cập nhật kho (`Stock Movement`), tính giá vốn (`FIFO`), cập nhật nợ (`Receivable Debt`).
- **Signed Ledger**: Dòng tiền nợ được tính theo công thức bù trừ (`SUM(amount) = Current Debt`), ví dụ Mua hàng là `+`, Thanh toán hoặc Trả hàng là `-`.

## 3. Runtime topology hiện tại

```text
                          Client Browser
                               |
                               v
                    +----------------------+
                    |   erp-frontend       |
                    |   Nginx :80          |
                    +----------+-----------+
                               |
                         /api/* proxy
                               |
                               v
                    +----------------------+
                    |   hq-app              |
                    |   Spring Boot :8080   |
                    +----+-------------+----+
                         |             |
                         v             v
                 +-----------+    +---------+
                 | hq-db     |    | redis-hq|
                 | :5432     |    | :6379   |
                 +-----------+    +---------+

Branch TP1:
Client -> branch-tp1-nginx :81 -> branch-tp1-app :8080 -> branch-tp1-db :5432

Branch TP2:
Client -> branch-tp2-nginx :82 -> branch-tp2-app :8080 -> branch-tp2-db :5432
```

## 4. Docker port mapping

| Service | Host port | Container port | Vai trò |
|---|---:|---:|---|
| `erp-frontend` | 80 | 80 | ERP UI + reverse proxy API |
| `hq-app` | 8080 | 8080 | HQ Spring Boot |
| `branch-tp1-app` | 8081 | 8080 | Branch TP1 Spring Boot |
| `branch-tp2-app` | 8082 | 8080 | Branch TP2 Spring Boot |
| `branch-tp1-nginx` | 81 | 80 | Reverse proxy Branch TP1 |
| `branch-tp2-nginx` | 82 | 80 | Reverse proxy Branch TP2 |
| `hq-db` | 5432 | 5432 | PostgreSQL HQ |
| `branch-tp1-db` | 5433 | 5432 | PostgreSQL TP1 |
| `branch-tp2-db` | 5434 | 5432 | PostgreSQL TP2 |
| `redis-hq` | 6379 | 6379 | Redis HQ |

## 5. Request path

```text
Browser
  -> erp-frontend Nginx
  -> /api/* proxy
  -> hq-app hoặc branch-app
  -> Spring Security Filter Chain (verify JWT RS256)
  -> Controller (DTO Mapping)
  -> Application Service / Facade (Business Logic)
  -> Hibernate / JPA (Batch Fetching)
  -> PostgreSQL
```
