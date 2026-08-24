# SKILL: SOLID, OOP & Cấu trúc 4-Layer

**Trigger:** Đọc skill này trước khi tạo bất kỳ class mới nào (Entity, Service,
Controller, Repository, DTO) — không có ngoại lệ.

---

## 1. Cấu trúc bắt buộc mỗi module

Mỗi module nghiệp vụ (`catalog`, `inventory`, `crm`, `order`, `procurement`,
`identity`, `branch`, `visualizer`, `analytics`) PHẢI có đúng 4 sub-package sau,
không được nhảy cóc layer:

```
<module>/
 ├── api/            → Controller, DTO request/response, Facade interface public
 ├── application/     → Service (nghiệp vụ), UseCase, Strategy implementations
 ├── domain/           → Entity, Value Object, Domain Event, Repository interface
 └── infrastructure/   → Repository implementation (JPA), mapper, external integration
```

**Quy tắc dòng chảy phụ thuộc (dependency direction):**
`api` → `application` → `domain` ← `infrastructure`

- `api` KHÔNG được gọi thẳng `domain`/`infrastructure`, luôn qua `application`.
- `domain` KHÔNG được phụ thuộc Spring/JPA nếu tránh được (Entity JPA là ngoại lệ
  chấp nhận được, nhưng business rule thuần phải nằm trong domain method, không
  rải rác ở Controller/Service).
- Module A chỉ gọi module B qua `B.api.XxxFacade` (interface public), KHÔNG BAO GIỜ
  import trực tiếp `Entity`/`Repository` nội bộ của module khác.

## 2. SOLID — áp dụng cụ thể, không phải lý thuyết suông

| Nguyên tắc | Quy tắc thực thi cụ thể |
|---|---|
| **S — Single Responsibility** | Controller CHỈ điều phối request/response, không chứa `if/else` nghiệp vụ. Business rule luôn nằm trong domain method hoặc Strategy class riêng. |
| **O — Open/Closed** | Bất kỳ nghiệp vụ nào có ≥ 2 biến thể xử lý khác nhau (VD: costing FIFO/bình quân gia quyền, thứ tự ưu tiên giá bán) BẮT BUỘC implement qua **Strategy Pattern** (1 interface + N implementation). CẤM viết `if (type == X) {...} else if (type == Y) {...}` cho logic nghiệp vụ có thể mở rộng thêm loại mới sau này. |
| **L — Liskov Substitution** | Mọi implementation của cùng 1 interface phải hoán đổi cho nhau mà không đổi hành vi phía gọi. |
| **I — Interface Segregation** | Tách interface theo nhu cầu người dùng thực tế (VD: `ProductReader` cho luồng đọc/public, `ProductWriter` cho luồng ghi/ERP) — KHÔNG gộp 1 interface khổng lồ rồi để implementation throw `UnsupportedOperationException` cho phần không cần.
| **D — Dependency Inversion** | Constructor injection BẮT BUỘC (dùng `@RequiredArgsConstructor` của Lombok trên `final` field). CẤM field injection (`@Autowired` trực tiếp trên field). |

## 3. Quy tắc Entity & DTO

- Entity JPA **KHÔNG BAO GIỜ** được trả trực tiếp ra khỏi `application`/`api` layer
  — luôn convert sang DTO qua MapStruct trước khi trả về Controller.
- Entity KHÔNG có setter public tuỳ tiện. Thay đổi trạng thái luôn qua domain
  method có validate, đặt tên theo nghiệp vụ (không đặt tên kiểu CRUD chung
  chung):
  - ĐÚNG: `stock.decreaseAllowNegative(qty, reason)`, `invoice.confirm(amountPaid, ...)`,
    `orderLine.fulfill(qty)`
  - SAI: `stock.setQuantity(x)`, `invoice.setStatus(CONFIRMED)`
- DTO Request/Response ưu tiên dùng Java `record` khi có thể (immutable).
- Domain Event dùng cho giao tiếp liên module (VD: `SalesInvoiceConfirmedEvent`,
  `StockDecreasedEvent`) — module khác lắng nghe qua Spring Event Listener, KHÔNG
  gọi trực tiếp Service của module phát sinh sự kiện.

## 4. Checklist tự kiểm tra (chạy trước khi coi 1 task là xong)

- [ ] Class/method có đúng 1 trách nhiệm không? (SRP)
- [ ] Có logic rẽ nhánh theo loại/kiểu nên tách Strategy không? (OCP)
- [ ] Có Entity JPA nào bị lộ trực tiếp ra API response không?
- [ ] Có setter tuỳ tiện nào thay vì domain method có validate không?
- [ ] Constructor injection đã dùng đúng, không có field injection nào không?
- [ ] Module này có import trực tiếp Entity/Repository của module khác không? (phải qua Facade)

## 5. Ví dụ tham chiếu đầy đủ

Xem ví dụ luồng request hoàn chỉnh (Controller → Facade → Service → Entity →
Repository interface → Repository implementation) trong phần giải thích kiến trúc
đã trao đổi — module `catalog`, entity `Product`. Áp dụng đúng khuôn mẫu này cho
mọi entity mới.
