# Sprint 2.2: Bán hàng & Công nợ + Idempotency Key

**Ngày hoàn thành:** 2026-08-21
**Tên Sprint:** Sales & Idempotency

## 1. Mục tiêu Sprint (Goal)
Sprint này nhằm mục tiêu hiện thực hoá quy trình bán hàng cốt lõi (Sales Invoice), quản lý công nợ khách hàng (Receivable Debt) và giá bán đặc thù theo khách hàng.
Đây cũng là sprint đầu tiên hiện thực hóa cơ chế chặn lặp request (Idempotency Key) đã được lên khung từ Sprint 0.1, để đảm bảo việc tạo hoá đơn và trừ kho nhiều lần (double-spending) không thể xảy ra.

## 2. Thành quả đạt được (Done)
- Triển khai Domain Order & CRM: Tạo bảng và entities `Customer`, `CustomerProductPrice`, `ReceivableDebt`, `SalesInvoice`, `SalesInvoiceLine` tại `order.domain` và `crm.domain`.
- Triển khai AOP Idempotency: Hoàn thiện logic cho `@IdempotencyProtected` chặn lặp request nhờ bảng `idempotency_record` (tại `common/aop/IdempotencyAspect.java`).
- Chỉnh sửa cơ chế Inventory: Bổ sung khả năng bán khống `decreaseAllowNegative` cho `StockOnHand` và ứng dụng vào `OutboundReceiptService`.
- Tích hợp Cross-Domain: `SalesInvoiceService` kết hợp tạo phiếu xuất kho (Inventory) và cộng công nợ (CRM) trong cùng một Transaction context.
- Xác thực 100% Tests Pass: Hoàn thành 4 file tests (Unit & Integration) bao phủ các tình huống transaction, concurrency và data logic.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions)
- Quyết định: Sử dụng chung `OutboundReceiptService` để tự động tạo `OutboundReceipt` khi confirm `SalesInvoice`.
  - Lý do: Tái sử dụng logic trừ kho, thẻ kho và retry optimistic locking đã hoàn thiện trong Sprint 2.1, không làm mất dấu vết xuất kho so với việc gọi thẳng `InventoryFacade`.
  - Đã cân nhắc: Gọi trực tiếp `InventoryFacade` để trừ số lượng. Bỏ vì thiếu object `OutboundReceipt` để truy vết trên UI thẻ kho sau này.
- Quyết định: Idempotency Aspect join cùng Transaction của Service.
  - Lý do: Nếu Service lỗi hoặc rollback, `idempotency_record` cũng được rollback, cho phép Client retry một cách an toàn. Đạt được nhờ `@Order(Ordered.HIGHEST_PRECEDENCE)`.
  - Đã cân nhắc: Tạo transaction độc lập cho Idempotency. Bỏ vì nếu Service fail mà key đã lưu, request retry sau đó sẽ bị block vĩnh viễn (false positive).
- Quyết định: Json Deserialize của Cached Response dùng MethodSignature JavaType.
  - Lý do: Đảm bảo type safety, không bị `ClassCastException` do Jackson mặc định trả về `LinkedHashMap` cho đối tượng `ApiResponse<T>`.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. `db/migration/V4__sales_invoice_schema.sql` - Chú ý bảng `sales_invoice` lưu snapshot `previous_debt` và `remaining_debt`.
2. `order/application/SalesInvoiceService.java` - Đọc luồng `confirmInvoice` để thấy cách 3 domain (Order, CRM, Inventory) kết hợp trong 1 transaction.
3. `common/aop/IdempotencyAspect.java` - Cách lấy `Idempotency-Key` từ header và ghi `idempotency_record` kết hợp `proceed()`.
4. `inventory/application/OutboundReceiptService.java` - Chú ý đoạn rẽ nhánh `SALES_INVOICE` gọi `decreaseAllowNegative`.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Khởi tạo UUID user ảo (`UUID.randomUUID()`) trong `SalesInvoiceController`: Tạm thời stub user confirm hoá đơn do hệ thống lấy JWT Context chưa map sang User entity thực. Sprint sau xử lý Identity sẽ fix.
- Idempotency hash payload request (`request_hash`): Tạm thời để trống. Có thể dẫn đến rủi ro user đổi nội dung request nhưng tái sử dụng Idempotency Key cũ.

## 6. Việc chưa làm / Out of scope
- Thanh toán hoá đơn (Payment): Chưa xử lý quy trình thu tiền mặt, công nợ chỉ mới tăng (chưa có luồng giảm). Sẽ xử lý ở Sprint 2.3/2.4.
- In hoá đơn (Template): Chỉ lưu trữ dữ liệu, không xử lý generate file PDF hay template (thuộc UI/Frontend hoặc Reporting Service).

## 7. Cách chạy & Cách verify (Reproduce)
Lệnh chạy toàn bộ test của Sprint 2.2:
```bash
./mvnw test -Dtest="DebtReconciliationIntegrationTest,CustomerProductPriceUnitTest,SalesInvoiceIntegrationTest,IdempotencyIntegrationTest"
```
Kết quả mong đợi: `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0` - BUILD SUCCESS.

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Sprint 2.3 sẽ triển khai Procurement (Purchase Order) và Stock Transfer.
- Cần chú ý tái sử dụng lại AOP `@IdempotencyProtected` trên InboundReceiptController.
- Khi làm luồng Inbound, StockOnHand hiện không có hàm `increaseAllowNegative` (vì logic nhập kho không bao giờ cần), chỉ xài `increase()` như cũ.
