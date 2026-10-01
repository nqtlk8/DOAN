# Database Replication Architecture (v7)

Cập nhật: 2026-10-01 — gộp Flyway, bỏ seed, copy master data từ HQ (`copy_data = true`), khóa ghi master data ở DB chi nhánh, thêm `receivable_debt_movement`, script bật/tắt chi nhánh.
Quy trình vận hành từng bước: `REPLICATION_RUNBOOK.md`. Danh sách bảng: `scripts/replication-tables.conf`.

## 1. Mục tiêu và kiến trúc

PostgreSQL **logical replication hai chiều** giữa HQ và từng chi nhánh, theo nguyên tắc:

- **Single-writer**: mỗi bảng chỉ có một nơi được ghi (owner). Owner của từng bảng: `DATA_OWNERSHIP_MATRIX.md`.
- **HQ**: owner của master data (chi nhánh, tài khoản, danh mục, sản phẩm, nhà cung cấp, bảng giá, khách hàng, ngưỡng cảnh báo).
- **Chi nhánh**: owner của dữ liệu giao dịch phát sinh tại chi nhánh (hóa đơn, trả hàng, phiếu nhập, sổ kho, lô giá vốn, tồn, công nợ).

```text
                     pub_hq_master  (MASTER_TABLES)
[ HQ ] ===============================================> [ Chi nhánh N ]   sub_<n>_from_hq
[ HQ ] <=============================================== [ Chi nhánh N ]   pub_<n>_to_hq
 sub_hq_from_<n>     TRANSACTION_TABLES + SNAPSHOT_TABLES WHERE (branch_id = N)
```

## 2. Publication và subscription

| Đối tượng | Nằm ở | Nội dung |
|---|---|---|
| `pub_hq_master` | HQ | `MASTER_TABLES`. Một publication dùng chung cho mọi chi nhánh; mỗi chi nhánh có slot riêng. |
| `sub_<n>_from_hq` | Chi nhánh | Subscribe `pub_hq_master`, `copy_data = true` |
| `pub_<n>_to_hq` | Chi nhánh | `TRANSACTION_TABLES` + `SNAPSHOT_TABLES` có row filter `WHERE (branch_id = <id>)` |
| `sub_hq_from_<n>` | HQ | Subscribe `pub_<n>_to_hq`, `copy_data = true` |

Danh sách bảng nằm ở **một chỗ duy nhất** là `scripts/replication-tables.conf`:

- `MASTER_TABLES`: `branch role permission role_permission user_account user_branch_role category product supplier price_list customer inventory_alert_config dim_date`
- `TRANSACTION_TABLES`: `sales_invoice sales_invoice_line goods_return goods_return_line inbound_receipt inbound_receipt_line stock_movement cost_layer receivable_debt_movement`
- `SNAPSHOT_TABLES` (lọc `branch_id`): `stock_on_hand receivable_debt`

`setup-replication.sh` chạy lại được nhiều lần: publication đã có thì `ALTER PUBLICATION ... SET TABLE` theo file cấu hình, subscription đã có thì `REFRESH PUBLICATION WITH (copy_data = true)` (chỉ copy bảng mới thêm).

### 2.1. Vì sao `copy_data = true` cho master data

Từ khi gộp Flyway (`db/migration` V1..V8), migration **chỉ tạo schema**, không seed. Master data của chi nhánh đến 100% từ HQ qua lần copy đầu tiên. Dữ liệu hệ thống bắt buộc (2 chi nhánh, 2 vai trò, 3 tài khoản, cây danh mục sản phẩm 2 cấp) nằm trong `db/migration-hq/R__reference_data.sql` và **chỉ chạy ở HQ**. Nếu chi nhánh cũng tự chèn cùng dữ liệu, lần copy sẽ lỗi trùng khóa. Vì vậy `setup-replication.sh` từ chối tạo subscription khi bảng master ở chi nhánh đã có dữ liệu, trừ khi chạy với `--resync-master`.

Cách cũ (trước 2026-10-01) seed giống nhau ở cả hai phía rồi dùng `copy_data = false`. Cách đó có hai hệ quả: dữ liệu HQ tạo trước thời điểm subscribe không bao giờ xuống chi nhánh, và DB chi nhánh nào cũng chứa giao dịch seed của TP1.

### 2.2. Row filter cho bảng snapshot

`stock_on_hand` và `receivable_debt` lưu số dư theo `(…, branch_id)`. Row filter `WHERE (branch_id = N)` bảo đảm chi nhánh không bao giờ đẩy dòng của chi nhánh khác lên HQ, kể cả khi DB chi nhánh có dữ liệu lạc. PostgreSQL chỉ cho UPDATE/DELETE đi qua publication có row filter khi cột trong filter thuộc REPLICA IDENTITY, nên `V8__replication_support.sql` đặt `REPLICA IDENTITY USING INDEX` trên unique key `(product_id|customer_id, branch_id)`.

## 3. Bảo vệ single-writer ở 3 lớp

| Lớp | Cơ chế |
|---|---|
| Gateway | `nginx-branch-*.conf`: GET master data → app chi nhánh; POST/PUT/DELETE master data, đăng nhập → HQ |
| Ứng dụng | Controller ghi master chỉ bật ở HQ (`@ConditionalOnExpression instance.role == HQ`); controller giao dịch chỉ bật ở chi nhánh |
| Database | App kết nối bằng role `erp_app` (không phải superuser). Ở chi nhánh, `db/migration-branch/R__branch_db_security.sql` thu quyền ghi trên mọi bảng rồi chỉ cấp lại cho bảng do chi nhánh sở hữu ("mặc định chỉ đọc"). Apply worker của subscription chạy bằng superuser nên vẫn ghi được master data từ HQ. |

Trước 2026-10-01, app và Flyway dùng chung `erp_user`, là superuser (`POSTGRES_USER`), nên lệnh REVOKE trong `V9__branch_db_security.sql` cũ không có tác dụng.

## 4. Role database

Tạo bởi `docker/postgres/init/01-roles.sh` khi volume Postgres được tạo mới (giống nhau trên HQ và mọi chi nhánh):

| Role | Dùng cho | Quyền |
|---|---|---|
| `erp_user` (`POSTGRES_USER`) | Flyway, script replication, chủ subscription | superuser, chủ sở hữu bảng |
| `erp_app` (`APP_DB_USER`) | Spring Boot | DML qua `ALTER DEFAULT PRIVILEGES`; chi nhánh bị thu hẹp như mục 3 |
| `erp_repl` (`REPL_DB_USER`) | Chuỗi kết nối của subscription | `REPLICATION`, `SELECT` mọi bảng |

## 5. Bật/tắt chi nhánh

HQ có subscription kéo dữ liệu từ từng chi nhánh. Nếu chỉ dừng container chi nhánh, subscription đó vẫn bật và HQ log lỗi mỗi 5 giây:

```
ERROR: could not connect to the publisher: could not translate host name "branch-tp2-db" to address
```

Docker chỉ phân giải tên service đang chạy. Dùng `scripts/branch.sh off tp2`: tắt subscription ở HQ trước, rồi mới dừng container. `scripts/branch.sh on tp2` làm ngược lại.

Trong lúc chi nhánh tắt, slot `sub_<n>_from_hq` trên HQ giữ lại WAL để khi bật lại không mất master data. Lượng WAL bị giới hạn bởi `max_slot_wal_keep_size=2GB`. Vượt ngưỡng thì slot bị vô hiệu (`wal_status = lost`) và phải thiết lập lại chi nhánh: `remove-branch.sh` rồi `setup-replication.sh --resync-master`.

## 6. Cấu hình PostgreSQL (docker-compose.yml)

| Tham số | HQ | Chi nhánh | Lý do |
|---|---|---|---|
| `wal_level` | logical | logical | bắt buộc cho logical replication (cả hai phía đều là publisher) |
| `max_replication_slots`, `max_wal_senders` | 16 | mặc định (10) | HQ có 1 slot + 1 walsender cho mỗi chi nhánh |
| `max_logical_replication_workers` | 8 | mặc định (4) | HQ có 1 apply worker cho mỗi chi nhánh, cộng worker copy ban đầu |
| `max_worker_processes` | 16 | mặc định | phải ≥ số worker replication + worker khác |
| `max_slot_wal_keep_size` | 2GB | 1GB | chặn WAL phình vô hạn khi đầu bên kia tắt lâu |

## 7. Thay đổi schema (DDL không được replicate)

- Mọi migration `V__` nằm trong `db/migration`, chạy giống hệt ở HQ và mọi chi nhánh. `scripts/check-schema-version.sh <n>` so **toàn bộ danh sách** version, không chỉ version cuối.
- File riêng cho HQ hoặc chi nhánh chỉ được là repeatable (`R__`), để chuỗi version luôn giống nhau.
- Thêm cột vào bảng đang replicate thì phía **nhận** phải có cột trước: bảng HQ → chi nhánh thì migrate chi nhánh trước; bảng chi nhánh → HQ thì migrate HQ trước. Trong môi trường docker-compose, cách đơn giản nhất là dừng app, khởi động lại toàn bộ (Flyway chạy ở mọi instance) rồi `setup-replication.sh` cho từng chi nhánh nếu có bảng mới.

## 8. Tính nhất quán

Logical replication bất đồng bộ nên dữ liệu giữa HQ và chi nhánh nhất quán cuối (eventual consistency). Thường trễ dưới 1–2 giây. Ví dụ: nhà cung cấp admin vừa tạo ở HQ xuất hiện ở chi nhánh sau khoảng 1 giây.

## 9. Hạn chế đã biết

| Hạn chế | Ảnh hưởng | Hướng xử lý |
|---|---|---|
| HQ chưa bị chặn ghi bảng do chi nhánh sở hữu ở mức DB; mới chặn ở mức ứng dụng (controller theo instance, `opening-balance` đòi branchId) | Một thay đổi code sai ở HQ có thể ghi vào bảng giao dịch, rồi bị dữ liệu replicate từ chi nhánh ghi đè | Thêm `db/migration-hq/R__hq_db_security.sql` đối xứng với chi nhánh |
| Danh sách bảng nằm ở 3 nơi: `scripts/replication-tables.conf`, `R__branch_db_security.sql`, `DATA_OWNERSHIP_MATRIX.md` | Quên sửa một nơi: bảng không được replicate, hoặc app chi nhánh báo `permission denied` | Test tự động so khớp ba danh sách |
| Nginx chi nhánh khai báo upstream tĩnh | App chi nhánh không chạy thì nginx chi nhánh đó không khởi động được | Profile `tp2` bật/tắt nginx cùng app; xử lý tận gốc bằng `resolver` + upstream qua biến |
| WAL ở HQ giữ tối đa 2GB cho mỗi chi nhánh đang tắt | Vượt ngưỡng: slot bị vô hiệu, phải thiết lập lại chi nhánh | `remove-branch.sh` + `setup-replication.sh --resync-master` (Runbook mục 4) |
| Image Docker (backend, frontend, nginx) chưa được build/chạy trong lần kiểm thử 2026-10-01. Script và DB đã được kiểm với `docker-compose.yml` thật, backend chạy từ jar | Lần chạy đầu trên máy dev là lần đầu chạy toàn bộ qua nginx | Chạy `scripts/bootstrap.sh --seed` rồi `scripts/test-replication-e2e.sh tp1` |

