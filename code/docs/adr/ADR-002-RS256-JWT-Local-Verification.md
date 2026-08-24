# ADR-002: Xác thực JWT RS256 Cục bộ và HQ-Only Issuance

## Tình trạng
Chấp nhận

## Bối cảnh
Với kiến trúc N-Instance (Hub-and-Spoke), các Branch instance (Spoke) cần hoạt động độc lập và giảm thiểu tối đa độ trễ mạng do phải gọi về HQ. Nếu dùng Session truyền thống hoặc JWT HMAC (HS256) yêu cầu share secret key, sẽ tạo ra lỗ hổng bảo mật (vì Branch bị lộ secret key có thể tạo ra token giả mạo cho toàn hệ thống). 

## Quyết định
1. Sử dụng thuật toán bất đối xứng **RS256** (RSA Signature) cho JWT.
2. **Chỉ duy nhất HQ** giữ Private Key và cung cấp endpoint `/auth/login`, `/auth/refresh` để cấp phát token (Issuance).
3. **Mọi Branch** chỉ được cấu hình Public Key. Khi có request tới Branch, Branch sẽ tự động xác minh chữ ký JWT bằng Public Key hoàn toàn **cục bộ** (Local Verification), không phát sinh bất kỳ network call nào về HQ.
4. Token sẽ chứa các claim quan trọng như `branchId` và `role` để thực thi phân quyền (RBAC) và Multi-tenancy cục bộ.

## Hậu quả
- **Tích cực:** Các chi nhánh hoạt động cực kỳ nhanh chóng và độc lập sau khi User đã có token. Không có Single Point of Failure về Authentication ở mặt verify. Môi trường Branch an toàn vì không giữ Private Key.
- **Tiêu cực:** Đánh đổi khả năng Revocation (thu hồi token tức thời). Vì Branch không gọi về HQ, nếu HQ block một user, user đó vẫn có thể dùng JWT cũ (chưa hết hạn) truy cập Branch. Để khắc phục, ta phải dùng thời gian sống (TTL) của Access Token ngắn (15-30 phút).
