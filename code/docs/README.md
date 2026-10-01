# ERP System Documentation — Simplified Architecture v5

## 0. Mục đích

Đây là **bộ tài liệu chuẩn hiện hành (source of truth)** của hệ thống ERP. Bộ tài liệu này được tổng hợp từ mã nguồn thực tế, Docker Compose, migration SQL, cấu hình Spring Boot và tài liệu kỹ thuật hiện tại.

**Phiên bản:** v6  
**Ngày rà soát:** 2026-10-01

## 1. Thứ tự ưu tiên khi có khác biệt

Khi tài liệu mâu thuẫn với implementation, ưu tiên theo thứ tự:

1. `erp-backend/src/main/java/` — hành vi backend thực tế.
2. `erp-backend/src/main/resources/db/migration/` và `migration-branch/` — schema và quyền database.
3. `docker-compose.yml`, Nginx và application profiles — triển khai thực tế.
4. Test đang chạy trong `src/test/` — hành vi được xác nhận bằng test.
5. Các tài liệu trong thư mục này — mô tả và giải thích implementation.

## 2. Các tài liệu chính

| Tài liệu | Nội dung |
|---|---|
| `architecture/SYSTEM_ARCHITECTURE.md` | Kiến trúc tổng thể, N-instance, HQ/Branch, request path |
| `architecture/DATA_SCHEMA.md` | Schema hiện hành, bảng, khóa, quan hệ, ownership |
| `architecture/API_CONTRACT.md` | REST API hiện hành và phạm vi truy cập |
| `architecture/WORKFLOWS.md` | Workflow login, request API, sales, return, inbound, customer |
| `architecture/SECURITY_MODEL.md` | JWT, Spring Security, branch scope, DB permissions |
| `architecture/ARCHITECTURE_DECISIONS.md` | Các quyết định kiến trúc và trade-off |
| `development/BACKEND_STRUCTURE.md` | Cấu trúc source và module Java |
| `development/RUNTIME_CONFIG.md` | Docker, Spring Profiles, port và kết nối |
| `architecture/DATABASE_REPLICATION.md` | Logical replication HQ ↔ chi nhánh, role DB, bật/tắt chi nhánh, hạn chế đã biết |
| `architecture/REPLICATION_RUNBOOK.md` | Lệnh vận hành: dựng từ đầu, bật/tắt TP2, dữ liệu demo, xử lý sự cố |
| `architecture/DATA_OWNERSHIP_MATRIX.md` | Bảng nào do ai ghi, replicate chiều nào |
| `testing/TEST_STRATEGY.md` | Test hiện hành, quy tắc viết test tích hợp, kết quả chạy gần nhất |
| `AI_CONTEXT.md` | Context ngắn gọn dành cho AI Agent |
| `fix-replication/00-PLAN.md` | Hiện trạng trước khi sửa và các quyết định đợt fix-replication (2026-10-01) |

## 3. Quy tắc cập nhật

Mỗi khi chức năng hoặc schema thay đổi, phải cập nhật tối thiểu:

- `API_CONTRACT.md` nếu endpoint thay đổi;
- `DATA_SCHEMA.md` nếu entity/schema thay đổi;
- `WORKFLOWS.md` nếu luồng nghiệp vụ thay đổi;
- `SYSTEM_ARCHITECTURE.md` nếu topology/deployment thay đổi;
- `AI_CONTEXT.md` nếu thay đổi ảnh hưởng đến cách AI đọc project.

Không đưa chức năng đã bị loại bỏ vào tài liệu hiện hành. Các tài liệu sprint cũ chỉ có giá trị lịch sử, không phải source of truth.
