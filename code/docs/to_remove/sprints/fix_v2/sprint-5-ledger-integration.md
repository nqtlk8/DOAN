# Sprint 5: Ledger Integration
**Ngày hoàn thành:** 16/09/2026

## 1. Mục tiêu Sprint (Goal)
- Kết nối logic ghi Sổ cái công nợ (Debt Ledger) vào các luồng tăng/giảm công nợ hiện tại.
- Đảm bảo mỗi khi công nợ khách hàng thay đổi, sẽ có một dòng log (movement) được chèn vào database với đầy đủ thông tin tham chiếu.

## 2. Thành quả đạt được (Done)
- **Tạo Repository:** Tạo `ReceivableDebtMovementRepository.java` cho bảng `receivable_debt_movement`.
- **Refactor `ReceivableDebtService`:**
  - Bổ sung `ReceivableDebtMovementRepository`.
  - Cập nhật hàm `increaseDebt` và `decreaseDebt`: Thêm tham số `ReceivableDebtMovementType`, `refType`, `refId`, `userId`, `note`.
  - Mỗi khi gọi tăng/giảm công nợ, tạo 1 Entity `ReceivableDebtMovement` mang số tiền thay đổi (`amount`), số dư sau thay đổi (`balanceAfter`), thông tin người tạo và lưu lại. Hàm `decreaseDebt` lưu giá trị `amount` dạng số âm (`amount.negate()`) để phản ánh đúng dòng tiền ra/vào.
- **Cập nhật Callers (Nơi gọi Service):**
  - Cập nhật `SalesInvoiceService.confirmInvoice` truyền `INVOICE` khi tăng nợ và `PAYMENT` khi khách trả trước.
  - Cập nhật `GoodsReturnService.confirmReturn` truyền `RETURN` khi khách trả hàng.
  - Sửa và pass toàn bộ Unit Tests liên quan (`ReceivableDebtServiceTest`, `SalesInvoiceServiceTest`, `GoodsReturnServiceTest`).
  - Sửa các luồng Integration Tests để truyền mock value tương ứng (`GoodsReturnWithInvoiceTest`, v.v.).

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- **Quyết định:** Ghi nhận `amount` trong `movement` mang dấu (+ hoặc -). `increaseDebt` lưu số dương, `decreaseDebt` lưu số âm (`amount.negate()`).
- **Lý do:** Giúp việc query tổng tiền phát sinh Nợ/Có theo từng loại (INVOICE, PAYMENT) nhanh chóng bằng `SUM(amount)`.
- **Quyết định:** Gắn Ledger insert trực tiếp vào `increaseDebt`/`decreaseDebt` thay vì dùng Domain Events.
- **Lý do:** Đảm bảo ACID và tính nhất quán dữ liệu mà không cần setup phức tạp (như Spring Events có thể bị miss nếu không dùng TransactionalEventListener đúng cách).

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. `ReceivableDebtMovementRepository.java`: Nơi lưu trữ.
2. `ReceivableDebtService.java`: Hàm `increaseDebt`, `decreaseDebt` và `recordMovement`.
3. `SalesInvoiceService.java`, `GoodsReturnService.java`: Nơi phát sinh giao dịch.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Các Transaction cũ chưa có `receivable_debt_movement` (do script seed data V13 không tạo movement). Sẽ cần một tính năng (hoặc sprint) để tạo "Opening Balance" cho các khoản nợ cũ. (Mục tiêu của Sprint 6).

## 6. Việc chưa làm / Out of scope
- Chưa cung cấp API cho Frontend xem danh sách lịch sử công nợ (Movement API - Sprint 7).

## 7. Cách chạy & Cách verify (Reproduce)
- Chạy `.\mvnw test`. Toàn bộ test pass chứng minh luồng transaction và Entity mapping hoạt động hoàn hảo.

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Ledger đã có thể lưu log. Sprint 6 tiếp theo (Opening balance) sẽ tập trung vào việc tạo Movement type "OPENING_BALANCE" để chốt lại số dư cho các khoản nợ trước khi triển khai sổ cái.
