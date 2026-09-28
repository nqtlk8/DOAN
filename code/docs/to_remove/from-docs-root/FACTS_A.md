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
