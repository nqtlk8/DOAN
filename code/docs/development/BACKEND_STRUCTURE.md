# ERP Backend Structure — v5

## 1. Root package

```text
com.storename.erp
├── identity/
├── branch/
├── catalog/
├── crm/
├── order/
├── inventory/
├── analytics/
├── common/
├── infrastructure/
└── system/
```

## 2. Module structure

Các module nghiệp vụ chính thường có:

```text
<module>/
├── api/
├── application/
├── domain/
└── infrastructure/
```

## 3. Module hiện hành

### Identity

Auth, user, role, permission, branch-role mapping.

### Branch

Quản lý branch tại HQ.

### Catalog

Product, Category, PriceList, Supplier.

### CRM

Customer, ReceivableDebt.

### Order

SalesInvoice, GoodsReturn, CustomerProductPrice.

### Inventory

StockOnHand, InboundReceipt, CostingStrategy, UnitConversion.

### Analytics

Dashboard, ETL job, low-stock alert, report export.

### Common

Security, AOP, ApiResponse, BaseEntity, exception, idempotency.

## 4. Thành phần cross-cutting

- `JwtAuthenticationFilter`
- `JwtTokenProvider`
- `BranchScopedAspect`
- `IdempotencyAspect`
- `AuditAspect`
- `LogExecutionTimeAspect`
- `BaseEntity`
- `ApiResponse`

## 5. Persistence

Spring Data JPA + Hibernate. Schema do Flyway quản lý; Hibernate dùng `ddl-auto=validate` trong HQ/Branch profiles.

## 6. Build

Maven project dùng Java 21, Spring Boot 3.4.0. Các thư viện chính gồm Spring Web, Security, Data JPA, Redis, Flyway, Spring Retry, MapStruct, JJWT, SpringDoc, Apache POI và OpenPDF.
