# THIẾT KẾ KIẾN TRÚC HỆ THỐNG WEBSITE CỬA HÀNG VLXD & TTNT — v3

**Vai trò:** Software Architect
**Kiến trúc:** Modular Monolith, triển khai **1 instance riêng cho mỗi chi nhánh + 1 instance HQ**
**Stack:** Next.js (Frontend) — Spring Boot (Backend) — PostgreSQL Replication (Hub-and-Spoke) — JWT (RS256)

> **Ghi chú phiên bản v3:** Thay đổi kiến trúc lớn nhất so với v2 — chuyển từ **1 instance backend tập trung** sang **N instance độc lập (mỗi chi nhánh 1 instance + 1 instance HQ)**, xác thực bằng **JWT ký bất đối xứng (RS256)** thay vì Session-Redis, **không dùng Redis tại chi nhánh** (chỉ dùng tại HQ). Quyết định này xuất phát từ đặc điểm thực tế: các chi nhánh ở xa nhau về mặt địa lý, ưu tiên khả năng mở rộng (scalability) và khả năng hoạt động độc lập khi mất kết nối tới HQ. Toàn bộ thiết kế nghiệp vụ (Phần 5.2: hoá đơn, công nợ, giá theo khách hàng, đặt hàng, trả hàng...) từ tài liệu v2 **giữ nguyên không đổi** — v3 chỉ thay đổi tầng hạ tầng/triển khai/xác thực, có bổ sung 1 yêu cầu kỹ thuật mới (idempotency key) phát sinh từ kiến trúc N-instance. Các phần thay đổi được đánh dấu **[v3]**.

---

## PHẦN 1 — NGHIÊN CỨU & TỔNG HỢP ĐẶC ĐIỂM NGÀNH

*(Không đổi so với v2 — xem lại: đặc điểm SKU-heavy, đa chi nhánh, visualizer là điểm khác biệt cạnh tranh, ERP là hệ thống lõi vận hành.)*

---

## PHẦN 2 — TỔNG HỢP YÊU CẦU HỆ THỐNG

### 2.1 Yêu cầu chức năng (Functional Requirements)

*(Không đổi so với v2 — FR1 đến FR32, xem tài liệu v2 để biết đầy đủ. Toàn bộ nghiệp vụ bán hàng/kho/công nợ/đặt hàng không bị ảnh hưởng bởi thay đổi kiến trúc hạ tầng ở v3.)*

**[v3] Bổ sung 1 yêu cầu chức năng mới, phát sinh trực tiếp từ kiến trúc N-instance:**

- FR33 **[v3]**: Mọi API tạo giao dịch tài chính/kho (`SalesInvoice.confirm`, `GoodsReturn.confirm`, `InboundReceipt.confirm`, `StockTransfer`...) phải hỗ trợ **idempotency key** do client sinh sẵn — tránh tạo trùng giao dịch khi request bị gửi lại do mạng chập chờn giữa chi nhánh và các khu vực xa nhau (xem Phần 5.2.10).

### 2.2 Yêu cầu phi chức năng (Non-Functional Requirements) **[v3 — cập nhật nhóm Availability/Consistency]**

| Nhóm | Yêu cầu |
|---|---|
| Hiệu năng | Trang public < 2.5s (LCP), API thường < 300ms, báo cáo tổng hợp < 2s. **[v3]** Vì mỗi chi nhánh có DB + app riêng tại chỗ, latency API nghiệp vụ tại chi nhánh **không phụ thuộc** khoảng cách địa lý tới HQ — dễ đạt ngưỡng này hơn so với kiến trúc tập trung. |
| Khả năng mở rộng | **[v3]** Thêm 1 chi nhánh mới = thêm 1 instance mới, không ảnh hưởng chi nhánh đang chạy — scale theo chiều ngang tự nhiên theo đúng mục tiêu đã chốt. |
| Sẵn sàng cao | **[v3]** Mỗi chi nhánh hoạt động **hoàn toàn độc lập** — mất điện/mất mạng ở 1 chi nhánh không ảnh hưởng chi nhánh khác. Nhân viên đã đăng nhập (JWT còn hạn) tiếp tục thao tác được ngay cả khi HQ mất kết nối, chỉ riêng chức năng **đăng nhập mới** cần HQ online. |
| Nhất quán dữ liệu | Branch-local ACID cho từng giao dịch tại chi nhánh; eventual consistency khi đồng bộ lên HQ qua logical replication (bất đồng bộ, không chặn luồng nghiệp vụ tại chi nhánh). |
| Bảo mật | **[v3]** JWT RS256 — HQ giữ private key (nơi duy nhất phát hành token), mọi chi nhánh chỉ giữ public key (chỉ xác thực, không phát hành được). RBAC theo vai trò + chi nhánh (claim `branchId` trong JWT), audit log tại từng chi nhánh. |
| Khả năng bảo trì | Modular monolith — **[v3]** cùng 1 codebase/artifact deploy cho cả HQ và mọi chi nhánh, chỉ khác cấu hình (`application.yml` theo từng instance: vai trò HQ/Branch, `branchId`, connection string DB cục bộ). |
| Khả năng quan sát | Logging tập trung — **[v3]** log từ mọi chi nhánh gửi về hệ thống log tập trung tại HQ (bất đồng bộ, không chặn nghiệp vụ), tránh phải SSH vào từng chi nhánh để xem log khi có sự cố. |
| Tuân thủ nghiệp vụ kế toán kho VN | Sổ kho, số chứng từ liên tục theo từng chi nhánh, soft-delete + audit. |
| Disaster Recovery | **[v3]** Mỗi chi nhánh **tự có backup riêng** (không ỷ lại vào bản sao trên HQ vì đó là bản sao có độ trễ, không phải backup point-in-time thật sự — xem Phần 4.5). |

---

## PHẦN 3 — KIẾN TRÚC TỔNG THỂ **[v3 — THIẾT KẾ LẠI HOÀN TOÀN]**

### 3.1 Nguyên tắc thiết kế

Hệ thống gồm **N+1 instance giống hệt nhau về mã nguồn** (cùng 1 artifact Spring Boot Modular Monolith), khác nhau về **cấu hình vai trò**:

- **1 instance HQ:** phục vụ website public, admin dashboard/analytics tổng hợp, và là **Identity Provider duy nhất** (nơi duy nhất phát hành JWT).
- **N instance Chi nhánh:** mỗi chi nhánh 1 instance, phục vụ nghiệp vụ ERP tại chỗ (bán hàng, kho, công nợ...) cho đúng chi nhánh đó, kết nối với **database riêng của chi nhánh**.

Không có instance nào gọi trực tiếp instance khác theo kiểu đồng bộ (synchronous call) — toàn bộ giao tiếp giữa HQ và chi nhánh đi qua **logical replication bất đồng bộ** ở tầng dữ liệu (xem Phần 4), **trừ duy nhất luồng đăng nhập** (chi nhánh không tự phát hành token, phải xác thực người dùng thông qua HQ).

### 3.2 Sơ đồ tổng thể

```
                    ┌─────────────────────────────────────────┐
                    │      REVERSE PROXY (Nginx/Caddy)          │
                    │      TLS termination, public internet     │
                    └──────────────────┬────────────────────────┘
                                        │
                    ┌───────────────────▼───────────────────────┐
                    │              HQ INSTANCE                    │
                    │  ┌─────────────┐  ┌──────────────────────┐ │
                    │  │  Identity    │  │  Public Site API      │ │
                    │  │  (phát hành  │  │  Admin/Dashboard API  │ │
                    │  │  JWT, RS256  │  │  (đọc Read Replica)   │ │
                    │  │  private key)│  └──────────────────────┘ │
                    │  └─────────────┘                            │
                    │  ┌──────────────┐                           │
                    │  │  Redis (HQ)   │  ← CHỈ TẠI HQ            │
                    │  │  cache catalog│                          │
                    │  │  + revocation │                          │
                    │  │  list refresh │                          │
                    │  │  token        │                          │
                    │  └──────────────┘                           │
                    └──────────────────┬────────────────────────┘
                                        │
                     ┌──────────────────┼──────────────────────┐
                     │         Logical Replication (bất đồng bộ) │
                     │         HQ ↔ mỗi chi nhánh, 2 chiều riêng  │
         ┌───────────▼──────┐   ┌───────▼──────────┐   ┌────────▼─────────┐
         │  Reverse Proxy    │   │  Reverse Proxy    │   │  Reverse Proxy    │
         │  (nội bộ/VPN)     │   │  (nội bộ/VPN)     │   │  (nội bộ/VPN)     │
         └───────────┬──────┘   └───────┬──────────┘   └────────┬─────────┘
         ┌───────────▼──────┐   ┌───────▼──────────┐   ┌────────▼─────────┐
         │  CHI NHÁNH A       │   │  CHI NHÁNH B       │   │  CHI NHÁNH C       │
         │  App instance      │   │  App instance      │   │  App instance      │
         │  (verify JWT bằng  │   │  (verify JWT bằng  │   │  (verify JWT bằng  │
         │  public key, KHÔNG │   │  public key)       │   │  public key)       │
         │  gọi lại HQ)       │   │                    │   │                    │
         │  [KHÔNG có Redis]  │   │  [KHÔNG có Redis]  │   │  [KHÔNG có Redis]  │
         │  Cache: Caffeine   │   │  Cache: Caffeine   │   │  Cache: Caffeine   │
         │  (in-memory, nếu   │   │  (in-memory, nếu   │   │  (in-memory, nếu   │
         │  cần)              │   │  cần)              │   │  cần)              │
         └───────────┬──────┘   └───────┬──────────┘   └────────┬─────────┘
         ┌───────────▼──────┐   ┌───────▼──────────┐   ┌────────▼─────────┐
         │  PostgreSQL        │   │  PostgreSQL        │   │  PostgreSQL        │
         │  Chi nhánh A       │   │  Chi nhánh B       │   │  Chi nhánh C       │
         │  (+ backup riêng)  │   │  (+ backup riêng)  │   │  (+ backup riêng)  │
         └───────────────────┘   └───────────────────┘   └───────────────────┘
```

### 3.3 Vì sao KHÔNG cần API Gateway đúng nghĩa

Phân biệt rõ 2 khái niệm hay bị nhầm:

| | API Gateway | Reverse Proxy *(đã chọn)* |
|---|---|---|
| Vai trò | Điều phối routing động, tổng hợp nhiều service, business logic (rate-limit theo API key, transform request, service discovery) | TLS termination, forward request theo domain/path **cố định** |
| Khi nào thật sự cần | Nhiều microservice độc lập, cần 1 cửa ngõ chung điều phối | Vài instance cố định, biết trước địa chỉ, không có logic điều phối phức tạp |
| Hệ thống này | **Không cần** — mỗi chi nhánh là 1 monolith độc lập hoàn toàn, không có service nào cần "điều phối" giữa các chi nhánh với nhau | **Cần** — chỉ để có TLS + domain gọn, không mang logic nghiệp vụ |

Lý do sâu xa nhất: **JWT ký RS256 khiến việc xác thực hoàn toàn phân tán** — mỗi instance tự verify chữ ký bằng public key, không cần 1 điểm trung tâm "chặn" request để kiểm tra token trước khi forward (đây chính là điểm khác biệt so với kiến trúc Session-Redis tập trung đã cân nhắc và loại bỏ trước đó).

**Cấu hình cụ thể:**
- HQ: reverse proxy public ra internet (phục vụ website + admin dashboard + đăng nhập).
- Mỗi chi nhánh: reverse proxy chỉ mở trong mạng nội bộ/VPN của chi nhánh đó — nhân viên truy cập qua domain nội bộ cố định (VD `branch-a.internal.yourstore.com`), không public ra internet.
- Nếu số chi nhánh tăng lên rất nhiều (vài chục+) và cần rate-limit/observability tập trung, cân nhắc nâng cấp lên API Gateway thật — chưa cần ở quy mô hiện tại.

### 3.4 Xác thực & phân quyền — luồng JWT RS256 **[v3 — mới hoàn toàn]**

**Nguyên tắc cốt lõi:** chỉ HQ giữ **private key** để **ký (issue)** token — mọi chi nhánh chỉ giữ **public key** để **xác thực (verify)** chữ ký, không thể tự phát hành token mới. Đây là kiến trúc chuẩn cho hệ thống phân tán nhiều instance (khác với thiết kế Session-Redis đã cân nhắc và loại bỏ khi còn ở phương án 1-instance-tập-trung).

**Cấu trúc JWT:**

```json
{
  "sub": "12345",          // userId
  "roles": ["NHAN_VIEN_BAN_HANG"],
  "branchId": 2,             // NULL nếu là role toàn hệ thống (VD ADMIN_TONG)
  "iss": "hq-identity",
  "iat": 1755500000,
  "exp": 1755501800          // access token sống ngắn — 15-30 phút
}
```

**Luồng đăng nhập đầy đủ (xem sơ đồ đã minh hoạ ở phần trao đổi trước, tóm tắt lại đây làm tài liệu chuẩn):**

1. Nhân viên tại chi nhánh mở trình duyệt, gửi username/password **tới HQ Identity** (không gửi tới app tại chi nhánh mình).
2. HQ kiểm tra credential (bảng `user_account`, `user_branch_role` — dữ liệu master tại HQ), nếu hợp lệ, **ký** access token (RS256, private key) + refresh token, trả về client.
3. Từ đó, mọi request nghiệp vụ đi thẳng tới **app instance tại đúng chi nhánh** của nhân viên, kèm access token trong header `Authorization: Bearer ...`.
4. App tại chi nhánh **tự verify chữ ký** bằng public key đã cấu hình sẵn lúc deploy — hoàn toàn cục bộ, không gọi lại HQ cho bước này.
5. Access token hết hạn (15-30 phút) → client gọi API refresh — **luôn gửi về HQ** (không gửi tới chi nhánh), vì HQ là nơi duy nhất giữ danh sách refresh token còn hiệu lực/đã thu hồi (denylist tại Redis HQ).

**Thu hồi quyền truy cập (revocation):**
- Khoá tài khoản/nghỉ việc → HQ thêm refresh token vào denylist (Redis tại HQ) → nhân viên không refresh được nữa sau khi access token hiện tại hết hạn.
- Access token cũ (đã phát hành, chưa hết hạn tự nhiên) vẫn còn hiệu lực trong tối đa 15-30 phút — đây là đánh đổi cố hữu, chấp nhận được nhờ access token sống ngắn.
- Trường hợp cần thu hồi **tức thời** (VD phát hiện tài khoản bị xâm nhập) → cần thêm cơ chế kiểm tra bổ sung, cân nhắc thêm nếu có yêu cầu bảo mật cao hơn (xem mục "Vấn đề mở" cuối Phần 3.4).

**Vì sao chi nhánh vẫn hoạt động khi HQ mất kết nối:** vì bước xác thực (Bước 4) hoàn toàn cục bộ — chỉ riêng **đăng nhập mới** (Bước 1-2) và **refresh token** (Bước 5) cần HQ online. Nhân viên đã đăng nhập từ trước, access token còn hạn, tiếp tục thao tác bán hàng bình thường dù HQ mất kết nối hoàn toàn.

**Vấn đề mở cần bạn xác nhận:** nếu nghiệp vụ yêu cầu thu hồi quyền truy cập **tức thời tuyệt đối** (không chấp nhận độ trễ 15-30 phút của access token cũ), cần bổ sung 1 bước kiểm tra denylist ngay tại chi nhánh — nhưng điều này kéo theo chi nhánh phải có kết nối online tới HQ (hoặc 1 bản sao denylist được đồng bộ xuống) cho **mọi** request, phá vỡ chính lợi ích "hoạt động độc lập khi mất mạng" đang theo đuổi. Khuyến nghị: **giữ đánh đổi 15-30 phút** này, vì nó phù hợp với quy mô nhân sự rất ít của bạn (rủi ro thấp) — trừ khi có yêu cầu compliance cụ thể buộc phải thu hồi tức thời.

### 3.5 Vai trò của Redis — chỉ tại HQ, không tại chi nhánh **[v3]**

| | HQ | Chi nhánh |
|---|---|---|
| Redis | **Có, bắt buộc** | **Không có** |
| Lý do | Traffic public website cao hơn hẳn (đúng đặc điểm ngành đã khảo sát — SKU-heavy, nhiều lượt xem), cache catalog/giá thực sự cần thiết; đồng thời là nơi giữ denylist refresh token | Traffic mỗi chi nhánh rất thấp (vài người dùng đồng thời) — PostgreSQL cục bộ trả lời trực tiếp đã đủ nhanh, không cần thêm tầng cache; JWT không cần lưu trạng thái nên không có nhu cầu Redis cho session như thiết kế cũ |
| Cache thay thế tại chi nhánh (nếu thực sự cần) | — | Cache in-memory đơn giản (Caffeine, trong chính JVM của app instance) cho các query lặp lại nhiều — KHÔNG vận hành thêm 1 service Redis riêng cho mỗi chi nhánh, tránh tăng chi phí vận hành không tương xứng với quy mô |

### 3.6 Sơ đồ module Spring Boot (không đổi so với v2)

*(Không đổi — package structure `catalog/inventory/crm/procurement/order/branch/visualizer/analytics/identity/common/infrastructure` giữ nguyên, chỉ khác cấu hình theo instance HQ/Branch ở tầng `infrastructure` — VD `infrastructure/DataSourceConfig` trỏ tới DB cục bộ đúng instance đang chạy, `infrastructure/JwtConfig` load private key nếu là HQ hoặc chỉ public key nếu là Branch.)*

---

## PHẦN 4 — THIẾT KẾ CHI TIẾT CƠ SỞ DỮ LIỆU PHÂN TÁN (POSTGRESQL) **[v3 — cập nhật bối cảnh, giữ nguyên cơ chế]**

### 4.1 Chiến lược Hub-and-Spoke — vẫn giữ nguyên cơ chế, khác bối cảnh triển khai

Cơ chế **Logical Replication filtered theo branch** (đã thiết kế ở v1/v2) **không đổi về mặt kỹ thuật** — vẫn là:
- HQ giữ master data (`product`, `category`, `price_list`, `customer`...), replicate READ-ONLY xuống từng chi nhánh.
- Mỗi chi nhánh giữ dữ liệu giao dịch phát sinh tại chỗ (`sales_invoice`, `inbound_receipt`...), replicate lên HQ để tổng hợp.

**[v3] Điểm khác biệt duy nhất so với v2:** trước đây (v2) giả định 1 app instance tập trung, Branch DB chỉ là nơi lưu trữ dữ liệu (không có app riêng). **Nay mỗi Branch DB đi kèm 1 app instance riêng tại đúng vị trí đó** — App + DB **đồng vị trí (co-located)**, giao tiếp cục bộ (latency gần như 0), không phải qua mạng diện rộng như khi app tập trung phải gọi tới Branch DB ở xa. Đây chính là điểm cải thiện hiệu năng lớn nhất của quyết định N-instance.

### 4.2–4.4 Bảng dữ liệu chính, xử lý mất kết nối

*(Không đổi so với v2 — xem lại bảng đầy đủ tại Phần 4.3 tài liệu v2: `stock_on_hand`, `sales_invoice`, `customer_order`, `goods_return`, `supplier_purchase_order`... branch-local; `product`, `category`, `customer`... HQ master.)*

### 4.5 Backup & Disaster Recovery — làm rõ ranh giới với Replication **[v3 — bổ sung quan trọng]**

**Cảnh báo kiến trúc quan trọng:** Logical Replication lên HQ **không phải** là backup, dù trực giác dễ nhầm ("dữ liệu đã có bản sao ở HQ rồi, chắc không cần backup riêng nữa"). Lý do:

1. Replication chỉ đồng bộ dữ liệu **đã commit và đã kịp gửi** — nếu ổ đĩa tại chi nhánh hỏng vĩnh viễn đúng lúc có giao dịch **chưa kịp đồng bộ**, phần đó mất thật, HQ không có để khôi phục.
2. Replication không có khái niệm "point-in-time" — không thể dùng nó để khôi phục về đúng trạng thái tại 1 thời điểm quá khứ cụ thể (VD "khôi phục về 10h sáng hôm qua trước khi có thao tác nhầm").

**Yêu cầu bắt buộc:** mỗi chi nhánh **tự có cơ chế backup riêng** (`pg_basebackup` + WAL archiving định kỳ lên cloud storage, VD S3-compatible) — độc lập hoàn toàn với cơ chế replication lên HQ. Đây là 2 cơ chế phục vụ 2 mục đích khác nhau, không thay thế cho nhau:

| | Logical Replication → HQ | Backup riêng tại chi nhánh |
|---|---|---|
| Mục đích | Tổng hợp báo cáo, dashboard toàn hệ thống | Disaster Recovery, khôi phục point-in-time |
| Bảo vệ khỏi | Không bảo vệ khỏi mất dữ liệu tại chi nhánh | Bảo vệ khỏi hỏng ổ đĩa, thao tác nhầm, xoá dữ liệu sai |
| Có tính phí không | Có (băng thông, hạ tầng HQ) | Có (lưu trữ backup), nhưng bắt buộc, không thể bỏ qua |

---

## PHẦN 5 — THIẾT KẾ CHI TIẾT TỪNG MODULE

### 5.1 Module 1 — Website giới thiệu (Public Site)

*(Không đổi so với v2 — chạy tại HQ instance, có Redis cache như thiết kế v2/v3 Phần 3.5.)*

### 5.2 Module 2 — ERP nội bộ

*(Toàn bộ thiết kế nghiệp vụ 5.2.1 → 5.2.9 từ tài liệu v2 — `SalesInvoice`, `CustomerProductPrice`, bán khống/âm kho, `GoodsReturn`, `CustomerOrder`, `SupplierPurchaseOrder` — **giữ nguyên không đổi**, vì đây là logic nghiệp vụ độc lập với tầng hạ tầng/xác thực. Xem tài liệu v2 để có đầy đủ chi tiết domain method, entity, quy tắc.)*

#### 5.2.10 Idempotency Key cho các API giao dịch tài chính/kho **[v3 — bổ sung mới]**

**Vấn đề phát sinh từ kiến trúc N-instance:** khi chi nhánh ở xa HQ về mặt địa lý, khả năng mạng chập chờn **giữa nhân viên và app instance tại chi nhánh** (không phải giữa chi nhánh và HQ — đường này không nằm trên luồng nghiệp vụ) tuy thấp hơn nhưng vẫn có thể xảy ra, đặc biệt nếu nhân viên thao tác qua kết nối không ổn định (wifi yếu, 4G...). Nếu response bị rớt trên đường về sau khi server đã xử lý xong, nhân viên có thể bấm gửi lại → rủi ro **tạo trùng hoá đơn/phiếu nhập**.

**Giải pháp:**

```
Client sinh sẵn 1 idempotency_key (UUID) TRƯỚC khi gửi request tạo giao dịch,
gửi kèm trong header (VD: Idempotency-Key: <uuid>).

Server (app instance tại chi nhánh):
  1. Kiểm tra idempotency_key đã tồn tại trong bảng idempotency_record chưa
     (bảng mới, branch-local, key = idempotency_key, lưu kèm request_hash +
     response đã trả về lần đầu).
  2. NẾU ĐÃ CÓ: trả lại đúng response đã lưu từ lần xử lý đầu tiên, KHÔNG xử lý
     lại nghiệp vụ (tránh tạo trùng).
  3. NẾU CHƯA CÓ: xử lý nghiệp vụ bình thường (VD SalesInvoice.confirm()),
     sau khi xong, lưu lại idempotency_key + response vào bảng, TRONG CÙNG
     transaction với chính giao dịch nghiệp vụ đó.
```

**Áp dụng bắt buộc cho:** `SalesInvoice.confirm()`, `GoodsReturn.confirm()`,
`InboundReceipt.confirm()`, `StockTransfer` (bước tạo), `CustomerOrder` (bước tạo).

**Test case bổ sung vào Test Plan (Data Integrity):** gửi 2 lần **cùng 1**
`idempotency_key` cho cùng 1 request tạo hoá đơn (mô phỏng client gửi lại do
tưởng lỗi mạng) → xác nhận chỉ **1** hoá đơn được tạo, lần gọi thứ 2 trả về đúng
response của lần đầu, không trừ kho/công nợ lần thứ 2.

### 5.3 Module 3 — Demo/Visualizer sản phẩm gạch

*(Không đổi so với v2 — chạy tại HQ instance.)*

### 5.4 Module 4 — Đa chi nhánh & CSDL phân tán

*(Đã cập nhật đầy đủ ở Phần 4 phía trên.)*

### 5.5 Module 5 — Dashboard tổng hợp chỉ số kinh doanh (Analytics)

*(Không đổi so với v2 — chạy tại HQ instance, đọc Read Replica riêng.)*

---

## PHẦN 6 — TECH STACK, TRIỂN KHAI & LỘ TRÌNH **[v3 — cập nhật phần triển khai]**

### 6.1 Tech stack chi tiết

*(Không đổi so với v2, bổ sung/điều chỉnh các dòng sau:)*

| Thành phần | Công nghệ |
|---|---|
| Xác thực **[v3]** | JWT RS256 (thư viện `jjwt` hoặc Spring Security OAuth2 Resource Server), Redis (chỉ tại HQ) cho refresh token denylist |
| Cache **[v3]** | Redis (chỉ HQ, cache catalog public); Caffeine in-memory (tại mỗi chi nhánh, tuỳ chọn) |
| Reverse Proxy **[v3]** | Nginx hoặc Caddy — 1 instance mỗi vị trí (HQ public, mỗi chi nhánh nội bộ/VPN) |
| Backup **[v3]** | `pg_basebackup` + WAL archiving lên S3-compatible storage, cấu hình **riêng tại mỗi chi nhánh** |

### 6.2 Chiến lược triển khai N-instance **[v3 — mới]**

- **Cùng 1 artifact** (Docker image) cho cả HQ và mọi chi nhánh — khác nhau qua biến môi trường: `INSTANCE_ROLE=HQ|BRANCH`, `BRANCH_ID`, connection string DB cục bộ, đường dẫn public/private key JWT.
- Không cần Kubernetes/orchestration phức tạp ở quy mô hiện tại (ít chi nhánh, ít người dùng) — **Docker Compose đơn giản tại mỗi vị trí** (app + Postgres + reverse proxy) là đủ, giảm chi phí vận hành.
- Quy trình rollout: cập nhật artifact mới → deploy tuần tự từng chi nhánh (không cần đồng thời, vì các chi nhánh độc lập hoàn toàn) → giảm rủi ro so với hệ thống buộc phải đồng bộ version.
- Nếu số chi nhánh tăng mạnh trong tương lai, cân nhắc chuyển sang orchestration tự động (Ansible/Terraform để tạo instance mới nhanh, hoặc Kubernetes nếu vượt quá vài chục chi nhánh) — chưa cần ở giai đoạn hiện tại.

### 6.3 Lộ trình triển khai

*(Không đổi về bản chất 5 giai đoạn đã thiết kế ở Kế hoạch triển khai v2 — chi tiết Sprint cần cập nhật thêm: Sprint 1.1 (Identity) đổi từ Session-Redis sang JWT RS256 + phân biệt cấu hình HQ/Branch; Sprint 5.2 (Load test/DR) bổ sung test backup/restore riêng từng chi nhánh và test idempotency key. Đề xuất cập nhật đầy đủ Kế hoạch triển khai + Bộ prompt Sprint sang bản v3 tương ứng nếu cần, để đồng bộ với thay đổi kiến trúc này.)*

---

*Tài liệu v3 là bản kiến trúc hạ tầng/xác thực cập nhật, thay thế hoàn toàn phần "Kiến trúc tổng thể" và "CSDL phân tán" của v2. Toàn bộ thiết kế nghiệp vụ (Phần 5.2) không đổi.*
