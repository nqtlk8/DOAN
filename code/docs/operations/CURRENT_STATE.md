# Current State — v5

## 1. Đã chốt

- Modular Monolith.
- N-instance HQ/Branch.
- PostgreSQL riêng cho HQ và từng Branch.
- JWT RS256.
- HQ-only token issuance.
- React/Vite ERP UI.
- Sales Invoice: Draft -> Confirm.
- Goods Return.
- Inbound Receipt: Draft -> Confirm.
- Stock On Hand.
- Receivable Debt.
- Customer / Catalog / Branch management.
- Idempotency.
- Optimistic locking.
- Flyway migrations.
- Docker deployment.

## 2. Đã loại bỏ khỏi phạm vi hiện hành

- Procurement module.
- Customer Order.
- Purchase Order.
- Payable Debt.
- Stock Transfer.
- Outbound Receipt.
- RFQ.

## 3. Các điểm cần xử lý trong implementation

### Security

`SecurityConfig` đã được bổ sung `@EnableMethodSecurity` để `@PreAuthorize` hoạt động đúng.

### Authority mismatch

Đã chuẩn hóa, JWT trực tiếp ánh xạ sang Authority (không còn `ROLE_`).

### Customer master write

`CustomerController` đã được tách thành `CustomerWriteController` (HQ-only) và `CustomerReadController` (HQ & Branch).

### User ID handling

Một số controller hiện lấy user ID không nhất quán; cần bổ sung claim user ID vào JWT hoặc thống nhất nguồn định danh.

### Documentation drift

Các tài liệu sprint cũ vẫn có mô tả tính năng đã bị loại bỏ. Không dùng chúng làm context hiện hành.

### Inventory README drift

`inventory/README.md` vẫn nhắc Outbound Receipt và một số rule cũ. Tài liệu này không phải source of truth cho v5.

### Replication documentation

`DATABASE_REPLICATION.md` cũ mô tả PoC với bảng `products`; đây là ví dụ lịch sử, không phải schema v5.
