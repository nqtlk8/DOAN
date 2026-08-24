# Sprint 2.3 - Procurement & Idempotency
**Ngày lập:** 2026-08-21
**Sprint:** 2.3

## 1. Mục tiêu Sprint (Goal)
- Xây dựng module `procurement` (mua sắm) bao gồm quản lý đơn đặt hàng nhà cung cấp (Supplier Purchase Order), công nợ phải trả (Payable Debt) và luân chuyển kho nội bộ (Stock Transfer).
- Tích hợp tính năng tự động nhận PO (auto-receive) khi phiếu nhập kho (InboundReceipt) được xác nhận.
- Áp dụng cơ chế Idempotency đã xây dựng ở Sprint 2.2 cho các API tạo phiếu nhập kho và xuất/nhập luân chuyển kho nhằm ngăn chặn lỗi double-submit.

## 2. Thành quả đạt được (Done)
- **Database Schema**: Tạo `V5__procurement_schema.sql` định nghĩa các bảng `supplier`, `payable_debt`, `supplier_purchase_order`, `stock_transfer` và bổ sung `purchase_order_id` cho `inbound_receipt`.
- **Domain Entities**: Khởi tạo các entity cho module procurement tại `procurement.domain.*`.
- **Domain Events**: Tạo `InboundReceiptConfirmedEvent` tại `inventory.domain.event` để module `procurement` tự lắng nghe và xử lý công nợ.
- **Application Services & DTOs**: Tạo `PayableDebtService`, `SupplierPurchaseOrderService`, `StockTransferService` tại `procurement.application` và các DTO tương ứng.
- **API Controllers**: Tạo `StockTransferController` và `SupplierPurchaseOrderController` tại `procurement.api.*`.
- **Idempotency**: Gắn `@IdempotencyProtected` lên `InboundReceiptController.confirm()`, `StockTransferController.ship()` và `receive()`.
- **Tests**: Hoàn thành 5 bài test Integration Level 3 cho Procurement và Idempotency tại `src/test/java/com/storename/erp/.../integration/branch/`.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions)
- **Quyết định 1**: Sử dụng **Domain Event (Event-Driven)** (`InboundReceiptConfirmedEvent`) để liên kết giữa `InboundReceipt` và `SupplierPurchaseOrder` thay vì tiêm Service trực tiếp.
  - **Lý do**: Tuân thủ chặt chẽ nguyên tắc DDD (Loose Coupling). Module `inventory` không cần phải biết về module `procurement`.
  - **Đã cân nhắc**: Inject `SupplierPurchaseOrderService` vào `InboundReceiptService` (bỏ vì gây circular dependency hoặc phá vỡ nguyên tắc độc lập module).
- **Quyết định 2**: Giá vốn (Unit Cost) trong chuyển kho nội bộ (`StockTransferService`) được lấy trực tiếp từ `InventoryFacade.getAverageCost()` tại chi nhánh xuất để dùng cho phiếu nhập ở chi nhánh nhận.
  - **Lý do**: Chuyển kho nội bộ là di dời vật lý, cần bảo lưu giá trị tài sản đã ghi nhận.
  - **Đã cân nhắc**: Đặt cost = 0 (bỏ vì sẽ làm sai lệch giá vốn trung bình của chi nhánh nhận khi tính lợi nhuận gộp sau này).
- **Quyết định 3**: Sửa đổi luồng test Idempotency trong Spring Security test context.
  - **Lý do**: Yêu cầu gọi qua HTTP `mockMvc` để aspect được trigger, nhưng filter bảo mật đòi hỏi JWT token. Do đó phải dùng `JwtTokenProvider` cấp phát token thay vì mock `@WithMockUser` đơn thuần.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
- `V5__procurement_schema.sql`: Xem cấu trúc DB mới.
- `StockTransferService.java`: Nơi có logic gọi chéo `OutboundReceiptService` và `InboundReceiptService` để đồng bộ kho vật lý.
- `SupplierPurchaseOrderService.java`: Xem phương thức `handleInboundReceiptConfirmed` với `@EventListener` tự động map hóa đơn.
- `IdempotencyAspect.java` (từ Sprint 2.2): Đọc lại Aspect xử lý request lặp.
- `InboundReceiptIdempotencyTest.java`: Xem cách test lặp request trong điều kiện Async + Spring Security.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- **Chưa có logic giá vận chuyển luân chuyển kho (Freight Cost)**: Giá nhập hiện tại kế thừa nguyên bản `avgCost`, chưa tính chi phí logistics nội bộ.
- **`UnitConversionService`**: Hiện đang là mock bean hoặc stub, chưa quy đổi đơn vị đo lường thật. (Giữ nguyên nợ từ Sprint 2.1).

## 6. Việc chưa làm / Out of scope
- **Thanh toán công nợ**: Chưa có API "Payment" (Trả tiền nhà cung cấp). Hiện chỉ có logic tự tăng/giảm công nợ, luồng thanh toán tiền thật sẽ ở một module Tài chính riêng.
- **Hoàn trả PO (PO Return)**: Nằm trong scope của GoodsReturn (Sprint 2.4).

## 7. Cách chạy & Cách verify (Reproduce)
```bash
.\mvnw test -Dtest="StockTransferReconciliationTest,PurchaseOrderNoStockImpactTest,PurchaseOrderAutoMatchTest,SalesInvoiceNotBlockedByPOTest,InboundReceiptIdempotencyTest" -ActiveProfiles="branch,test"
```
- **Kết quả mong đợi**: `[INFO] BUILD SUCCESS`, 5 tests pass 100%. Nếu có `StaleObjectStateException` trong log thì đó là hành vi mong muốn của Idempotency test (1 request bị rollback).

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Sprint 2.4 (CustomerOrder & GoodsReturn) sẽ cần tận dụng lại các service bán hàng và tồn kho đã có.
- Chú ý phần tách hoá đơn (AutoSplitInvoice) sẽ phải gọi lại `SalesInvoiceService.createDraft()` đã được xây dựng từ Sprint 2.2.
- Gate 2.4 sẽ là "Gate Cứng" yêu cầu pass toàn bộ > 21 tests từ đầu dự án đến giờ.
