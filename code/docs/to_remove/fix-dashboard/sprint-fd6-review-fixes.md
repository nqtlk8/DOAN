# Sprint FD-6: Sửa lỗi sau review
Ngày hoàn thành: 2026-09-28 · Mã sprint: FD-6 · Người thực hiện: Claude

## 1. Mục tiêu Sprint
Sửa các lỗi phát hiện trong `REVIEW-2026-09-28.md` sau khi đợt fix-dashboard (FD-0 → FD-5) kết thúc: 3 integration test sai schema nên chưa từng chạy được, thiếu test cho phần đổi múi giờ, `StockAlertPanel` lệch design system, và một số tài liệu bị hỏng encoding. Sprint này build trên FD-1 → FD-5; không đổi logic nghiệp vụ.

## 2. Thành quả đạt được
- **Fixture integration test (review B1)** — viết lại theo đúng cột NOT NULL, khóa ngoại và kiểu dữ liệu của schema sau V21:
  - `src/test/java/com/storename/erp/analytics/infrastructure/AnalyticsDataAdapterPostgresIT.java`: 4 test có assert số cụ thể (doanh thu/giá vốn chỉ tính hóa đơn CONFIRMED trong khoảng `[fromTs, toTs)`, giá trị tồn từ `cost_layer`, công nợ bỏ số dư âm và dòng đã xoá, tồn = `SUM(stock_movement)`); mỗi test kiểm cả `branchId` cụ thể và `branchId = null`.
  - `src/test/java/com/storename/erp/analytics/api/StockAlertApiPostgresIT.java`: kịch bản 4 sản phẩm (tồn âm không ngưỡng, dưới ngưỡng, trên ngưỡng, có ngưỡng nhưng chưa có movement) + case không truyền `branchId`; assert thứ tự, mã sản phẩm, tên chi nhánh.
  - `src/test/java/com/storename/erp/replication/SnapshotReplicationPostgresIT.java`: 2 container PostgreSQL 15; kiểm copy ban đầu, INSERT/UPDATE dòng của chi nhánh, dòng `branch_id = 1` (seed) không lên HQ, `receivable_debt`, và replica identity ở cả hai DB.
- **Test múi giờ (review C1):** `DashboardServiceTest.getDashboardMetrics_shouldConvertBusinessDayToStorageZoneRange` — ngày 02/09/2026 giờ VN phải thành `[2026-09-01T17:00, 2026-09-02T17:00)` giờ UTC.
- **Frontend (review C2):** `apps/erp-frontend/src/components/sales/StockAlertPanel.tsx` dùng token/class của `index.css` (`card`, `text-ink*`, `bg-danger-soft`, `text-danger`, `text-warning`, `badge-*`, `btn`, `erp-table`, `num`), màu số tồn theo mức cảnh báo, số lượng qua `formatNumber`. Test bổ sung assert màu theo mức: `__tests__/StockAlertPanel.test.tsx`. `config/dashboard.ts` dùng `import type`.
- **Javadoc + fallback (review D):** Javadoc giải thích lý do ở `AnalyticsProperties`, `AnalyticsSqlParams` (thêm constructor private, lớp `final`), `AnalyticsDataAdapter`, `StockAlertService`; `DashboardService` dùng fallback tên `"#<id>"` giống `StockAlertService`.
- **Tài liệu (review B2, C3):**
  - `docs/architecture/ARCHITECTURE_DECISIONS.md`: viết lại ADR-13 (bản cũ mất dấu tiếng Việt, không phục hồi được).
  - `docs/CHANGELOG.md`: viết lại mục `[fix-dashboard]` (tên field, tên bảng bị PowerShell nuốt ký tự).
  - `docs/fix-dashboard/README.md`: viết lại thành 1 bảng FD-0 → FD-6.
  - `docs/sprints/README.md`: bỏ 183 ký tự NUL, chuyển toàn file về UTF-8; khôi phục nguyên văn dòng Sprint 39 (giải mã phần UTF-16); gộp 2 dòng fix-dashboard thành 1 dòng trỏ tới README của đợt.
  - FD-1: ghi rõ chưa quan sát được test đỏ trên PostgreSQL. FD-3: bổ sung mục 7 và checklist.

## 3. Quyết định kiến trúc & Lý do
- **Quyết định:** Fixture IT dùng chi nhánh riêng (901, 902) + product/customer seed V2, và so sánh chênh lệch trước/sau cho truy vấn "tất cả chi nhánh".
  - Lý do: DB test chạy Flyway có seed V2/V13; assert số tuyệt đối trên dữ liệu seed sẽ vỡ khi seed thay đổi. Dùng lại product seed tránh phải tạo category/product với 8 cột NOT NULL.
  - Đã cân nhắc: tắt seed trong profile `postgres-it` — bỏ vì seed nằm trong migration V2/V13 (không tách được bằng `locations`) và các IT cũ đang dựa vào seed.
- **Quyết định:** Trong test replication, kiểm "dòng `branch_id = 1` không lên HQ" bằng cách chờ một thay đổi của chi nhánh 2 phát sinh SAU đó đi qua, thay vì `Thread.sleep` cố định.
  - Lý do: logical replication áp dụng theo thứ tự commit; khi thay đổi sau đã tới HQ thì các thay đổi trước (bị lọc) chắc chắn đã được xử lý → test không phụ thuộc thời gian chờ.
  - Đã cân nhắc: `sleep(2000)` như bản cũ — bỏ vì có thể pass sai khi máy chậm.
- **Quyết định:** Không nâng version Testcontainers trong sprint này.
  - Lý do: nguyên nhân "Could not find a valid Docker environment" chưa được xác nhận (cần output `docker version` trên máy dev). Rule R2/1.5 cấm đoán version khi chưa có bằng chứng.
- **Quyết định:** Chưa đổi nguồn tồn kho cho cảnh báo (review B3).
  - Lý do: cần kết quả truy vấn đối soát trên Docker thật và lựa chọn của người dùng (câu Q1 trong review).

## 4. Hướng dẫn đọc code theo thứ tự
1. `REVIEW-2026-09-28.md` mục B1 — bảng lỗi fixture cũ, để hiểu vì sao phải viết lại.
2. `AnalyticsDataAdapterPostgresIT.java` — chú ý Javadoc lớp (cách tránh phụ thuộc seed) và các helper `insert*` (đủ cột NOT NULL).
3. `StockAlertApiPostgresIT.java` — chú ý Javadoc liệt kê 4 case và thứ tự mong đợi.
4. `SnapshotReplicationPostgresIT.java` — chú ý `setUpReplication()` làm đúng các bước của `enable-snapshot-replication.sh`, và bước 4 của `stockOnHand_shouldReplicateOnlyOwnBranchRows`.
5. `DashboardServiceTest.getDashboardMetrics_shouldConvertBusinessDayToStorageZoneRange` — ví dụ số cụ thể cho M-06.
6. `StockAlertPanel.tsx` — map `TONE_CLASS` từ mức cảnh báo sang class design system.

## 5. Rủi ro / Nợ kỹ thuật đã biết
- **Code Java chưa được biên dịch trong sprint này:** môi trường thực hiện không tải được dependency Maven. Toàn bộ câu SQL trong fixture và kịch bản replication đã chạy thử trên PostgreSQL 16 thật với schema dựng từ Flyway V1 → V21 (kết quả ở mục 7), nhưng người dùng cần chạy `mvnw verify` để xác nhận biên dịch và chạy JUnit.
- **Testcontainers vẫn chưa kết nối được Docker trên máy dev** (theo FD-1, FD-5). Nếu `docker version` cho Engine ≥ 29, khả năng cao cần nâng Testcontainers (Spring Boot 3.4.0 đang quản lý bản 1.20.4) — cần xác nhận trước khi sửa `pom.xml`.
- **Review B3 chưa xử lý:** `stock_movement` ở HQ có thể thiếu lịch sử phát sinh trước khi bật replication (subscription HQ tạo với `copy_data = false`), trong khi `stock_on_hand` được copy đủ → cảnh báo có thể lệch với màn tồn kho. Cần chạy truy vấn đối soát trong review.
- `docs/sprints/README.md` có link `sprint-10-audit-remediation.md` nhưng file không tồn tại (có từ trước đợt này, không sửa).

## 6. Việc chưa làm / Out of scope
- Không đổi logic cảnh báo/KPI, không sửa migration, không chạy script replication trên Docker.
- Không nâng version Testcontainers (xem mục 3).
- Không sửa các file `tests/_legacy/**`.

## 7. Cách chạy & Cách verify
Backend (thư mục `code/erp-platform/services/erp-backend`):
```bash
.\mvnw.cmd test
.\mvnw.cmd -Dtest=AnalyticsDataAdapterPostgresIT -Dsurefire.failIfNoSpecifiedTests=false test
.\mvnw.cmd -Dtest=StockAlertApiPostgresIT -Dsurefire.failIfNoSpecifiedTests=false test
.\mvnw.cmd -Dtest=SnapshotReplicationPostgresIT -Dsurefire.failIfNoSpecifiedTests=false test
.\mvnw.cmd verify
```
Mong đợi: 3 IT trên pass khi Docker/Testcontainers hoạt động; `DashboardServiceTest` có 5 test pass.

Đã kiểm chứng trong sprint này (PostgreSQL 16, schema tạo bằng Flyway V1 → V21 gồm cả seed):
- Fixture + truy vấn của `AnalyticsDataAdapterPostgresIT`: doanh thu 200, giá vốn 120 (bỏ hóa đơn CANCELLED và hóa đơn đúng mốc `toTs`), giá trị tồn 150, công nợ 5000, tồn = 9 — khớp assert.
- Fixture của `StockAlertApiPostgresIT`: tồn SP1 = -1, SP2 = 5, SP3 = 20, cấu hình SP2/SP3 = 10, SP4 = 5, mã SP1 = `SP-G001` — khớp assert.
- Kịch bản `SnapshotReplicationPostgresIT` chạy bằng logical replication thật giữa 2 DB: copy ban đầu 42; INSERT 7; UPDATE -3; dòng seed branch 1 giữ nguyên 585 và dòng mới branch 1 không lên HQ; `receivable_debt` 5000 → 2000.

Frontend (thư mục `code/erp-platform/apps/erp-frontend`) — đã chạy:
```bash
npm run typecheck   # tsc --noEmit: 0 lỗi
npx eslint .        # 0 errors (107 warnings có từ trước)
npx vitest run      # Test Files 11 passed, Tests 48 passed
npx vite build      # built thành công (cảnh báo chunk > 500 kB có từ trước)
```

Kiểm tra encoding các file `.md` đã sửa: đọc được bằng UTF-8, không còn ký tự NUL, không còn CR giữa dòng hay chữ bị thay bằng `?`.

## 8. Điểm nối cho Sprint tiếp theo
- Chạy `mvnw verify` trên máy dev; nếu vẫn lỗi Docker, gửi output `docker version` để xác định cách nâng Testcontainers.
- Chạy truy vấn đối soát ở review mục B3 trên HQ; tùy kết quả và lựa chọn Q1, sprint sau đổi nguồn cảnh báo sang `stock_on_hand` (sửa 1 query trong `AnalyticsDataAdapter.getStockLevels` + test) hoặc backfill `stock_movement`.
- Khi ghi file tài liệu, không dùng PowerShell `Set-Content`/`Out-File` với chuỗi có backtick.

---
**Checklist tự kiểm:**
- [x] 1. Mục tiêu Sprint
- [x] 2. Thành quả đạt được (có trỏ đường dẫn file cụ thể)
- [x] 3. Quyết định kiến trúc & Lý do (có nêu phương án đã bỏ + lý do)
- [x] 4. Hướng dẫn đọc code theo thứ tự
- [x] 5. Rủi ro / Nợ kỹ thuật đã biết
- [x] 6. Việc chưa làm / Out of scope
- [x] 7. Cách chạy & Cách verify
- [x] 8. Điểm nối cho Sprint tiếp theo
- [x] Đã thêm link vào `code/docs/fix-dashboard/README.md`
- [x] Không có tính từ mơ hồ tự đánh giá chất lượng
