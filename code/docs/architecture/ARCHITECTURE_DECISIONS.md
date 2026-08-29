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

## ADR-06 — Weighted Average Costing

### Bối cảnh
Tồn kho cần có giá vốn bình quân để phục vụ bán hàng và nhập hàng.

### Quyết định
Sử dụng strategy `WeightedAverageCostingStrategy` làm chiến lược hiện hành.

### Trade-off
Logic FIFO chưa phải chiến lược đang dùng chính thức trong runtime hiện tại.

## ADR-07 — Draft → Confirm

### Bối cảnh
Chứng từ cần được nhập trước khi tạo tác động nghiệp vụ.

### Quyết định
Sales Invoice và Inbound Receipt dùng hai giai đoạn Draft và Confirm.

### Trade-off
Luồng dài hơn thao tác một bước nhưng giúp kiểm soát transaction và rollback tốt hơn.
