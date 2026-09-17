# Sprint 24: Database Test Infrastructure Audit (Sprint 0)

Ngày hoàn thành: 2026-09-17

## 1. Mục tiêu Sprint (Goal)
- Xác định nguyên nhân toàn bộ các test hiện tại không chạy Flyway và H2 chỉ verify được mức cơ bản.
- Lên kế hoạch chiến lược phân lớp test (Fast Tests vs PostgreSQL Integration Tests) không phá vỡ tốc độ của các test hiện có.
- Khởi tạo văn bản định hướng chiến lược test cho dự án.

## 2. Thành quả đạt được (Done)
- Đã audit cấu hình test hiện tại: thấy rõ `spring.flyway.enabled=false` và `spring.jpa.hibernate.ddl-auto=create-drop` đang được sử dụng trong file `application-test.yml`.
- Đã tạo tài liệu chiến lược phân lớp Database Test tại `docs/testing/database-test-strategy.md`.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- Quyết định: Phân lớp test thành Fast Tests (H2) và Integration Tests (PostgreSQL Testcontainers + Flyway).
- Lý do: Không làm chậm 133 tests hiện tại (vẫn chạy trên H2), nhưng có thêm một chốt chặn ("production-like database gate") bằng DB thật để bắt các lỗi extension, schema type và Flyway data migration mà H2 không thể mô phỏng.
- Đã cân nhắc: Chuyển toàn bộ 133 tests sang Testcontainers — bỏ vì thời gian chạy CI sẽ tăng vọt và không cần thiết cho business logic đơn thuần.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
- `docs/testing/database-test-strategy.md`: Đọc để hiểu tại sao cần phải thêm Testcontainers và cấu trúc của các lớp Test (Fast test, Postgres IT, Flyway suite).
- `services/erp-backend/src/main/resources/application-test.yml`: Đọc để xác nhận trạng thái hiện tại (Flyway = false, H2 đang dùng).

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Các cấu hình Testcontainers và Flyway Test profile chưa được setup, cần thực hiện ở các sprint tiếp theo.
- Việc chạy Testcontainers có thể bị lỗi trên môi trường CI nếu Docker chưa được cấp quyền (Docker-in-Docker), cần chú ý ở pipeline.

## 6. Việc chưa làm / Out of scope
- Không thực hiện thay đổi code hay config nào đối với hệ thống trong Sprint này (như đúng định nghĩa Audit).

## 7. Cách chạy & Cách verify (Reproduce)
- File tài liệu đã được tạo. Không có code để chạy ở sprint này.

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Sprint tiếp theo (Sprint 1) sẽ bắt đầu cấu hình Maven để thêm các thư viện `Testcontainers` (PostgreSQL module, JUnit 5 integration) và viết một Smoke Test nhỏ khởi tạo Container đầu tiên.
