# Danh sách lỗi phát hiện (Found Issues)

## Sprint 0 - Chụp Baseline
1. Kết quả `mvn test` (Backend): **PASS** (Tests run: 58, Failures: 0, Errors: 0, Skipped: 1)
2. Kết quả `npm run build` (Frontend): **PASS** (Không có lỗi, build thành công với 1 vài warning kích thước chunk).
3. Ghi nhận lỗi B1 (Nhập Hàng):
   - Khi bấm nút "Nhập Hàng", giao diện bị gãy hoặc console báo lỗi do `ApiService.Purchasing.createOrder()` không tồn tại vì module `Purchasing` (và API cũ) đã bị gỡ bỏ ở backend (V11 inventory refactor) nhưng frontend vẫn gọi.
4. Ghi nhận lỗi B2 (Dashboard):
   - Khi tạo Sales Invoice thành công và confirm, qua Dashboard xem Doanh thu/Lợi nhuận thì giá trị hiển thị là 0. Lý do vì UI đọc số liệu từ bảng `fact_sales`, nhưng `AnalyticsEtlJob` hiện tại mới chỉ là class rỗng (scaffold), không nạp dữ liệu vào bảng fact. Do đó data hiển thị luôn bị rỗng.
