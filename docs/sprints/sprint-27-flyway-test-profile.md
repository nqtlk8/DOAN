# Sprint 27: Flyway Test Profile cho Integration Tests (Sprint 3)

Ngày hoàn thành: 2026-09-17

## 1. Mục tiêu Sprint (Goal)
- Cấu hình một profile Spring riêng (`postgres-it`) chuyên dùng để chạy Flyway vào database PostgreSQL Testcontainers.
- Đổi cơ chế khởi tạo Schema từ `create-drop` của Hibernate sang `validate` để đảm bảo Flyway là nơi duy nhất tạo schema.

## 2. Thành quả đạt được (Done)
- Đã tạo file `application-postgres-it.yml` tại `services/erp-backend/src/test/resources/`.
- Cấu hình Flyway enabled, URL datasource đọc từ biến môi trường `testcontainers.jdbc.*`.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- Quyết định: Sử dụng `ddl-auto: validate` thay vì `create-drop` trong profile `postgres-it`.
- Lý do: Bắt buộc schema phải được sinh ra từ Flyway migrations (`V1`, `V2`...). Nếu Hibernate tự tạo (`create-drop`), Flyway sẽ bị dư thừa và không kiểm chứng được độ chính xác của các file `.sql` khi deploy Production.
- Đã cân nhắc: Sửa trực tiếp file `application-test.yml` — bỏ vì sẽ làm 133 Fast Tests đang chạy H2 (in-memory) mất ổn định và bị chậm.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
- `services/erp-backend/src/test/resources/application-postgres-it.yml`: Cấu hình profile chứa Flyway config và DB Dialect của PostgreSQL.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Các file Flyway migration (`src/main/resources/db/migration/`) có thể chứa cú pháp native PostgreSQL mà H2 không hiểu, nên profile cũ không chạy được Flyway. Với profile mới, nếu schema thiếu trường so với Hibernate Entity, `validate` sẽ báo lỗi gay gắt.

## 6. Việc chưa làm / Out of scope
- Chưa viết Test Class gọi Flyway thực tế (được thực hiện ở Sprint 4).

## 7. Cách chạy & Cách verify (Reproduce)
- File chỉ chứa cấu hình, sẽ được verify cùng Sprint 4 khi load profile `postgres-it`.

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Sprint 4 sẽ tạo class `FlywayPostgresIntegrationTest` kết hợp Spring Context với `application-postgres-it.yml` để chạy test kiểm chứng Flyway V1 -> Vcurrent có áp dụng thành công lên DB hay không.
