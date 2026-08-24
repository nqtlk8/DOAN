# BỘ PROMPT THEO TỪNG SPRINT CHO AI CODING AGENT
### Tích hợp công cụ Codegraph — dùng cùng với 2 tài liệu trước (Kiến trúc hệ thống + Kế hoạch triển khai)

Tài liệu này cung cấp **prompt sẵn sàng dán trực tiếp** cho AI coding agent, theo từng sprint đã định nghĩa ở kế hoạch Agile. Mỗi prompt tuân theo 1 khung chuẩn để đảm bảo agent luôn: (1) tra cứu codebase hiện có qua Codegraph trước khi viết mới, (2) tuân thủ nguyên tắc SOLID/OOP/AOP/Doc, (3) tự viết black-box test tương ứng, (4) tự chốt Definition of Done.

---

## CÁCH DÙNG TÀI LIỆU NÀY

1. Dán **"Prompt hệ thống chung"** (Phần A) vào đầu mỗi phiên làm việc với AI agent (system prompt / project instructions) — chỉ cần làm 1 lần, áp dụng cho toàn dự án.
2. Ở đầu mỗi sprint, dán **prompt của sprint tương ứng** (Phần B) làm nhiệm vụ chính cho agent.
3. Nếu sprint có nhiều task lớn, có thể tách nhỏ hơn — mỗi prompt trong Phần B đã được chia theo "bước" (Step) để agent xử lý tuần tự, tránh làm tràn lan cùng lúc.
4. Sau khi agent báo hoàn thành 1 sprint, dùng **"Prompt review cuối sprint"** (Phần C) để bắt agent tự audit lại trước khi con người review.

---

## PHẦN A — PROMPT HỆ THỐNG CHUNG (dán 1 lần, áp dụng toàn dự án)

```
Bạn là AI coding agent triển khai hệ thống Website + ERP cho cửa hàng VLXD & TTNT.
Kiến trúc: Modular Monolith — Next.js (frontend) / Spring Boot (backend) / PostgreSQL Replication (Hub-and-Spoke: HQ Primary + Branch DB + Read Replica cho Analytics).

BẠN CÓ CÔNG CỤ CODEGRAPH để tra cứu codebase (tìm class, method, dependency graph, caller/callee, symbol reference). QUY TẮC BẮT BUỘC khi dùng Codegraph:

1. TRƯỚC KHI viết code mới cho 1 nghiệp vụ/entity/service nào đó:
   - Dùng Codegraph tìm xem đã tồn tại class/interface/DTO nào cùng chức năng hoặc tên tương tự chưa (tránh trùng lặp — vi phạm SRP/DRY).
   - Tìm các implementation hiện có của interface liên quan (VD: các Strategy đã có) để giữ đúng convention đặt tên và cấu trúc.
   - Tìm Entity liên quan và các Repository/Service đang dùng Entity đó, để hiểu rõ ranh giới module trước khi thêm field/quan hệ mới.

2. TRƯỚC KHI sửa 1 method/class đã tồn tại:
   - Dùng Codegraph tra cứu "callers" (ai đang gọi hàm này) và "callees" (hàm này gọi gì) để đánh giá phạm vi ảnh hưởng (impact analysis) trước khi sửa.
   - Nếu method được gọi từ ≥ 2 module khác nhau, PHẢI báo cáo rõ trong phần tóm tắt thay đổi (không tự ý sửa breaking change mà không nêu rõ).
   - Kiểm tra xem method có đang được test (unit/black-box) tham chiếu tới không, để không làm vỡ test hiện có mà không cập nhật.

3. TRƯỚC KHI tạo 1 module/package mới:
   - Dùng Codegraph xem sơ đồ dependency giữa các module hiện có, đảm bảo module mới không tạo dependency vòng (circular dependency) với module đã có.
   - Xác nhận module mới chỉ phụ thuộc vào `xxx.api` (Facade) của module khác, không import trực tiếp `domain`/`infrastructure` của module khác.

4. KHI HOÀN THÀNH 1 task, LUÔN dùng Codegraph để tự kiểm tra:
   - Có Entity JPA nào bị leak ra Controller/API response không (tìm caller trả trực tiếp Entity thay vì DTO).
   - Có đoạn code trùng lặp logic (duplicate) với chỗ khác trong hệ thống không.
   - Có vi phạm quy tắc layer (Controller gọi thẳng Repository, bỏ qua Service) không.

NGUYÊN TẮC BẮT BUỘC (áp dụng mọi lúc, xem đầy đủ ở tài liệu "Kế hoạch triển khai" Phần 1):
- SOLID nghiêm ngặt: SRP cho mỗi class, dùng Strategy Pattern cho nghiệp vụ có nhiều biến thể (không if/else theo enum), constructor injection (không field injection), interface tách nhỏ theo nhu cầu người dùng (không "God Interface").
- Cấu trúc 4 layer bắt buộc mỗi module: api / application / domain / infrastructure — không nhảy cóc layer.
- Cross-cutting concern (audit, transaction, validation, branch-scope, đo hiệu năng) triển khai bằng AOP/annotation (@Auditable, @Transactional, @Valid, @BranchScoped, @LogExecutionTime) — không viết tay lặp lại trong từng service.
- DTO tách biệt hoàn toàn khỏi Entity, mapping qua MapStruct.
- Document bắt buộc mỗi task: Javadoc cho public method, cập nhật OpenAPI (springdoc), cập nhật README.md của module nếu thay đổi trách nhiệm/luồng dữ liệu, tạo ADR nếu có quyết định kỹ thuật quan trọng.
- Test: viết black-box test (Postman/REST Assured cho API, Playwright cho UI luồng chính) cho MỌI tính năng mới trước khi báo hoàn thành — ưu tiên tuyệt đối 3 nhóm rủi ro: (a) không mất/sai lệch dữ liệu, (b) hiệu năng đạt NFR, (c) không sập hệ thống khi quá tải.

QUY TẮC BÁO CÁO: Sau mỗi task, tóm tắt ngắn gọn: (1) file đã tạo/sửa, (2) kết quả tra cứu Codegraph quan trọng (nếu có phát hiện đáng chú ý), (3) test case đã viết + kết quả chạy, (4) document đã cập nhật, (5) rủi ro/giả định cần người review xác nhận.

KHÔNG được tự ý bỏ qua bước tra cứu Codegraph hoặc bước viết test để "làm nhanh hơn" — đây là yêu cầu bắt buộc, không phải tuỳ chọn.
```

---

## PHẦN B — PROMPT CHI TIẾT THEO TỪNG SPRINT

### 🟦 SPRINT 0.1 — Nền tảng & hạ tầng

```
NHIỆM VỤ SPRINT 0.1: Dựng khung dự án modular monolith hoàn chỉnh, chưa cần nghiệp vụ thật.

Bước 1 — Khởi tạo cấu trúc:
- Tạo backend Spring Boot 3.x với package gốc com.storename.erp, tạo sẵn khung package rỗng cho các module: identity, branch, catalog, inventory, procurement, crm, order, visualizer, analytics, common, infrastructure — mỗi module có sẵn 4 sub-package api/application/domain/infrastructure (theo đúng chuẩn Phần 1.2 tài liệu Kế hoạch triển khai).
- Tạo frontend Next.js (App Router) với 2 route group: (public) cho website, (admin) cho ERP.
- Trước khi đặt tên package/class đầu tiên, dùng Codegraph xác nhận chưa có class trùng tên trong repo (đề phòng đã có boilerplate cũ).

Bước 2 — Hạ tầng:
- Viết docker-compose.yml gồm: PostgreSQL HQ, PostgreSQL Branch mẫu (branch_hcm01), Redis, backend, frontend.
- Cấu hình logical replication PoC: publication ở HQ cho 1 bảng thử nghiệm (dummy_master_table), subscription ở Branch DB — viết script SQL khởi tạo kèm README hướng dẫn (docs/replication-setup.md).

Bước 3 — Bảo mật khung:
- Cấu hình Spring Security + JWT (access token + refresh token), chưa cần role thật, chỉ cần 1 endpoint /api/health yêu cầu token hợp lệ.
- Tạo sẵn khung AOP: @Auditable + AuditAspect (log ra console tạm), @LogExecutionTime + Aspect, @BranchScoped (chưa có logic thật, chỉ tạo annotation + Aspect rỗng ghi log "not yet implemented").

Bước 4 — CI/CD & chuẩn hoá:
- Cấu hình GitHub Actions: build + lint (Checkstyle/Spotless cho Java, ESLint cho Next.js) + test placeholder chạy mỗi PR.
- Cấu hình springdoc-openapi tự sinh Swagger UI.
- Tạo template ADR tại docs/adr/template.md và viết ADR-001 ghi lại quyết định chọn Hub-and-Spoke + Logical Replication.
- Tạo template README.md cho module tại mỗi package module đã tạo ở Bước 1 (mô tả placeholder "sẽ cập nhật ở sprint sau").

TEST CẦN CÓ: 1 test black-box xác nhận docker compose up thành công và /api/health trả 200 với JWT hợp lệ, trả 401 khi không có token.

DEFINITION OF DONE: docker compose up chạy được toàn hệ thống rỗng; CI pipeline xanh; ADR-001 tồn tại; Swagger UI truy cập được; replication PoC đồng bộ thành công dummy_master_table (verify bằng query trực tiếp 2 DB).
```

---

### 🟦 SPRINT 1.1 — Identity & Branch

```
NHIỆM VỤ SPRINT 1.1: Triển khai module identity (user/role/permission/JWT thật) và branch (CRUD chi nhánh) với RBAC theo Role × Branch scope.

Trước khi bắt đầu: Dùng Codegraph liệt kê toàn bộ class/annotation đã tạo ở Sprint 0.1 liên quan security/AOP (JWT filter, @BranchScoped, AuditAspect) để tái sử dụng đúng, không tạo lại từ đầu.

Bước 1 — Domain & DB:
- Thiết kế entity: User, Role, Permission, RolePermission, Branch (tại HQ DB — bảng master, xem Phần 4.3 tài liệu kiến trúc).
- Migration script (Flyway/Liquibase) — không dùng hibernate auto-ddl cho production schema.

Bước 2 — Nghiệp vụ Identity:
- Đăng ký/đăng nhập, sinh JWT chứa claim: userId, roles, branchId (hoặc danh sách branchId nếu user quản lý nhiều chi nhánh), refresh token flow.
- Áp dụng @BranchScoped thật: Aspect đọc branchId từ JWT claim, tự động thêm điều kiện lọc vào query nếu method đánh dấu annotation này — dùng Codegraph tìm lại các Repository sẽ cần áp annotation này ở sprint sau (inventory, order...) để thiết kế Aspect đủ tổng quát ngay từ đầu, tránh sửa lại nhiều lần.

Bước 3 — Nghiệp vụ Branch:
- CRUD chi nhánh (mã chi nhánh, tên, địa chỉ, giờ mở cửa, kho liên kết).
- API: chỉ role ADMIN_TONG được tạo/sửa/xoá chi nhánh; role khác chỉ được đọc chi nhánh mình thuộc về.

Bước 4 — Strategy cho phân quyền (áp dụng SOLID):
- Nếu có ≥ 2 kiểu kiểm tra quyền khác nhau (VD: theo role đơn giản vs. theo policy phức tạp sau này), thiết kế sẵn interface AuthorizationStrategy để dễ mở rộng — không hard-code if/else theo role name.

TEST CẦN CÓ (black-box, ưu tiên rủi ro bảo mật):
- Đăng nhập đúng/sai mật khẩu.
- User role NHAN_VIEN_CHI_NHANH_A gọi API lấy dữ liệu chi nhánh B → phải bị từ chối (403), viết test cho ít nhất 3 role khác nhau x 2 chi nhánh khác nhau (ma trận quyền).
- Refresh token hết hạn → xử lý đúng luồng.

DEFINITION OF DONE: Ma trận quyền (role × branch) pass 100% test; Swagger cập nhật đầy đủ endpoint identity/branch; README.md của module identity và branch cập nhật mô tả thật (không còn placeholder); ADR mới nếu có quyết định về cấu trúc JWT claim.
```

---

### 🟦 SPRINT 1.2 — Catalog

```
NHIỆM VỤ SPRINT 1.2: Triển khai module catalog (danh mục phân cấp, sản phẩm, thuộc tính động, bảng giá theo chi nhánh) + đồng bộ thật xuống Branch DB.

Trước khi bắt đầu: Dùng Codegraph tìm entity Branch (Sprint 1.1) và cách nó được reference, để thiết kế đúng khoá ngoại/quan hệ cho PriceList theo branch mà không phá vỡ module branch đã có.

Bước 1 — Domain:
- Entity: Category (phân cấp - self-reference parentId), Product, ProductAttribute (JSONB hoặc bảng EAV — quyết định ghi vào ADR, so sánh trade-off 2 phương án trước khi chọn), PriceList (product_id, branch_id, price, effective_date).
- Domain method rõ ràng (không setter tuỳ tiện): product.updatePrice(branchId, newPrice, effectiveDate) thay vì set trực tiếp field.

Bước 2 — API nội bộ (ERP) & API public:
- ERP API (api/admin/products/**): CRUD đầy đủ, có @Auditable cho các thao tác sửa giá (nhạy cảm).
- Public API (api/public/products/**): chỉ đọc, có cache Redis (TTL cấu hình được qua application.yml, không hard-code).
- Tách 2 interface ProductReader (dùng cho public + branch) và ProductWriter (dùng cho ERP HQ) — đúng nguyên tắc Interface Segregation đã thống nhất.

Bước 3 — Đồng bộ Branch thật:
- Cấu hình publication/subscription thật cho bảng product, category, product_attribute, price_list (thay cho dummy_master_table ở Sprint 0.1).
- Viết script đo replication lag + endpoint nội bộ /api/admin/system/replication-status để kiểm tra độ trễ đồng bộ (chuẩn bị cho alert ở Phase 5).

TEST CẦN CÓ:
- CRUD sản phẩm qua API, xác nhận dữ liệu public API trả đúng sau khi cập nhật.
- Test đồng bộ: sửa giá tại HQ → verify xuất hiện đúng tại Branch DB trong ngưỡng thời gian cấu hình (đo thời gian thực tế, ghi vào báo cáo test).
- Test cache: gọi lại API public 2 lần liên tiếp → xác nhận lần 2 nhanh hơn đáng kể (cache hit) — chuẩn bị baseline cho performance test ở Phase 5.
- Test business rule: xoá category cha đang có sản phẩm con → phải bị chặn hoặc xử lý đúng theo quy tắc đã định (không cho xoá "mồ côi" dữ liệu).

DEFINITION OF DONE: Toàn bộ test trên pass; ADR ghi quyết định JSONB vs EAV cho attribute; README module catalog mô tả rõ ranh giới ProductReader/ProductWriter; replication lag đo được và < ngưỡng đã thống nhất trong NFR.
```

---

### 🟦 SPRINT 2.1 — Inventory nền tảng (SPRINT RỦI RO CAO — cần review kỹ)

```
NHIỆM VỤ SPRINT 2.1: Triển khai module inventory — tồn kho theo chi nhánh, phiếu nhập kho, Strategy tính giá vốn. ĐÂY LÀ SPRINT RỦI RO DỮ LIỆU CAO NHẤT — làm chậm, kỹ, test kỹ hơn tốc độ.

Trước khi bắt đầu: 
- Dùng Codegraph tra cứu toàn bộ nơi Product/Branch entity đang được reference (từ Sprint 1.1, 1.2) để thiết kế StockOnHand đúng khoá liên kết, tránh trùng lặp entity.
- Dùng Codegraph kiểm tra @BranchScoped Aspect hiện có (Sprint 1.1) có đủ tổng quát để áp cho Repository của inventory không — nếu chưa, báo cáo cần điều chỉnh Aspect (không viết logic branch-check riêng lẻ trong inventory).

Bước 1 — Domain & concurrency:
- Entity: StockOnHand (product_id, branch_id, quantity, avg_cost — có version field cho optimistic locking @Version của JPA).
- Domain method: stock.increase(qty, unitCost, reason), stock.decrease(qty, reason) — validate không cho âm kho ngay trong domain method (invariant bảo vệ tại nguồn, không chỉ chặn ở service).
- Thiết kế interface CostingStrategy với 2 implementation: WeightedAverageCostingStrategy, FifoCostingStrategy (Strategy Pattern — KHÔNG if/else theo config trong service, injection đúng implementation qua @ConditionalOnProperty hoặc factory).

Bước 2 — Phiếu nhập kho:
- Entity InboundReceipt + InboundReceiptLine, trạng thái (DRAFT → CONFIRMED), khi CONFIRMED thì gọi stock.increase() trong transaction, publish StockIncreasedEvent (domain event, chuẩn bị cho module analytics ở Phase 4 lắng nghe — không gọi trực tiếp analytics module).

Bước 3 — Phiếu xuất kho:
- Entity OutboundReceipt + line, xử lý quy đổi đơn vị tính (unit conversion table: thùng ↔ m², viên ↔ m² — thiết kế UnitConversionService riêng, không nhúng logic quy đổi rải rác).
- Khi CONFIRMED: kiểm tra đủ tồn kho (dùng locking phù hợp — quyết định pessimistic hay optimistic locking, ghi lý do vào ADR), gọi stock.decrease(), publish StockDecreasedEvent.

TEST CẦN CÓ (BẮT BUỘC, đây là gate cứng theo tài liệu kế hoạch — Phần 3.3):
1. Test race condition: bắn N request xuất kho đồng thời (dùng script gọi song song, N ≥ 20) cho 1 sản phẩm tồn kho giới hạn → tổng xuất không vượt tồn kho ban đầu, không có exception "mất" giao dịch.
2. Test đối soát: chuỗi ngẫu nhiên phiếu nhập/xuất → tồn cuối = tồn đầu + tổng nhập − tổng xuất, khớp tuyệt đối.
3. Test rollback: ép lỗi giữa transaction xuất kho (throw exception giả sau khi trừ kho nhưng trước khi commit) → xác nhận toàn bộ rollback, tồn kho không bị trừ treo.
4. Test 2 Strategy costing cho cùng 1 chuỗi giao dịch → kết quả giá vốn khác nhau đúng như công thức kỳ vọng (so khớp bằng tay 1 kịch bản mẫu).

DEFINITION OF DONE: 4 test trên pass 100%, chạy lại ≥ 3 lần liên tiếp không flaky (không lúc pass lúc fail — dấu hiệu race condition chưa xử lý triệt để); báo cáo kết quả test đính kèm số liệu đo được (không chỉ pass/fail) để người review xác nhận trước khi sang Sprint 2.2.
```

---

### 🟦 SPRINT 2.2 — Procurement & CRM & Chuyển kho liên chi nhánh

```
NHIỆM VỤ SPRINT 2.2: Nhà cung cấp, công nợ phải trả, khách hàng, công nợ phải thu, chuyển kho liên chi nhánh.

Trước khi bắt đầu: Dùng Codegraph tra cứu StockOnHand/InboundReceipt/OutboundReceipt (Sprint 2.1) — chuyển kho liên chi nhánh sẽ tái sử dụng logic increase/decrease đã có, KHÔNG viết lại logic trừ/cộng kho riêng cho tính năng này (tránh duplicate — vi phạm DRY).

Bước 1 — Procurement:
- Entity Supplier, PayableDebt (công nợ phải trả) — liên kết InboundReceipt, cập nhật công nợ qua domain event (lắng nghe StockIncreasedEvent nếu phiếu nhập là mua chịu) thay vì gọi trực tiếp chéo module.

Bước 2 — CRM:
- Entity Customer (phân loại LE/CONG_TRINH), ReceivableDebt (công nợ phải thu) — tương tự, cập nhật qua lắng nghe OrderConfirmedEvent (sẽ có ở Sprint 2.3, tạm thời thiết kế interface listener trước, implement đầy đủ khi Order module xong — dùng Codegraph xác nhận không có vòng phụ thuộc ngược giữa crm và order).

Bước 3 — Chuyển kho liên chi nhánh:
- Entity StockTransfer (from_branch, to_branch, status: REQUESTED → IN_TRANSIT → RECEIVED/CANCELLED).
- Nghiệp vụ: tạo StockTransfer → gọi OutboundReceipt tại chi nhánh nguồn (tái sử dụng Bước 1 Sprint 2.1) → khi chi nhánh đích xác nhận nhận hàng → gọi InboundReceipt tại chi nhánh đích. 2 giao dịch này PHẢI có cơ chế đối soát (không dùng distributed transaction phức tạp — dùng Saga đơn giản/Outbox pattern như đã thống nhất trong tài liệu kiến trúc Phần 4.4/5.4).

TEST CẦN CÓ:
- Công nợ phải thu/trả cập nhật đúng sau giao dịch bán/mua chịu.
- Test chuyển kho: tổng tồn kho toàn hệ thống (chi nhánh A + B) không đổi trước/sau; test trường hợp chi nhánh đích KHÔNG xác nhận nhận hàng (hàng đang "IN_TRANSIT") → tồn kho chi nhánh nguồn đã trừ đúng, chi nhánh đích chưa cộng, không bị đếm 2 lần hoặc mất hàng.
- Test lỗi giữa chừng khi chi nhánh đích mất kết nối lúc xác nhận nhận hàng → dữ liệu ở trạng thái nhất quán (IN_TRANSIT), không bị lỗi ngầm.

DEFINITION OF DONE: Đối soát chuyển kho pass 100% qua ≥ 5 kịch bản khác nhau (bao gồm kịch bản lỗi giữa chừng); README module procurement/crm cập nhật rõ cơ chế lắng nghe event (không gọi trực tiếp).
```

---

### 🟦 SPRINT 2.3 — Order

```
NHIỆM VỤ SPRINT 2.3: Module order — đơn hàng tại quầy + đơn hàng web (RFQ), publish domain event cho các module khác.

Trước khi bắt đầu: Dùng Codegraph tìm toàn bộ nơi đã định nghĩa domain event (StockDecreasedEvent, StockIncreasedEvent ở Sprint 2.1; listener đã tạo tạm ở Sprint 2.2) để đảm bảo OrderConfirmedEvent thiết kế đúng convention (naming, payload structure) đã dùng, và để crm module ở Sprint 2.2 lắng nghe được đúng như thiết kế tạm.

Bước 1 — Domain:
- Entity SalesOrder + line, trạng thái: DRAFT (RFQ từ web) → CONFIRMED → COMPLETED/CANCELLED. Dùng State Machine đơn giản (không if/else validate chuyển trạng thái rải rác — có thể dùng enum + Map<TrangThaiHienTai, Set<TrangThaiChoPhep>> hoặc thư viện State Machine nếu phù hợp).
- Khi CONFIRMED: gọi outbound flow (Sprint 2.1) trừ kho, publish OrderConfirmedEvent (payload: orderId, customerId, branchId, totalAmount, lines) — module crm lắng nghe để cập nhật công nợ (hoàn thiện listener đã tạo tạm ở Sprint 2.2).

Bước 2 — Concurrency cho Order:
- Dùng Codegraph xác nhận lại cơ chế locking đã chọn ở Sprint 2.1 (Bước 3) và áp dụng nhất quán khi Order gọi outbound (không tạo 1 cách xử lý khác cho cùng vấn đề tồn kho).

TEST CẦN CÓ:
- Test chuyển trạng thái hợp lệ/không hợp lệ (VD: không cho từ DRAFT nhảy thẳng COMPLETED).
- Test tích hợp toàn luồng: tạo đơn → confirm → xác nhận kho bị trừ đúng + công nợ (nếu bán chịu) được tạo đúng — đây là test end-to-end nối 3 module (order, inventory, crm) qua event, PHẢI verify cả 3 điểm dữ liệu khớp nhau, không chỉ verify riêng lẻ từng module.
- Test đơn hàng đồng thời cạnh tranh cùng 1 sản phẩm tồn kho giới hạn (tái sử dụng kịch bản race condition Sprint 2.1 nhưng qua đường Order thay vì gọi thẳng Outbound).

DEFINITION OF DONE (ĐÂY LÀ GATE CỨNG KẾT THÚC PHASE 2 — theo tài liệu kế hoạch Phần 3.6): Toàn bộ test Data Integrity của Phase 2 (Sprint 2.1 + 2.2 + 2.3) chạy lại 1 lần tổng hợp, pass 100%, có báo cáo đầy đủ trình người review trước khi sang Phase 3.
```

---

### 🟦 SPRINT 3.1 — Public Website

```
NHIỆM VỤ SPRINT 3.1: Trang public (Next.js) — trang chủ, danh mục/chi tiết sản phẩm, tìm kiếm, giới thiệu, RFQ.

Trước khi bắt đầu: Dùng Codegraph (nếu hỗ trợ phân tích cả frontend) hoặc tra cứu thủ công API public đã có ở catalog module (Sprint 1.2) để đảm bảo frontend dùng đúng contract API hiện có, không yêu cầu backend đổi API tuỳ tiện — nếu cần field mới, kiểm tra trước xem ProductReader interface có dễ mở rộng không (đúng OCP).

Bước 1 — Trang chủ & danh mục:
- SSG cho trang giới thiệu/chi nhánh, ISR (revalidate theo khoảng thời gian cấu hình) cho danh sách/chi tiết sản phẩm.
- Component tìm kiếm nhanh + nâng cao gọi API public catalog (có debounce, tránh gọi API quá tần suất — chuẩn bị sẵn cho yêu cầu "không overload" ở Phase 5).

Bước 2 — SEO:
- Metadata động (generateMetadata của Next.js) theo sản phẩm, sitemap.xml tự sinh, JSON-LD schema.org Product.

Bước 3 — RFQ:
- Giỏ hàng tạm (client state), submit → gọi API tạo SalesOrder trạng thái DRAFT (module order, Sprint 2.3) — xác nhận lại với Codegraph rằng API tạo order từ nguồn "web public" dùng đúng endpoint/role phù hợp (không dùng chung endpoint ERP nội bộ có quyền cao hơn cần thiết).

TEST CẦN CÓ:
- Playwright black-box: luồng người dùng tìm sản phẩm → xem chi tiết → thêm giỏ → gửi RFQ → xác nhận đơn DRAFT xuất hiện đúng trong ERP.
- Lighthouse test: đo LCP trang chủ và trang danh mục, phải đạt < 2.5s theo NFR đã cam kết — ghi số liệu vào báo cáo, không chỉ pass/fail cảm tính.

DEFINITION OF DONE: Luồng RFQ end-to-end hoạt động; Lighthouse đạt ngưỡng; sitemap/schema kiểm tra hợp lệ bằng công cụ kiểm tra schema.org.
```

---

### 🟦 SPRINT 3.2 — Visualizer

```
NHIỆM VỤ SPRINT 3.2: Module visualizer — công cụ admin định nghĩa phòng mẫu, frontend "lót thử" gạch bằng perspective transform.

Trước khi bắt đầu: Dùng Codegraph xác nhận cấu trúc module visualizer đã tạo package rỗng từ Sprint 0.1 đúng chuẩn 4-layer, và kiểm tra ProductReader (catalog) đã đủ field cần thiết (ảnh pattern gạch, kích thước thực) chưa — nếu thiếu, làm rõ đây là thay đổi mở rộng (thêm field) chứ không phá vỡ interface hiện có.

Bước 1 — Backend:
- Entity VisualizerRoom (ảnh phòng mẫu, polygon 4 điểm góc theo từng vùng lót — lưu dạng JSON toạ độ), VisualizerSession (lưu lịch sử demo nếu cần phân tích sau).
- API admin: CRUD phòng mẫu + công cụ định nghĩa polygon (có thể trả về endpoint đơn giản, phần vẽ polygon xử lý ở frontend admin).
- API public: lấy danh sách phòng mẫu + metadata cần thiết cho render.

Bước 2 — Frontend (client-side xử lý chính, theo đúng quyết định kiến trúc đã chọn):
- Canvas/WebGL: load ảnh phòng mẫu + polygon, load texture gạch (tile lặp theo tỷ lệ pixel/cm đã hiệu chỉnh sẵn ở phòng mẫu), áp phép biến đổi phối cảnh (homography) giữa polygon thực và toạ độ ảnh.
- Cho phép đổi sản phẩm gạch tức thời (không reload trang), xuất ảnh kết quả (canvas.toDataURL), gắn kèm vào RFQ (Sprint 3.1).

TEST CẦN CÓ:
- Test với ≥ 3 phòng mẫu khác nhau × ≥ 3 sản phẩm gạch khác nhau → ảnh xuất ra không vỡ hình, đúng tỷ lệ (kiểm tra bằng mắt + đối chiếu kích thước pixel dự kiến).
- Test hiệu năng client: đo thời gian render khi đổi gạch (phải cảm giác tức thời, < 500ms trên máy cấu hình trung bình).
- Test admin: tạo phòng mẫu mới với polygon tự vẽ → hiển thị đúng ở phía public ngay sau khi publish.

DEFINITION OF DONE: Demo thực tế được ≥ 3 phòng mẫu; ảnh kết quả gắn được vào RFQ và xuất hiện đúng trong ERP; README module visualizer mô tả rõ thuật toán perspective transform đã dùng.
```

---

### 🟦 SPRINT 4.1 — Read Replica & ETL

```
NHIỆM VỤ SPRINT 4.1: Dựng Read Replica riêng cho Analytics, ETL/materialized view tổng hợp định kỳ.

Trước khi bắt đầu: Dùng Codegraph liệt kê toàn bộ Domain Event đã publish xuyên các module trước đó (StockIncreasedEvent, StockDecreasedEvent, OrderConfirmedEvent...) — đây chính là nguồn dữ liệu chuẩn để module analytics lắng nghe, KHÔNG query trực tiếp bảng OLTP của module khác (giữ đúng ranh giới module đã thiết lập từ Phase 1-2).

Bước 1 — Hạ tầng:
- Cấu hình PostgreSQL streaming replication (vật lý) riêng cho Analytics — tách biệt khỏi Branch DB (logical replication) đã có, đúng như thiết kế Hub-and-Spoke.
- Kiểm tra Codegraph/đối chiếu tài liệu kiến trúc để đảm bảo không nhầm lẫn 2 cơ chế replication (logical cho branch vs. streaming cho analytics).

Bước 2 — ETL/Data mart:
- Module analytics: Event Listener lắng nghe các Domain Event → ghi vào bảng tổng hợp (materialized view hoặc bảng riêng dạng star-schema đơn giản: fact_sales, fact_stock_movement, dim_product, dim_branch...).
- Job định kỳ (Spring Scheduler) refresh materialized view nếu dùng, hoặc batch ETL nếu dùng bảng riêng — cấu hình tần suất qua application.yml (không hard-code).

TEST CẦN CÓ:
- Test đối chiếu số liệu: tạo giao dịch OLTP (bán hàng/nhập kho) → verify số liệu xuất hiện đúng trong data mart trong ngưỡng ETL delay cấu hình.
- Test cách ly tải: chạy tải nặng (query lớn) lên Analytics đồng thời với giao dịch bán hàng bình thường trên OLTP → đo latency OLTP KHÔNG bị ảnh hưởng đáng kể (đây là test xác nhận kiến trúc tách replica hoạt động đúng thực tế, không chỉ lý thuyết).

DEFINITION OF DONE: Đối chiếu số liệu khớp 100% (trong ngưỡng delay cho phép); test cách ly tải chứng minh được bằng số liệu đo (trước/sau khi tách replica).
```

---

### 🟦 SPRINT 4.2 — Dashboard UI & Export

```
NHIỆM VỤ SPRINT 4.2: Dashboard tổng hợp chỉ số kinh doanh, export báo cáo, cảnh báo tồn kho thấp.

Trước khi bắt đầu: Dùng Codegraph xác nhận data mart (fact/dim tables) từ Sprint 4.1 đã có đủ field cần cho các chỉ số yêu cầu (doanh thu, lợi nhuận gộp, tồn kho quay vòng...) — nếu thiếu field, xác định đây là mở rộng ETL chứ không phải query vòng qua OLTP để "chữa cháy".

Bước 1 — API Dashboard:
- API tổng hợp theo filter (thời gian, chi nhánh, danh mục) — query trên Read Replica/data mart, KHÔNG bao giờ trỏ vào OLTP DB.
- Chỉ số: doanh thu, lợi nhuận gộp, tỷ lệ tồn kho quay vòng, top sản phẩm bán chạy/chậm, công nợ quá hạn, so sánh chi nhánh.

Bước 2 — Frontend Dashboard:
- Next.js + thư viện chart, filter UI theo thời gian/chi nhánh/danh mục.

Bước 3 — Export & cảnh báo:
- Export Excel/PDF (Apache POI), job định kỳ gửi email báo cáo tự động.
- Cảnh báo tồn kho thấp: job định kỳ so sánh StockOnHand với ngưỡng cấu hình theo sản phẩm/chi nhánh, gửi notification.

TEST CẦN CÓ:
- Test số liệu Dashboard khớp với số liệu ERP gốc (đối chiếu tự động qua script, không chỉ kiểm tra bằng mắt).
- Test export file → mở file thật, kiểm tra dữ liệu đúng định dạng, không lỗi encoding (đặc biệt tiếng Việt có dấu).
- Test cảnh báo tồn kho: tạo kịch bản tồn kho dưới ngưỡng → xác nhận cảnh báo được sinh đúng, không sinh trùng lặp nhiều lần cho cùng 1 sự kiện.

DEFINITION OF DONE: Dashboard hiển thị đúng ≥ 5 chỉ số chính; export file hợp lệ; cảnh báo hoạt động đúng không trùng lặp.
```

---

### 🟦 SPRINT 5.1 — Observability & Bảo mật

```
NHIỆM VỤ SPRINT 5.1: Prometheus/Grafana, log tập trung, rà soát bảo mật OWASP Top 10, kiểm tra rò rỉ dữ liệu chéo chi nhánh.

Trước khi bắt đầu: Dùng Codegraph quét toàn bộ codebase tìm:
- Mọi Repository/Service KHÔNG có @BranchScoped nhưng đang truy vấn bảng có branch_id (rủi ro rò rỉ dữ liệu chéo chi nhánh — đây là bước rà soát bảo mật quan trọng nhất của sprint này).
- Mọi Controller trả trực tiếp Entity thay vì DTO (rủi ro lộ cấu trúc DB).
- Mọi nơi dùng field injection thay vì constructor injection (không tuân thủ DIP đã thống nhất, dấu hiệu code chưa được review kỹ).

Bước 1 — Observability:
- Tích hợp Micrometer + Prometheus cho backend, dashboard Grafana cho các metrics: latency API, replication lag (tận dụng endpoint đã tạo Sprint 1.2), số lượng lỗi 5xx, connection pool DB.
- Log tập trung (ELK/Loki), đảm bảo AuditAspect (đã tạo Sprint 0.1) ghi log có cấu trúc (structured logging) để dễ truy vấn.

Bước 2 — Bảo mật:
- Khắc phục toàn bộ phát hiện từ bước quét Codegraph ở trên.
- Rà soát OWASP Top 10 (injection, broken auth, sensitive data exposure...) — checklist cụ thể theo từng mục, ghi kết quả vào docs/security-review.md.
- Mã hoá dữ liệu nhạy cảm (mật khẩu — đã bcrypt từ đầu; xem xét thêm cho dữ liệu công nợ/khách hàng nếu cần theo yêu cầu tuân thủ).

TEST CẦN CÓ:
- Test lại toàn bộ ma trận quyền role × branch (tái sử dụng + mở rộng test Sprint 1.1) sau khi khắc phục các phát hiện — đảm bảo không còn endpoint nào bỏ sót @BranchScoped.
- Penetration test cơ bản (SQL injection, XSS) trên các form input chính (đăng nhập, tìm kiếm, tạo đơn hàng).

DEFINITION OF DONE: Không còn phát hiện rò rỉ dữ liệu chéo chi nhánh nào qua quét Codegraph; báo cáo security-review.md hoàn chỉnh; Grafana dashboard hiển thị đủ metrics chính.
```

---

### 🟦 SPRINT 5.2 — Load Test, DR, Tối ưu hiệu năng cuối cùng

```
NHIỆM VỤ SPRINT 5.2: Backup/restore thực tế, load test toàn hệ thống, tối ưu hiệu năng dựa trên kết quả đo được. Đây là sprint nghiệm thu cuối cùng trước go-live.

Trước khi bắt đầu: Dùng Codegraph xác nhận danh sách toàn bộ API endpoint hiện có (qua OpenAPI đã duy trì xuyên suốt các sprint) để đảm bảo load test bao phủ đúng các API nghiệp vụ quan trọng nhất, không bỏ sót.

Bước 1 — Backup & DR:
- Cấu hình backup định kỳ (pg_basebackup/WAL archiving) cho HQ, Branch DB.
- Test restore thực tế trên môi trường staging: xoá dữ liệu giả lập → restore từ backup → verify dữ liệu khớp, đo thời gian restore thực tế (RTO) và dữ liệu mất tối đa (RPO) — so sánh với ngưỡng đã cam kết trong NFR.

Bước 2 — Load test toàn hệ thống:
- Dùng k6: ramping từ 50 → 200 → 500 virtual users trên các luồng chính (tìm sản phẩm, tạo đơn, dashboard) đồng thời — xác định breaking point.
- Xác nhận hệ thống degrade có kiểm soát (trả 429/503 rõ ràng, có rate-limit tại API Gateway layer) khi vượt ngưỡng, KHÔNG crash toàn bộ, KHÔNG ảnh hưởng dữ liệu (chạy lại test data integrity Phase 2 dưới điều kiện tải cao để chắc chắn không có race condition xuất hiện thêm khi tải lớn).

Bước 3 — Tối ưu:
- Dựa trên kết quả load test, dùng Codegraph tìm các query N+1 (Repository gọi lồng trong vòng lặp), thiếu index, hoặc cache chưa áp dụng đúng chỗ — khắc phục có mục tiêu (không tối ưu tràn lan không có số liệu chứng minh).

TEST CẦN CÓ: Toàn bộ test suite từ Phase 0 đến Phase 5 chạy lại 1 lần cuối (regression toàn diện) — functional + data integrity + performance + load, có báo cáo tổng hợp.

DEFINITION OF DONE (GATE GO-LIVE): Toàn bộ NFR đã cam kết đạt được, có số liệu đo thực tế làm bằng chứng (không phải ước lượng); báo cáo tổng hợp trình người review làm căn cứ nghiệm thu cuối cùng.
```

---

## PHẦN C — PROMPT REVIEW CUỐI SPRINT (dùng lại cho mọi sprint)

```
Sprint vừa hoàn thành. Trước khi báo cáo với người review, hãy tự audit lại bằng Codegraph theo checklist sau và trình bày kết quả rõ ràng:

1. Dùng Codegraph liệt kê toàn bộ file mới tạo/sửa trong sprint này.
2. Với mỗi Service mới: xác nhận không có Entity nào bị trả trực tiếp ra Controller (kiểm tra kiểu trả về của method Controller).
3. Với mỗi method public mới: xác nhận có Javadoc, có xuất hiện trong OpenAPI (nếu là REST endpoint).
4. Tìm các đoạn logic trùng lặp (duplicate) với code đã có ở module khác — nếu có, đề xuất refactor (không tự ý refactor module khác nếu chưa được yêu cầu, chỉ đề xuất).
5. Xác nhận mọi Repository truy vấn bảng có branch_id đều có áp dụng @BranchScoped hoặc lý do rõ ràng tại sao không cần (VD: bảng master dùng chung).
6. Liệt kê toàn bộ test case đã viết trong sprint + kết quả chạy (pass/fail, số liệu đo được nếu là performance test).
7. Liệt kê document đã cập nhật (README module, ADR nếu có, CHANGELOG).
8. Nêu rõ mọi giả định hoặc rủi ro cần người review xác nhận trước khi merge vào nhánh chính.

Trình bày kết quả theo đúng 8 mục trên, ngắn gọn, không lặp lại nội dung code đã viết.
```
