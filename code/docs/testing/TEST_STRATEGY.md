# Test Strategy — v5

## 1. Test hiện có

Backend hiện có các nhóm test:

- Architecture tests
- Integration tests
- Unit tests
- Replication E2E test

## 2. Test inventory

```text
ArchitectureV3BranchTest.java
ArchitectureV3HqTest.java
BranchRbacIntegrationTest.java
ReplicationE2ETest.java
HealthControllerTest.java
AuthIntegrationTest.java
InboundReceiptIdempotencyTest.java
CostingStrategyUnitTest.java
StockOnHandUnitTest.java
DebtReconciliationIntegrationTest.java
GoodsReturnWithInvoiceTest.java
GoodsReturnWithoutInvoiceTest.java
SalesInvoiceIntegrationTest.java
CustomerProductPriceUnitTest.java
```

## 3. Các hành vi đang được kiểm thử

### Architecture

Kiểm tra đặc điểm khác nhau giữa HQ và Branch.

### Authentication

Kiểm thử login và Security Context.

### Inventory

Kiểm thử StockOnHand, costing và idempotency của inbound receipt.

### Sales

Kiểm thử Sales Invoice và transaction liên quan.

### Goods Return

Kiểm thử trả hàng có/không có hóa đơn và đối soát công nợ.

### Replication

Có E2E test dành riêng cho replication.

## 4. Test không phải bằng chứng tự động cho mọi annotation

Sự tồn tại của `@PreAuthorize` trong source đã được bảo vệ bằng `@EnableMethodSecurity`. Tuy nhiên, vẫn cần phải có Integration Test để đảm bảo config không bị thay đổi ngẫu nhiên trong tương lai.
