# GIẢI THÍCH THIẾT KẾ CHUẨN HOÁ DATABASE (NF) & CÁC ĐIỂM TỐI ƯU HOÁ (DENORMALIZE)

Tài liệu này đi kèm `schema-database-v1.sql`, giải thích rõ **quy trình 2 bước** đã áp dụng khi thiết kế:

> **Bước 1:** Thiết kế đạt chuẩn 3NF (Third Normal Form) cho toàn bộ schema.
> **Bước 2:** Từ schema chuẩn 3NF đó, xác định các **hot path** (đường dẫn dữ liệu chạy thường xuyên, ảnh hưởng trực tiếp tới NFR hiệu năng đã cam kết), và **chỉ phá chuẩn ở đúng những điểm đó**, có ghi chú lý do rõ ràng trong cả tài liệu này lẫn comment trong file SQL.

---

## PHẦN 1 — KIỂM TRA CHUẨN HOÁ (1NF → 2NF → 3NF)

### 1.1 Nhắc lại 3 chuẩn áp dụng

- **1NF (First Normal Form):** mỗi cột chứa giá trị nguyên tử (atomic), không có nhóm lặp (repeating group), không có mảng/danh sách nhồi trong 1 cột.
- **2NF (Second Normal Form):** đạt 1NF, và mọi cột không khoá phải phụ thuộc **toàn bộ** khoá chính (không phụ thuộc bộ phận — chỉ áp dụng khi khoá chính là composite).
- **3NF (Third Normal Form):** đạt 2NF, và không có phụ thuộc bắc cầu (transitive dependency) — cột không khoá không được phụ thuộc vào 1 cột không khoá khác.

### 1.2 Ví dụ áp dụng cụ thể trên các bảng quan trọng

**`category` (self-reference phân cấp):**
- 1NF: đạt — mỗi cột 1 giá trị, không nhồi danh sách con vào 1 cột.
- 2NF/3NF: đạt — không lưu `full_path` hay `level` (đây là dữ liệu **suy ra được** từ việc duyệt `parent_id`, nếu lưu thêm sẽ vi phạm 3NF vì phụ thuộc vào chính dữ liệu khác trong bảng, dễ sai lệch khi cây danh mục thay đổi).

**`product_attribute_value` (EAV, thay vì nhồi thuộc tính vào nhiều cột trên `product`):**
- Đây chính là bài toán chuẩn hoá kinh điển: nếu thiết kế `product` có sẵn cột `color`, `size`, `origin`... cứng trong bảng — vi phạm 1NF về bản chất khi mỗi loại sản phẩm có bộ thuộc tính khác nhau (gạch có `size`/`pattern`, sơn có `color`/`volume`) → sẽ có rất nhiều cột NULL tuỳ loại sản phẩm, đây là dấu hiệu thiết kế sai chuẩn hoá.
- Giải pháp 3NF: tách `attribute_definition` (khai báo thuộc tính) + `category_attribute` (thuộc tính nào áp dụng cho danh mục nào — quan hệ N-N, dùng bảng junction) + `product_attribute_value` (giá trị thực tế theo từng sản phẩm) — đây là bảng **nguồn sự thật (source of truth)**, đạt 3NF hoàn toàn.

**`role_permission`, `category_attribute` (quan hệ N-N):**
- Đúng nguyên tắc chuẩn hoá quan hệ nhiều-nhiều: **luôn** tách bảng junction với khoá chính là composite (2 cột FK), không bao giờ nhồi danh sách ID vào 1 cột dạng CSV/mảng (vi phạm 1NF).

**`price_list` (lưu lịch sử giá theo `effective_date` thay vì 1 cột `current_price` trên `product`):**
- Nếu chỉ lưu `current_price` ngay trên `product`, sẽ mất lịch sử giá (không đối soát được giá tại 1 thời điểm quá khứ) và vi phạm nguyên tắc "1 sự kiện = 1 bản ghi" của chuẩn hoá dữ liệu giao dịch.

### 1.3 Kết luận Phần 1

Toàn bộ schema trong `schema-database-v1.sql` đạt chuẩn 3NF **trước khi** áp dụng bất kỳ tối ưu hoá nào — đây là baseline đúng đắn để đảm bảo **không mất/sai lệch dữ liệu** (đúng NFR quan trọng nhất đã cam kết từ đầu dự án).

---

## PHẦN 2 — CÁC ĐIỂM DENORMALIZE CÓ CHỦ ĐÍCH (PHÁ CHUẨN ĐỂ TỐI ƯU HIỆU NĂNG)

Nguyên tắc áp dụng: **chỉ phá chuẩn khi có 2 điều kiện đồng thời** — (a) dữ liệu đó nằm trên 1 **hot path** thực sự (chạy rất thường xuyên, ảnh hưởng latency nghiệp vụ chính), và (b) có **cơ chế đảm bảo đồng bộ** giữa bản ghi denormalize và nguồn sự thật (không để chúng "trôi" ra khỏi nhau theo thời gian).

### 2.1 `product.attributes_cache` (JSONB) — cache của `product_attribute_value`

| | |
|---|---|
| **Vi phạm gì** | Dữ liệu trùng lặp — `attributes_cache` là bản sao suy ra được từ `product_attribute_value` |
| **Vì sao chấp nhận** | Trang public catalog (public site) là trang **đọc nhiều nhất** trong toàn hệ thống (đúng đặc điểm ngành đã khảo sát ở tài liệu kiến trúc — SKU-heavy, traffic cao). Nếu mỗi request phải JOIN `product` × `product_attribute_value` × `attribute_definition` để dựng lại bộ thuộc tính, chi phí JOIN nhân với traffic cao sẽ khó đạt NFR "LCP < 2.5s". |
| **Cơ chế đồng bộ** | `product_attribute_value` là nguồn sự thật duy nhất được **ghi** (write). Application Service (`ProductWriter`, module `catalog`) **bắt buộc** cập nhật lại `attributes_cache` **trong cùng transaction** mỗi khi `product_attribute_value` thay đổi — không có đường ghi nào khác vào `attributes_cache` ngoài luồng này. |
| **Rủi ro còn lại** | Nếu code sau này thêm 1 đường ghi khác vào `product_attribute_value` mà quên đồng bộ cache → dữ liệu lệch. Đây là điểm cần review kỹ ở checklist code (đã ghi trong Kế hoạch triển khai v2, Phần 1.6). |

### 2.2 `sales_invoice.previous_debt` / `total_amount` / `remaining_debt` — snapshot lưu cứng

| | |
|---|---|
| **Vi phạm gì** | Về lý thuyết, `previous_debt` suy ra được từ `receivable_debt` tại thời điểm trước hoá đơn, `remaining_debt` suy ra được bằng công thức. Lưu cứng là dữ liệu dẫn xuất — vi phạm 3NF thuần tuý. |
| **Vì sao chấp nhận** | Đây **không phải** tối ưu hiệu năng, mà là yêu cầu **toàn vẹn lịch sử/audit bắt buộc**: hoá đơn đã in ra cho khách **không được phép** thay đổi số liệu khi công nợ khách hàng biến động ở giao dịch sau — nếu tính động, in lại hoá đơn cũ sẽ cho ra số khác với lúc khách nhận hàng, đây là lỗi nghiệp vụ nghiêm trọng (đã xác nhận rõ trong tài liệu bổ sung nghiệp vụ trước đó). |
| **Cơ chế đảm bảo đúng** | Chỉ ghi 1 lần duy nhất tại thời điểm `SalesInvoice.confirm()`, không bao giờ `UPDATE` lại sau đó (trừ trường hợp `CANCELLED`, không sửa số liệu). |

### 2.3 `stock_on_hand` — số dư tồn kho tổng hợp (running balance)

| | |
|---|---|
| **Vi phạm gì** | Về lý thuyết thuần 3NF, tồn kho tại 1 thời điểm = `SUM(inbound_receipt_line.quantity) − SUM(outbound_receipt_line.quantity) − SUM(sales_invoice_line.quantity) + SUM(goods_return_line.quantity)` — có thể tính động, không cần bảng riêng. |
| **Vì sao chấp nhận** | Đây là **hot path quan trọng nhất hệ thống** — mọi giao dịch bán hàng đều phải đọc/ghi tồn kho. Tính `SUM` động trên toàn bộ lịch sử giao dịch mỗi lần bán (có thể hàng chục nghìn dòng sau vài năm vận hành) **chắc chắn không đạt** NFR "API thường < 300ms", và đặc biệt sẽ tạo áp lực khoá (lock contention) cực lớn khi nhiều giao dịch đồng thời — đúng vấn đề đã lường trước ở Test Plan (race condition). |
| **Cơ chế đảm bảo đúng** | Đây là điểm rủi ro cao nhất trong toàn bộ hệ thống — vì vậy đã có **bộ test Data Integrity riêng** (Sprint 2.1, xem Kế hoạch triển khai v2) kiểm tra tuyệt đối: race condition, đối soát tổng nhập-xuất-tồn, rollback khi lỗi giữa transaction. `version` (optimistic locking) đảm bảo không mất cập nhật khi nhiều giao dịch đồng thời. |

### 2.4 `receivable_debt` / `payable_debt` — số dư công nợ tổng hợp

Tương tự mục 2.3 — công nợ tính động từ toàn bộ `sales_invoice`/`goods_return`/`inbound_receipt` sẽ chậm dần theo thời gian vận hành và không đáp ứng được yêu cầu hiển thị tức thời "nợ cũ" ngay khi mở hoá đơn mới. Denormalize thành số dư tổng hợp, cập nhật **trong cùng transaction** với giao dịch phát sinh — nguyên tắc toàn vẹn giao dịch đã nêu ở Phần 1.6 tài liệu Kế hoạch triển khai v2 áp dụng chính xác cho bảng này.

### 2.5 `customer_product_price.last_price` — giá gần nhất theo khách hàng

Về lý thuyết, giá gần nhất suy ra được bằng truy vấn `SELECT unit_price FROM sales_invoice_line ... ORDER BY created_at DESC LIMIT 1`. Nhưng đây là **query chạy mỗi lần thêm 1 dòng sản phẩm vào hoá đơn** (hot path tương tác trực tiếp với nhân viên bán hàng — cần phản hồi tức thời) — denormalize thành bảng tra cứu trực tiếp theo khoá `(customer_id, product_id, branch_id)` giúp query đơn giản = 1 lần tra `PRIMARY KEY lookup`, thay vì `ORDER BY ... LIMIT 1` trên bảng giao dịch ngày càng lớn.

### 2.6 `sales_invoice_line.unit_cost_snapshot` — giá vốn tại thời điểm bán

Không phải tối ưu hiệu năng, mà là **toàn vẹn báo cáo**: nếu tính lợi nhuận gộp bằng cách lấy `avg_cost` hiện tại của `stock_on_hand` cho 1 giao dịch bán đã xảy ra từ lâu, kết quả sẽ sai vì `avg_cost` đã thay đổi qua nhiều lần nhập hàng sau đó. Snapshot giá vốn tại đúng thời điểm bán đảm bảo báo cáo lợi nhuận gộp lịch sử luôn chính xác, không bị "viết lại" theo thời gian.

### 2.7 `audit_log.before_data`/`after_data` (JSONB snapshot toàn bộ record)

Đây là bảng **ghi log thuần** (write-heavy, gần như không bao giờ cần query theo từng field cụ thể ngoài `entity_name`/`entity_id`/`created_at`) — chuẩn hoá chi tiết từng field thay đổi sẽ tốn chi phí thiết kế/ghi không tương xứng với giá trị sử dụng. JSONB snapshot toàn bộ record trước/sau là lựa chọn hợp lý riêng cho loại bảng này.

### 2.8 Nhóm bảng Analytics (`fact_sales`, `fact_stock_movement`, `dim_date`...) — denormalize hoàn toàn theo thiết kế

Khác với các mục trên (denormalize *một phần*, có kiểm soát), nhóm bảng này **được phép denormalize hoàn toàn theo đúng bản chất star-schema** của data warehouse — đây không phải ngoại lệ cần giải thích riêng lẻ, mà là **mục đích thiết kế** của toàn bộ nhóm bảng: tối ưu tuyệt đối cho truy vấn báo cáo (đọc), chấp nhận trùng lặp dữ liệu, đồng bộ qua ETL định kỳ, đặt tại Read Replica riêng biệt (không ảnh hưởng OLTP) — đúng theo kiến trúc đã thiết kế ở Phase 4.

---

## PHẦN 3 — BẢNG TỔNG HỢP: BẢNG NÀO CHUẨN 3NF THUẦN, BẢNG NÀO DENORMALIZE

| Bảng | Trạng thái | Lý do (nếu denormalize) |
|---|---|---|
| `branch`, `user_account`, `role`, `permission`, `role_permission`, `user_branch_role` | 3NF thuần | — |
| `category`, `unit_of_measure`, `unit_conversion` | 3NF thuần | — |
| `attribute_definition`, `category_attribute`, `product_attribute_value` | 3NF thuần | — (đây là "nguồn sự thật" cho mục 2.1) |
| `product` | Denormalize 1 cột | `attributes_cache` — mục 2.1 |
| `price_list` | 3NF thuần | — |
| `customer` | 3NF thuần | — |
| `receivable_debt`, `payable_debt` | Denormalize | Số dư tổng hợp — mục 2.4 |
| `customer_product_price` | Denormalize | Giá gần nhất — mục 2.5 |
| `supplier`, `supplier_purchase_order`, `supplier_purchase_order_line` | 3NF thuần | — |
| `stock_on_hand` | Denormalize | Running balance — mục 2.3 |
| `inbound_receipt(_line)`, `outbound_receipt(_line)`, `stock_transfer(_line)` | 3NF thuần | — |
| `sales_invoice` | Denormalize 3 cột | `previous_debt`/`total_amount`/`remaining_debt` — mục 2.2 |
| `sales_invoice_line` | Denormalize 1 cột | `unit_cost_snapshot` — mục 2.6 |
| `customer_order(_line)`, `goods_return(_line)` | 3NF thuần | — |
| `visualizer_room(_zone)`, `visualizer_session` | 3NF thuần | `polygon_json` là dữ liệu hình học tự nhiên dạng cấu trúc, không phải denormalize theo nghĩa trùng lặp dữ liệu |
| `audit_log` | Denormalize | JSONB snapshot — mục 2.7 |
| Nhóm Analytics (`fact_*`, `dim_*`) | Denormalize hoàn toàn theo thiết kế | mục 2.8 |

**Tỷ lệ:** trong tổng số ~30 bảng OLTP (chưa tính nhóm Analytics), chỉ **7 bảng** có denormalize — đúng tinh thần "chuẩn NF trước, tối ưu sau, chỉ ở đúng chỗ cần" mà bạn yêu cầu, không denormalize tràn lan.

---

## PHẦN 4 — GHI CHÚ INDEX PHỤC VỤ HIỆU NĂNG (không phải denormalize, nhưng liên quan tối ưu)

Ngoài PK/FK index tự động, đã thêm các index có chủ đích cho hot path:

- `ix_product_name_trgm` (GIN + `pg_trgm`): phục vụ tìm kiếm sản phẩm gần đúng (fuzzy search) trên trang public — cần `CREATE EXTENSION pg_trgm;` trước khi chạy migration này.
- `ix_product_attributes_cache` (GIN trên JSONB): phục vụ lọc sản phẩm theo thuộc tính động (VD lọc theo màu sắc/kích thước) mà không cần JOIN ngược lại `product_attribute_value`.
- `ix_price_list_lookup`, `ix_invoice_customer`, `ix_customer_order_line_lookup`: index composite theo đúng thứ tự cột dùng trong `WHERE`/`ORDER BY` của các query hot path đã thiết kế ở tài liệu nghiệp vụ trước.

---

## VIỆC CẦN LÀM TIẾP THEO (gợi ý, không bắt buộc ngay)

1. Viết migration script Flyway/Liquibase từ file `schema-database-v1.sql` này (chia nhỏ theo từng Sprint đã thiết kế ở Kế hoạch triển khai v2 — Sprint 1.1 tạo `branch`/`identity`, Sprint 1.2 tạo `catalog`, Sprint 2.1 tạo `inventory`, v.v. thay vì chạy 1 file lớn duy nhất).
2. Bổ sung `CREATE EXTENSION pg_trgm;` vào migration đầu tiên nếu dùng tìm kiếm fuzzy.
3. Viết trigger hoặc application-level hook đảm bảo `product.attributes_cache` luôn đồng bộ đúng với `product_attribute_value` (mục 2.1) — đây là điểm rủi ro kỹ thuật cần review kỹ nhất trong toàn bộ các điểm denormalize.
