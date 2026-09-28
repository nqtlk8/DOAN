# Code Review toàn bộ source — ERP Platform

- Ngày: 2026-09-18 · Commit gốc: `809b14f` (+ thay đổi chưa commit của Sprint 38/39)
- Phạm vi: `services/erp-backend` (157 file main, 63 file test), `apps/erp-frontend` (56 file .ts/.tsx), `packages/api-contract`, migration Flyway, docker-compose/nginx, tài liệu trong `code/docs`.
- Trọng tâm theo yêu cầu: **nguyên tắc code, rủi ro bug, lệch schema ↔ API contract, lệch docs ↔ code**. Không đánh giá đúng/sai nghiệp vụ (vd. cho phép âm kho được coi là quyết định nghiệp vụ đã chốt).
- Cách làm: quét tự động (parse entity ↔ migration, controller ↔ openapi.json, URL frontend ↔ endpoint backend, annotation security/transaction/scheduling) + đọc tay các file lõi (SalesInvoiceService, GoodsReturnService, ReceivableDebtService, InventoryFacadeImpl, IdempotencyAspect, JwtAuthenticationFilter, SecurityConfig, GlobalExceptionHandler, SalesOrderForm, GenericDocumentForm, axiosInstance, AuthContext).
- **Giới hạn**: chưa build/chạy test được trong môi trường review (không có JDK 21 + Maven Central bị chặn; `npm install` lỗi nội bộ). Các phát hiện dưới đây dựa trên đọc code tĩnh, mỗi mục đều có trích dẫn file:line để kiểm chứng.

## Tổng hợp

| Mức | Số lượng | Ý nghĩa |
|---|---|---|
| 🔴 Nghiêm trọng | 4 | Mất an toàn, tính năng chết, hoặc chặn người khác build/test |
| 🟠 Cao | 12 | Gây bug thật hoặc che giấu lỗi trong vận hành |
| 🟡 Trung bình | 15 | Nợ kỹ thuật, rủi ro khi dữ liệu/đội ngũ lớn lên |
| ✅ Đạt | 6 nhóm | Đã kiểm tra, không phát hiện vấn đề |

Ba việc nên làm trước tiên: **E1** (test không chạy được trên máy sạch), **A1** (private key trong repo), **B1+B2** (annotation không có hiệu lực).

---

## A. Bảo mật & cấu hình

**A1 🔴 Private key ký JWT nằm trong git.**
`services/erp-backend/src/main/resources/keys/private.pem` đang được git theo dõi (`git ls-files` xác nhận), dù `.gitignore` có `*.pem` — file đã vào index từ trước khi có rule. Đây là khoá dùng cho profile `local`.
→ `git rm --cached` file này, tạo khoá mới cho môi trường dev, và coi khoá cũ là đã lộ. Commit `3ec58d9` mới dọn một khoá khác, còn sót file này.

**A2 🟠 Access token không thể thu hồi.**
`AuthService.revoke` chỉ ghi `tokenId` của **refresh token** vào Redis, và chỉ `refresh()` mới kiểm tra danh sách thu hồi. `JwtAuthenticationFilter` không kiểm tra gì → sau khi bấm Đăng xuất, access token vẫn dùng được tới 30 phút. Instance Branch còn không có Redis nên không thể kiểm tra kể cả khi muốn.
→ Tối thiểu: rút ngắn TTL access token, hoặc thêm bộ nhớ thu hồi dùng chung mà Branch đọc được.

**A3 🟠 Token ADMIN được chấp nhận trên instance BRANCH và đọc được dữ liệu mọi chi nhánh.**
`JwtAuthenticationFilter.java:63` chỉ chặn khi `branchId != null`; token ADMIN có `branchId = null` nên đi lọt. Sau đó `AuthUtils.getBranchIdOrNull()` trả null, và các read path hiểu null = "xem tất cả": `StockService.java:27` (`findAll`), `CustomerReadController.java:28`, `ReceivableDebtController.java:38`.
→ Ở instance BRANCH nên từ chối token không có `branchId`, hoặc ép dùng `instance.branch-id` thay vì null.

**A4 🟠 Thiếu `@PreAuthorize` trên endpoint đọc thông tin hạ tầng.**
`BranchController.java:34` và `:41` (GET danh sách/chi tiết chi nhánh) không có annotation → mọi user đã đăng nhập, kể cả STAFF, đọc được `internal_url` của tất cả chi nhánh.

**A5 🟡 6 endpoint ghi nhận `@RequestBody` nhưng không `@Valid`:**
`BranchController:26`, `BranchController:48`, `ReceivableDebtController:50` (opening-balance), `AuthController:25/31/37`. (9/18 endpoint ghi có `@Valid`.)

**A6 🟡 Access + refresh token đều nằm trong `localStorage`** (35 vị trí trong frontend) → một lỗ XSS lấy được cả hai. `AGENTS.md` đang quy định như vậy, nếu giữ thì nên ghi rõ đây là đánh đổi có ý thức.

**A7 🟡 Credential DB hardcode** trong `docker-compose.yml` và default của `application-{hq,branch}.yml` (`app_user/app_password`). Chấp nhận được cho đồ án, nhưng nên tách ra `.env` trước khi demo.

---

## B. Tính đúng đắn & rủi ro bug

**B1 🔴 `@Scheduled` không bao giờ chạy — tính năng cảnh báo tồn kho là code chết.**
`ErpApplication.java` chỉ có `@SpringBootApplication` + `@EnableRetry`, **không có `@EnableScheduling`**. Hai job `AnalyticsEtlJob:20` và `LowStockAlertJob:30` không bao giờ được gọi. `AnalyticsEtlJob` tự nhận là scaffold (chấp nhận được), nhưng `LowStockAlertJob` có logic thật (ghi `inventory_alert_log`, gửi mail) và im lặng không chạy.
→ Thêm `@EnableScheduling` (và cân nhắc `@SchedulerLock` khi chạy nhiều instance), hoặc xoá job nếu chưa dùng.

**B2 🔴 `@Cacheable`/`@CacheEvict` không có hiệu lực.**
Không có `@EnableCaching` ở đâu trong source. `ProductReader:30,39` và `ProductWriter:32,61,117` gây ấn tượng đã có cache nhưng thực tế mọi request đều xuống DB.
→ Bật `@EnableCaching` + cấu hình CacheManager (HQ: Redis đã có; Branch: Caffeine như ghi chú trong `application-branch.yml`), hoặc gỡ annotation.

**B3 🟠 Idempotency có thể kẹt vĩnh viễn và bảng phình vô hạn.**
`IdempotencyAspect.java:67` ghi `IN_PROGRESS` trước khi chạy nghiệp vụ; nếu tiến trình chết/timeout giữa chừng, bản ghi này không bao giờ được dọn, và mọi request sau cùng `Idempotency-Key` sẽ nhận 409 "Request is currently processing" mãi mãi (`:86`). Ngoài ra `idempotency_record` không có TTL hay job dọn.
→ Thêm cột thời điểm + coi `IN_PROGRESS` quá N phút là hết hạn; thêm job/`DELETE` định kỳ.

**B4 🟠 Nhiều lỗi nghiệp vụ bị trả về HTTP 500.**
`GlobalExceptionHandler` không xử lý `IllegalStateException`, `ObjectOptimisticLockingFailureException`, `DataIntegrityViolationException`, nên các trường hợp sau rơi vào handler `Exception` → 500 "Internal server error": "Invoice is not DRAFT" (`SalesInvoiceService:167`), "Receivable debt cannot become negative" (`ReceivableDebtService:64`), "Cannot set opening balance..." (`:77`). Cùng nhóm: `RuntimeException("Unauthorized")` ở `SalesInvoiceService:187`, `GoodsReturnService:154`, `InboundReceiptService:103` — lẽ ra 403.
→ Bổ sung handler: `IllegalStateException` → 409, optimistic/integrity → 409, và thay `RuntimeException("Unauthorized")` bằng `SecurityException` (đã có handler 403).

**B5 🟠 Hành vi khi `branchId = null` không nhất quán giữa các module.**
`SalesInvoiceService:179` trả **rỗng** (`findByBranchId(null)`), trong khi Stock/Customer/Debt trả **tất cả**. Cùng một token ADMIN sẽ thấy dữ liệu ở màn này nhưng trống ở màn kia.
→ Chốt một quy ước (khuyến nghị: null = tất cả, và chặn ở tầng security như A3).

**B6 🟠 Vi phạm rules-of-hooks bị eslint làm ngơ.**
`hooks/useSalesInvoice.ts:14`: `const getById = (id: string) => useQuery({...})` — gọi hook trong hàm thường; nếu có component nào gọi `getById` trong nhánh điều kiện, React sẽ vỡ. Rule phát hiện việc này đã bị hạ xuống `warn` ở `eslint.config.mjs:19` (cùng với `react-hooks/set-state-in-effect: off`, `immutability: off` từ Sprint 36).
→ Đổi thành `useSalesInvoiceById(id)` và trả `rules-of-hooks` về `error`.

**B7 🟠 Nút "Xóa chứng từ" không xoá gì.**
`SalesOrderForm.tsx` (khối `onConfirm` của `ConfirmDialog`): nhánh `DELETE` chỉ gọi `closeTab(activeTabId)` — giống hệt nhánh `EXIT`. Người dùng xác nhận "Hành động này không thể hoàn tác" rồi tin rằng chứng từ đã bị xoá, trong khi đơn vẫn nguyên trong DB. Backend cũng không có endpoint xoá/huỷ hoá đơn.
→ Ẩn nút, hoặc làm endpoint huỷ (CANCELLED) có bù trừ kho/công nợ.

**B8 🟠 Chiết khấu/Thuế vẫn nhập được nhưng không được gửi lên backend.**
`SalesOrderForm.tsx:101-102` giữ state, `:192` tính `finalAmount = total - discount + tax`, `:327-330` truyền cả `setDiscount`/`setTax` xuống form (ô nhập chỉ bị khoá khi ở chế độ xem). Payload gửi đi không có 2 trường này, backend tính `totalAmount = Σ lineTotal` → số "Còn nợ" trên form và bản in lệch với công nợ thực ghi. Sprint 39 tuyên bố đã vô hiệu hoá nhưng **code chưa làm**.

**B9 🟡 Không chọn được hình thức thanh toán.**
`SalesOrderForm.tsx:86`: `const [paymentMethod] = useState(initialData?.paymentMethod || 'CASH')` — không có setter, không có UI chọn → mọi đơn đều `CASH`, dù DTO backend coi đây là trường bắt buộc và có đủ `CASH/CREDIT/MIXED`.

**B10 🟡 Cổng chất lượng TypeScript đang tắt.**
`tsconfig.json` của app Vite lại là **cấu hình Next.js** (`plugins:[{name:"next"}]`, include `next-env.d.ts`, `.next/types/**`), thiếu `types: ["vite/client"]`; `package.json` build bằng `vite build` (không type-check). Hệ quả: lỗi kiểu thật vẫn nằm im, ví dụ `SalesOrderForm.tsx:353` dùng `customer.customerId` và `:358` `customer.contactPerson` — hai field **không tồn tại** trong `CustomerResponseDto`; `SearchModal`/`SearchableCombobox` bị ép kiểu không khớp ở `:449`, `:476`. Toàn repo có ~83 chỗ `any`.
→ Thêm script `typecheck: tsc --noEmit` và chạy trong lint/CI; viết lại tsconfig cho Vite.

**B11 🟡 Không có phân trang, lọc bằng bộ nhớ.**
Không có `Pageable`/`Page<>` ở bất kỳ đâu. `CustomerReadController:28` tải toàn bộ khách rồi `filter` trong Java cho mỗi lần gõ tìm kiếm; tương tự `ProductReadController:36`, `ProductReader:33/52`, `StockService:27`, `ReceivableDebtController:38`, `BranchService:55`.
→ Chuyển sang query có `WHERE` + `Pageable` trước khi dữ liệu thật lớn.

**B12 🟡 Chính sách retry không nhất quán.** `SalesInvoiceService` retry cả `ObjectOptimisticLockingFailureException` lẫn `DataIntegrityViolationException`; `GoodsReturnService:74` chỉ retry loại đầu → cùng một tranh chấp ghi lại cho kết quả khác nhau giữa hai nghiệp vụ.

**B13 🟡 Tạo `stock_on_hand` lần đầu có thể va unique constraint.** `InventoryFacadeImpl:67-68` dùng `orElseGet(save(new StockOnHand(...)))`; hai request song song cho sản phẩm chưa có dòng tồn kho sẽ cùng insert. Bán hàng có retry nên qua được, trả hàng thì không (B12).

**B14 🟡 Thông báo trùng lặp.** Mutation vừa `notify.success` trong hook (`useCustomers.ts:17,25,33`, `useProducts.ts`, `useSalesInvoice.ts`), vừa có toast lỗi toàn cục trong `axiosInstance`, vừa có `toast.error` trong component → một hành động có thể bắn 2 toast.

---

## C. Schema ↔ code ↔ API contract

**C1 ✅ Entity ↔ migration: không lệch.** Script đối chiếu toàn bộ `@Entity` với schema dựng lại từ V1→V20 (29 bảng) không tìm thấy cột nào entity có mà DB thiếu. Hai cảnh báo ban đầu là dương tính giả: `receivable_debt_movement.idempotency_key` được thêm ở `V18` (ALTER nhiều cột trong một câu lệnh), `role_permission` dùng `@EmbeddedId`. Ngoài ra `ddl-auto: validate` ở cả hq/branch là lưới an toàn tốt.

**C2 ✅ Controller ↔ `openapi.json`: khớp 47/47 endpoint** (cả hai chiều). URL frontend cũng khớp hết với route backend.

**C3 🟠 `openapi.json` là ảnh chụp thủ công.** `packages/api-contract` sinh type từ file tĩnh (`npm run generate` đọc `openapi.json`), nhưng không có bước nào bảo đảm file đó được xuất lại từ backend sau khi sửa DTO. Contract đang khớp là do may, không phải do quy trình.
→ Thêm bước CI: chạy backend (hoặc `springdoc` ở test) xuất `/v3/api-docs`, so sánh với file trong repo, khác thì fail.

**C4 🟡 Hai DTO trùng tên ở hai package:** `InboundReceiptResponseDto` tồn tại ở cả `inventory/api/dto` và `inventory/application/dto` → dễ import nhầm, và công cụ sinh contract cũng khó phân biệt.

**C5 🟡 Hai nguồn type ở frontend:** vừa dùng `@erp/api-contract` (sinh tự động) vừa có `src/types/*.ts` viết tay (`catalog.ts`, `sales.ts`, `api/customer.ts`) cho cùng khái niệm → chính là nơi phát sinh lỗi kiểu ở B10.

---

## D. Cấu trúc & bảo trì

**D1 🟡 17 file script rác đã commit** ở `apps/erp-frontend/`: `apply_classic_erp.js`, `apply_footer.js`, `apply_ux_ui*.js`, `build_ribbon.js`, `compare.js`, `fix_*.js`, `rebuild_final_form.js`, `repair.js`, `repair2.js`, `ui_ux_promax.js`, `test.js`. Đây là script sinh code một lần, không thuộc sản phẩm.

**D2 🟡 Component trùng:** `src/layout/MdiModuleLayout.tsx` và `src/components/layout/MdiModuleLayout.tsx` — chỉ bản trong `components/` được import.

**D3 🟡 `shared/components/RoleGuard.tsx` không được dùng ở đâu**; phân quyền UI đang rải rác bằng `user.role === ...` và `menuConfig.roles`.

**D4 🟡 Lạm dụng tên class đầy đủ** (`com.storename.erp.…`) ngay trong thân hàm ở `SalesInvoiceService`, `GoodsReturnService`, `ReceivableDebtController` thay vì import — trái tinh thần "code sạch, dễ đọc" của `.agents/AGENTS.md`.

**D5 🟡 Thiếu `.gitattributes`** → ~300 file luôn hiện "modified" vì CRLF/LF, làm loãng mọi diff và dễ vô tình commit nhầm.

---

## E. Build, test, CI

**E1 🔴 Test không chạy được trên máy sạch.**
`application-test.yml` trỏ `jwt.private-key: classpath:certs/private_key.pem`. File `src/test/resources/certs/private_key.pem` **có trên máy nhưng không được commit** (`git ls-files src/test/resources` chỉ trả về `application-postgres-it.yml` và `replication-e2e/docker-compose.yml`). `JwtTokenProvider` lặng lẽ để `privateKey = null` khi file không tồn tại, nên mọi test gọi `generateToken` (vd. `InboundReceiptIdempotencyTest:77`, `SupplierBranchScopeTest:61`, `ArchitectureV3BranchTest:68`) sẽ ném `SigningKeyNotConfiguredException` sau khi clone mới.
→ Commit một cặp khoá **chỉ dùng cho test**, hoặc sinh khoá trong `@BeforeAll`.

**E2 🟠 Chưa có CI.** `sprint-35` chỉ "lên kế hoạch"; repo không có workflow nào. Không có gì chặn commit làm vỡ test hay type-check.

**E3 🟠 Cả hai cổng chất lượng frontend đều không chặn:** `vite build` không type-check (B10) và `npm run lint` đã hạ `rules-of-hooks`, `no-explicit-any`, `no-unused-vars`, `no-empty` xuống `warn`, `set-state-in-effect`/`immutability` xuống `off` (Sprint 36 làm vậy để build không đứt).

**E4 🟡 Test chạy H2, production chạy Postgres.** Phần lớn test dùng `ddl-auto: create-drop` + `flyway.enabled=false`, nên migration thật, kiểu dữ liệu Postgres, `PESSIMISTIC_WRITE`, và `gen_random_uuid()` gần như không được phủ. Có bộ `*IT` dùng Testcontainers nhưng `ReplicationE2ETest` đang `@Disabled`.

**E5 🟡 Không đo coverage**; test dày ở sales/công nợ, mỏng ở catalog/branch/analytics/replication.

---

## F. Docs lệch code (ưu tiên code — docs cần sửa theo)

| # | Tài liệu | Nội dung sai | Thực tế trong code |
|---|---|---|---|
| F1 | `architecture/SECURITY_MODEL.md:17` | `sub / subject = username` | `sub = user_account.public_id` (UUID), từ migration `V19` |
| F2 | `architecture/ARCHITECTURE_DECISIONS.md:73` (ADR-07), `SYSTEM_ARCHITECTURE.md:39`, `API_CONTRACT.md:54`, `BUSINESS_ANALYSIS*.md:138` | Đơn bán hàng theo 2 bước Draft → Confirm | `POST /api/v1/sales-invoices` tạo thẳng `CONFIRMED` trong 1 transaction (Sprint 37). Inbound & Goods Return vẫn 2 bước — docs cần tách bạch |
| F3 | `SECURITY_MODEL.md:78-79`, `DATABASE_REPLICATION.md:48` | `@ConditionalOnProperty(name="instance.role", havingValue="BRANCH")` | Code dùng `@ConditionalOnExpression("… == 'BRANCH' or … == 'ALL'")` — `ALL` bật cả hai phía |
| F4 | `sprints/sprint-39-…md` mục 2 | "Đã vô hiệu hoá ô chiết khấu/thuế" | Chưa làm (B8) |
| F5 | `architecture/DATA_SCHEMA.md:17` | Mô tả `user_account`, `customer` theo schema cũ | Thiếu `public_id` (V19); thiếu quy ước `customer.branch_id IS NULL` = khách dùng chung (V20) |
| F6 | `development/RUNTIME_CONFIG.md` | Chỉ mô tả cổng 8080/DB | Chưa có mô hình cổng 80 (HQ/admin) và 81/82 (chi nhánh), cũng chưa nói STAFF bắt buộc đăng nhập đúng cổng |
| F7 | `architecture/API_CONTRACT.md:54` | `POST /api/v1/sales-invoices` = "Draft Invoice" | Tạo và xác nhận luôn |

---

## Đã kiểm tra, không phát hiện vấn đề

1. **Entity ↔ migration** (29 bảng) và **controller ↔ openapi.json** (47 endpoint) đều khớp.
2. **Không có SQL nối chuỗi**: `AnalyticsDataAdapter` dùng `NamedParameterJdbcTemplate` với tham số đặt tên; repository dùng JPQL có `@Param`.
3. **Không có `catch` rỗng** nuốt lỗi; không có `@Transactional` trên method `private`/`protected`; không có self-invocation làm mất transaction.
4. **Ranh giới transaction ở nghiệp vụ tiền/kho là đúng**: `createAndConfirm` gói trọn tạo hoá đơn + trừ kho + ghi công nợ trong một `@Transactional`; `FifoCostService.consume` dùng `Propagation.MANDATORY` để không tự mở transaction riêng; có `PESSIMISTIC_WRITE` ở `CostLayerRepository` và `SalesInvoiceRepository.findByIdForUpdate`.
5. **Sổ công nợ append-only có khoá idempotency** (`ReceivableDebtService.recordMovement` + unique constraint `uk_debt_movement_idempotency` ở V18) — chống ghi trùng tốt.
6. **Tách module theo bounded context** (`order`, `inventory`, `crm`, `catalog`, `identity`, `analytics`, `branch`) với Facade giữa các domain được tuân thủ; có `ArchitectureV3*Test` kiểm tra ranh giới.

---

## Thứ tự xử lý đề xuất

1. **Chặn chảy máu**: A1 (rotate + gỡ key khỏi git), E1 (khoá test), B1, B2.
2. **Sửa lỗi người dùng chạm phải**: B7, B8, B9, B4.
3. **Siết cổng chất lượng**: E3 (bật `tsc --noEmit`, trả `rules-of-hooks` về error) → xử lý B6, B10 lộ ra sau đó; rồi E2 (CI) và C3 (kiểm tra contract trong CI).
4. **Củng cố nền**: A2, A3, A4, B3, B5, B11.
5. **Dọn dẹp & tài liệu**: D1–D5, và cập nhật F1–F7 theo code.
