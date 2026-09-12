# Ma trận Bao phủ Kế hoạch Kiểm thử (Test Coverage Matrix)

| Tính năng / Class | Level 1 | Level 2 | Level 3 | Level 4 | Ghi chú |
|---|---|---|---|---|---|
| `LowStockAlertJob` | ✅ | — | — | — | Unit test xử lý NEGATIVE_STOCK, LOW_STOCK và RESOLVED |
| `DashboardService` | ✅ | — | — | — | Unit test logic tổng hợp Dashboard Metrics và xử lý Null |
| `AnalyticsDataAdapter` | — | — | — | — | **[SKIP Level 3]** File này sử dụng native query của Postgres `to_date(?::text, 'YYYYMMDD')`. Do đồ án chưa setup Testcontainers PostgreSQL, tôi tạm bỏ qua Level 3 để tránh lỗi trên H2. Nếu setup xong Testcontainers thì nên bổ sung test này. |
| `InboundReceiptModule` | ✅ (FE) | — | — | — | Đã có Playwright spec test happy-path cho luồng nhập hàng tại `inbound-receipt.spec.ts` |
