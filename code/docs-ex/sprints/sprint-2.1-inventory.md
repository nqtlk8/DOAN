# Sprint 2.1 - Inventory
**Ngày hoàn thành:** 2026-08-20

## 1. Mục tiêu Sprint (Goal)
Sprint này tập trung xây dựng nền tảng cốt lõi cho module Tồn kho (Inventory) trong hệ thống ERP. Mục tiêu là quản lý số lượng tồn kho và giá vốn (Costing) của các sản phẩm tại từng chi nhánh một cách an toàn, tránh race condition khi xuất kho đồng thời. Sprint này xây dựng dựa trên kiến trúc Modular Monolith và N-Instance đã thiết lập từ các sprint trước.

## 2. Thành quả đạt được (Done)
- Khởi tạo BaseEntity chuẩn hóa ID (UUID), Version (phục vụ Optimistic Lock) và Audit Fields: `com.storename.erp.common.domain.BaseEntity`
- Tạo các Entity trọng tâm của Inventory: `StockOnHand`, `InboundReceipt`, `OutboundReceipt` và các Line tương ứng (`com.storename.erp.inventory.domain.*`).
- Triển khai chiến lược tính giá vốn thông qua Interface `CostingStrategy` và 2 implementation: `WeightedAverageCostingStrategy`, `FifoCostingStrategy`.
- Xây dựng Application Services (`InboundReceiptService`, `OutboundReceiptService`, `StockQueryService`) xử lý nghiệp vụ tạo nháp, xác nhận nhập/xuất và tự động phát event.
- Thiết kế cơ chế Retry thủ công (`TransactionTemplate`) cho việc giảm tồn kho để xử lý ObjectOptimisticLockingFailureException.
- Cấu hình REST Controllers và DTOs có validate, OpenAPI và xác thực theo `@BranchScoped`.
- Tạo Interface `InventoryFacade` để chuẩn bị cho giao tiếp cross-module (Sales, Purchasing).
- Tạo script Flyway V3 (`V3__inventory_schema.sql`) định nghĩa bảng và constraint `CHECK (quantity >= 0)`.
- Tạo script Mock Data V3.1 (`V3.1__mock_inventory.sql`) giả lập dữ liệu tồn kho phục vụ môi trường DEV.
- Ghi nhận tài liệu quyết định kiến trúc `ADR-003-inventory-concurrency.md`.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- **Quyết định:** Áp dụng Optimistic Locking (`@Version`) + Service-level Retry để xử lý đồng thời xuất kho.
  - **Lý do:** Tỷ lệ xung đột khi xuất cùng 1 mặt hàng ở 1 chi nhánh không quá lớn. Optimistic Locking rất nhẹ nhàng cho trường hợp bình thường, khi có xung đột (OptimisticLockException), hệ thống tự retry 3 lần ngầm. Tầng database có thêm `CHECK (quantity >= 0)` để chặn dứt điểm tình huống vượt rào.
  - **Đã cân nhắc:** Pessimistic Locking (`SELECT FOR UPDATE`). Bỏ qua vì nguy cơ xảy ra Deadlock rất cao khi một chứng từ xuất kho nhiều dòng sản phẩm, đòi hỏi lock nhiều row ở các thứ tự ngẫu nhiên.
- **Quyết định:** Tách `BaseEntity` riêng dùng `UUID`.
  - **Lý do:** Đảm bảo hệ sinh thái phân tán (N-Instance) sinh ID không bị đụng độ, đồng thời hỗ trợ `@Version` mặc định cho Optimistic Lock.
  - **Đã cân nhắc:** Tái sử dụng `Long id` của Catalog cũ. Bỏ qua vì module cũ cần refactor dần, việc thiết kế ID mới (UUID) phải làm chuẩn ngay từ bây giờ cho Inventory.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. `docs/adr/ADR-003-inventory-concurrency.md` - Đọc trước để hiểu chiến lược đồng bộ.
2. `com.storename.erp.common.domain.BaseEntity` - Hiểu cấu trúc class cha chứa UUID và Version.
3. `com.storename.erp.inventory.domain.StockOnHand` - Quan sát logic increase/decrease lượng tồn và update giá vốn thuần ở Domain.
4. `com.storename.erp.inventory.domain.CostingStrategy` (và các Impl) - Xem cách tính giá vốn (đặc biệt Weighted Average).
5. `com.storename.erp.inventory.application.OutboundReceiptService` - Chú ý method `confirmReceiptWithRetry` sử dụng `TransactionTemplate` để retry transaction khi gặp lỗi Optimistic Lock.
6. `com.storename.erp.inventory.api.InventoryFacade` - Nhìn qua Contract API để hiểu cách các Bounded Context khác sẽ gọi Inventory.
7. `db/migration/V3__inventory_schema.sql` - Hiểu về cấu trúc database.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- **CostingStrategy Factory:** Hiện tại việc dùng Weighted Average hay FIFO đang được quyết định lúc load context thông qua `@ConditionalOnProperty`. Nếu tương lai yêu cầu cấu hình chiến lược giá vốn khác nhau theo từng sản phẩm hoặc từng chi nhánh, cần refactor chuyển sang Factory Pattern. Nên xử lý khi có requirements cụ thể ở các sprint sau.
- **Thiếu `userId` claim trong JWT:** Ở JWT hiện tại (của `Identity` module), token không chứa claim `userId`. Vì vậy, trong controller Inventory, `getUserId()` đang return `UUID.randomUUID()` tạm thời. Cần sửa module `Identity` để chèn thêm `userId` vào Token ở sprint tiếp theo.
- **Primitive Return Type ở InventoryFacade:** Phương thức getQuantity và getAverageCost hiện trả về trực tiếp `BigDecimal` thay vì DTO để tối giản. Nếu Sales module cần thêm metadata (Ví dụ: ĐVT gốc, reservation data), phải refactor signature sang DTO. YAGNI được ưu tiên hiện tại.
- **Refactor DRY cho Controllers:** Các hàm `getBranchId()` và `getUserId()` đang lặp lại ở 2 Controller. Cần extract ra `SecurityUtils` hoặc `BaseInventoryController` (N3).
- **Phân quyền chi tiết (RBAC):** Chưa áp dụng `@PreAuthorize` cấp method theo Role (admin/sales) do chưa chốt RBAC Matrix (N4).
- **Thiết lập Max Retries:** Hardcode `maxRetries = 20` đang khá cao cho production, nên giảm về 3-5 và externalize ra application config ở các sprint tiếp theo (N7).
- **Documentation:** Một số method phụ trợ và constructor chưa có Javadoc đầy đủ (N1). Mặc dù Domain rule có validate, nhưng `increase/decrease` nên có thêm `null-check` (N2).

## 6. Việc chưa làm / Out of scope
- **Kiểm soát lô (Lot/Batch) và Hạn sử dụng (Expiry Date):** Chưa triển khai vì ngành VLXD ưu tiên số lượng tổng trước, tính FIFO có thể làm sau.
- **Giữ chỗ tồn kho (Stock Reservation):** Đã tạo method stub `reserveStock` trong Facade nhưng chưa code logic. Sẽ được xử lý trong Sprint 2.2 (kết nối với Order).
- **Unit Conversion phức tạp:** `UnitConversionService` đang là stub (trả về quantity nguyên bản), chưa đọc tỷ lệ quy đổi từ module Catalog. Sẽ tích hợp chéo ở Sprint sau.

## 7. Cách chạy & Cách verify (Reproduce)
- **Kiểm chứng qua các lớp Test tự động (phân loại theo chuẩn Testing Matrix):**
  - **Level 1 (Unit Test):**
    - `CostingStrategyUnitTest` — Chạy lệnh `mvn test -Dtest=CostingStrategyUnitTest` để verify logic tính giá vốn Weighted Average so với FIFO.
    - `StockOnHandUnitTest` — Chạy lệnh `mvn test -Dtest=StockOnHandUnitTest` để verify invariant của entity Stock.
  - **Level 3 (Integration Test):**
    - `InventoryTransactionIntegrationTest` — Chạy lệnh `mvn test -Dtest=InventoryTransactionIntegrationTest` để verify tính toàn vẹn (atomicity) của transaction xuất kho.
    - `InventoryConcurrencyIntegrationTest` — Chạy lệnh `mvn test -Dtest=InventoryConcurrencyIntegrationTest` để verify cơ chế xử lý đồng thời (Race Condition với 20 luồng) bằng Optimistic Locking và Service Retry.
- Kết quả mong đợi: `BUILD SUCCESS`, toàn bộ tests đều pass màu xanh, log thể hiện được các luồng retry (WARN: Optimistic locking failure...).

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
Sprint tiếp theo (Sprint 2.2) có thể bắt đầu xây dựng chức năng Giữ chỗ tồn kho (Reservation) và Tích hợp chéo (Sales -> Inventory) thông qua `InventoryFacade`. Chú ý giải quyết Nợ Kỹ Thuật liên quan đến `userId` claim để audit chính xác người xác nhận phiếu. Cần hoàn thành nốt các API Unit Conversion lấy số liệu từ module Catalog.
