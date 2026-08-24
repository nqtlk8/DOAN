# KẾ HOẠCH TRIỂN KHAI HỆ THỐNG CHO AI CODING AGENT — v3
### Website & ERP Cửa hàng VLXD & TTNT — Modular Monolith triển khai N-instance (Next.js / Spring Boot / PostgreSQL Replication / JWT RS256)

> **Ghi chú phiên bản v3:** Cập nhật theo bản thiết kế kiến trúc v3 — chuyển từ 1 instance backend tập trung sang **N instance độc lập (mỗi chi nhánh 1 instance + 1 instance HQ)**, xác thực **JWT RS256** (HQ ký, chi nhánh chỉ verify), **không Redis tại chi nhánh**, bổ sung **idempotency key** cho API tài chính/kho. Toàn bộ nghiệp vụ (Phase 2) không đổi về logic, chỉ bổ sung yêu cầu idempotency. Các phần thay đổi đánh dấu **[v3]**.

---

## PHẦN 1 — NGUYÊN TẮC BẮT BUỘC (CODING PRINCIPLES)

### 1.1 SOLID — áp dụng cụ thể vào từng layer Spring Boot

*(Không đổi so với v2.)*

| Nguyên tắc | Áp dụng cụ thể |
|---|---|
| **S — Single Responsibility** | Mỗi class chỉ 1 lý do để thay đổi. Controller chỉ điều phối. |
| **O — Open/Closed** | Nghiệp vụ nhiều biến thể (costing, thứ tự ưu tiên giá) dùng Strategy Pattern. |
| **L — Liskov Substitution** | Implementation thay thế được cho nhau. |
| **I — Interface Segregation** | Tách `ProductReader`/`ProductWriter`. |
| **D — Dependency Inversion** | Constructor injection, không field injection. |

### 1.2 OOP — chuẩn kiến trúc theo layer

*(Không đổi so với v2 — cấu trúc 4 layer `api/application/domain/infrastructure`, Entity không setter tuỳ tiện, DTO tách biệt qua MapStruct, Domain Event cho giao tiếp liên module.)*

**[v3] Bổ sung nguyên tắc mới — "Instance-role-aware configuration":** mọi cấu hình động tới hạ tầng (kết nối DB, key JWT, bật/tắt Redis) PHẢI đọc từ biến môi trường/`application.yml` theo `INSTANCE_ROLE` (`HQ` hoặc `BRANCH`), KHÔNG hard-code giả định đang chạy ở vai trò nào. Cùng 1 artifact phải chạy đúng cho cả 2 vai trò chỉ bằng đổi cấu hình, không đổi code.

### 1.3 AOP — xử lý cross-cutting concerns **[v3 — cập nhật bảng]**

| Mối quan tâm | Cách triển khai |
|---|---|
| Audit log | `@Auditable` + `AuditAspect` |
| Transaction boundary | `@Transactional` ở tầng Service |
| Validation input | `@Valid` + Bean Validation + `@ControllerAdvice` |
| Phân quyền theo Branch scope | `@BranchScoped` — **[v3]** đọc `branchId` từ claim JWT (không phải session) |
| Đo hiệu năng | `@LogExecutionTime` |
| Retry đồng bộ | `@Retryable` (Spring Retry) |
| **[v3] Idempotency** | `@IdempotencyProtected` (Aspect mới) — bắt buộc trên mọi domain method tạo giao dịch tài chính/kho (xem Phần 1.6, mục cuối) |

### 1.4 & 1.5 Documentation & Coding convention

*(Không đổi so với v2.)*

### 1.6 Checklist tự kiểm tra trước khi hoàn thành 1 task **[v3 — bổ sung 2 mục]**

- [ ] Class/method có đúng 1 trách nhiệm không? (SRP)
- [ ] Có logic rẽ nhánh theo loại/kiểu nên tách Strategy không? (OCP)
- [ ] Có Entity JPA nào bị lộ trực tiếp ra API không?
- [ ] Cross-cutting concern có dùng annotation/AOP thay vì viết tay không?
- [ ] Đã có Javadoc, đã cập nhật OpenAPI chưa?
- [ ] Đã cập nhật README module nếu thay đổi trách nhiệm/luồng dữ liệu chưa?
- [ ] Có unit test / black-box test case tương ứng chưa?
- [ ] Nếu động tới kho/công nợ: có đảm bảo trừ kho + cập nhật công nợ + fulfill đơn đặt hàng nằm trong **cùng 1 transaction** không?
- [ ] **[v3]** Nếu là API tạo giao dịch tài chính/kho: có hỗ trợ `idempotency_key` chưa? (xem Phần 3.3 Test Plan)
- [ ] **[v3]** Cấu hình mới thêm (nếu có) có đọc theo `INSTANCE_ROLE` thay vì hard-code không? Có giả định "chỉ có 1 instance" ở đâu trong code không?

---

## PHẦN 2 — KẾ HOẠCH TRIỂN KHAI AGILE THEO PHASES

### 2.1 Nguyên tắc quản lý Agile áp dụng

*(Không đổi so với v2.)*

### 2.2 Tổng quan các Phase lớn

| Phase | Tên | Mục tiêu chính | Số sprint gợi ý |
|---|---|---|---|
| Phase 0 | Nền tảng & hạ tầng | **[v3]** Khung dự án N-instance-aware, JWT RS256 key pair, reverse proxy, CI/CD | 1 sprint |
| Phase 1 | Core Domain | **[v3]** Identity với JWT RS256 (HQ ký/Branch verify), Branch, Catalog | 2 sprint |
| Phase 2 | ERP vận hành | Inventory → Bán hàng & công nợ (**[v3]** + idempotency) → Procurement & PO NCC (**[v3]** + idempotency) → Đặt hàng khách & trả hàng (**[v3]** + idempotency) | 4 sprint |
| Phase 3 | Trải nghiệm khách hàng | Public website (chạy tại HQ), RFQ, Visualizer | 2–3 sprint |
| Phase 4 | Dữ liệu & báo cáo | Read Replica, Analytics/Dashboard (chạy tại HQ) | 2 sprint |
| Phase 5 | Hoàn thiện & vận hành | **[v3]** Bảo mật JWT/N-instance, backup riêng từng chi nhánh, test resilience khi mất mạng/điện | 2 sprint |

---

### PHASE 0 — NỀN TẢNG & HẠ TẦNG **[v3 — thiết kế lại cho N-instance]** (1 sprint)

**Mục tiêu:** Khung dự án chạy được cả 2 vai trò (HQ/Branch) từ **cùng 1 artifact**, chỉ khác cấu hình; JWT RS256 key pair sẵn sàng; reverse proxy mẫu.

**Công việc:**
- Khởi tạo backend Spring Boot theo cấu trúc 4-layer × N module như v2, KHÔNG đổi.
- **[v3]** Thiết kế `application.yml` theo profile `hq` và `branch` — biến môi trường `INSTANCE_ROLE`, `BRANCH_ID`, connection string DB, đường dẫn key JWT.
- **[v3]** Sinh cặp key RS256 (private key chỉ tồn tại ở cấu hình profile `hq`; profile `branch` chỉ có public key).
- **[v3]** Docker Compose mẫu: 1 instance `hq` (kèm Redis + PostgreSQL HQ) + 1 instance `branch-sample` (KHÔNG kèm Redis, chỉ PostgreSQL riêng) + reverse proxy (Nginx) cho mỗi instance.
- Cấu hình logical replication PoC giữa HQ và Branch DB mẫu (không đổi cơ chế so với v2, chỉ khác là giờ Branch DB đi kèm 1 app instance riêng thay vì chỉ là DB).
- **[v3]** Khung AOP: `@Auditable`, `@LogExecutionTime`, `@BranchScoped` (đọc từ JWT claim), và **`@IdempotencyProtected`** (khung rỗng, implement đầy đủ ở Sprint 2.2).
- CI/CD, OpenAPI, ADR/README template — không đổi so với v2.

**Test cần có:**
1. Test instance `hq` khởi động đúng, có Redis, có private key.
2. Test instance `branch-sample` khởi động đúng, KHÔNG có Redis, chỉ có public key, verify JWT do `hq` ký thành công.
3. Test replication PoC hoạt động giữa 2 instance.

**Definition of Done:** `docker compose up` chạy được cả 2 vai trò từ cùng 1 image; instance `branch-sample` verify JWT ký bởi `hq` thành công mà không cần gọi API nào tới `hq`; CI xanh; ADR-001 (Hub-and-Spoke) + ADR mới (JWT RS256 HQ-only issuance) tồn tại.

---

### PHASE 1 — CORE DOMAIN: Identity, Branch, Catalog (2 sprint)

#### Sprint 1.1 — Identity & Branch **[v3 — thiết kế lại phần Identity]**

**Mục tiêu:** JWT RS256 — HQ là nơi duy nhất phát hành token, mọi instance chỉ verify; RBAC theo Role × Branch scope; CRUD chi nhánh.

- Entity `User`, `Role`, `Permission`, `RolePermission`, `UserBranchRole` (tại HQ DB, xem `schema-database-v1.sql`).
- **[v3]** Đăng nhập (`POST /auth/login`) CHỈ tồn tại tại profile `hq` — không expose endpoint này ở profile `branch`. Ký access token (15-30 phút) + refresh token bằng private key RS256.
- **[v3]** Refresh token (`POST /auth/refresh`) CHỈ tồn tại tại `hq`, kiểm tra denylist tại Redis HQ trước khi cấp access token mới.
- **[v3]** Filter xác thực (`JwtAuthenticationFilter`) chạy ở MỌI profile (cả `hq` lẫn `branch`) — chỉ verify chữ ký bằng public key, đọc claim `userId`/`roles`/`branchId`, KHÔNG gọi API nào khác để xác thực.
- `@BranchScoped` Aspect thật — đọc `branchId` từ claim JWT hiện tại (SecurityContext), tự thêm điều kiện lọc.
- CRUD chi nhánh — chỉ ADMIN_TONG tạo/sửa/xoá (role toàn hệ thống, claim `branchId = null`).

**Test cần có:**
1. Test đăng nhập tại `hq` thành công/thất bại.
2. **[v3]** Test verify JWT tại instance `branch` KHÔNG cần gọi HQ (đo bằng cách ngắt kết nối branch↔hq, xác nhận request nghiệp vụ vẫn qua được nếu access token còn hạn).
3. **[v3]** Test refresh token bị denylist tại HQ → không cấp access token mới; access token cũ đã phát hành trước đó vẫn hoạt động tới khi hết hạn tự nhiên (đúng đánh đổi đã chốt ở kiến trúc v3).
4. Ma trận quyền (≥ 3 role × 2 chi nhánh) — role chi nhánh A gọi API chi nhánh B bị từ chối.

**Definition of Done:** Toàn bộ 4 test trên pass 100%; README module identity mô tả rõ ranh giới "chỉ HQ phát hành, mọi nơi verify"; ADR ghi lại quyết định đánh đổi revocation không tức thời tuyệt đối.

#### Sprint 1.2 — Catalog

*(Không đổi so với v2 — Category, Product, ProductAttribute, PriceList, đồng bộ Branch DB thật. Chạy tại cả HQ (ghi) và Branch (đọc, replicate xuống).)*

---

### PHASE 2 — ERP VẬN HÀNH (4 sprint) **[v3 — bổ sung idempotency vào 3/4 sprint]**

#### Sprint 2.1 — Inventory nền tảng: cho phép âm kho có kiểm soát

*(Không đổi so với v2 — `StockOnHand`, `decreaseAllowNegative()`, Strategy costing, `InboundReceipt`. Chạy hoàn toàn tại instance chi nhánh, DB cục bộ.)*

#### Sprint 2.2 — Bán hàng & Công nợ: hoá đơn kiêm phiếu xuất kho **[v3 — bổ sung Idempotency Key]**

**Mục tiêu:** Giữ nguyên toàn bộ thiết kế v2 (`SalesInvoice`, `CustomerProductPrice`), bổ sung bắt buộc cơ chế idempotency.

- Toàn bộ domain `SalesInvoice`/`SalesInvoiceLine`/`CustomerProductPrice` — không đổi so với v2.
- **[v3] Bổ sung bảng `idempotency_record`** (branch-local): `id`, `idempotency_key` (unique), `request_hash`, `response_snapshot` (JSONB), `created_at`.
- **[v3] Bổ sung `@IdempotencyProtected`** trên `SalesInvoiceController.confirm()`: đọc header `Idempotency-Key`, kiểm tra tồn tại trong `idempotency_record` trước khi gọi `SalesInvoice.confirm()`; nếu đã tồn tại, trả lại `response_snapshot` đã lưu, không xử lý lại nghiệp vụ; nếu chưa, xử lý bình thường rồi lưu lại **trong cùng transaction** với `SalesInvoice.confirm()`.

**Test cần có (bổ sung so với v2):**
1–4. *(Giữ nguyên 4 test từ v2 — đối soát công nợ, giá theo khách hàng, transaction toàn vẹn, snapshot không đổi.)*
5. **[v3] Test idempotency:** gửi 2 lần **cùng** `Idempotency-Key` cho request tạo hoá đơn giống hệt nhau → xác nhận chỉ **1** hoá đơn được tạo trong DB, lần gọi thứ 2 trả về đúng response của lần đầu (không trừ kho/công nợ lần 2).
6. **[v3] Test idempotency khác key:** gửi 2 request khác `Idempotency-Key` nhưng cùng nội dung → xác nhận **2** hoá đơn được tạo (idempotency chỉ chặn trùng theo key, không chặn theo nội dung — đúng thiết kế, tránh chặn nhầm 2 đơn hàng hợp lệ giống nhau tình cờ).

**Definition of Done:** 6 test trên pass 100% (thêm 2 test so với v2).

#### Sprint 2.3 — Procurement, Chuyển kho liên chi nhánh & Đơn đặt hàng NCC **[v3 — bổ sung Idempotency Key]**

*(Toàn bộ thiết kế nghiệp vụ không đổi so với v2 — `Supplier`, `PayableDebt`, `StockTransfer`, `SupplierPurchaseOrder`.)*

**[v3] Bổ sung:** áp dụng `@IdempotencyProtected` cho `InboundReceipt.confirm()` và bước tạo `StockTransfer` — cùng cơ chế `idempotency_record` đã xây ở Sprint 2.2, tái sử dụng (KHÔNG viết lại bảng/Aspect riêng).

**Test bổ sung:** test idempotency cho `InboundReceipt.confirm()` (gửi trùng `Idempotency-Key` → chỉ 1 phiếu nhập được tạo, tồn kho chỉ cộng 1 lần).

#### Sprint 2.4 — Đặt hàng của khách & Trả hàng **[v3 — bổ sung Idempotency Key, vẫn là gate cứng kết thúc Phase 2]**

*(Toàn bộ thiết kế nghiệp vụ không đổi so với v2 — `CustomerOrder`, `GoodsReturn`, logic tự tách dòng hoá đơn.)*

**[v3] Bổ sung:** áp dụng `@IdempotencyProtected` cho bước tạo `CustomerOrder` và `GoodsReturn.confirm()`.

**Test cần có (bổ sung so với v2, vẫn giữ 6 nhóm test cũ + thêm):**
7. **[v3] Test idempotency GoodsReturn:** gửi trùng `Idempotency-Key` cho trả hàng → chỉ 1 lần cộng kho + trừ công nợ.
8. **[GATE CỨNG, cập nhật]** Chạy lại toàn bộ test Data Integrity từ Sprint 2.1 → 2.4 **bao gồm cả 3 test idempotency mới** (Sprint 2.2, 2.3, 2.4) trong 1 lần tổng hợp — pass 100% mới coi là hoàn thành Phase 2.

**Definition of Done:** Toàn bộ test Phase 2 (bao gồm idempotency) pass 100%, báo cáo trình người review trước khi sang Phase 3.

---

### PHASE 3 — TRẢI NGHIỆM KHÁCH HÀNG: Public Site, RFQ, Visualizer (2–3 sprint)

*(Không đổi so với v2 — chạy tại instance HQ, có Redis cache. RFQ tạo `SalesInvoice` trạng thái `DRAFT` tại HQ — lưu ý: đây là 1 trong số ít nơi `SalesInvoice` được tạo tại HQ thay vì tại chi nhánh, vì khách hàng trên web không thuộc chi nhánh cụ thể nào tại thời điểm gửi RFQ; khi nhân viên xử lý RFQ, hoá đơn cần được "nhận" về đúng chi nhánh xử lý — cần làm rõ cơ chế này trong Sprint 3.1.)*

**[v3] Bổ sung làm rõ cho Sprint 3.1:** Khi RFQ từ web được nhân viên 1 chi nhánh cụ thể tiếp nhận để xử lý, cần 1 bước "chuyển giao" bản ghi `SalesInvoice` DRAFT từ ngữ cảnh HQ sang đúng Branch DB tương ứng (không đơn thuần chỉ set `branch_id` vì dữ liệu này cần nằm ở đúng Branch DB để chạy tiếp `confirm()` cục bộ tại chi nhánh) — cần thiết kế API `POST /api/admin/rfq/{id}/claim` tại HQ, kết quả là tạo lại `SalesInvoice` tương ứng tại đúng Branch DB (qua API nội bộ gọi từ HQ tới Branch instance, đây là **ngoại lệ duy nhất** trong toàn hệ thống có gọi đồng bộ giữa 2 instance — cần ghi rõ vào ADR vì phá vỡ nguyên tắc "không gọi đồng bộ giữa các instance" đã nêu ở kiến trúc v3 Phần 3.1).

---

### PHASE 4 — DỮ LIỆU & BÁO CÁO: Read Replica, Analytics (2 sprint)

*(Không đổi so với v2 — chạy tại instance HQ.)*

---

### PHASE 5 — HOÀN THIỆN & VẬN HÀNH **[v3 — bổ sung đáng kể]** (2 sprint)

#### Sprint 5.1 — Observability & Bảo mật **[v3 — mở rộng phạm vi]**

*(Giữ nguyên yêu cầu Observability + rà soát `@BranchScoped`/OWASP từ v2.)*

**[v3] Bổ sung rà soát bảo mật riêng cho kiến trúc N-instance + JWT:**
- Xác nhận **KHÔNG** instance `branch` nào vô tình có private key JWT trong cấu hình/artifact (đây là lỗi bảo mật nghiêm trọng — nếu 1 chi nhánh bị xâm nhập và có private key, kẻ tấn công có thể tự ký token giả cho toàn hệ thống).
- Xác nhận endpoint `/auth/login` và `/auth/refresh` **CHỈ** tồn tại/hoạt động tại profile `hq`, không vô tình bị expose ở profile `branch`.
- Rà soát toàn bộ nơi dùng `@IdempotencyProtected` — đảm bảo áp dụng đủ cho mọi API tài chính/kho (đối chiếu với danh sách đã liệt kê ở Phase 2).
- Lên kế hoạch xoay vòng key (key rotation) cho JWT RS256 — ghi vào ADR, chưa cần implement tự động ở giai đoạn này nhưng phải có quy trình thủ công rõ ràng (VD: xoay key theo chu kỳ, có `kid` trong JWT header để hỗ trợ nhiều key cùng lúc trong giai đoạn chuyển đổi).

#### Sprint 5.2 — Load Test, DR, Tối ưu hiệu năng cuối cùng **[v3 — bổ sung test riêng N-instance]**

*(Giữ nguyên yêu cầu Load test/Backup/Tối ưu từ v2, áp dụng cho TỪNG instance thay vì 1 instance duy nhất.)*

**[v3] Bổ sung test bắt buộc:**
1. **Test backup/restore riêng từng chi nhánh:** không chỉ test 1 lần cho 1 chi nhánh mẫu — xác nhận quy trình backup/restore áp dụng nhất quán được cho MỌI chi nhánh (script tham số hoá theo `BRANCH_ID`, không viết riêng cho từng chi nhánh).
2. **Test resilience khi 1 chi nhánh mất mạng/mất điện:** mô phỏng ngắt hoàn toàn kết nối 1 instance chi nhánh khỏi HQ trong khi vẫn giữ instance đó chạy → xác nhận nhân viên (JWT còn hạn) vẫn tạo/xác nhận hoá đơn thành công bình thường; khôi phục kết nối → xác nhận replication tự động catch-up, không mất dữ liệu, không trùng dữ liệu.
3. **Test rollout N-instance:** deploy artifact mới tuần tự qua ≥ 2 instance mẫu (mô phỏng HQ + 2 chi nhánh) → xác nhận không cần đồng bộ thời điểm deploy giữa các instance, mỗi instance hoạt động bình thường trong lúc các instance khác đang được cập nhật.
4. Load test + rollback + tối ưu — như v2, chạy trên từng loại instance (HQ chịu tải cao từ public site; Branch chịu tải thấp nhưng cần đo latency cục bộ tốt).

**Definition of Done Phase 5 (GATE GO-LIVE):** Toàn bộ NFR đã cam kết đạt được cho **cả 2 loại instance** (HQ và Branch), có số liệu đo thực tế cho từng loại; test resilience khi mất mạng/điện pass; backup/restore áp dụng được cho mọi chi nhánh bằng quy trình chung.

---

## PHẦN 3 — TEST PLAN (ĐƠN GIẢN HOÁ, BLACK-BOX, TẬP TRUNG RỦI RO CHÍNH)

### 3.1 & 3.2 Triết lý test, loại test & công cụ

*(Không đổi so với v2 — black-box only, 3 nhóm rủi ro chính: không mất/sai lệch dữ liệu, hiệu năng tốt, không overload.)*

### 3.3 Test case trọng tâm — Data Integrity **[v3 — bổ sung nhóm Idempotency & Resilience]**

**Nhóm gốc (giữ nguyên từ v2, 12 test case — xem Kế hoạch v2 để có đầy đủ chi tiết):** bán đồng thời/âm kho, đối soát nhập-xuất-tồn, chuyển kho, replication consistency, rollback, đối soát công nợ, giá theo khách hàng, tách dòng đơn đặt hàng, concurrency đơn đặt hàng, trả hàng, PO không ảnh hưởng tồn kho.

**Nhóm mới bổ sung [v3]:**

13. **Test idempotency (áp dụng cho `SalesInvoice`, `InboundReceipt`, `GoodsReturn`, `CustomerOrder`, `StockTransfer`):** gửi trùng `Idempotency-Key` → chỉ xử lý nghiệp vụ 1 lần, lần gọi lặp trả về đúng kết quả lần đầu.
14. **Test resilience khi mất kết nối HQ:** ngắt kết nối 1 instance chi nhánh khỏi HQ, xác nhận toàn bộ luồng bán hàng vẫn hoạt động đúng với JWT còn hạn (không mất/sai dữ liệu do thiếu kết nối HQ).
15. **Test backup/restore theo chi nhánh:** restore dữ liệu 1 chi nhánh từ backup riêng, xác nhận khớp đúng dữ liệu tại thời điểm backup — độc lập với cơ chế replication lên HQ.

### 3.4 & 3.5 Performance/No-Overload, Test case theo module

*(Không đổi so với v2, riêng mục 3.5 bổ sung dòng cho Identity: "Test JWT verify tại Branch không gọi HQ; test denylist refresh token hoạt động đúng".)*

### 3.6 Gate kiểm soát chất lượng theo từng Phase **[v3 — bổ sung gate Phase 0 và Phase 5]**

- **Sau Phase 0:** xác nhận instance `branch-sample` verify JWT ký bởi `hq` mà không cần gọi API nào tới `hq` — gate này phải pass trước khi bắt đầu Phase 1, vì toàn bộ thiết kế N-instance phụ thuộc vào việc này đúng ngay từ đầu.
- **Sau Phase 2 (ERP — 4 sprint):** chạy full bộ test Data Integrity (mục 3.3, cả 15 test case bao gồm idempotency) — release gate cứng.
- **Sau Phase 4 (Analytics):** test cách ly tải OLTP/Analytics.
- **Sau Phase 5:** test resilience khi mất mạng/điện + backup/restore theo chi nhánh — gate cứng cuối cùng trước go-live.
- **Trước khi go-live:** chạy toàn bộ test suite 1 lần cuối, có báo cáo tổng hợp.

---

## TÓM TẮT QUY TRÌNH LÀM VIỆC CHO AI AGENT

1. Đọc kỹ Phần 1 (nguyên tắc, bao gồm nguyên tắc mới "Instance-role-aware configuration") trước khi bắt đầu bất kỳ phase nào.
2. Với mỗi Phase → chia Sprint theo Phần 2, mỗi sprint bắt đầu bằng việc xác nhận Sprint Goal.
3. Với mỗi task trong sprint → code theo checklist Phần 1.6 (đã bổ sung 2 mục idempotency + instance-role-aware), viết black-box test theo Phần 3 trước khi đánh dấu hoàn thành.
4. Cuối mỗi sprint → chạy Definition of Done, cập nhật document.
5. Tại các Gate kiểm soát chất lượng (mục 3.6, đã bổ sung gate Phase 0 và Phase 5) → dừng lại, báo cáo kết quả test cho người review trước khi tiếp tục phase kế tiếp.
