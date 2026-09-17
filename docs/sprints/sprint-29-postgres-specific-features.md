# Sprint 29: PostgreSQL-specific Features (Sprint 5)

Ngày hoàn thành: 2026-09-17

## 1. Mục tiêu Sprint (Goal)
- Đảm bảo các function/extension native của PostgreSQL (như sinh UUID) hoạt động bình thường qua Flyway.
- Chứng minh được Testcontainers bắt được lỗi mà H2 (in-memory) không thể bắt, cụ thể là việc quên không cài extension (`uuid-ossp`) trong script migration.

## 2. Thành quả đạt được (Done)
- Tạo bài test `PostgresSpecificFeaturesIT.java`.
- Viết test `testUuidGenerateV4FunctionExists` để query native function `uuid_generate_v4()` của PostgreSQL.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- Quyết định: Cố tình query trực tiếp hàm `uuid_generate_v4()` bằng native SQL (JdbcTemplate) trong quá trình test.
- Lý do: Mặc dù ứng dụng có thể tạo UUID từ Java (`UUID.randomUUID()`), nhiều logic migration cũ hoặc default value trên bảng yêu cầu hàm sinh UUID ở tầng Database. H2 thường mock hàm này hoặc bỏ qua, nhưng PostgreSQL thật sẽ throw Exception ngay lập tức nếu thiếu câu lệnh `CREATE EXTENSION IF NOT EXISTS "uuid-ossp";` trong file Flyway. Bài test này ngăn chặn developer quên extension.
- Đã cân nhắc: Bỏ qua extension để dễ test — không thể chấp nhận, Flyway Testcontainers sinh ra chính là để test những case sát phần cứng / CSDL thật này.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
- `PostgresSpecificFeaturesIT.java`: Kiểm tra cách lấy string UUID trực tiếp từ PostgreSQL và parse qua `UUID.fromString()`.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Các file migration (`V1...`) nếu chưa có dòng `CREATE EXTENSION...` thì test này sẽ hỏng, yêu cầu dev sửa lại script migration. (Vì không có Docker nên chưa chạy thử).

## 6. Việc chưa làm / Out of scope
- Chưa test Ledger (Nghiệp vụ tài chính) của các Table.

## 7. Cách chạy & Cách verify (Reproduce)
- Chạy lệnh: `.\mvnw.cmd test -Dtest=PostgresSpecificFeaturesIT`
- Kết quả mong đợi: Trả về UUID hợp lệ nếu migration setup chuẩn xác.

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Chuyển sang Sprint 6 để Validate Ledger Database (schema, unique constraint, idempotency).
