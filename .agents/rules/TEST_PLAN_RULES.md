# Test Plan Rules — Chiến lược Testing cho Project

Quy tắc bắt buộc khi viết test (cho AI Agent và người dev) trong project này.
Áp dụng cho kiến trúc N-Instance (HQ/Branch), Spring Boot, JWT RS256, Redis, Docker.

Mục tiêu: mỗi test được viết vào ĐÚNG tầng, dùng ĐÚNG công cụ (mock / H2 / Testcontainers),
không lãng phí thời gian chạy CI, và toàn bộ bộ test có thể tra cứu được độ bao phủ.

---

## PHẦN 1: PHÂN LOẠI TEST — XÁC ĐỊNH ĐÚNG TẦNG TRƯỚC KHI VIẾT

Trước khi viết bất kỳ test nào, PHẢI trả lời câu hỏi: **"Test này cần chạm vào bao
nhiêu lớp hạ tầng thật?"** Câu trả lời quyết định Level. KHÔNG được mặc định dùng
Level cao hơn mức cần thiết (ví dụ: dùng Testcontainers cho 1 hàm validate JWT
thuần logic là SAI — chậm CI vô ích).

### Level 1 — Unit Test (nhanh nhất, nhiều nhất)

**Dùng khi:** logic thuần Java/Kotlin, không cần Spring Context, không cần DB,
không cần network. Ví dụ: `JwtTokenProvider.validateToken()`, hàm tính toán,
validator, mapper, util class, business logic thuần trong Service (mock hết
repository/dependency).

**Công cụ:** JUnit 5 + Mockito. KHÔNG load Spring Context (`@SpringBootTest` bị
CẤM ở Level này).

**Tiêu chí:**
- Chạy xong toàn bộ trong < 1 giây / test.
- Không có annotation `@SpringBootTest`, `@DataJpaTest`, `@Testcontainers`.
- Mọi dependency ngoài (Repository, external client, Clock, Redis...) đều bị mock.

**Vị trí:** `src/test/java/.../unit/`

---

### Level 2 — Slice Test (Spring Context rút gọn, dùng H2)

**Dùng khi:** cần test tương tác thật với JPA/Repository/Query, nhưng KHÔNG cần
toàn bộ ứng dụng chạy lên (không cần Redis, không cần Nginx, không cần JWT
filter chain đầy đủ).

**Công cụ:** `@DataJpaTest` (hoặc `@WebMvcTest` cho Controller layer riêng) +
**H2 in-memory** (profile `test` riêng, KHÔNG dùng chung config với `hq`/`branch`).

**Tiêu chí:**
- Dùng khi test Repository (custom query, native query, mapping Entity).
- Dùng khi test riêng Controller (validate input, response status, không cần
  Service thật — mock Service).
- H2 chỉ mô phỏng tầng SQL cơ bản — KHÔNG dùng Level này để test bất kỳ điều gì
  liên quan tới:
  - Đặc thù cú pháp/behavior riêng của DB thật (nếu Postgres/MySQL) —
    ví dụ: JSON column, full-text search, lock cơ chế riêng của DB.
  - Redis, JWT filter chain thật, multi-instance behavior.
- Nếu Entity/Query dùng tính năng đặc thù DB thật → PHẢI chuyển sang Level 3
  (Testcontainers), không được "cho qua" bằng H2 vì H2 sẽ pass giả (false
  positive).

**Vị trí:** `src/test/java/.../slice/`

---

### Level 3 — Integration Test (Testcontainers, Spring Context đầy đủ)

**Dùng khi:** cần verify hành vi thật của hạ tầng: Postgres/MySQL thật, Redis
thật, hoặc cần load toàn bộ Spring Context với đúng Profile (`hq` / `branch`)
để verify các quyết định kiến trúc (VD: `RedisTemplate` có tồn tại ở `hq` và
KHÔNG tồn tại ở `branch`).

**Công cụ:** `@SpringBootTest` + **Testcontainers** (Postgres container, Redis
container tuỳ theo Profile đang test).

**Tiêu chí bắt buộc phải dùng Level này (không được hạ xuống H2):**
- Test liên quan tới `@Profile("hq")` / `@Profile("branch")` — verify Bean có
  tồn tại/không tồn tại đúng theo profile (VD: `ArchitectureV3HqTest`,
  `ArchitectureV3BranchTest`).
- Test liên quan tới Redis thật (session, cache, idempotency key thật).
- Test liên quan tới hành vi DB thật khác H2 (transaction isolation, lock,
  kiểu dữ liệu đặc thù).
- Test toàn bộ Security Filter Chain thật (JWT ký RS256 thật → gửi request →
  filter xác thực → trả response), không mock filter.

**Vị trí:** `src/test/java/.../integration/`

**Lưu ý hiệu năng:** Container nên dùng chung instance qua Singleton Container
Pattern (static container, reuse giữa các test class) để tránh spin-up lại
container mỗi test class, gây CI chậm.

---

### Level 4 — End-to-End / Cross-Instance Test (Docker Compose thật)

**Dùng khi:** cần verify hành vi XUYÊN NHIỀU INSTANCE thật — đây là đặc thù
riêng của kiến trúc Hub-and-Spoke (HQ phát token → Branch xác thực token đó
mà KHÔNG hỏi lại HQ). Level 3 (Testcontainers 1 Spring Context) KHÔNG thể
verify được kịch bản 2 instance độc lập nói chuyện qua Nginx.

**Dùng khi (cụ thể):**
- HQ login → nhận JWT → gọi API ở Branch (TP1/TP2) → xác nhận Branch chấp
  nhận token mà không cần gọi lại HQ (đúng thiết kế offline-first).
- Verify routing qua Nginx (port 80/81/82) đúng instance.
- Verify token bị revoke/hết hạn ở HQ có phản ánh đúng hành vi mong đợi ở Branch
  (bao gồm cả xác nhận rằng KHÔNG có đồng bộ real-time nếu thiết kế là vậy —
  đây là test bảo vệ cho quyết định kiến trúc đã ghi trong docs sprint).
- Smoke test toàn hệ thống trước khi release (docker-compose up 3 node thật).

**Công cụ:** Docker Compose (đúng file `docker-compose.yml` của project, KHÔNG
dùng bản rút gọn riêng cho test) + REST-assured / HTTP client trong test script,
hoặc test riêng chạy ngoài Maven/Gradle lifecycle (script bash/CI job riêng).

**Tiêu chí:**
- KHÔNG chạy ở mỗi lần build local thông thường (quá chậm) — chỉ chạy ở:
  CI trước khi merge vào nhánh chính, hoặc trước khi tag release.
- Phải có bước dọn dẹp (`docker-compose down -v`) đảm bảo môi trường sạch
  trước/sau khi chạy, tránh test sau bị ảnh hưởng bởi state cũ.

**Vị trí:** `src/test/java/.../e2e/` hoặc thư mục riêng `e2e-tests/` ngoài
module chính nếu dùng script độc lập.

---

### Bảng quyết định nhanh (Decision Matrix)

| Câu hỏi | Nếu CÓ → Level |
|---|---|
| Chỉ test logic thuần, không đụng Spring/DB/Network? | Level 1 — Unit |
| Cần test Repository/Query nhưng không cần Redis/Profile thật? | Level 2 — Slice (H2) |
| Cần verify `@Profile`, Redis thật, hoặc DB có behavior đặc thù? | Level 3 — Integration (Testcontainers) |
| Cần verify HQ và Branch là 2 tiến trình độc lập nói chuyện qua mạng thật? | Level 4 — E2E (Docker Compose) |

**Quy tắc bắt buộc:** Nếu phân vân giữa 2 Level, LUÔN CHỌN LEVEL THẤP HƠN trước,
chỉ nâng lên Level cao hơn khi Level thấp không thể verify được đúng hành vi
(và phải ghi rõ lý do trong docstring/comment đầu file test).

---

## PHẦN 2: KẾ HOẠCH CHI TIẾT THỰC HIỆN TỪNG LOẠI TEST

### 2.1. Cấu hình Profile riêng cho Test (bắt buộc)

- Phải có `application-test.yml` riêng (Level 1/2), KHÔNG được tái sử dụng
  `application-hq.yml` / `application-branch.yml` cho H2 — vì 2 file đó trỏ
  Redis/Postgres thật, dùng nhầm sẽ làm Slice Test không còn "nhẹ" nữa.
- Level 3/4 dùng lại đúng profile `hq`/`branch` thật, chỉ override
  `datasource.url` bằng Testcontainers JDBC URL tại runtime (qua
  `@DynamicPropertySource`), KHÔNG hardcode connection string trong file yml.

### 2.2. Quy trình viết test cho 1 tính năng mới (bắt buộc theo thứ tự)

Khi thêm 1 tính năng/Entity/API mới, PHẢI viết test theo đúng trình tự sau
(không được nhảy cóc thẳng lên Integration/E2E "cho chắc"):

1. **Unit trước:** viết Unit Test cho logic nghiệp vụ thuần (Service method,
   validator) — mock hết dependency.
2. **Slice nếu có Repository mới:** viết `@DataJpaTest` cho query mới, verify
   bằng H2.
3. **Integration nếu tính năng đụng tới:** Redis, Profile-specific Bean, hoặc
   Security Filter Chain thật.
4. **E2E chỉ khi:** tính năng có hành vi xuyên instance (HQ↔Branch) — ví dụ
   thêm 1 API mới ở Branch cần xác thực token do HQ phát hành.

Không được bỏ qua bước 1-2 để nhảy thẳng Level 3/4 — vì Level thấp cho feedback
loop nhanh hơn nhiều lần khi debug logic sai.

### 2.3. Test Idempotency AOP (đặc thù project — lưu ý riêng)

Vì HQ có Redis còn Branch KHÔNG có Redis, khung AOP Idempotency PHẢI có 2 bộ
test riêng biệt, không dùng chung:

- **HQ:** Integration Test (Level 3, Testcontainers Redis) — verify key
  idempotency lưu/đọc đúng qua Redis thật.
- **Branch:** Integration Test (Level 3, Testcontainers Postgres/DB local) —
  verify cơ chế thay thế (unique constraint DB / cơ chế khác đã chọn trong
  docs sprint) hoạt động đúng khi KHÔNG có Redis.
- Nếu 2 luồng trên đưa ra kết quả khác nhau cho cùng 1 kịch bản race-condition
  (VD: 2 request đồng thời cùng idempotency key), phải có test riêng ghi nhận
  rõ sự khác biệt này — KHÔNG được coi là "chấp nhận được" mà không ghi lại,
  vì đây là rủi ro đã nêu trong docs sprint.

### 2.4. Test Security/JWT (đặc thù project — lưu ý riêng)

Bắt buộc phải có các test case sau, không được thiếu (do bài toán RS256
phi tập trung có rủi ro bảo mật riêng đã phân tích ở phần review kiến trúc):

- Level 1 (Unit): token hợp lệ → `validateToken()` trả true; token hết hạn →
  trả false; token ký sai key (giả mạo) → trả false; token sai format → không
  throw exception không kiểm soát (phải catch đúng loại exception).
- Level 3 (Integration): gọi `/auth/login` ở Branch phải trả 404/403 (verify
  `@Profile("hq")` chặn đúng); gọi API cần auth ở Branch bằng token giả mạo
  chữ ký phải bị Filter chặn ở tầng HTTP (401), không lọt xuống Controller.
- Level 4 (E2E): token thật lấy từ HQ dùng gọi API ở TP1 và TP2 — xác nhận cả
  2 branch đều chấp nhận (verify Public Key đồng bộ đúng ở mọi node).

### 2.5. Tốc độ & Tần suất chạy (CI Strategy)

| Level | Chạy khi nào | Thời gian mục tiêu |
|---|---|---|
| Unit | Mỗi lần save/commit (watch mode local) | Toàn bộ < 30 giây |
| Slice (H2) | Mỗi lần push / mỗi PR | Toàn bộ < 2 phút |
| Integration (Testcontainers) | Mỗi PR trước khi merge | < 10 phút |
| E2E (Docker Compose) | Trước khi merge vào nhánh chính / trước release | Không giới hạn cứng, nhưng phải báo cáo thời gian chạy trong docs sprint |

Nếu 1 Level vượt quá thời gian mục tiêu đáng kể, phải xem lại: có test nào
đang bị đặt sai Level (dùng Testcontainers cho việc lẽ ra Unit làm được)
không, trước khi chấp nhận CI chậm là bình thường.

---

## PHẦN 3: TỔ CHỨC — ĐẢM BẢO CÓ HỆ THỐNG & THỂ HIỆN ĐỘ BAO PHỦ

### 3.1. Cấu trúc thư mục bắt buộc

```
src/test/java/
  └── <package>/
        ├── unit/            (Level 1)
        ├── slice/           (Level 2, H2)
        ├── integration/     (Level 3, Testcontainers)
        │     ├── hq/
        │     └── branch/
        └── e2e/             (Level 4, Docker Compose — hoặc thư mục riêng ngoài module)
```

- Tên file test PHẢI phản ánh đúng Level: hậu tố `*UnitTest`, `*SliceTest` (hoặc
  giữ chuẩn Spring `*RepositoryTest`), `*IntegrationTest`, `*E2ETest`.
- KHÔNG trộn lẫn assertion của nhiều Level trong cùng 1 class test (VD: không
  được vừa mock Repository vừa dùng Testcontainers Postgres trong cùng file).

### 3.2. Ma trận bao phủ (Test Coverage Matrix) — bắt buộc duy trì

Phải có file `docs/testing/coverage-matrix.md`, cập nhật mỗi khi thêm tính
năng mới. Format bảng:

| Tính năng / Module | Unit | Slice (H2) | Integration | E2E | Ghi chú |
|---|---|---|---|---|---|
| JwtTokenProvider.validateToken | ✅ | — | ✅ (filter chain) | ✅ (cross-instance) | |
| RedisConfig (Bean tồn tại đúng Profile) | — | — | ✅ | — | Không cần E2E vì đã verify đủ ở Integration |
| AuthController /auth/login chỉ tồn tại ở HQ | — | ✅ (WebMvcTest) | ✅ (403 ở Branch) | — | |
| Idempotency AOP - HQ (Redis) | ✅ (logic thuần) | — | ✅ | — | |
| Idempotency AOP - Branch (non-Redis) | ✅ (logic thuần) | — | ✅ | — | Cơ chế thay thế xem docs sprint 0.1 mục 5 |

- Cột nào để trống (`—`) PHẢI có lý do ngầm hiểu được (VD: tính năng đó không
  liên quan tới hạ tầng đó) — nếu để trống mà không rõ lý do, coi là **thiếu
  test**, phải bổ sung hoặc note rõ trong cột Ghi chú tại sao cố tình bỏ qua.

### 3.3. Liên kết với Docs Sprint (Phần 2 của bộ rule trước)

- Mỗi file docs sprint (`docs/sprints/sprint-X.md`, mục 7 "Cách chạy & Verify")
  PHẢI trỏ tới đúng test class/Level nào chứng minh sprint đó hoạt động đúng —
  không lặp lại nội dung, chỉ link tới `coverage-matrix.md` + tên class cụ thể.
- Nếu 1 rủi ro/nợ kỹ thuật được ghi trong docs sprint (mục 5), và rủi ro đó có
  thể kiểm chứng được bằng test, PHẢI có ít nhất 1 test (ở đúng Level) verify
  hành vi hiện tại — kể cả khi hành vi đó "chưa hoàn hảo", để nếu sau này ai
  sửa mà vô tình đổi hành vi, test sẽ báo đỏ thay vì âm thầm trôi qua.

### 3.4. Checklist bắt buộc trước khi merge PR có test mới

- [ ] Test được đặt đúng Level theo Bảng quyết định nhanh (Phần 1).
- [ ] Không có `@SpringBootTest`/`Testcontainers` dùng cho case lẽ ra Unit đủ.
- [ ] Không có H2 dùng để test hành vi đặc thù DB thật (Postgres/MySQL).
- [ ] Đã cập nhật `docs/testing/coverage-matrix.md`.
- [ ] Nếu tính năng liên quan JWT/Redis/Profile — đã có đủ test case theo
      mục 2.4/2.3 tương ứng.
- [ ] Nếu tính năng có hành vi xuyên instance (HQ↔Branch) — có ít nhất 1 test
      Level 3 hoặc Level 4 xác nhận, không chỉ dừng ở Unit.
