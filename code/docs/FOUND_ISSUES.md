# Danh sách lỗi phát hiện (Found Issues)

## Sprint 0 - Chụp Baseline
1. Kết quả `mvn test` (Backend): **PASS** (Tests run: 58, Failures: 0, Errors: 0, Skipped: 1)
2. Kết quả `npm run build` (Frontend): **PASS** (Không có lỗi, build thành công với 1 vài warning kích thước chunk).
3. Ghi nhận lỗi B1 (Nhập Hàng):
   - Khi bấm nút "Nhập Hàng", giao diện bị gãy hoặc console báo lỗi do `ApiService.Purchasing.createOrder()` không tồn tại vì module `Purchasing` (và API cũ) đã bị gỡ bỏ ở backend (V11 inventory refactor) nhưng frontend vẫn gọi.
4. Ghi nhận lỗi B2 (Dashboard):
   - Khi tạo Sales Invoice thành công và confirm, qua Dashboard xem Doanh thu/Lợi nhuận thì giá trị hiển thị là 0. Lý do vì UI đọc số liệu từ bảng `fact_sales`, nhưng `AnalyticsEtlJob` hiện tại mới chỉ là class rỗng (scaffold), không nạp dữ liệu vào bảng fact. Do đó data hiển thị luôn bị rỗng.

## Final Definition of Done (Handover)

| Tiêu chí | Trạng thái | Ghi chú |
|---------|-------------|---------|
| Chạy lại toàn bộ Tests (Backend) | PASS | Toàn bộ test của backend đã pass. Tests run: 58, Failures: 0. |
| Build Frontend (
pm run build) | PASS | Frontend build thành công, không gặp lỗi TS nào. |
| B1: Sửa luồng Inbound Receipt UI | PASS | Đã tạo InboundReceiptModule.tsx, tích hợp thành công thay thế PurchaseModule. Đã xóa code dư thừa. |
| B2: Sửa Dashboard Analytics | PASS | Đã đổi Query sang trực tiếp sales_invoice qua AnalyticsDataAdapter, lấy đúng Doanh thu, Lợi nhuận gộp và Tồn kho mà không cần cấu trúc ETL phức tạp ở hiện tại. |
| B3 & B4: Dọn dẹp Document và Dead code | PASS | Cập nhật Inventory README.md loại bỏ các thuật ngữ/cấu trúc cũ. Xóa trường purchaseOrderId khỏi sự kiện, ghi chú lại ETL Job và schema V1. |
| B5: Cải thiện Low Stock Alert | PASS | Đã phân tách rõ ràng NEGATIVE_STOCK và LOW_STOCK, cập nhật thư viện gửi email logic (dù bị vô hiệu hóa vì thiếu SMTP thật). |
