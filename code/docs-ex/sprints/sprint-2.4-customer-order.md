# Sprint 2.4 - Customer Order & Goods Return
Ngày hoàn thành: 2026-08-21

## 1. Mục tiêu Sprint (Goal)
- Xây dựng luồng Đặt hàng của Khách (Customer Order) và Trả hàng (Goods Return) nhằm khép kín vòng đời giao dịch bán hàng (Sales).
- Tích hợp Customer Order với Sales Invoice (Tự động chuyển đổi Order -> Invoice khi xác nhận).
- Tích hợp Goods Return với Inventory (Nhập kho hàng trả) và CRM (Hoàn công nợ khách hàng).

## 2. Thành quả đạt được (Done)
- Tạo Flyway Schema `V6__customer_order_schema.sql` cho các bảng `customer_order`, `customer_order_line`, `goods_return`, `goods_return_line`.
- Tạo Domain Entities: `CustomerOrder` và `GoodsReturn` tại `code/erp-backend/src/main/java/com/storename/erp/order/domain/`.
- Tạo Services xử lý nghiệp vụ:
  - `CustomerOrderService`: Xử lý tạo và xác nhận đơn hàng (gọi sang `SalesInvoiceService` để tạo hoá đơn tự động).
  - `GoodsReturnService`: Xử lý trả hàng, tự động gọi `InboundReceiptService` để nhập kho, và `ReceivableDebtService` để giảm công nợ nếu có tham chiếu hoá đơn gốc.
- Thêm method `createFromOrder` vào `SalesInvoiceService` (`code/erp-backend/src/main/java/com/storename/erp/order/application/SalesInvoiceService.java`).
- Tạo 6 Integration Tests toàn diện: kiểm thử Auto-Split, Concurrency, Trả hàng có/không hoá đơn, Rollback Transaction, Idempotency tại `code/erp-backend/src/test/java/com/storename/erp/order/integration/branch/`.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions)
- **Quyết định 1: Đóng gói logic chuyển đổi Order -> Invoice tại SalesInvoiceService thay vì CustomerOrderService.**
  - **Phương án đã chọn**: `CustomerOrderService` gọi `salesInvoiceService.createFromOrder(order)`.
  - **Phương án đã cân nhắc**: `CustomerOrderService` tự map dữ liệu thành `SalesInvoiceCreateDto` rồi gọi hàm tạo hoá đơn thông thường. 
  - **Lý do bỏ**: Phương án cũ vi phạm ranh giới Bounded Context, làm lộ logic nội bộ của Hoá đơn (Invoice) cho module Đặt hàng (Order). Đóng gói `createFromOrder` bên trong `SalesInvoiceService` giúp đảm bảo Single Responsibility.

- **Quyết định 2: Quản lý Transaction & Cleanup trong Regression Tests.**
  - **Phương án đã chọn**: Dùng `@Transactional` cho test đồng bộ; dùng `JdbcTemplate` với `SET REFERENTIAL_INTEGRITY FALSE` cho test Concurrency và Transaction Rollback.
  - **Phương án đã cân nhắc**: Dùng `repository.deleteAll()` cho mọi test.
  - **Lý do bỏ**: Gây ra lỗi khoá ngoại (DataIntegrityViolation) khi mô hình CSDL phức tạp lên (ví dụ bảng ReceivableDebt trỏ về Customer). Việc tách bạch cách dọn dẹp bằng JdbcTemplate giúp test song song (Concurrency) và test Rollback (GoodsReturnTransactionTest) phản ánh đúng Transaction Propagation ở tầng service.

## 4. Hướng dẫn đọc code theo thứ tự
1. `code/erp-backend/src/main/resources/db/migration/V6__customer_order_schema.sql`: Sơ đồ CSDL mới.
2. `code/erp-backend/src/main/java/com/storename/erp/order/domain/GoodsReturn.java`: Entity cốt lõi cho việc trả hàng.
3. `code/erp-backend/src/main/java/com/storename/erp/order/application/SalesInvoiceService.java` (Method `createFromOrder`): Đọc để hiểu cách Bounded Context giao tiếp.
4. `code/erp-backend/src/main/java/com/storename/erp/order/application/GoodsReturnService.java`: Nơi chứa logic giao tiếp liên module (Inventory + CRM).
5. `code/erp-backend/src/test/java/com/storename/erp/order/integration/branch/GoodsReturnTransactionTest.java`: Chú ý cách test rollback mà không cần bọc `@Transactional` ngoài cùng.

## 5. Rủi ro / Nợ kỹ thuật đã biết
- **Định giá hàng trả (Goods Return Costing):** Hiện tại, khi tạo `InboundReceipt` để nhập lại hàng trả vào kho, `unitCost` đang được tạm lấy từ đơn giá bán gốc (hoặc đơn giá nhập tay). Đúng ra cần có chiến lược costing (ví dụ: cost bằng cost lúc xuất) nhưng vì chưa có spec rõ ràng nên tạm thời sử dụng cách này. Khắc phục khi hoàn thiện module Finance/Costing.
- **Không kiểm tra số lượng trả vs số lượng xuất:** Hệ thống chưa validate số lượng hàng trả có vượt quá số lượng trên hoá đơn gốc hay không (nếu có tham chiếu). Cần xử lý ở giai đoạn sau.

## 6. Việc chưa làm / Out of scope
- **Thanh toán hoàn tiền (Refund Payment):** Phiếu trả hàng hiện tại mới chỉ cập nhật lại Kho và Công nợ (Receivable Debt). Việc chi tiền mặt (Cash Payment) để hoàn trả cho khách không thuộc phạm vi module Order, sẽ được xử lý riêng ở module CRM/Cash Management.
- **Bán chéo / Dropship:** Không nằm trong scope của MVP.

## 7. Cách chạy & Cách verify
Chạy lệnh sau tại thư mục `code/erp-backend` để kiểm tra Regression Gate Cứng (Phase 2):
```bash
./mvnw test -Dtest="CostingStrategyUnitTest,StockOnHandUnitTest,InventoryConcurrencyIntegrationTest,InventoryTransactionIntegrationTest,DebtReconciliation*,CustomerProductPrice*,SalesInvoiceTransaction*,DebtSnapshot*,Idempotency*,StockTransfer*,PurchaseOrder*,OrderAutoSplit*,OrderConcurrency*,GoodsReturn*,InboundReceiptIdempotencyTest"
```
- **Kết quả mong đợi:** 19/19 tests PASS, không có Exception về DataIntegrityViolation hoặc dirty reads trong transaction.

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
Phase 2 (Procurement, Inventory, Order, CRM cơ bản) đã được xác nhận (Gate Hard Passed) với sự bảo vệ của các Idempotency & Transaction tests toàn diện. 
Sprint tiếp theo có thể tự tin chuyển sang **Phase 3 (Retail / POS / Offline capabilities)**. Module Offline Sync (CouchDB / PouchDB) sẽ lấy dữ liệu từ nhánh, lúc này có thể dùng các Service đã viết ở Phase 2 để đồng bộ lên tổng công ty.
