# Data Schema - v5 (Post-Refactoring)

## 1. Nguồn chuẩn
Schema hiện hành lấy từ:
`erp-platform/services/erp-backend/src/main/resources/db/migration/V1__init_schema.sql`
Và được cấp quyền bảo mật tại:
`erp-platform/services/erp-backend/src/main/resources/db/migration-branch/V9__branch_db_security.sql`

## 2. Các thay đổi cốt lõi trong Schema (Sprints 1-23)
- **Tách Bounded Contexts (No Cross-Module Foreign Keys)**: Bảng thuộc các module khác nhau không liên kết FK ở mức DB/JPA (ví dụ: `goods_return.customer_id` là UUID trần, không phải FK ràng buộc cứng sang `customer.id` bằng `FOREIGN KEY`). Trách nhiệm đảm bảo toàn vẹn tham chiếu nằm ở Application Layer thông qua Facade.
- **Idempotency**: Thêm bảng `idempotency_record` để theo dõi các Request Key chống trùng lặp.
- **Append-only Financial Ledger**: `receivable_debt_movement` đóng vai trò là Sổ cái công nợ duy nhất ghi nhận `SALE` (+), `PAYMENT` (-), `RETURN` (-). Bảng `receivable_debt` chỉ là snapshot (Projection) từ `receivable_debt_movement`.

## 3. Danh sách bảng chính theo Domain

### Identity & Access (HQ Owned)
- `user_account`: Thông tin user (đăng nhập bằng username, password_hash).
- `role`: Chứa `ADMIN` và `STAFF`.
- `permission`, `role_permission`: Danh sách quyền nhỏ nát.
- `user_branch_role`: Mapping User vào Branch và Role.

### Catalog (HQ Owned)
- `category`: Phân cấp danh mục (có `parent_id`).
- `product`: Thông tin sản phẩm. Khóa chính `Long id`.
- `price_list`: Bảng giá theo thời gian và chi nhánh.

### CRM (Customer HQ, Debt Branch)
- `customer`: Khách hàng Master (Khóa UUID, `customer_code` unique).
- `receivable_debt`: Snapshot dư nợ tại một Branch (Unique Constraint: `customer_id, branch_id`).
- `receivable_debt_movement`: Lịch sử giao dịch công nợ (`SALE`, `PAYMENT`, `RETURN`). Khóa chính UUID.

### Sales & Return (Branch Owned)
- `sales_invoice`: Hóa đơn bán (Trạng thái: `DRAFT`, `CONFIRMED`, `CANCELLED`). Có `customer_id` (UUID trần, không FK mềm).
- `sales_invoice_line`: Chi tiết hóa đơn (Sản phẩm, số lượng, đơn giá).
- `goods_return`: Phiếu trả hàng khách. Có `customer_id`, `invoice_id` (UUID).
- `goods_return_line`: Chi tiết trả hàng.

### Inventory (Branch Owned)
- `stock_on_hand`: Tồn kho hiện tại. (Unique: `product_id, branch_id`).
- `stock_movement`: Sổ cái kho (Nhập/Xuất/Bán/Trả). Lưu vết số lượng thay đổi.
- `inbound_receipt` & `inbound_receipt_line`: Phiếu nhập kho từ Supplier (Supplier ở bản V5 cũng Branch-owned).
- `cost_layer`: Lớp giá FIFO sinh ra sau khi Confirm Phiếu nhập. Được tiêu thụ lúc Confirm Bán.

### Analytics (HQ / Aggregated)
- `dim_date`: Chiều thời gian cho Data Warehouse đơn giản.
- `fact_sales`: Báo cáo bán hàng.
- `fact_stock_movement`: Báo cáo tồn kho.
- `inventory_alert_config` & `inventory_alert_log`: Cảnh báo khi tồn kho quá ít.

## 4. Bảng ngoài phạm vi (Removed or Planned for V6)
Các bảng sau ĐÃ BỊ LOẠI BỎ và **KHÔNG TỒN TẠI** trong codebase thực tế:
- `customer_order` (Chỉ dùng `sales_invoice` bán trực tiếp)
- `supplier_purchase_order` (Chỉ dùng `inbound_receipt` nhập trực tiếp)
- `payable_debt` (Chưa quản lý công nợ nhà cung cấp)
- `stock_transfer` (Chưa hỗ trợ chuyển kho)
