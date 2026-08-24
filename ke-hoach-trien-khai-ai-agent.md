# KẾ HOẠCH TRIỂN KHAI HỆ THỐNG CHO AI CODING AGENT
### Website & ERP Cửa hàng VLXD & TTNT — Modular Monolith (Next.js / Spring Boot / PostgreSQL Replication)

Tài liệu này là **prompt/kế hoạch làm việc chuẩn** dành cho AI agent thực hiện code trực tiếp lên codebase, dựa trên bản thiết kế kiến trúc đã thống nhất trước đó. AI agent cần đọc và tuân thủ tài liệu này **trước khi sinh bất kỳ dòng code nào**, và tham chiếu lại ở đầu mỗi phase/sprint mới.

---

## PHẦN 1 — NGUYÊN TẮC BẮT BUỘC (CODING PRINCIPLES)

> Nguyên tắc áp dụng xuyên suốt **mọi phase**, không phải làm 1 lần rồi bỏ. Mỗi khi AI agent sinh code mới hoặc sửa code cũ, phải tự kiểm tra lại theo checklist ở mục 1.6.

### 1.1 SOLID — áp dụng cụ thể vào từng layer Spring Boot

| Nguyên tắc | Áp dụng cụ thể trong hệ thống này |
|---|---|
| **S — Single Responsibility** | Mỗi class chỉ 1 lý do để thay đổi. VD: `InventoryService` chỉ xử lý nghiệp vụ tồn kho; việc tính giá vốn tách riêng `CostingStrategy`; việc gửi email tách riêng `NotificationService`. Controller chỉ điều phối request/response, không chứa logic nghiệp vụ. |
| **O — Open/Closed** | Nghiệp vụ có nhiều biến thể (VD: phương pháp tính giá vốn FIFO/bình quân gia quyền, loại chiết khấu, loại chi nhánh) phải implement qua **Strategy Pattern** (interface `CostingStrategy`, `DiscountPolicy`...), không dùng `if/else`/`switch` theo enum rải rác trong service. Thêm biến thể mới = thêm class mới, không sửa class cũ. |
| **L — Liskov Substitution** | Mọi implementation của 1 interface phải thay thế được cho nhau mà không phá vỡ hành vi hệ thống. VD: `LocalBranchDataSourceRouter` và `HQDataSourceRouter` đều implement `DataSourceRouter`, service tầng trên không được biết/quan tâm đang chạy implementation nào. |
| **I — Interface Segregation** | Không tạo interface "God Interface". VD: tách `ProductReader` (cho Public site — chỉ đọc) và `ProductWriter` (cho ERP — CRUD) thay vì 1 `ProductService` khổng lồ dùng chung. |
| **D — Dependency Inversion** | Module cấp cao (Service) phụ thuộc vào abstraction (interface), không phụ thuộc vào class cụ thể. Việc inject luôn qua constructor injection (Spring `@RequiredArgsConstructor` của Lombok), không dùng field injection (`@Autowired` trên field) — để dễ test & tuân thủ DIP. |

### 1.2 OOP — chuẩn kiến trúc theo layer

Mỗi module (`catalog`, `inventory`, `crm`...) tuân thủ cấu trúc 4 lớp rõ ràng, **không cho phép** một layer nhảy cóc qua layer khác:

```
xxx/
 ├── api/            → Controller (REST), DTO request/response, Facade interface public cho module khác gọi
 ├── application/     → Service (nghiệp vụ), UseCase, Strategy implementations
 ├── domain/           → Entity, Value Object, Domain Event, business rules thuần (không phụ thuộc Spring/JPA nếu có thể)
 └── infrastructure/   → Repository (JPA), mapper, external integration (S3, email...)
```

- **Encapsulation**: Entity không để field `public`, không expose setter tùy tiện — thay đổi trạng thái qua method nghiệp vụ có validate (VD: `stock.decrease(qty, reason)` chứ không phải `stock.setQuantity(x)`).
- **Composition over Inheritance**: Ưu tiên compose (VD: `Product` có `List<ProductAttribute>`) thay vì kế thừa sâu nhiều tầng.
- **DTO tách biệt Entity**: Không bao giờ trả Entity JPA trực tiếp ra Controller/API response (tránh lộ cấu trúc DB, tránh lỗi lazy-loading). Dùng MapStruct để mapping Entity ↔ DTO.
- **Domain Event cho nghiệp vụ liên module**: VD khi `outbound_receipt` được xác nhận → publish `StockDecreasedEvent`, module `analytics` lắng nghe để cập nhật data mart — tránh gọi trực tiếp chéo module gây coupling chặt.

### 1.3 AOP — xử lý cross-cutting concerns

AI agent triển khai các mối quan tâm xuyên suốt (cross-cutting) bằng **Spring AOP / Annotation**, không lặp code thủ công trong từng service:

| Mối quan tâm | Cách triển khai |
|---|---|
| **Audit log** (ai sửa gì, khi nào — bắt buộc cho nghiệp vụ kho/tài chính) | `@Auditable` annotation + `AuditAspect` tự ghi log trước/sau khi method thực thi (before/after advice), không cần viết log thủ công trong từng service. |
| **Transaction boundary** | `@Transactional` chuẩn Spring ở tầng Service (application layer), không đặt ở Controller hay Repository. |
| **Validation input** | `@Valid` + Bean Validation (Jakarta Validation) ở DTO, kết hợp `@ControllerAdvice` xử lý exception tập trung — không viết `if (input == null) throw...` rải rác. |
| **Phân quyền theo Branch scope** | `@BranchScoped` annotation + Aspect tự động thêm điều kiện `branch_id` vào query/kiểm tra quyền truy cập, tránh quên check ở service nào đó (rủi ro lộ dữ liệu chi nhánh khác). |
| **Đo hiệu năng / logging chậm** | `@LogExecutionTime` Aspect log cảnh báo nếu method nghiệp vụ chạy > ngưỡng cấu hình (VD > 500ms), phục vụ giám sát performance theo NFR. |
| **Retry cho đồng bộ replication/event** | `@Retryable` (Spring Retry) cho các thao tác publish event/đồng bộ có thể fail tạm thời. |

### 1.4 Documentation bắt buộc — theo từng phase

AI agent **không được coi là hoàn thành 1 task nếu thiếu phần document tương ứng**. Yêu cầu tối thiểu mỗi phase:

1. **Javadoc** cho mọi public method ở tầng `api`/`application` (mục đích, tham số, exception có thể ném ra) — không cần Javadoc cho getter/setter đơn giản.
2. **OpenAPI/Swagger** tự sinh từ annotation (`springdoc-openapi`) cho toàn bộ REST endpoint — cập nhật ngay khi thêm/sửa endpoint, không để lệch với code thật.
3. **README.md theo từng module** (`catalog/README.md`, `inventory/README.md`...): mô tả trách nhiệm module, các nghiệp vụ chính, sơ đồ luồng dữ liệu ngắn gọn, dependency tới module khác.
4. **ADR (Architecture Decision Record)** — mỗi quyết định kỹ thuật quan trọng (VD: chọn logical replication thay vì multi-master, chọn Strategy Pattern cho costing) phải ghi lại file ngắn trong `/docs/adr/NNN-tên-quyết-định.md`: bối cảnh, phương án đã cân nhắc, quyết định, hệ quả.
5. **CHANGELOG.md** cập nhật theo từng phase hoàn thành (không cần theo từng commit nhỏ).

### 1.5 Coding convention khác

- **Naming**: package `com.storename.erp.<module>.<layer>`; class Service kết thúc bằng `Service`, Repository bằng `Repository`, DTO Response bằng `...Response`/`...Dto`.
- **Exception handling**: custom exception theo domain (`InsufficientStockException`, `BranchNotFoundException`...), tất cả kế thừa từ 1 `BusinessException` base, xử lý tập trung ở `@ControllerAdvice` trả về mã lỗi chuẩn hoá (error code + message, không lộ stack trace ra client).
- **Immutable DTO**: dùng Java `record` cho Request/Response DTO khi có thể.
- **Git**: nhánh đặt tên `feature/<module>-<mô-tả-ngắn>`, commit theo Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`, `test:`), mỗi PR phải gắn với 1 task/user story cụ thể trong phase.
- **Không hard-code**: cấu hình (ngưỡng cảnh báo tồn kho, timeout replication...) đưa vào `application.yml`/config table, không hard-code trong logic.

### 1.6 Checklist tự kiểm tra trước khi hoàn thành 1 task (AI agent tự chạy qua checklist này)

- [ ] Class/method có đúng 1 trách nhiệm không? (SRP)
- [ ] Có logic rẽ nhánh theo loại/kiểu nên tách Strategy không? (OCP)
- [ ] Có Entity JPA nào bị lộ trực tiếp ra API không? (phải có DTO)
- [ ] Cross-cutting concern (audit, transaction, validation) có dùng annotation/AOP thay vì viết tay không?
- [ ] Đã có Javadoc cho public method, đã cập nhật OpenAPI chưa?
- [ ] Đã cập nhật README module nếu thay đổi trách nhiệm/luồng dữ liệu chưa?
- [ ] Có unit test / black-box test case tương ứng chưa? (xem Phần 3)

---

## PHẦN 2 — KẾ HOẠCH TRIỂN KHAI AGILE THEO PHASES

### 2.1 Nguyên tắc quản lý Agile áp dụng

- **Sprint length**: 2 tuần/sprint (khuyến nghị — có thể điều chỉnh theo tốc độ AI agent + review của người).
- **Mỗi Phase lớn = 1 Epic**, chia thành nhiều Sprint nhỏ, mỗi Sprint có Sprint Goal rõ ràng và Definition of Done (DoD).
- **Definition of Done chung cho mọi sprint** (áp dụng thêm DoD riêng theo từng phase):
  1. Code tuân thủ Phần 1 (SOLID/OOP/AOP/Doc).
  2. Test black-box (Phần 3) cho tính năng trong sprint pass 100%.
  3. Không có lỗi build/lint; CI pipeline xanh.
  4. Document cập nhật (README module, OpenAPI, ADR nếu có quyết định mới).
  5. Demo được luồng nghiệp vụ chính bằng Postman collection hoặc UI thực tế.
- **Người review (human-in-the-loop)**: sau mỗi sprint, cần điểm dừng để con người review/nghiệm thu trước khi AI agent tiếp tục sprint kế — đặc biệt bắt buộc ở các sprint liên quan tài chính/tồn kho/bảo mật.

### 2.2 Tổng quan các Phase lớn

| Phase | Tên | Mục tiêu chính | Số sprint gợi ý |
|---|---|---|---|
| Phase 0 | Nền tảng & hạ tầng | Dựng khung dự án, CI/CD, chuẩn coding, môi trường DB phân tán cơ bản | 1 sprint |
| Phase 1 | Core Domain | Identity, Branch, Catalog — nền dữ liệu master dùng chung toàn hệ thống | 2 sprint |
| Phase 2 | ERP vận hành | Inventory, Procurement, CRM, Order — lõi nghiệp vụ nhập/xuất/công nợ | 3 sprint |
| Phase 3 | Trải nghiệm khách hàng | Public website, RFQ, Module Visualizer gạch | 2–3 sprint |
| Phase 4 | Dữ liệu & báo cáo | Read Replica, Analytics/Dashboard | 2 sprint |
| Phase 5 | Hoàn thiện & vận hành | Observability, bảo mật, DR, load test, tối ưu hiệu năng | 2 sprint |

---

### PHASE 0 — NỀN TẢNG & HẠ TẦNG (1 sprint)

**Mục tiêu:** Có khung dự án chạy được end-to-end (rỗng nghiệp vụ) với đầy đủ tiêu chuẩn kỹ thuật, để mọi phase sau chỉ tập trung vào nghiệp vụ.

**Công việc:**
- Khởi tạo monorepo hoặc 2 repo (frontend Next.js, backend Spring Boot) + cấu trúc package theo Phần 1.2.
- Cấu hình CI/CD (build, lint, test tự động chạy mỗi PR).
- Dựng Docker Compose: PostgreSQL (HQ + 1 branch mẫu), Redis, backend, frontend — chạy local 1 lệnh.
- Cấu hình Spring Security khung (JWT) + cấu trúc RBAC (role, permission) — chưa cần đầy đủ nghiệp vụ.
- Cấu hình logical replication thử nghiệm giữa HQ DB và 1 Branch DB mẫu (proof of concept).
- Thiết lập chuẩn OpenAPI, Javadoc generation, template ADR, template README module.

**Definition of Done Phase 0:**
- `docker compose up` chạy được toàn bộ hệ thống rỗng.
- CI pipeline pass (build + lint).
- 1 ADR mẫu ghi lại quyết định chọn logical replication.
- Đăng nhập JWT giả lập hoạt động (1 endpoint test).

---

### PHASE 1 — CORE DOMAIN: Identity, Branch, Catalog (2 sprint)

**Mục tiêu:** Có dữ liệu nền (master data) chuẩn để mọi module nghiệp vụ sau dựa vào — người dùng đăng nhập được, chi nhánh được định nghĩa, sản phẩm/danh mục quản lý được.

**Sprint 1.1 — Identity & Branch**
- CRUD user, role, permission; RBAC theo Role × Branch scope (áp dụng `@BranchScoped` AOP).
- CRUD chi nhánh (branch), cấu hình chi nhánh (giờ mở cửa, địa chỉ, kho liên kết).
- Đăng nhập/JWT đầy đủ (refresh token, phân quyền endpoint theo role).

**Sprint 1.2 — Catalog**
- CRUD danh mục phân cấp (category), sản phẩm, thuộc tính động (attribute — dùng JSONB hoặc bảng EAV).
- Bảng giá theo chi nhánh/thời điểm (`price_list`).
- API public (read-only, có cache Redis) cho danh mục/sản phẩm — chuẩn bị sẵn cho Phase 3 (website public).
- Đồng bộ Catalog từ HQ xuống Branch DB (logical replication read-only) — kiểm thử thực tế không chỉ PoC.

**Definition of Done Phase 1:**
- Đăng nhập, phân quyền theo branch hoạt động đúng qua các role khác nhau (test black-box với ≥3 role).
- CRUD Catalog đầy đủ qua API, có OpenAPI doc.
- Dữ liệu Catalog sinh ở HQ xuất hiện đúng, đúng thời gian tại Branch DB (đo được replication lag).

---

### PHASE 2 — ERP VẬN HÀNH: Inventory, Procurement, CRM, Order (3 sprint)

**Mục tiêu:** Vận hành được nghiệp vụ nhập – xuất – tồn kho – công nợ – đơn hàng hoàn chỉnh, đúng chuẩn kế toán kho, đây là phase **rủi ro dữ liệu cao nhất** nên cần test kỹ nhất.

**Sprint 2.1 — Inventory nền tảng**
- Model tồn kho theo chi nhánh (`stock_on_hand`), phiếu nhập kho (`inbound_receipt`), Strategy tính giá vốn (FIFO/bình quân gia quyền — implement Strategy Pattern như Phần 1.1).
- Phiếu xuất kho (`outbound_receipt`), quy đổi đơn vị tính (thùng ↔ m², viên ↔ m²).

**Sprint 2.2 — Procurement & CRM**
- Quản lý nhà cung cấp, công nợ phải trả.
- Quản lý khách hàng, phân loại khách lẻ/công trình, công nợ phải thu.
- Chuyển kho liên chi nhánh (2 bản ghi liên kết, trạng thái "đang chuyển" — xem Phần 5.2 thiết kế kiến trúc).

**Sprint 2.3 — Order**
- Đơn hàng tại quầy + đơn hàng từ web (trạng thái: DRAFT/QUOTE_REQUEST → CONFIRMED → COMPLETED).
- Domain Event: `StockDecreasedEvent`, `OrderConfirmedEvent` — chuẩn bị cho module Analytics ở Phase 4 lắng nghe mà không coupling trực tiếp.
- Concurrency control cho tồn kho (tránh bán vượt tồn kho khi nhiều giao dịch đồng thời — dùng optimistic/pessimistic locking phù hợp).

**Definition of Done Phase 2:**
- Toàn bộ luồng nhập → tồn → xuất → công nợ khớp số liệu 100% qua các kịch bản test (không lệch dù chạy đồng thời nhiều giao dịch — xem Test Plan mục 3.3).
- Chuyển kho liên chi nhánh đối soát đúng (không có kho "mất hàng" hoặc "nhân đôi hàng").
- Không tình trạng bán âm kho khi test tải đồng thời.

---

### PHASE 3 — TRẢI NGHIỆM KHÁCH HÀNG: Public Site, RFQ, Visualizer (2–3 sprint)

**Sprint 3.1 — Public website**
- Trang chủ, danh mục/chi tiết sản phẩm (SSR/ISR), tìm kiếm nhanh + nâng cao, trang giới thiệu/chi nhánh.
- SEO: metadata động, sitemap, schema.org Product.
- Giỏ hàng/RFQ (gửi yêu cầu báo giá → sinh `sales_order` trạng thái DRAFT).

**Sprint 3.2 — Visualizer**
- Công cụ nội bộ (admin) định nghĩa phòng mẫu + polygon vùng lót.
- Frontend: chọn phòng mẫu, chọn gạch, perspective transform, xuất/lưu ảnh kết quả, gắn kết quả vào RFQ.

**Sprint 3.3 (nếu cần buffer)** — hoàn thiện responsive, tối ưu hiệu năng trang public (Core Web Vitals).

**Definition of Done Phase 3:**
- Trang public đạt LCP < 2.5s (đo bằng Lighthouse) theo NFR đã đề ra.
- Visualizer cho ra ảnh kết quả đúng tỷ lệ, đúng góc phối cảnh với ≥ 3 phòng mẫu test.
- RFQ tạo đơn hàng nháp thành công, đồng bộ đúng vào ERP.

---

### PHASE 4 — DỮ LIỆU & BÁO CÁO: Read Replica, Analytics (2 sprint)

**Sprint 4.1** — Dựng Read Replica riêng cho Analytics, ETL/materialized view định kỳ tổng hợp doanh thu/tồn kho/công nợ.

**Sprint 4.2** — Dashboard UI (doanh thu, lợi nhuận gộp, tồn kho quay vòng, top sản phẩm, so sánh chi nhánh), export Excel/PDF, cảnh báo tồn kho thấp.

**Definition of Done Phase 4:**
- Dashboard không truy vấn trực tiếp DB giao dịch OLTP (đảm bảo không ảnh hưởng hiệu năng bán hàng — kiểm chứng bằng test tải đồng thời giữa OLTP và Analytics).
- Số liệu dashboard khớp với số liệu ERP gốc (đối chiếu tự động, không lệch quá ngưỡng cấu hình do ETL delay).

---

### PHASE 5 — HOÀN THIỆN & VẬN HÀNH (2 sprint)

- Observability: Prometheus/Grafana, log tập trung, alert replication lag.
- Bảo mật: rà soát OWASP Top 10, kiểm tra RBAC/branch-scope không rò rỉ dữ liệu chéo chi nhánh, mã hoá dữ liệu nhạy cảm.
- Backup & Disaster Recovery: kiểm thử thực tế restore từ backup, xác định RPO/RTO đạt yêu cầu.
- Load test toàn hệ thống theo kịch bản thực tế (xem Phần 3.4).
- Tối ưu hiệu năng dựa trên kết quả load test (index DB, cache, query N+1...).

**Definition of Done Phase 5:** Hệ thống đạt toàn bộ NFR đã cam kết trong tài liệu kiến trúc gốc (hiệu năng, availability, bảo mật, DR) — có báo cáo test đính kèm làm bằng chứng.

---

## PHẦN 3 — TEST PLAN (ĐƠN GIẢN HOÁ, BLACK-BOX, TẬP TRUNG RỦI RO CHÍNH)

### 3.1 Triết lý test

- **Black-box only**: AI agent test qua **API/UI thực tế** (input → output mong đợi), không cần viết test đi sâu vào chi tiết implementation nội bộ (giảm chi phí bảo trì test khi refactor).
- **Không test tràn lan mọi field/mọi trường hợp nhỏ nhặt** — tập trung vào 3 nhóm rủi ro quan trọng nhất theo yêu cầu của bạn:
  1. **Không mất/sai lệch dữ liệu** (data integrity).
  2. **Hiệu năng tốt** (performance).
  3. **Không bị quá tải** (no overload / graceful degradation).
- Mỗi tính năng trong Definition of Done của sprint **phải có ít nhất 1 black-box test case tương ứng** trước khi coi là hoàn thành — không cần coverage 100% dòng code.

### 3.2 Loại test & công cụ

| Loại test | Công cụ | Khi nào chạy |
|---|---|---|
| Functional black-box (API) | Postman/Newman hoặc REST Assured | Mỗi sprint, chạy trong CI |
| Data integrity test (nghiệp vụ kho/tài chính) | REST Assured / script kịch bản đồng thời (concurrent requests) | Bắt buộc ở Phase 2, chạy lại ở Phase 5 |
| Performance test (API latency) | k6 hoặc JMeter | Cuối mỗi phase có API mới quan trọng |
| Load/Stress test | k6 (ramping VUs) | Phase 5, và lại trước khi go-live |
| UI smoke test (happy path) | Playwright (black-box, theo luồng người dùng) | Phase 3 trở đi, cho luồng quan trọng (mua hàng, visualizer, dashboard) |

### 3.3 Test case trọng tâm — Data Integrity (ưu tiên cao nhất)

Đây là nhóm test **bắt buộc phải pass trước khi merge** bất kỳ code nào liên quan Inventory/Order/Finance:

1. **Test bán đồng thời (race condition):** Giả lập N request xuất kho cùng lúc cho 1 sản phẩm có tồn kho giới hạn → tổng số lượng xuất ra **không được vượt** tồn kho ban đầu; không có đơn hàng nào "biến mất" hay bị ghi đè.
2. **Test đối soát nhập-xuất-tồn:** Sau chuỗi ngẫu nhiên các phiếu nhập/xuất, `tồn kho cuối = tồn đầu + tổng nhập − tổng xuất` phải khớp tuyệt đối (kiểm bằng script đối chiếu, không làm tròn sai số).
3. **Test chuyển kho liên chi nhánh:** Sau khi chuyển kho, tổng tồn kho **toàn hệ thống** (chi nhánh A + chi nhánh B) phải không đổi trước/sau giao dịch; không có trạng thái "hàng biến mất" khi 1 trong 2 chi nhánh gặp lỗi giữa chừng (test rollback).
4. **Test replication consistency:** Ghi dữ liệu master (sản phẩm/giá) tại HQ → xác nhận dữ liệu xuất hiện đúng, đúng nội dung tại Branch DB trong ngưỡng thời gian cấu hình (đo replication lag thực tế, không chỉ giả định).
5. **Test rollback khi lỗi giữa transaction:** Ép lỗi giữa chừng 1 nghiệp vụ nhiều bước (VD: tạo đơn hàng + trừ kho + tạo công nợ) → xác nhận **toàn bộ rollback**, không để lại dữ liệu nửa vời (đơn hàng tạo nhưng kho không trừ, hoặc ngược lại).

### 3.4 Test case trọng tâm — Performance & No-Overload

1. **API latency test:** Với mỗi API nghiệp vụ chính (tìm sản phẩm, tạo đơn hàng, dashboard), đo P95 latency dưới tải bình thường (VD 50 concurrent users) — phải đạt ngưỡng NFR đã cam kết (< 300ms cho API thường, < 2s cho báo cáo tổng hợp).
2. **Load test tăng dần (ramping):** Tăng dần số user ảo (VD 50 → 200 → 500) lên hệ thống — xác định **điểm gãy (breaking point)** và xác nhận hệ thống **degrade có kiểm soát** (trả lỗi 429/503 rõ ràng, không crash, không treo toàn bộ) khi vượt ngưỡng — đúng yêu cầu "không bị overload".
3. **Test cách ly tải Analytics khỏi OLTP:** Chạy load test nặng lên Dashboard/Analytics đồng thời với giao dịch bán hàng bình thường → xác nhận tốc độ bán hàng (OLTP) **không bị ảnh hưởng** (vì đã tách Read Replica riêng — đây là test xác nhận kiến trúc mục 4.1 hoạt động đúng thực tế).
4. **Test cache hiệu quả:** So sánh latency API public catalog khi có/không có cache Redis — xác nhận cache giảm tải đáng kể lên PostgreSQL.
5. **Test rate-limit/backpressure:** Gửi tải bất thường (spike) vào API public → xác nhận rate-limiting tại API Gateway layer hoạt động, không để 1 nguồn tải bất thường làm sập toàn hệ thống cho các chi nhánh khác.

### 3.5 Test case theo từng module (Functional Black-box — rút gọn theo tính năng quan trọng, không liệt kê chi tiết từng field)

| Module | Test case chính (Given–When–Then rút gọn) |
|---|---|
| Identity/Branch | Đăng nhập đúng/sai; user role A không truy cập được dữ liệu chi nhánh khác. |
| Catalog | Tạo/sửa sản phẩm → hiển thị đúng trên public API; xoá danh mục cha có sản phẩm con → phải chặn hoặc xử lý đúng theo quy tắc nghiệp vụ. |
| Inventory | (xem mục 3.3, đây là trọng tâm). |
| CRM/Procurement | Công nợ phải thu/phải trả cập nhật đúng sau mỗi giao dịch bán chịu/mua chịu. |
| Order | Trạng thái đơn hàng chuyển đúng luồng (DRAFT → CONFIRMED → COMPLETED), không cho phép nhảy trạng thái bất hợp lệ. |
| Visualizer | Với input phòng mẫu + gạch xác định → ảnh xuất ra đúng tỷ lệ, không lỗi vỡ hình khi đổi sản phẩm gạch khác. |
| Analytics | Số liệu dashboard khớp số liệu nguồn ERP trong ngưỡng ETL delay cho phép. |

### 3.6 Gate kiểm soát chất lượng theo từng Phase

- **Sau Phase 2 (ERP)**: bắt buộc chạy full bộ test Data Integrity (mục 3.3) — đây là **release gate cứng**, không cho qua Phase 3 nếu còn fail.
- **Sau Phase 4 (Analytics)**: chạy test cách ly tải OLTP/Analytics (mục 3.4.3).
- **Trước khi go-live (cuối Phase 5)**: chạy toàn bộ test suite (functional + data integrity + performance + load) 1 lần cuối, có báo cáo tổng hợp làm căn cứ nghiệm thu.

---

## TÓM TẮT QUY TRÌNH LÀM VIỆC CHO AI AGENT

1. Đọc kỹ Phần 1 (nguyên tắc) trước khi bắt đầu bất kỳ phase nào.
2. Với mỗi Phase → chia Sprint theo Phần 2, mỗi sprint bắt đầu bằng việc xác nhận Sprint Goal.
3. Với mỗi task trong sprint → code theo checklist Phần 1.6, viết black-box test tương ứng theo Phần 3 trước khi đánh dấu hoàn thành.
4. Cuối mỗi sprint → chạy Definition of Done, cập nhật document (README/OpenAPI/ADR/CHANGELOG).
5. Tại các Gate kiểm soát chất lượng (mục 3.6) → dừng lại, báo cáo kết quả test cho người review trước khi tiếp tục phase kế tiếp.
