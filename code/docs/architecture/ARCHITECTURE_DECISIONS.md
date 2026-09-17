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
