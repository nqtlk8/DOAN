# FACTS SPRINT 0b (Order, Inventory, Common)

## 1. ENTITY (E)

| ID | Tên lớp | Tên bảng | Module | Các trường chính (Tên, Kiểu, Ràng buộc) | Nguồn |
|---|---|---|---|---|---|
| E14 | SalesInvoice | sales_invoice | order.domain | branchId (Long, nullable=false), customerId (UUID, nullable=false), invoiceCode (String, nullable=false, unique), status (SalesInvoiceStatus, nullable=false), totalAmount (BigDecimal, nullable=false), previousDebt, remainingDebt, paymentMethod, advancePayment, confirmedAt, confirmedBy | [SalesInvoice.java] |
| E15 | SalesInvoiceLine | sales_invoice_line | order.domain | invoice (SalesInvoice, @ManyToOne), productId (Long, nullable=false), quantity (BigDecimal, nullable=false), unitPrice, unitCost, costBasis, lineTotal, unitOfMeasure | [SalesInvoiceLine.java] |
| E16 | GoodsReturn | goods_return | order.domain | branchId (Long, nullable=false), customerId (UUID, nullable=false), returnCode (String, nullable=false, unique), invoiceId (UUID), status (GoodsReturnStatus), totalAmount (BigDecimal), reason, confirmedAt, confirmedBy | [GoodsReturn.java] |
| E17 | GoodsReturnLine | goods_return_line | order.domain | goodsReturn (GoodsReturn, @ManyToOne), productId (Long, nullable=false), quantity (BigDecimal), unitPrice, unitOfMeasure | [GoodsReturnLine.java] |
| E18 | CustomerProductPrice | customer_product_price | order.domain | customerId (UUID), productId (Long), branchId (Long), unitPrice (BigDecimal), effectiveFrom, effectiveTo. (Ghi chú: Thay thế cho CustomerPrice) | [CustomerProductPrice.java] |
| E19 | InboundReceipt | inbound_receipt | inventory.domain | branchId (Long), receiptCode (String, unique), status (ReceiptStatus), supplierId, confirmedAt, confirmedBy | [InboundReceipt.java] |
| E20 | InboundReceiptLine | inbound_receipt_line | inventory.domain | receipt (InboundReceipt, @ManyToOne), productId (Long), quantity (BigDecimal), unitCost (BigDecimal), unitOfMeasure | [InboundReceiptLine.java] |
| E21 | StockOnHand | stock_on_hand | inventory.domain | productId (Long), branchId (Long), quantity (BigDecimal, default 0). Ràng buộc: Unique(productId, branchId) | [StockOnHand.java] |
| E22 | StockMovement | stock_movement | inventory.domain | id (UUID), productId, branchId, movementType (MovementType), quantity (BigDecimal), refType, refId, refLineId, performedBy, createdAt (ZonedDateTime) | [StockMovement.java] |
| E23 | CostLayer | cost_layer | inventory.domain | id (UUID), productId, branchId, unitCost, initialQty, remainingQty, inboundMovementId, costBasis, version (@Version Long - Optimistic Lock) | [CostLayer.java] |
| E24 | ReceivableDebt | receivable_debt | crm.domain | customer (Customer, @ManyToOne), branchId (Long), totalDebt (BigDecimal). Ràng buộc: Unique(customerId, branchId) | [ReceivableDebt.java] |
| E25 | ReceivableDebtMovement | receivable_debt_movement | crm.domain | id (UUID), branchId, customer (Customer, @ManyToOne), movementType, amount, balanceBefore, balanceAfter, refType, refId, idempotencyKey (unique) | [ReceivableDebtMovement.java] |
| E26 | IdempotencyRecord | idempotency_record | common.domain | id (UUID), idempotencyKey (String, unique), requestHash, responseSnapshot, createdAt | [IdempotencyRecord.java] |

*Tổng số Entity: 13*

## 2. QUAN HỆ (R)

| ID | Từ | Đến | Loại | Bản số / Cấu hình | Nguồn |
|---|---|---|---|---|---|
| R17 | SalesInvoice | SalesInvoiceLine | @OneToMany | cascade=ALL, orphanRemoval=true, mappedBy="invoice" | [SalesInvoice.java] |
| R18 | SalesInvoiceLine | SalesInvoice | @ManyToOne | @JoinColumn(name="invoice_id", nullable=false) | [SalesInvoiceLine.java] |
| R19 | GoodsReturn | GoodsReturnLine | @OneToMany | cascade=ALL, orphanRemoval=true, mappedBy="goodsReturn" | [GoodsReturn.java] |
| R20 | GoodsReturnLine | GoodsReturn | @ManyToOne | @JoinColumn(name="return_id", nullable=false) | [GoodsReturnLine.java] |
| R21 | InboundReceipt | InboundReceiptLine | @OneToMany | cascade=ALL, orphanRemoval=true, mappedBy="receipt" | [InboundReceipt.java] |
| R22 | InboundReceiptLine | InboundReceipt | @ManyToOne | @JoinColumn(name="receipt_id", nullable=false) | [InboundReceiptLine.java] |
| R23 | ReceivableDebt | Customer | @ManyToOne | @JoinColumn(name="customer_id", nullable=false) | [ReceivableDebt.java] |
| R24 | ReceivableDebtMovement | Customer | @ManyToOne | @JoinColumn(name="customer_id", nullable=false) | [ReceivableDebtMovement.java] |

*Tổng số Quan hệ: 8*

## 3. ENUM (N)

| ID | Tên Enum | Các giá trị | Nguồn |
|---|---|---|---|
| N04 | SalesInvoiceStatus | DRAFT, CONFIRMED, CANCELLED | [SalesInvoiceStatus.java] |
| N05 | GoodsReturnStatus | DRAFT, CONFIRMED | [GoodsReturnStatus.java] |
| N06 | ReceiptStatus | DRAFT, CONFIRMED | [InboundReceipt.java] |
| N07 | MovementType | INBOUND, SALE, RETURN | [StockMovement.java] |
| N08 | ReceivableDebtMovementType | INVOICE, PAYMENT, RETURN, ADJUSTMENT, OPENING_BALANCE | [ReceivableDebtMovementType.java] |

*Tổng số Enum: 5*

## 4. PHƯƠNG THỨC (M)

| ID | Tên phương thức | Lớp | Annotation | Nguồn |
|---|---|---|---|---|
| M11 | createAndConfirm | SalesInvoiceService | @Transactional, @Retryable (ObjectOptimisticLockingFailureException, DataIntegrityViolationException) | [SalesInvoiceService.java] |
| M12 | applyConfirmationEffects | SalesInvoiceService | (Private method) | [SalesInvoiceService.java] |
| M13 | confirmInvoice | SalesInvoiceService | @Transactional, @Retryable | [SalesInvoiceService.java] |
| M14 | confirmReturn | GoodsReturnService | @Transactional, @Retryable | [GoodsReturnService.java] |
| M15 | confirmReceipt | InboundReceiptService | @Transactional, @Retryable | [InboundReceiptService.java] |
| M16 | recordSaleAndGetCost | InventoryFacadeImpl | @Transactional | [InventoryFacadeImpl.java] |
| M17 | recordReturn | InventoryFacadeImpl | @Transactional | [InventoryFacadeImpl.java] |
| M18 | consume | FifoCostService | @Transactional(propagation = Propagation.MANDATORY) | [FifoCostService.java] |
| M19 | fromInbound | CostLayer | (Static factory) | [CostLayer.java] |
| M20 | fromReturn | CostLayer | (Static factory) | [CostLayer.java] |
| M21 | consume | CostLayer | (Domain logic) | [CostLayer.java] |
| M22 | confirm | SalesInvoice / GoodsReturn / InboundReceipt | (Domain logic) | [Các Entity tương ứng] |

*Tổng số Phương thức: 12*

## 5. QUY TẮC NGHIỆP VỤ & LUỒNG LỜI GỌI (B)

| ID | Mô tả quy tắc / Luồng gọi | Nguồn |
|---|---|---|
| B05 | **Luồng `createAndConfirm` (Bán hàng)**: `crmFacade.customerExists` -> `invoiceRepository.save` -> `applyConfirmationEffects` -> `invoiceRepository.save`. Trong `applyConfirmationEffects`: duyệt lines gọi `inventoryFacade.recordSaleAndGetCost` -> `debtService.increaseDebt` -> `debtService.decreaseDebt` (nếu có advance) -> `invoice.snapshotDebt` -> `invoice.confirm`. | [SalesInvoiceService.java] |
| B06 | **Luồng `confirmReturn` (Trả hàng)**: `returnRepository.findById` -> `invoiceRepository.findByIdForUpdate` (khóa PESSIMISTIC_WRITE) -> `goodsReturn.confirm` -> duyệt lines gọi `inventoryFacade.recordReturn` -> `debtService.decreaseDebt` -> `returnRepository.save`. | [GoodsReturnService.java] |
| B07 | **Luồng `confirmReceipt` (Nhập kho)**: `inboundRepo.findById` -> `receipt.confirm` -> duyệt lines: (`stockRepo.findByProductIdAndBranchId`, `stock.increase`, `stockRepo.save`, `StockMovement.inbound`, `movementRepo.save`, `CostLayer.fromInbound`, `costLayerRepo.save`) -> `inboundRepo.save`. | [InboundReceiptService.java] |
| B08 | **Luồng `recordSaleAndGetCost`**: `fifoCostService.consume` -> `stockOnHandRepository.findByProductIdAndBranchId` -> `stock.decreaseAllowNegative` -> `stockOnHandRepository.save` -> `StockMovement.sale` -> `stockMovementRepository.save`. | [InventoryFacadeImpl.java] |
| B09 | **Luồng `FifoCostService.consume`**: Gọi `costLayerRepo.findAvailableForFifoWithLock` (PESSIMISTIC_WRITE) -> lặp trừ dần `layer.consume` -> `costLayerRepo.save`. | [FifoCostService.java] |
| B10 | Optimistic Lock được sử dụng tại `CostLayer` thông qua cột `@Version version`, `@Retryable` bắt lỗi `ObjectOptimisticLockingFailureException` ở các API confirm. | [CostLayer.java, Các Service] |
| B11 | Pessimistic Lock được sử dụng ngầm định thông qua các repository method `findByIdForUpdate`, `findAvailableForFifoWithLock`. | [GoodsReturnService.java, FifoCostService.java] |

*Tổng số Quy tắc: 7*
