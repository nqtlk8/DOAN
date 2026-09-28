# Sprint 10: Sales Payment UI
**Ngày hoàn thành:** 16/09/2026

## 1. Mục tiêu Sprint (Goal)
- Kết nối trường "Trả trước" (`advancePayment`) từ giao diện tạo Đơn bán hàng (Sales Order Form) xuống API Backend.
- Đảm bảo khi tạo đơn hàng, số tiền trả trước được truyền đúng vào Request Payload để Backend có thể tự động cấn trừ công nợ và ghi log sao kê (SALES_PAYMENT) khi xác nhận đơn.

## 2. Thành quả đạt được (Done)
- Rà soát file `SalesOrderForm.tsx`: Trường nhập liệu "Trả trước" và hook state (`advancePayment`) đã có sẵn trên UI (component `OrderFinancialSummary`).
- Cập nhật hàm xử lý submit form: Bổ sung field `advancePayment` vào đối tượng `payload` để truyền đúng định dạng `SalesInvoiceCreateDto`.
- Compile frontend pass 100%.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- **Quyết định:** Sử dụng trực tiếp trường `advancePayment` của component tạo đơn hàng thay vì xây dựng một trang "Thanh toán (Payment)" riêng biệt.
- **Lý do:** Đối với luồng Sale của chi nhánh hiện tại, quy trình thanh toán trả trước (tiền mặt/chuyển khoản lúc mua) thường xảy ra cùng lúc với việc lên đơn hàng. Chèn thẳng vào form giúp nhân viên tiết kiệm thao tác. Việc thanh toán riêng lẻ (thu nợ cũ) sẽ được phát triển ở module Payment riêng trong tương lai (nếu cần).

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. `apps/erp-frontend/src/components/sales/SalesOrderForm.tsx`: Xem dòng cấu trúc object `payload` đã được bổ sung thuộc tính `advancePayment`.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Frontend hiện chỉ map field, chưa validate chặt chẽ xem `advancePayment` có lớn hơn tổng giá trị đơn hàng hay không. Backend hiện đang xử lý tính toán số dư công nợ nên chưa rủi ro về Data, nhưng trải nghiệm UX có thể chưa hoàn hảo nếu nhân viên nhập nhầm số tiền lớn hơn thực tế.

## 6. Việc chưa làm / Out of scope
- Chưa tích hợp Payment Gateway (Thanh toán thẻ/Ví điện tử). Mọi khoản trả trước hiện đang giả định là Thu tiền mặt (CASH) hoặc chuyển khoản thủ công.

## 7. Cách chạy & Cách verify (Reproduce)
- `npm run dev` ở frontend. Mở màn hình tạo đơn bán hàng.
- Thêm sản phẩm, điền số tiền vào ô "Trả trước" và bấm lưu đơn.
- Kiểm tra lại trong Tab "Công nợ" -> Mở sao kê của Khách hàng, sẽ thấy cả 2 dòng: `Bán hàng (Tăng nợ)` và `Thanh toán/Trả trước (Giảm nợ)` (nếu đơn hàng được bấm Xác nhận).

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Kế hoạch triển khai mã nguồn liên quan đến UI đã cơ bản hoàn thiện trơn tru (Danh sách Sales, Nhập kho, Công nợ).
- Sprint 11 (Admin/Staff regression) sẽ tiến hành review và test phân quyền hệ thống để đảm bảo người dùng có vai trò `STAFF` chỉ thấy data của chi nhánh họ, và `ADMIN` có thể thấy tổng quan (Hoặc tuân thủ luật Multi-tenant/Branch isolation).
