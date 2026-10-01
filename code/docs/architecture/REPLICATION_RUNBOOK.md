# Replication Runbook (vận hành)

Cập nhật: 2026-10-01. Kiến trúc và lý do thiết kế: `DATABASE_REPLICATION.md`.
Mọi lệnh chạy trong **Git Bash** (Windows) hoặc bash (Linux/macOS), tại thư mục `code/` (cùng chỗ `docker-compose.yml`). Script tự `cd` về đúng thư mục nên gọi từ đâu cũng được.

## 0. Script có sẵn

| Script | Việc làm |
|---|---|
| `scripts/bootstrap.sh [tp2] [--seed] [--no-build]` | Dựng toàn bộ từ đầu: compose up → chờ Flyway → setup replication → (tuỳ chọn) dữ liệu demo |
| `scripts/setup-replication.sh <cn> [--resync-master]` | Tạo/đồng bộ lại publication + subscription hai chiều cho một chi nhánh. Chạy lại an toàn. |
| `scripts/branch.sh on\|off\|status <cn>` | Bật/tắt container chi nhánh **kèm** subscription ở HQ |
| `scripts/remove-branch.sh <cn> [--purge-hq-data]` | Gỡ replication của một chi nhánh, giải phóng slot/WAL ở HQ |
| `scripts/check-schema-version.sh <cn>` | So danh sách version Flyway HQ ↔ chi nhánh |
| `scripts/test-replication-e2e.sh [cn]` | Kiểm thử nhanh hai chiều + quyền ghi của role ứng dụng |
| `scripts/seed-demo.py` | Nạp dữ liệu demo qua API (xem mục 3) |
| `scripts/add-branch.sh <cn>` | Thêm chi nhánh mới (đọc phần đầu file để biết các bước thủ công trước) |

## 1. Dựng từ đầu (DB trống)

```bash
cp .env.example .env                  # đổi mật khẩu nếu muốn (TRƯỚC lần chạy đầu)
docker compose --profile tp2 down -v  # XOÁ toàn bộ dữ liệu cũ (kể cả volume TP2)
scripts/bootstrap.sh --seed           # HQ + TP1 + dữ liệu demo
# hoặc: scripts/bootstrap.sh tp2 --seed   # thêm TP2
```

Kết quả mong đợi: dòng `Hoàn tất replication HQ <-> TP1`, rồi `==> Xong dữ liệu demo`.
Đăng nhập: `admin` / `password` tại http://localhost; `staff_tp1` / `password` tại http://localhost:81.

> Sau khi đổi migration Flyway (gộp V1..V8 ngày 2026-10-01), DB cũ **không dùng tiếp được**: Flyway sẽ báo lệch checksum/version. Bắt buộc `down -v`.
> Nếu chạy backend trong IDE, chạy `mvn clean` một lần để xoá file migration cũ còn trong `target/classes`.

## 2. Bật/tắt TP2 hằng ngày

```bash
scripts/branch.sh on tp2       # lần đầu bật TP2 trên hệ thống đã có: chạy tiếp lệnh dưới
scripts/setup-replication.sh tp2
python scripts/seed-demo.py --branches tp2 --transactions-only   # (tuỳ chọn) giao dịch demo cho TP2

scripts/branch.sh off tp2      # tắt: tắt subscription ở HQ trước rồi mới dừng container
scripts/branch.sh status tp2   # xem subscription + WAL HQ đang giữ cho từng chi nhánh
```

**Không** dừng TP2 bằng `docker compose stop` hay comment service trong compose mà không chạy `branch.sh off`. HQ vẫn giữ subscription `sub_hq_from_tp2` và log mỗi 5 giây:
`could not connect to the publisher: could not translate host name "branch-tp2-db"`.
Nếu đã lỡ dừng như vậy: chạy `scripts/branch.sh off tp2` (container đã dừng thì lệnh stop không làm gì thêm).

## 3. Dữ liệu demo (`scripts/seed-demo.py`)

- Danh mục **không** do script tạo: cây danh mục (giống website web-public) có sẵn từ `R__reference_data.sql`. Script đọc qua `GET /api/v1/catalog/categories` và gắn 12 sản phẩm demo (tên và giá theo sản phẩm mẫu của website) vào đủ 10 danh mục con.
- Master data tạo tại HQ: sản phẩm, nhà cung cấp, khách hàng qua API admin; bảng giá, ngưỡng cảnh báo tồn qua SQL (chưa có API ghi). Replication đưa xuống chi nhánh.
- Giao dịch tạo tại chi nhánh qua API staff, có header `Idempotency-Key` như frontend. TP1: 6 phiếu nhập (có 2 lô chậu rửa LT548 giá khác nhau để thấy FIFO), 6 hóa đơn (trả đủ / ghi nợ / trả trước một phần), 1 phiếu trả hàng. TP2: 2 phiếu nhập, 2 hóa đơn.
- Kết quả mong đợi: tổng nợ TP1 139.300.000 đ, TP2 15.540.000 đ; dashboard có 3 cảnh báo tồn thấp ở TP1 (Sofa, bồn cầu TOTO TCF33320, chậu rửa LT548) và 1 ở TP2 (đèn chùm).
- Không tự đặt id. UUID và id số do ứng dụng / sequence sinh. Mã hóa đơn `HD…`, trả hàng `TH…`, khách hàng `KH…` do backend sinh. Mã sản phẩm `PRD-<epoch ms>` và nhà cung cấp `SUP-<epoch ms>` theo đúng quy ước của màn hình quản trị.
- Script từ chối chạy nếu HQ đã có sản phẩm (tránh nạp trùng).

Tham số hay dùng: `--branches tp1,tp2`, `--transactions-only`, `--hq-url`, `--branch-url tp2=http://localhost:82`.

## 4. Sự cố thường gặp

| Hiện tượng | Nguyên nhân | Xử lý |
|---|---|---|
| HQ log `could not translate host name "branch-tpN-db"` | Container chi nhánh dừng nhưng subscription ở HQ vẫn bật | `scripts/branch.sh off tpN` |
| `setup-replication.sh`: "Bảng master ở TPN đã có dữ liệu" | DB chi nhánh cũ (còn seed hoặc đã từng replicate) | DB mới: `docker compose --profile tpN down` rồi xoá volume `code_branch_tpN_db_data`. DB đã replicate trước đó: thêm `--resync-master` |
| `setup-replication.sh`: "HQ đã có … dòng giao dịch của TPN" | HQ còn dữ liệu chi nhánh từ lần thiết lập trước | `scripts/remove-branch.sh tpN --purge-hq-data` rồi chạy lại (giao dịch được copy lại từ chi nhánh) |
| `branch.sh status`: slot `wal_status = lost` | Chi nhánh tắt quá lâu, vượt `max_slot_wal_keep_size` | `remove-branch.sh tpN` → `setup-replication.sh tpN --resync-master` |
| "Thiếu role replication 'erp_repl'" | Volume DB tạo trước khi có `docker/postgres/init/01-roles.sh` | Tạo lại volume (mục 1) |
| "Lệch version Flyway" | HQ và chi nhánh chạy image backend khác nhau | Build lại và khởi động lại cả hai app (`docker compose up -d --build`) |
| App chi nhánh báo `permission denied for table …` khi ghi | Có bảng mới do chi nhánh ghi nhưng chưa có trong danh sách của `R__branch_db_security.sql` | Thêm bảng vào danh sách đó (và `replication-tables.conf` nếu cần replicate), khởi động lại app |

## 5. Kiểm tra nhanh

```bash
scripts/test-replication-e2e.sh tp1     # mong đợi: 4 dòng PASS, "TẤT CẢ PASS"
scripts/branch.sh status tp1
```

Trên DB: `pg_stat_subscription` (phía nhận), `pg_stat_replication` và `pg_replication_slots` (phía gửi).
