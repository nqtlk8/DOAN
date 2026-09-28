# Sprint FD-0: Tài liệu kiến trúc & baseline (2026-09-27)

## 1. Mục tiêu Sprint (Goal)
- Viết và cập nhật tài liệu kiến trúc cho đợt "Fix Dashboard & Cảnh báo tồn kho" dựa trên bản kế hoạch tổng thể `00-PLAN.md`.
- Chạy baseline test (trước khi sửa đổi code) để ghi nhận thực trạng các lỗi test đã có sẵn từ trước, làm cơ sở so sánh cho các thay đổi sau này.
- Đặt nền móng tài liệu cho các agent thực hiện code ở các sprint FD-1 đến FD-5 mà không phải tự đoán logic.

## 2. Thành quả đạt được (Done)
- Tạo mục lục danh sách sprint tại: `code/docs/fix-dashboard/README.md`.
- Ghi lại các quy tắc nghiệp vụ tại: `code/docs/fix-dashboard/business-rules.md`.
- Cập nhật Data Schema (mô tả V21, xóa `inventory_alert_log`): `code/docs/architecture/DATA_SCHEMA.md`.
- Cập nhật Ownership Matrix (thêm `stock_on_hand`, `receivable_debt` replication): `code/docs/architecture/DATA_OWNERSHIP_MATRIX.md`.
- Cập nhật API Contract (thêm endpoint cảnh báo, cập nhật DTO dashboard ⚠️ BREAKING): `code/docs/architecture/API_CONTRACT.md`.
- Cập nhật cơ chế Replication (giải thích row filter): `code/docs/architecture/DATABASE_REPLICATION.md` và `code/docs/architecture/REPLICATION_RUNBOOK.md`.
- Bổ sung ADR-13 về việc replicate snapshot tables: `code/docs/architecture/ARCHITECTURE_DECISIONS.md`.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- Quyết định: Replicate snapshot tables (`stock_on_hand`, `receivable_debt`) từ Branch lên HQ kết hợp bộ lọc (row filter `WHERE branch_id = N`). ⚠️ Bỏ qua quyết định cũ từ Sprint 6.
  - Lý do: Phải tính được số liệu công nợ từng chi nhánh hiện tại mà không phải dựa vào suy luận tính toán phức tạp trên `sales_invoice` (do có số dư đầu kỳ/trả hàng/khách trả nợ). Dùng snapshot trực tiếp giúp báo cáo nhanh và chính xác. Row filter tránh việc chi nhánh này replicate seed data (branch_id = 1) đè lên bản ghi của chi nhánh khác (P7).
  - Đã cân nhắc: Giữ nguyên Sprint 6 (tính động) — bỏ vì không tính đúng công nợ/tồn kho do thiếu dữ kiện lịch sử hoàn chỉnh; Replicate không lọc — bỏ vì dính lỗi trùng mã seed data.
- Quyết định: Xóa `inventory_alert_log` và bỏ `LowStockAlertJob`. ⚠️ BREAKING.
  - Lý do: Dashboard tính cảnh báo tồn kho realtime trực tiếp từ bảng `stock_movement` bằng cách tính tổng, không cần lưu trữ lịch sử báo động ở DB.
  - Đã cân nhắc: Lưu lịch sử và dùng job đẩy trạng thái — bỏ vì làm hệ thống phức tạp dư thừa và không có tính năng xem lịch sử ở UI.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
(Trong sprint này không có mã nguồn thay đổi, hãy đọc các tài liệu theo trình tự)
1. `code/docs/fix-dashboard/00-PLAN.md` - Đọc để hiểu tổng quan kế hoạch và baseline.
2. `code/docs/fix-dashboard/business-rules.md` - Đọc để nắm luật tính toán số liệu và logic cảnh báo.
3. `code/docs/architecture/ARCHITECTURE_DECISIONS.md` - Đọc mục ADR-13.
4. `code/docs/architecture/API_CONTRACT.md` - Đọc mục 4.6 cho format DTO dashboard/alert mới.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Các test Fail/Error trong baseline Backend cần được ghi chú. Cụ thể lỗi Integration Test: `FlywayPostgresIntegrationTest` và `PostgresContainerSmokeTest` đang bị Error do không kết nối được Docker daemon trong quá trình chạy maven test. Chúng ta không "chữa cháy" bằng `@Disabled` trong sprint này mà sẽ thiết lập lại hạ tầng test Postgres trong sprint FD-1.
- Dữ liệu rác Seed (V2/V13) gây rủi ro cao khi replicate chưa có row filter.

## 6. Việc chưa làm / Out of scope
- Cấu hình giá vốn hàng trả lại (Goods Return COGS) không xử lý trong đợt cập nhật này, vẫn giữ nguyên logic cũ.
- Chưa tạo UI quản lý cho `inventory_alert_config`. Admin phải tự insert DB tay ở HQ.
- Sprint này chưa sửa bất kỳ code hay test (chỉ chạy baseline để ghi nhận kết quả).

## 7. Cách chạy & Cách verify (Reproduce)
- Frontend baseline test: Chạy `npm run build; if ($?) { npx vitest run }` tại thư mục `code/erp-platform/apps/erp-frontend`.
  - Kết quả: Build thành công, Tests pass 44/44.
- Backend baseline test: Chạy `.\mvnw.cmd test` tại `code/erp-platform/services/erp-backend`.
  - Kết quả: Tests run: 142, Failures: 0, Errors: 2, Skipped: 1. (Lỗi 2 IT do Docker).

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Ở Sprint FD-1, agent sẽ bắt đầu cấu hình lại maven plugin để chạy các test `*IT.java` (failsafe) và sửa code để xử lý lỗi HTTP 500 do `branchId = null` gây ra khi không truyền kiểu cho query parameter trong PostgreSQL.

---
### Checklist tự kiểm trước khi báo "sprint xong"
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
