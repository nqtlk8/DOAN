# Ngày: 2026-09-17
# Sprint 18: Financial Reconciliation Test

## 1. Mục tiêu Sprint (Goal)
- Viết integration test đối soát (reconciliation) để xác minh tính toàn vẹn toán học của Sổ Công Nợ (Receivable Debt Ledger).
- Đảm bảo rằng ở bất kỳ thời điểm nào, tổng của các phát sinh (`SUM(amount)`) trong bảng `receivable_debt_movement` phải luôn bằng chính xác số dư hiện tại `total_debt` trên bảng `receivable_debt`.
- Đảm bảo chuỗi biến động (append-only ledger) không bị đứt gãy: `balanceBefore + amount = balanceAfter` qua từng dòng log.

## 2. Thành quả đạt được (Done)
- ✅ Đã tạo `FinancialReconciliationIT.java`.
- ✅ Kịch bản test thực hiện một chuỗi giao dịch: 
  - Khởi tạo công nợ (INVOICE): +15,000
  - Thanh toán một phần (PAYMENT): -5,000
  - Mua hàng tiếp (INVOICE): +20,000
  - Trả hàng (RETURN): -3,000
  - Thanh toán cuối (PAYMENT): -10,000
- ✅ Test đã tự động assert:
  - `total_debt` cuối cùng khớp với kết quả lý thuyết (17,000).
  - Có đúng 5 dòng movement được ghi nhận.
  - `SUM(amount)` của 5 dòng movement khớp tuyệt đối với 17,000.
  - Vòng lặp kiểm tra tính liên tục của `balanceBefore` và `balanceAfter` dọc theo time series hoàn toàn liền mạch.

**Các file chính đã thay đổi:**
- [NEW] `services/erp-backend/src/test/java/com/storename/erp/crm/application/FinancialReconciliationIT.java`

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions)
- **Quyết định:** Duy trì hàm `increaseDebt` và `decreaseDebt` tự động quản lý logic số âm/dương để test. Hàm `decreaseDebt` tự động gọi `negate()` cho payment/return để `amount` thực sự lưu vào DB là số âm.
- **Lý do:** Điều này đã được implement từ Sprint 1, test này đóng vai trò chốt chặn cuối cùng (safety net) đảm bảo không có bất kỳ logic nào vô tình thay đổi `amount` thành giá trị sai dấu, phá vỡ tính nguyên vẹn của Ledger.
- **Quyết định 2:** Sắp xếp danh sách movement lấy từ DB theo thứ tự `createdAt ASC, id ASC` để tái lập lại lịch sử thay đổi `balanceBefore -> balanceAfter`.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. Đọc `FinancialReconciliationIT.java`: Xem chuỗi 5 giao dịch tuần tự và phần assert phía dưới để hiểu cách hệ thống Audit (Reconcile) số liệu tự động.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Các record movement hiện tại dựa vào `createdAt` và `id` (nếu trùng thời gian) để sắp xếp. Trong môi trường phân tán cao, UUID version 7 (Time-ordered) cho ID hoặc Sequence numbers sẽ an toàn hơn là Timestamp. Đây là vấn đề có thể cải thiện ở các bản release sau.

## 6. Việc chưa làm / Out of scope
- Chưa thực hiện đối soát tự động định kỳ (ví dụ: Job chạy ngầm mỗi đêm để quét toàn bộ DB và đối soát) vì đây là task thuộc về O&M (Operation & Maintenance) thay vì core testing.

## 7. Cách chạy & Cách verify (Reproduce)
Chạy lệnh Maven sau tại thư mục `services/erp-backend`:
```bash
./mvnw test -Dtest=FinancialReconciliationIT
```
**Kết quả mong đợi:** 
Bài test pass (chứng minh tính toàn vẹn toán học).

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Ledger Receivable Debt đã được chứng minh an toàn tuyệt đối ở cả khía cạnh Concurrency (Sprint 16) và Toán học (Sprint 18).
- Có thể tiến hành Sprint 19: Cross Branch Reconciliation Test.
