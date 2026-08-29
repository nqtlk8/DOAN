# Frontend Test Coverage Matrix

Theo quy tắc TEST_PLAN_RULES, đây là ma trận theo dõi tiến độ test cho Frontend (apps/erp-frontend).

| Tính năng / Component | Unit (Component) | Integration (Hook/Context) | E2E | Ghi chú |
|---|---|---|---|---|
| Login Component | ✅ | ❌ | ❌ | Đã test render form chuẩn xác sau khi gỡ Next.js |
| AuthContext | ❌ | ❌ | ❌ | Cần mock axios để test login flow |
| TopRibbon | ❌ | ❌ | ❌ | |
| SalesModule | ❌ | ❌ | ❌ | Cần mock TanStack Query |
