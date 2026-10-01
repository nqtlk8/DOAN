# PLAN — Replication, Docker Compose, Flyway (fix-replication)

Ngày lập: 2026-10-01 · Người lập: Claude · Phạm vi: `scripts/*replication*.sh`, `scripts/add-branch.sh`, `docker-compose.yml`, `nginx-branch-*.conf`, `db/migration*`, module `catalog/supplier`.

> **Trạng thái (2026-10-01): ĐÃ THỰC HIỆN RF-1 → RF-5.** Quyết định đã chốt: `README.md`. Kết quả nằm trong tài liệu chuẩn (`docs/architecture/DATABASE_REPLICATION.md`, ADR-14, `docs/testing/TEST_STRATEGY.md`). Điểm lệch chính so với kế hoạch: D-02 giữ tài khoản bằng `R__reference_data.sql` thay vì bootstrap admin từ biến môi trường; migration chỉ có V1..V8 (thêm V7 common, V8 replication).
>
> Tài liệu này gồm hai phần. Phần **hiện trạng** chỉ ghi những gì đã đọc được trong source, có dẫn bằng chứng. Phần **kế hoạch** là đề xuất và cần chốt các quyết định ở mục 2 trước khi thực thi.
> Phần chưa xác minh được: Claude không truy cập được Docker trên máy, nên trạng thái DB đang chạy (subscription, slot, dữ liệu) được suy ra từ code và log người dùng gửi. Lệnh để xác minh nằm ở mục 6.

---

## 0. Tóm tắt

| Nhóm | Kết luận chính |
|---|---|
| Supplier | Đồng bộ HQ → Branch **đúng**. Staff không thấy supplier là do API đọc ở chi nhánh lọc `WHERE branch_id = <chi nhánh>`, trong khi mọi supplier do HQ tạo có `branch_id = NULL`. |
| Replication | Script chạy được cho TP1/TP2, nhưng cả thiết kế dựa trên giả định **"seed giống nhau ở hai phía + `copy_data=false`"**. Bỏ seed thì chi nhánh sẽ trống master data. Ngoài ra còn thiếu bảng `receivable_debt_movement`, không đồng bộ lại khi danh sách bảng thay đổi, không có quy trình tạm tắt/gỡ chi nhánh. |
| Lỗi `branch-tp2-db` | HQ vẫn giữ subscription `sub_hq_from_tp2` đang bật, trong khi container `branch-tp2-db` không chạy. Docker DNS chỉ phân giải container đang chạy, nên HQ báo lỗi và thử lại liên tục. Vòng đời của compose và của replication đang không gắn với nhau. |
| Bảo mật DB | Mọi app và Flyway đều kết nối bằng `erp_user`, cũng là `POSTGRES_USER`, tức là **superuser**. Lệnh `REVOKE` trong `V9__branch_db_security.sql` vì vậy **không có tác dụng**: app chi nhánh vẫn ghi được bảng master. |
| Flyway | 15 file lẫn lộn schema, sửa schema, seed và vá dữ liệu. Thiếu V3–V8, V9 chỉ có ở chi nhánh. Seed giao dịch `branch_id=1` chạy trên **mọi** DB, kể cả TP2/TP3. |

---

## 1. Hiện trạng đã xác minh

### 1.1. Supplier (S)

| # | Vấn đề | Bằng chứng | Mức |
|---|---|---|---|
| S1 | Staff chi nhánh nhận danh sách supplier rỗng | `SupplierReadController.getSuppliers()`: `branchId != null → supplierRepository.findByBranchId(branchId)`. Supplier do HQ tạo và seed `SUP-001/002` đều có `branch_id = NULL`. GET `/api/v1/suppliers` ở cổng chi nhánh được nginx chuyển tới `branch_backend` (`map $request_method $master_data_backend`). | Critical |
| S2 | DTO cho phép gắn supplier vào một chi nhánh | `SupplierCreateDto.branchId`, `SupplierWriteService.createSupplier` gán `setBranchId(dto.getBranchId())`. Trái với `DATA_OWNERSHIP_MATRIX.md` (supplier: owner HQ, dùng chung). | Medium |
| S3 | Staff mở chi tiết supplier dùng chung bị 403 | `getSupplier(id)`: `branchId != null && !branchId.equals(supplier.getBranchId())` → với `branch_id = NULL` luôn lỗi 403. | Medium |
| S4 | Supplier đã xóa mềm vẫn hiện cho staff | `getSuppliers()` không lọc `is_active`. | Low |
| S5 | Test đang khóa hành vi sai | `SupplierBranchScopeTest.staffSeesOnlyOwnBranchSuppliers`. | Medium |
| S6 | Bảng `supplier` ở chi nhánh không bị khóa ghi | `migration-branch/V9__branch_db_security.sql` chỉ REVOKE `branch, category, product, price_list, customer`. | Medium |

So sánh: `CustomerRepository` (crm) đã được sửa thành `:branchId IS NULL OR c.branchId IS NULL OR c.branchId = :branchId`, còn supplier chưa được sửa theo.

### 1.2. Script replication & hạ tầng (R)

| # | Vấn đề | Bằng chứng | Mức |
|---|---|---|---|
| R1 | Master data HQ → Branch mặc định **không copy ban đầu**. Chi nhánh có master data chỉ nhờ Flyway seed V2 chạy trên chính nó (cùng UUID/ID với HQ). | `setup-replication.sh`: `COPY_DATA=${2:-false}`. Log TP1 ngày 27/09 không có dòng `table synchronization worker ... "sub_tp1_from_hq"`. | High (chặn việc bỏ seed) |
| R2 | Publication/subscription chỉ **tạo khi chưa có**, không bao giờ cập nhật danh sách bảng. Thêm bảng mới vào `MASTER_TABLES`/`TRANSACTION_TABLES` sẽ không có tác dụng với chi nhánh đã setup, và không có `REFRESH PUBLICATION`. | Các khối `if [ "$PUB_EXISTS" != "1" ]` / `SUB_EXISTS`. | High |
| R3 | `receivable_debt_movement` (sổ cái công nợ, V15) **không được replicate** lên HQ. Ma trận ownership cũng không nhắc tới bảng này. | `TRANSACTION_TABLES` thiếu bảng này. Bảng được ghi ở chi nhánh (`SalesInvoiceService`, `GoodsReturnService`). `ReceivableDebtController` không có `@ConditionalOnExpression` nên cũng chạy ở HQ, và đọc HQ sẽ ra dữ liệu rỗng. | High |
| R4 | Thiếu chế độ tạm tắt/gỡ chi nhánh. Khi chi nhánh tắt: (a) subscription `sub_hq_from_<b>` ở HQ lặp lỗi; (b) slot `sub_<b>_from_hq` trên HQ giữ WAL không giới hạn, ổ đĩa HQ phình dần. | Không có script disable/drop. `hq-db` không đặt `max_slot_wal_keep_size`. | High |
| R5 | Quyền đọc cho `erp_repl` chỉ áp lên **bảng đã tồn tại**. Bảng tạo bởi migration sau này sẽ lỗi `permission denied` khi initial copy. | `GRANT SELECT ON ALL TABLES IN SCHEMA public TO erp_repl`, không có `ALTER DEFAULT PRIVILEGES`. | Medium |
| R6 | Kiểm tra sau setup có thể báo sai: `sleep 5` cố định, và chỉ đọc `LIMIT 1` dòng `pg_subscription_rel`. | Bước 6–7 của `setup-replication.sh`. | Medium |
| R7 | Phụ thuộc tên container `code-<svc>-1`, tức tên thư mục làm project name. Đổi thư mục hoặc `-p` là script hỏng. | `HQ_DOCKER="code-hq-db-1"` ở mọi script. | Low |
| R8 | `BRANCH_NAME=$1` dưới `set -u`: thiếu tham số sẽ crash "unbound variable" thay vì in usage. | Dòng 4. | Low |
| R9 | Mỗi chi nhánh có một publication riêng ở HQ (`pub_hq_to_<b>`) với nội dung giống hệt nhau, thừa. | Bước 4. | Low |
| R10 | Cấu hình worker HQ: HQ là subscriber của N chi nhánh (1 apply worker mỗi chi nhánh, cộng tablesync worker), nhưng `max_logical_replication_workers` để mặc định 4. Từ 3 chi nhánh trở lên là sát giới hạn khi initial sync. | `docker-compose.yml` hq-db `command`. | Medium |
| R11 | DDL không được replicate. `check-schema-version.sh` chỉ so **version cuối**, và chưa có quy tắc thứ tự deploy migration khi thêm cột vào bảng đang replicate. | `check-schema-version.sh`. | Medium |
| R12 | `add-branch.sh` không chạy được: gọi `mvn flyway:migrate` nhưng `pom.xml` không khai báo `flyway-maven-plugin`; port `5435` cứng; không chạy `migration-branch`; `branch-tp3-app` không có trong compose. | `add-branch.sh`, `pom.xml`. | Medium |
| R13 | **Bảo mật DB chi nhánh vô hiệu**: `erp_user` là superuser (biến `POSTGRES_USER` của image postgres), app và Flyway đều dùng user này. Superuser bỏ qua mọi kiểm tra quyền, nên `REVOKE` trong V9 vô tác dụng. | `docker-compose.yml` (`SPRING_DATASOURCE_USERNAME: erp_user`), `V9__branch_db_security.sql`. | High |
| R14 | Hệ quả của R13: guard trong V20 `has_table_privilege(current_user,'customer','UPDATE')` luôn true, nên chi nhánh vẫn chạy `UPDATE customer` lên bảng do HQ sở hữu (vi phạm single-writer, hiện chưa gây hại vì cùng giá trị). | `V20__branch_portal_url_and_shared_customers.sql`. | Medium |

Những phần **đang hợp lý**, giữ lại:
- Hướng dữ liệu đúng ma trận ownership (master HQ → Branch, giao dịch Branch → HQ, không có bảng nào bị ghi ở cả hai phía).
- Kiểm tra Flyway version trước khi tạo subscription.
- Snapshot `stock_on_hand`/`receivable_debt` dùng row filter `branch_id = N` cùng `REPLICA IDENTITY USING INDEX` (V21), có IT kiểm chứng (`SnapshotReplicationPostgresIT`).
- Nginx: GET master đi về branch, ghi master đi về HQ.

### 1.3. docker-compose (C)

| # | Vấn đề | Bằng chứng | Mức |
|---|---|---|---|
| C1 | **Lỗi người dùng báo** trong `code-hq-db-1`: `could not connect to the publisher: could not translate host name "branch-tp2-db"`. HQ có subscription `sub_hq_from_tp2` (`host=branch-tp2-db`) đang bật, nhưng container `branch-tp2-db` không chạy (Docker DNS chỉ phân giải container đang chạy trong network). HQ thử lại mỗi `wal_retrieve_retry_interval` (5 giây). Hệ quả: giao dịch TP2 không lên HQ, và slot `sub_tp2_from_hq` trên HQ giữ WAL (R4). Nguyên nhân gốc: không có bước nào gắn việc bật/tắt service với `ALTER SUBSCRIPTION ... ENABLE/DISABLE`. | Log người dùng 2026-10-01 02:06 UTC. `setup-replication.sh` bước 5. | High |
| C2 | Nginx của chi nhánh khai báo `upstream` tĩnh. Nếu `branch-tpN-app` không chạy thì nginx **không khởi động được** (`host not found in upstream`). | `nginx-branch-tp2.conf` `upstream branch_backend { server branch-tp2-app:8080; }`. | Medium |
| C3 | `branch-tp3-db` có volume nhưng không có app/nginx, chỉ là service mồ côi. | `docker-compose.yml`. | Low |
| C4 | Superuser cho app (xem R13). Mật khẩu viết cứng trong compose và script. | `erp_password`, `erp_repl_password`. | High (môi trường thật) |
| C5 | Cổng lộ ra host: DB 5432–5435, Redis 6379, `hq-app` 8080, `branch-tp1-app` 8081. Gọi 8081 trực tiếp sẽ bỏ qua whitelist IP của nginx. | `ports:`. | Medium |
| C6 | Build trùng: backend build 3 lần (hq, tp1, tp2), frontend build 3 lần cùng tag `code-erp-frontend`. | `build:` lặp lại. | Low |
| C7 | Thiếu `restart:`. App không có healthcheck (pom không có actuator). `depends_on: hq-app` không chờ app sẵn sàng. | `docker-compose.yml`, `pom.xml`. | Low |
| C8 | Replication không nằm trong vòng đời compose: sau `down -v` hoặc tạo lại volume phải chạy script tay, không có gì cảnh báo nếu quên. | Không có service khởi tạo hay hướng dẫn. | Medium |
| C9 | Comment lỗi thời "No need for tp2-app right now" ngay trên định nghĩa `branch-tp2-app`. | `docker-compose.yml`. | Low |

### 1.4. Flyway (F)

Phân loại 15 file hiện có:

| File | Nội dung | Loại |
|---|---|---|
| V1 `init_schema` | `pg_dump --schema-only` của schema do Hibernate sinh: 26 bảng, cột xếp theo kiểu dữ liệu, FK tên tự sinh (`fk1mtsbur…`), `set_config('search_path','')`. Có cả bảng/cột sau này bị xóa (`inventory_alert_log`, `stock_on_hand.avg_cost`, `customer.customer_type`). | Schema |
| V2 `seed_test_data` | branch, role, user (kèm hash mật khẩu), user_branch_role, category, product, price_list, customer, supplier, stock_on_hand | **Seed** |
| *(V3–V8)* | không tồn tại | — |
| V9 *(chỉ ở `migration-branch`)* | REVOKE DML 5 bảng master | Bảo mật (chỉ chi nhánh) |
| V10 `reset_test_passwords` | UPDATE mật khẩu | **Vá seed** |
| V11 `inventory_refactor` | DROP bảng legacy (không có trong V1), DROP cột, CREATE `stock_movement`, `cost_layer`, ADD `cost_basis` | Sửa schema |
| V12 `uuid_inventory` | Đổi PK BIGINT → UUID (`stock_movement`, `cost_layer`) kèm backfill | Sửa schema + dữ liệu |
| V13 `seed_transactions` | 280 dòng: phiếu nhập, hóa đơn, trả hàng, công nợ, dim_date, fact_* cho TP1 | **Seed** |
| V14 | ADD `sales_invoice.advance_payment` | Sửa schema |
| V15 | CREATE `receivable_debt_movement` | Schema |
| V16 | ADD `customer.branch_id`, `UPDATE customer SET branch_id = 1` | Schema + dữ liệu |
| V17 | Backfill số dư đầu kỳ | Dữ liệu |
| V18 | ADD `balance_before`, `idempotency_key` + chuyển đổi dữ liệu | Schema + dữ liệu |
| V19 | ADD `user_account.public_id` + backfill | Schema + dữ liệu |
| V20 | UPDATE `branch.internal_url`, `customer.branch_id = NULL` | **Vá seed** |
| V21 | Làm sạch `inventory_alert_config` + ràng buộc + REPLICA IDENTITY + DROP `inventory_alert_log` + index | Trộn lẫn |

| # | Vấn đề | Mức |
|---|---|---|
| F1 | Schema, sửa schema, seed và vá dữ liệu nằm lẫn trong cùng chuỗi version. Muốn hiểu schema cuối cùng phải "chạy trong đầu" 15 file. | High |
| F2 | Seed (V2, V13) chạy trên **mọi** instance: HQ, TP1, TP2, TP3. DB TP2 vì thế chứa giao dịch `branch_id=1` trùng UUID với HQ, rủi ro đã ghi ở P7 của fix-dashboard. | High |
| F3 | Đánh số rời rạc (V1, V2, V9, V10…), V9 chỉ có ở chi nhánh. `check-schema-version.sh` so version cuối nên không phát hiện lệch giữa các version ở giữa. | Medium |
| F4 | V10, V16, V17 có BOM UTF-8 (`EF BB BF`). `FlywayEncodingTest` hard-code tên V16/V17. | Low |
| F5 | Test phụ thuộc seed: `StockAlertApiPostgresIT` (`SP-G001`), `AnalyticsDataAdapterPostgresIT` (seed V2/V13), `SnapshotReplicationPostgresIT` (`KH-002`, "seed `branch_id=1`"). | Medium |
| F6 | Gộp version làm đổi checksum, nên DB hiện có sẽ fail `validate`. Phải tạo lại volume, hoặc `baseline`/`repair`. | Ràng buộc kỹ thuật |
| F7 | Seed V2 đang cung cấp dữ liệu **bắt buộc** (role `STAFF`/`ADMIN`, tài khoản `admin`). Bỏ seed thì phải có cách khác để tạo dữ liệu này, nếu không sẽ không đăng nhập được. | High |

---

## 2. Quyết định cần chốt

| Mã | Câu hỏi | Đề xuất |
|---|---|---|
| D-01 | Có chấp nhận **xóa volume DB** (HQ, TP1, TP2, TP3) để chạy chuỗi Flyway mới từ đầu không? | **Có.** Đây là đồ án, dữ liệu hiện tại chỉ là seed/test. Phương án còn lại (`flyway baseline` trên DB cũ) phức tạp và dễ lệch schema. |
| D-02 | Tài khoản admin đầu tiên tạo bằng cách nào? | **Bootstrap trong app HQ** từ biến môi trường `ADMIN_USERNAME`/`ADMIN_PASSWORD`: chỉ tạo khi chưa có user ADMIN nào. Không lưu hash mật khẩu trong repo. |
| D-03 | Role `STAFF`/`ADMIN` (dữ liệu hệ thống bắt buộc) đặt ở đâu? | Migration **chỉ chạy ở HQ**, dạng repeatable `db/migration-hq/R__reference_data.sql`, rồi replicate xuống chi nhánh. Lý do: nếu chèn ở cả hai phía, initial copy sẽ lỗi trùng khóa. |
| D-04 | Supplier là dữ liệu dùng chung toàn hệ thống, hay vẫn cần "NCC riêng của chi nhánh"? | **Dùng chung**: bỏ cột `supplier.branch_id` khi gộp Flyway. |
| D-05 | Có cần bộ dữ liệu demo cho buổi bảo vệ/báo cáo không? | Nếu có: script `scripts/seed-demo.sql` **ngoài Flyway**, chạy tay **chỉ trên HQ** rồi để replication đưa xuống (đúng luồng thật). |
| D-06 | TP2/TP3 chạy thường trực hay chỉ bật khi cần? | Dùng **compose profiles** (`--profile tp2`), kèm script `pause-branch.sh`/`resume-branch.sh` để tắt/bật subscription tương ứng. |
| D-07 | Có replicate `receivable_debt_movement` lên HQ không? | **Có** (Branch → HQ, không cần row filter vì chỉ chi nhánh đó ghi). |

---

## 3. Thiết kế đích

### 3.1. Flyway

```
db/migration/                      # chạy trên HQ VÀ mọi Branch, giống hệt nhau
  V1__identity.sql                 # branch, role, permission, role_permission, user_account(+public_id), user_branch_role
  V2__catalog.sql                  # category, product, supplier (không branch_id), price_list
  V3__crm.sql                      # customer(+branch_id), customer_product_price, receivable_debt, receivable_debt_movement(+balance_before, idempotency_key)
  V4__inventory.sql                # stock_on_hand (không avg_cost), stock_movement(UUID), cost_layer(UUID), inbound_receipt(+line), inventory_alert_config(+ràng buộc V21)
  V5__sales.sql                    # sales_invoice(+advance_payment), sales_invoice_line(+cost_basis), goods_return(+line), idempotency_record
  V6__analytics.sql                # dim_date, fact_sales, fact_stock_movement
  V7__replication_support.sql      # REPLICA IDENTITY stock_on_hand, receivable_debt (+ index hỗ trợ)
db/migration-hq/
  R__reference_data.sql            # role STAFF/ADMIN (ON CONFLICT DO NOTHING) — chỉ HQ
db/migration-branch/
  R__branch_db_security.sql        # REVOKE DML toàn bộ bảng master khỏi role app (erp_app) — chỉ Branch
```

Quy tắc từ nay về sau (ghi vào `docs/development/`):
1. Mỗi thay đổi là một file `V<n>__<module>_<hành_động>_<đối_tượng>.sql`, đánh số liên tục, **chung** cho HQ và Branch.
2. **Không trộn** DDL và cập nhật dữ liệu trong một file. Không bao giờ đưa seed demo vào `db/migration*`.
3. File riêng cho HQ hoặc Branch chỉ được là **repeatable** (`R__`), để version của chuỗi chung luôn giống nhau ở mọi instance và `check-schema-version.sh` vẫn hợp lệ.
4. FK/index đặt tên dễ đọc (`fk_<bảng>_<cột>`, `ix_…`, `uk_…`). Không dùng tên do Hibernate sinh.
5. File lưu UTF-8 **không BOM**, xuống dòng LF.

Cách làm và xác minh: dựng 2 Postgres 15 (Testcontainers hoặc compose tạm). Một DB chạy chuỗi **cũ** V1..V21, sau đó `TRUNCATE` toàn bộ dữ liệu. DB kia chạy chuỗi **mới**. So sánh `information_schema.columns` (tên, kiểu, nullable, default), unique/check constraint và index. Ngoài tên ràng buộc, mọi thứ phải khớp 100%. Sau đó chạy app với `ddl-auto: validate` và chạy toàn bộ IT.

### 3.2. Replication

| Thành phần | Hiện tại | Đích |
|---|---|---|
| Danh sách bảng | Viết cứng trong `setup-replication.sh` | Một file `scripts/replication-tables.conf` dùng chung cho script, IT và tài liệu |
| Publication HQ | `pub_hq_to_<b>` mỗi chi nhánh | Một `pub_hq_master` dùng chung |
| Sub chi nhánh ← HQ | `copy_data=false` | `copy_data=true` (chi nhánh không còn seed, master trống) |
| Publication chi nhánh | 8 bảng giao dịch + snapshot | Thêm `receivable_debt_movement`; snapshot giữ row filter |
| Sub HQ ← chi nhánh | `copy_data=false` | `copy_data=true` (an toàn vì chi nhánh mới không có dữ liệu cũ, và không mất giao dịch phát sinh trước khi subscribe) |
| Chạy lại script | Bỏ qua nếu đã có | `ALTER PUBLICATION … SET TABLE …` rồi `ALTER SUBSCRIPTION … REFRESH PUBLICATION` (idempotent thật sự) |
| Quyền `erp_repl` | `GRANT SELECT ON ALL TABLES` | Thêm `ALTER DEFAULT PRIVILEGES … GRANT SELECT ON TABLES TO erp_repl` |
| Kiểm tra | `sleep 5`, `LIMIT 1` | Thăm dò tới khi **mọi** dòng `pg_subscription_rel` = `r`, có timeout, in bảng trạng thái |
| Gọi DB | `docker exec code-…-1` | `docker compose exec -T <service>` |
| Tắt/gỡ chi nhánh | Không có | `pause-branch.sh` (DISABLE sub ở HQ), `resume-branch.sh`, `remove-branch.sh` (DROP sub ở HQ + drop slot `sub_<b>_from_hq` trên HQ) |
| HQ postgres | `max_replication_slots=10`, `max_wal_senders=10` | thêm `max_logical_replication_workers=8`, `max_worker_processes=16`, `max_slot_wal_keep_size=2GB` |
| Thứ tự deploy migration | Không quy định | Dev: dừng app → migrate mọi DB → bật lại. Ghi rõ: thêm cột vào bảng HQ → Branch thì migrate **Branch trước**; bảng Branch → HQ thì migrate **HQ trước**. |

### 3.3. docker-compose

- Script init DB (`/docker-entrypoint-initdb.d/01-roles.sql`) tạo 3 role: `erp_owner` (Flyway, chủ sở hữu bảng), `erp_app` (app, **không** superuser), `erp_repl`. App dùng `erp_app`, Flyway dùng `erp_owner`. Subscription vẫn do superuser tạo (cần để apply).
- Mật khẩu chuyển sang file `.env` (thêm `.env.example`, đưa `.env` vào `.gitignore`).
- Backend build **một lần** (`image: erp-backend:local`), các service khác dùng lại image. Frontend tương tự.
- TP2/TP3 đặt trong `profiles: [tp2]`, `[tp3]`. Bỏ `branch-tp3-db` mồ côi, hoặc bổ sung đủ app/nginx cho TP3.
- Nginx chi nhánh dùng `resolver 127.0.0.11` và upstream qua biến, để nginx vẫn khởi động khi backend chưa lên.
- Bỏ expose cổng app chi nhánh và DB ra host, hoặc bind `127.0.0.1:` khi cần debug.
- Thêm `restart: unless-stopped`.
- Thêm `scripts/bootstrap.sh`: `up` DB → `up` app (để Flyway chạy) → `setup-replication.sh` cho từng chi nhánh đang bật → in trạng thái.

---

## 4. Kế hoạch thực hiện

Thứ tự: RF-0 → RF-1 → RF-2 → RF-3 → RF-4 → RF-5. RF-1 độc lập, làm ngay được.

### RF-0 — Chẩn đoán & chốt quyết định (không sửa code)
- Chạy các lệnh ở mục 6, ghi kết quả vào sprint doc.
- Người dùng chốt D-01 … D-07.
- **Xong khi:** biết chắc trạng thái container TP2, subscription, slot, WAL đang giữ; mọi quyết định đã chốt.

### RF-1 — Sửa hiển thị supplier (hotfix)
- `SupplierRepository`: thêm `findByIsActiveTrueOrderByNameAsc()` (nếu D-04 = dùng chung), hoặc query kiểu customer (`branch_id IS NULL OR branch_id = :b`).
- `SupplierReadController`: staff đọc danh sách dùng chung đang hoạt động; bỏ kiểm tra 403 theo branch ở `getSupplier`.
- `SupplierCreateDto`/`SupplierWriteService`: bỏ `branchId` (hoặc ép `null`).
- Sửa `SupplierBranchScopeTest` → `staffSeesSharedActiveSuppliers`, `staffCanOpenSharedSupplier`, `inactiveSupplierHiddenFromStaff`.
- **Xong khi:** đăng nhập `staff_tp1` (cổng 81) thấy supplier admin vừa tạo; test xanh.

### RF-2 — Gộp Flyway
- Tạo `V1..V7` mới, `migration-hq/R__reference_data.sql`, `migration-branch/R__branch_db_security.sql` theo mục 3.1. Đưa 15 file cũ vào `docs/to_remove/flyway-legacy/` để tra cứu.
- `application-hq.yml`: `locations: classpath:db/migration,classpath:db/migration-hq`.
- Bỏ `supplier.branch_id` (D-04): sửa entity, DTO, mapper.
- Bootstrap admin (D-02): `AdminBootstrap` (`ApplicationRunner`, chỉ bật khi role HQ), đọc env, idempotent.
- Viết `FlywayConsolidationPostgresIT` so sánh schema cũ và mới như mục 3.1, sau đó xóa test này cùng chuỗi cũ khi đã ổn định.
- Sửa test phụ thuộc seed (F5): dùng `@Sql` fixture riêng trong `src/test/resources/fixtures/`. Cập nhật `FlywayEncodingTest` quét **mọi** file migration.
- **Xong khi:** `mvn verify` xanh (gồm `*IT`); DB mới chỉ có role và admin; `ddl-auto: validate` chạy qua ở cả profile hq và branch.

### RF-3 — Role DB & bảo mật
- `docker/postgres/init/01-roles.sql` (mount vào cả 4 DB): tạo `erp_owner`, `erp_app`, `erp_repl`, phân quyền kèm `ALTER DEFAULT PRIVILEGES`.
- Compose: `SPRING_DATASOURCE_USERNAME=erp_app`, `SPRING_FLYWAY_USER=erp_owner`; dùng `.env`.
- `R__branch_db_security.sql` REVOKE DML trên **toàn bộ** bảng master (gồm cả `supplier`, `role`, `permission`, `user_account`, `user_branch_role`, `inventory_alert_config`, `dim_date`) khỏi `erp_app`.
- Bỏ guard `has_table_privilege` (không còn cần sau khi gộp).
- **Xong khi:** từ app chi nhánh, `INSERT INTO supplier …` bị `permission denied`; replication vẫn apply bình thường.

### RF-4 — Viết lại script replication & compose (gồm lỗi `branch-tp2-db`)
- `scripts/replication-tables.conf`, `setup-replication.sh` (idempotent, `copy_data=true`, `pub_hq_master`, default privileges, kiểm tra đầy đủ), `pause-branch.sh`, `resume-branch.sh`, `remove-branch.sh`, `bootstrap.sh`. Viết lại `add-branch.sh` (bỏ `mvn flyway:migrate`, để Flyway chạy qua app container), cập nhật `check-schema-version.sh` (dùng `docker compose exec`, so cả danh sách version).
- Compose theo mục 3.3 (profiles, build một lần, tham số WAL/worker cho HQ, resolver nginx, bỏ cổng thừa).
- **Xong khi:** (1) `docker compose down -v` → `bootstrap.sh` → hệ thống chạy, đủ dữ liệu master ở mọi chi nhánh; (2) chạy **không** có profile tp2 thì log HQ không còn lỗi `branch-tp2-db`; (3) chạy lại `setup-replication.sh` lần hai không đổi gì và không lỗi.

### RF-5 — Kiểm thử E2E & tài liệu
- E2E: admin tạo supplier/product ở HQ → staff TP1 và TP2 thấy; staff TP1 bán hàng → HQ thấy `sales_invoice`, `receivable_debt_movement`, snapshot `stock_on_hand`; pause/resume TP2 không mất giao dịch.
- Cập nhật `DATA_OWNERSHIP_MATRIX.md` (thêm `receivable_debt_movement`, `customer_product_price`, `idempotency_record`), `DATABASE_REPLICATION.md`, `REPLICATION_RUNBOOK.md`, `RUNTIME_CONFIG.md`, `DATA_SCHEMA.md`, `SECURITY_MODEL.md`, `AI_CONTEXT.md`.
- Sprint doc theo `.agents/rules/Sprint-docs-rule.md`.

---

## 5. Rủi ro

| Rủi ro | Giảm thiểu |
|---|---|
| Schema sau khi gộp lệch với schema cũ, app fail `validate` hoặc lỗi runtime | Bước so sánh tự động ở RF-2 trước khi xóa chuỗi cũ |
| Mất dữ liệu khi xóa volume | Chốt D-01. Nếu cần giữ, `pg_dump --data-only` HQ trước để tham khảo |
| Initial copy lỗi trùng khóa | Không chèn dữ liệu ở cả hai phía; reference data chỉ ở HQ (D-03) |
| Không đăng nhập được sau khi bỏ seed | Bootstrap admin (D-02), kiểm tra ngay trong RF-2 |
| WAL HQ phình khi chi nhánh tắt lâu | `max_slot_wal_keep_size` và `pause/remove-branch.sh` |

---

## 6. Lệnh chẩn đoán (RF-0) — chạy trong PowerShell tại thư mục `code`

```powershell
# Trạng thái mọi container (kể cả đã dừng)
docker compose ps -a

# Subscription trên HQ: có sub_hq_from_tp2 và đang bật không?
docker exec code-hq-db-1 psql -U erp_user -d erp_hq -c "SELECT subname, subenabled, subconninfo FROM pg_subscription;"

# Slot trên HQ và lượng WAL đang bị giữ
docker exec code-hq-db-1 psql -U erp_user -d erp_hq -c "SELECT slot_name, active, pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn)) AS retained_wal FROM pg_replication_slots;"

# Supplier ở HQ và TP1
docker exec code-hq-db-1 psql -U erp_user -d erp_hq -c "SELECT code, name, branch_id, created_at FROM supplier ORDER BY created_at;"
docker exec code-branch-tp1-db-1 psql -U erp_user -d erp_branch_tp1 -c "SELECT code, name, branch_id, created_at FROM supplier ORDER BY created_at;"
```

Xử lý tạm cho lỗi `branch-tp2-db`, chọn **một** trong hai:

```powershell
# (a) TP2 cần chạy -> bật lại DB TP2 (subscription tự kết nối lại)
docker compose up -d branch-tp2-db

# (b) TP2 cố ý tắt -> tắt subscription ở HQ, bật lại khi TP2 chạy
docker exec code-hq-db-1 psql -U erp_user -d erp_hq -c "ALTER SUBSCRIPTION sub_hq_from_tp2 DISABLE;"
# ... sau này:  ALTER SUBSCRIPTION sub_hq_from_tp2 ENABLE;
```

Lưu ý: (b) chỉ dừng log lỗi. Slot `sub_tp2_from_hq` trên HQ vẫn giữ WAL cho tới khi TP2 chạy lại hoặc subscription của TP2 bị gỡ (RF-4).
