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
## Bug Fixes from Code Review
1. **(CRITICAL) Admin UI Blank**: Sửa logic check role trong AuthContext.tsx và TopRibbon.tsx đảm bảo vai trò ADMIN được so khớp đúng (chữ hoa).
2. **(HIGH) Thiếu Retryable trong Nhập kho**: Bổ sung @Retryable vào InboundReceiptService.confirmReceipt để xử lý OptimisticLocking khi nhập kho đồng thời.
3. **(HIGH) Trả hàng không giảm nợ**: Điều chỉnh business rule trong GoodsReturnService để luôn giảm công nợ cho khách hàng khi trả hàng, kể cả khi không có hóa đơn gốc. Cập nhật test GoodsReturnWithoutInvoiceTest tương ứng.
4. **(HIGH) Controller trả thẳng Entity**: Tạo SalesInvoiceResponseDto và InboundReceiptResponseDto, map dữ liệu qua DTO để trả về thay vì lộ Domain Entity trực tiếp.
5. **(HIGH) API Facade Violation**: Đưa logic gọi API đăng nhập của AuthContext.tsx vào ApiService.Auth.
6. **(HIGH) Thiếu Error Boundary**: Thêm lớp ErrorBoundary.tsx bao bọc root app App.tsx giúp ứng dụng không bị trắng toàn tập nếu một phần UI văng lỗi.
7. **(MEDIUM) JWT role claim thiếu null check**: Bổ sung if (role == null) trong JwtAuthenticationFilter.java để ném JwtException (bị 401/403) thay vì NullPointerException (gây lỗi 500).
8. **(MEDIUM) Hardcode Menu**: Tách cấu hình phân quyền Tab ra menuConfig.ts.

## Sprint 0 - Vá Branch Scope & Lỗi Kiến Trúc ERP - Baseline
1. Kết quả `mvn test` (Backend): **PASS** (Tests run: 113, Failures: 0, Errors: 0, Skipped: 1)

## Final Definition of Done (Handover) - Sprints 1-7
| Tiêu chí | Trạng thái | Ghi chú |
|---------|-------------|---------|
| Chạy lại toàn bộ Tests (Backend) | PASS | Toàn bộ test của backend đã pass. Tests run: 126, Failures: 0. Các bài kiểm tra đã bao phủ toàn bộ luồng Exception, Branch Isolation, và HQ Analytics. |
| Tài liệu Sprint & CHANGELOG | PASS | Đã hoàn thành các file markdown trong `docs/sprints/` và cập nhật `CHANGELOG.md`, tuân thủ hoàn toàn `Sprint-docs-rule.md`. |
| Cập nhật AI_CONTEXT | PASS | Đã phản ánh việc xóa `@BranchScoped`, tích hợp xử lý ngoại lệ tập trung, và kiến trúc tính toán động tồn kho/công nợ (HQ) mà không cần replicate Snapshot. |
