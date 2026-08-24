# THIẾT KẾ KIẾN TRÚC HỆ THỐNG WEBSITE CỬA HÀNG VLXD & TTNT

**Vai trò:** Software Architect
**Kiến trúc:** Modular Monolith
**Stack:** Next.js (Frontend) — Spring Boot (Backend) — PostgreSQL Replication (Database phân tán)

---

## PHẦN 1 — NGHIÊN CỨU & TỔNG HỢP ĐẶC ĐIỂM NGÀNH

### 1.1 Quan sát các website VLXD & nội thất trong và ngoài nước

Từ khảo sát các website/theme chuyên ngành VLXD tại Việt Nam (VD: theme VLXD của Sapo/Bizweb, các đơn vị thiết kế web VLXD như I-WEB, TLPtech, Jamina...) và các website quốc tế lớn cùng lĩnh vực (Houzz, Home Depot, Lowe's), có thể rút ra các đặc trưng chung sau:

| Đặc điểm | Ghi chú |
|---|---|
| **Danh mục sản phẩm rất lớn, nhiều thuộc tính (SKU-heavy)** | Gạch, sơn, thiết bị vệ sinh... mỗi loại có kích thước, màu sắc, xuất xứ, đơn vị tính (m², viên, thùng) khác nhau → cần mô hình sản phẩm linh hoạt (EAV/JSONB attributes). |
| **Tìm kiếm & lọc nâng cao** | Lọc theo khu vực/chi nhánh, hãng, giá, kích thước, màu sắc. Tìm kiếm nhanh (từ khóa) + tìm kiếm nâng cao (bộ lọc). |
| **Tính năng mô phỏng/demo trực quan (visualizer)** | Houzz và các site quốc tế có tính năng "ướm" vật liệu (gạch, sàn, sơn) lên không gian mẫu — đây chính là yêu cầu module 3 của bạn, là điểm khác biệt cạnh tranh rõ rệt. |
| **Trang giới thiệu doanh nghiệp mạnh (trust-building)** | Vì giá trị đơn hàng VLXD thường lớn, khách hàng ưu tiên thương hiệu uy tín → cần trang giới thiệu, dự án đã triển khai, chứng nhận đại lý chính hãng, đánh giá khách hàng. |
| **Đa chi nhánh / đại lý phân phối** | Nhiều cửa hàng VLXD vận hành theo chuỗi (showroom + kho), cần hiển thị tồn kho theo từng chi nhánh, định giá có thể khác nhau theo khu vực. |
| **Không phải thương mại điện tử "chốt đơn online" thuần túy** | Đa số đóng vai trò lead-generation + tư vấn báo giá (đặc biệt đơn hàng số lượng lớn, công trình) hơn là giỏ hàng thanh toán tức thời — nhưng vẫn cần giỏ hàng/yêu cầu báo giá (RFQ). |
| **Backend/ERP nội bộ tách biệt** | Do dữ liệu lớn, các đơn vị chuyên nghiệp khuyến nghị xây dựng hệ thống quản lý dữ liệu riêng (không dùng mã nguồn mở đóng gói sẵn) để dễ mở rộng nghiệp vụ nhập-xuất-tồn, công nợ, nhiều chi nhánh. |
| **Chuẩn SEO & tốc độ tải trang** | Vì cạnh tranh từ khóa tìm kiếm địa phương ("VLXD + khu vực") rất khốc liệt → cần SSR/SSG, structured data, tối ưu ảnh sản phẩm. |

### 1.2 Kết luận rút ra cho thiết kế hệ thống

1. Cần **mô hình dữ liệu sản phẩm linh hoạt** (danh mục cha/con + thuộc tính động).
2. Cần **kiến trúc đa chi nhánh (multi-branch)** ngay từ đầu: tồn kho, giá bán, đơn hàng đều phải gắn với chi nhánh.
3. Module **visualizer gạch** là tính năng khác biệt hoá — cần tách riêng thành module độc lập, có thể dùng canvas/WebGL xử lý phía client để giảm tải server.
4. ERP không thể là "phần phụ" — đây là hệ thống lõi vận hành (nhập/xuất/tồn/công nợ), phải thiết kế chuẩn nghiệp vụ kế toán kho (không phải chỉ CRUD đơn giản).
5. Vì đa chi nhánh + cần báo cáo tổng hợp toàn hệ thống, bài toán "phân tán dữ liệu nhưng vẫn tổng hợp được" là trọng tâm kỹ thuật khó nhất → cần chiến lược PostgreSQL Replication rõ ràng (trình bày ở Phần 4).

---

## PHẦN 2 — TỔNG HỢP YÊU CẦU HỆ THỐNG

### 2.1 Yêu cầu chức năng (Functional Requirements)

**A. Website giới thiệu (Public site)**
- FR1: Trang chủ giới thiệu cửa hàng, banner khuyến mãi, sản phẩm nổi bật.
- FR2: Danh mục sản phẩm phân cấp (nhóm > loại > sản phẩm), trang chi tiết sản phẩm với thuộc tính kỹ thuật, hình ảnh, giá tham khảo theo chi nhánh.
- FR3: Tìm kiếm nhanh + tìm kiếm nâng cao (lọc theo hãng, giá, kích thước, khu vực còn hàng).
- FR4: Trang giới thiệu công ty, hệ thống chi nhánh (bản đồ, giờ mở cửa), tin tức/dự án đã thực hiện.
- FR5: Giỏ hàng / Yêu cầu báo giá (RFQ) — gửi yêu cầu tư vấn, không bắt buộc thanh toán online ngay.
- FR6: Form liên hệ, chat/hotline, tích hợp Zalo/Facebook (phổ biến tại VN).
- FR7: Tối ưu SEO (SSR/SSG, sitemap, schema.org Product).

**B. Trang ERP nội bộ**
- FR8: Quản lý khách hàng (CRM cơ bản): thông tin, lịch sử mua hàng, công nợ, phân loại khách lẻ/khách công trình.
- FR9: Quản lý danh mục sản phẩm: CRUD sản phẩm, thuộc tính động, đơn vị tính, quy đổi đơn vị (thùng ↔ m², viên ↔ m²), giá bán theo chi nhánh/thời điểm.
- FR10: Quản lý nhập hàng: phiếu nhập kho, nhà cung cấp, công nợ phải trả.
- FR11: Quản lý xuất hàng: phiếu xuất bán, xuất chuyển kho giữa chi nhánh, xuất huỷ/hao hụt.
- FR12: Quản lý tồn kho theo từng chi nhánh + tồn kho tổng hợp toàn hệ thống.
- FR13: Quản lý đơn hàng (từ web hoặc tại quầy), quản lý công nợ phải thu.
- FR14: Quản lý người dùng nội bộ, phân quyền theo vai trò (RBAC) và theo chi nhánh (nhân viên chi nhánh A không thấy dữ liệu chi nhánh B, trừ quản trị tổng).
- FR15: Quản lý chi nhánh (branch master data).

**C. Module Demo/Visualizer sản phẩm gạch**
- FR16: Cho phép chọn 1 trong các "phòng mẫu" (ảnh nền chuẩn hoá: phòng khách, phòng tắm, sân vườn...).
- FR17: Cho phép chọn sản phẩm gạch (từ danh mục thực tế) để "lót" thử lên khu vực sàn/tường trong ảnh phòng mẫu.
- FR18: Xử lý phối cảnh (perspective warp) để hoạ tiết gạch bám đúng góc nhìn phòng mẫu.
- FR19: Cho phép lưu/tải ảnh kết quả, chia sẻ hoặc gửi kèm yêu cầu báo giá.
- FR20: Quản trị viên có thể thêm/xoá "phòng mẫu" và định nghĩa vùng lót (mask polygon) qua công cụ nội bộ.

**D. Kiến trúc đa chi nhánh & CSDL phân tán**
- FR21: Mỗi chi nhánh có thể vận hành độc lập (nhập/xuất/bán hàng) ngay cả khi mất kết nối tạm thời tới trung tâm (yêu cầu gần-đồng bộ, không bắt buộc real-time tuyệt đối).
- FR22: Dữ liệu tổng hợp toàn hệ thống (đa chi nhánh) phải nhất quán và có khả năng truy vấn báo cáo nhanh.

**E. Module Dashboard/Data Analytics**
- FR23: Dashboard doanh thu, lợi nhuận gộp theo thời gian/chi nhánh/nhóm sản phẩm.
- FR24: Phân tích tồn kho, sản phẩm bán chạy/chậm, cảnh báo tồn kho thấp.
- FR25: Phân tích công nợ (phải thu/phải trả) theo hạn.
- FR26: So sánh hiệu suất giữa các chi nhánh.
- FR27: Xuất báo cáo (Excel/PDF), lịch báo cáo định kỳ (email tự động).

### 2.2 Yêu cầu phi chức năng (Non-Functional Requirements) — bắt buộc

| Nhóm | Yêu cầu |
|---|---|
| **Hiệu năng** | Trang public load < 2.5s (LCP), API ERP phản hồi < 300ms cho truy vấn thường, < 2s cho báo cáo tổng hợp phức tạp. |
| **Khả năng mở rộng (Scalability)** | Kiến trúc module hoá cho phép tách module thành service riêng trong tương lai (từ modular monolith → có thể "strangler" dần sang microservices nếu cần). |
| **Sẵn sàng cao (Availability)** | ERP tại từng chi nhánh không phụ thuộc hoàn toàn vào kết nối trung tâm; PostgreSQL Replication đảm bảo failover cho khu vực trung tâm. |
| **Nhất quán dữ liệu (Consistency)** | Dữ liệu master (sản phẩm, giá, khách hàng) cần nhất quán cuối cùng (eventual consistency) giữa các chi nhánh trong thời gian ngắn (giây–vài phút); dữ liệu giao dịch tài chính (đơn hàng, phiếu kho) cần đảm bảo ACID tại nơi phát sinh. |
| **Bảo mật** | Xác thực JWT/OAuth2, RBAC theo vai trò + chi nhánh, mã hoá dữ liệu nhạy cảm (mật khẩu, thông tin thanh toán), audit log cho các thao tác kho/tài chính. |
| **Khả năng bảo trì** | Modular monolith: mỗi module (catalog, inventory, crm, visualizer, analytics...) đóng gói rõ ràng, giao tiếp nội bộ qua interface/service layer, hạn chế phụ thuộc chéo. |
| **Khả năng quan sát (Observability)** | Logging tập trung, metrics (Prometheus/Grafana), tracing cho các luồng nghiệp vụ liên chi nhánh. |
| **Tuân thủ nghiệp vụ kế toán kho VN** | Nhập trước xuất trước hoặc bình quân gia quyền (tuỳ chọn cấu hình), có sổ kho, phiếu có số chứng từ liên tục, không sửa xoá cứng (soft-delete + audit). |
| **Đa thiết bị** | Responsive, ERP tối ưu cho desktop/tablet tại quầy; web public tối ưu mobile-first. |
| **Khả năng phục hồi (Disaster Recovery)** | Backup định kỳ + point-in-time recovery cho PostgreSQL; RPO/RTO xác định rõ theo từng loại dữ liệu. |

---

## PHẦN 3 — KIẾN TRÚC TỔNG THỂ (MODULAR MONOLITH)

### 3.1 Nguyên tắc thiết kế

Modular Monolith nghĩa là: **một ứng dụng backend triển khai (deploy) duy nhất**, nhưng được chia thành các **module nghiệp vụ độc lập về mã nguồn**, mỗi module có:
- Package Java riêng biệt, không truy cập trực tiếp Repository/Entity của module khác.
- Giao tiếp giữa các module thông qua **Application Service Interface** (nội bộ, trong cùng JVM — không qua HTTP).
- Schema PostgreSQL riêng theo từng module (dùng `schema` trong cùng 1 database hoặc database logic riêng), giúp sau này dễ tách thành microservice nếu cần scale.

```
┌──────────────────────────────────────────────────────────────────┐
│                         NEXT.JS FRONTEND                          │
│  ┌───────────────┐  ┌───────────────┐  ┌─────────────────────┐   │
│  │  Public Site   │  │   ERP Admin   │  │  Visualizer (Canvas) │  │
│  │  (SSR/SSG)     │  │   (SPA/CSR)   │  │  + Dashboard (CSR)   │  │
│  └───────┬────────┘  └───────┬───────┘  └──────────┬───────────┘  │
└──────────┼───────────────────┼─────────────────────┼──────────────┘
           │                REST/GraphQL API (HTTPS, JWT)
┌──────────┼───────────────────┼─────────────────────┼──────────────┐
│                     SPRING BOOT MONOLITH (1 deployable)            │
│  ┌────────────────────────── API Gateway Layer ─────────────────┐ │
│  │        (Spring Security, RBAC theo Role + Branch, Rate limit) │ │
│  └───────────────────────────────┬────────────────────────────── ┘ │
│         ┌───────────────┬────────┼────────┬───────────────┐       │
│    ┌────▼────┐    ┌─────▼───┐ ┌──▼───┐ ┌───▼────┐   ┌──────▼────┐ │
│    │ Catalog │    │Inventory│ │ CRM  │ │ Order  │   │ Visualizer │ │
│    │ Module  │    │ Module  │ │Module│ │ Module │   │  Module    │ │
│    └────┬────┘    └────┬────┘ └──┬───┘ └───┬────┘   └──────┬─────┘ │
│         │  ┌───────────┴──┐      │         │   ┌───────────┘       │
│    ┌────▼──▼──┐    ┌──────▼──────▼──┐  ┌───▼───▼───┐               │
│    │  Branch  │    │  Analytics /   │  │  Identity  │              │
│    │  Module  │    │  Dashboard Mod │  │  & Access  │              │
│    └────┬─────┘    └───────┬────────┘  └─────┬──────┘              │
│         └──────────────────┴──────────────────┘                    │
│              Event Bus nội bộ (Spring Events / Outbox)             │
└──────────────────────────────────┬──────────────────────────────── ┘
                                    │
┌───────────────────────────────────▼───────────────────────────────┐
│                  POSTGRESQL — KIẾN TRÚC PHÂN TÁN                    │
│                                                                      │
│   ┌───────────────────┐         ┌────────────────────────────┐     │
│   │  PRIMARY (HQ)      │──WAL──▶│ Read Replica (Reporting/    │     │
│   │  - master data      │        │  Analytics, read-only)     │     │
│   │  - transaction hub  │        └────────────────────────────┘     │
│   └─────────┬──────────┘                                            │
│             │ Logical Replication (per-branch subscription)         │
│    ┌────────┼────────────────┬───────────────────┐                  │
│ ┌──▼───┐ ┌──▼───┐        ┌───▼──┐            ┌────▼───┐             │
│ │Branch│ │Branch│  ...   │Branch│            │ Branch │             │
│ │ DB 1 │ │ DB 2 │        │ DB N │            │ DB N+1 │             │
│ │(local│ │(local│        │(local│            │(local  │             │
│ │write)│ │write)│        │write)│            │ write) │             │
│ └──────┘ └──────┘        └──────┘            └────────┘             │
└──────────────────────────────────────────────────────────────────── ┘
```

### 3.2 Sơ đồ module Spring Boot (chi tiết package)

```
com.storename.erp
 ├── catalog/          → sản phẩm, danh mục, thuộc tính, giá theo chi nhánh
 ├── inventory/         → nhập kho, xuất kho, tồn kho, chuyển kho liên chi nhánh
 ├── crm/                → khách hàng, công nợ phải thu
 ├── procurement/        → nhà cung cấp, công nợ phải trả
 ├── order/              → đơn hàng web + tại quầy, RFQ (yêu cầu báo giá)
 ├── branch/             → chi nhánh, cấu hình theo chi nhánh
 ├── visualizer/         → phòng mẫu, vùng lót (mask), lịch sử demo
 ├── analytics/          → data mart nội bộ, dashboard API, export báo cáo
 ├── identity/           → user, role, permission, JWT, RBAC theo branch
 ├── common/             → shared kernel: audit, exception, DTO base, utils
 └── infrastructure/     → config DB routing, replication, event bus, cache
```

**Quy tắc bắt buộc:** module A chỉ được gọi module B qua interface `xxxFacade`/`xxxService` được expose công khai (package `xxx.api`), **không bao giờ** import Entity hoặc Repository nội bộ của module khác. Điều này giữ cho hệ thống có thể "bóc tách" thành microservice sau này mà không phải viết lại logic nghiệp vụ.

---

## PHẦN 4 — THIẾT KẾ CHI TIẾT CƠ SỞ DỮ LIỆU PHÂN TÁN (POSTGRESQL)

### 4.1 Chiến lược: Hub-and-Spoke với Logical Replication

Vì cửa hàng có nhiều chi nhánh, và yêu cầu (FR21) là mỗi chi nhánh vẫn hoạt động được khi mạng trung tâm gián đoạn, đề xuất mô hình **Hub-and-Spoke**:

- **HQ Primary DB** (trụ sở/trung tâm dữ liệu): giữ **master data** dùng chung — danh mục sản phẩm, thuộc tính, bảng giá chuẩn, thông tin khách hàng toàn hệ thống, cấu hình chi nhánh.
- **Branch DB** (mỗi chi nhánh 1 instance PostgreSQL nhỏ, có thể chạy tại chỗ hoặc trên cloud khu vực gần chi nhánh): giữ **dữ liệu giao dịch phát sinh tại chi nhánh** — phiếu nhập/xuất, đơn hàng tại quầy, tồn kho cục bộ.
- **Logical Replication** (`pglogical`/native PostgreSQL logical replication từ PG10+):
  - HQ → Branch: đẩy một chiều dữ liệu master (sản phẩm, giá, khách hàng) — branch chỉ **đọc**.
  - Branch → HQ: đẩy một chiều dữ liệu giao dịch (đơn hàng, phiếu kho) về HQ để tổng hợp — HQ **không sửa** dữ liệu giao dịch của chi nhánh, chỉ tổng hợp/báo cáo.
- **Read Replica cho Analytics**: 1 replica riêng (streaming replication vật lý) chỉ phục vụ module Dashboard/Analytics, tách khỏi tải OLTP để không ảnh hưởng tốc độ bán hàng.

### 4.2 Vì sao không chọn sharding hay multi-master ngay từ đầu

- Multi-master (BDR) phức tạp trong xử lý xung đột (conflict resolution) — không cần thiết ở quy mô chuỗi cửa hàng vừa/nhỏ.
- Sharding theo branch_id phù hợp khi số chi nhánh rất lớn (hàng trăm) — ở giai đoạn đầu, hub-and-spoke với logical replication đã đáp ứng đủ yêu cầu "phân tán nhưng tổng hợp được", đơn giản để vận hành và rẻ hơn.
- Nếu tương lai mở rộng mạnh, có thể nâng cấp: mỗi vùng miền (Bắc/Trung/Nam) có 1 "Regional Hub", tạo phân tán 2 tầng.

### 4.3 Bảng dữ liệu chính (rút gọn) và nơi lưu

| Bảng | Nơi lưu chính | Ghi chú |
|---|---|---|
| `product`, `category`, `attribute`, `price_list` | HQ (master), replicate READ-ONLY xuống branch | 1 nguồn sự thật (single source of truth) |
| `customer` | HQ (master), replicate xuống branch | Khách hàng có thể mua ở nhiều chi nhánh |
| `branch`, `warehouse` | HQ | Danh mục chi nhánh/kho |
| `stock_on_hand` (tồn kho) | Branch local, tổng hợp về HQ qua materialized view/ETL | Ghi tại chỗ để đảm bảo tốc độ bán hàng |
| `inbound_receipt` (phiếu nhập) | Branch local → replicate về HQ | |
| `outbound_receipt` (phiếu xuất) | Branch local → replicate về HQ | |
| `sales_order` | Branch local (đơn tại quầy) hoặc HQ (đơn từ web) → hợp nhất về HQ | |
| `visualizer_room`, `visualizer_session` | HQ (dữ liệu dùng chung cho web public) | |
| `user_account`, `role`, `permission` | HQ | Đăng nhập tập trung |
| Data mart cho Dashboard | Read Replica riêng | ETL định kỳ (5–15 phút) từ dữ liệu giao dịch |

### 4.4 Xử lý khi mất kết nối chi nhánh ↔ trung tâm

- Ứng dụng ERP tại chi nhánh kết nối `branch DB` local trước tiên (mọi thao tác bán hàng/nhập-xuất vẫn chạy được).
- Logical replication tự động catch-up khi kết nối phục hồi (WAL được giữ lại tại publisher).
- Cảnh báo (alert) nếu độ trễ đồng bộ (replication lag) vượt ngưỡng cấu hình (VD: > 15 phút) để admin can thiệp.

---

## PHẦN 5 — THIẾT KẾ CHI TIẾT TỪNG MODULE

### 5.1 Module 1 — Website giới thiệu (Public Site)

- **Frontend:** Next.js App Router, SSG cho trang tĩnh (giới thiệu, chi nhánh), ISR (Incremental Static Regeneration) cho danh sách/chi tiết sản phẩm để vừa nhanh vừa cập nhật giá gần thời gian thực.
- **Backend API:** Catalog module cung cấp REST endpoint public (`/api/public/products`, `/api/public/categories`) — có cache tầng Redis (TTL ngắn) để giảm tải PostgreSQL khi lượng truy cập lớn.
- **SEO:** metadata động theo sản phẩm, sitemap.xml tự sinh, JSON-LD schema Product.
- **RFQ (yêu cầu báo giá)/giỏ hàng:** lưu tạm ở client (localStorage), khi gửi thì tạo bản ghi `sales_order` trạng thái `DRAFT/QUOTE_REQUEST` trong Order module.

### 5.2 Module 2 — ERP nội bộ

- **Frontend:** Next.js (CSR, dashboard layout riêng `/admin`), dùng React Query để cache và đồng bộ trạng thái với backend.
- **Backend:** các module `catalog`, `inventory`, `crm`, `procurement`, `order`, `branch` giao tiếp nội bộ qua service interface.
- **Nghiệp vụ nhập/xuất kho:**
  - Phiếu nhập → cập nhật `stock_on_hand` (+), tạo công nợ phải trả nếu mua chịu.
  - Phiếu xuất bán → giảm tồn kho (-), sinh công nợ phải thu nếu bán chịu, tính giá vốn theo phương pháp đã cấu hình (bình quân gia quyền / FIFO).
  - Phiếu chuyển kho liên chi nhánh → sinh 2 bản ghi liên kết (xuất chi nhánh A, nhập chi nhánh B), trạng thái "đang chuyển" để đối soát.
- **Phân quyền:** RBAC hai lớp — Role (Admin tổng, Quản lý chi nhánh, Nhân viên bán hàng, Thủ kho, Kế toán) × Branch scope (chỉ thấy dữ liệu chi nhánh được gán, trừ Admin tổng).

### 5.3 Module 3 — Demo/Visualizer sản phẩm gạch

**Kiến trúc kỹ thuật đề xuất:** xử lý chủ yếu ở **client-side (Canvas/WebGL)** để giảm tải server và cho trải nghiệm tức thời.

- Backend (`visualizer` module) chỉ lưu trữ:
  - Ảnh phòng mẫu (đã chuẩn hoá, có sẵn toạ độ 4 điểm góc của vùng cần lót — do admin định nghĩa qua công cụ nội bộ).
  - Metadata sản phẩm gạch (ảnh hoạ tiết pattern, kích thước thực tế viên gạch tính bằng cm).
- Frontend xử lý:
  1. Load ảnh phòng mẫu + polygon vùng lót (4 điểm góc).
  2. Load ảnh pattern gạch, tạo texture lặp lại (tile) theo đúng tỷ lệ kích thước thật so với kích thước phòng mẫu (đã hiệu chỉnh sẵn tỷ lệ pixel/cm khi admin tạo phòng mẫu).
  3. Dùng **phép biến đổi phối cảnh (perspective transform / homography)** giữa 4 điểm góc thực và 4 điểm góc ảnh để "dán" texture đúng góc nhìn — có thể dùng thư viện như `glfx.js`, hoặc tự triển khai bằng WebGL shader.
  4. Cho phép chọn nhiều vùng (sàn/tường) trong cùng phòng mẫu.
  5. Xuất ảnh kết quả (canvas.toDataURL) → tải về hoặc gửi kèm RFQ.
- **Vì sao không AI-render phía server:** demo dạng "warp ảnh có sẵn" (image compositing) rẻ, nhanh, đủ dùng cho nhu cầu; không cần AI sinh ảnh (giữ độ trễ thấp, chi phí thấp, phù hợp giai đoạn đầu). Có thể nâng cấp lên AI room-generation (sinh phòng từ ảnh khách tự chụp) ở giai đoạn 2 nếu cần.

### 5.4 Module 4 — Đa chi nhánh & CSDL phân tán

(Đã trình bày kỹ tại Phần 4). Bổ sung ở tầng ứng dụng:
- Spring Boot dùng **routing DataSource động** (`AbstractRoutingDataSource`) — mỗi request ERP mang theo `branchId` (từ JWT claim hoặc header), tầng infrastructure tự chọn kết nối tới Branch DB tương ứng cho các bảng giao dịch, trong khi vẫn dùng kết nối HQ cho bảng master.
- Đảm bảo transaction boundary rõ ràng: 1 transaction nghiệp vụ không được ghi xuyên 2 DB khác nhau cùng lúc (tránh distributed transaction phức tạp) — nếu cần, dùng **Outbox pattern** để đảm bảo nhất quán cuối cùng khi đồng bộ.

### 5.5 Module 5 — Dashboard tổng hợp chỉ số kinh doanh (Analytics)

- **Nguồn dữ liệu:** Read Replica riêng (không ảnh hưởng OLTP) + data mart dạng bảng tổng hợp trước (materialized view/ETL định kỳ), tối ưu cho truy vấn theo thời gian/chi nhánh/nhóm sản phẩm.
- **Chỉ số chính:** doanh thu, lợi nhuận gộp, tỷ lệ tồn kho quay vòng, top sản phẩm bán chạy/chậm, công nợ quá hạn, so sánh hiệu suất chi nhánh.
- **Frontend:** Next.js + thư viện chart (Recharts/ECharts), có thể filter theo khoảng thời gian, chi nhánh, danh mục.
- **Xuất báo cáo:** API export Excel/PDF (Apache POI phía Spring Boot), lịch báo cáo tự động (Spring Scheduler + email).

---

## PHẦN 6 — TECH STACK, TRIỂN KHAI & LỘ TRÌNH

### 6.1 Tech stack chi tiết

| Thành phần | Công nghệ |
|---|---|
| Frontend Public | Next.js (App Router, SSR/SSG/ISR), Tailwind CSS |
| Frontend ERP/Dashboard | Next.js (CSR), React Query, shadcn/ui hoặc tương đương |
| Visualizer | Canvas API / WebGL (glfx.js hoặc custom shader) |
| Backend | Spring Boot 3.x, Spring Security (JWT + RBAC), Spring Data JPA |
| Giao tiếp nội bộ module | Java interface + Spring Events (đồng bộ nội bộ, không qua mạng) |
| Database | PostgreSQL (Primary HQ + Branch DBs + Read Replica), Logical Replication |
| Cache | Redis (cache catalog public, session) |
| Search (tuỳ chọn mở rộng) | PostgreSQL full-text search giai đoạn đầu → Elasticsearch nếu catalog lớn |
| Object storage | S3-compatible (ảnh sản phẩm, ảnh phòng mẫu, ảnh kết quả demo) |
| CI/CD | GitHub Actions / GitLab CI, Docker, triển khai monolith dạng container duy nhất |
| Observability | Prometheus + Grafana, ELK/Loki cho log tập trung |

### 6.2 Đề xuất lộ trình triển khai (gợi ý, có thể điều chỉnh theo nguồn lực)

1. **Giai đoạn 1 — Nền tảng:** module `identity`, `branch`, `catalog`, hạ tầng PostgreSQL HQ + 1 branch mẫu, frontend public cơ bản.
2. **Giai đoạn 2 — ERP lõi:** `inventory`, `procurement`, `crm`, `order`; hoàn thiện routing đa chi nhánh + logical replication.
3. **Giai đoạn 3 — Trải nghiệm khách hàng:** module `visualizer`, RFQ, tối ưu SEO/hiệu năng trang public.
4. **Giai đoạn 4 — Dữ liệu & báo cáo:** Read Replica, `analytics`/dashboard, export báo cáo, cảnh báo tồn kho.
5. **Giai đoạn 5 — Hoàn thiện vận hành:** observability, backup/DR, load test, hardening bảo mật.

### 6.3 Lưu ý khả năng mở rộng về sau

Vì đã tuân thủ nguyên tắc module hoá nghiêm ngặt (không phụ thuộc chéo Entity/Repository giữa các module), khi hệ thống tăng trưởng đủ lớn (nhiều chi nhánh, traffic cao), có thể "bóc tách" dần các module có tải cao nhất (thường là `catalog` cho public site và `analytics`) thành microservice riêng mà không cần viết lại logic nghiệp vụ — chỉ cần thay lớp giao tiếp nội bộ (Java interface) bằng giao tiếp qua REST/gRPC.

---

*Tài liệu này là bản thiết kế kiến trúc mức cao (high-level design). Ở bước tiếp theo có thể triển khai chi tiết: ERD đầy đủ từng module, đặc tả API (OpenAPI), và wireframe UI cho từng trang.*
