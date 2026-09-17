# Sprint 28: Flyway Migration Integration Test (Sprint 4)

Ngày hoàn thành: 2026-09-17

## 1. Mục tiêu Sprint (Goal)
- Viết test case cực kỳ quan trọng để kiểm chứng luồng: Spring Boot khởi động -> Testcontainers chạy -> Flyway chạy toàn bộ script -> Hibernate Validate Schema thành công.
- Xác minh tính toàn vẹn của lịch sử Flyway và sự tồn tại của các bảng dữ liệu cốt lõi (Core Tables).

## 2. Thành quả đạt được (Done)
- Đã tạo `FlywayPostgresIntegrationTest.java` extends `PostgresIntegrationTest`.
- Sử dụng `@SpringBootTest` và `@ActiveProfiles("postgres-it")` để gọi Spring Context thật.
- Viết 3 test:
  - `testFlywayMigrationSuccessAndHibernateValidation`: Kiểm chứng Spring Context load thành công, suy ra Flyway & Hibernate hợp lệ.
  - `testFlywaySchemaHistory`: Đọc bảng `flyway_schema_history` qua `JdbcTemplate` để đảm bảo mọi migration record đều có `success = true`.
  - `testRequiredTablesExist`: Check `information_schema.tables` đảm bảo các bảng cốt lõi của hệ thống ERP đã được tạo.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- Quyết định: Sử dụng `JdbcTemplate` để truy vấn schema meta-data thay vì dùng JPA Entity.
- Lý do: Truy vấn cấu trúc DB (Information schema, flyway history) không thuộc phạm vi quản lý của JPA/Hibernate Entities (không có Entity nào map tới `flyway_schema_history`). Native query qua `JdbcTemplate` là trực tiếp và an toàn nhất.
- Đã cân nhắc: Kiểm tra từng cột trong bảng (Columns inspection) — bỏ vì sẽ được cover kỹ càng hơn bởi tính năng Hibernate Schema Validate (`ddl-auto: validate`). Nếu cột sai type, Hibernate sẽ ném exception khi khởi động, làm failed bài Test 1.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
- `FlywayPostgresIntegrationTest.java`: Xem lần lượt 3 test. Cần chú ý test 1 rỗng (chỉ check container running) vì bản thân việc Spring Boot không crash khi khởi động (load context) đã chứng minh Flyway + Hibernate Validate thành công.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Các file Flyway nếu có sử dụng extension của PostgreSQL (như `uuid-ossp`) thì cần phải test thêm để đảm bảo extension được enable đúng.
- Test chưa thể chạy xanh trên môi trường thiếu Docker Daemon.

## 6. Việc chưa làm / Out of scope
- Chưa test trực tiếp các extension đặc thù của Postgres (như UUID) (Sprint 5).

## 7. Cách chạy & Cách verify (Reproduce)
- Chạy lệnh: `.\mvnw.cmd test -Dtest=FlywayPostgresIntegrationTest`
- Kết quả mong đợi: `BUILD SUCCESS`.

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Sprint 5 sẽ focus vào viết test chuyên kiểm tra các PostgreSQL-specific features (như UUID functions) mà H2/in-memory database không bao giờ có thể xác minh được.
