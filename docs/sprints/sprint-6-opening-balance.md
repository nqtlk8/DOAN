# Sprint 6: Opening balance
**Ngày hoàn thành:** 16/09/2026

## 1. Mục tiêu Sprint (Goal)
- Hỗ trợ cập nhật số dư đầu kỳ (Opening balance) cho khách hàng để phục vụ việc chuyển đổi dữ liệu từ hệ thống cũ hoặc seed data.
- Đảm bảo khi set số dư đầu kỳ, sẽ sinh ra một Movement Type là `OPENING_BALANCE` tương ứng trong Ledger để bảo toàn tính minh bạch (append-only) của luồng công nợ.

## 2. Thành quả đạt được (Done)
- Thêm enum `OPENING_BALANCE` vào `ReceivableDebtMovementType`.
- Bổ sung logic hàm `setOpeningBalance()` trong `ReceivableDebtService`:
  - Hàm sẽ kiểm tra nếu đã tồn tại bất kỳ Movement nào của khách hàng tại branch thì chặn lại (ném `RuntimeException`) để ngăn ngừa sửa số dư đầu kỳ sau khi đã có giao dịch phát sinh.
  - Cập nhật số dư `totalDebt` vào bảng `receivable_debt`.
  - Sinh record `OPENING_BALANCE` vào bảng `receivable_debt_movement`.
- Bổ sung DTO `OpeningBalanceRequestDto`.
- Cung cấp API `POST /api/v1/receivable-debts/opening-balance` trong `ReceivableDebtController`.
- Cập nhật và pass Unit Test 100% (129/129 tests).

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- **Quyết định:** Chỉ cho phép gọi `setOpeningBalance` nếu khách hàng CHƯA có bất kỳ movement nào.
- **Lý do:** Số dư đầu kỳ chỉ có ý nghĩa trước khi có giao dịch. Nếu đã có hóa đơn/thanh toán mà hệ thống lại chèn số dư đầu kỳ thì sẽ gây sai lệch tính chất append-only của toàn bộ Ledger về sau. 

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. `ReceivableDebtMovementType.java`: Xem type `OPENING_BALANCE`.
2. `ReceivableDebtService.java` (Hàm `setOpeningBalance`): Xem logic kiểm tra và lưu Movement.
3. `ReceivableDebtController.java`: Xem API endpoint.
4. `ReceivableDebtServiceTest.java`: Xem cách Unit test xác thực logic chặn ghi đè opening balance.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Endpoint `/opening-balance` hiện đang lấy branch ID qua context (`AuthUtils.getBranchIdOrNull()`). Việc đồng bộ hóa dữ liệu từ hệ thống HQ xuống Branch có thể cần script gọi API này ở quy mô lớn thay vì từng request đơn lẻ.

## 6. Việc chưa làm / Out of scope
- Chưa tích hợp giao diện frontend cho chức năng "Nhập số dư đầu kỳ" bằng Excel. Frontend sẽ sử dụng API này trong một sprint tới (Sprint 8: Ledger UI).

## 7. Cách chạy & Cách verify (Reproduce)
- Chạy `.\mvnw test`. Toàn bộ test pass chứng minh endpoint và validation hoạt động bình thường.
- Có thể dùng Postman gọi `POST /api/v1/receivable-debts/opening-balance` với token ADMIN và body `{"customerId": "uuid", "amount": 5000, "note": "Migrate data"}`.

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- API Core cho Ledger đã gần hoàn thiện.
- Sprint 7 (Movement API) sẽ tập trung vào việc tạo GET API liệt kê (List) toàn bộ Movement của một khách hàng, hỗ trợ filter và pagination để Frontend có thể hiển thị bảng đối soát công nợ.
