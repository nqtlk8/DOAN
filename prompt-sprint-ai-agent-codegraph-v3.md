# BỘ PROMPT THEO TỪNG SPRINT CHO AI CODING AGENT — v3
### Tích hợp Codegraph — dùng cùng "Kiến trúc hệ thống v3" + "Kế hoạch triển khai v3"

> **Ghi chú phiên bản v3:** Cập nhật theo kiến trúc N-instance (mỗi chi nhánh 1 instance + 1 instance HQ), JWT RS256 (HQ ký/Branch verify), không Redis tại chi nhánh, bổ sung Idempotency Key cho API tài chính/kho. Sprint 0.1, 1.1, 2.2, 2.3, 2.4, 5.1, 5.2 thay đổi đáng kể. Sprint 1.2, 3.1 (bổ sung nhỏ), 3.2, 4.1, 4.2 giữ nguyên phần lớn so với v2.

---

## CÁCH DÙNG TÀI LIỆU NÀY

*(Không đổi so với v2.)*

1. Dán **Phần A** vào đầu mỗi phiên làm việc — 1 lần cho toàn dự án.
2. Ở đầu mỗi sprint, dán prompt sprint tương ứng ở **Phần B**.
3. Tách nhỏ hơn theo Step nếu cần.
4. Dùng **Phần C** sau khi agent báo hoàn thành 1 sprint.

---

## PHẦN A — PROMPT HỆ THỐNG CHUNG (dán 1 lần, áp dụng toàn dự án) **[v3 — cập nhật]**

```
Bạn là AI coding agent triển khai hệ thống Website + ERP cho cửa hàng VLXD & TTNT.

KIẾN TRÚC [v3]: Modular Monolith, TRIỂN KHAI N-INSTANCE — mỗi chi nhánh 1 instance
độc lập (Spring Boot + PostgreSQL riêng, KHÔNG Redis) + 1 instance HQ (Spring Boot +
PostgreSQL master + Redis). TẤT CẢ instance dùng CHUNG 1 ARTIFACT (image), chỉ khác
cấu hình qua biến môi trường INSTANCE_ROLE=HQ|BRANCH và BRANCH_ID. Không có instance
nào gọi đồng bộ (synchronous call) tới instance khác, TRỪ 1 ngoại lệ duy nhất: API
"claim RFQ" tại Sprint 3.1 (đã ghi rõ trong ADR riêng, không tạo thêm ngoại lệ nào
khác nếu không có lý do tương đương được ghi ADR).

XÁC THỰC [v3]: JWT RS256. CHỈ profile HQ giữ private key và có endpoint
/auth/login, /auth/refresh. MỌI profile (cả HQ lẫn BRANCH) đều verify JWT bằng
public key, hoàn toàn cục bộ, KHÔNG gọi API nào để xác thực. Trước khi viết bất kỳ
code nào liên quan xác thực, PHẢI xác nhận đang code cho đúng ngữ cảnh nào (chỉ HQ,
chỉ Branch, hay cả 2) — nếu không chắc, dùng Codegraph tra cứu lại thiết kế Sprint 1.1.

REDIS [v3]: CHỈ tồn tại tại profile HQ (cache catalog public + denylist refresh
token). KHÔNG được thêm bất kỳ dependency Redis nào vào code chạy ở profile BRANCH —
nếu cần cache tại Branch, dùng Caffeine in-memory.

BẠN CÓ CÔNG CỤ CODEGRAPH để tra cứu codebase. QUY TẮC BẮT BUỘC:

1. TRƯỚC KHI viết code mới: tìm class/interface/DTO cùng chức năng đã tồn tại chưa;
   tìm implementation hiện có của interface liên quan để giữ đúng convention; tìm
   Entity liên quan và Repository/Service đang dùng.
2. TRƯỚC KHI sửa code đã tồn tại: tra cứu callers/callees để đánh giá phạm vi ảnh
   hưởng; nếu method được gọi từ ≥ 2 module, PHẢI báo cáo rõ; kiểm tra test tham
   chiếu tới method đó.
3. TRƯỚC KHI tạo module/package mới: xem dependency graph, đảm bảo không circular
   dependency; module mới chỉ phụ thuộc `xxx.api` (Facade) của module khác.
4. KHI HOÀN THÀNH task: kiểm tra Entity JPA có leak ra API không; code trùng lặp
   logic không; vi phạm layer không; [v3] có dependency Redis nào lọt vào code
   dùng chung cho profile BRANCH không; [v3] có endpoint /auth/login hay
   /auth/refresh nào vô tình khả dụng ở profile BRANCH không.

NGUYÊN TẮC BẮT BUỘC:
- SOLID nghiêm ngặt, cấu trúc 4 layer, constructor injection.
- Cross-cutting concern qua AOP (@Auditable, @Transactional, @Valid, @BranchScoped,
  @LogExecutionTime, [v3] @IdempotencyProtected).
- DTO tách biệt Entity qua MapStruct.
- Document bắt buộc: Javadoc, OpenAPI, README module, ADR khi có quyết định kỹ
  thuật quan trọng.
- Test: black-box test cho MỌI tính năng mới — ưu tiên tuyệt đối: (a) không mất/sai
  lệch dữ liệu, (b) hiệu năng đạt NFR, (c) không sập hệ thống khi quá tải.
- Thao tác động đến kho + công nợ PHẢI nằm trong CÙNG 1 transaction.
- [v3] MỌI API tạo giao dịch tài chính/kho (SalesInvoice, InboundReceipt,
  GoodsReturn, CustomerOrder, StockTransfer) PHẢI hỗ trợ Idempotency-Key — xem chi
  tiết cơ chế ở Sprint 2.2. KHÔNG tạo API mới loại này mà thiếu cơ chế idempotency.
- [v3] Mọi cấu hình hạ tầng mới thêm PHẢI đọc theo INSTANCE_ROLE, không hard-code
  giả định 1 instance duy nhất.

QUY TẮC BÁO CÁO: Sau mỗi task, tóm tắt (1) file đã tạo/sửa, (2) kết quả tra cứu
Codegraph quan trọng, (3) test case đã viết + kết quả chạy, (4) document đã cập
nhật, (5) rủi ro/giả định cần người review xác nhận — [v3] đặc biệt nêu rõ nếu task
có động chạm tới ranh giới HQ/Branch (auth, Redis, cấu hình instance).

KHÔNG được bỏ qua bước tra cứu Codegraph hoặc bước viết test để "làm nhanh hơn".
```

---

## PHẦN B — PROMPT CHI TIẾT THEO TỪNG SPRINT

### 🟦 SPRINT 0.1 — Nền tảng & hạ tầng N-instance **[v3 — thiết kế lại]**

```
NHIỆM VỤ SPRINT 0.1: Dựng khung dự án chạy được CẢ 2 vai trò (HQ và Branch) từ
CÙNG 1 artifact, chỉ khác cấu hình. Đây là nền tảng cho toàn bộ kiến trúc N-instance
— nếu sprint này sai, mọi sprint sau đều sai theo.

Bước 1 — Khởi tạo cấu trúc:
- Backend Spring Boot 3.x, package gốc com.storename.erp, khung 4-layer cho các
  module: identity, branch, catalog, inventory, procurement, crm, order,
  visualizer, analytics, common, infrastructure.
- Frontend Next.js: route group (public) + (admin).
- Dùng Codegraph xác nhận chưa có class trùng tên trước khi đặt tên đầu tiên.

Bước 2 — Cấu hình đa profile [v3]:
- Tạo application-hq.yml và application-branch.yml, kích hoạt qua biến môi trường
  SPRING_PROFILES_ACTIVE và INSTANCE_ROLE, BRANCH_ID.
- application-hq.yml: bật Redis, đường dẫn PRIVATE key JWT, endpoint /auth/login
  và /auth/refresh được đăng ký.
- application-branch.yml: KHÔNG cấu hình Redis (không có dependency Redis trong
  Spring context khi chạy profile này — kiểm tra bằng cách xác nhận
  RedisAutoConfiguration bị loại trừ hoặc conditional theo profile), chỉ đường dẫn
  PUBLIC key JWT, KHÔNG đăng ký endpoint /auth/login hay /auth/refresh.

Bước 3 — JWT RS256 [v3]:
- Sinh cặp key RS256 (dùng cho môi trường dev/test — ghi rõ trong README rằng môi
  trường production cần quy trình quản lý key riêng, không commit key thật vào repo).
- JwtTokenProvider (chỉ active ở profile hq): ký access token (exp 15-30 phút, claim
  userId/roles/branchId) + refresh token.
- JwtAuthenticationFilter (active ở CẢ 2 profile): verify chữ ký bằng public key,
  parse claim vào SecurityContext — KHÔNG gọi API nào khác.

Bước 4 — Hạ tầng Docker:
- docker-compose.yml: 1 service "hq" (app profile=hq, PostgreSQL HQ, Redis, Nginx
  reverse proxy public), 1 service "branch-sample" (app profile=branch,
  PostgreSQL riêng, Nginx reverse proxy nội bộ, KHÔNG có service Redis nào đi kèm).
- Cấu hình logical replication PoC giữa 2 PostgreSQL (publication tại HQ, subscription
  tại Branch DB) cho 1 bảng thử nghiệm.

Bước 5 — Khung AOP & CI/CD:
- @Auditable + AuditAspect, @LogExecutionTime + Aspect, @BranchScoped (đọc từ JWT
  claim — cần Bước 3 xong trước).
- [v3] @IdempotencyProtected — chỉ tạo annotation rỗng + Aspect log "not yet
  implemented" ở sprint này, implement đầy đủ ở Sprint 2.2.
- GitHub Actions build+lint+test, springdoc-openapi, template ADR + README module.
- Viết ADR-001 (Hub-and-Spoke + Logical Replication) và ADR-002 [v3] (quyết định
  JWT RS256, HQ-only issuance, đánh đổi revocation không tức thời).

TEST CẦN CÓ (BẮT BUỘC, gate cứng theo Kế hoạch v3):
1. Instance "hq" khởi động đúng, có Redis, có private key, /auth/login hoạt động.
2. Instance "branch-sample" khởi động đúng, KHÔNG có Redis (verify bằng cách kiểm
   tra Spring context không có RedisTemplate bean), chỉ có public key,
   /auth/login KHÔNG tồn tại (trả 404, không phải 401/403).
3. [QUAN TRỌNG NHẤT] Lấy 1 JWT hợp lệ từ "hq" → gọi API bất kỳ (VD /api/health) tại
   "branch-sample" → THÀNH CÔNG mà KHÔNG có bất kỳ network call nào từ
   "branch-sample" quay lại "hq" trong lúc xử lý request đó (verify bằng cách ngắt
   hẳn kết nối mạng giữa 2 container sau khi đã lấy JWT, request vẫn phải thành công).
4. Replication PoC đồng bộ đúng.

DEFINITION OF DONE: 4 test trên pass 100%, đặc biệt test 3 PHẢI pass rõ ràng (đây
là gate cứng xác nhận toàn bộ giả định kiến trúc N-instance đúng ngay từ đầu) —
không được sang Sprint 1.1 nếu test 3 chưa pass.
```

---

### 🟦 SPRINT 1.1 — Identity & Branch: JWT RS256 HQ-only issuance **[v3 — thiết kế lại]**

```
NHIỆM VỤ SPRINT 1.1: Hoàn thiện Identity (JWT RS256 đầy đủ, không chỉ khung như
Sprint 0.1) và Branch (CRUD chi nhánh) với RBAC theo Role × Branch scope.

Trước khi bắt đầu: Dùng Codegraph liệt kê JwtTokenProvider, JwtAuthenticationFilter,
@BranchScoped đã tạo ở Sprint 0.1 để tái sử dụng đúng, không viết lại.

Bước 1 — Domain & DB: Entity User, Role, Permission, RolePermission,
UserBranchRole (branch_id nullable = role toàn hệ thống), Branch — tại HQ DB
(xem schema-database-v1.sql). Migration Flyway/Liquibase.

Bước 2 — Đăng nhập & Refresh [v3, chỉ tồn tại ở profile hq]:
- POST /auth/login: kiểm tra credential (user_account tại HQ), ký access token +
  refresh token bằng private key.
- POST /auth/refresh: kiểm tra refresh token có trong denylist (Redis HQ) không →
  nếu có, từ chối; nếu không, cấp access token mới. LƯU Ý: chỉ endpoint này mới
  cần Redis, không phải toàn bộ luồng auth.
- POST /auth/revoke (hoặc tương đương, dùng khi khoá tài khoản): thêm refresh
  token vào denylist Redis HQ.

Bước 3 — Xác thực tại mọi instance [v3, đã có khung từ Sprint 0.1]:
- Hoàn thiện JwtAuthenticationFilter: xử lý đúng token hết hạn, chữ ký sai, claim
  thiếu — trả lỗi 401 rõ ràng, không lộ chi tiết kỹ thuật.
- @BranchScoped Aspect thật: đọc branchId từ SecurityContext (được set bởi
  JwtAuthenticationFilter), tự động thêm điều kiện lọc vào Repository query.

Bước 4 — Branch: CRUD chi nhánh — chỉ role có branchId=null (VD ADMIN_TONG) mới
tạo/sửa/xoá; role có branchId cụ thể chỉ đọc được đúng chi nhánh mình.

TEST CẦN CÓ (BẮT BUỘC):
1. Đăng nhập đúng/sai tại hq.
2. [v3] Verify JWT tại branch-sample hoàn toàn cục bộ — lặp lại test 3 của Sprint
   0.1 nhưng với luồng nghiệp vụ thật (không chỉ /api/health).
3. [v3] Refresh token bị revoke → không cấp access token mới; access token cũ
   (chưa hết hạn) vẫn hoạt động bình thường tại mọi instance cho tới khi hết hạn
   tự nhiên (xác nhận đúng đánh đổi đã chốt, KHÔNG được code sai thành "revoke có
   hiệu lực tức thời" vì điều đó đòi hỏi mọi instance phải online liên tục tới HQ,
   phá vỡ chính mục tiêu kiến trúc).
4. Ma trận quyền (≥ 3 role × 2 chi nhánh, bao gồm 1 role branchId=null) — role
   chi nhánh A gọi API dữ liệu chi nhánh B bị từ chối (403).

DEFINITION OF DONE: 4 test trên pass 100%; README module identity mô tả rõ bảng
"endpoint nào chỉ tồn tại ở hq, endpoint nào tồn tại ở mọi nơi"; ADR-002 (từ Sprint
0.1) được bổ sung chi tiết đầy đủ nếu cần.
```

---

### 🟦 SPRINT 1.2 — Catalog

*(Không đổi so với v2 — Category, Product, ProductAttribute, PriceList, đồng bộ
Branch DB thật qua logical replication. Lưu ý [v3]: API ghi (ProductWriter) chỉ
cần hoạt động đúng tại hq vì đây là master data; API đọc (ProductReader) hoạt động
tại cả hq và mọi branch, đọc từ bản sao local đã replicate xuống.)*

```
NHIỆM VỤ SPRINT 1.2: Triển khai module catalog (danh mục phân cấp, sản phẩm,
thuộc tính động, bảng giá theo chi nhánh) + đồng bộ thật xuống Branch DB.

Trước khi bắt đầu: Dùng Codegraph tìm entity Branch (Sprint 1.1) để thiết kế đúng
khoá ngoại cho PriceList theo branch.

Bước 1 — Domain: Category (phân cấp), Product, ProductAttribute (theo thiết kế
EAV chuẩn 3NF + attributes_cache JSONB denormalize — xem
giai-thich-chuan-hoa-database.md), PriceList (product_id, branch_id, price,
effective_date). Domain method rõ ràng, không setter tuỳ tiện.

Bước 2 — API: ProductWriter (ghi, chỉ cần hoạt động đúng tại hq), ProductReader
(đọc, hoạt động ở mọi nơi — hq cho public site có cache Redis, branch cho ERP nội
bộ không cache Redis, dùng trực tiếp query DB local hoặc Caffeine nếu cần).

Bước 3 — Đồng bộ Branch thật: publication/subscription cho product, category,
product_attribute_value, price_list. Endpoint /api/admin/system/replication-status.

TEST CẦN CÓ: CRUD sản phẩm tại hq; test đồng bộ đo replication lag thực tế xuống
branch-sample; test cache hit/miss tại hq; test branch đọc đúng dữ liệu đã đồng bộ
mà KHÔNG cần Redis; test chặn xoá category cha còn sản phẩm con.

DEFINITION OF DONE: Toàn bộ test pass; replication lag < ngưỡng NFR.
```

---

### 🟦 SPRINT 2.1 — Inventory nền tảng: cho phép âm kho có kiểm soát

*(Không đổi so với v2 — chạy hoàn toàn tại instance chi nhánh, DB cục bộ. Xem
prompt-sprint-ai-agent-codegraph-v2.md để có nội dung đầy đủ: StockOnHand,
decreaseAllowNegative(), Strategy costing, InboundReceipt, UnitConversionService,
5 test bắt buộc về race condition/bán khống/đối soát/rollback/costing.)*

---

### 🟦 SPRINT 2.2 — Bán hàng & Công nợ + Idempotency Key **[v3 — bổ sung cơ chế idempotency]**

```
NHIỆM VỤ SPRINT 2.2: Triển khai hoá đơn bán hàng (SalesInvoice), giá theo khách
hàng (CustomerProductPrice) NHƯ THIẾT KẾ v2, cộng thêm cơ chế Idempotency Key áp
dụng đầu tiên cho SalesInvoice (sẽ tái sử dụng lại ở Sprint 2.3, 2.4).

Trước khi bắt đầu: Dùng Codegraph tra cứu StockOnHand.decreaseAllowNegative()
(Sprint 2.1), khung @IdempotencyProtected rỗng (Sprint 0.1) để tái sử dụng đúng.

Bước 1 — Domain SalesInvoice & CustomerProductPrice: GIỐNG HỆT thiết kế v2 (xem
prompt-sprint-ai-agent-codegraph-v2.md Sprint 2.2 để có chi tiết đầy đủ: entity
SalesInvoice/SalesInvoiceLine, confirm() trong 1 transaction, snapshot
previous_debt/remaining_debt, CustomerProductPrice, template in hoá đơn).

Bước 2 — Idempotency Key [v3, MỚI, implement đầy đủ lần đầu ở sprint này]:
- Bảng idempotency_record (branch-local): id, idempotency_key (UNIQUE), request_hash,
  response_snapshot (JSONB), created_at.
- Hoàn thiện @IdempotencyProtected Aspect (khung đã tạo rỗng ở Sprint 0.1):
  @Around advice đọc header Idempotency-Key từ request hiện tại → query
  idempotency_record → NẾU CÓ: trả lại response_snapshot đã lưu, KHÔNG proceed()
  method thật; NẾU CHƯA CÓ: gọi proceed() xử lý nghiệp vụ thật, sau khi xong LƯU
  lại idempotency_key + response TRONG CÙNG TRANSACTION với chính giao dịch nghiệp
  vụ (đây là điểm kỹ thuật khó nhất — @Auditable dùng AOP tách rời được vì chỉ ghi
  log phụ, nhưng idempotency_record PHẢI cam kết cùng lúc với dữ liệu nghiệp vụ,
  nếu không sẽ có khoảng hở: nghiệp vụ thành công nhưng lưu idempotency_key thất
  bại → request lặp lại sẽ xử lý nghiệp vụ 2 lần).
- Áp dụng @IdempotencyProtected lên SalesInvoiceController.confirm().

TEST CẦN CÓ (BẮT BUỘC, gate cứng):
1-4. Giữ nguyên 4 test từ v2 (đối soát công nợ, giá theo khách hàng, transaction
toàn vẹn, snapshot không đổi) — xem chi tiết ở prompt Sprint 2.2 bản v2.
5. [v3] Test idempotency: gửi 2 lần CÙNG Idempotency-Key cho request tạo hoá đơn
giống hệt nhau (chạy song song thật, không tuần tự, để bắt cả race condition ở
chính bảng idempotency_record) → xác nhận CHỈ 1 hoá đơn được tạo, lần gọi lặp trả
đúng response lần đầu, KHÔNG trừ kho/công nợ lần 2.
6. [v3] Test khác key: 2 request khác Idempotency-Key, nội dung giống nhau →
xác nhận 2 hoá đơn được tạo (idempotency chỉ chặn theo key, không chặn theo nội dung).

DEFINITION OF DONE: 6 test trên pass 100%; README module order mô tả rõ cơ chế
idempotency và lý do phải cùng transaction với nghiệp vụ chính.
```

---

### 🟦 SPRINT 2.3 — Procurement, Chuyển kho liên chi nhánh & PO NCC + Idempotency **[v3]**

```
NHIỆM VỤ SPRINT 2.3: Nhà cung cấp, công nợ phải trả, chuyển kho liên chi nhánh,
SupplierPurchaseOrder — GIỐNG HỆT thiết kế v2, cộng thêm áp dụng Idempotency Key
đã xây ở Sprint 2.2.

Trước khi bắt đầu: Dùng Codegraph tra cứu @IdempotencyProtected + idempotency_record
(Sprint 2.2) — TÁI SỬ DỤNG nguyên vẹn, KHÔNG viết lại bảng/Aspect riêng cho module này.

Bước 1-3: GIỐNG HỆT thiết kế v2 (xem prompt-sprint-ai-agent-codegraph-v2.md Sprint
2.3 để có chi tiết đầy đủ: Supplier, PayableDebt, StockTransfer, SupplierPurchaseOrder,
liên kết purchase_order_id trên InboundReceipt).

Bước 4 — Idempotency [v3]: áp dụng @IdempotencyProtected lên
InboundReceiptController.confirm() và bước tạo StockTransfer.

TEST CẦN CÓ: Giữ nguyên các test từ v2 (đối soát chuyển kho, PO không ảnh hưởng
tồn kho, PO tự động khớp, SalesInvoice không bị chặn bởi PO) + [v3] test idempotency
cho InboundReceipt.confirm() (gửi trùng Idempotency-Key → chỉ 1 phiếu nhập được
tạo, tồn kho chỉ cộng đúng 1 lần).

DEFINITION OF DONE: Toàn bộ test v2 + test idempotency mới pass 100%.
```

---

### 🟦 SPRINT 2.4 — Đặt hàng của khách & Trả hàng + Idempotency **[v3, gate cứng kết thúc Phase 2]**

```
NHIỆM VỤ SPRINT 2.4: CustomerOrder, GoodsReturn — GIỐNG HỆT thiết kế v2, cộng thêm
Idempotency Key. Đây là sprint CUỐI Phase 2 — sau sprint này chạy lại TOÀN BỘ test
Data Integrity từ 2.1 đến 2.4 (bao gồm cả 3 test idempotency của 2.2/2.3/2.4).

Trước khi bắt đầu: Dùng Codegraph tra cứu SalesInvoice/CustomerProductPrice (2.2),
@IdempotencyProtected (2.2) để tái sử dụng đúng.

Bước 1-3: GIỐNG HỆT thiết kế v2 (xem prompt-sprint-ai-agent-codegraph-v2.md Sprint
2.4: CustomerOrder/CustomerOrderLine, logic tự tách dòng hoá đơn, GoodsReturn).

Bước 4 — Idempotency [v3]: áp dụng @IdempotencyProtected lên bước tạo CustomerOrder
và GoodsReturnController.confirm().

TEST CẦN CÓ (BẮT BUỘC, GATE CỨNG kết thúc Phase 2):
1-5. Giữ nguyên 5 test từ v2 (tách dòng tự động, concurrency đơn đặt hàng, trả hàng
có/không hoá đơn gốc, transaction toàn vẹn GoodsReturn).
6. [v3] Test idempotency GoodsReturn: gửi trùng Idempotency-Key → chỉ 1 lần cộng
kho + trừ công nợ.
7. [GATE CỨNG] Chạy lại TOÀN BỘ test Data Integrity Sprint 2.1 → 2.4, BAO GỒM 3
test idempotency (Sprint 2.2 test 5-6, Sprint 2.3 idempotency, Sprint 2.4 test 6)
trong 1 lần tổng hợp — pass 100% mới coi là hoàn thành Phase 2.

DEFINITION OF DONE: 7 nhóm test trên pass 100%, báo cáo tổng hợp trình người review
trước khi bắt đầu Sprint 3.1 — không bỏ qua bước dừng lại chờ review.
```

---

### 🟦 SPRINT 3.1 — Public Website **[v3 — bổ sung cơ chế claim RFQ]**

```
NHIỆM VỤ SPRINT 3.1: Trang public (Next.js, chạy tại instance hq) — trang chủ,
danh mục/chi tiết sản phẩm, tìm kiếm, giới thiệu, RFQ.

[v3] LƯU Ý QUAN TRỌNG: RFQ tạo SalesInvoice trạng thái DRAFT TẠI HQ (không phải
tại branch, vì khách web chưa thuộc chi nhánh nào). Khi nhân viên 1 chi nhánh tiếp
nhận RFQ để xử lý tiếp, cần API POST /api/admin/rfq/{id}/claim tại HQ — API này
gọi ĐỒNG BỘ sang đúng Branch instance để tạo lại SalesInvoice tương ứng tại Branch
DB (đây là NGOẠI LỆ DUY NHẤT trong toàn hệ thống có gọi đồng bộ giữa 2 instance,
đã ghi trong ADR — không tạo thêm ngoại lệ tương tự nếu không có lý do tương đương
được ghi ADR mới).

Trước khi bắt đầu: Dùng Codegraph tra cứu API public catalog (Sprint 1.2) và
SalesInvoice/@IdempotencyProtected (Sprint 2.2) — API claim RFQ cũng cần
Idempotency Key vì đây cũng là API tạo giao dịch (tạo SalesInvoice tại Branch).

Bước 1 — Trang chủ & danh mục: SSG cho trang giới thiệu/chi nhánh, ISR cho danh
sách/chi tiết sản phẩm. Tìm kiếm có debounce.

Bước 2 — SEO: Metadata động, sitemap.xml, JSON-LD schema.org Product.

Bước 3 — RFQ: Giỏ hàng tạm (client state), submit → gọi API tại HQ tạo SalesInvoice
DRAFT (KHÔNG gán branch_id cụ thể, hoặc gán branch_id gợi ý theo địa chỉ khách
nhưng vẫn ở trạng thái "chưa claim").

Bước 4 — Claim RFQ [v3, mới]: API tại HQ nhận request claim từ nhân viên chi
nhánh → gọi API nội bộ (có Idempotency-Key) tới đúng Branch instance để tạo
SalesInvoice DRAFT tương ứng tại Branch DB → đánh dấu bản ghi tại HQ là "đã claim,
xem tiếp tại chi nhánh X" (không xoá, giữ lại để đối soát).

TEST CẦN CÓ: Playwright luồng tìm sản phẩm → xem chi tiết → thêm giỏ → gửi RFQ →
xác nhận SalesInvoice DRAFT xuất hiện tại HQ. [v3] Test claim RFQ: nhân viên claim
→ xác nhận SalesInvoice xuất hiện đúng tại Branch DB tương ứng, gửi trùng request
claim (idempotency) → không tạo trùng. Lighthouse đo LCP.

DEFINITION OF DONE: Luồng RFQ + claim end-to-end hoạt động; Lighthouse đạt ngưỡng
NFR; ADR về ngoại lệ gọi đồng bộ duy nhất này được viết đầy đủ.
```

---

### 🟦 SPRINT 3.2 — Visualizer

*(Không đổi so với v2 — chạy tại instance hq. Xem prompt-sprint-ai-agent-codegraph-v2.md để có nội dung đầy đủ.)*

---

### 🟦 SPRINT 4.1 — Read Replica & ETL

*(Không đổi so với v2 — chạy tại instance hq. Xem prompt-sprint-ai-agent-codegraph-v2.md để có nội dung đầy đủ.)*

---

### 🟦 SPRINT 4.2 — Dashboard UI & Export

*(Không đổi so với v2 — chạy tại instance hq. Xem prompt-sprint-ai-agent-codegraph-v2.md để có nội dung đầy đủ, bao gồm cảnh báo LOW_STOCK/NEGATIVE_STOCK.)*

---

### 🟦 SPRINT 5.1 — Observability & Bảo mật N-instance + JWT **[v3 — mở rộng đáng kể]**

```
NHIỆM VỤ SPRINT 5.1: Prometheus/Grafana, log tập trung, rà soát bảo mật OWASP Top
10 VÀ rà soát riêng cho kiến trúc N-instance + JWT RS256.

Trước khi bắt đầu: Dùng Codegraph quét toàn bộ codebase tìm:
- Mọi Repository/Service KHÔNG có @BranchScoped nhưng truy vấn bảng có branch_id.
- Mọi Controller trả trực tiếp Entity thay vì DTO.
- Mọi nơi dùng field injection.
- Mọi nơi gọi SalesInvoice.confirm()/GoodsReturn.confirm() KHÔNG nằm trong 1
  transaction duy nhất.
- [v3] MỌI dependency Redis xuất hiện trong code path có thể chạy ở profile
  BRANCH — bao gồm cả transitive dependency qua auto-configuration Spring.
- [v3] MỌI nơi private key JWT có thể bị load/tham chiếu ở profile BRANCH.
- [v3] MỌI API tạo giao dịch tài chính/kho CHƯA có @IdempotencyProtected — đối
  chiếu với danh sách đã áp dụng ở Phase 2 (SalesInvoice, InboundReceipt,
  GoodsReturn, CustomerOrder, StockTransfer, RFQ claim).

Bước 1 — Observability: Micrometer + Prometheus, Grafana (latency API,
replication lag, lỗi 5xx, connection pool DB) — [v3] tách riêng dashboard theo
từng loại instance (hq vs branch) vì đặc tính tải khác nhau hoàn toàn. Log tập
trung (ELK/Loki) — [v3] log từ mọi branch gửi về HQ bất đồng bộ, không chặn nghiệp
vụ nếu đường gửi log tạm thời gián đoạn.

Bước 2 — Bảo mật: Khắc phục toàn bộ phát hiện ở bước quét trên. Rà soát OWASP Top
10, ghi docs/security-review.md. [v3] Viết quy trình xoay vòng key JWT RS256 (thêm
`kid` vào JWT header để hỗ trợ nhiều key cùng lúc trong giai đoạn chuyển đổi, ghi
vào ADR, chưa cần tự động hoá ở giai đoạn này).

TEST CẦN CÓ: Ma trận quyền role × branch (mở rộng cho toàn bộ entity Phase 2).
Penetration test cơ bản. [v3] Test xác nhận: build artifact cho profile branch
KHÔNG chứa bất kỳ file private key nào (kiểm tra tại bước CI, không chỉ kiểm tra
runtime).

DEFINITION OF DONE: Không còn phát hiện rò rỉ dữ liệu chéo chi nhánh; không còn
transaction bị tách rời sai; [v3] xác nhận bằng CI rằng artifact profile branch
không thể chứa private key dù vô tình cấu hình sai; báo cáo security-review.md
hoàn chỉnh bao gồm phần rà soát riêng N-instance/JWT.
```

---

### 🟦 SPRINT 5.2 — Load Test, DR theo từng chi nhánh, Tối ưu hiệu năng **[v3 — mở rộng]**

```
NHIỆM VỤ SPRINT 5.2: Backup/restore thực tế THEO TỪNG CHI NHÁNH, load test toàn hệ
thống (cả hq và nhiều branch), test resilience khi mất kết nối/mất điện, tối ưu
hiệu năng. Đây là sprint nghiệm thu cuối cùng trước go-live.

Trước khi bắt đầu: Dùng Codegraph xác nhận danh sách toàn bộ API endpoint (qua
OpenAPI) để load test bao phủ đúng, phân loại theo endpoint chỉ chạy ở hq / chỉ
chạy ở branch / chạy ở cả 2.

Bước 1 — Backup & DR theo từng chi nhánh [v3]: viết script backup
(pg_basebackup/WAL archiving) THAM SỐ HOÁ theo BRANCH_ID, không viết riêng cho
từng chi nhánh — test restore thực tế trên ≥ 2 chi nhánh mẫu trên staging, đo
RTO/RPO cho từng chi nhánh độc lập.

Bước 2 — Test resilience khi mất kết nối/mất điện [v3]: mô phỏng ngắt hoàn toàn
kết nối mạng 1 instance branch khỏi hq (container vẫn chạy) → xác nhận toàn bộ
luồng bán hàng (JWT còn hạn) hoạt động đúng, không lỗi, không mất dữ liệu → khôi
phục kết nối → xác nhận replication tự động catch-up, không trùng/mất dữ liệu.
Lặp lại với kịch bản "tắt hẳn container branch rồi bật lại" (mô phỏng mất điện) —
xác nhận PostgreSQL khởi động lại đúng, không mất giao dịch đã commit trước đó.

Bước 3 — Load test toàn hệ thống: k6 ramping 50 → 200 → 500 virtual users — [v3]
chạy riêng cho hq (tải cao từ public site) và riêng cho 1 branch mẫu (tải thấp,
tập trung đo latency tuyệt đối vì đây là điều quan trọng với nhân viên thao tác
trực tiếp). Xác nhận degrade có kiểm soát, không crash.

Bước 4 — Test rollout N-instance [v3]: deploy artifact mới tuần tự qua ≥ 3 instance
mẫu (hq + 2 branch) → xác nhận không cần đồng bộ thời điểm deploy, mỗi instance
hoạt động bình thường trong lúc instance khác đang cập nhật.

Bước 5 — Tối ưu: Dùng Codegraph tìm query N+1, thiếu index, cache chưa đúng chỗ —
khắc phục dựa trên số liệu load test.

TEST CẦN CÓ: Toàn bộ test suite Phase 0 → 5 chạy lại 1 lần cuối (regression toàn
diện), bao gồm mọi test [v3] mới (idempotency, resilience, backup/restore theo
chi nhánh, rollout).

DEFINITION OF DONE (GATE GO-LIVE): Toàn bộ NFR đạt được cho CẢ 2 loại instance, có
số liệu đo thực tế; test resilience mất mạng/điện pass; backup/restore theo chi
nhánh pass cho ≥ 2 chi nhánh mẫu; báo cáo tổng hợp trình người review làm căn cứ
nghiệm thu cuối cùng.
```

---

## PHẦN C — PROMPT REVIEW CUỐI SPRINT (dùng lại cho mọi sprint)

*(Không đổi so với v2, bổ sung 1 mục.)*

```
Sprint vừa hoàn thành. Trước khi báo cáo với người review, hãy tự audit lại bằng
Codegraph theo checklist sau:

1. Liệt kê toàn bộ file mới tạo/sửa trong sprint này.
2. Với mỗi Service mới: xác nhận không có Entity nào bị trả trực tiếp ra Controller.
3. Với mỗi method public mới: xác nhận có Javadoc, có xuất hiện trong OpenAPI.
4. Tìm đoạn logic trùng lặp với code đã có ở module khác — đề xuất refactor (không
   tự ý refactor module khác nếu chưa được yêu cầu).
5. Xác nhận mọi Repository truy vấn bảng có branch_id đều có @BranchScoped hoặc lý
   do rõ ràng tại sao không cần.
6. [v3] Xác nhận mọi API tạo giao dịch tài chính/kho mới trong sprint này đã có
   @IdempotencyProtected chưa.
7. [v3] Xác nhận không có dependency Redis nào lọt vào code path có thể chạy ở
   profile branch.
8. Liệt kê toàn bộ test case đã viết trong sprint + kết quả chạy.
9. Liệt kê document đã cập nhật (README module, ADR nếu có, CHANGELOG).
10. Nêu rõ mọi giả định hoặc rủi ro cần người review xác nhận trước khi merge.

Trình bày kết quả theo đúng 10 mục trên, ngắn gọn, không lặp lại nội dung code đã viết.
```
