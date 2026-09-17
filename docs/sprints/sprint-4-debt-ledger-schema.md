# Sprint 4: Debt Ledger Schema
**Ngày hoàn thành:** 16/09/2026

## 1. Mục tiêu Sprint (Goal)
- Xây dựng thiết kế dữ liệu (Schema) và cấu trúc Entity cho sổ cái công nợ (Debt Ledger).
- Đây là bước chuẩn bị (Foundation) để lưu trữ mọi biến động công nợ dưới dạng Append-Only log (immutable ledger).

## 2. Thành quả đạt được (Done)
- **Thiết kế Database Migration:** 
  - Tạo script Flyway `V15__create_receivable_debt_movement.sql`.
  - Bảng `receivable_debt_movement` với các thông tin: `id`, `branch_id`, `customer_id`, `movement_type`, `amount`, `balance_after`, `ref_type`, `ref_id`, `created_at`, `created_by`, `note`.
  - Composite index trên `(customer_id, branch_id)` để tra cứu nhanh toàn bộ lịch sử công nợ của 1 khách hàng.
  - Composite index trên `(ref_type, ref_id)` để đảm bảo idempotency và tra cứu ngược về chứng từ gốc.
- **Tạo Entity JPA tương ứng:**
  - `ReceivableDebtMovement.java`: Map trực tiếp vào bảng mới.
  - `ReceivableDebtMovementType.java`: Enum định nghĩa các loại biến động (`INVOICE`, `PAYMENT`, `RETURN`, `ADJUSTMENT`).
- **Verify System:** Toàn bộ 127 tests của backend đã pass thành công sau khi chạy Migration.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- **Quyết định:** Sử dụng thiết kế Append-Only với trường `balance_after` lưu ở mỗi dòng (snapshotting).
- **Lý do:** Giúp việc đối soát và chốt sổ nhanh chóng. Thay vì mỗi lần lấy công nợ phải sum toàn bộ log từ đầu (Rất chậm khi dữ liệu lớn), hệ thống chỉ cần đọc dòng log mới nhất để lấy được số dư. Bảng này không được phép UPDATE hay DELETE.
- **Đã cân nhắc:** Không lưu `balance_after` mà chỉ tính toán (On-the-fly calculation) -> Bỏ qua vì làm giảm hiệu năng hệ thống khi tra cứu công nợ thời gian thực.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. `V15__create_receivable_debt_movement.sql`: File chứa thiết kế schema bảng ledger và sự điều chỉnh thêm `branch_id` cho customer.
2. `ReceivableDebtMovement.java`: Entity mapping.
3. `ReceivableDebtMovementType.java`: Định nghĩa phân loại giao dịch sổ cái.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- **Tenant Isolation cho Customer:** Master data ở HQ có tất cả customer, còn ở branch chỉ nên thấy customer của họ. Nợ kỹ thuật này đã được ghi nhận nhưng tạm thời chưa xử lý ở Sprint này (Hoàn tác việc thêm `branch_id`), sẽ được giải quyết sau.
- Code hiện tại mới chỉ là Schema. Backend service chưa insert dữ liệu vào bảng này khi có biến động.

## 6. Việc chưa làm / Out of scope
- Chưa tích hợp việc tự động ghi nhận (Insert) vào sổ cái mỗi khi đơn bán, thanh toán, hoặc trả hàng diễn ra. (Đây là mục tiêu chính của Sprint 5).

## 7. Cách chạy & Cách verify (Reproduce)
- Chạy `.\mvnw test` ở `services/erp-backend`. Hệ thống sẽ tự động build Hibernate entities và chạy Flyway V15, nếu cấu trúc Entity và SQL match, test sẽ pass màu xanh.

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Entity và Database đã sẵn sàng. Ở Sprint 5 (Ledger Integration), hệ thống cần refactor hàm `increaseDebt` và `decreaseDebt` của `ReceivableDebtService` để khi số dư tổng thay đổi, một bản ghi `ReceivableDebtMovement` sẽ đồng thời được persist trong cùng 1 database transaction.
