# Sprint 35: CI/CD Pipeline Strategy (Sprint 12)

Ngày hoàn thành: 2026-09-17

## 1. Mục tiêu Sprint (Goal)
- Viết tài liệu chiến lược định hướng cho DevOps/Kỹ sư CI cách tích hợp bộ Fast Tests (H2) và DB Integration Tests (Testcontainers) vào Pipeline.

## 2. Thành quả đạt được (Done)
- Đã tạo `docs/testing/ci-cd-pipeline-strategy.md`.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- Quyết định: Yêu cầu môi trường Docker-in-Docker cho runner CI thay vì cấp sẵn một External PostgreSQL DB chung.
- Lý do: Dùng Testcontainers mỗi nhánh CI sẽ khởi tạo DB rỗng riêng, tự chạy Flyway riêng, đảm bảo 100% Isolation (cách ly) giữa các luồng test song song. Tránh lỗi race-condition khi test database chung.
- Đã cân nhắc: Setup 1 PostgreSQL dev chung trên server test — bỏ vì sẽ xảy ra xung đột khi 2 nhánh test chạy cùng lúc.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
- `docs/testing/ci-cd-pipeline-strategy.md`

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- CI runner sẽ cần RAM mạnh hơn một chút để chứa các container của Testcontainers. Thời gian chạy pipeline sẽ dài hơn thêm ~10-20 giây cho thời gian khởi động image Docker.

## 6. Việc chưa làm / Out of scope
- Chưa tạo file config YAML (như `.github/workflows/ci.yml`) vì tùy thuộc team dự án dùng tool CI gì (Github Actions hay Gitlab CI).

## 7. Cách chạy & Cách verify (Reproduce)
- (Tài liệu thuần)

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Project đã hoàn thiện được bộ khung Database Testing "Fast + Integration" toàn diện theo yêu cầu.
