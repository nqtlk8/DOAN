# Sprint 34: Hibernate Schema Validation (Sprint 11)

Ngày hoàn thành: 2026-09-17

## 1. Mục tiêu Sprint (Goal)
- Minh chứng bằng test độc lập rằng Hibernate Validator (`ddl-auto: validate`) đang hoạt động hiệu quả.
- Bảo vệ schema: Nếu DB thiếu cột so với Java Entity (do quên viết Flyway migration), bài test sẽ crash ngay lúc Spring khởi động.

## 2. Thành quả đạt được (Done)
- Đã tạo `SchemaCompatibilityIT.java`.
- Sử dụng profile `postgres-it` với `ddl-auto: validate`.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- Quyết định: Kế thừa tính năng validate của Hibernate lúc khởi động context thay vì viết tool so sánh schema thủ công.
- Lý do: Hibernate `SchemaValidator` là công cụ trưởng thành, có khả năng so sánh cả kiểu dữ liệu (data types), các khóa ngoại (foreign keys) và khóa chính (primary keys).
- Đã cân nhắc: Viết JDBC code đọc `information_schema.columns` để so sánh với Java Reflection — bỏ vì tốn thời gian và dễ lỗi hơn dùng Hibernate Validator có sẵn.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
- `SchemaCompatibilityIT.java`: Chú ý đây là test rỗng (nhưng đầy quyền năng), chỉ kiểm tra `postgres.isRunning()`. Giá trị của bài test nằm ở Phase Context Loading của Spring Boot.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Hibernate `validate` đôi khi báo lỗi sai nếu dùng các custom type mapping phức tạp của PostgreSQL (ví dụ JSONB), cần chú ý dùng annotation `@JdbcTypeCode` chuẩn trong entity để validator không phàn nàn.

## 6. Việc chưa làm / Out of scope
- Sprint 12 (CI/CD pipeline config) không xử lý trong repository này vì thuộc về DevOps (nhưng document sẽ được tạo).

## 7. Cách chạy & Cách verify (Reproduce)
- `.\mvnw.cmd test -Dtest=SchemaCompatibilityIT`

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Hoàn thành bộ Test DB Integration. Chuyển giao sang Sprint 12 để thiết lập CI/CD script (tài liệu).
