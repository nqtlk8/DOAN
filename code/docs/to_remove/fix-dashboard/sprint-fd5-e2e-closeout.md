Ngày hoàn thành: 2026-09-28
Mã sprint: FD-5

#### 1. Mục tiêu Sprint (Goal)
- Chạy hệ thống đầy đủ (HQ + TP1 + TP2) trên Docker để kiểm thử E2E quy trình cảnh báo tồn kho và đối soát dữ liệu Dashboard.
- Đóng đợt (close-out) luồng `fix-dashboard` bằng việc cập nhật tài liệu CHANGELOG, AI_CONTEXT và tổng hợp Known Risks/Tech Debt.

#### 2. Thành quả đạt được (Done)
- Xác nhận Flyway đạt `V21` ở tất cả database nhánh (`hq`, `tp1`, `tp2`).
- Chạy thành công `code/scripts/enable-snapshot-replication.sh` cho `tp1` và `tp2` với tính năng copy data cho bảng snapshot với row filter.
- Vượt qua toàn bộ kịch bản E2E:
  - Xem dashboard tổng công ty không bị lỗi 500.
  - Test xuất Excel thành công.
  - Thiết lập threshold tại HQ hiển thị ngay lập tức mức cảnh báo `LOW_STOCK`.
  - Giả lập xuất bán quá tồn tại nhánh TP1 hiển thị ngay mức cảnh báo `NEGATIVE_STOCK` tại HQ.
  - Giả lập nhập hàng lại bù tồn tại nhánh TP1 loại bỏ mức cảnh báo tại HQ.
  - Xác nhận đối soát `stock_movement` và `stock_on_hand` trùng khớp cho các phát sinh mới. Đối soát công nợ trùng khớp.
- Cập nhật tài liệu:
  - `code/docs/CHANGELOG.md`
  - `code/docs/AI_CONTEXT.md`
  - `code/docs/FOUND_ISSUES.md`
  - `code/docs/plans/analytics-dashboard-alert-questions.md`
  - `code/docs/fix-dashboard/README.md`
  - `code/docs/sprints/README.md`

#### 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
Quyết định: Hoàn thiện luồng kiểm thử thủ công và chốt các cấu hình hiện tại cho phiên bản sản xuất.
Lý do: Sau khi replication được nâng cấp để có row filter cho `stock_on_hand` và `receivable_debt`, các rủi ro duplicate do seed data được cách ly. Việc sử dụng `stock_movement` làm nguồn cảnh báo tồn (tính tổng động) chứng minh hoạt động ổn định và nhất quán với các giao dịch bán hàng (invoice) và trả hàng (return).
Đã cân nhắc: Chạy lại seed DB từ đầu để xoá dữ liệu giả sinh trùng lặp — bỏ vì phức tạp và không cần thiết do mục tiêu của row filter là đủ để cách ly.

#### 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
- Xem `code/docs/fix-dashboard/README.md` để nắm bắt tổng quan đợt fix.
- Xem `code/docs/CHANGELOG.md` cho danh sách thay đổi và BREAKING CHANGES.
- Đọc `code/docs/AI_CONTEXT.md` để hiểu current baseline của toàn hệ thống sau đợt này.

#### 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Lỗi Testcontainers khi chạy `mvn verify` tại một số môi trường Windows do `NpipeSocketClientProviderStrategy` - cần config DOCKER_HOST hoặc WSL rõ ràng hơn.
- Cấu trúc ETL và các bảng fact (fact_sales, fact_stock_movement) vẫn chưa được sử dụng trong tính toán dashboard (do tính động từ transaction tables).
- Chỉ số doanh thu dashboard gộp toàn bộ, hiện tại **chưa trừ hàng trả**.
- Vòng quay tồn kho đang dùng lượng tồn hiện tại làm ước lượng trung bình, chưa phải công thức tồn kho bình quân chuẩn.
- Bảng `receivable_debt_movement` chưa được replicate lên HQ, do đó lịch sử công nợ theo thời gian không truy vết được hoàn chỉnh từ HQ.
- Seed data (V2/V13) tạo ra dữ liệu cho `branch_id = 1` ở mọi database (kể cả TP2). Điều này có thể dẫn đến nhầm lẫn trong quá trình phát triển (đã xử lý cô lập bằng row filter khi publish).
- Email cho cảnh báo tồn kho chưa có SMTP thực tế, hiện tại chỉ là mock log. Cảnh báo mới chỉ áp dụng cho HQ xem, các chi nhánh chưa được nhận thông báo độc lập.

#### 6. Việc chưa làm / Out of scope
- Sửa đổi kịch bản seed DB ở các bản migration cũ.
- Áp dụng trừ doanh thu đối với Goods Return trên dashboard (thuộc phạm vi đợt refactor kế toán tương lai).
- Gửi thông báo cảnh báo trực tiếp về Dashboard nội bộ của chi nhánh.

#### 7. Cách chạy & Cách verify (Reproduce)
Lệnh E2E (khởi chạy):
```bash
docker compose up -d --build
```
Kiểm tra Flyway (PostgreSQL DB nội bộ):
```bash
docker exec code-hq-db-1 psql -U erp_user -d erp_hq -c "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;"
```
Chạy Test Unit & IT (Backend):
```bash
cd erp-platform/services/erp-backend && ./mvnw verify
```
(Kết quả: `Tests run: 147, Failures: 0, Errors: 2 (do lỗi Testcontainers connect docker local), Skipped: 1`)
Chạy Test (Frontend):
```bash
cd erp-platform/apps/erp-frontend && npm run build && npx vitest run
```
(Kết quả: `Test Files: 11 passed | Tests: 48 passed`)

#### 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Đợt fix-dashboard đã hoàn tất. Các luồng công việc kế tiếp nên tập trung vào việc giải quyết phần Tech Debt (cụ thể là xử lý logic doanh thu trừ trả hàng) và mở rộng ETL job để tận dụng lại các bảng Fact, giảm tải cho các bảng transaction đang bị SUM realtime.

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
