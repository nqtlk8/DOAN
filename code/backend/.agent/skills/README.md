# .agent/skills — Bộ quy tắc bắt buộc cho AI coding agent (Backend)

Thư mục này chứa toàn bộ "skill" (quy tắc + hướng dẫn thực thi) mà AI coding agent
**bắt buộc phải đọc** trước khi thực hiện bất kỳ task nào trong dự án Website + ERP
VLXD & TTNT. Đây không phải tài liệu tham khảo tuỳ chọn — vi phạm các quy tắc trong
đây được coi là lỗi triển khai, không phải sự khác biệt phong cách.

## Cách đọc

1. **Đọc `01-solid-oop-layering`, `02-aop-cross-cutting`, `03-codegraph-usage` trước
   khi viết dòng code đầu tiên của bất kỳ sprint nào** — đây là 3 skill nền tảng áp
   dụng cho mọi task không ngoại lệ.
2. Đọc thêm các skill khác **theo loại task đang làm** (bảng dưới).
3. Trước khi báo cáo hoàn thành 1 task, đọc lại `07-testing-black-box` và chạy
   checklist trong `04-documentation-standards`.

## Bảng tra cứu nhanh — task đang làm cần đọc skill nào

| Đang làm gì | Bắt buộc đọc thêm |
|---|---|
| Tạo Entity/Service/Controller mới | `01-solid-oop-layering`, `03-codegraph-usage` |
| Thêm audit log / transaction / validation / rate limit | `02-aop-cross-cutting` |
| Tạo bảng DB mới hoặc sửa schema | `06-database-schema-conventions`, `10-replication-hub-spoke` |
| Code động tới kho, công nợ, hoá đơn (bất kỳ module nào có tiền/số lượng) | `05-transaction-data-integrity` |
| Viết Repository/Service truy vấn dữ liệu theo chi nhánh | `08-branch-scope-multitenancy` |
| Viết test cho task vừa xong | `07-testing-black-box` |
| Chuẩn bị commit/PR | `09-git-commit-conventions` |
| Task xong, chuẩn bị báo cáo | `04-documentation-standards` |

## Nguồn gốc

Các quy tắc trong thư mục này được rút ra và hợp nhất từ toàn bộ tài liệu thiết kế
dự án: `kien-truc-he-thong-vlxd-ttnt-v2.md`, `ke-hoach-trien-khai-ai-agent-v2.md`,
`prompt-sprint-ai-agent-codegraph-v2.md`, `giai-thich-chuan-hoa-database.md`,
`schema-database-v1.sql`. Khi có mâu thuẫn giữa skill và prompt sprint cụ thể, skill
trong thư mục này là **nguồn quy tắc chuẩn** (source of truth) — prompt sprint chỉ mô
tả *việc cần làm*, còn skill mô tả *làm đúng cách như thế nào*.
