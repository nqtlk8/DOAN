# Data Schema - v6 (Flyway gộp 2026-10-01)

## 1. Nguồn chuẩn
Thư mục `erp-platform/services/erp-backend/src/main/resources/`:

| Vị trí | Chạy ở | Nội dung |
|---|---|---|
| `db/migration/V1__identity_and_branch.sql` | HQ + mọi chi nhánh | branch, role, permission, role_permission, user_account, user_branch_role |
| `db/migration/V2__catalog.sql` | HQ + mọi chi nhánh | category, product, supplier, price_list |
| `db/migration/V3__crm.sql` | HQ + mọi chi nhánh | customer, receivable_debt, receivable_debt_movement |
| `db/migration/V4__inventory.sql` | HQ + mọi chi nhánh | stock_on_hand, stock_movement, cost_layer, inbound_receipt(_line), inventory_alert_config |
| `db/migration/V5__sales.sql` | HQ + mọi chi nhánh | sales_invoice(_line), goods_return(_line), customer_product_price |
| `db/migration/V6__analytics.sql` | HQ + mọi chi nhánh | dim_date, fact_sales, fact_stock_movement |
| `db/migration/V7__common.sql` | HQ + mọi chi nhánh | idempotency_record |
| `db/migration/V8__replication_support.sql` | HQ + mọi chi nhánh | REPLICA IDENTITY cho bảng snapshot |
| `db/migration-hq/R__reference_data.sql` | chỉ HQ | dữ liệu hệ thống: chi nhánh TP1/TP2, vai trò, 3 tài khoản mặc định; cây danh mục sản phẩm (giống website web-public) |
| `db/migration-branch/R__branch_db_security.sql` | chỉ chi nhánh | quyền "mặc định chỉ đọc" cho role ứng dụng |

Chuỗi cũ (V1..V21, trộn schema + seed + vá dữ liệu) được lưu tham khảo tại `docs/to_remove/flyway-legacy/`. Schema cuối của chuỗi mới đã được so sánh tự động với chuỗi cũ (cột, kiểu, nullable, default, unique/check/FK, index, replica identity). Khác biệt duy nhất là bỏ `supplier.branch_id`.

Quy tắc thêm migration mới:
1. File `V9__<module>_<hành_động>_<đối_tượng>.sql`, đánh số tiếp, đặt trong `db/migration` (chung cho mọi instance).
2. Không trộn DDL và cập nhật dữ liệu trong một file. Không đưa dữ liệu demo vào Flyway (dùng `scripts/seed-demo.py`).
3. File riêng HQ hoặc chi nhánh chỉ được là repeatable `R__`.
4. Bảng mới do chi nhánh ghi: thêm vào danh sách trong `R__branch_db_security.sql`. Bảng cần replicate: thêm vào `scripts/replication-tables.conf`.
5. UTF-8 không BOM (`FlywayEncodingTest` kiểm tra).

## 2. Các đặc điểm cốt lõi của schema
- **Khóa ngoại**: FK chỉ đặt trong cùng module (vd. `product → category`, `sales_invoice_line → sales_invoice`, `cost_layer → stock_movement`), cộng một số FK lịch sử sang `customer` (`receivable_debt`, `receivable_debt_movement`, `goods_return`) và sang `branch`/`product` (`stock_movement`, `cost_layer`, `price_list`). `sales_invoice.customer_id` và `inbound_receipt.supplier_id` là UUID trần. Logical replication không kiểm FK khi apply nên thứ tự đồng bộ giữa các bảng không gây lỗi.
- **Idempotency**: Thêm bảng `idempotency_record` để theo dõi các Request Key chống trùng lặp.
- **Append-only Financial Ledger**: `receivable_debt_movement` đóng vai trò là Sổ cái công nợ duy nhất ghi nhận `SALE` (+), `PAYMENT` (-), `RETURN` (-). Bảng `receivable_debt` chỉ là snapshot (Projection) từ `receivable_debt_movement`.

## 3. Danh sách bảng chính theo Domain

### Identity & Access (HQ Owned)
- `user_account`: Thông tin user (đăng nhập bằng username, password_hash).
- `role`: Chứa `ADMIN` và `STAFF`.
- `permission`, `role_permission`: Danh sách quyền nhỏ nát.
- `user_branch_role`: Mapping User vào Branch và Role.

### Catalog (HQ Owned)
- `category`: Phân cấp danh mục (có `parent_id`), 2 cấp. Dữ liệu ban đầu trong `R__reference_data.sql`, lấy từ cây danh mục của website (`apps/web-public/src/test/mockData.ts`), `code` = slug của website:
  - `vlxd` VẬT LIỆU XÂY DỰNG: `ban-cau`, `bon-cau-1-khoi`, `bon-cau-2-khoi`, `bon-cau-thong-minh`, `chau-rua`
  - `ttnt` TRANG TRÍ NỘI THẤT: `den-trang-tri`, `rem-cua`, `sofa`, `ban-tra`, `do-trang-tri`
  Sản phẩm chỉ gắn vào danh mục con (`ProductWriter.requireLeafCategory`); danh mục gốc dùng để nhóm. Thêm danh mục mới: thêm dòng vào `R__reference_data.sql` (chèn nếu chưa có theo `code`), Flyway chạy lại ở lần khởi động HQ tiếp theo và replication đưa xuống chi nhánh.
- `product`: Thông tin sản phẩm. Khóa chính `Long id`.
- `supplier`: Nhà cung cấp — dữ liệu dùng chung toàn hệ thống, chỉ HQ (ADMIN) tạo/sửa, mọi chi nhánh thấy các NCC đang hoạt động. Không có `branch_id`.
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
- `inbound_receipt` & `inbound_receipt_line`: Phiếu nhập kho từ nhà cung cấp (`supplier_id` tham chiếu `supplier` dùng chung do HQ quản lý).
- `cost_layer`: Lớp giá FIFO sinh ra sau khi Confirm Phiếu nhập. Được tiêu thụ lúc Confirm Bán.

### Analytics (HQ / Aggregated)
- `dim_date`: Chiều thời gian cho Data Warehouse đơn giản.
- `fact_sales`: Báo cáo bán hàng.
- `fact_stock_movement`: Báo cáo tồn kho.
- `inventory_alert_config`: Cấu hình cảnh báo khi tồn kho dưới ngưỡng (theo `product_id`, `branch_id`).

*(Ghi chú: Bảng `inventory_alert_log` không còn trong schema — đã bỏ từ chuỗi migration cũ).*

## 4. Bảng ngoài phạm vi (Removed or Planned for V6)
Các bảng sau ĐÃ BỊ LOẠI BỎ và **KHÔNG TỒN TẠI** trong codebase thực tế:
- `customer_order` (Chỉ dùng `sales_invoice` bán trực tiếp)
- `supplier_purchase_order` (Chỉ dùng `inbound_receipt` nhập trực tiếp)
- `payable_debt` (Chưa quản lý công nợ nhà cung cấp)
- `stock_transfer` (Chưa hỗ trợ chuyển kho)
