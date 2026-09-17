# Sprint 33: Flyway Migration Upgrade Test (Sprint 9 & 10)

Ngày hoàn thành: 2026-09-17

## 1. Mục tiêu Sprint (Goal)
- Đảm bảo script migration mới (V18) có thể áp dụng thành công lên một database cũ (V17) CÓ CHỨA DỮ LIỆU.
- Ngăn chặn lỗi ALTER TABLE làm rớt dữ liệu (data loss) hoặc vi phạm NOT NULL constraint trên dữ liệu cũ.

## 2. Thành quả đạt được (Done)
- Tạo bài test `FlywayUpgradePostgresIT.java`.
- Cấu hình tắt Flyway Auto Config qua `@TestPropertySource(properties = {"spring.flyway.enabled=false"})`.
- Test tự động chạy Flyway bằng code đến phiên bản `target="17"`.
- Insert dữ liệu `customer`, `branch`, `receivable_debt` theo chuẩn Schema V17.
- Kích hoạt chạy tiếp Flyway đến `target="18"`.
- Xác minh (Verify) dữ liệu `total_debt` vẫn là 10,000,000 sau migration.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- Quyết định: Điều khiển Flyway programmatically (bằng tay qua Java API) trong bài test.
- Lý do: Mặc định Spring Boot sẽ chạy Flyway 1 lèo từ V1 -> V18 lúc khởi động Context. Nếu để Spring chạy tự động, ta chỉ test được "Fresh Database Install", không có cơ hội chèn dữ liệu cũ vào lúc DB đang ở state V17 để kiểm tra V18 backfill. Bằng cách gọi Flyway API thủ công, ta mô phỏng chính xác quá trình Deploy Production (DB đang ở V17 có data -> Chạy script V18 update).
- Đã cân nhắc: Dump SQL từ Prod về test — bỏ vì vi phạm bảo mật và tốn thời gian tải. Data nên được mock bằng SQL.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
- `FlywayUpgradePostgresIT.java`: Chú ý đoạn `flywayV17.migrate()`, sau đó là `jdbcTemplate.update` để chèn data, và cuối cùng là `flywayV18.migrate()`.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Test này sẽ cần sửa mỗi khi Vcurrent thay đổi. Khuyến cáo không để bài test này chạy mãi, mà chỉ viết cho những Version quan trọng (như V18 thay đổi schema mạnh) và có thể xóa/disable khi V18 đã lên Prod an toàn từ lâu.

## 6. Việc chưa làm / Out of scope
- Sprint 11: Chạy ddl-auto=validate (đã được bao hàm ở Sprint 4 FlywayPostgresIntegrationTest).

## 7. Cách chạy & Cách verify (Reproduce)
- `.\mvnw.cmd test -Dtest=FlywayUpgradePostgresIT`

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Sprint 11 & 12: Bổ sung Pipeline CI/CD, kết nối Fast Test (H2) và Integration Test (Testcontainers) trong Github Actions hoặc Gitlab CI.
