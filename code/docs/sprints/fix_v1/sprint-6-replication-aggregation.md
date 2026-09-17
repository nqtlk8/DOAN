# Báo cáo Sprint 6 - Dynamic Replication Aggregation for HQ
Ngày hoàn thành: 2026-09-16

## 1. Mục tiêu Sprint
Giải quyết rủi ro ghi đè dữ liệu (Race Condition / Split-brain) khi đồng bộ các bảng Snapshot (`stock_on_hand`, `receivable_debt`) từ Branch lên HQ. Dữ liệu Snapshot bị cấm đồng bộ, thay vào đó HQ phải tự tính toán động từ bảng giao dịch (`stock_movement`, `sales_invoice`).

## 2. Thành quả đạt được
- Loại bỏ logic SQL cũ ở `AnalyticsDataAdapter` đối với HQ (khi `branchId == null`).
- Triển khai logic tính toán động (On-The-Fly): 
  - `getTotalReceivableDebt` tại HQ giờ tính tổng `remaining_debt` từ bảng `sales_invoice` đã confirm.
  - `getCurrentStockQuantity` tại HQ giờ tính tổng `quantity` từ bảng `stock_movement` (do quantity đã quy ước âm/dương nên chỉ cần `SUM(quantity)`).
- Kiểm chứng config replication (script `setup-replication.sh` và `DATABASE_REPLICATION.md`) đảm bảo 2 bảng Snapshot không nằm trong publication `TRANSACTION_TABLES`.

## 3. Quyết định kiến trúc & Lý do
- Quyết định: HQ không lưu trữ bản copy Snapshot của các nhánh mà tự tổng hợp (Aggregate) khi có truy vấn (Phương án A).
- Lý do: Tối đa hóa tính nhất quán. `stock_on_hand` và `receivable_debt` liên tục bị Update, nếu Replicate sẽ gây ra lượng WAL lớn và dễ lệch dữ liệu khi có độ trễ mạng. Các bảng append-only (`stock_movement`, `sales_invoice`) an toàn hơn khi đồng bộ logic.
- Đã cân nhắc: Tạo bảng `stock_on_hand_hq` riêng biệt - bỏ vì sẽ làm tăng độ phức tạp của schema.

## 4. Hướng dẫn đọc code theo thứ tự
1. `AnalyticsDataAdapter.java`: Xem nhánh `if (branchId == null)` trong 2 hàm `getTotalReceivableDebt` và `getCurrentStockQuantity`.
2. `StockMovement.java`: Xem invariant `SUM(quantity)` ở mô tả class.
3. `DATABASE_REPLICATION.md`: Xem danh sách `TRANSACTION_TABLES` (không chứa 2 bảng snapshot).

## 5. Rủi ro / Nợ kỹ thuật đã biết
- Tính toán động trên `stock_movement` toàn hệ thống (HQ) sẽ chậm dần khi lượng dữ liệu phình to. Sau này cần một Background Job ở HQ để gom dữ liệu định kỳ (Materialized View hoặc Data Warehouse).

## 6. Việc chưa làm / Out of scope
- Chưa xây dựng kiến trúc Data Warehouse / Materialized View.

## 7. Cách chạy & Cách verify
- Chạy unit test: `mvn test -Dtest=AnalyticsDataAdapterTest`
- Xác nhận các truy vấn động tại HQ (null branch) thực thi mượt mà trên H2/Postgres.

## 8. Điểm nối cho Sprint tiếp theo
Sẵn sàng cho việc Full Regression Test toàn bộ dự án để kết thúc pha phát triển và Handover.
