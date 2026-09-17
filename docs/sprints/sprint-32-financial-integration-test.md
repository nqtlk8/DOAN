# Sprint 32: Financial Integration Test (Sprint 8)

Ngày hoàn thành: 2026-09-17

## 1. Mục tiêu Sprint (Goal)
- Đảm bảo logic kế toán (sổ cái công nợ) hoạt động chính xác từ đầu đến cuối trên database thật PostgreSQL.
- Ngăn ngừa lỗi dữ liệu (Data Regression): `SUM(movement.amount) == receivable_debt.total_debt`.

## 2. Thành quả đạt được (Done)
- Tạo bài test `ReceivableDebtFinancialPostgresIT.java` test toàn bộ vòng đời của công nợ.
- Thực hiện kịch bản:
  - Opening balance: +5m
  - Sales invoice: +10m
  - Payment: -3m
  - Goods return: -2m
- Xác minh bằng Spring Service (`getCurrentDebt` trả về `10m`) và query native trực tiếp vào DB để kiểm chứng Invariant (`SUM(amount) == total_debt`).

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- Quyết định: Cố tình query DB gốc (`SUM(amount)`) thay vì chỉ gọi service.
- Lý do: Service trả về kết quả lấy từ Hibernate L1 Cache. Nếu logic tính toán trên Entity sai sót nhưng vẫn được lưu vào Cache, thì bài test sẽ pass ảo (False Positive). Lấy tổng trực tiếp qua SQL bypass Cache, xác minh dữ liệu thực tế đẩy xuống bảng (PostgreSQL) chính xác 100%.
- Đã cân nhắc: Chỉ verify qua Service — bỏ vì sẽ mất đi ý nghĩa của một Integration Test thực thụ với Database.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
- `ReceivableDebtFinancialPostgresIT.java`: Đọc phương thức `testFinancialConsistencyOverMultipleTransactions` để hiểu chuỗi giao dịch tạo nợ, giảm nợ và cách so sánh Invariant.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Khi thêm nhiều giao dịch kế toán phức tạp khác (chuyển nợ, xóa nợ), bài test này sẽ cần được cập nhật theo để kiểm tra toàn vẹn Invariant.

## 6. Việc chưa làm / Out of scope
- Chưa test luồng Migration nâng cấp Database (Sprint 9).

## 7. Cách chạy & Cách verify (Reproduce)
- `.\mvnw.cmd test -Dtest=ReceivableDebtFinancialPostgresIT`

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Chuyển sang Sprint 9, không test "Fresh Database" nữa, mà sẽ test kịch bản cực kỳ thực tế: Khởi tạo DB rỗng, chạy Flyway tới V(current-1), sau đó nâng cấp lên Vcurrent để bắt các lỗi Backfill và Default Value.
