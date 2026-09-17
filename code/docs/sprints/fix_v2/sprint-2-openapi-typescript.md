# Sprint 2: OpenAPI + TypeScript
**Ngày hoàn thành:** 16/09/2026

## 1. Mục tiêu Sprint (Goal)
- Chuyển đổi toàn bộ TypeScript types ở Frontend để trực tiếp sử dụng cấu trúc được sinh ra từ API Backend qua OpenAPI (Swagger).
- Dọn dẹp tất cả các trường hợp dùng `any` và các đoạn code fallback (như `customerName || customerId`) để che đậy sự không khớp giữa Frontend và Backend. 

## 2. Thành quả đạt được (Done)
- **Tự động hóa sinh OpenAPI schema:** Tạo class test `OpenApiGeneratorTest` để tự động khởi tạo Spring Context và trích xuất file `openapi.json` xuống trực tiếp thư mục `packages/api-contract`.
- **Cập nhật Backend Dependencies:** Nâng cấp SpringDoc WebMVC UI lên `2.7.0` để khắc phục lỗi tương thích (NoSuchMethodError) với Spring Boot 3.4.0 (Spring Framework 6.2).
- **Cấu hình property cho testing context:** Bổ sung `@TestPropertySource(properties = {"instance.role=BRANCH"})` vào Generator Test để đảm bảo các endpoint branch được load và export vào schema.
- **Tạo và sử dụng API interface tĩnh:** Sinh `api.d.ts` thông qua thư viện `openapi-typescript` thành công ở API Contract package.
- **Loại bỏ fallback và any tại Frontend UI:**
  - `src/api/ApiService.ts`: Typed toàn bộ hàm gọi API bằng `components['schemas'][...]`.
  - `src/components/crm/DebtList.tsx`: Xoá `partnerName`, thay bằng `customerName`.
  - `src/components/inventory/StockList.tsx`: Cập nhật schema chính xác cho table và API calls.
  - `src/components/sales/SalesOrderForm.tsx`, `src/components/sales/SalesList.tsx`, `src/hooks/useSalesInvoice.ts`: Xoá fallback. Ép đúng kiểu số cho `productId` để truyền vào Backend.
  - `src/components/inventory/InboundReceiptModule.tsx`, `src/hooks/useInboundReceipt.ts`: Khắc phục lỗi schema mapping cho Inbound receipt create.
  - `src/components/sales/Dashboard.tsx`, `tests/dashboard.spec.ts`: Cập nhật static schema, fix type strict TypeScript compiler rules.
  - Xử lý hoàn toàn các lỗi `Property 'children' is missing in type` ở các component bọc `DataState` khi compile TypeScript.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- **Quyết định:** Sử dụng `@SpringBootTest` kết hợp `MockMvc` và `@ActiveProfiles("test")` để sinh `openapi.json` tĩnh lúc build/test, thay vì bắt buộc chạy app backend lên.
- **Lý do:** Không cần phụ thuộc vào cơ sở dữ liệu thật lúc build. Tránh tình trạng server thiếu cấu hình (FATAL database connection failed) cản trở việc cập nhật schema phía frontend.
- **Đã cân nhắc:** Sử dụng Plugin maven `springdoc-openapi-maven-plugin`. Bỏ qua vì cần cấu hình rườm rà ở phase pre-integration-test và dễ kẹt lại nếu app crash vì database không online. Code chay `MockMvc` test đơn giản hơn và hoàn toàn tái sử dụng được môi trường test có sẵn (H2 database).

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. `services/erp-backend/src/test/java/com/storename/erp/OpenApiGeneratorTest.java`: Tìm hiểu cách JSON OpenAPI được dump ra file thông qua unit test.
2. `packages/api-contract/package.json`: Lệnh sinh TypeScript interface từ json sử dụng `openapi-typescript`.
3. `apps/erp-frontend/src/api/ApiService.ts`: Cách import `components` type thay vì tự định nghĩa DTO.
4. `apps/erp-frontend/src/components/sales/SalesList.tsx` & `SalesOrderForm.tsx`: Các ví dụ về cách mapping chính xác Frontend UI với Backend Response DTOs sau khi áp dụng type strict.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Các module khác ngoài (Sales, Inbound, Debt, Stock) ở frontend có thể vẫn còn sử dụng `any` hoặc fallback tạm. Việc dọn dẹp triệt để trên toàn bộ ERP sẽ diễn ra theo nhu cầu khi chạm tới từng module.
- OpenAPI schema hiện đang dump sử dụng Role Branch. Tương lai nếu có API khác biệt giữa `BRANCH` và `HQ`, ta cần có phương pháp dump schema cho cả hai (ví dụ: tách file, hoặc gộp chung 1 file JSON). Sẽ xử lý ở các phase nâng cấp multi-branch.

## 6. Việc chưa làm / Out of scope
- Sửa lỗi frontend hoặc luồng UI không liên quan đến type-checking và mismatch (ví dụ: responsive css, UX/UI animation).
- Nghiệp vụ công nợ thực sự (Advance payment) sẽ được xử lý độc lập tại Sprint 3.

## 7. Cách chạy & Cách verify (Reproduce)
- **Bước 1 (Backend):** Ở thư mục `services/erp-backend`, chạy lệnh `.\mvnw test -Dtest=OpenApiGeneratorTest`. Lệnh này dump `openapi.json` vào packages contract.
- **Bước 2 (Contract):** Ở thư mục `packages/api-contract`, chạy lệnh `npm run generate`. File `src/generated/api.d.ts` sẽ được sinh lại.
- **Bước 3 (Frontend):** Ở thư mục `apps/erp-frontend`, chạy lệnh `npx tsc --noEmit`. Trình biên dịch TypeScript chạy thành công và báo kết quả OK (exit code 0) không còn lỗi mismatched type, missing children components, hay parameter errors.

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
Mọi response contract giữa Frontend và Backend đã khớp và minh bạch. 
Sprint sau (Sprint 3) bắt đầu tập trung vào Sales Advance Payment (Tạo đơn bán có trả trước). Ta đã có thể tận dụng `SalesInvoiceCreateDto` chuẩn, giờ cần mở rộng thêm `advancePayment` (hoặc method) cho API này ở Backend và gọi lại sinh `openapi.json` là Frontend sẽ sử dụng được ngay.
