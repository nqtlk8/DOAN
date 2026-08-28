# THIẾT KẾ KIẾN TRÚC HỆ THỐNG WEBSITE CỬA HÀNG VLXD & TTNT — v4

**Vai trò:** Software Architect
**Kiến trúc:** Modular Monolith, triển khai **1 instance riêng cho mỗi chi nhánh + 1 instance HQ**
**Stack:** Next.js (3 ứng dụng frontend tách biệt) — Spring Boot (Backend) — PostgreSQL Replication (Hub-and-Spoke) — JWT (RS256)

> **Ghi chú phiên bản v4:** Chốt mô hình phân quyền — **RBAC rút gọn còn 2 role (`STAFF`, `ADMIN`)**, và quan trọng nhất: **Admin chỉ hoạt động tại HQ instance, không bao giờ có đường kết nối tới bất kỳ Branch instance nào** (không còn bước "chọn chi nhánh" ở luồng đăng nhập như đã cân nhắc và loại bỏ). Đây là cách khử triệt để rủi ro ghi nhầm vào dữ liệu master tại Branch DB (đã phân tích ở phiên bản trao đổi trước) — loại bỏ hẳn đường đi, không chỉ canh chặn trên đường đi. Kéo theo đó, kiến trúc frontend được xác nhận tách thành **3 ứng dụng độc lập**: Public Site, Branch ERP, HQ Admin/Dashboard. Toàn bộ thiết kế nghiệp vụ (Phần 5.2), cơ chế replication (Phần 4.1-4.4), idempotency key (Phần 5.2.10) **giữ nguyên không đổi** so với v3. Các phần thay đổi đánh dấu **[v4]**.

---

## PHẦN 1 — NGHIÊN CỨU & TỔNG HỢP ĐẶC ĐIỂM NGÀNH

*(Không đổi so với v3.)*

---

## PHẦN 2 — TỔNG HỢP YÊU CẦU HỆ THỐNG

### 2.1 Yêu cầu chức năng (Functional Requirements)

*(Không đổi so với v3 — FR1 đến FR33.)*

**[v4] Cập nhật FR19 (đã có ở v2/v3):**

- FR19 **[v4 — cập nhật]**: Quản lý người dùng nội bộ, phân quyền theo **2 role duy nhất**: `STAFF` (toàn quyền trong phạm vi 1 chi nhánh cố định) và `ADMIN` (toàn quyền toàn hệ thống, chỉ thao tác tại HQ). Thay thế hoàn toàn mô hình 5 role (`ADMIN_TONG`/`QUAN_LY_CHI_NHANH`/`NHAN_VIEN_BAN_HANG`/`THU_KHO`/`KE_TOAN`) đã đề cập ở các phiên bản trước.

### 2.2 Yêu cầu phi chức năng (Non-Functional Requirements) **[v4 — cập nhật dòng Bảo mật]**

| Nhóm | Yêu cầu |
|---|---|
| Hiệu năng | *(không đổi so với v3)* |
| Khả năng mở rộng | *(không đổi so với v3)* |
| Sẵn sàng cao | *(không đổi so với v3)* |
| Nhất quán dữ liệu | *(không đổi so với v3)* |
| Bảo mật **[v4]** | JWT RS256 — HQ giữ private key. RBAC 2 role (`STAFF`/`ADMIN`), phân tách theo **instance** thay vì chỉ theo claim: `ADMIN` chỉ được cấp quyền truy cập mạng tới HQ instance (reverse proxy Branch KHÔNG cho phép kết nối từ ngoài mạng nội bộ chi nhánh, kể cả với JWT hợp lệ của Admin — chặn ở tầng network trước khi tới tầng application). Bổ sung **[v4] phòng thủ cấp cơ sở dữ liệu**: user PostgreSQL của app tại mỗi Branch DB chỉ được `GRANT SELECT` trên các bảng master data (`product`, `category`, `price_list`, `customer`...), không có quyền ghi — phòng ngừa cả trường hợp có lỗi ở tầng ứng dụng trong tương lai. |
| Khả năng bảo trì | *(không đổi so với v3)* |
| Khả năng quan sát | *(không đổi so với v3)* |
| Tuân thủ nghiệp vụ kế toán kho VN | *(không đổi so với v3)* |
| Disaster Recovery | *(không đổi so với v3)* |

---

## PHẦN 3 — KIẾN TRÚC TỔNG THỂ

### 3.1 Nguyên tắc thiết kế **[v4 — bổ sung nguyên tắc phân tách Admin/Staff theo instance]**

*(Giữ nguyên nguyên tắc N+1 instance từ v3 — 1 HQ + N Branch, cùng 1 artifact backend, khác cấu hình.)*

**[v4] Bổ sung nguyên tắc bắt buộc mới — "Admin không có đường kết nối tới Branch instance":**

> Người dùng role `ADMIN` **chỉ được phép kết nối tới HQ instance**. Không tồn tại bất kỳ luồng nghiệp vụ, API, hay giao diện nào cho phép Admin gửi request trực tiếp tới 1 Branch instance cụ thể — kể cả khi mang JWT hợp lệ với quyền cao nhất. Đây là quyết định kiến trúc đã chốt, thay thế hoàn toàn phương án trước đó ("Admin chọn chi nhánh để truy cập") sau khi phân tích rủi ro: dữ liệu master (`product`, `category`, `price_list`, `customer`) tại Branch DB chỉ là bản sao chỉ-đọc nhận qua Logical Replication — PostgreSQL Logical Replication **không có cơ chế phát hiện xung đột** (conflict detection), nên bất kỳ thao tác ghi trực tiếp nào vào các bảng này tại Branch DB sẽ bị **âm thầm ghi đè** ở lần đồng bộ kế tiếp từ HQ mà không có cảnh báo — đây là lỗi khó phát hiện và khắc phục nhất trong toàn hệ thống nếu để xảy ra.
>
> **Đánh đổi đã xác nhận:** Admin không còn khả năng sửa trực tiếp dữ liệu cục bộ tại 1 chi nhánh cụ thể (VD: khắc phục sự cố nhập sai số lượng tồn kho) — việc này bắt buộc phải do đúng nhân viên (`STAFF`) tại chi nhánh đó thực hiện. Hệ thống **không có** cơ chế "hỗ trợ từ xa" nào khác ngoài ngoại lệ RFQ claim đã ghi nhận ở Phần 3.4 v3 — giữ nguyên tắc tối thiểu hoá số lượng ngoại lệ gọi đồng bộ giữa các instance để hệ thống dễ suy luận và audit.

### 3.2 Sơ đồ tổng thể **[v4 — cập nhật thể hiện 3 frontend app]**

```
                    ┌─────────────────────────────────────────┐
                    │      REVERSE PROXY HQ (public internet)   │
                    └──────────────────┬────────────────────────┘
                                        │
                    ┌───────────────────▼───────────────────────┐
                    │              HQ INSTANCE                    │
                    │  ┌──────────────┐  ┌───────────────────┐   │
                    │  │  Public Site  │  │  HQ Admin/Dashboard│   │
                    │  │  (frontend)   │  │  (frontend) [v4]   │   │
                    │  └──────────────┘  └───────────────────┘   │
                    │  ┌─────────────┐  ┌──────────────────────┐ │
                    │  │  Identity    │  │  Backend API          │ │
                    │  │  (JWT RS256, │  │  (public + admin)     │ │
                    │  │  private key)│  └──────────────────────┘ │
                    │  └─────────────┘                            │
                    │  Redis (cache catalog + revocation list)     │
                    └──────────────────┬────────────────────────┘
                                        │  Logical Replication
                                        │  (bất đồng bộ, 2 chiều riêng)
                     ┌──────────────────┼──────────────────────┐
         ┌───────────▼──────┐   ┌───────▼──────────┐   ┌────────▼─────────┐
         │  REVERSE PROXY     │   │  REVERSE PROXY     │   │  REVERSE PROXY     │
         │  Chi nhánh A        │   │  Chi nhánh B        │   │  Chi nhánh C        │
         │  [v4] CHỈ CHO PHÉP  │   │  [v4] CHỈ CHO PHÉP  │   │  [v4] CHỈ CHO PHÉP  │
         │  kết nối từ mạng    │   │  kết nối từ mạng    │   │  kết nối từ mạng    │
         │  nội bộ/VPN chi     │   │  nội bộ/VPN chi     │   │  nội bộ/VPN chi     │
         │  nhánh đó — KHÔNG   │   │  nhánh đó — KHÔNG   │   │  nhánh đó — KHÔNG   │
         │  public ra internet │   │  public ra internet │   │  public ra internet │
         └───────────┬──────┘   └───────┬──────────┘   └────────┬─────────┘
         ┌───────────▼──────┐   ┌───────▼──────────┐   ┌────────▼─────────┐
         │  Branch ERP (FE)   │   │  Branch ERP (FE)   │   │  Branch ERP (FE)   │
         │  [v4] đóng gói     │   │  [v4] đóng gói     │   │  [v4] đóng gói     │
         │  TĨNH cùng image   │   │  TĨNH cùng image   │   │  TĨNH cùng image   │
         │  + Backend API      │   │  + Backend API      │   │  + Backend API      │
         │  [KHÔNG Redis]      │   │  [KHÔNG Redis]      │   │  [KHÔNG Redis]      │
         └───────────┬──────┘   └───────┬──────────┘   └────────┬─────────┘
         ┌───────────▼──────┐   ┌───────▼──────────┐   ┌────────▼─────────┐
         │  PostgreSQL        │   │  PostgreSQL        │   │  PostgreSQL        │
         │  Chi nhánh A        │   │  Chi nhánh B        │   │  Chi nhánh C        │
         │  [v4] app_user chỉ  │   │  [v4] app_user chỉ  │   │  [v4] app_user chỉ  │
         │  SELECT trên bảng   │   │  SELECT trên bảng   │   │  SELECT trên bảng   │
         │  master data        │   │  master data        │   │  master data        │
         └───────────────────┘   └───────────────────┘   └───────────────────┘
```

**[v4] Điểm mấu chốt của sơ đồ:** không có mũi tên nào nối trực tiếp từ Admin/HQ Admin frontend xuống bất kỳ Branch instance nào — toàn bộ tương tác giữa HQ và Branch chỉ đi qua đúng 1 kênh duy nhất: **Logical Replication bất đồng bộ**.

### 3.3 Vì sao không cần API Gateway

*(Không đổi so với v3.)*

### 3.4 Xác thực & phân quyền — RBAC 2 role, luồng JWT RS256 **[v4 — viết lại theo quyết định mới]**

**Mô hình RBAC:**

| Role | Phạm vi quyền | Instance được phép truy cập | `branchId` trong JWT |
|---|---|---|---|
| `STAFF` | Toàn quyền trong phạm vi 1 chi nhánh cố định (bán hàng, kho, công nợ, đặt hàng, trả hàng) | **Chỉ đúng 1 Branch instance** của mình | Giá trị cụ thể (VD `2`) |
| `ADMIN` | Toàn quyền toàn hệ thống (tạo/sửa sản phẩm, quản lý chi nhánh, quản lý người dùng, xem dashboard tổng hợp) | **Chỉ HQ instance** | `null` |

**Luồng đăng nhập (đơn giản hơn phương án đã cân nhắc trước đó — không còn bước "chọn chi nhánh"):**

1. Người dùng (Staff hoặc Admin) gửi username/password tới `POST /auth/login` **tại HQ** (endpoint này chỉ tồn tại ở HQ, như đã thiết kế từ v3).
2. HQ xác thực, ký JWT (RS256), trả về theo đúng 1 trong 2 dạng response sau — **không có bước lựa chọn nào ở giữa**:

```json
// Đăng nhập bằng tài khoản STAFF
{
  "accessToken": "...",
  "refreshToken": "...",
  "role": "STAFF",
  "branchUrl": "https://branch-a.internal.yourstore.com"
}

// Đăng nhập bằng tài khoản ADMIN
{
  "accessToken": "...",
  "refreshToken": "...",
  "role": "ADMIN"
  // KHÔNG có branchUrl, KHÔNG có danh sách chi nhánh để chọn
}
```

3. Frontend xử lý theo `role` nhận được:
   - `STAFF` → tự động redirect trình duyệt sang `branchUrl` (URL của Branch ERP app tại đúng chi nhánh, đã cấu hình sẵn trong bảng `branch.internal_url`).
   - `ADMIN` → tự động redirect sang HQ Admin/Dashboard app (URL cố định, không cần tra cứu).

**[v4] Chặn ở tầng mạng, không chỉ ở tầng ứng dụng:** Reverse proxy tại mỗi Branch instance được cấu hình **chỉ chấp nhận kết nối từ mạng nội bộ/VPN của đúng chi nhánh đó** (VD: whitelist theo dải IP nội bộ, hoặc yêu cầu VPN client certificate). Điều này có nghĩa: dù giả sử (do lỗi cấu hình nào đó) Admin cố tình gọi thẳng API của Branch A từ mạng ngoài, request sẽ **bị chặn ngay tại reverse proxy**, không bao giờ tới được tầng ứng dụng Spring Boot để JWT filter kịp xử lý. Đây là lớp phòng thủ đầu tiên trong 3 lớp phòng thủ (xem tổng hợp ở mục 3.4.1).

**Thu hồi quyền truy cập:** *(không đổi so với v3 — denylist refresh token tại Redis HQ, đánh đổi revocation không tức thời tuyệt đối vẫn giữ nguyên.)*

#### 3.4.1 Tổng hợp 3 lớp phòng thủ bảo vệ dữ liệu master tại Branch **[v4 — mới]**

| Lớp | Cơ chế | Chặn được gì |
|---|---|---|
| **1. Tầng mạng** | Reverse proxy Branch chỉ nhận kết nối từ mạng nội bộ/VPN của đúng chi nhánh | Chặn mọi kết nối từ bên ngoài, kể cả khi có JWT Admin hợp lệ |
| **2. Tầng ứng dụng** | `ProductWriter`/`CustomerWriter` (ghi master data) chỉ đăng ký endpoint khi `@Profile("hq")`, không tồn tại tại Branch instance | Chặn nếu (giả định) lớp 1 bị vượt qua bằng cách nào đó (VPN nội bộ bị xâm nhập...) |
| **3. Tầng cơ sở dữ liệu** | User PostgreSQL của app tại Branch DB chỉ có `GRANT SELECT` trên bảng master data | Chặn ngay cả khi có bug ở tầng ứng dụng (VD lỡ bật nhầm `ProductWriter` ở profile branch trong 1 lần deploy sau này) |

**Nguyên tắc:** không tin tưởng tuyệt đối vào 1 lớp duy nhất — 3 lớp độc lập với nhau, lớp sau vẫn bảo vệ được dù lớp trước bị bỏ sót.

### 3.5 Vai trò của Redis

*(Không đổi so với v3 — chỉ tại HQ, không tại chi nhánh.)*

### 3.6 Kiến trúc frontend — 3 ứng dụng độc lập **[v4 — chính thức hoá]**

Xác nhận và chính thức hoá cấu trúc frontend (đã được gợi mở khi thảo luận về phân quyền), thay cho cấu trúc "2 route group `(public)`/`(admin)` trong 1 app Next.js" đã đề cập sơ lược ở v2/v3:

```
apps/
 ├── public-site/     → Next.js SSR/SSG, deploy tập trung (CDN), chỉ gọi HQ backend
 │                        (trang giới thiệu, danh mục sản phẩm, RFQ, visualizer)
 ├── hq-admin/          → Next.js CSR, deploy cùng vị trí HQ, chỉ gọi HQ backend
 │                        (CRUD sản phẩm/danh mục/giá, quản lý chi nhánh, quản lý
 │                        người dùng, Dashboard/Analytics, xử lý/claim RFQ)
 └── branch-erp/        → Next.js CSR, ĐÓNG GÓI TĨNH cùng Docker image của MỖI
                            Branch instance, phục vụ bởi đúng reverse proxy nội bộ
                            của chi nhánh đó — KHÔNG fetch từ HQ mỗi lần tải trang
                            (đảm bảo tính sẵn sàng khi mất kết nối tới HQ, đúng
                            nguyên tắc đã thống nhất từ v3)
                            (bán hàng/SalesInvoice, tồn kho, công nợ, đặt hàng
                            khách, trả hàng, nhập kho, chuyển kho)

packages/
 └── ui-shared/          → design system dùng chung giữa 3 app (component, style
                            tokens) — tránh trùng lặp code UI nhưng vẫn giữ 3 app
                            triển khai/deploy hoàn toàn độc lập
```

**Lý do tách 3 app thay vì 1 app với route group (quyết định đã chốt):**

1. **Tập người dùng và tập chức năng không giao nhau** — Staff không bao giờ cần màn hình Dashboard/CRUD sản phẩm, Admin không bao giờ cần màn hình tạo hoá đơn/nhập kho. Tách app giúp mỗi app chỉ chứa đúng chức năng cần thiết, không cần ẩn/hiện theo role bằng logic phức tạp.
2. **Ràng buộc triển khai khác nhau về bản chất** — `branch-erp` bắt buộc phải đóng gói tĩnh, chạy local tại từng chi nhánh (yêu cầu sẵn sàng khi mất mạng); `public-site` và `hq-admin` có thể lưu trữ tập trung bình thường. Đây không phải khác biệt về UI mà là khác biệt về **mô hình triển khai**, không thể giải quyết chỉ bằng route group trong 1 app.
3. **Giảm bề mặt tấn công (attack surface)** — tách vật lý đúng theo ranh giới quyền hạn, nhất quán với nguyên tắc "loại bỏ đường đi thay vì canh chặn" đã áp dụng ở tầng backend (mục 3.1).

---

## PHẦN 4 — THIẾT KẾ CHI TIẾT CƠ SỞ DỮ LIỆU PHÂN TÁN (POSTGRESQL)

### 4.1 – 4.5 Chiến lược Hub-and-Spoke, bảng dữ liệu, xử lý mất kết nối, Backup/DR

*(Không đổi so với v3.)*

### 4.6 Phân quyền cấp cơ sở dữ liệu tại Branch DB **[v4 — mới, Lớp phòng thủ 3]**

Bổ sung script cấp quyền (chạy trong migration khởi tạo mỗi Branch DB, tham số hoá để áp dụng nhất quán cho mọi chi nhánh — không viết tay riêng lẻ từng nơi):

```sql
-- Tại mỗi Branch DB, sau khi bảng đã được tạo qua Flyway/Liquibase migration
REVOKE ALL ON product, category, product_attribute_value, price_list, customer FROM app_user;
GRANT SELECT ON product, category, product_attribute_value, price_list, customer TO app_user;

-- Các bảng do chính Branch ghi (giao dịch cục bộ) vẫn có đầy đủ quyền DML
GRANT SELECT, INSERT, UPDATE, DELETE ON
  sales_invoice, sales_invoice_line, stock_on_hand, inbound_receipt, inbound_receipt_line,
  outbound_receipt, outbound_receipt_line, stock_transfer, stock_transfer_line,
  customer_order, customer_order_line, goods_return, goods_return_line,
  receivable_debt, customer_product_price, supplier, payable_debt,
  supplier_purchase_order, supplier_purchase_order_line, idempotency_record, audit_log
  TO app_user;
```

**Lưu ý vận hành:** user PostgreSQL thực hiện logical replication (subscriber role) là **user riêng biệt**, khác với `app_user` — user này cần đủ quyền ghi lên các bảng master data để nhận dữ liệu từ HQ (đây là kênh ghi hợp lệ duy nhất vào các bảng đó tại Branch DB). Không nhầm lẫn 2 user này khi cấu hình.

---

## PHẦN 5 — THIẾT KẾ CHI TIẾT TỪNG MODULE

### 5.1 Module 1 — Website giới thiệu (Public Site)

*(Không đổi so với v3 — chạy tại app `public-site`, gọi HQ backend.)*

### 5.2 Module 2 — ERP nội bộ

*(Toàn bộ thiết kế nghiệp vụ 5.2.1 → 5.2.10 — `SalesInvoice`, `CustomerProductPrice`, bán khống/âm kho, `GoodsReturn`, `CustomerOrder`, `SupplierPurchaseOrder`, Idempotency Key — **giữ nguyên không đổi** so với v3. Chạy tại app `branch-erp`, gọi Branch backend tương ứng.)*

#### 5.2.9 Phân quyền **[v4 — viết lại]**

RBAC 2 role (`STAFF`, `ADMIN`) như đã trình bày ở Phần 3.4 — thay thế hoàn toàn mô hình 5 role đã đề cập ở các phiên bản trước. `STAFF` thao tác toàn bộ nghiệp vụ Phần 5.2 tại đúng 1 Branch instance của mình (không có khái niệm phân quyền chi tiết hơn theo chức danh — VD không còn tách riêng "Thủ kho" chỉ được nhập/xuất kho mà không được tạo hoá đơn; nếu sau này cần phân quyền chi tiết hơn trong nội bộ 1 chi nhánh, đây là điểm mở rộng tự nhiên của bảng `role_permission` đã thiết kế sẵn, không cần đổi cấu trúc).

---

## PHẦN 6 — TECH STACK, TRIỂN KHAI & LỘ TRÌNH

### 6.1 Tech stack chi tiết **[v4 — bổ sung]**

*(Không đổi so với v3, bổ sung dòng sau:)*

| Thành phần | Công nghệ |
|---|---|
| Kiến trúc frontend **[v4]** | Monorepo (Turborepo/Nx hoặc npm/pnpm workspace đơn giản) — 3 app Next.js độc lập (`public-site`, `hq-admin`, `branch-erp`) + 1 package `ui-shared` |
| Reverse proxy Branch **[v4]** | Nginx với whitelist IP nội bộ/VPN — không public ra internet |

### 6.2 Chiến lược triển khai N-instance

*(Không đổi so với v3, bổ sung: mỗi lần build Docker image cho Branch instance, bước build cần bao gồm `branch-erp` frontend build tĩnh, đóng gói vào cùng image/container, không tách riêng bước deploy frontend.)*

### 6.3 Lộ trình triển khai

*(Không đổi về 5 giai đoạn — chi tiết Sprint cần cập nhật: Sprint 1.1 viết lại theo Phần 3.4 v4 (RBAC 2 role, không còn bước chọn chi nhánh), Sprint 5.1 bổ sung kiểm tra 3 lớp phòng thủ mục 3.4.1. Xem prompt cập nhật Sprint 1.1 riêng đi kèm tài liệu này.)*

---

*Tài liệu v4 chốt phương án phân quyền cuối cùng — thay thế hoàn toàn phần RBAC/luồng đăng nhập/kiến trúc frontend của v3. Toàn bộ thiết kế nghiệp vụ, cơ chế replication, idempotency không đổi.*
