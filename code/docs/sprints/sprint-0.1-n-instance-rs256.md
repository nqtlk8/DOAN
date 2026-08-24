# Sprint 0.1: Nền tảng Kiến trúc N-Instance & JWT RS256
Ngày hoàn thành: 2026-08-19

## 1. Mục tiêu Sprint (Goal)
- Dựng khung dự án chạy được cả 2 vai trò Trụ sở (HQ) và Chi nhánh (Branch) từ cùng một mã nguồn (artifact) nhưng có bối cảnh hoạt động độc lập qua cấu hình.
- Thiết lập hệ thống bảo mật không trạng thái (stateless) cho phép các Branch tự xác minh JWT hoàn toàn offline mà không phụ thuộc HQ.
- Khởi tạo hạ tầng môi trường với 3 instance riêng biệt: HQ, Branch TP1, Branch TP2 cùng Redis tập trung tại HQ.

## 2. Thành quả đạt được (Done)
- Tạo cấu trúc ứng dụng đa cấu hình qua Spring Profiles (`hq` và `branch`).
  - [application-hq.yml](file:///d:/Docs/CodeProject/DOAN/code/erp-backend/src/main/resources/application-hq.yml)
  - [application-branch.yml](file:///d:/Docs/CodeProject/DOAN/code/erp-backend/src/main/resources/application-branch.yml)
- Bổ sung xác thực JWT RS256 có phân biệt Token Type (`access` vs `refresh`) thông qua lớp Provider chung và Filter chặn.
  - [JwtTokenProvider.java](file:///d:/Docs/CodeProject/DOAN/code/erp-backend/src/main/java/com/storename/erp/common/security/JwtTokenProvider.java)
  - [JwtAuthenticationFilter.java](file:///d:/Docs/CodeProject/DOAN/code/erp-backend/src/main/java/com/storename/erp/common/security/JwtAuthenticationFilter.java)
- Tạo API Authentication Mock chỉ tồn tại tại HQ: `/api/auth/login`, `/api/auth/refresh`, `/api/auth/revoke`.
  - [AuthController.java](file:///d:/Docs/CodeProject/DOAN/code/erp-backend/src/main/java/com/storename/erp/identity/api/AuthController.java)
- Rào vùng AutoConfiguration của Redis chỉ kích hoạt tại Profile HQ, giữ Branch không dính dependency ngoại lai.
  - [RedisConfig.java](file:///d:/Docs/CodeProject/DOAN/code/erp-backend/src/main/java/com/storename/erp/infrastructure/redis/RedisConfig.java)
- Khởi tạo stub Aspect cho tính năng Idempotency (Sprint sau).
  - [IdempotencyProtected.java](file:///d:/Docs/CodeProject/DOAN/code/erp-backend/src/main/java/com/storename/erp/common/aop/IdempotencyProtected.java)
- Viết Test-case xác nhận giới hạn ngữ cảnh cho AuthController và RedisTemplate.
  - [ArchitectureV3HqTest.java](file:///d:/Docs/CodeProject/DOAN/code/erp-backend/src/test/java/com/storename/erp/ArchitectureV3HqTest.java)
  - [ArchitectureV3BranchTest.java](file:///d:/Docs/CodeProject/DOAN/code/erp-backend/src/test/java/com/storename/erp/ArchitectureV3BranchTest.java)
- Cập nhật Docker Compose khởi động 3 Nodes cùng với 2 cấu hình Reverse Proxy Nginx.
  - [docker-compose.yml](file:///d:/Docs/CodeProject/DOAN/code/docker-compose.yml)

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions)
**Quyết định 1: Dùng RS256 thay vì HS256 cho JWT**
- Lý do: Branch cần xác thực JWT offline mà không lưu khóa bí mật để phòng chống lộ lọt thông tin.
- Đã cân nhắc: HS256 — Bỏ vì khóa đối xứng sẽ phải đặt chung ở mọi instance Branch, tiềm ẩn rủi ro Branch sinh ra được token mạo danh.

**Quyết định 2: Exclude Redis AutoConfiguration ở Branch bằng Property thay vì Code Programmatic**
- Lý do: Tách biệt mạnh mẽ, không cần thêm code if/else ở Context, bảo vệ toàn diện môi trường Branch.
- Đã cân nhắc: Để Redis kết nối chung tới HQ — Bỏ vì Branch bắt buộc hoạt động phi tập trung, đứt mạng với HQ vẫn phải thao tác được cục bộ.

**Quyết định 3: JWT Resource File Reader thay vì File Paths**
- Lý do: Khi build Jar hoặc chạy Docker image, Resource classpath là cách an toàn duy nhất để đọc file PEM mà không bị `NoSuchFileException`.
- Đã cân nhắc: Đọc theo `Paths.get` absolute — Bỏ vì đường dẫn sẽ bị gãy vỡ ở môi trường Docker Linux.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. `docker-compose.yml`: Xem cách khai báo service, mapping profile, và cô lập Redis khỏi `branch-tp1-app` và `branch-tp2-app`.
2. `application-hq.yml` & `application-branch.yml`: Lưu ý biến `spring.autoconfigure.exclude` để tắt Redis.
3. `SecurityConfig.java`: Lưu ý đường dẫn loại trừ (permitAll) cho `/api/auth/**`.
4. `JwtTokenProvider.java`: Phương thức `buildToken` sinh payload `type` và `Resource` Loader.
5. `JwtAuthenticationFilter.java`: Bộ lọc chính với logic đọc `tokenId`, `branchId` nạp vào `JwtAuthDetails` và trả lỗi 401 chuẩn JSON qua `ApiResponse`.
6. `AuthController.java`: Lưu ý sự tồn tại của `@Profile("hq")`.
7. Các Test: `ArchitectureV3HqTest` & `ArchitectureV3BranchTest` kiểm chứng tính cách ly bối cảnh (Redis bean, Endpoint 404).

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Khóa RSA đang load từ `classpath:certs/` hardcode cho mục đích Dev. Ở môi trường Production cần truyền qua Environment Variable/Secret Manager. (Sẽ cấu hình ở CI/CD/Ops Phase).
- Redis Denylist chưa implement logic filter ở `JwtAuthenticationFilter`. Tạm thời chỉ xử lý revoke theo form, nhưng Token chưa bị từ chối thực sự nếu Revoke. Phải xử lý logic này ở Sprint 1.1 khi viết xong module Identity.
- Cấu hình Nginx rất sơ khai (chỉ là forward proxy cục bộ), chưa có HTTPS/TLS, Rate Limiting. Sẽ cải thiện ở Sprint 5.1.

## 6. Việc chưa làm / Out of scope
- Chưa tạo Schema User/Account/Role. Toàn bộ logic login/refresh/revoke đang là Mock. Điều này thuộc Scope của Sprint 1.1.
- Replication PostgreSQL chưa cấu hình do chưa có Table nào tồn tại ở Sprint này.

## 7. Cách chạy & Cách verify (Reproduce)
**Chạy bằng Docker:**
```powershell
cd code
docker compose up -d --build
```
Kiểm tra kết quả: Có 6 containers Up (hq-app, branch-tp1-app, branch-tp2-app, hq-db, branch-tp1-db, branch-tp2-db, redis-hq, hq-nginx, branch-tp1-nginx, branch-tp2-nginx).
Truy cập POST `http://localhost:80/api/auth/login` (body JSON) -> Nhận JWT (Môi trường HQ).
Truy cập POST `http://localhost:81/api/auth/login` (body JSON) -> 404 Not Found (Môi trường TP1).

**Chạy Unit/Integration Tests bằng Maven:**
```powershell
cd code/erp-backend
.\mvnw clean test
```
Mong đợi: Build SUCCESS, 0 failures. (Phải đảm bảo hq-db và branch-arch-db chạy, hoặc như code hiện tại đang mock H2 in-memory DB nên test chạy độc lập hoàn toàn không cần Docker).

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
Sprint 1.1 sẽ bắt tay ngay vào tạo Database Schema cho Module Identity (User, Role, Permission). Các bạn cần tái sử dụng `JwtTokenProvider` hiện tại để gen ra Access Token thực dựa trên User thật trong DB.
Lưu ý về Nợ kĩ thuật (Mục 5): Phải nhúng kết nối Redis vào filter chặn JWT cho luồng RefreshToken ở HQ để phục vụ Denylist Revoke.
