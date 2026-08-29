# ADR-001: Sử dụng Hub-and-Spoke với PostgreSQL Logical Replication

## Tình trạng
Chấp nhận

## Bối cảnh
Hệ thống ERP cần phục vụ nhiều chi nhánh. Các chi nhánh cần tốc độ truy cập nhanh, không bị ảnh hưởng bởi độ trễ mạng khi gọi về máy chủ trung tâm (HQ). Đồng thời HQ cần có toàn bộ dữ liệu để làm báo cáo và quản lý tổng thể. 

## Quyết định
Sử dụng kiến trúc Hub-and-Spoke. 
- HQ sẽ là Primary Database.
- Các Branch sẽ có Database riêng.
- Sử dụng PostgreSQL Logical Replication để đồng bộ dữ liệu. Publication tại HQ và Subscription tại Branch.

## Hậu quả
- **Tích cực:** Tăng tốc độ đọc/ghi tại chi nhánh. Các chi nhánh hoạt động độc lập ngay cả khi mất kết nối mạng tạm thời với HQ.
- **Tiêu cực:** Tăng độ phức tạp quản trị DB. Có rủi ro conflict dữ liệu nếu cấu hình ghi 2 chiều hoặc chia cắt partition không kỹ lưỡng. Phải quản lý conflict thủ công nếu xảy ra.
