---
name: test-plan
description: Hướng dẫn viết test case đầy đủ cho Backend và rút gọn cho Frontend sau khi dev tính năng mới.
---

# Skill: Viết Test Case Sau Khi Dev (Backend đầy đủ / Frontend rút gọn)

## Tóm tắt

Skill này được dùng **ngay sau khi một tính năng/API/component mới được code
xong**, trước khi coi tính năng đó là "done". Mục tiêu khác nhau rõ rệt giữa
2 phía, KHÔNG áp dụng cùng một tiêu chuẩn cho cả hai:

- **Backend (quan trọng, phải đầy đủ):** đây là nơi chứa toàn bộ business
  logic có rủi ro thật — FIFO costing, branch scope/write-ownership,
  JWT RS256 phi tập trung, idempotency, replication. Một lỗi ở đây có thể
  làm sai số liệu tồn kho/công nợ mà không ai nhận ra ngay. Vì vậy BE test
  phải đi đúng 4 tầng (Unit → Slice → Integration → E2E) theo quyết định
  rủi ro, không được bỏ tầng.
- **Frontend (đơn giản, chỉ cần chức năng chính chạy được):** UI trong dự án
  này không chứa business logic quan trọng (logic thật nằm ở BE), nên FE
  test chỉ cần xác nhận **1 luồng chính (happy path)** hoạt động đúng từ
  đầu đến cuối. KHÔNG cần edge case, KHÔNG cần coverage cao, KHÔNG cần test
  lỗi mạng/lỗi input/lỗi từng field trừ khi được yêu cầu riêng.

## Quy trình tổng (entry point mỗi khi có code mới)

1. Xác định phạm vi thay đổi vừa code xong:
   - Chỉ Backend (Service/Repository/Controller/Domain trong
     `code/erp-platform/services/erp-backend`) → làm theo **PHẦN A**.
   - Chỉ Frontend (component/hook/page trong
     `code/erp-platform/apps/erp-frontend`) → làm theo **PHẦN B**.
   - Tính năng full-stack (có cả API mới lẫn UI mới, ví dụ thêm hẳn một
     module nghiệp vụ) → làm **PHẦN A trước, PHẦN B sau**. Không đảo ngược,
     vì test FE dựa vào contract API đã ổn định ở BE (nếu API còn đổi,
     mock ở FE sẽ phải viết lại).
2. Sau khi viết xong, chạy PHẦN C (quy trình tích hợp) để chốt "Done".

---

## PHẦN A — BACKEND TEST (bắt buộc đầy đủ theo tầng)

### A.1. Xác định đúng tầng trước khi viết (không mặc định chọn tầng cao)

Trước khi viết bất kỳ test nào, trả lời: **"Test này cần chạm bao nhiêu lớp
hạ tầng thật?"** Không dùng Testcontainers cho một hàm validate thuần —
chậm CI vô ích và làm feedback loop khi debug chậm lại không cần thiết.

| Câu hỏi | Nếu CÓ → Tầng | Vị trí | Công cụ |
|---|---|---|---|
| Chỉ logic thuần, không đụng Spring/DB/network? | **Unit** | `src/test/java/.../unit/` | JUnit 5 + Mockito, cấm `@SpringBootTest` |
| Cần test Repository/Query nhưng không cần Redis/Profile thật? | **Slice** | `src/test/java/.../slice/` | `@DataJpaTest`/`@WebMvcTest` + H2 |
| Cần verify `@Profile("hq")`/`@Profile("branch")`, Redis thật, hoặc behavior DB thật? | **Integration** | `src/test/java/.../integration/{hq,branch}/` | `@SpringBootTest` + Testcontainers |
| Cần verify HQ và Branch là 2 tiến trình độc lập nói chuyện qua mạng thật? | **E2E** | `src/test/java/.../e2e/` hoặc script riêng | Docker Compose thật + HTTP client |

Quy tắc bắt buộc: nếu phân vân giữa 2 tầng, **luôn chọn tầng thấp hơn
trước**; chỉ nâng lên khi tầng thấp không thể verify đúng hành vi (và phải
ghi lý do trong comment đầu file test).

Quy trình viết test cho **1 tính năng mới** (theo đúng thứ tự, không nhảy
cóc thẳng lên Integration/E2E "cho chắc"):
1. Unit trước — logic nghiệp vụ thuần (Service method, validator), mock hết
   dependency.
2. Slice nếu có Repository/Query mới — verify bằng H2.
3. Integration nếu tính năng đụng Redis, Bean theo Profile, hoặc Security
   Filter Chain thật.
4. E2E chỉ khi tính năng có hành vi xuyên instance (HQ ↔ Branch).

### A.2. Ba nhóm test đặc thù của dự án — không được thiếu

**a) FIFO Costing (trọng tâm nghiệp vụ số 1 của hệ thống)**

Mọi thay đổi chạm tới `FifoCostService`, `CostLayer`, `StockMovement`, hoặc
luồng Confirm Sales Invoice/Inbound Receipt/Goods Return, bắt buộc có Unit
Test cho các case sau (tham khảo file đã có `application/FifoCostServiceTest.java`
— **lưu ý:** file này hiện nằm ở `application/` thay vì `unit/`, không đúng
cấu trúc thư mục chuẩn ở mục A.4; khi thêm test case mới cho class này, đặt
đúng vào `unit/`, không tiếp tục thêm vào file cũ nằm sai chỗ):
- Bán hết đúng 1 CostLayer (không tràn sang layer kế tiếp).
- Bán tràn qua nhiều CostLayer liên tiếp, kiểm tra thứ tự tiêu thụ đúng FIFO
  (layer cũ nhất bị consume trước) và giá trị `cost_basis` tính đúng theo
  trọng số từng layer.
- Bán vượt quá tổng `remainingQty` của mọi layer hiện có → `costBasis =
  NO_LAYER`, `unitCost = 0`, tồn kho vẫn giảm âm (không được throw
  exception chặn giao dịch — đây là business rule đã chốt, không phải bug).
- Goods Return tạo `CostLayer` mới với `costBasis = RETURN` và `unitCost =
  returnPrice` truyền vào (không lấy lại giá vốn gốc của lần bán).
- Optimistic Locking: 2 luồng cùng tiêu thụ chung 1 `CostLayer` — verify có
  1 luồng bị `OptimisticLockException`/retry đúng như thiết kế `@Version`
  trên `CostLayer`.

**b) Branch/HQ Write Ownership (chống multi-writer conflict khi replicate)**

Bất kỳ Controller/API mới nào thuộc nhóm dữ liệu có phân biệt quyền ghi
theo instance (`@ConditionalOnProperty(name = "instance.role", ...)`), bắt
buộc có Integration Test theo đúng pattern đã có sẵn trong
`src/test/java/com/storename/erp/ReplicationOwnershipTest.java` và
`ReplicationOwnershipHqTest.java`:
- Verify bean Controller/Service **không tồn tại** trong context của phía
  không được ghi (`context.containsBean(...)` trả `false`) — không chỉ
  test "trả lỗi 403", vì đây là kiểm tra endpoint bị loại vật lý khỏi
  Spring Context, chặt hơn kiểm tra quyền runtime.
- Verify endpoint trả **404** (không tồn tại) ở phía không được ghi, và
  hoạt động đúng ở phía được ghi (trả 400/201 tùy input, chứng minh mapping
  tồn tại thật).
- Khi thêm bảng/entity mới vào nhóm Master Data hay Transaction Data (xem
  phân loại ở Chương 2 báo cáo — Global Master, Branch-scoped Master,
  Transaction/Operational), viết đúng 1 cặp test HQ/Branch tương tự để giữ
  bất biến "mỗi bảng chỉ một phía ghi hợp lệ".

**c) JWT / Auth (rủi ro bảo mật đã phân tích riêng)**

- Unit: token hợp lệ → `validateToken()`/`getClaimsFromToken()` đọc đúng
  claim; token hết hạn → xử lý đúng; token ký sai key (giả mạo) → bị từ
  chối; token sai format → không throw exception không kiểm soát.
- Integration: gọi `/auth/login` ở Branch phải không tồn tại/bị chặn (verify
  `@ConditionalOnProperty(instance.role=HQ)` trên `AuthController` chặn
  đúng, theo đúng cách `ReplicationOwnershipTest` đã làm cho Product); gọi
  API cần auth ở Branch bằng token ký sai chữ ký phải bị Filter chặn ở tầng
  HTTP (401), không lọt xuống Controller.
- Bất kỳ thay đổi nào ở `JwtTokenProvider`/`JwtAuthenticationFilter` liên
  quan tới claim `sub` (hiện đang là `user.getId().toString()`, dùng làm
  `userId` để ghi `performed_by` trên `StockMovement`) bắt buộc có test xác
  nhận claim `sub` parse được thành `UUID` hợp lệ — đây từng là điểm lệch
  giữa code và docs (README cũ nói `userId` bị mock ngẫu nhiên), nên cần
  test khóa chặt hành vi đúng lại để tránh regress về sau.

### A.3. Idempotency AOP (đặc thù HQ có Redis, Branch không có)

- HQ: Integration Test (Testcontainers Redis) — verify key idempotency
  lưu/đọc đúng qua Redis thật.
- Branch: Integration Test — verify cơ chế thay thế khi không có Redis
  (tham khảo `integration/branch/InboundReceiptIdempotencyTest.java` đã có
  sẵn làm mẫu khi viết test idempotency cho API mới ở Branch).
- Nếu 2 luồng trên cho kết quả khác nhau với cùng 1 kịch bản race-condition
  (2 request đồng thời cùng idempotency key), phải có test riêng ghi nhận
  rõ sự khác biệt — không được coi là "chấp nhận được" mà không ghi lại.

### A.4. Cấu trúc thư mục bắt buộc

```
code/erp-platform/services/erp-backend/src/test/java/com/storename/erp/<module>/
  ├── unit/            (Level 1 — logic thuần, mock hết dependency)
  ├── slice/           (Level 2 — @DataJpaTest/@WebMvcTest + H2)
  ├── integration/
  │     ├── hq/        (Level 3 — Testcontainers, profile hq)
  │     └── branch/    (Level 3 — Testcontainers, profile branch)
  └── e2e/             (Level 4 — Docker Compose thật, hoặc thư mục riêng)
```

- Hậu tố tên file phải phản ánh đúng tầng: `*UnitTest`, `*SliceTest` (hoặc
  chuẩn Spring `*RepositoryTest`), `*IntegrationTest`, `*E2ETest`.
- Không trộn assertion nhiều tầng trong cùng 1 class (không vừa mock
  Repository vừa dùng Testcontainers Postgres trong cùng file).
- Khi thấy file test cũ đặt sai thư mục (như `FifoCostServiceTest.java`
  hiện ở `application/`), không bắt buộc dọn ngay, nhưng file **mới** viết
  thêm phải đặt đúng chỗ theo cấu trúc trên.

### A.5. Ma trận bao phủ — bắt buộc cập nhật mỗi khi thêm tính năng

File `docs/testing/coverage-matrix.md`. Mỗi dòng mới:

| Tính năng / Module | Unit | Slice (H2) | Integration | E2E | Ghi chú |
|---|---|---|---|---|---|
| (ví dụ) FifoCostService.consume | ✅ | — | — | — | Logic thuần, không cần DB thật |
| (ví dụ) InboundReceiptController write-ownership | — | — | ✅ (404 ở HQ) | — | Theo pattern ReplicationOwnershipTest |

Ô để trống (`—`) phải có lý do ngầm hiểu được; nếu không, coi là **thiếu
test**, phải bổ sung hoặc ghi rõ lý do cố tình bỏ qua trong cột Ghi chú.

### A.6. Checklist bắt buộc trước khi coi 1 API/Service backend là "Done"

- [ ] Test đặt đúng tầng theo bảng quyết định A.1, không dùng
      `@SpringBootTest`/Testcontainers cho case Unit đủ giải quyết.
- [ ] Không dùng H2 để test hành vi đặc thù Postgres thật (JSON column,
      lock, transaction isolation...).
- [ ] Nếu tính năng chạm tới FIFO/CostLayer → đủ 5 case ở mục A.2.a.
- [ ] Nếu tính năng thêm API mới có phân biệt quyền ghi HQ/Branch → có cặp
      test write-ownership theo mục A.2.b.
- [ ] Nếu tính năng chạm JWT/Redis/Idempotency → đủ case theo A.2.c/A.3.
- [ ] Đã cập nhật `docs/testing/coverage-matrix.md`.
- [ ] `mvn test` chạy pass. Nếu môi trường không tải được Maven (lỗi mạng
      tới `repo.maven.apache.org`), ghi rõ điều này, **không được báo cáo
      "đã pass" khi thực tế chưa chạy được**.

---

## PHẦN B — FRONTEND TEST (rút gọn, chỉ cần chức năng chính chạy đúng)

### B.1. Nguyên tắc

- Dùng đúng 2 công cụ đã có sẵn trong repo, không thêm công cụ mới:
  **Playwright** (`code/erp-platform/apps/erp-frontend/tests/*.spec.ts`) là
  công cụ chính cho test luồng người dùng; **Vitest + Testing Library**
  (`src/**/*.test.tsx`) chỉ dùng khi cần test 1 hàm/hook thuần túy tách
  biệt khỏi UI (hiếm, không bắt buộc).
- Mỗi tính năng UI mới chỉ cần **1 luồng chính (happy path)**: người dùng
  thao tác đúng theo kịch bản bình thường nhất → xác nhận kết quả đúng.
  KHÔNG viết test cho: lỗi mạng, API trả 500, bỏ trống field, giá trị âm,
  loading state, session hết hạn... trừ khi được yêu cầu thêm rõ ràng.
- Số lượng test/tính năng: **1–3 test case là đủ**. Một feature nhỏ (ví dụ
  1 form CRUD đơn giản) thường chỉ cần đúng 1 test "tạo mới thành công, dữ
  liệu hiển thị đúng".
- Không đặt mục tiêu coverage %, không đo coverage FE trong CI.
- Mock API bằng `page.route('**/api/v1/...', ...)`, không gọi backend
  thật, không cần chạy `docker compose up` khi chạy test FE.
- Dùng `data-testid` đã có sẵn trên component; nếu component mới hoàn toàn
  chưa có testid, chỉ thêm đúng những testid cần thiết để assert (nút hành
  động chính, input chính, vùng hiển thị kết quả) — không thêm tràn lan.

### B.2. Lưu ý quan trọng về các file test hiện có

`tests/login.spec.ts`, `tests/sales-create.spec.ts`, `tests/sales-list.spec.ts`,
`tests/customer.spec.ts` hiện đang viết **chi tiết hơn nhiều** so với tiêu
chuẩn "đơn giản" ở trên (có test lỗi mạng, lỗi 500, validate từng field,
loading state...). Đây là các file **đã tồn tại trước tiêu chuẩn này**:
- Không xóa hay rút gọn các test case đã có trong 4 file này.
- Không tiếp tục thêm test edge case mới vào các file này trừ khi được yêu
  cầu riêng.
- Từ nay, tiêu chuẩn "đơn giản, chỉ happy path" áp dụng cho **tính năng
  UI mới** (ví dụ khi vừa thêm xong `InboundReceiptModule.tsx`, chỉ cần 1
  file `tests/inbound-receipt.spec.ts` với 1 test case chính: tạo phiếu
  nhập → thêm 1 dòng sản phẩm → lưu nháp → xác nhận → kiểm tra phiếu
  chuyển trạng thái đúng — không cần thêm test cho validate số lượng âm,
  API lỗi, v.v. như cách `sales-create.spec.ts` đã làm).

### B.3. Mẫu cấu trúc 1 test đơn giản (tham khảo cách mock đã dùng trong repo)

```ts
import { test, expect } from '@playwright/test';

// Mock tối thiểu cần cho 1 happy path — không cần mock mọi field
const mockStaffResponse = { /* ...giữ tối giản, copy phần cần thiết từ
  sales-create.spec.ts, không cần đầy đủ như file gốc nếu tính năng mới
  không dùng tới field đó */ };

test.describe('<Ten tinh nang> flow', () => {
  test.beforeEach(async ({ page }) => {
    // Mock API cần thiết + mock session đăng nhập, y hệt cách
    // sales-create.spec.ts đã làm trong beforeEach
  });

  test('TC-<MODULE>-01: <Luồng chính hoạt động đúng>', async ({ page }) => {
    // 1 kịch bản duy nhất, từ thao tác đầu tới khi thấy kết quả đúng
    // Không cần chia nhiều test nhỏ cho cùng 1 luồng
  });
});
```

### B.4. Checklist trước khi coi 1 tính năng UI mới là "Done"

- [ ] Đã xác định đúng 1 luồng chính (happy path) là gì.
- [ ] Có `tests/<feature>.spec.ts` cover luồng đó từ đầu đến cuối.
- [ ] API được mock bằng `page.route`, không gọi network thật.
- [ ] Không thêm test lỗi/edge case trừ khi được yêu cầu riêng.
- [ ] `npx playwright test tests/<feature>.spec.ts` pass.
- [ ] Không cần cập nhật `docs/testing/coverage-matrix.md` (file đó chỉ
      dành cho Backend).

---

## PHẦN C — QUY TRÌNH TÍCH HỢP (áp dụng sau khi viết xong test)

1. Dev code xong tính năng → xác định phạm vi (BE/FE/cả hai) theo mục
   "Quy trình tổng" ở đầu skill.
2. Viết test theo đúng Phần A và/hoặc Phần B tương ứng.
3. Chạy test thật, xác nhận pass trước khi báo "xong". Nếu không chạy được
   do hạn chế môi trường (không tải được Maven, chưa cài Playwright
   browser...), ghi rõ lý do, không giả định đã pass.
4. BE: cập nhật `docs/testing/coverage-matrix.md`. FE: bỏ qua bước này.
5. Nếu tính năng có liên kết tới một sprint doc (`docs/sprints/sprint-X.md`)
   hoặc một rủi ro/nợ kỹ thuật đã ghi trong docs kiến trúc (ví dụ các mục
   đã liệt kê trong `docs/FOUND_ISSUES.md`), và rủi ro đó kiểm chứng được
   bằng test, phải có ít nhất 1 test verify hành vi hiện tại — kể cả khi
   hành vi đó "chưa hoàn hảo" (ví dụ: Branch không tức thời nhận revoke
   token) — để nếu sau này ai sửa mà vô tình đổi hành vi, test báo đỏ thay
   vì âm thầm trôi qua.
6. Đánh dấu tính năng "Done" chỉ khi cả BE (nếu có) và FE (nếu có) đều đạt
   checklist tương ứng ở A.6/B.4.

## Khi nào KHÔNG cần viết test mới (tránh over-engineering)

- Sửa lỗi chính tả, đổi tên biến nội bộ không đổi hành vi, format lại code.
- Cập nhật comment/docs/README (như các việc dọn dẹp `inventory/README.md`).
- Đổi style CSS/Tailwind thuần túy không đổi logic/hành vi component.
- Thay đổi chỉ ảnh hưởng tới `AnalyticsEtlJob`/scaffold đang ghi rõ là chưa
  hiện thực — chỉ cần test khi logic ETL thật sự được viết.