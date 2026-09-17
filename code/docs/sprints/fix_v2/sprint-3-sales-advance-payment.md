# Sprint 3: Sales Advance Payment (Backend)
**Ngày hoàn thành:** 16/09/2026

## 1. Mục tiêu Sprint (Goal)
- Thêm trường `advancePayment` vào API tạo đơn bán hàng để ghi nhận số tiền khách hàng thanh toán trước ngay tại thời điểm tạo đơn.
- Xử lý nghiệp vụ trừ công nợ tương ứng với số tiền trả trước khi xác nhận đơn hàng.

## 2. Thành quả đạt được (Done)
- **Bổ sung entity và DTO:** 
  - Thêm `advancePayment` vào `SalesInvoice` entity và các DTO (`SalesInvoiceCreateDto`, `SalesInvoiceResponseDto`).
- **Xử lý logic công nợ:**
  - Cập nhật `SalesInvoiceService.createDraft` để lưu `advancePayment`.
  - Cập nhật `SalesInvoiceService.confirmInvoice` để tính toán đúng phần công nợ thực tế (`newDebt = currentDebt + totalAmount - advancePayment`) và gọi `ReceivableDebtService.decreaseDebt` với phần đã trả trước.
- **Tạo Database Migration:** 
  - Tạo script Flyway `V14__add_advance_payment_to_sales_invoice.sql` để thêm cột `advance_payment` vào bảng `sales_invoice`.
- **Cập nhật OpenAPI Schema:** 
  - Khởi chạy test gen lại `openapi.json` và chạy lệnh gen code TypeScript ở phía frontend để nhận trường dữ liệu mới.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- **Quyết định:** Giữ nguyên cấu trúc gọi `increaseDebt(totalAmount)` sau đó gọi tiếp `decreaseDebt(advancePayment)` thay vì gộp lại truyền thẳng `totalAmount - advancePayment` vào hàm increase.
- **Lý do:** Điều này chuẩn bị trước cơ sở để dễ dàng chuyển đổi ở Sprint 4 & 5 (khi có Ledger). `increaseDebt` sẽ tương ứng với hóa đơn (Invoice Transaction), và `decreaseDebt` sẽ tương ứng với một phiếu thu (Payment Receipt Transaction). Ghi nhận thành 2 vệt giao dịch tách biệt sẽ giúp việc truy xuất sao kê công nợ rõ ràng hơn so với 1 vệt giao dịch gộp.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. `V14__add_advance_payment_to_sales_invoice.sql`: Cột mới ở database.
2. `SalesInvoiceCreateDto.java` & `SalesInvoiceResponseDto.java`: Trường `advancePayment` mới trên API Contract.
3. `SalesInvoiceService.java` (hàm `confirmInvoice`): Nơi xử lý tính toán và trừ số dư công nợ.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Currently `ReceivableDebtService` uses optimistic locking or simple queries which might face concurrency issues if multiple endpoints update the same customer's debt at the exact same millisecond. (Sẽ được fix triệt để ở Sprint 13).
- Lịch sử thay đổi công nợ (Ledger) hiện tại vẫn chưa được ghi lại, mọi thứ mới chỉ trừ trên số tổng (current balance). Sẽ khắc phục ở Sprint 4 & 5.

## 6. Việc chưa làm / Out of scope
- Frontend UI để người dùng thực sự nhập số tiền trả trước khi tạo đơn chưa được làm (sẽ được implement ở Sprint 10: Sales Payment UI). Hiện tại Frontend chỉ mới có interface TypeScript mới nhất.

## 7. Cách chạy & Cách verify (Reproduce)
- **Bước 1 (Backend):** Chạy `.\mvnw test` để verify toàn bộ 127 tests pass và Flyway script chạy đúng.
- **Bước 2 (API Contract):** Xác minh file `api.d.ts` trong package `api-contract` đã có trường `advancePayment` trong `SalesInvoiceCreateDto`.

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
Backend hiện tại đã hỗ trợ tạo đơn hàng có thanh toán trước và xử lý tổng dư nợ (total balance). Sprint tiếp theo (Sprint 4: Debt ledger schema) sẽ tập trung vào thiết kế cấu trúc database cho "append-only receivable debt ledger" (nhật ký công nợ) nhằm lưu lại từng thay đổi chi tiết, để giải quyết Nợ kỹ thuật ở mục 5.
