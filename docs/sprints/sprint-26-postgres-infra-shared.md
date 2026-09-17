# Sprint 26: PostgreSQL Container Infrastructure Shared (Sprint 2)

Ngày hoàn thành: 2026-09-17

## 1. Mục tiêu Sprint (Goal)
- Tạo lớp cơ sở chung (`abstract class`) để quản lý lifecycle của PostgreSQL Testcontainer cho tất cả các Integration Tests.
- Đảm bảo Container chỉ khởi tạo một lần (Singleton pattern cho container) để tăng tốc độ chạy test.

## 2. Thành quả đạt được (Done)
- Đã tạo class `PostgresIntegrationTest` tại `services/erp-backend/src/test/java/com/storename/erp/test/PostgresIntegrationTest.java`.
- Sử dụng `@DynamicPropertySource` để map động các tham số cấu hình (host, port, DB, user, password) do Testcontainers sinh ra vào các properties `testcontainers.jdbc.*`.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- Quyết định: Sử dụng thuộc tính `static` cho `PostgreSQLContainer` kèm theo annotation `@Container`.
- Lý do: Đảm bảo Spring Boot Test Context có thể share cùng 1 container qua nhiều test class, giảm thiểu chi phí (overhead) khởi động và tắt Docker container liên tục.
- Đã cân nhắc: Khởi tạo container trong từng file test — bỏ vì sẽ làm thời gian chạy Integration tests cực kỳ chậm (mỗi class tốn thêm 5-10 giây khởi động DB).

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
- `PostgresIntegrationTest.java`: Xem cách cấu hình abstract class, chú ý method `registerPostgresProperties` dùng để tiêm dynamic properties vào Spring Environment.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Các biến `testcontainers.jdbc.*` cần được cấu hình vào file `application-test.yml` hoặc profile test riêng để Spring DataSource đọc. Điều này sẽ thực hiện ở Sprint 3.

## 6. Việc chưa làm / Out of scope
- Chưa tạo Test Profile chạy Flyway thật (thuộc Sprint 3).

## 7. Cách chạy & Cách verify (Reproduce)
- Hiện tại là abstract class nên chưa chạy trực tiếp được. Các test extends class này sẽ được verify sau.

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Sprint 3 sẽ tạo profile riêng `application-postgres-it.yml` để kích hoạt Flyway và map URL DB vào datasource thật, sau đó cấu hình `ddl-auto=validate`.
