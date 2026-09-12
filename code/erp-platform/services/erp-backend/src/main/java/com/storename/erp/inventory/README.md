# Module Inventory

## Overview
Module Inventory quản lý trạng thái tồn kho thực tế, tính toán giá vốn và lịch sử luân chuyển hàng hóa (nhập/xuất) cho toàn bộ hệ thống ERP (cả mô hình HQ và Branch).
Module này tuân thủ chặt chẽ kiến trúc **Modular Monolith 4-layer** của dự án.

## Core Components
- **`StockOnHand`**: Entity trọng tâm quản lý tồn kho tức thời (Quantity) theo ma trận: `Branch` x `Product`. (Lưu ý: Giá vốn được lưu tại `CostLayer`, không nằm ở `StockOnHand`).
- **`StockMovement`**: Sổ kho (Ledger) append-only ghi nhận mọi thay đổi số lượng. SUM(quantity) của StockMovement sẽ luôn bằng quantity trong StockOnHand.
- **`CostLayer`**: Các lớp giá vốn nhập vào, phục vụ cho thuật toán tính giá vốn FIFO.
- **`InboundReceipt`**: Các chứng từ (phiếu) nhập kho. Áp dụng quy trình trạng thái cơ bản `DRAFT` (nháp) -> `CONFIRMED` (đã xác nhận/có hiệu lực).

## Concurrency & Integrity
Để biết chi tiết về quyết định xử lý đồng thời, vui lòng tham khảo [ADR-003: Optimistic Locking cho Inventory Concurrency](../../../../docs/adr/ADR-003-inventory-concurrency.md).
Tính toàn vẹn của tồn kho được bảo vệ nghiêm ngặt qua 3 lớp:
1. **Domain Layer**: `StockOnHand.decrease()` kiểm tra và ném `StockInsufficientException` nếu xuất lố.
2. **JPA Layer**: Optimistic Locking qua trường `@Version` chống lost-update.
3. **Database Layer**: Ràng buộc `CHECK (quantity >= 0)` mức schema.

## Costing Strategy (FIFO)
Hệ thống tính giá vốn xuất kho theo phương pháp FIFO (First-In, First-Out) thông qua `FifoCostService`. Mỗi lần nhập hàng sẽ tạo ra một `CostLayer` mới, và mỗi lần xuất hàng sẽ tiêu thụ dần số lượng từ các `CostLayer` cũ nhất đến mới nhất.

## Inter-Module Communication (Facade Pattern)
Tuân thủ giới hạn của Bounded Context:
- Các module khác (Sales, Order, Catalog) **KHÔNG ĐƯỢC PHÉP** inject trực tiếp `StockOnHandRepository` hay thao tác Entity của Inventory.
- Mọi giao tiếp đọc/ghi xuyên module phải đi qua interface `InventoryFacade`. 
- Module Inventory nhận tín hiệu tham chiếu sản phẩm qua `UUID productId` (loose coupling, không `@ManyToOne` cross-module).

## Nợ kỹ thuật (Technical Debt)
1. **CostingStrategy Factory:** Hiện tại chiến lược tính giá vốn đang hardcode theo FIFO. Nếu tương lai yêu cầu áp dụng chiến lược giá vốn khác nhau tùy theo chi nhánh (per-branch) hoặc sản phẩm (per-product), cần triển khai Factory Pattern để resolve strategy lúc runtime.
2. **Primitive Return Type cho Facade:** `InventoryFacade` hiện trả về trực tiếp `BigDecimal` (áp dụng YAGNI). Nếu các sprint sau module Sales cần thêm metadata (như ĐVT gốc, timestamp cập nhật), cần refactor signature chuyển sang dạng DTO (VD: `StockAvailabilityDto`).
