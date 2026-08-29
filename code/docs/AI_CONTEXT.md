# AI Context — ERP v5

## Identity

ERP cho cửa hàng VLXD & thiết bị thông minh nhà.

## Architecture

- Java 21 + Spring Boot 3.4.0.
- Modular Monolith.
- Same backend artifact -> 1 HQ + N Branch instances.
- PostgreSQL DB riêng cho từng instance.
- Redis chỉ tại HQ.
- HQ signs JWT RS256; Branch verifies locally.

## Active business modules

- Identity
- Branch
- Catalog
- CRM
- Sales / Goods Return
- Inventory / Inbound
- Analytics
- Common

## Removed modules

Do NOT describe as active:

- Customer Order
- Procurement / Purchase Order
- Payable Debt
- Stock Transfer
- Outbound Receipt
- RFQ

## Active inventory-changing operations

1. Inbound Receipt -> increase stock + costing.
2. Sales Invoice Confirm -> decrease stock + debt update.
3. Goods Return Confirm -> increase stock + debt adjustment.

## Docker

- ERP frontend: host 80.
- HQ app: host 8080 -> container 8080.
- TP1 app: host 8081 -> container 8080.
- TP2 app: host 8082 -> container 8080.
- TP1 Nginx: 81 -> 80.
- TP2 Nginx: 82 -> 80.
- HQ DB 5432, TP1 DB 5433, TP2 DB 5434.
- Redis HQ 6379.

## Backend package

```text
com.storename.erp
├── identity
├── branch
├── catalog
├── crm
├── order
├── inventory
├── analytics
├── common
├── infrastructure
└── system
```

## Current security facts

- `SecurityConfig` is stateless.
- Public: auth/public/health/swagger paths.
- JWT authority generated as exact role without ROLE_ prefix.

- `@BranchScoped` validates branchId.
- `@IdempotencyProtected` prevents duplicate processing.
- Branch master tables are restricted by `V9__branch_db_security.sql` for `erp_user` in Docker runtime.

## Known implementation mismatches

Do not silently assume these are fixed:

1. Some user-id extraction is incomplete/inconsistent.
2. Old docs contain removed features; ignore them.

## Source of truth

When answering questions about current behavior, inspect:

1. Java source.
2. SQL migration.
3. Docker Compose / Nginx.
4. Tests.
5. This docs set.
