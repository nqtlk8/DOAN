# Sprint 25: Testcontainers Dependencies (Sprint 1)

Ngày hoàn thành: 2026-09-17

## 1. Mục tiêu Sprint (Goal)
- Tích hợp Testcontainers và PostgreSQL module vào dự án Spring Boot hiện tại.
- Viết Smoke Test để kiểm tra việc khởi tạo container và kết nối đến PostgreSQL.

## 2. Thành quả đạt được (Done)
- Đã thêm dependencies `spring-boot-testcontainers`, `junit-jupiter`, `postgresql` (của `org.testcontainers`) vào `services/erp-backend/pom.xml`.
- Đã tạo file `PostgresContainerSmokeTest.java` (`services/erp-backend/src/test/java/com/storename/erp/test/PostgresContainerSmokeTest.java`) thực hiện khởi chạy `postgres:16-alpine` và chạy `SELECT version()`.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- Quyết định: Sử dụng image `postgres:16-alpine` thay vì `latest` cho Testcontainers.
- Lý do: Đảm bảo tương thích với phiên bản Production (PostgreSQL 16) và giảm dung lượng tải image qua bản alpine.
- Đã cân nhắc: Dùng `latest` — bỏ vì sẽ dẫn tới rủi ro khác biệt schema và function giữa Test và Production.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
- `services/erp-backend/pom.xml`: Kiểm tra phần khai báo thư viện Testcontainers được thêm vào.
- `services/erp-backend/src/test/java/com/storename/erp/test/PostgresContainerSmokeTest.java`: Đọc đoạn code khởi tạo PostgreSQL container và JDBC connection.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Lỗi hiện tại: Test bị `FAILED` do môi trường chạy đang không có hoặc không tìm thấy Docker ( `Could not find a valid Docker environment` ). Đây là kết quả chuẩn xác theo yêu cầu "không giả thành PASS".
- Tương lai các test integration sẽ đều failed nếu máy host/CI không hỗ trợ Docker.

## 6. Việc chưa làm / Out of scope
- Chưa tạo base class chung cho toàn bộ Integration test (sẽ làm ở Sprint 2).
- Chưa cấu hình Test Profile với Flyway (sẽ làm ở Sprint 3).

## 7. Cách chạy & Cách verify (Reproduce)
- Chạy lệnh: `.\mvnw.cmd test -Dtest=PostgresContainerSmokeTest`
- Kết quả mong đợi: Nếu máy có Docker, test sẽ Pass và in ra Version PostgreSQL. Nếu không, test sẽ hiện lỗi "Could not find a valid Docker environment" rõ ràng.

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Tiếp tục Sprint 2: Sẽ tạo class abstract base `PostgresIntegrationTest` cấu hình chung Testcontainers để tái sử dụng container cho toàn bộ Integration tests mà không phải khởi tạo đi khởi tạo lại nhiều lần.
