# Ngày: 2026-09-17
# Sprint 19: Cross Branch Reconciliation Test

## 1. Mục tiêu Sprint (Goal)
- Viết integration test để xác minh khả năng cô lập (isolation) công nợ của một khách hàng trên nhiều chi nhánh khác nhau.
- Đảm bảo rằng giao dịch phát sinh ở chi nhánh A sẽ không ghi đè, cộng gộp sai, hay làm lệch số dư công nợ của chính khách hàng đó ở chi nhánh B.

## 2. Thành quả đạt được (Done)
- ✅ Đã tạo `CrossBranchReconciliationIT.java`.
- ✅ Kịch bản test:
  1. Tạo 1 khách hàng `CUST-CROSS-01` thuộc `branch_id = 1`.
  2. Ghi nhận mua hàng (INVOICE) 10,000 tại chi nhánh A (`branch_id = 1`).
  3. Ghi nhận mua hàng (INVOICE) 5,000 tại chi nhánh B (`branch_id = 2`).
  4. Ghi nhận thanh toán (PAYMENT) 3,000 tại chi nhánh A (`branch_id = 1`).
- ✅ Kết quả assert tự động xác nhận:
  - Công nợ tại nhánh A = 7,000 (đúng với tính toán 10,000 - 3,000). Có 2 dòng movement cho nhánh A.
  - Công nợ tại nhánh B = 5,000. Có đúng 1 dòng movement cho nhánh B.
  - Tổng số lượng movement trên toàn hệ thống cho khách hàng này là 3. (Sử dụng API truy vấn `findByCustomerIdOrderByCreatedAtDesc` mới thiết lập thêm).

**Các file chính đã thay đổi:**
- [NEW] `services/erp-backend/src/test/java/com/storename/erp/crm/application/CrossBranchReconciliationIT.java`
- [MODIFY] `services/erp-backend/src/main/java/com/storename/erp/crm/infrastructure/ReceivableDebtMovementRepository.java` (Bổ sung hàm `findByCustomerIdOrderByCreatedAtDesc`).

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions)
- **Quyết định:** Sử dụng hàm chuyên biệt `findByCustomerIdOrderByCreatedAtDesc` thay vì `findAll()` filter bằng Java stream.
- **Lý do:** Tăng hiệu năng và đảm bảo code production-ready (việc load toàn bộ bảng `receivable_debt_movement` ra RAM là không thể chấp nhận được ở môi trường thật).

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. Đọc `CrossBranchReconciliationIT.java` để xem kịch bản chia nhánh A và B.
2. Đọc `ReceivableDebtMovementRepository.java` xem query method mới.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Việc xem tổng công nợ trên toàn bộ tất cả chi nhánh (Consolidated View) hiện tại phải query toàn bộ phong trào (movements) hoặc query trên tất cả `ReceivableDebt` records của khách hàng đó. Backend chưa có DTO gom nhóm này cho UI cấp HQ. Đây là hạng mục UI cần hoàn thiện thêm (nếu business requirement yêu cầu HQ được xem 1 con số tổng duy nhất thay vì split theo branch).

## 6. Việc chưa làm / Out of scope
- API báo cáo Consolidated View cho HQ chưa được hiện thực hóa ở sprint này vì nó không ảnh hưởng đến toàn vẹn giao dịch (core integrity) vốn là mục tiêu chính của audit.

## 7. Cách chạy & Cách verify (Reproduce)
Chạy lệnh Maven sau tại thư mục `services/erp-backend`:
```bash
./mvnw test -Dtest=CrossBranchReconciliationIT
```
**Kết quả mong đợi:** 
Bài test pass (chứng minh tính độc lập của chi nhánh).

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Hoàn tất Phase 7 (Heavy Integration Testing). Toàn bộ Ledger Core đã vững chãi.
- Tiếp theo là Phase 8 (Final Cleanup & E2E) với các tác vụ dọn dẹp N+1 Query, chốt lại Boundary và regression test lần cuối.
