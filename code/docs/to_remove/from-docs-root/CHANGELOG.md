# CHANGELOG

## [Unreleased]

- **Sprint 2:** Standardized exception handling in Auth flow (`NoRoleAssignedException`, `SigningKeyNotConfiguredException`) and integrated with `GlobalExceptionHandler` (Status 401).
- **Sprint 3:** Bound JWT `branchId` to running instance via `JwtAuthenticationFilter`. Dropped old `@BranchScoped` aspect.
- **Sprint 4:** Fixed `ProductReader` blocking Admin users by returning `null` branch ID safely and skipping branch-specific pricing.
- **Sprint 5:** Updated `AnalyticsDataAdapter` to properly handle `branchId = null` as HQ company-wide scope using `NamedParameterJdbcTemplate`.
- **Sprint 6:** Fixed Replication issue by avoiding sync of snapshot tables (`stock_on_hand`, `receivable_debt`) and dynamically aggregating from `stock_movement` and `sales_invoice` for HQ scope.

## [fix-dashboard]
- **Dashboard & Cảnh báo:** Sửa lỗi 500 khi xem "Tất cả chi nhánh" (tham số `branchId` null không có kiểu trên PostgreSQL); thêm chỉ số vòng quay tồn kho, sản phẩm bán chậm; thêm cảnh báo tồn kho (tồn âm / dưới ngưỡng) với API `GET /api/v1/analytics/stock-alerts`; dashboard tự làm mới mỗi 60 giây.
- **⚠️ BREAKING:** Đổi tên field API Dashboard `totalOverdueDebt` thành `totalReceivableDebt` (nhãn UI: "Tổng công nợ phải thu").
- **⚠️ BREAKING:** DROP bảng `inventory_alert_log` và bỏ job `LowStockAlertJob` (V21). Cảnh báo được tính trực tiếp từ `stock_movement` khi gọi API.
- **⚠️ BREAKING — Replication:** Đảo ngược quyết định Sprint 6 (ADR-13). Replicate `stock_on_hand`, `receivable_debt` từ chi nhánh lên HQ, có row filter `WHERE (branch_id = N)`; V21 đặt `REPLICA IDENTITY USING INDEX`. Chi nhánh đang chạy cần chạy `scripts/enable-snapshot-replication.sh <branch>`.
- **Test:** Thêm `maven-failsafe-plugin` để chạy các test `*IT.java` (Testcontainers) bằng `mvn verify`.
