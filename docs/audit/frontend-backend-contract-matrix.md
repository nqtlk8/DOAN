# ERP Frontend-Backend Contract Matrix

*Tài liệu Baseline Sprint 0*
*Quy tắc:* `openapi.json` hiện tại không được coi là source of truth. Backend Controller/DTO là source of truth thực sự. Mọi thông tin dưới đây được trích xuất từ source code thực tế.

## 1. Debt (Receivable Debt)

*   **Endpoint:** `GET /api/v1/receivable-debts`
*   **Request Params:** (Không) (Auth context ngầm xác định `branchId`)
*   **Response Backend (Thực tế):** `ApiResponse<List<ReceivableDebt>>`. `ReceivableDebt` entity chứa: `customer` object, `branchId`, `totalDebt`.
*   **Frontend UI Component (`DebtList.tsx`) kỳ vọng:** `partnerCode`, `partnerName`, `debtAmount`.
*   **Mismatch:** Có.
*   **Expected Correction:** 
    *   Backend: Tạo `ReceivableDebtResponseDto` với các flat fields `customerId`, `customerCode`, `customerName`, `totalDebt`.
    *   Frontend: Sửa component render đúng các trường này, bỏ các alias/fallback.

## 2. Stock (Inventory Stock On Hand)

*   **Endpoint:** `GET /api/v1/inventory/stock`
*   **Request Params:** (Không)
*   **Response Backend (Thực tế):** `ApiResponse<List<StockOnHand>>`. `StockOnHand` entity chứa: `productId`, `branchId`, `quantity`. Không có product object.
*   **Frontend UI Component (`StockList.tsx`) kỳ vọng:** `productCode`, `productName`, `quantity`, `warehouse`.
*   **Mismatch:** Có. Thiếu dữ liệu join nghiêm trọng.
*   **Expected Correction:** 
    *   Backend: Viết query/Facade trả về `StockOnHandResponseDto` có kèm thông tin product (Code, Name) và branch (Name).

## 3. Sales Invoice (List & Detail)

*   **Endpoint:** `GET /api/v1/sales-invoices`
*   **Request Params:** (Không)
*   **Response Backend (Thực tế):** `ApiResponse<List<SalesInvoiceResponseDto>>`. Chứa `id`, `invoiceCode`, `customerId`, `totalAmount`, `status`, `previousDebt`, `remainingDebt`.
*   **Frontend UI Component (`SalesList.tsx`) kỳ vọng:** `order.code || order.orderId` và `order.customerName || order.customerId`.
*   **Mismatch:** Có. UI đang mix giữa `orderId` và `code`, đồng thời fallback hiển thị UUID nếu không có `customerName`.
*   **Expected Correction:** 
    *   Backend: Nếu UI cần hiển thị tên khách hàng, `SalesInvoiceResponseDto` cần được populate `customerName`. 
    *   Frontend: Sửa field name chính xác là `id`, `invoiceCode`.

## 4. Inbound Receipt (Create)

*   **Endpoint:** `POST /api/v1/inventory/inbound`
*   **Request Body:** `InboundReceiptCreateDto`
*   **Response Backend (Thực tế):** `ApiResponse<UUID>`.
*   **Frontend UI API (`ApiService.ts`) mapping:** `res.data.data` (trả về UUID string).
*   **Frontend UI Hook (`useInboundReceipt.ts`) kỳ vọng:** `response.id`. Do `response` đã là string, nên `response.id` là `undefined`.
*   **Mismatch:** Có. Lỗi unpack response.
*   **Expected Correction:** 
    *   Backend: Trả về một DTO rõ ràng thay vì UUID, ví dụ: `{ "id": "uuid-..." }` (được wrap trong `ApiResponse`).
    *   Frontend: Đọc đúng cấu trúc `response.id`.

## 5. Catalog - Product Price

*   **Endpoint:** `GET /api/v1/catalog/products`
*   **Response Backend (Thực tế):** `ApiResponse<List<ProductResponseDto>>`. Chứa `id`, `code`, `name`, `price`, `attributes`.
*   **Frontend UI Component (`InboundReceiptModule.tsx`) kỳ vọng:** `product.basePrice`.
*   **Mismatch:** Có. Tên trường sai (`basePrice` vs `price`).
*   **Expected Correction:** Sửa UI dùng `product.price`.

## 6. Catalog - Supplier

*   **Endpoint:** `GET /api/v1/suppliers`
*   **Response Backend (Thực tế):** `ApiResponse<List<SupplierResponseDto>>`.
*   **OpenAPI định nghĩa:** Array trực tiếp (không có ApiResponse wrapper).
*   **Frontend API (`ApiService.ts`):** `res.data.data` (đúng với backend thực tế).
*   **Mismatch:** OpenAPI bị out-of-sync. Backend và Frontend runtime đang khớp nhau.
*   **Expected Correction:** Cập nhật lại OpenAPI spec để khớp với backend runtime.

## 7. Sales Prepayment (Trả trước)

*   **Form `SalesOrderForm.tsx` (dự kiến):** Có nhập số tiền trả trước.
*   **API `POST /api/v1/sales-invoices`:** `SalesInvoiceCreateDto` CHƯA CÓ trường `advancePayment`.
*   **Backend Entity `SalesInvoice`:** CHƯA CÓ trường lưu số tiền đã trả trước.
*   **Backend Logic `SalesInvoiceService.confirmInvoice`:** 
    *   Thêm TOÀN BỘ `invoice.getTotalAmount()` vào `currentDebt`. Bỏ qua số tiền đã trả trước.
*   **Mismatch:** Logic thanh toán (payment flow) đang bị hổng, UI không thể submit số tiền trả trước.
*   **Expected Correction:** Phải thực thi Sprint 3 (Sửa schema, truyền `advancePayment`, update công nợ đúng công thức `oldDebt + totalAmount - advancePayment`).

## Kết luận Sprint 0
Baseline đã được chụp. Các lỗi đều nằm ở boundary contract. Không fix bằng cách chế biến data ở component. Sẽ tiến hành chuẩn hóa DTO ở Backend trong Sprint 1.
