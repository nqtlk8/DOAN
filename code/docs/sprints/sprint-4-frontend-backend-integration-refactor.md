# Sprint 4: Frontend-Backend Integration & Monorepo Refactor
Ngày hoàn thành: 2026-08-24

## 1. Mục tiêu Sprint (Goal)
Refactor cấu trúc frontend thành mô hình Monorepo, chuẩn bị cho việc tích hợp mượt mà giữa các ứng dụng Next.js (Web-Public, ERP-Frontend) với Spring Boot backend. Đồng thời, đồng bộ lại kiểu dữ liệu ID của Backend (chuyển UUID sang Long cho các Master Data từ HQ) nhằm tuân thủ thiết kế Hybrid ID phân tán của dự án.

## 2. Thành quả đạt được (Done)
- Tạo cấu trúc Workspace Monorepo chứa pps/web-public (phục vụ khách hàng, ISR/SEO) và pps/erp-frontend (ứng dụng nội bộ).
- Cấu hình BFF (Backend-For-Frontend) Proxy qua Next.js Middleware (pps/erp-frontend/src/middleware.ts) để inject access token từ HttpOnly Cookie.
- Nâng cấp API gọi danh mục sản phẩm ở Web-Public sang dùng Fetch chuẩn của Next.js với ISR caching (pps/web-public/src/features/products/api/productApi.ts).
- Migrate thành công kiểu dữ liệu ranchId và productId từ UUID sang Long trên toàn bộ Backend (Entity, DTO, Repository, Service) tại services/erp-backend/src/main/java.
- Sửa toàn bộ lỗi test (43 tests), bao gồm chuyển đổi UUID.randomUUID() thành Long, đổi endpoint sang chuẩn /api/v1/, và cô lập Spring Context cache bằng @DirtiesContext tại services/erp-backend/src/test/java.
- Tạo luồng Request For Quote (RFQ) client form (pps/web-public/src/app/rfq/page.tsx) và viết E2E Playwright test (e2e/rfq-flow.spec.ts).

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions)
**Quyết định: Sử dụng Long cho branchId và productId, giữ UUID cho các Entity giao dịch phân tán (invoiceId, receiptId).**
- Lý do: Phù hợp với chiến lược Hybrid ID (HQ tạo Branch/Product tập trung -> dùng Long; Branch tự tạo giao dịch -> dùng UUID để tránh conflict khi đồng bộ).
- Đã cân nhắc: Dùng UUID cho toàn bộ -> Bỏ vì Branch/Product được master data HQ quản lý tập trung và số lượng ít, Long tốt cho index hơn. Bỏ ý tưởng dùng Long cho giao dịch vì bắt buộc phải phân tán.

**Quyết định: Dùng Next.js Middleware làm BFF proxy thay vì gọi trực tiếp từ Client cho ERP-Frontend.**
- Lý do: Giấu token trong HttpOnly Cookie, bảo mật hơn so với localStorage, tránh lộ JWT token cho JavaScript ở trình duyệt.
- Đã cân nhắc: Gọi API trực tiếp bằng Axios từ trình duyệt và lưu token vào localStorage -> Bỏ vì rủi ro bị tấn công XSS lấy cắp token.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. services/erp-backend/src/main/java/com/storename/erp/inventory/domain/StockOnHand.java: Xem cấu trúc Entity sử dụng Long cho các foreign key tham chiếu đến dữ liệu HQ.
2. services/erp-backend/src/main/java/com/storename/erp/common/security/SecurityConfig.java: Xem cấu hình Security permit endpoint public /api/v1/auth/** và /api/v1/public/**.
3. pps/erp-frontend/src/middleware.ts: Đọc luồng BFF Proxy để hiểu cách token được trích xuất từ cookie và inject vào Request Header trước khi forward đến Backend.
4. pps/web-public/src/features/products/api/productApi.ts: Xem cách Fetch ISR caching được cấu hình cho Public Catalog.
5. e2e/rfq-flow.spec.ts: Xem Playwright test để hiểu luồng RFQ cross-app (Web-Public -> HQ -> Branch).

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Các backend integration tests đang dùng @DirtiesContext(classMode = ClassMode.AFTER_CLASS) để sửa lỗi Spring Context rò rỉ state. Điều này giúp test pass 100% nhưng làm giảm đáng kể tốc độ chạy test (mất ~45s để nạp lại Context cho từng class). Nên tối ưu hóa cấu hình @TestPropertySource trong tương lai để tái sử dụng context tốt hơn.
- Cần nâng cấp hệ thống CI/CD để hỗ trợ build Turborepo thay vì build thủ công từng app.

## 6. Việc chưa làm / Out of scope
- Chưa sinh tự động Typescript API interfaces (OpenAPI to TS) cho package pi-contract dùng chung.
- Chưa triển khai một giải pháp Next.js Auth chuyên sâu (như NextAuth/Auth.js) để xử lý hoàn toàn luồng Auth. Hiện tại vẫn đang sử dụng một luồng custom Middleware Proxy khá thô sơ.

## 7. Cách chạy & Cách verify (Reproduce)
Chạy backend test để kiểm chứng code backend hoạt động ổn định:
`ash
cd code/erp-platform/services/erp-backend
./mvnw clean test
`
Kỳ vọng output: Tests run: 43, Failures: 0, Errors: 0, Skipped: 1 và BUILD SUCCESS.

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Backend và cấu trúc Frontend cơ bản đã hoàn thiện. Sprint tiếp theo cần tập trung vào việc generate OpenAPI Specification (openapi.json) từ Spring Boot, sau đó dùng openapi-typescript để sinh mã TS tự động đẩy vào package pi-contract.
- Các frontend components (trong ERP-Frontend) sau đó cần được migrate để gọi API dựa trên các Typescript types đã được sinh ra này thay vì hardcode type.
