# Sprint FD-2: Migration V21 + replicate snapshot tables
Ngày hoàn thành: 2026-09-28

## 1. Mục tiêu Sprint
Sprint này giải quyết bài toán thiếu dữ liệu tồn kho và công nợ trên dashboard tại HQ (do 2 bảng này không được đồng bộ từ Branch lên HQ trước đây). Mục tiêu là thiết lập cơ chế replicate cho `stock_on_hand` và `receivable_debt` thông qua row filter để tránh đụng độ dữ liệu seed, đồng thời cấu trúc lại `inventory_alert_config`.

## 2. Thành quả đạt được
- Tạo file migration V21 (`src/main/resources/db/migration/V21__analytics_alert_and_snapshot_replication.sql`) cài đặt logic cấu trúc alert config và đặt `REPLICA IDENTITY USING INDEX` cho 2 bảng snapshot.
- Xóa bỏ toàn bộ mã nguồn của hệ thống cảnh báo cũ (job, entity, repository) như `InventoryAlertLog.java`, `InventoryAlertLogRepository.java`, `LowStockAlertJob.java`.
- Cập nhật entity `InventoryAlertConfig.java` phản ánh các cột `NOT NULL`.
- Bổ sung shell script mới cấu hình đồng bộ hóa `code/scripts/enable-snapshot-replication.sh`.
- Cập nhật shell script `code/scripts/setup-replication.sh` để gọi script trên lúc khởi tạo.
- Viết integration test `SnapshotReplicationPostgresIT.java` bằng Testcontainers.
- Bổ sung unit tests chặn HQ ghi vào snapshot tables trong `ReplicationOwnershipHqTest.java`.

## 3. Quyết định kiến trúc & Lý do
- **Quyết định 1:** Đảo ngược quyết định Sprint 6 — Bật đồng bộ `stock_on_hand` và `receivable_debt` từ Branch lên HQ.
  - *Phương án đã bỏ:* Tính động từ `stock_movement` và `sales_invoice` trên HQ. Bỏ vì công nợ không tính được chính xác từ hóa đơn do còn yếu tố trả hàng, số dư đầu kỳ, và thanh toán nợ.
- **Quyết định 2 (⚠️ BREAKING):** Dùng PostgreSQL Logical Replication với row filter `WHERE branch_id = N` cho 2 bảng này.
  - *Phương án đã bỏ:* Replicate không lọc. Bỏ vì dữ liệu seed ban đầu tạo ra bản ghi `branch_id = 1` ở mọi database chi nhánh; nếu không lọc, chi nhánh 2 sẽ đẩy đè dữ liệu của chi nhánh 1 lên HQ (vi phạm nguyên tắc single-writer).
- **Quyết định 3 (⚠️ BREAKING):** DROP bảng `inventory_alert_log` và loại bỏ `LowStockAlertJob`.
  - *Lý do:* Theo D-07 và D-08, dashboard sẽ tính cảnh báo trực tiếp từ `stock_movement` không phụ thuộc job và không lưu lịch sử cảnh báo.

## 4. Hướng dẫn đọc code theo thứ tự
1. `V21__analytics_alert_and_snapshot_replication.sql`: Xem thay đổi database schema, cấu trúc UNIQUE, và `REPLICA IDENTITY`.
2. `enable-snapshot-replication.sh`: Xem logic kiểm tra và thiết lập row-filtered publication/subscription, lệnh khởi tạo (DELETE các row cũ trên HQ).
3. `setup-replication.sh`: Xem vị trí móc nối script `enable-snapshot-replication.sh`.
4. `InventoryAlertConfig.java`: Xem các column properties mới được áp dụng `nullable = false`.
5. `SnapshotReplicationPostgresIT.java`: Xem kịch bản Integration Test bằng Testcontainers mô phỏng 2 container postgres để test replication logic.
6. `ReplicationOwnershipHqTest.java`: Xem các test assert rằng HQ không thể thay đổi snapshot tables.

## 5. Rủi ro / Nợ kỹ thuật đã biết
- Môi trường Docker trên máy chạy test không khả dụng (`Could not find a valid Docker environment`), dẫn tới Testcontainers không khởi động được IT. Chưa thể verify hoàn chỉnh `SnapshotReplicationPostgresIT` qua Maven trên môi trường này.

## 6. Việc chưa làm / Out of scope
- Cập nhật logic API trả về trên `AnalyticsDataAdapter` chưa được làm trong sprint này.
- Chưa trực tiếp cấu hình trên các container Docker thật của ứng dụng (dành cho sprint FD-5).

## 7. Cách chạy & Cách verify
Để tái tạo thành quả sprint (Bỏ qua IT fail do thiếu Docker trên máy):
```bash
# Backend test
.\mvnw.cmd test -Dtest=ReplicationOwnershipHqTest

# Check script syntax
bash -n code/scripts/enable-snapshot-replication.sh
```

Trong thực tế, khi cần bật replicate bảng snapshot cho chi nhánh đang chạy (ví dụ tp1, tp2):
```bash
# Đối với tp1
bash code/scripts/enable-snapshot-replication.sh tp1 --yes

# Đối với tp2
bash code/scripts/enable-snapshot-replication.sh tp2 --yes
```
Kết quả mong đợi: Thông báo `Replication Verification (Branch ID: X)` với số liệu row count tương đương nhau giữa Branch và HQ.

## 8. Điểm nối cho Sprint tiếp theo
- Các sprint tiếp theo (FD-3, FD-4) có thể giả định `stock_on_hand` và `receivable_debt` ở HQ đã luôn được update theo thời gian thực từ các nhánh.
- Sprint FD-3 sẽ bắt đầu tập trung vào `AnalyticsDataAdapter` tính toán lại các chỉ số dashboard dựa trên database đã update và tạo API cảnh báo.

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
- [x] Đã thêm link vào `docs/sprints/README.md`
- [x] Không có tính từ mơ hồ tự đánh giá chất lượng
