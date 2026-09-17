---
date: 2026-09-17
---

# Sprint 12: Financial Consistency

## 1. Mục tiêu Sprint (Goal)
Đảm bảo tính nhất quán tài chính (Financial Consistency) giữa dư nợ hiện tại (snapshot `total_debt` trong bảng `receivable_debt`) và lịch sử phát sinh công nợ (append-only ledger log `receivable_debt_movement`).
Ngoài ra, hiển thị chính xác khoản công nợ cũ (oldDebt) trên màn hình tạo Đơn bán hàng, giúp tính toán đúng khoản cần thanh toán khi có khoản trả trước (advancePayment).

## 2. Thành quả đạt được (Done)
- Tạo Flyway script `V17__backfill_opening_balance.sql` để backfill (chèn hồi tố) các bản ghi `OPENING_BALANCE` vào bảng `receivable_debt_movement` đối với những khách hàng đã có số dư công nợ nhưng bị khuyết lịch sử movement (do dữ liệu seed cũ từ Sprint trước).
- Cập nhật hàm `decreaseDebt` trong `ReceivableDebtService` để ghi nhận `amount` ở dạng giá trị tuyệt đối (positive) thay vì âm (negative). Điều này giúp đồng bộ với logic cộng/trừ của Frontend (`isIncrease` quyết định hiển thị `+` hay `-`).
- Bổ sung API endpoint mới `GET /api/v1/receivable-debts/{customerId}/balance` tại `ReceivableDebtController` để lấy dư nợ hiện tại của khách hàng.
- Sinh lại (Generate) Type Definitions cho Frontend thông qua package `@erp/api-contract` sau khi API được cập nhật (`openapi.json`).
- Cập nhật `SalesOrderForm.tsx` ở Frontend để gọi API lấy dư nợ (`ApiService.Debt.getBalance`) thay vì dựa vào trường `currentDebt` không tồn tại của `CustomerResponseDto`.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
Quyết định: Ghi số tiền (amount) trong bảng `receivable_debt_movement` luôn là số dương (absolute value), và sử dụng `movement_type` (như `SALES_INVOICE`, `PAYMENT`, `RETURN`) để xác định chiều tăng hay giảm.
Lý do: Giúp dữ liệu dễ đọc hiểu hơn, thuận tiện hơn cho các hệ thống kế toán bên thứ 3 khi đối soát, đồng thời phù hợp với logic render UI ở màn hình Sao kê (Statement) mà không cần biến đổi dấu thủ công dễ gây nhầm lẫn (như trừ một số âm).
Đã cân nhắc: Lưu số âm với các nghiệp vụ giảm nợ (như Payment) — Bỏ vì dẫn đến rủi ro hiển thị lỗi (`- -200`) trên UI, và việc tính toán tổng các báo cáo có thể phức tạp nếu không quy định rõ ràng ngữ nghĩa của dấu âm.

Quyết định: Thêm endpoint `/balance` riêng để lấy số dư nợ thay vì gộp chung vào `CustomerResponseDto`.
Lý do: Giữ vững Bounded Context. Context Customer (CRM) không nên "ôm" dữ liệu số dư nợ của Context Receivable Debt (Tài chính). UI `SalesOrderForm` hoàn toàn có thể tự gọi thêm 1 request nhỏ để lấy nợ khi cần (vì người dùng chỉ thao tác lấy thông tin này khi chọn Khách hàng).
Đã cân nhắc: Nối bảng (JOIN) và trả về `currentDebt` ngay bên trong API tìm kiếm khách hàng — Bỏ vì phá vỡ ranh giới module đã thiết kế trước đó.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. `V17__backfill_opening_balance.sql`: Xem script hồi tố dữ liệu lịch sử bằng `gen_random_uuid()`.
2. `ReceivableDebtService.java` (hàm `decreaseDebt`): Xem việc bỏ gọi hàm `.negate()` để lưu số dương tuyệt đối.
3. `ReceivableDebtController.java`: Xem API endpoint mới `/balance`.
4. `SalesOrderForm.tsx`: Xem logic gọi `ApiService.Debt.getBalance` trong callback `onSelect` của Customer.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Việc dùng `gen_random_uuid()` trong script V17 yêu cầu PostgreSQL version 13 trở lên. Với môi trường H2 database khi test, script này sẽ bị bỏ qua (do flyway.enabled = false trong test), nhưng có thể gặp vấn đề nếu chạy trên bản PostgreSQL quá cũ ở local dev.

## 6. Việc chưa làm / Out of scope
- Cải thiện UX khi mạng chậm (việc lấy dư nợ cũ tốn thời gian gọi thêm 1 network request). Hiện tại tôi dùng state 0 tạm thời nếu lỗi, chưa có loading indicator cụ thể ngay trên combo-box.

## 7. Cách chạy & Cách verify (Reproduce)
- **Backend:** Chạy `.\mvnw test` để đảm bảo API hoạt động đúng (đã pass 100%).
- **Frontend:** Chạy `npx tsc --noEmit` ở thư mục `apps/erp-frontend` để đảm bảo các thay đổi không gây lỗi type (đã kiểm tra không lỗi).
- Mở Frontend UI, vào màn hình Tạo đơn bán hàng, chọn một Khách hàng đã có công nợ cũ (ví dụ KH-001). Quan sát phần "Dư nợ cũ" sẽ được cập nhật đúng giá trị tự động thay vì bằng 0.

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Với tính nhất quán tài chính được đảm bảo, Ledger hiện tại đã hoàn toàn sẵn sàng cho môi trường Production ở mặt số liệu.
- Sprint 13 (Idempotency/concurrency) sẽ tập trung vào việc ngăn chặn vấn đề nhấp đúp (double click) hay các thao tác đồng thời (race condition) gây ghi sổ kế toán (Ledger) hoặc trừ kho (Inventory) hai lần đối với cùng một giao dịch.
