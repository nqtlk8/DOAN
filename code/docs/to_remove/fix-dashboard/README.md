# Đợt fix-dashboard — Sửa Dashboard & Cảnh báo tồn kho

Đợt này sửa lỗi 500 của dashboard HQ khi chọn "Tất cả chi nhánh", replicate `stock_on_hand` và `receivable_debt` từ chi nhánh lên HQ (có row filter theo chi nhánh), hoàn thiện các chỉ số dashboard và thêm cảnh báo tồn kho âm / dưới ngưỡng.

## Danh sách sprint

| Mã | Tài liệu | Ngày | Tóm tắt |
|----|----------|------|---------|
| FD-0 | [sprint-fd0-architecture-docs.md](sprint-fd0-architecture-docs.md) | 2026-09-27 | Cập nhật tài liệu kiến trúc (API contract, schema, replication, ADR-13) và chạy baseline test. |
| FD-1 | [sprint-fd1-fix-500-postgres-it.md](sprint-fd1-fix-500-postgres-it.md) | 2026-09-28 | Sửa lỗi 500 (tham số `branchId` null có kiểu), thêm `maven-failsafe-plugin`, xử lý múi giờ lọc ngày (M-06). |
| FD-2 | [sprint-fd2-snapshot-replication.md](sprint-fd2-snapshot-replication.md) | 2026-09-28 | Migration V21, replicate 2 bảng snapshot có row filter, script `enable-snapshot-replication.sh`, bỏ `LowStockAlertJob`. |
| FD-3 | [sprint-fd3-analytics-backend-alerts.md](sprint-fd3-analytics-backend-alerts.md) | 2026-09-28 | Chỉ số KPI mới (vòng quay tồn kho, bán chậm, tổng công nợ phải thu) và API `GET /api/v1/analytics/stock-alerts`. |
| FD-4 | [sprint-fd4-frontend-dashboard-alerts.md](sprint-fd4-frontend-dashboard-alerts.md) | 2026-09-28 | Panel cảnh báo tồn kho, bảng sản phẩm bán chậm, tự làm mới 60 giây, đổi nhãn thẻ công nợ. |
| FD-5 | [sprint-fd5-e2e-closeout.md](sprint-fd5-e2e-closeout.md) | 2026-09-28 | Chạy E2E trên Docker (HQ + TP1 + TP2), bật replicate snapshot cho tp1/tp2, cập nhật CHANGELOG/AI_CONTEXT. |
| FD-6 | [sprint-fd6-review-fixes.md](sprint-fd6-review-fixes.md) | 2026-09-28 | Sửa lỗi sau review: fixture 3 integration test, test múi giờ, `StockAlertPanel` theo design system, tài liệu hỏng encoding. |

## Tài liệu liên quan
- [00-PLAN.md](00-PLAN.md) — kế hoạch tổng, quyết định đã chốt (mục 2) và thiết kế đích (mục 3).
- [business-rules.md](business-rules.md) — quy tắc tính cảnh báo và chỉ số, kèm ví dụ SQL nhập ngưỡng.
- [REVIEW-2026-09-28.md](REVIEW-2026-09-28.md) — báo cáo review code sau FD-5.
- ADR-13 trong [../architecture/ARCHITECTURE_DECISIONS.md](../architecture/ARCHITECTURE_DECISIONS.md) — lý do replicate bảng snapshot có row filter.
