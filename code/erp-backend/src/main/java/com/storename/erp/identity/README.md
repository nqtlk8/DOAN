# Module Identity

Module quản lý tài khoản, phân quyền và xác thực (JWT) trong hệ thống ERP theo kiến trúc Hub-and-Spoke.

## Kiến trúc [v3]
- Sử dụng thuật toán **RS256** (bất đối xứng) với Private Key (chỉ có ở HQ) và Public Key (có ở mọi chi nhánh).
- Cache (Redis) cho denylist (revoke refresh token) **chỉ tồn tại ở HQ**. Các chi nhánh không kết nối Redis.

## Danh sách Endpoint
| API Endpoint | Chi tiết (Mục đích) | Tồn tại ở HQ | Tồn tại ở Branch |
| --- | --- | --- | --- |
| POST /api/auth/login | Đăng nhập và nhận Access/Refresh token (ký bằng Private Key) | Có | KHÔNG |
| POST /api/auth/refresh| Cấp mới token (sử dụng Redis để check denylist) | Có | KHÔNG |
| POST /api/auth/revoke | Thu hồi token (đẩy vào Redis denylist) | Có | KHÔNG |
| *(Mọi API nghiệp vụ)* | Verify token (JwtAuthenticationFilter) dùng Public Key | Có | Có |

