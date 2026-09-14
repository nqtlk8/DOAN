# Architecture Decisions — v5

## ADR-01 — Modular Monolith

### Bối cảnh
Hệ thống có nhiều nghiệp vụ liên quan chặt chẽ và quy mô chưa yêu cầu scale độc lập từng module.

### Quyết định
Sử dụng một Spring Boot application với các module nghiệp vụ.

### Trade-off
Giảm độ phức tạp vận hành nhưng không scale từng module độc lập như Microservices.

## ADR-02 — N-instance HQ/Branch

### Bối cảnh
Chi nhánh phải có khả năng xử lý nghiệp vụ cục bộ và sở hữu database riêng.

### Quyết định
Triển khai một artifact thành HQ và nhiều Branch instances.

### Trade-off
Tăng chi phí triển khai, backup và giám sát theo số lượng branch.

## ADR-03 — PostgreSQL Logical Replication

### Bối cảnh
HQ và Branch có database độc lập nhưng cần trao đổi dữ liệu.

### Quyết định
Sử dụng PostgreSQL Logical Replication cho mô hình Hub-and-Spoke.

### Trade-off
Chấp nhận eventual consistency và tăng độ phức tạp vận hành database.

## ADR-04 — JWT RS256

### Bối cảnh
Branch cần verify token mà không gọi HQ trong mỗi request và không nên sở hữu private signing key.

### Quyết định
HQ ký JWT bằng private key; Branch verify bằng public key.

### Trade-off
Revocation trên Branch không tức thời nếu Branch không gọi HQ.

## ADR-05 — React/Vite cho ERP UI

### Bối cảnh
ERP UI không yêu cầu SEO/SSR.

### Quyết định
Sử dụng React/Vite SPA cho ERP frontend.

### Trade-off
Không có các cơ chế SSR/SSG nhưng giảm độ phức tạp runtime.

## ADR-06 — FIFO Costing

Status: Accepted

Decision:
* FIFO
* CostLayer
* FifoCostService
* StockMovement là inventory ledger/history
* cost_basis snapshot trên SalesInvoiceLine

### Trade-off
Tính toán phức tạp hơn do phải maintain state của nhiều CostLayer nhưng cho phép xác định chính xác lợi nhuận gộp theo từng lô hàng.

## ADR-07 — Draft → Confirm

### Bối cảnh
Chứng từ cần được nhập trước khi tạo tác động nghiệp vụ.

### Quyết định
Sales Invoice và Inbound Receipt dùng hai giai đoạn Draft và Confirm.

### Trade-off
Luồng dài hơn thao tác một bước nhưng giúp kiểm soát transaction và rollback tốt hơn.

## ADR-08 - API Facade & Data Transfer Objects (DTO) Enforcement

### Bối cảnh
Quá trình Code Review phát hiện Controller đang trả thẳng Domain Entity (SalesInvoice, InboundReceipt) qua API, đồng thời UI gọi trực tiếp Axios thay vì qua API Facade.

### Quyết định
1. Controller bắt buộc phải mapping Entity sang DTO (VD: SalesInvoiceResponseDto) trước khi serialize.
2. Frontend mọi lệnh gọi HTTP bắt buộc thực hiện thông qua ApiService.ts.

### Trade-off
Tốn effort viết class DTO và mapping (bằng tay hoặc qua thư viện), nhưng đảm bảo decouple UI khỏi core Domain Model.
