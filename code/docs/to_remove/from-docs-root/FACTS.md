# FACTS_A

## 1. ENTITY
| ID | Lớp | Bảng | Module (Package) | Các trường | Nguồn |
|---|---|---|---|---|---|
| E01 | `Branch` | `branch` | `com.storename.erp.branch.domain` | `id` (Long, @Id, @GeneratedValue), `code` (String 50, nullable=false, unique), `name` (String 200, nullable=false), `address` (String 500), `phone` (String 20), `openingHours` (String 100), `internalUrl` (String 255), `isActive` (boolean, nullable=false), `createdAt`, `updatedAt` | `Branch.java` |
| E02 | `Category` | `category` | `com.storename.erp.catalog.domain` | `id` (Long, @Id), `parent` (Category), `children` (List), `code` (String, nullable=false, unique), `name` (String, nullable=false), `isActive` (boolean), `createdAt`, `updatedAt` | `Category.java` |
| E03 | `Product` | `product` | `com.storename.erp.catalog.domain` | `id` (UUID, @Id), `category` (Category), `code` (String, unique), `name` (String), `baseUnit` (String), `attributesCache` (Map), `isActive` (boolean), `createdAt`, `updatedAt` | `Product.java` |
| E04 | `Supplier` | `supplier` | `com.storename.erp.catalog.domain` | `id` (UUID, @Id), `branchId` (Long), `code` (String, unique), `name` (String), `phone` (String), `email` (String), `address` (String), `taxCode` (String), `isActive` (boolean), `createdAt`, `updatedAt` | `Supplier.java` |
| E05 | `PriceList` | `price_list` | `com.storename.erp.catalog.domain` | `id` (Long, @Id), `product` (Product), `branchId` (Long), `price` (BigDecimal), `effectiveDate` (OffsetDateTime), `createdAt` (OffsetDateTime) | `PriceList.java` |
| E06 | `Customer` | `customer` | `com.storename.erp.crm.domain` | `id` (UUID, @Id từ BaseEntity), `customerCode` (String, unique), `name` (String), `phone` (String), `email` (String), `address` (String), `taxCode` (String), `branchId` (Long, nullable=true) | `Customer.java` |
| E07 | `ReceivableDebt` | `receivable_debt` | `com.storename.erp.crm.domain` | `id` (UUID, @Id từ BaseEntity), `customer` (Customer), `branchId` (Long), `totalDebt` (BigDecimal) | `ReceivableDebt.java` |
| E08 | `ReceivableDebtMovement` | `receivable_debt_movement` | `com.storename.erp.crm.domain` | `id` (UUID, @Id), `branchId` (Long), `customer` (Customer), `movementType` (ReceivableDebtMovementType), `amount` (BigDecimal), `balanceBefore` (BigDecimal), `balanceAfter` (BigDecimal), `refType` (String), `refId` (String), `createdAt` (ZonedDateTime), `createdBy` (UUID), `note` (String), `idempotencyKey` (String, unique) | `ReceivableDebtMovement.java` |
| E09 | `Permission` | `permission` | `com.storename.erp.identity.domain` | `id` (Short, @Id), `code` (String, unique), `description` (String) | `Permission.java` |
| E10 | `Role` | `role` | `com.storename.erp.identity.domain` | `id` (Short, @Id), `code` (String, unique), `name` (String) | `Role.java` |
| E11 | `RolePermission` | `role_permission` | `com.storename.erp.identity.domain` | `id` (RolePermissionId, @EmbeddedId), `role` (Role), `permission` (Permission) | `RolePermission.java` |
| E12 | `UserAccount` | `user_account` | `com.storename.erp.identity.domain` | `id` (Long, @Id), `publicId` (UUID, unique), `username` (String, unique), `passwordHash` (String), `fullName` (String), `email` (String), `phone` (String), `isActive` (boolean), `createdAt`, `updatedAt` | `UserAccount.java` |
| E13 | `UserBranchRole` | `user_branch_role` | `com.storename.erp.identity.domain` | `id` (Long, @Id), `user` (UserAccount), `role` (Role), `branch` (Branch) | `UserBranchRole.java` |

Tổng số: 13 entity

## 2. QUAN HỆ
| ID | Lớp.Trường | Loại | Ghi chú | Nguồn |
|---|---|---|---|---|
| R01 | `Category.parent` | `@ManyToOne` | | `Category.java` |
| R02 | `Category.children` | `@OneToMany` | `mappedBy = "parent"` | `Category.java` |
| R03 | `Product.category` | `@ManyToOne` | Bắt buộc (`nullable = false`) | `Product.java` |
| R04 | `PriceList.product` | `@ManyToOne` | Bắt buộc (`nullable = false`) | `PriceList.java` |
| R05 | `ReceivableDebt.customer` | `@ManyToOne` | Bắt buộc (`nullable = false`) | `ReceivableDebt.java` |
| R06 | `ReceivableDebtMovement.customer` | `@ManyToOne` | Bắt buộc (`nullable = false`) | `ReceivableDebtMovement.java` |
| R07 | `RolePermission.role` | `@ManyToOne` | | `RolePermission.java` |
| R08 | `RolePermission.permission` | `@ManyToOne` | | `RolePermission.java` |
| R09 | `UserBranchRole.user` | `@ManyToOne` | Bắt buộc (`nullable = false`) | `UserBranchRole.java` |
| R10 | `UserBranchRole.role` | `@ManyToOne` | Bắt buộc (`nullable = false`) | `UserBranchRole.java` |
| R11 | `UserBranchRole.branch` | `@ManyToOne` | Không bắt buộc | `UserBranchRole.java` |
| R12 | `Customer.branchId` | Tham chiếu logic | Tham chiếu logic qua `branch_id`, nullable | `Customer.java` |
| R13 | `Supplier.branchId` | Tham chiếu logic | Tham chiếu logic qua `branch_id` | `Supplier.java` |
| R14 | `PriceList.branchId` | Tham chiếu logic | Tham chiếu logic qua `branch_id` | `PriceList.java` |
| R15 | `ReceivableDebt.branchId` | Tham chiếu logic | Tham chiếu logic qua `branch_id` | `ReceivableDebt.java` |
| R16 | `ReceivableDebtMovement.branchId` | Tham chiếu logic | Tham chiếu logic qua `branch_id` | `ReceivableDebtMovement.java` |

Tổng số: 16 quan hệ

## 3. ENUM
| ID | Tên | Các giá trị | Nguồn |
|---|---|---|---|
| N01 | `ReceivableDebtMovementType` | `INVOICE`, `PAYMENT`, `RETURN`, `ADJUSTMENT`, `OPENING_BALANCE` | `ReceivableDebtMovementType.java` |
| N02 | JWT Roles | `ADMIN`, `STAFF`, `BRANCH` | `UserBranchRole.java`, `JwtAuthenticationFilter.java` |
| N03 | JWT Claims | `sub` (subject), `role`, `branchId`, `tokenId`, `type` | `JwtTokenProvider.java` |

Tổng số: 3 enum/nhóm hằng số

## 4. PHƯƠNG THỨC
| ID | Phương thức | Lớp | Annotation | Nguồn |
|---|---|---|---|---|
| M01 | `createBranch` | `BranchService` | | `BranchService.java` |
| M02 | `getAllBranches` | `BranchService` | | `BranchService.java` |
| M03 | `getProductBasicInfo` | `CatalogFacade` | | `CatalogFacade.java` |
| M04 | `createCustomer` | `CustomerWriteService` | | `CustomerWriteService.java` |
| M05 | `increaseDebt` | `ReceivableDebtService` | | `ReceivableDebtService.java` |
| M06 | `decreaseDebt` | `ReceivableDebtService` | | `ReceivableDebtService.java` |
| M07 | `setOpeningBalance` | `ReceivableDebtService` | | `ReceivableDebtService.java` |
| M08 | `login` | `AuthService` | | `AuthService.java` |
| M09 | `refresh` | `AuthService` | | `AuthService.java` |
| M10 | `revoke` | `AuthService` | | `AuthService.java` |

Tổng số: 10 phương thức

## 5. ENDPOINT
| ID | Method & Path | Controller | Nguồn |
|---|---|---|---|
| P01 | `POST /login` | `AuthController` | `AuthController.java` |
| P02 | `POST /refresh` | `AuthController` | `AuthController.java` |
| P03 | `POST /revoke` | `AuthController` | `AuthController.java` |
| P04 | `POST /` (Customers) | `CustomerWriteController` | `CustomerWriteController.java` |
| P05 | `PUT /{id}` (Customers) | `CustomerWriteController` | `CustomerWriteController.java` |
| P06 | `DELETE /{id}` (Customers) | `CustomerWriteController` | `CustomerWriteController.java` |
| P07 | `GET /` (Debts) | `ReceivableDebtController` | `ReceivableDebtController.java` |
| P08 | `POST /opening-balance` | `ReceivableDebtController` | `ReceivableDebtController.java` |
| P09 | `GET /{customerId}/movements` | `ReceivableDebtController` | `ReceivableDebtController.java` |
| P10 | `GET /{customerId}/balance` | `ReceivableDebtController` | `ReceivableDebtController.java` |

Tổng số: 10 endpoint

## 6. QUY TẮC NGHIỆP VỤ
| ID | Mô tả | Nguồn |
|---|---|---|
| B01 | Người dùng role `STAFF` bắt buộc phải có `branchId`. | `UserBranchRole.java` |
| B02 | Người dùng role `ADMIN` không được phép có `branchId`. | `UserBranchRole.java` |
| B03 | `balanceBefore` + `amount` bắt buộc phải bằng `balanceAfter` (Invariant violation). | `ReceivableDebtMovement.java` |
| B04 | Refresh token phát hành với subject là ID dạng số sẽ bị từ chối xác thực để bắt đăng nhập lại sinh UUID. | `AuthService.java` |

Tổng số: 4 quy tắc

## 7. ACTOR
| ID | Tên vai trò | Ghi chú |
|---|---|---|
| A01 | ADMIN | Vai trò quản trị viên toàn hệ thống. |
| A02 | STAFF | Nhân viên tại một chi nhánh cụ thể. |
| A03 | BRANCH | Vai trò của một chi nhánh giao tiếp nội bộ qua JWT. |

Tổng số: 3 actor

## 8. SỞ HỮU DỮ LIỆU
| ID | Bảng | Chiều đồng bộ | Publication | Nguồn |
|---|---|---|---|---|
| O01 | `branch` | HQ -> Branch | `pub_hq_to_${BRANCH_ID}` | `setup-replication.sh` |
| O02 | `category` | HQ -> Branch | `pub_hq_to_${BRANCH_ID}` | `setup-replication.sh` |
| O03 | `product` | HQ -> Branch | `pub_hq_to_${BRANCH_ID}` | `setup-replication.sh` |
| O04 | `supplier` | HQ -> Branch | `pub_hq_to_${BRANCH_ID}` | `setup-replication.sh` |
| O05 | `price_list` | HQ -> Branch | `pub_hq_to_${BRANCH_ID}` | `setup-replication.sh` |
| O06 | `customer` | HQ -> Branch | `pub_hq_to_${BRANCH_ID}` | `setup-replication.sh` |
| O07 | `role` | HQ -> Branch | `pub_hq_to_${BRANCH_ID}` | `setup-replication.sh` |
| O08 | `permission` | HQ -> Branch | `pub_hq_to_${BRANCH_ID}` | `setup-replication.sh` |
| O09 | `role_permission` | HQ -> Branch | `pub_hq_to_${BRANCH_ID}` | `setup-replication.sh` |
| O10 | `user_account` | HQ -> Branch | `pub_hq_to_${BRANCH_ID}` | `setup-replication.sh` |
| O11 | `user_branch_role` | HQ -> Branch | `pub_hq_to_${BRANCH_ID}` | `setup-replication.sh` |
| O12 | `inventory_alert_config` | HQ -> Branch | `pub_hq_to_${BRANCH_ID}` | `setup-replication.sh` |
| O13 | `dim_date` | HQ -> Branch | `pub_hq_to_${BRANCH_ID}` | `setup-replication.sh` |
| O14 | `sales_invoice` | Branch -> HQ | `pub_${BRANCH_ID}_to_hq` | `setup-replication.sh` |
| O15 | `sales_invoice_line`| Branch -> HQ | `pub_${BRANCH_ID}_to_hq` | `setup-replication.sh` |
| O16 | `goods_return` | Branch -> HQ | `pub_${BRANCH_ID}_to_hq` | `setup-replication.sh` |
| O17 | `goods_return_line` | Branch -> HQ | `pub_${BRANCH_ID}_to_hq` | `setup-replication.sh` |
| O18 | `inbound_receipt` | Branch -> HQ | `pub_${BRANCH_ID}_to_hq` | `setup-replication.sh` |
| O19 | `inbound_receipt_line`| Branch -> HQ | `pub_${BRANCH_ID}_to_hq` | `setup-replication.sh` |
| O20 | `stock_movement` | Branch -> HQ | `pub_${BRANCH_ID}_to_hq` | `setup-replication.sh` |
| O21 | `cost_layer` | Branch -> HQ | `pub_${BRANCH_ID}_to_hq` | `setup-replication.sh` |

Tổng số: 21 bảng đồng bộ


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
