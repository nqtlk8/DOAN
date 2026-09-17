# Sprint 31: Repository Integration Test (Sprint 7)

Ngày hoàn thành: 2026-09-17

## 1. Mục tiêu Sprint (Goal)
- Chuyển tiếp từ kiểm tra schema SQL thuần (Sprint 6) sang kiểm tra qua tầng JPA / Hibernate (Repository).
- Đảm bảo JPA mapping với database thật tương thích, lưu và lấy dữ liệu chuẩn, và cơ chế unique idempotency hoạt động thông qua `saveAndFlush`.

## 2. Thành quả đạt được (Done)
- Tạo bài test `ReceivableDebtMovementRepositoryIT.java` nằm trong `com.storename.erp.crm.infrastructure`.
- Kế thừa `PostgresIntegrationTest` để dùng Testcontainers.
- Test `testSaveAndFindMovement`: Lưu movement qua repository, đảm bảo BigDecimal mapping đúng với PostgreSQL `precision=19, scale=4`.
- Test `testIdempotencyUniqueConstraintViaRepository`: Gọi `saveAndFlush` lần 2 với cùng `idempotencyKey` để bắt ngoại lệ `DataIntegrityViolationException` từ Hibernate đẩy lên.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- Quyết định: Sử dụng `saveAndFlush` thay vì `save` trong test.
- Lý do: Với `@Transactional` trong test, Hibernate chỉ cache object vào session (First-level cache). Nếu gọi `save` mà không `flush`, Hibernate có thể chưa phát SQL Insert xuống database, dẫn đến việc không kích hoạt được DB Constraint. Dùng `saveAndFlush` ép SQL chạy ngay lập tức.
- Đã cân nhắc: Bỏ `@Transactional` — bỏ vì muốn rollback lại dữ liệu sau mỗi test để không làm dơ DB (dirty data) ảnh hưởng test khác.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
- `ReceivableDebtMovementRepositoryIT.java`: Lưu ý phần `setUp()` dùng SQL thuần để chèn Customer dummy nhằm thỏa mãn foreign key, do việc khởi tạo object Customer theo luồng chuẩn đôi khi khá phức tạp.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Phụ thuộc vào setup Customer, nếu schema Customer thay đổi (thêm field `NOT NULL`) có thể làm fail test. (Đã dùng raw SQL insert tối giản nhất).

## 6. Việc chưa làm / Out of scope
- Chưa kiểm tra luồng nghiệp vụ end-to-end (sẽ làm ở Sprint 8).

## 7. Cách chạy & Cách verify (Reproduce)
- `.\mvnw.cmd test -Dtest=ReceivableDebtMovementRepositoryIT`

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Sprint 8 sẽ nâng mức kiểm thử lên tầng Application/Service (Financial Integration Test) thay vì chỉ ở mức Repository, kiểm tra tính toàn vẹn của logic cập nhật Sổ Cái.
