# Core Workflows — v5

## 1. Quy tắc đọc workflow

Mỗi request có thể được nhìn qua các lớp:

```text
Client
 -> Nginx / Reverse Proxy
 -> Spring Security Filter Chain
 -> Controller
 -> AOP (nếu có)
 -> Application Service / Repository
 -> Hibernate/JPA
 -> PostgreSQL
```

Không phải endpoint nào cũng đi qua mọi lớp AOP; chỉ method được annotation tương ứng mới được aspect intercept.

## 2. Docker startup

```text
Start docker-compose
        |
        +--> hq-db
        +--> branch-tp1-db
        +--> branch-tp2-db
        +--> redis-hq
        |
        +--> hq-app (profile=hq)
        +--> branch-tp1-app (profile=branch, BRANCH_ID=TP1)
        +--> branch-tp2-app (profile=branch, BRANCH_ID=TP2)
        |
        +--> erp-frontend
        +--> branch-tp1-nginx
        +--> branch-tp2-nginx
        +--> web-public
```

Backend chỉ khởi động sau khi database dependency tương ứng healthy theo `depends_on` trong compose.

## 3. Login flow

```text
Browser
  |
  | POST /api/v1/auth/login
  v
erp-frontend / Nginx
  |
  | proxy /api/*
  v
hq-app:8080
  |
  v
Security Filter Chain
  |
  | /api/v1/auth/** is permitAll
  v
AuthController
  |
  v
AuthService
  |
  +--> UserRepository
  +--> UserBranchRoleRepository
  +--> PasswordEncoder (BCrypt)
  |
  v
JwtTokenProvider
  |
  | RSA private key + RS256
  v
Access Token + Refresh Token
  |
  v
Client
```

Access token hiện có thời hạn mặc định 30 phút; refresh token mặc định 7 ngày trong `JwtTokenProvider`.

## 4. Protected request

Sau login, client gửi:

```http
Authorization: Bearer <access-token>
```

JWT filter:

1. lấy token từ Authorization header;
2. parse và verify bằng public key;
3. kiểm tra `type=access`;
4. đọc `username`, `role`, `branchId`, `tokenId`;
5. tạo Authentication;
6. ghi Authentication vào SecurityContext.

Role được tạo thành authority bằng chính string name (không có tiền tố `ROLE_`).

## 5. Ví dụ: ADMIN tạo customer

```text
Admin thao tác UI
       |
       | POST /api/v1/customers
       | Authorization: Bearer <JWT>
       v
erp-frontend Nginx
       |
       v
hq-app
       |
       v
JwtAuthenticationFilter
       |
       | verify JWT
       v
CustomerController
       |
       | @PreAuthorize(...)
       v
CustomerRepository.save()
       |
       v
Hibernate/JPA
       |
       v
HQ PostgreSQL
       |
       v
201 Created
```

### Dữ liệu request

```json
{
  "customerCode": "KH001",
  "name": "Nguyen Van A",
  "phone": "0901234567",
  "email": "a@example.com",
  "address": "...",
  "taxCode": "0123456789",
  "customerType": "RETAIL"
}
```

### Điểm cần chú ý

`CustomerWriteController` hiện tạo customer trực tiếp qua `CustomerRepository`, không qua `CustomerService`.

Annotation hiện tại là:

```java
@PreAuthorize("hasAuthority('ADMIN')")
```

Hệ thống đã loại bỏ cơ chế `ROLE_` và hoàn toàn dựa trên chuỗi role gốc. Do đó authority này đồng nhất với role `ADMIN` trong token.

## 6. Sales Invoice — Create Draft

```text
POST /api/v1/sales-invoices
        |
        +--> authenticate JWT
        +--> branch scope
        +--> validate request
        +--> SalesInvoiceService.createDraft()
        +--> save invoice + lines
        |
        v
HTTP 201
```

Chưa thay đổi tồn kho và công nợ ở bước Draft.

## 7. Sales Invoice — Confirm

```text
POST /api/v1/sales-invoices/{id}/confirm
Idempotency-Key: <key>
        |
        v
JwtAuthenticationFilter
        |
        v
BranchScopedAspect
        |
        v
IdempotencyAspect
        |
        +--> request hash
        +--> check idempotency_record
        |
        v
SalesInvoiceService.confirmInvoice()
        |
        +--> load invoice
        +--> load stock
        +--> lock stock row, check quantity (không đủ -> 409 INSUFFICIENT_STOCK, rollback cả hóa đơn)
        +--> snapshot unit cost (FIFO)
        +--> decrease stock
        +--> save customer price (CustomerProductPriceService.applyFromInvoice)
        +--> update receivable debt
        +--> snapshot previous/remaining debt
        |
        v
PostgreSQL transaction commit
```

Nếu idempotency key đã tồn tại và request hash giống nhau, hệ thống có thể trả lại response snapshot thay vì chạy lại nghiệp vụ.

### 7.1. Giá bán theo khách (từ 2026-10-10, migration V9)

- **Xác định giá khi lập phiếu** (`GET /api/v1/customer-prices/{customerId}/resolve`): giá riêng của khách còn hiệu lực (`customer_product_price`) > giá niêm yết chi nhánh (`price_list`, `effective_date <= now` mới nhất) > nhân viên nhập tay. Form bán hàng tra giá khi chọn khách và khi chọn sản phẩm; dòng đã sửa giá tay được giữ nguyên khi đổi khách.
- **Nhân viên được sửa giá.** Dòng có giá khác giá đang áp dụng được tô nổi kèm ghi chú "Giá này sẽ được lưu làm giá riêng cho khách khi lưu đơn".
- **Khi xác nhận hóa đơn**, trong cùng transaction với trừ kho và công nợ: với mỗi sản phẩm (lặp nhiều dòng thì lấy dòng cuối), nếu giá khác giá đang áp dụng thì upsert `customer_product_price` theo (`customer_id`, `product_id`, `branch_id`) và ghi 1 dòng `customer_product_price_history`. Khách chưa có giá riêng mà giá bằng giá niêm yết thì không ghi.
- `sales_invoice_line.unit_price` vẫn là **snapshot bất biến** của hóa đơn; đổi giá niêm yết hay giá riêng sau đó không ảnh hưởng hóa đơn cũ.

## 8. Inbound Receipt

```text
Create Draft
    -> Save Receipt
    -> Confirm
    -> Convert Unit
    -> Calculate Cost
    -> Increase Stock
    -> Update Average Cost
```

Phiếu nhập chọn Supplier trực tiếp. Không có Purchase Order trong workflow hiện hành.

## 9. Goods Return

```text
Create Return Draft
      |
      v
Confirm Return
      |
      +--> validate original invoice / quantity
      +--> resolve return unit cost (goods_return_line.unit_cost, V10):
      |      có hóa đơn gốc -> sales_invoice_line.unit_cost của dòng khớp
      |      không có       -> unit_cost lần bán gần nhất SP đó cho khách đó; không có -> lớp gần nhất; không có -> 0
      +--> create internal inbound movement
      +--> increase stock + cost layer RETURN theo GIÁ VỐN trên (không theo giá bán)
      +--> adjust customer debt
      |
      v
Commit
```

## 10. Không cho tồn âm (từ 2026-10-11)

Nhập trước, bán sau. Khi xác nhận hóa đơn (`InventoryFacadeImpl.recordSaleAndGetCost`), với từng dòng:

1. Khóa ghi dòng `stock_on_hand` (`StockOnHandRepository.findForUpdate`) để hai hóa đơn đồng thời không cùng vượt qua bước kiểm tra.
2. Tồn < số lượng bán -> `StockInsufficientException` -> HTTP **409**, `errors = ["INSUFFICIENT_STOCK"]`, message hiển thị thẳng cho nhân viên:
   `Không đủ hàng: [Mã] Tên SP — tồn kho còn X, cần Y. Vui lòng nhập hàng trước khi bán.`
   Toàn bộ hóa đơn rollback (không trừ kho, không ghi công nợ, không lưu giá riêng).
3. `FifoCostService.consume` cũng từ chối khi tổng `remaining_qty` của các lớp giá vốn không đủ (tồn kho và lớp lệch nhau). Không còn sinh `cost_basis = NO_LAYER`.

Hóa đơn nháp (`createDraft`) không kiểm tra tồn; kiểm tra chạy lúc xác nhận. Form bán hàng tạo + xác nhận trong một bước.

### 10.1. Giá vốn trên dashboard

`AnalyticsDataAdapter` tính số **thuần**, mỗi chứng từ theo `confirmed_at` của chính nó (hàng trả trừ vào kỳ xác nhận phiếu trả):

- Doanh thu = `SUM(sales_invoice_line.line_total)` − `SUM(goods_return_line.quantity × unit_price)`
- Giá vốn = `SUM(sales_invoice_line.quantity × unit_cost)` − `SUM(goods_return_line.quantity × unit_cost)`
- Top sản phẩm: số lượng và doanh thu thuần; sản phẩm có số lượng thuần ≤ 0 bị bỏ.

## 11. Error path

```text
JWT invalid
  -> HTTP 401

Authentication/authorization failure
  -> HTTP 401/403 tùy cơ chế thực thi thực tế

Business exception / validation
  -> transaction rollback
  -> global error handling

Idempotency conflict
  -> HTTP 409

Database concurrency
  -> rollback
  -> retry nếu exception phù hợp được cấu hình
```
