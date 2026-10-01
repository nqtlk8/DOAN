# Đợt fix-replication (2026-10-01)

| Tài liệu | Nội dung |
|---|---|
| [00-PLAN.md](./00-PLAN.md) | Hiện trạng đã xác minh trước khi sửa, quyết định D-01 → D-07, kế hoạch RF-0 → RF-5 |

Kết quả đã được đưa vào tài liệu chuẩn: `architecture/DATABASE_REPLICATION.md`, `REPLICATION_RUNBOOK.md`, `DATA_OWNERSHIP_MATRIX.md`, `DATA_SCHEMA.md`, `SECURITY_MODEL.md`, `ARCHITECTURE_DECISIONS.md` (ADR-14), `testing/TEST_STRATEGY.md`.

Quyết định đã chốt (người dùng, 2026-10-01):

| Mã | Quyết định |
|---|---|
| D-01 | Đồng ý xoá volume DB để chạy chuỗi Flyway mới |
| D-02 | Giữ tài khoản (admin, staff_tp1, staff_tp2 + chi nhánh, vai trò); bỏ toàn bộ master data và giao dịch khỏi Flyway |
| D-03 | Dữ liệu hệ thống chỉ chèn ở HQ rồi replicate xuống (đề xuất, không bị phản đối) |
| D-04 | Nhà cung cấp dùng chung toàn hệ thống |
| D-05 | Cần dữ liệu demo, đúng cấu trúc code (không tự đặt id kiểu `SUP_001` cho bảng dùng UUID) |
| D-06 | TP2 chỉ bật khi cần, người dùng tự bật/tắt |
| D-07 | Replicate `receivable_debt_movement` lên HQ |
