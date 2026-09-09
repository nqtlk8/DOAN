# API Contract — v5

## 1. Chuẩn response

Backend dùng `ApiResponse<T>` cho phần lớn REST API:

```json
{
  "success": true,
  "data": {},
  "message": "...",
  "errors": null
}
```

Một số endpoint đặc biệt trả binary hoặc response trực tiếp theo implementation.

## 2. Health và Public

| Method | Path | Controller | Quyền |
|---|---|---|---|
| GET | `/api/health` | `HealthController` | Public |
| GET | `/api/v1/public/catalog/products` | `PublicCatalogController` | Public |
| GET | `/api/v1/public/catalog/products/{id}` | `PublicCatalogController` | Public |

## 3. Authentication — HQ only

| Method | Path | Controller | Instance |
|---|---|---|---|
| POST | `/api/v1/auth/login` | `AuthController` | HQ |
| POST | `/api/v1/auth/refresh` | `AuthController` | HQ |
| POST | `/api/v1/auth/revoke` | `AuthController` | HQ |

`AuthController` được giới hạn bằng `@ConditionalOnProperty(instance.role=HQ)`.

## 4. Branch

| Method | Path | Ghi chú |
|---|---|---|
| POST | `/api/v1/branches` | ADMIN; HQ-only controller |
| GET | `/api/v1/branches` | HQ-only controller; hiện không có `@PreAuthorize` trên method |
| GET | `/api/v1/branches/{id}` | HQ-only controller; hiện không có `@PreAuthorize` trên method |
| PUT | `/api/v1/branches/{id}` | ADMIN |
| DELETE | `/api/v1/branches/{id}` | ADMIN |

## 5. Catalog

| Method | Path | Ghi chú |
|---|---|---|
| GET | `/api/v1/catalog/products` | ADMIN/STAFF theo annotation hiện tại |
| GET | `/api/v1/catalog/products/{id}` | ADMIN/STAFF |
| POST | `/api/v1/catalog/products` | HQ + ADMIN |
| PUT | `/api/v1/catalog/products/{id}` | HQ + ADMIN |
| DELETE | `/api/v1/catalog/products/{id}` | HQ + ADMIN (Soft-delete) |
| GET | `/api/v1/suppliers` | ADMIN/STAFF (Response wrapped in ApiResponse) |
| GET | `/api/v1/suppliers/{id}` | ADMIN/STAFF (Response wrapped in ApiResponse) |
| POST | `/api/v1/suppliers` | ADMIN/STAFF (Response wrapped in ApiResponse) |

## 6. CRM

| Method | Path | Ghi chú |
|---|---|---|
| POST | `/api/v1/customers` | `CustomerWriteController`; HQ-only, ADMIN |
| PUT | `/api/v1/customers/{id}` | `CustomerWriteController`; HQ-only, ADMIN |
| DELETE | `/api/v1/customers/{id}` | `CustomerWriteController`; HQ-only, ADMIN (Soft-delete) |
| GET | `/api/v1/customers` | `CustomerReadController`; `hasAnyAuthority('CUSTOMER_READ', 'ADMIN', 'STAFF')` |
| GET | `/api/v1/receivable-debts` | `@BranchScoped`; STAFF/ADMIN theo annotation |

## 7. Customer product price

| Method | Path | Ghi chú |
|---|---|---|
| GET | `/api/v1/customer-prices/{customerId}/product/{productId}` | `@BranchScoped`; `STAFF`/`ADMIN` theo annotation |

## 8. Sales Invoice

| Method | Path | Ghi chú |
|---|---|---|
| POST | `/api/v1/sales-invoices` | Draft; `@BranchScoped`; STAFF/ADMIN theo annotation |
| POST | `/api/v1/sales-invoices/{id}/confirm` | `@BranchScoped` + `@IdempotencyProtected` |
| GET | `/api/v1/sales-invoices` | `@BranchScoped` |
| GET | `/api/v1/sales-invoices/{id}` | `@BranchScoped` |

## 9. Goods Return

| Method | Path | Ghi chú |
|---|---|---|
| POST | `/api/v1/goods-returns` | Draft; `@BranchScoped` |
| POST | `/api/v1/goods-returns/{id}/confirm` | `@BranchScoped` + `@IdempotencyProtected` |
| GET | `/api/v1/goods-returns` | STAFF/ADMIN theo annotation |
| GET | `/api/v1/goods-returns/{id}` | STAFF/ADMIN theo annotation |

## 10. Inventory

| Method | Path | Ghi chú |
|---|---|---|
| GET | `/api/v1/inventory/stock` | `@BranchScoped`; STAFF/ADMIN |
| POST | `/api/v1/inventory/inbound` | Draft; `@BranchScoped` |
| POST | `/api/v1/inventory/inbound/{id}/confirm` | `@BranchScoped` + `@IdempotencyProtected` |
| GET | `/api/v1/inventory/inbound` | STAFF/ADMIN |
| GET | `/api/v1/inventory/inbound/{id}` | STAFF/ADMIN |
| GET | `/api/v1/stock-movements` | `@BranchScoped`; STAFF/ADMIN; Trả về lịch sử biến động kho |

## 11. Analytics / System

| Method | Path | Ghi chú |
|---|---|---|
| GET | `/api/v1/analytics/dashboard` | HQ-only controller; `hasRole('ADMIN')` |
| GET | `/api/v1/analytics/export/excel` | HQ-only controller; `hasRole('ADMIN')` |
| GET | `/api/v1/admin/system/replication-status` | `hasRole('ADMIN')`; kiểm tra PostgreSQL replication views |

## 12. Lưu ý về authority hiện tại

JWT filter hiện tạo authority khớp hoàn toàn với chuỗi định danh quyền (không có prefix `ROLE_`). Do đó các annotaton như `hasAuthority('ADMIN')` hoặc `hasAnyAuthority('STAFF', 'ADMIN')` sẽ hoạt động chính xác theo đúng quyền được cấp phát.
Đồng thời `SecurityConfig` cũng đã khai báo `@EnableMethodSecurity` nên các annotation `@PreAuthorize` đều có hiệu lực.
