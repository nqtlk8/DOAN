# Database Replication Architecture (v6)

## 1. Mục tiêu và Kiến trúc

Hệ thống sử dụng **PostgreSQL Logical Replication** hai chiều để đồng bộ dữ liệu giữa HQ và các Branch. Kiến trúc được thiết kế theo nguyên tắc:

- **Single-Writer Invariant**: Mỗi bảng chỉ có một "Owner" duy nhất được phép ghi (thực thi trên cả Database Level và Application Level).
- **HQ (Headquarters)**: Master node, quản lý toàn bộ Master Data.
- **Branch (Chi nhánh)**: Transaction node, quản lý toàn bộ Transaction Data phát sinh tại chi nhánh đó.

Mô hình dữ liệu di chuyển:
```text
[ HQ Database ] ====== (Master Data) =======> [ Branch Database ]
[ HQ Database ] <=== (Transaction Data) ===== [ Branch Database ]
```

## 2. Publications & Subscriptions

Việc phân quyền replication được thực hiện rõ ràng thông qua 2 `PUBLICATION` chính:

### HQ tới Branch (Master Data)
HQ tạo `pub_hq_to_<branch_id>` bao gồm các bảng:
`branch, category, customer, dim_date, inventory_alert_config, permission, price_list, product, role, role_permission, supplier, user_account, user_branch_role`

Branch tạo `SUBSCRIPTION` tương ứng để nhận luồng dữ liệu này.

### Branch tới HQ (Transaction Data)
Branch tạo `pub_<branch_id>_to_hq` bao gồm các bảng:
`sales_invoice, sales_invoice_line, goods_return, goods_return_line, inbound_receipt, inbound_receipt_line, stock_movement, cost_layer`

HQ tạo `SUBSCRIPTION` tương ứng để nhận dữ liệu từ tất cả các nhánh.

## 3. Provisioning & Runbook

Hệ thống cung cấp các shell scripts chuẩn hóa trong thư mục `/scripts` để đảm bảo idempotent provisioning:

- `setup-replication.sh <branch_name> [copy_data]`: Cấu hình toàn bộ Role, Publication, Subscription 2 chiều.
- `check-schema-version.sh <branch_name>`: Kiểm tra tính đồng nhất của phiên bản Flyway (Bắt buộc HQ và Branch phải cùng version mới được phép kết nối).
- `test-replication-e2e.sh`: Script kiểm chứng E2E chéo (Tạo Product ở HQ -> Branch nhận, Tạo Invoice ở Branch -> HQ nhận).
- `add-branch.sh <branch_name>`: Wrapper script tự động hóa luồng thêm chi nhánh mới (Bật Container -> Init Schema -> Truncate Seed Master Data -> Bật Replication với copy_data=true -> Bật App).

**Lưu ý quan trọng (Bootstrapping):**
Khi tạo mới một nhánh (VD: TP3), Flyway sẽ tự động chạy `V2__seed_test_data.sql` tạo ra các bản ghi có sẵn. Do đó `add-branch.sh` sẽ tự động `TRUNCATE CASCADE` các bảng Master trước khi gọi `setup-replication.sh tp3 true`. Nếu không, cờ `copy_data=true` từ HQ sẽ gây lỗi trùng lặp khóa chính (Duplicate PK). Đối với các nhánh đã đồng bộ sẵn, ta dùng `copy_data=false` để tránh lặp dữ liệu.

## 4. Application Level Invariants

Để triệt để ngăn chặn rủi ro ghi đè dữ liệu (split-brain hoặc dual-writer):
- Các Controller ghi dữ liệu giao dịch tại Branch (như `SalesInvoiceController`, `GoodsReturnController`, `InboundReceiptController`) được gắn annotation `@ConditionalOnProperty(name = "instance.role", havingValue = "BRANCH")`.
- Tương tự, Master Data controller chỉ hoạt động ở HQ.
- Lớp bảo vệ này khiến ứng dụng trả về HTTP 404 (Not Found) nếu người dùng hoặc Client cố tình bắn API ghi giao dịch trực tiếp lên HQ (hoặc ngược lại).

## 5. Cấu hình PostgreSQL

Để Logical Replication hoạt động, các instances trong `docker-compose.yml` bắt buộc chạy với cờ:
```bash
wal_level=logical
max_replication_slots=10
max_wal_senders=10
```

## 6. Tính nhất quán (Consistency)

Do đặc thù của Logical Replication, độ trễ (replication lag) là không thể tránh khỏi. Dữ liệu giữa HQ và Branch tuân theo mô hình **Eventual Consistency** (Nhất quán cuối). Ứng dụng Frontend và Backend cần được thiết kế (UX/UI) để chấp nhận độ trễ này (ví dụ: tạo sản phẩm mới ở HQ có thể mất 1-2 giây mới xuất hiện trên thanh tìm kiếm của Branch).
