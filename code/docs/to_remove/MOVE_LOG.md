# Nhật ký dọn dẹp docs — 2026-09-28

Toàn bộ file dưới đây chỉ được **di chuyển**, không xoá gì trên máy bạn.
Xem lại nếu thấy ổn thì tự xoá cả folder `to_remove` này.

## 1. Từ `code/docs/` (file rời ở gốc)
| File | Lý do đưa vào to_remove |
|---|---|
| `BAOCAO_HOANCHINH.md` | Bản cũ (288 dòng) — đã có `BAOCAO_HOANCHINH_V2.md` (1939 dòng, mới hơn) thay thế, giữ lại ở docs |
| `BUSINESS_ANALYSIS_MODIFIED.md` | Ghi v1.2 (2026-09-08) — cũ hơn `BUSINESS_ANALYSIS.md` (v1.3, 2026-09-15), giữ lại ở docs |
| `FACTS.md`, `FACTS_A.md`, `FACTS_B.md` | Ghi chú scratch tra cứu entity từ code, đã được tổng hợp thành `architecture/DATA_SCHEMA.md` |
| `FOUND_ISSUES.md` | Ghi chú "Sprint 0 - Chụp Baseline" |
| `VERIFY.md` | Ghi "SPRINT D0 – XÁC MINH" |
| `CHANGELOG.md` | Nội dung chỉ log theo từng Sprint (Sprint 2, Sprint 3...) |
| `Plan & Prompt cho Gemini – Hoàn thiện báo cáo.md` | File "Plan" |
| `Plan tối ưu Diagram – Chương 3.md` | File "Plan" |
| `Sửa outline báo cáo - Thứ tự lược đồ.md` | Ghi chú làm việc tạm về outline |

`CH3_final.md`, `CH3_diagram_v2.md` | Theo yêu cầu của bạn ngày 2026-09-28: chuyển cả 2 vào to_remove (nội dung Chương 3 coi như đã nằm trong `BAOCAO_HOANCHINH_V2.md`).

## 2. Nguyên cụm folder trong `code/docs/`
- `audit/` — review code theo ngày (giống các file review khác)
- `diagrams/` — toàn bộ ảnh/diagram .png/.svg/.mmd (theo lựa chọn của bạn)
- `fix-dashboard/` — cả cụm sprint fd0–fd6 + plan + review
- `out/` — output trung gian sinh diagram (D1..D5, S1..S6)
- `plans/` — các file plan
- `scratch/` — script scratch
- `sprints/` — sprint-2..sprint-39 + fix_v1/fix_v2

## 3. Từ `erp-platform/apps/erp-frontend/docs/`
`CODE_REVIEW_FRONTEND.md`, `GEMINI_ACTION_LOG.md`, `sprint-10-audit-remediation.md`,
`sprint-11-claude-remediation.md`, `UI_UX_PLAN_GEMINI.md`, `UI_UX_PROGRESS.md`,
`UI_UX_QUESTIONS.md` — review/plan/log tạm thời (theo lựa chọn của bạn).

## 4. Từ `erp-platform/apps/erp-frontend/_to_delete/`
`DESIGN_SYSTEM.old-utf16.md` — bạn đã tự đánh dấu để xoá từ trước.

**Lưu ý:** folder `_to_delete/` gốc vẫn còn (chứa các script .js/.py và code cũ,
không phải docs nên tôi không đụng vào) — bạn tự xử lý riêng nếu muốn xoá luôn.

## Các file/doc đã ĐƯỢC GỘP vào docs (không phải to_remove)
- `erp-backend/.../identity/README.md` → `docs/backend/erp-backend/identity-README.md`
  (để lại stub trỏ về vị trí mới tại chỗ cũ)
- `erp-backend/.../inventory/README.md` → `docs/backend/erp-backend/inventory-README.md` (stub tương tự)
- `web-public/design_system.md` → `docs/frontend/web-public/DESIGN_SYSTEM.md`
- `web-public/agent-instructions.md` → `docs/frontend/web-public/agent-instructions.md`
- `erp-frontend/docs/DESIGN_SYSTEM.md` → `docs/frontend/erp-frontend/DESIGN_SYSTEM.md`
- `erp-frontend/docs/test-cases/*.md` → `docs/testing/frontend/*.md`

## Giữ nguyên vị trí cũ (không đụng tới)
- Mọi `README.md` ở gốc từng package/app (quy ước chuẩn, công cụ/GitHub trông cậy vào đó)
- `AGENTS.md`, `CLAUDE.md`, `.agents/skills/**` — file cấu hình cho AI agent, không phải docs dự án
- `schema-database-v1.sql`, `docker-compose.yml`, `nginx-*.conf`, `scripts/`, `secrets/` — file vận hành/hạ tầng
- `packages/api-contract/openapi.json` — spec API dùng trực tiếp trong code (không phải tài liệu mô tả)
