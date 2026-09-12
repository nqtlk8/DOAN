# Data Schema — v5

## 1. Nguồn chuẩn

Schema hiện hành lấy từ:

`erp-platform/services/erp-backend/src/main/resources/db/migration/V1__init_schema.sql`

và quyền database của Branch từ:

`erp-platform/services/erp-backend/src/main/resources/db/migration-branch/V9__branch_db_security.sql`

Schema hiện hành có **26 bảng**.

## 2. Danh sách bảng

| Module | Bảng | Vai trò |
|---|---|---|
| Identity | `user_account` | Tài khoản |
| Identity | `role` | Vai trò |
| Identity | `permission` | Quyền |
| Identity | `role_permission` | Liên kết role-quyền |
| Identity | `user_branch_role` | Gán user vào role/branch |
| Branch | `branch` | Chi nhánh |
| Catalog | `category` | Danh mục |
| Catalog | `product` | Sản phẩm |
| Catalog | `price_list` | Giá theo branch |
| Catalog | `supplier` | Nhà cung cấp |
| CRM | `customer` | Khách hàng master |
| CRM | `receivable_debt` | Công nợ phải thu |
| CRM / Sales | `customer_product_price` | Giá riêng theo khách hàng |
| Sales | `sales_invoice` | Hóa đơn |
| Sales | `sales_invoice_line` | Dòng hóa đơn |
| Return | `goods_return` | Phiếu trả hàng |
| Return | `goods_return_line` | Dòng trả hàng |
| Inventory | `stock_on_hand` | Tồn kho hiện tại |
| Inventory | `inbound_receipt` | Phiếu nhập |
| Inventory | `inbound_receipt_line` | Dòng nhập |
| Inventory | `stock_movement` | Sổ kho (Ledger) |
| Inventory | `cost_layer` | Lớp giá FIFO |
| Common | `idempotency_record` | Idempotency |
| Analytics | `dim_date` | Chiều ngày |
| Analytics | `fact_sales` | Fact bán hàng |
| Analytics | `fact_stock_movement` | Fact biến động kho |
| Analytics | `inventory_alert_config` | Cấu hình cảnh báo tồn |
| Analytics | `inventory_alert_log` | Log cảnh báo |

## 3. Nhóm Identity

### `user_account`

Khóa chính `id` kiểu bigint. Lưu username, mật khẩu hash, họ tên, email, phone và trạng thái hoạt động.

### `role`

Hai role được seed trong môi trường hiện tại: `STAFF` và `ADMIN`.

### `permission`

Lưu mã quyền và mô tả.

### `role_permission`

Liên kết role và permission.

### `user_branch_role`

Liên kết user với role và branch. `branch_id` có thể NULL đối với ADMIN HQ.

## 4. Catalog

### `category`

Danh mục sản phẩm. `parent_id` cho phép tạo cấu trúc cây.

### `product`

Thông tin sản phẩm gồm `code`, `name`, `base_unit`, `category_id`, `attributes_cache` và trạng thái hoạt động.

### `price_list`

Lưu giá theo sản phẩm, branch và ngày hiệu lực.

### `supplier`

Thông tin nhà cung cấp được dùng trực tiếp trong nghiệp vụ nhập hàng.

## 5. CRM

### `customer`

Khách hàng là master data. Khóa chính kiểu UUID; `customer_code` là duy nhất. `customer_type` đã được loại bỏ theo yêu cầu mới nhất (tất cả khách hàng đều có thể mua chịu và được đối xử bình đẳng).

### `receivable_debt`

Quản lý công nợ theo cặp `customer_id + branch_id`.

### `customer_product_price`

Lưu giá sản phẩm riêng cho một khách hàng trong một branch cùng khoảng hiệu lực.

## 6. Sales

### `sales_invoice`

Khóa chính UUID. Các trạng thái hiện hành:

- `DRAFT`
- `CONFIRMED`
- `CANCELLED`

Phương thức thanh toán:

- `CASH`
- `CREDIT`
- `MIXED`

Bảng lưu thêm `previous_debt`, `remaining_debt`, `confirmed_by` và `confirmed_at`.

### `sales_invoice_line`

Liên kết với hóa đơn bằng `invoice_id`. Lưu sản phẩm, tên snapshot, đơn vị tính, số lượng, đơn giá, giá vốn và thành tiền.

## 7. Goods Return

### `goods_return`

Liên kết tới khách hàng, branch và hóa đơn gốc. Trạng thái:

- `DRAFT`
- `CONFIRMED`

### `goods_return_line`

Lưu sản phẩm, số lượng, đơn giá và đơn vị tính.

## 8. Inventory

### `stock_on_hand`

Trạng thái tồn kho hiện tại của một sản phẩm tại một branch. Có unique constraint:

```text
(product_id, branch_id)
```

Lưu `quantity`, `version` và metadata audit (đã loại bỏ `avg_cost` để tuân thủ 100% FIFO theo Cost Layer).

### `inbound_receipt`

Phiếu nhập kho. Trạng thái:

- `DRAFT`
- `CONFIRMED`

Liên kết với `supplier_id` và `branch_id`.

### `inbound_receipt_line`

Lưu sản phẩm, số lượng, đơn vị tính và đơn giá nhập.

### `stock_movement`

Sổ kho (Inventory Ledger) ghi nhận lịch sử mọi thay đổi tồn kho (Nhập/Xuất/Bán/Trả). Khóa chính kiểu UUID. Lưu `movement_type`, `quantity`, và `reference_id`.

### `cost_layer`

Theo dõi các lớp giá nhập (FIFO Costing) cho từng đợt hàng để tính giá vốn chính xác khi xuất. Khóa chính kiểu UUID. Lưu `inbound_movement_id`, `original_quantity`, `remaining_quantity`, `unit_cost`.

## 9. Analytics

- `dim_date`: ngày.
- `fact_sales`: doanh thu, giá vốn, lợi nhuận, số lượng theo giao dịch.
- `fact_stock_movement`: biến động tồn kho.
- `inventory_alert_config`: Cấu hình ngưỡng cảnh báo tồn kho tối thiểu theo sản phẩm và branch.
- `inventory_alert_log`: lịch sử cảnh báo.

*Ghi chú*: Việc thu thập dữ liệu từ các module khác (như Inventory, CRM) cho Analytics được thực hiện qua `AnalyticsDataPort` để giữ tính độc lập của các bảng schema.

## 10. Common

### `idempotency_record`

Lưu `idempotency_key`, `request_hash` và `response_snapshot`. `idempotency_key` là duy nhất.

## 11. Base fields

Nhiều entity nghiệp vụ dùng các trường chung:

```text
id
version
created_at
updated_at
created_by
updated_by
is_deleted
```

## 12. Ownership

### HQ-owned master data

- `branch`
- `user_account`
- `role`
- `permission`
- `role_permission`
- `user_branch_role`
- `category`
- `product`
- `price_list`
- `customer`

### Branch business data

- `stock_on_hand`
- `inbound_receipt`
- `inbound_receipt_line`
- `sales_invoice`
- `sales_invoice_line`
- `goods_return`
- `goods_return_line`
- `receivable_debt`
- `customer_product_price`
- `supplier`

### Analytics

- `dim_date`
- `fact_sales`
- `fact_stock_movement`
- `inventory_alert_config`
- `inventory_alert_log`

`idempotency_record` phục vụ cross-cutting concern tại instance nơi request được xử lý.

## 13. Không thuộc schema v5

Không có các bảng:

- `customer_order`
- `customer_order_line`
- `supplier_purchase_order`
- `supplier_purchase_order_line`
- `payable_debt`
- `stock_transfer`
- `outbound_receipt`

Do đó không được mô tả các bảng trên như một phần của schema hiện hành.
