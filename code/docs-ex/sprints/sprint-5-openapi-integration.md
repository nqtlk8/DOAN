# Sprint 5: OpenAPI Integration & TypeScript Strict Typing

## 1. Mục tiêu (Sprint Goal)
- Tự động sinh openapi.json từ Spring Boot Backend (erp-backend).
- Sinh ra các định nghĩa TypeScript (types/interfaces) sử dụng openapi-typescript vào package @erp/api-contract.
- Tích hợp package @erp/api-contract vào cả erp-frontend và web-public.
- Áp dụng strict typing cho các API calls ở Frontend, đảm bảo dữ liệu truyền nhận luôn khớp 100% với DTO của Backend.

## 2. Các công việc đã hoàn thành (Done)
- [x] Chạy backend với chế độ ddl-auto=update để fix lỗi lệch schema DB với code.
- [x] Trích xuất file openapi.json từ endpoint /v3/api-docs của Spring Boot.
- [x] Khởi tạo package nội bộ @erp/api-contract và dùng openapi-typescript sinh file pi.d.ts.
- [x] Cập nhật Workspace Monorepo: thêm @erp/api-contract vào dependencies của erp-frontend và web-public (sử dụng npm workspace *).
- [x] Sửa lỗi Type Strictness (Circuit Breaker): Chỉnh sửa ApiService.ts, PurchaseOrderContainer.tsx (erp-frontend), productApi.ts, ProductSection.tsx, page.tsx (web-public) để đáp ứng chuẩn type mới của backend (như SalesInvoiceCreateDto, SupplierPurchaseOrderCreateDto, ProductResponseDto).
- [x] Build thành công toàn bộ Monorepo (
pm run build --workspaces).

## 3. Quyết định kiến trúc (Architecture Decisions)
- **Monorepo Internal Packages:** Sử dụng tính năng workspaces của NPM (
pm workspaces) để chia sẻ code (Types) giữa các Next.js apps và backend contract thay vì publish lên private registry.
- **Data Transfer Objects (DTO) Mapping:** Frontend UI Models (như Product) và Backend DTOs (như ProductResponseDto) đôi khi có sự khác biệt (ví dụ UI cần image nhưng backend không lưu). Quyết định: Map dữ liệu (tạo Adapter/Mapper) ngay tại tầng Component Page/Service thay vì ép backend phải đổi schema.

## 4. Hướng dẫn đọc code (File Reading Guide)
- packages/api-contract/openapi.json: File đặc tả API OpenAPI v3.
- packages/api-contract/src/generated/api.d.ts: Các định nghĩa Typescript được sinh ra tự động.
- pps/erp-frontend/src/api/ApiService.ts: Tầng giao tiếp API của ERP Frontend, đã được refactor để sử dụng type từ contract.
- pps/web-public/src/features/products/api/productApi.ts: API Facade của Web Public gọi lấy danh mục sản phẩm (sử dụng ProductResponseDto).

## 5. Nợ kỹ thuật / Rủi ro đã biết (Known Risks/Tech Debt)
- Các UI forms ở ERP Frontend (như PurchaseOrderContainer) hiện đang dùng mock data hoặc hardcode ID (1 thay vì UUID thực). Trong các sprint sau, cần phải call API lấy các master data như danh sách Branch, User, Supplier để điền đúng UUID.
- openapi.json hiện tại đang được generate thủ công bằng cách chạy backend và dùng lệnh cURL/fetch. Có thể cải thiện bằng một build-step maven plugin tự sinh ra file.

## 6. Phạm vi nằm ngoài (Out of scope)
- Sửa lại toàn bộ giao diện của ERP frontend (vì hiện tại mới chỉ chỉnh Type ở lớp Data, chưa build toàn bộ layout UI cho từng module).

## 7. Hướng dẫn Reproduce / Test
1. Khởi động DB và Redis: docker-compose up -d hq-db redis-hq
2. Tại thư mục root (code/erp-platform), chạy: 
pm install
3. Chạy lệnh build: 
pm run build
4. Cả erp-frontend và web-public đều phải build thành công với 0 lỗi Type Error.

## 8. Bàn giao (Handoff)
- [x] Frontend đã được type-safe hoàn toàn với API Backend.
- [x] Đã sẵn sàng cho giai đoạn phát triển và tích hợp sâu các tính năng nghiệp vụ cốt lõi ở Backend như Quản lý Tồn Kho, Giá Bán.