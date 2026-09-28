# Sprint 1: Chuẩn hóa Backend DTO & API Contract
Ngày hoàn thành: 2026-09-16

## 1. Mục tiêu Sprint (Goal)
- Sửa đổi các DTO trả về ở backend để phục vụ đúng dữ liệu frontend đang kỳ vọng, tránh tình trạng alias/fallback.
- Không để frontend phải gọi nhiều API chéo để hiển thị tên đối tác/sản phẩm (tránh N+1 hoặc join trên UI).

## 2. Thành quả đạt được (Done)
- **Debt DTO:** Tạo `ReceivableDebtResponseDto` bổ sung thông tin từ `Customer` (name, code, phone, address). Sửa `ReceivableDebtController` trả về DTO này.
- **Stock DTO:** Tạo `StockOnHandResponseDto`. Tạo `StockService` (Inventory context) dùng map/join in-memory hoặc query repository để đính kèm `productName`, `productCode`, `branchName` vào response, trả qua `StockController`.
- **Sales DTO:** Sửa `SalesInvoiceResponseDto` thêm `customerName`. Sửa `SalesInvoiceService` join dữ liệu từ `CustomerRepository`. Cập nhật `SalesInvoiceController` để return `{ id: "...", invoiceCode: "..." }` cho các lệnh create/confirm (không trả về chuỗi UUID rỗng).
- **Inbound DTO:** Tạo `InboundReceiptCreateResponseDto` có trường `id: UUID`, sửa controller trả về object JSON chuẩn có wrapper thay vì string.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions)
- Quyết định: Viết `StockService` để query danh sách `Product` (từ `ProductRepository`) và mapping vào `StockOnHandResponseDto` thay vì thay đổi query SQL hoặc schema trực tiếp.
- Lý do: Tôn trọng Modular Monolith; Inventory và Catalog là hai domain khác nhau. Bằng cách gọi Repository (in-memory hash join) hoặc qua Facade, chúng ta giữ schema độc lập mà API vẫn trả về DTO đủ dữ liệu.
- Đã cân nhắc: Bắt Frontend gọi `/catalog/products` rồi tự join. Bỏ vì gây performance kém ở UI và code phức tạp.

## 4. Hướng dẫn đọc code theo thứ tự
1. `ReceivableDebtResponseDto.java`: Xem cấu trúc phẳng.
2. `StockService.java`: Xem logic join product và branch để map ra view DTO.
3. `SalesInvoiceResponseDto.java` & `SalesInvoiceService.java`: Cách map `customerName`.
4. `InboundReceiptCreateResponseDto.java` & Controller: Fix lỗi string UUID của frontend.

## 5. Rủi ro / Nợ kỹ thuật đã biết
- `SalesInvoiceService` đang inject trực tiếp `CustomerRepository` (vượt domain boundary). Ở tương lai nên đưa qua `CrmFacade`. 
- `StockService` cũng inject `ProductRepository`. Nên đưa vào `CatalogFacade` sau. (Chấp nhận trong Sprint này vì dự án đang cho phép ở module khác như GoodsReturn).

## 6. Việc chưa làm / Out of scope
- OpenAPI schema vẫn chưa regenerate (sẽ làm ngay Sprint 2).
- Frontend chưa được cập nhật type và remove `any` (Sprint 2/8).

## 7. Cách chạy & Cách verify
- Run lệnh: `./mvnw test` tại thư mục backend. Các test API liên quan Inbound, Sales, Stock sẽ pass, JSON format output chứa đầy đủ tên khách hàng/sản phẩm và nested DTO.

## 8. Điểm nối cho Sprint tiếp theo
- Sau khi code backend pass test, Sprint 2 sẽ lấy JSON structure này để regenerate `openapi.json` và update TypeScript interface.
