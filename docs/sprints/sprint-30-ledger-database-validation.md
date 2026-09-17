# Sprint 30: Validate Ledger Database (Sprint 6)

Ngày hoàn thành: 2026-09-17

## 1. Mục tiêu Sprint (Goal)
- Xác thực cấu trúc bảng Ledger (sổ cái nợ) `receivable_debt_movement` được Flyway tạo đúng theo yêu cầu.
- Đảm bảo cơ sở dữ liệu thực sự bảo vệ được tính idempotent thông qua `Unique Constraint` ở database level.

## 2. Thành quả đạt được (Done)
- Đã tạo `ReceivableDebtLedgerPostgresIT.java`.
- Xác minh sự tồn tại của bảng `receivable_debt_movement`.
- Kiểm tra danh sách các cột bắt buộc: `id`, `customer_id`, `branch_id`, `movement_type`, `amount`, `balance_before`, `balance_after`, `ref_type`, `ref_id`, `idempotency_key`, `created_at`.
- Kiểm tra constraint idempotency key bằng native query (insert hai bản ghi cùng key để bắt ngoại lệ `DataIntegrityViolationException`).

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- Quyết định: Sử dụng native query qua `JdbcTemplate` để test DB Unique Constraint.
- Lý do: Java `@Transactional` và Application level checks không thể thay thế được DB unique constraint khi xử lý concurrent request (race condition). DB là chốt chặn cuối cùng. Test bằng Testcontainers đảm bảo constraint này tồn tại ở mức Schema (PostgreSQL), điều mà H2 đôi khi pass êm đềm nếu config sai.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
- `ReceivableDebtLedgerPostgresIT.java`: Chú ý method `testIdempotencyKeyUniqueConstraint` đã thêm xử lý insert dummy FK cho Branch/Customer trước khi insert Movement để tránh lỗi Foreign Key, sau đó kiểm thử chặn 2 movement cùng key.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Lược đồ thực tế nếu có khác biệt tên cột (ví dụ `movement_type` thay bằng kiểu Enum khác) có thể cần điều chỉnh lại.

## 6. Việc chưa làm / Out of scope
- Tương tác qua Spring Data JPA Repository (Sprint 7).

## 7. Cách chạy & Cách verify (Reproduce)
- `.\mvnw.cmd test -Dtest=ReceivableDebtLedgerPostgresIT`
- Kết quả mong đợi: `BUILD SUCCESS` (nếu có Docker).

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Tiến đến Sprint 7, sẽ chuyển sang kiểm thử mức độ tích hợp của `Repository` (JPA/Hibernate) với DB thay vì chỉ query SQL Native.
