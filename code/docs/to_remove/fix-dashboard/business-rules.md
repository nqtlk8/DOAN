# Quy tắc nghiệp vụ Cảnh báo Tồn kho & Dashboard (Sprint FD)

Dựa trên yêu cầu và quyết định đã chốt từ [00-PLAN.md](00-PLAN.md).

## 1. Cảnh báo tồn kho (Inventory Alerts)

### Phạm vi và Vị trí hiển thị
- Cảnh báo **chỉ hiển thị trên Dashboard của HQ** (yêu cầu quyền `ADMIN`).
- Màn hình hiển thị danh sách các sản phẩm đang có số lượng tồn kho âm hoặc dưới ngưỡng quy định, cho "Tất cả chi nhánh" hoặc cho chi nhánh được chọn.
- Mỗi cảnh báo là 1 cặp (Sản phẩm – Chi nhánh). Không gộp chung.

### Mức cảnh báo (Alert Type)
1. **NEGATIVE_STOCK (Tồn âm):**
   - Áp dụng khi `Tồn hiện tại < 0` (BẤT KỂ CÓ ĐƯỢC CẤU HÌNH NGƯỠNG HAY KHÔNG).
   - *Ví dụ:* Sản phẩm A có tồn -5. Mức cảnh báo: `NEGATIVE_STOCK`.
2. **LOW_STOCK (Tồn dưới ngưỡng):**
   - Áp dụng khi `0 <= Tồn hiện tại <= Ngưỡng`. Tồn âm luôn đè lên LOW_STOCK (ưu tiên cao hơn).
   - Ngưỡng thiết lập theo cấp `(product_id, branch_id)`.
   - *Ví dụ 1:* Sản phẩm B có tồn 0, cấu hình ngưỡng 0. Mức cảnh báo: `LOW_STOCK`.
   - *Ví dụ 2:* Sản phẩm C có tồn 10, cấu hình ngưỡng 10. Mức cảnh báo: `LOW_STOCK`.
   - *Ví dụ 3:* Sản phẩm D có tồn 11, cấu hình ngưỡng 10. *Không cảnh báo*.

### Cơ chế tính toán (Trực tiếp từ Giao dịch)
- Dashboard tính cảnh báo trực tiếp từ lịch sử kho bảng `stock_movement` qua truy vấn `SUM(quantity)`. Không tính từ bảng snapshot `stock_on_hand` để ngăn cập nhật chồng (lợi thế append-only).
- KHÔNG dùng job lịch trình. Xóa bỏ `LowStockAlertJob` và entity bảng lịch sử cảnh báo `inventory_alert_log` (không yêu cầu truy xuất lịch sử).

### Cấu hình (inventory_alert_config)
- Chưa có UI quản lý, ngưỡng được import/insert thông qua SQL thủ công tại HQ và tự động replicate xuống chi nhánh.
- Ví dụ SQL thiết lập ngưỡng:
```sql
INSERT INTO inventory_alert_config (product_id, branch_id, min_quantity_threshold, email_recipients, is_active, created_at, updated_at) 
VALUES (1, 1, 10, NULL, true, now(), now());
```

## 2. Dashboard KPI

### Tổng doanh thu (Revenue) & Lợi nhuận gộp (Gross Profit)
- Tính doanh thu dựa vào tổng dòng `line_total` của phiếu bán có `status='CONFIRMED'` và `is_deleted=false`. (Chưa trừ hàng trả).
- Lợi nhuận gộp = Doanh thu − Giá vốn (Quantity × Unit Cost). Cả hai tính bằng 1 truy vấn.

### Tổng công nợ phải thu (Receivable Debt)
- Đổi tên trường từ `totalOverdueDebt` thành `totalReceivableDebt`.
- Chỉ lấy các khoản khách đang nợ: `SUM(GREATEST(total_debt, 0))`. Số dư âm (khách trả dư) bỏ qua.

### Chỉ số khác
- **Vòng quay tồn kho (Turnover Ratio):** `Giá vốn hàng bán trong kỳ / Giá trị tồn kho hiện tại`. (Giá trị tồn tính theo Cost Layer). Bằng 0 nếu mẫu số = 0.
- **Top bán chạy:** Top 10 sản phẩm theo **doanh thu** (chứ không phải theo số lượng bán).
- **Sản phẩm bán chậm:** Top 10 sản phẩm (sắp theo tồn hiện tại giảm dần) có mức tồn kho > 0 nhưng không bán (không phát sinh phiếu xuất bán) trong kỳ chọn.

## 3. Auto-refresh
- Dashboard trên UI sẽ tự động làm mới chu kỳ mỗi 60 giây (`DASHBOARD_REFETCH_INTERVAL_MS = 60_000`). Không refresh khi tab ẩn (chạy ngầm).
