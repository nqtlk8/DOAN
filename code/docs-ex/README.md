# TÀI LIỆU HỆ THỐNG ERP (DOCUMENTATION)

Đây là trung tâm tài liệu mô tả toàn bộ kiến trúc, thiết kế, quy chuẩn và tiến độ của dự án ERP. Các tài liệu được phân tách rõ ràng thành các danh mục sau:

## 1. 🏗️ Kiến Trúc Hệ Thống (Architecture)
Chứa các bản vẽ, hợp đồng API, cấu trúc Database, và các Quyết định Kiến trúc (ADRs).
- [Tổng quan Hệ thống (SYSTEM_OVERVIEW.md)](./architecture/SYSTEM_OVERVIEW.md) - Báo cáo toàn diện về thiết kế hệ thống.
- [Hợp đồng Data (DATA_CONTRACT.md)](./architecture/DATA_CONTRACT.md) - Đối chiếu chuẩn giữa Database, Entity và Schema.
- [Hợp đồng API (API_CONTRACT.md)](./architecture/API_CONTRACT.md) - Quy định endpoint, Request/Response giữa Frontend & Backend.
- [Luồng nghiệp vụ (WORKFLOWS.md)](./architecture/WORKFLOWS.md) - Giải thích các quy trình kinh doanh cốt lõi (Bán hàng, Trả hàng, v.v.).
- [Replication Database (DATABASE_REPLICATION.md)](./architecture/DATABASE_REPLICATION.md) - Cơ chế đồng bộ dữ liệu giữa Trụ sở & Chi nhánh.
- [ADRs (Architecture Decision Records)](./architecture/adr/) - Lịch sử các quyết định kỹ thuật cốt lõi.

## 2. 📖 Hướng Dẫn & Quy Chuẩn (Guidelines)
Dành cho lập trình viên và các AI Agents khi phát triển hệ thống.
- [Quy chuẩn Database (DATABASE_STANDARDS.md)](./guidelines/DATABASE_STANDARDS.md) - Cách chuẩn hóa CSDL.
- [Ngữ cảnh cho AI (AI_AGENT_CONTEXT.md)](./guidelines/AI_AGENT_CONTEXT.md) - Context thu gọn dành riêng cho Agent để nắm cấu trúc.
- [Template Module (MODULE_TEMPLATE.md)](./guidelines/MODULE_TEMPLATE.md) - Cấu trúc chuẩn của một Module code Backend.

## 3. 🏃 Tiến Độ Sprints (Sprints)
Lưu trữ các báo cáo kế hoạch và trạng thái hoàn thành của từng Sprint.
- [Danh sách Sprints (README.md)](./sprints/README.md)
- [Sprint 6: Tích hợp Frontend & Backend](./sprints/sprint-6-frontend-backend-integration.md) (Sprint hiện tại)

## 4. 📝 Lịch Sử Thay Đổi & Khắc Phục (Changelogs)
Lưu lại lịch sử sửa lỗi nghiêm trọng và các thay đổi lớn qua các phase.
- [Changelog Phase 3 & 4 (CHANGELOG_PHASE_3_4.md)](./changelogs/CHANGELOG_PHASE_3_4.md) - Cập nhật đồng bộ và refactor toàn diện Frontend/Backend.
- [Issues Confirmed (ISSUES_CONFIRMED.md)](./changelogs/ISSUES_CONFIRMED.md) - Các lỗi đã phân tích và chẩn đoán.

## 5. 🧪 Kiểm Thử (Testing)
Tài liệu về độ phủ test (Coverage) và kế hoạch kiểm thử.
- [Coverage Matrix (coverage-matrix.md)](./testing/coverage-matrix.md)
- [Coverage Frontend (coverage-matrix-frontend.md)](./testing/coverage-matrix-frontend.md)
