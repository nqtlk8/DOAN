# Architecture Decisions - v5

## ADR-01 - Modular Monolith

### Context
Hệ thống có nhiều nghiệp vụ liên quan chặt chẽ và quy mô chưa yêu cầu scale độc lập từng module.

### Decision
Sử dụng một Spring Boot application với các module nghiệp vụ được phân tách ranh giới rõ ràng.

### Trade-off
Giảm độ phức tạp vận hành nhưng không scale từng module độc lập như Microservices.

## ADR-02 - N-instance HQ/Branch

### Context
Chi nhánh phải có khả năng xử lý nghiệp vụ cục bộ và sở hữu database riêng.

### Decision
Triển khai một artifact thành HQ và nhiều Branch instances. Phân biệt bằng config `instance.role`.

### Trade-off
Tăng chi phí triển khai, backup và giám sát theo số lượng branch.

## ADR-03 - PostgreSQL Logical Replication

### Context
HQ và Branch có database độc lập nhưng cần trao đổi dữ liệu.

### Decision
Sử dụng PostgreSQL Logical Replication cho mô hình Hub-and-Spoke.
- Master data (HQ) -> Branch.
- Transaction data (Branch) -> HQ.

### Trade-off
Chấp nhận eventual consistency và tăng độ phức tạp vận hành database.

## ADR-04 - JWT RS256

### Context
Branch cần verify token mà không gọi HQ trong mọi request và không nên sở hữu private signing key.

### Decision
HQ ký JWT bằng private key; Branch verify bằng public key.

### Trade-off
Revocation trên Branch không tức thời nếu Branch không gọi HQ.

## ADR-05 - React/Vite cho ERP UI

### Context
ERP UI không yêu cầu SEO/SSR.

### Decision
Sử dụng React/Vite SPA cho ERP frontend.

### Trade-off
Không có các cơ chế SSR/SSG nhưng giảm độ phức tạp runtime.

## ADR-06 - FIFO Costing

### Context
Cần tính giá vốn chính xác cho từng mặt hàng bán ra.

### Decision
* Sử dụng FIFO thông qua `CostLayer`.
* `StockMovement` làm inventory ledger/history.
* Lưu snapshot `cost_basis` trên `SalesInvoiceLine`.

### Trade-off
Tính toán phức tạp hơn do phải maintain state của nhiều CostLayer nhưng cho phép xác định chính xác lợi nhuận gộp theo từng lô hàng.

## ADR-07 - Draft & Confirm

### Context
Chứng từ cần được nhập trước khi tạo tác động nghiệp vụ (trừ tồn kho, nợ).

### Decision
Sales Invoice, Inbound Receipt, Goods Return dùng hai giai đoạn Draft (lưu nháp) và Confirm (xác nhận để cập nhật số dư).

### Trade-off
Luồng dài hơn thao tác một bước nhưng giúp kiểm soát transaction và rollback tốt hơn.

## ADR-08 - API Facade & Data Transfer Objects (DTO) Enforcement

### Context
Controller không được trả thẳng Domain Entity để tránh rò rỉ (domain leakage) cấu trúc nội bộ ra ngoài API.

### Decision
1. Controller bắt buộc phải mapping Entity sang DTO (VD: `SalesInvoiceResponseDto`, `GoodsReturnResponseDto`) trước khi serialize.
2. Frontend mọi lệnh gọi HTTP bắt buộc thực hiện thông qua `ApiService`.

### Trade-off
Tốn effort viết class DTO và mapping, nhưng đảm bảo decouple UI khỏi core Domain Model.

## ADR-09 - Facade Pattern for Inter-Module Communication

### Context
Ngăn chặn các module (`order`, `crm`, `catalog`, vv.) inject chéo `Repository` của nhau, dẫn đến tightly-coupled và vi phạm Bounded Context.

### Decision
Sử dụng các Facade Interface (`CrmFacade`, `OrderFacade`, `CatalogFacade`, `BranchFacade`) để giao tiếp giữa các module. Cấm tuyệt đối import `com.storename.erp.*.infrastructure.*Repository` vào một module khác.

### Trade-off
Phải tạo thêm các Interface và Impl trung gian, tuy nhiên giúp dễ dàng tách thành Microservices trong tương lai và dọn sạch các truy vấn N+1 thông qua Batch Fetching.

## ADR-10 - Bounded Context Data Isolation (No Cross-Module @ManyToOne)

### Context
Tránh tình trạng Entity của Order module (`GoodsReturn`) mapping trực tiếp sang Entity của CRM module (`Customer`), khiến các context bị rò rỉ dữ liệu (Domain Leakage) ở cấp độ JPA.

### Decision
Lưu trữ tham chiếu ngoại thông qua Native Types (`UUID customerId`, `Long branchId`) thay vì Object Reference (`@ManyToOne Customer customer`). Giao việc lấy tên hoặc chi tiết cho các Facade.

### Trade-off
Mất tính năng join ngầm định của JPA, buộc phải fetch riêng biệt và handle việc merge dữ liệu ở cấp độ Service/Controller, nhưng tối ưu được bộ nhớ và tuân thủ tuyệt đối DDD.

## ADR-11 - Signed Ledger Convention for Receivable Debt

### Context
Hệ thống tính toán công nợ cần một công thức chuẩn duy nhất, tránh tình trạng mỗi hàm lại có công thức cộng/trừ khác nhau.

### Decision
Áp dụng cơ chế Dấu (Signed Amount) cho `ReceivableDebtMovement`:
- Phát sinh nợ (`SALE`): + Amount
- Khách trả tiền (`PAYMENT`) hoặc trả hàng (`RETURN`): - Amount
=> Công thức chuẩn: `SUM(amount) = current_debt`.

### Trade-off
Trái ngược với sổ kế toán kép truyền thống (Debit/Credit), nhưng đơn giản hoá việc truy vấn SQL và tự động bù trừ.

## ADR-12 - Strict Idempotency

### Context
Môi trường mạng không ổn định, frontend có thể gửi double-click, gây trùng lặp phiếu.

### Decision
Áp dụng Annotation `@IdempotencyProtected` cùng `Idempotency-Key` header cho tất cả API thay đổi trạng thái (POST/PUT). Key được lưu vào bảng `idempotency_record`.

### Trade-off
Tăng thêm một round-trip vào database để kiểm tra trạng thái key trước khi thực thi nghiệp vụ, đổi lấy sự an toàn 100%.

## ADR-13 - Replicate bảng snapshot có row filter (thay thế quyết định Sprint 6)

### Context
Sprint 6 quyết định KHÔNG replicate `stock_on_hand` và `receivable_debt` lên HQ; HQ tự tính động từ `stock_movement` và `sales_invoice.remaining_debt`. Thực tế, công nợ theo chi nhánh không tính đúng được từ `sales_invoice` vì còn số dư đầu kỳ, thu nợ và hàng trả (các khoản này ghi vào `receivable_debt` / `receivable_debt_movement` ở chi nhánh). Trong khi đó `AnalyticsDataAdapter` khi lọc theo chi nhánh lại đọc 2 bảng này ở HQ — vốn rỗng hoặc chỉ có dữ liệu seed cũ.

Thêm một ràng buộc: seed V2/V13 chèn dữ liệu `branch_id = 1` (cùng UUID) vào DB của MỌI instance, kể cả TP2. Nếu replicate nguyên bảng, TP2 sẽ đẩy bản sao dòng của TP1 lên HQ → hai writer cho cùng một khóa, vi phạm Single-Writer Invariant.

### Decision
- Mỗi chi nhánh publish thêm `stock_on_hand` và `receivable_debt` trong `pub_<branch>_to_hq`, **có row filter** `WHERE (branch_id = N)` với N là `branch.id` của chính chi nhánh đó (lấy từ DB, không hard-code).
- Migration `V21` đặt `REPLICA IDENTITY USING INDEX` (`uk_stock_product_branch`, `uk_debt_customer_branch`) trên mọi DB. PostgreSQL yêu cầu cột dùng trong row filter phải thuộc replica identity khi publish UPDATE/DELETE; thiếu bước này, lệnh `UPDATE stock_on_hand` ở chi nhánh sẽ báo lỗi và làm hỏng luồng bán hàng.
- Bật cho chi nhánh đang chạy bằng `scripts/enable-snapshot-replication.sh <branch>`: tại HQ chỉ `DELETE ... WHERE branch_id = N` rồi `ALTER SUBSCRIPTION ... REFRESH PUBLICATION WITH (copy_data = true)`. Không `TRUNCATE`, vì sẽ xoá dữ liệu các chi nhánh khác đã đồng bộ. Chi nhánh mới: `setup-replication.sh` tự gọi script này.
- HQ không có API ghi 2 bảng này (controller giao dịch chỉ bật ở BRANCH/ALL; opening-balance từ chối khi không có branchId) — được khẳng định bằng `ReplicationOwnershipHqTest`.
- Cảnh báo tồn kho vẫn tính từ `stock_movement` (append-only); `stock_on_hand` ở HQ dùng cho màn tồn kho và đối soát.

### Rejected alternatives
- **Giữ quyết định Sprint 6 (tính động ở HQ):** bỏ, vì công nợ theo chi nhánh sai khi có số dư đầu kỳ/thu nợ/hàng trả, và `receivable_debt_movement` cũng không được replicate.
- **Replicate không lọc dòng:** bỏ, vì dữ liệu seed `branch_id = 1` có ở mọi DB chi nhánh (vi phạm Single-Writer).
- **Bảng riêng `stock_on_hand_hq` do job tổng hợp:** bỏ, vì thêm bảng, thêm job và thêm độ trễ mà không giải quyết được công nợ.

### Trade-off
- Cần PostgreSQL 15+ (docker-compose đang dùng `postgres:15`).
- Tăng lượng WAL do 2 bảng snapshot được UPDATE thường xuyên; chấp nhận ở quy mô hiện tại.
- Dữ liệu ở HQ là nhất quán cuối (eventual consistency) theo độ trễ replication.
- ⚠️ BREAKING so với Sprint 6: quy trình bật replication cho chi nhánh có thêm bước chạy `enable-snapshot-replication.sh`.
