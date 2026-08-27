# Module Identity

Module quản lý tài khoản, phân quyền và xác thực (JWT) trong hệ thống ERP theo kiến trúc Hub-and-Spoke.

## Kiến trúc [v3]
- Sử dụng thuật toán **RS256** (bất đối xứng) với Private Key (chỉ có ở HQ) và Public Key (có ở mọi chi nhánh).
- Cache (Redis) cho denylist (revoke refresh token) **chỉ tồn tại ở HQ**. Các chi nhánh không kết nối Redis.

## Phân quyền RBAC (Sprint 1.1 v4)
Hệ thống sử dụng 2 role duy nhất:
- `ADMIN`: Quản trị viên tại trụ sở chính (HQ). Không gắn với chi nhánh nào (`branch_id` phải là NULL).
- `STAFF`: Nhân viên tại chi nhánh. Phải gắn với một chi nhánh cụ thể (`branch_id` NOT NULL).

Hệ thống bảo vệ việc ghi dữ liệu master (vốn chỉ được phép thực hiện từ HQ) thông qua 3 lớp phòng thủ:

### Lớp phòng thủ 1: Tầng mạng (Nginx Reverse Proxy)
- Cấu hình Nginx tại mỗi Branch instance CHỈ cho phép (allow) request từ dải IP nội bộ hoặc VPN của chính chi nhánh đó.
- Request từ mạng ngoài (bao gồm cả Admin từ HQ) sẽ bị từ chối ngay ở lớp Nginx (trả về 403 hoặc 444) và KHÔNG bao giờ tới được ứng dụng Spring Boot. 
- *Đây là bước cấu hình BẮT BUỘC khi triển khai hạ tầng cho một chi nhánh mới.*

### Lớp phòng thủ 2: Tầng ứng dụng (Spring Profiles)
- Các endpoint và bean chuyên phục vụ ghi master data (như `ProductWriter`, `ProductWriteController`, `CustomerWriter`, `CustomerWriteController`) đều được gắn `@ConditionalOnProperty(name = "instance.role", havingValue = "HQ")`.
- Khi Spring Boot chạy ở mode branch (profile `branch`), các class này hoàn toàn không được khởi tạo. Do đó, ngay cả khi lọt qua Lớp phòng thủ 1, app chi nhánh cũng không có API nào để nhận lệnh tạo/sửa dữ liệu master.

### Lớp phòng thủ 3: Tầng cơ sở dữ liệu (Database Permissions)
- Ứng dụng Spring Boot tại chi nhánh kết nối vào DB bằng user `app_user`.
- Logical Replication (từ HQ xuống) dùng một user khác (ví dụ `erp_user` hoặc subscriber user).
- Quá trình khởi tạo Branch DB (Flyway `migration-branch`) sẽ chủ động `REVOKE INSERT, UPDATE, DELETE` trên các bảng master data (`branch`, `product`, `category`, `price_list`, `customer`) đối với `app_user`.
- Như vậy, nếu hacker chiếm quyền ứng dụng, họ cũng bị database chặn lại ở mức permission denied khi cố tình `UPDATE` bảng price_list tại chi nhánh.

## Danh sách Endpoint
| API Endpoint | Chi tiết (Mục đích) | Tồn tại ở HQ | Tồn tại ở Branch |
| --- | --- | --- | --- |
| POST /api/auth/login | Đăng nhập và nhận Access/Refresh token (ký bằng Private Key) | Có | KHÔNG |
| POST /api/auth/refresh| Cấp mới token (sử dụng Redis để check denylist) | Có | KHÔNG |
| POST /api/auth/revoke | Thu hồi token (đẩy vào Redis denylist) | Có | KHÔNG |
| *(Mọi API nghiệp vụ)* | Verify token (JwtAuthenticationFilter) dùng Public Key | Có | Có |
