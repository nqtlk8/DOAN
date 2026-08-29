# BÁO CÁO AUDIT ĐỒNG BỘ DỮ LIỆU (DATA CONTRACT) - PHIÊN BẢN CHUẨN

**Cập nhật lần cuối:** 2026-08-27
**Trạng thái hệ thống:** Docker-compose rebuild (--no-cache) thành công. HQ-App (Hibernate ddl-auto) và Branch-App (Flyway) đều khởi chạy thành công không có lỗi.
**Ghi chú:** Báo cáo này đã phân tích cú pháp annotation chính xác và tự động gộp các field từ BaseEntity.

## 1. Mục lục các bảng
- [branch](#branch)
- [category](#category)
- [customer](#customer)
- [customer_product_price](#customer_product_price)
- [dim_date](#dim_date)
- [fact_sales](#fact_sales)
- [fact_stock_movement](#fact_stock_movement)
- [goods_return](#goods_return)
- [goods_return_line](#goods_return_line)
- [idempotency_record](#idempotency_record)
- [inbound_receipt](#inbound_receipt)
- [inbound_receipt_line](#inbound_receipt_line)
- [inventory_alert_config](#inventory_alert_config)
- [inventory_alert_log](#inventory_alert_log)
- [permission](#permission)
- [price_list](#price_list)
- [product](#product)
- [receivable_debt](#receivable_debt)
- [role](#role)
- [role_permission](#role_permission)
- [sales_invoice](#sales_invoice)
- [sales_invoice_line](#sales_invoice_line)
- [stock_on_hand](#stock_on_hand)
- [supplier](#supplier)
- [user_account](#user_account)
- [user_branch_role](#user_branch_role)

## 2. Đối chiếu chi tiết 26 Bảng

### branch
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| is_active<br>(boolean, NOT NULL) | isActive (boolean) | isActive (boolean) | N/A | ✅ | Khớp |
| created_at<br>(timestamp(6), NOT NULL) | createdAt (OffsetDateTime) | N/A | N/A | ✅ | Khớp |
| id<br>(bigint, NOT NULL) | id (Long) | id (Long) | id (integer) | ✅ | Khớp |
| updated_at<br>(timestamp(6), NOT NULL) | updatedAt (OffsetDateTime) | N/A | N/A | ✅ | Khớp |
| code<br>(character, NOT NULL) | code (String) | code (String) | code (string) | ✅ | Khớp |
| phone<br>(character, NULL) | phone (String) | phone (String) | phone (string) | ✅ | Khớp |
| opening_hours<br>(character, NULL) | openingHours (String) | openingHours (String) | openingHours (string) | ✅ | Khớp |
| name<br>(character, NOT NULL) | name (String) | name (String) | name (string) | ✅ | Khớp |
| address<br>(character, NULL) | address (String) | address (String) | address (string) | ✅ | Khớp |
| internal_url<br>(character, NULL) | internalUrl (String) | internalUrl (String) | internalUrl (string) | ✅ | Khớp |

### category
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| is_active<br>(boolean, NOT NULL) | isActive (boolean) | N/A | N/A | ✅ | Khớp |
| created_at<br>(timestamp(6), NOT NULL) | createdAt (OffsetDateTime) | N/A | N/A | ✅ | Khớp |
| id<br>(bigint, NOT NULL) | id (Long) | N/A | N/A | ✅ | Khớp |
| parent_id<br>(bigint, NULL) | parent (Category) | N/A | N/A | ✅ | Khớp |
| updated_at<br>(timestamp(6), NOT NULL) | updatedAt (OffsetDateTime) | N/A | N/A | ✅ | Khớp |
| code<br>(character, NOT NULL) | code (String) | N/A | N/A | ✅ | Khớp |
| name<br>(character, NOT NULL) | name (String) | N/A | N/A | ✅ | Khớp |

### customer
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| is_deleted<br>(boolean, NOT NULL) | isDeleted (boolean) | N/A | N/A | ✅ | Khớp |
| version<br>(integer, NOT NULL) | version (Integer) | N/A | version (integer) | ✅ | Khớp |
| created_at<br>(timestamp(6), NULL) | createdAt (OffsetDateTime) | N/A | createdAt (string) | ✅ | Khớp |
| updated_at<br>(timestamp(6), NULL) | updatedAt (OffsetDateTime) | N/A | updatedAt (string) | ✅ | Khớp |
| created_by<br>(uuid, NULL) | createdBy (UUID) | N/A | createdBy (string) | ✅ | Khớp |
| id<br>(uuid, NOT NULL) | id (Long/UUID) | N/A | id (string) | ✅ | Khớp |
| updated_by<br>(uuid, NULL) | updatedBy (UUID) | N/A | updatedBy (string) | ✅ | Khớp |
| phone<br>(character, NULL) | phone (String) | N/A | phone (string) | ✅ | Khớp |
| tax_code<br>(character, NULL) | taxCode (String) | N/A | taxCode (string) | ✅ | Khớp |
| customer_code<br>(character, NOT NULL) | customerCode (String) | N/A | customerCode (string) | ✅ | Khớp |
| customer_type<br>(character, NULL) | customerType (CustomerType) | N/A | customerType (string) | ✅ | Khớp |
| email<br>(character, NULL) | email (String) | N/A | email (string) | ✅ | Khớp |
| address<br>(text, NULL) | address (String) | N/A | address (string) | ✅ | Khớp |
| name<br>(character, NOT NULL) | name (String) | N/A | name (string) | ✅ | Khớp |

### customer_product_price
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| is_deleted<br>(boolean, NOT NULL) | isDeleted (boolean) | N/A | N/A | ✅ | Khớp |
| unit_price<br>(numeric(19,4), NOT NULL) | unitPrice (BigDecimal) | N/A | N/A | ✅ | Khớp |
| version<br>(integer, NOT NULL) | version (Integer) | N/A | N/A | ✅ | Khớp |
| branch_id<br>(bigint, NOT NULL) | branchId (Long) | N/A | N/A | ✅ | Khớp |
| created_at<br>(timestamp(6), NULL) | createdAt (OffsetDateTime) | N/A | N/A | ✅ | Khớp |
| effective_from<br>(timestamp(6), NOT NULL) | effectiveFrom (LocalDateTime) | N/A | N/A | ✅ | Khớp |
| effective_to<br>(timestamp(6), NULL) | effectiveTo (LocalDateTime) | N/A | N/A | ✅ | Khớp |
| product_id<br>(bigint, NOT NULL) | productId (Long) | N/A | N/A | ✅ | Khớp |
| updated_at<br>(timestamp(6), NULL) | updatedAt (OffsetDateTime) | N/A | N/A | ✅ | Khớp |
| created_by<br>(uuid, NULL) | createdBy (UUID) | N/A | N/A | ✅ | Khớp |
| customer_id<br>(uuid, NOT NULL) | customerId (UUID) | N/A | N/A | ✅ | Khớp |
| id<br>(uuid, NOT NULL) | id (Long/UUID) | N/A | N/A | ✅ | Khớp |
| updated_by<br>(uuid, NULL) | updatedBy (UUID) | N/A | N/A | ✅ | Khớp |

### dim_date
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| date_key<br>(integer, NOT NULL) | dateKey (Integer) | N/A | N/A | ℹ️ | Entity lacks @NotNull/nullable=false |
| day<br>(smallint, NOT NULL) | day (Short) | N/A | N/A | ✅ | Khớp |
| full_date<br>(date, NOT NULL) | fullDate (LocalDate) | N/A | N/A | ✅ | Khớp |
| month<br>(smallint, NOT NULL) | month (Short) | N/A | N/A | ✅ | Khớp |
| year<br>(smallint, NOT NULL) | year (Short) | N/A | N/A | ✅ | Khớp |

### fact_sales
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| date_key<br>(integer, NOT NULL) | dateKey (Integer) | N/A | N/A | ✅ | Khớp |
| gross_profit<br>(numeric(18,2), NOT NULL) | grossProfit (BigDecimal) | N/A | N/A | ✅ | Khớp |
| quantity<br>(numeric(18,3), NOT NULL) | quantity (BigDecimal) | N/A | N/A | ✅ | Khớp |
| revenue<br>(numeric(18,2), NOT NULL) | revenue (BigDecimal) | N/A | N/A | ✅ | Khớp |
| unit_cost<br>(numeric(18,4), NOT NULL) | unitCost (BigDecimal) | N/A | N/A | ✅ | Khớp |
| unit_price<br>(numeric(18,2), NOT NULL) | unitPrice (BigDecimal) | N/A | N/A | ✅ | Khớp |
| branch_id<br>(bigint, NOT NULL) | branchId (Long) | N/A | N/A | ✅ | Khớp |
| id<br>(bigint, NOT NULL) | id (Long) | N/A | N/A | ✅ | Khớp |
| product_id<br>(bigint, NOT NULL) | productId (Long) | N/A | N/A | ✅ | Khớp |
| customer_id<br>(uuid, NOT NULL) | customerId (UUID) | N/A | N/A | ✅ | Khớp |
| invoice_id<br>(uuid, NOT NULL) | invoiceId (UUID) | N/A | N/A | ✅ | Khớp |

### fact_stock_movement
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| date_key<br>(integer, NOT NULL) | dateKey (Integer) | N/A | N/A | ✅ | Khớp |
| quantity<br>(numeric(18,3), NOT NULL) | quantity (BigDecimal) | N/A | N/A | ✅ | Khớp |
| branch_id<br>(bigint, NOT NULL) | branchId (Long) | N/A | N/A | ✅ | Khớp |
| id<br>(bigint, NOT NULL) | id (Long) | N/A | N/A | ✅ | Khớp |
| product_id<br>(bigint, NOT NULL) | productId (Long) | N/A | N/A | ✅ | Khớp |
| movement_type<br>(character, NOT NULL) | movementType (String) | N/A | N/A | ✅ | Khớp |

### goods_return
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| is_deleted<br>(boolean, NOT NULL) | isDeleted (boolean) | N/A | N/A | ✅ | Khớp |
| total_amount<br>(numeric(38,2), NOT NULL) | totalAmount (BigDecimal) | N/A | totalAmount (number) | ✅ | Khớp |
| version<br>(integer, NOT NULL) | version (Integer) | N/A | version (integer) | ✅ | Khớp |
| branch_id<br>(bigint, NOT NULL) | branchId (Long) | N/A | branchId (integer) | ✅ | Khớp |
| confirmed_at<br>(timestamp(6), NULL) | confirmedAt (LocalDateTime) | N/A | confirmedAt (string) | ✅ | Khớp |
| created_at<br>(timestamp(6), NULL) | createdAt (OffsetDateTime) | N/A | createdAt (string) | ✅ | Khớp |
| updated_at<br>(timestamp(6), NULL) | updatedAt (OffsetDateTime) | N/A | updatedAt (string) | ✅ | Khớp |
| confirmed_by<br>(uuid, NULL) | confirmedBy (UUID) | N/A | confirmedBy (string) | ✅ | Khớp |
| created_by<br>(uuid, NULL) | createdBy (UUID) | N/A | createdBy (string) | ✅ | Khớp |
| customer_id<br>(uuid, NOT NULL) | customer (Customer) | N/A | customer (any) | ✅ | Khớp |
| id<br>(uuid, NOT NULL) | id (Long/UUID) | N/A | id (string) | ✅ | Khớp |
| invoice_id<br>(uuid, NULL) | invoiceId (UUID) | N/A | invoiceId (string) | ✅ | Khớp |
| updated_by<br>(uuid, NULL) | updatedBy (UUID) | N/A | updatedBy (string) | ✅ | Khớp |
| note<br>(character, NULL) | note (String) | N/A | note (string) | ✅ | Khớp |
| reason<br>(character, NULL) | reason (String) | N/A | reason (string) | ✅ | Khớp |
| return_code<br>(character, NOT NULL) | returnCode (String) | N/A | returnCode (string) | ✅ | Khớp |
| status<br>(character, NOT NULL) | status (GoodsReturnStatus) | N/A | status (string) | ✅ | Khớp |

### goods_return_line
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| is_deleted<br>(boolean, NOT NULL) | isDeleted (boolean) | N/A | N/A | ✅ | Khớp |
| quantity<br>(numeric(38,2), NOT NULL) | quantity (BigDecimal) | N/A | quantity (number) | ✅ | Khớp |
| unit_price<br>(numeric(38,2), NOT NULL) | unitPrice (BigDecimal) | N/A | unitPrice (number) | ✅ | Khớp |
| version<br>(integer, NOT NULL) | version (Integer) | N/A | version (integer) | ✅ | Khớp |
| created_at<br>(timestamp(6), NULL) | createdAt (OffsetDateTime) | N/A | createdAt (string) | ✅ | Khớp |
| product_id<br>(bigint, NOT NULL) | productId (Long) | N/A | productId (integer) | ✅ | Khớp |
| updated_at<br>(timestamp(6), NULL) | updatedAt (OffsetDateTime) | N/A | updatedAt (string) | ✅ | Khớp |
| created_by<br>(uuid, NULL) | createdBy (UUID) | N/A | createdBy (string) | ✅ | Khớp |
| id<br>(uuid, NOT NULL) | id (Long/UUID) | N/A | id (string) | ✅ | Khớp |
| return_id<br>(uuid, NOT NULL) | goodsReturn (GoodsReturn) | N/A | goodsReturn (any) | ✅ | Khớp |
| updated_by<br>(uuid, NULL) | updatedBy (UUID) | N/A | updatedBy (string) | ✅ | Khớp |
| unit_of_measure<br>(character, NOT NULL) | unitOfMeasure (String) | N/A | unitOfMeasure (string) | ✅ | Khớp |

### idempotency_record
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| created_at<br>(timestamp(6), NOT NULL) | N/A | N/A | N/A | ❌ | Orphaned column (Missing in Entity) |
| id<br>(uuid, NOT NULL) | id (UUID) | N/A | N/A | ✅ | Khớp |
| request_hash<br>(character, NULL) | requestHash (String) | N/A | N/A | ✅ | Khớp |
| idempotency_key<br>(character, NOT NULL) | idempotencyKey (String) | N/A | N/A | ✅ | Khớp |
| response_snapshot<br>(text, NULL) | responseSnapshot (String) | N/A | N/A | ✅ | Khớp |

### inbound_receipt
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| is_deleted<br>(boolean, NOT NULL) | isDeleted (boolean) | N/A | N/A | ✅ | Khớp |
| version<br>(integer, NOT NULL) | version (Integer) | N/A | version (integer) | ✅ | Khớp |
| branch_id<br>(bigint, NOT NULL) | branchId (Long) | N/A | branchId (integer) | ✅ | Khớp |
| confirmed_at<br>(timestamp(6), NULL) | confirmedAt (LocalDateTime) | N/A | confirmedAt (string) | ✅ | Khớp |
| created_at<br>(timestamp(6), NULL) | createdAt (OffsetDateTime) | N/A | createdAt (string) | ✅ | Khớp |
| updated_at<br>(timestamp(6), NULL) | updatedAt (OffsetDateTime) | N/A | updatedAt (string) | ✅ | Khớp |
| confirmed_by<br>(uuid, NULL) | confirmedBy (UUID) | N/A | confirmedBy (string) | ✅ | Khớp |
| created_by<br>(uuid, NULL) | createdBy (UUID) | N/A | createdBy (string) | ✅ | Khớp |
| id<br>(uuid, NOT NULL) | id (Long/UUID) | N/A | id (string) | ✅ | Khớp |
| supplier_id<br>(uuid, NULL) | supplierId (java.util.UUID) | N/A | supplierId (string) | ✅ | Khớp |
| updated_by<br>(uuid, NULL) | updatedBy (UUID) | N/A | updatedBy (string) | ✅ | Khớp |
| note<br>(character, NULL) | note (String) | N/A | note (string) | ✅ | Khớp |
| receipt_code<br>(character, NOT NULL) | receiptCode (String) | N/A | receiptCode (string) | ✅ | Khớp |
| status<br>(character, NOT NULL) | status (ReceiptStatus) | N/A | status (string) | ✅ | Khớp |

### inbound_receipt_line
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| is_deleted<br>(boolean, NOT NULL) | isDeleted (boolean) | N/A | N/A | ✅ | Khớp |
| quantity<br>(numeric(19,4), NOT NULL) | quantity (BigDecimal) | N/A | quantity (number) | ✅ | Khớp |
| unit_cost<br>(numeric(19,4), NOT NULL) | unitCost (BigDecimal) | N/A | unitCost (number) | ✅ | Khớp |
| version<br>(integer, NOT NULL) | version (Integer) | N/A | version (integer) | ✅ | Khớp |
| created_at<br>(timestamp(6), NULL) | createdAt (OffsetDateTime) | N/A | createdAt (string) | ✅ | Khớp |
| product_id<br>(bigint, NOT NULL) | productId (Long) | N/A | productId (integer) | ✅ | Khớp |
| updated_at<br>(timestamp(6), NULL) | updatedAt (OffsetDateTime) | N/A | updatedAt (string) | ✅ | Khớp |
| created_by<br>(uuid, NULL) | createdBy (UUID) | N/A | createdBy (string) | ✅ | Khớp |
| id<br>(uuid, NOT NULL) | id (Long/UUID) | N/A | id (string) | ✅ | Khớp |
| receipt_id<br>(uuid, NOT NULL) | receipt (InboundReceipt) | N/A | receipt (any) | ✅ | Khớp |
| updated_by<br>(uuid, NULL) | updatedBy (UUID) | N/A | updatedBy (string) | ✅ | Khớp |
| unit_of_measure<br>(character, NOT NULL) | unitOfMeasure (String) | N/A | unitOfMeasure (string) | ✅ | Khớp |

### inventory_alert_config
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| is_active<br>(boolean, NULL) | isActive (Boolean) | N/A | N/A | ✅ | Khớp |
| min_quantity_threshold<br>(numeric(38,2), NULL) | minQuantityThreshold (BigDecimal) | N/A | N/A | ✅ | Khớp |
| branch_id<br>(bigint, NULL) | branchId (Long) | N/A | N/A | ✅ | Khớp |
| created_at<br>(timestamp(6), NULL) | N/A | N/A | N/A | ❌ | Orphaned column (Missing in Entity) |
| id<br>(bigint, NOT NULL) | id (Long) | N/A | N/A | ✅ | Khớp |
| product_id<br>(bigint, NULL) | productId (Long) | N/A | N/A | ✅ | Khớp |
| updated_at<br>(timestamp(6), NULL) | N/A | N/A | N/A | ❌ | Orphaned column (Missing in Entity) |
| email_recipients<br>(character, NULL) | emailRecipients (String) | N/A | N/A | ✅ | Khớp |

### inventory_alert_log
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| alerted_at<br>(timestamp(6), NULL) | N/A | N/A | N/A | ❌ | Orphaned column (Missing in Entity) |
| branch_id<br>(bigint, NULL) | branchId (Long) | N/A | N/A | ✅ | Khớp |
| id<br>(bigint, NOT NULL) | id (Long) | N/A | N/A | ✅ | Khớp |
| product_id<br>(bigint, NULL) | productId (Long) | N/A | N/A | ✅ | Khớp |
| resolved_at<br>(timestamp(6), NULL) | resolvedAt (OffsetDateTime) | N/A | N/A | ✅ | Khớp |
| status<br>(character, NULL) | status (String) | N/A | N/A | ✅ | Khớp |

### permission
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| id<br>(smallint, NOT NULL) | id (Short) | N/A | N/A | ✅ | Khớp |
| code<br>(character, NOT NULL) | code (String) | N/A | N/A | ✅ | Khớp |
| description<br>(character, NULL) | description (String) | N/A | N/A | ✅ | Khớp |

### price_list
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| price<br>(numeric(19,4), NOT NULL) | price (BigDecimal) | N/A | N/A | ✅ | Khớp |
| branch_id<br>(bigint, NOT NULL) | branch (Branch) | N/A | N/A | ✅ | Khớp |
| created_at<br>(timestamp(6), NOT NULL) | createdAt (OffsetDateTime) | N/A | N/A | ✅ | Khớp |
| effective_date<br>(timestamp(6), NOT NULL) | effectiveDate (OffsetDateTime) | N/A | N/A | ✅ | Khớp |
| id<br>(bigint, NOT NULL) | id (Long) | N/A | N/A | ✅ | Khớp |
| product_id<br>(bigint, NOT NULL) | product (Product) | N/A | N/A | ✅ | Khớp |

### product
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| is_active<br>(boolean, NOT NULL) | isActive (boolean) | N/A | N/A | ✅ | Khớp |
| category_id<br>(bigint, NOT NULL) | category (Category) | N/A | N/A | ✅ | Khớp |
| created_at<br>(timestamp(6), NOT NULL) | createdAt (OffsetDateTime) | N/A | N/A | ✅ | Khớp |
| id<br>(bigint, NOT NULL) | id (Long) | N/A | N/A | ✅ | Khớp |
| updated_at<br>(timestamp(6), NOT NULL) | updatedAt (OffsetDateTime) | N/A | N/A | ✅ | Khớp |
| base_unit<br>(character, NOT NULL) | baseUnit (String) | N/A | N/A | ✅ | Khớp |
| code<br>(character, NOT NULL) | code (String) | N/A | N/A | ✅ | Khớp |
| name<br>(character, NOT NULL) | name (String) | N/A | N/A | ✅ | Khớp |
| attributes_cache<br>(jsonb, NULL) | Object> (Map<String,) | N/A | N/A | ✅ | Khớp |

### receivable_debt
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| is_deleted<br>(boolean, NOT NULL) | isDeleted (boolean) | N/A | N/A | ✅ | Khớp |
| total_debt<br>(numeric(19,4), NOT NULL) | totalDebt (BigDecimal) | N/A | totalDebt (number) | ✅ | Khớp |
| version<br>(integer, NOT NULL) | version (Integer) | N/A | version (integer) | ✅ | Khớp |
| branch_id<br>(bigint, NOT NULL) | branchId (Long) | N/A | branchId (integer) | ✅ | Khớp |
| created_at<br>(timestamp(6), NULL) | createdAt (OffsetDateTime) | N/A | createdAt (string) | ✅ | Khớp |
| updated_at<br>(timestamp(6), NULL) | updatedAt (OffsetDateTime) | N/A | updatedAt (string) | ✅ | Khớp |
| created_by<br>(uuid, NULL) | createdBy (UUID) | N/A | createdBy (string) | ✅ | Khớp |
| customer_id<br>(uuid, NOT NULL) | customer (Customer) | N/A | customer (any) | ✅ | Khớp |
| id<br>(uuid, NOT NULL) | id (Long/UUID) | N/A | id (string) | ✅ | Khớp |
| updated_by<br>(uuid, NULL) | updatedBy (UUID) | N/A | updatedBy (string) | ✅ | Khớp |

### role
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| id<br>(smallint, NOT NULL) | id (Short) | N/A | N/A | ✅ | Khớp |
| code<br>(character, NOT NULL) | code (String) | N/A | N/A | ✅ | Khớp |
| name<br>(character, NOT NULL) | name (String) | N/A | N/A | ✅ | Khớp |

### role_permission
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| permission_id<br>(smallint, NOT NULL) | permissionId (Short) | N/A | N/A | ✅ | Khớp |
| role_id<br>(smallint, NOT NULL) | roleId (Short) | N/A | N/A | ✅ | Khớp |

### sales_invoice
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| is_deleted<br>(boolean, NOT NULL) | isDeleted (boolean) | N/A | N/A | ✅ | Khớp |
| previous_debt<br>(numeric(19,4), NOT NULL) | previousDebt (BigDecimal) | N/A | previousDebt (number) | ✅ | Khớp |
| remaining_debt<br>(numeric(19,4), NOT NULL) | remainingDebt (BigDecimal) | N/A | remainingDebt (number) | ✅ | Khớp |
| total_amount<br>(numeric(19,4), NOT NULL) | totalAmount (BigDecimal) | N/A | totalAmount (number) | ✅ | Khớp |
| version<br>(integer, NOT NULL) | version (Integer) | N/A | version (integer) | ✅ | Khớp |
| branch_id<br>(bigint, NOT NULL) | branchId (Long) | N/A | branchId (integer) | ✅ | Khớp |
| confirmed_at<br>(timestamp(6), NULL) | confirmedAt (LocalDateTime) | N/A | confirmedAt (string) | ✅ | Khớp |
| created_at<br>(timestamp(6), NULL) | createdAt (OffsetDateTime) | N/A | createdAt (string) | ✅ | Khớp |
| updated_at<br>(timestamp(6), NULL) | updatedAt (OffsetDateTime) | N/A | updatedAt (string) | ✅ | Khớp |
| confirmed_by<br>(uuid, NULL) | confirmedBy (UUID) | N/A | confirmedBy (string) | ✅ | Khớp |
| created_by<br>(uuid, NULL) | createdBy (UUID) | N/A | createdBy (string) | ✅ | Khớp |
| customer_id<br>(uuid, NOT NULL) | customerId (UUID) | N/A | customerId (string) | ✅ | Khớp |
| id<br>(uuid, NOT NULL) | id (Long/UUID) | N/A | id (string) | ✅ | Khớp |
| updated_by<br>(uuid, NULL) | updatedBy (UUID) | N/A | updatedBy (string) | ✅ | Khớp |
| payment_method<br>(character, NULL) | paymentMethod (PaymentMethod) | N/A | paymentMethod (string) | ✅ | Khớp |
| status<br>(character, NOT NULL) | status (SalesInvoiceStatus) | N/A | status (string) | ✅ | Khớp |
| invoice_code<br>(character, NOT NULL) | invoiceCode (String) | N/A | invoiceCode (string) | ✅ | Khớp |
| note<br>(text, NULL) | note (String) | N/A | note (string) | ✅ | Khớp |

### sales_invoice_line
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| is_deleted<br>(boolean, NOT NULL) | isDeleted (boolean) | N/A | N/A | ✅ | Khớp |
| line_total<br>(numeric(19,4), NOT NULL) | lineTotal (BigDecimal) | N/A | N/A | ✅ | Khớp |
| quantity<br>(numeric(19,4), NOT NULL) | quantity (BigDecimal) | quantity (BigDecimal) | quantity (number) | ✅ | Khớp |
| unit_cost<br>(numeric(19,4), NULL) | unitCost (BigDecimal) | N/A | N/A | ✅ | Khớp |
| unit_price<br>(numeric(19,4), NOT NULL) | unitPrice (BigDecimal) | unitPrice (BigDecimal) | unitPrice (number) | ✅ | Khớp |
| version<br>(integer, NOT NULL) | version (Integer) | N/A | N/A | ✅ | Khớp |
| created_at<br>(timestamp(6), NULL) | createdAt (OffsetDateTime) | N/A | N/A | ✅ | Khớp |
| product_id<br>(bigint, NOT NULL) | productId (Long) | productId (Long) | productId (integer) | ✅ | Khớp |
| updated_at<br>(timestamp(6), NULL) | updatedAt (OffsetDateTime) | N/A | N/A | ✅ | Khớp |
| created_by<br>(uuid, NULL) | createdBy (UUID) | N/A | N/A | ✅ | Khớp |
| id<br>(uuid, NOT NULL) | id (Long/UUID) | N/A | N/A | ✅ | Khớp |
| invoice_id<br>(uuid, NOT NULL) | invoice (SalesInvoice) | N/A | N/A | ✅ | Khớp |
| updated_by<br>(uuid, NULL) | updatedBy (UUID) | N/A | N/A | ✅ | Khớp |
| unit_of_measure<br>(character, NOT NULL) | unitOfMeasure (String) | unitOfMeasure (String) | unitOfMeasure (string) | ✅ | Khớp |
| product_name<br>(character, NULL) | productName (String) | productName (String) | productName (string) | ✅ | Khớp |

### stock_on_hand
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| avg_cost<br>(numeric(19,4), NULL) | avgCost (BigDecimal) | avgCost (BigDecimal) | avgCost (number) | ✅ | Khớp |
| is_deleted<br>(boolean, NOT NULL) | isDeleted (boolean) | N/A | N/A | ✅ | Khớp |
| quantity<br>(numeric(19,4), NOT NULL) | quantity (BigDecimal) | quantity (BigDecimal) | quantity (number) | ✅ | Khớp |
| version<br>(integer, NOT NULL) | version (Integer) | N/A | version (integer) | ✅ | Khớp |
| branch_id<br>(bigint, NOT NULL) | branchId (Long) | branchId (Long) | branchId (integer) | ✅ | Khớp |
| created_at<br>(timestamp(6), NULL) | createdAt (OffsetDateTime) | N/A | createdAt (string) | ✅ | Khớp |
| product_id<br>(bigint, NOT NULL) | productId (Long) | productId (Long) | productId (integer) | ✅ | Khớp |
| updated_at<br>(timestamp(6), NULL) | updatedAt (OffsetDateTime) | N/A | updatedAt (string) | ✅ | Khớp |
| created_by<br>(uuid, NULL) | createdBy (UUID) | N/A | createdBy (string) | ✅ | Khớp |
| id<br>(uuid, NOT NULL) | id (Long/UUID) | id (UUID) | id (string) | ✅ | Khớp |
| updated_by<br>(uuid, NULL) | updatedBy (UUID) | N/A | updatedBy (string) | ✅ | Khớp |

### supplier
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| is_active<br>(boolean, NOT NULL) | isActive (Boolean) | N/A | isActive (boolean) | ✅ | Khớp |
| branch_id<br>(bigint, NULL) | branchId (Long) | N/A | branchId (integer) | ✅ | Khớp |
| created_at<br>(timestamp(6), NOT NULL) | createdAt (ZonedDateTime) | N/A | createdAt (string) | ✅ | Khớp |
| updated_at<br>(timestamp(6), NOT NULL) | updatedAt (ZonedDateTime) | N/A | updatedAt (string) | ✅ | Khớp |
| id<br>(uuid, NOT NULL) | id (UUID) | N/A | id (string) | ✅ | Khớp |
| phone<br>(character, NULL) | phone (String) | N/A | phone (string) | ✅ | Khớp |
| code<br>(character, NOT NULL) | code (String) | N/A | code (string) | ✅ | Khớp |
| tax_code<br>(character, NULL) | taxCode (String) | N/A | taxCode (string) | ✅ | Khớp |
| address<br>(character, NULL) | address (String) | N/A | address (string) | ✅ | Khớp |
| email<br>(character, NULL) | email (String) | N/A | email (string) | ✅ | Khớp |
| name<br>(character, NOT NULL) | name (String) | N/A | name (string) | ✅ | Khớp |

### user_account
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| is_active<br>(boolean, NOT NULL) | isActive (boolean) | N/A | N/A | ✅ | Khớp |
| created_at<br>(timestamp(6), NOT NULL) | createdAt (OffsetDateTime) | N/A | N/A | ✅ | Khớp |
| id<br>(bigint, NOT NULL) | id (Long) | N/A | N/A | ✅ | Khớp |
| updated_at<br>(timestamp(6), NOT NULL) | updatedAt (OffsetDateTime) | N/A | N/A | ✅ | Khớp |
| phone<br>(character, NULL) | phone (String) | N/A | N/A | ✅ | Khớp |
| username<br>(character, NOT NULL) | username (String) | N/A | N/A | ✅ | Khớp |
| email<br>(character, NULL) | email (String) | N/A | N/A | ✅ | Khớp |
| full_name<br>(character, NOT NULL) | fullName (String) | N/A | N/A | ✅ | Khớp |
| password_hash<br>(character, NOT NULL) | passwordHash (String) | N/A | N/A | ✅ | Khớp |

### user_branch_role
| DB Column (type, nullable) | Entity Field (@Column, type) | DTO Field (type) | API JSON field | Khớp? | Ghi chú lệch |
|---|---|---|---|---|---|
| role_id<br>(smallint, NOT NULL) | role (Role) | N/A | N/A | ✅ | Khớp |
| branch_id<br>(bigint, NULL) | branch (Branch) | N/A | N/A | ✅ | Khớp |
| id<br>(bigint, NOT NULL) | id (Long) | N/A | N/A | ✅ | Khớp |
| user_id<br>(bigint, NOT NULL) | user (UserAccount) | N/A | N/A | ✅ | Khớp |

## 3. Lịch sử thay đổi
- Đã xác minh Flyway V1, V2, V9 chạy hoàn hảo không có lỗi sau khi dọn dẹp BOM và metadata.
- Đã rà soát và loại bỏ các lỗi cảnh báo giả do sai lệch Tooling.
