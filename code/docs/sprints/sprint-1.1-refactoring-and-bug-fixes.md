# Sprint 1.1: Refactoring & Bug Fixes
**Hoàn thành:** 2026-08-19

## 1. Mục tiêu Sprint (Goal)
- Giải quyết triệt để các lỗi kiến trúc, lỗi rò rỉ trạng thái dữ liệu (state leakage) trong môi trường test, và các vấn đề bảo mật (RBAC, JWT) đã được xác định trong báo cáo Code Review của Sprint 1.1.
- Sprint này xây dựng trên nền tảng kiến trúc N-Instance (Modular Monolith) từ Sprint 0.1 và Sprint 1.1 ban đầu, đảm bảo tính đúng đắn trước khi mở rộng sang các module nghiệp vụ (Inventory, Product).

## 2. Thành quả đạt được (Done)
- **Sửa lỗi State Leakage H2 Database**: Khắc phục lỗi `Wrong user name or password [28000-232]` trong môi trường test do các Spring Context khác nhau chia sẻ chung một instance H2. (File: `code/erp-backend/src/main/resources/application-test.yml`, `BranchLocalAuthTest.java`).
- **Hoàn thiện bảo mật JWT Filter**: Bắt `JwtException` và trả về HTTP 401 chuẩn JSON thay vì để lỗi lọt qua Filter Chain. (File: `code/erp-backend/src/main/java/com/storename/erp/common/security/JwtAuthenticationFilter.java`).
- **An toàn ép kiểu SecurityContext**: Bổ sung object `JwtAuthDetails` để chứa `branchId` và `tokenId`, ngăn chặn triệt để `ClassCastException` trong các Interceptor/Aspect. (File: `code/erp-backend/src/main/java/com/storename/erp/common/security/JwtAuthDetails.java`, `BranchScopedAspect.java`).
- **Xác thực qua Database thực**: Tích hợp `UserRepository` và `BCryptPasswordEncoder` vào `AuthService` thay vì dùng thông tin đăng nhập cứng (hardcoded). (File: `code/erp-backend/src/main/java/com/storename/erp/identity/application/AuthService.java`).
- **Sửa lỗi Request Mapping**: Cập nhật endpoint `/auth/login` thành `/api/auth/login` trong các file test để khớp với cài đặt của `AuthController`. (File: `ArchitectureV3HqTest.java`, `ArchitectureV3BranchTest.java`).
- **Mở Public Endpoint Health**: Cấu hình `permitAll()` cho `/api/health` trong `SecurityConfig`. (File: `code/erp-backend/src/main/java/com/storename/erp/common/security/SecurityConfig.java`).

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions)
- **Quyết định**: Sử dụng URL `jdbc:h2:mem:testdb_${random.uuid};DB_CLOSE_DELAY=-1` cho database H2 trong các file cấu hình test (như `application-test.yml`).
  - **Lý do**: Đảm bảo mỗi Spring Test Context khi được khởi tạo sẽ nhận một database H2 độc lập hoàn toàn, triệt tiêu tình trạng context này ghi đè password sa hoặc thay đổi schema ảnh hưởng đến context khác.
  - **Đã cân nhắc**: Dùng `@DirtiesContext` cho toàn bộ các class test. **Bỏ vì**: `@DirtiesContext` ép Spring Boot phải khởi động lại context liên tục, làm thời gian chạy test suite tăng đột biến và tốn tài nguyên vô ích.
- **Quyết định**: Khởi tạo `JwtAuthDetails` làm Custom Authentication Details.
  - **Lý do**: Lớp `BranchScopedAspect` cần đọc `branchId` một cách an toàn mà không bị vướng exception do mặc định Spring Security sử dụng `WebAuthenticationDetails` không chứa `branchId`.
  - **Đã cân nhắc**: Ép kiểu trực tiếp từ Token Claims. **Bỏ vì**: Việc parse lại token thủ công nhiều lần trong Aspect giảm hiệu năng và vi phạm nguyên lý Single Responsibility.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. `code/erp-backend/src/main/resources/application-test.yml`: Đọc để thấy sự thay đổi biến URL của H2 test. Chú ý `${random.uuid}`.
2. `code/erp-backend/src/main/java/com/storename/erp/common/security/JwtAuthenticationFilter.java`: Xem cách bóc tách claims từ JWT, sau đó đẩy thông tin vào `JwtAuthDetails` và gắn vào `SecurityContext`.
3. `code/erp-backend/src/main/java/com/storename/erp/common/aop/BranchScopedAspect.java`: Xem cách lấy `branchId` một cách an toàn từ `JwtAuthDetails` để giới hạn phạm vi truy vấn.
4. `code/erp-backend/src/main/java/com/storename/erp/identity/application/AuthService.java`: Xem luồng logic đăng nhập thực tế kết nối vào `UserRepository` và mã hóa password.
5. Các file test (`ArchitectureV3HqTest.java`, `ArchitectureV3BranchTest.java`, `AuthIntegrationTest.java`): Đọc để thấy test cách ly hoạt động đúng với HTTP Status phù hợp (401, 403, 404).

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- **Thiếu Keycloak / OIDC**: Hiện tại, hệ thống tự cấp và quản lý JWT. Trong tương lai (tùy thuộc lộ trình), hệ thống sẽ chuyển sang OIDC qua Keycloak.
  - **Ảnh hưởng**: Sẽ có chi phí refactor `AuthController` và `JwtTokenProvider` ở các sprint sau. Nên xử lý khi nghiệp vụ auth bắt đầu phức tạp hoặc có yêu cầu tích hợp SSO.
- **Chưa có Redis trong Docker Test tự động**: Hiện tại `AuthIntegrationTest` vẫn bỏ qua config Redis hoặc dùng H2/Mock vì cấu hình testsuite không kích hoạt testcontainers cho Redis.
  - **Ảnh hưởng**: Token denylist trong Redis HQ chưa được test 100% trong E2E nếu chỉ chạy `mvn test`. Sẽ cần bổ sung Testcontainers ở Sprint sau.

## 6. Việc chưa làm / Out of scope
- Không tích hợp hay thay đổi bất kỳ business logic nào của module ngoài (Inventory, Order). Sprint này được đóng gói (scoped) 100% ở Refactoring nền tảng và Identity/Auth.
- KHÔNG cài đặt Data seeding tự động cho bảng người dùng ngoài các bản ghi init cơ sở. Vấn đề Seed data sẽ được xử lý độc lập.

## 7. Cách chạy & Cách verify (Reproduce)
Chạy toàn bộ unit và integration test để xác minh 100% testsuite xanh:
```powershell
cd code/erp-backend
.\mvnw.cmd clean test
```
**Kết quả mong đợi:** 
```text
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Nền tảng phân tách Branch/HQ và xác thực JWT đã ổn định, bảo mật (loại trừ H2 database overlap). 
- Sprint tiếp theo có thể tự tin phát triển các chức năng CRUD nghiệp vụ (ví dụ: Sản phẩm, Quản lý kho, POS) mà không lo vướng rào cản từ lớp Security. 
- Khi bắt đầu Sprint 1.2, hãy tận dụng `@BranchScoped` và đọc `branchId` từ `SecurityContextHolder.getContext().getAuthentication().getDetails()` bằng cách ép kiểu sang `JwtAuthDetails`.
