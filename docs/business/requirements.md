# Tài Liệu Phân Tích Yêu Cầu Nghiệp Vụ (System Requirements)
Dự án: ERP Cửa Hàng Nhỏ

Tài liệu này mô tả 12 nghiệp vụ cốt lõi của hệ thống ERP dành cho cửa hàng nhỏ dưới góc độ System Analysis, làm cơ sở để thiết kế kiến trúc và phát triển phần mềm.

## 1. Nghiệp Vụ Bán Hàng (Sales Operations)

### 1.1. Tạo đơn báo giá / Đặt hàng trước (Quotation / Pre-order)
- **Mô tả:** Nhân viên bán hàng tạo báo giá hoặc ghi nhận đơn đặt hàng trước cho khách hàng. Đơn này chưa ghi nhận doanh thu và chưa trừ tồn kho.
- **Dữ liệu đầu vào:** Thông tin khách hàng, danh sách sản phẩm, số lượng, giá dự kiến, thời hạn hiệu lực báo giá, tiền cọc (nếu có).
- **Quy trình:** Tạo mới -> Chờ duyệt/Khách xác nhận -> Chuyển thành Đơn bán hàng (Sales Order).

### 1.2. Tạo đơn bán hàng (Sales Order)
- **Mô tả:** Ghi nhận giao dịch bán hàng chính thức. Sinh ra doanh thu và yêu cầu xuất kho.
- **Dữ liệu đầu vào:** Khách hàng, sản phẩm, số lượng, đơn giá, chiết khấu, thuế, phương thức thanh toán.
- **Hệ quả:** 
  - Kích hoạt tiến trình trừ tồn kho (Inventory).
  - Sinh ra công nợ khách hàng hoặc phiếu thu nếu thanh toán ngay (Finance).

### 1.3. In đơn / Phiếu xuất kho (Delivery Note)
- **Mô tả:** Tạo và in phiếu giao hàng cho kho bãi hoặc đơn vị vận chuyển. **Yêu cầu đặc biệt:** Chỉ hiển thị thông tin hàng hóa và số lượng, tuyệt đối không hiển thị giá tiền.
- **Dữ liệu:** Tên khách hàng, địa chỉ giao hàng, danh sách tên sản phẩm, mã SKU, số lượng, ghi chú giao hàng.

### 1.4. Xem danh sách đơn bán hàng theo thời gian
- **Mô tả:** Chức năng tra cứu, lọc lịch sử đơn bán hàng.
- **Tiêu chí lọc:** Khoảng thời gian (Từ ngày - Đến ngày), trạng thái đơn, khách hàng, nhân viên bán hàng.
- **Hiển thị:** Danh sách phân trang, tổng doanh thu theo khoảng thời gian tra cứu.

### 1.5. Nhập lại hàng hóa đã bán (Sales Return)
- **Mô tả:** Xử lý trường hợp khách hàng trả lại hàng (lỗi, đổi trả).
- **Hệ quả:** 
  - Tạo đơn hàng trả lại (Return Order) có tham chiếu đến đơn hàng gốc (Original Sales Order). Yêu cầu ghi nhận rõ lý do trả hàng (hư hỏng, sai mẫu, v.v.).
  - Hoàn lại số lượng tồn kho (Inventory) tương ứng với số lượng hàng trả về.
  - **Kế toán & Luồng tiền (Accounting & Cash Flow):** Phát sinh luồng tiền ra (Cash Outflow) nếu hoàn tiền ngay cho khách hàng, hoặc giảm trừ công nợ phải thu (Accounts Receivable) nếu chưa thanh toán. Điều này ngược lại với Đơn bán hàng thông thường (sinh ra Cash Inflow/Tăng công nợ).

## 2. Quản Lý Đối Tác (Partner Management)

### 2.1. Quản lý Khách hàng, Nhà phân phối
- **Mô tả:** Lưu trữ và quản lý thông tin các đối tác kinh doanh.
- **Đối tượng:** Khách hàng (Customer) và Nhà cung cấp/Nhà phân phối (Supplier/Distributor).
- **Thuộc tính:** Tên, số điện thoại, địa chỉ, email, nhóm khách hàng, mã số thuế, hạn mức công nợ.

## 3. Quản Lý Hàng Hóa & Tồn Kho (Catalog & Inventory)

### 3.1. Quản lý Danh mục sản phẩm (Product Catalog)
- **Mô tả:** Quản lý thông tin gốc của sản phẩm.
- **Thuộc tính:** Mã SKU, tên sản phẩm, danh mục (Category), đơn vị tính, giá bán lẻ, giá nhập dự kiến, hình ảnh, mô tả.

### 3.2. Xem hàng tồn kho (Inventory View)
- **Mô tả:** Cung cấp view thời gian thực về số lượng hàng hóa trong kho.
- **Cơ chế:** Dữ liệu tồn kho được tính toán dựa trên các giao dịch nhập (Purchase) và xuất (Sales). Yêu cầu hỗ trợ cảnh báo tồn kho tối thiểu.

## 4. Nghiệp Vụ Mua Hàng (Purchasing)

### 4.1. Mua hàng hóa (Purchase Order)
- **Mô tả:** Lập đơn mua hàng từ Nhà phân phối để nhập kho.
- **Dữ liệu đầu vào:** Nhà phân phối, danh sách sản phẩm, số lượng nhập, giá nhập thực tế, chi phí phát sinh.
- **Hệ quả:** 
  - Tăng số lượng tồn kho (Inventory).
  - **Kế toán & Luồng tiền (Accounting & Cash Flow):** Phát sinh luồng tiền ra (Cash Outflow) nếu thanh toán tiền ngay thông qua Phiếu chi (Payment Voucher), hoặc tăng công nợ phải trả (Accounts Payable) cho nhà cung cấp. Nghiệp vụ này mang bản chất dòng tiền âm, phân biệt rõ với dòng tiền dương của Đơn bán hàng.

## 5. Kế Toán Tài Chính (Finance)

### 5.1. Phiếu thu tiền (Receipt Voucher)
- **Mô tả:** Ghi nhận dòng tiền vào (Khách hàng thanh toán công nợ, thu khác).
- **Liên kết:** Tham chiếu tới Đơn bán hàng hoặc Công nợ khách hàng.

### 5.2. Phiếu chi tiền (Payment Voucher)
- **Mô tả:** Ghi nhận dòng tiền ra (Thanh toán cho Nhà cung cấp, chi phí vận hành).
- **Liên kết:** Tham chiếu tới Đơn mua hàng hoặc Công nợ nhà cung cấp.

## 6. Báo Cáo & Thống Kê (Reporting & Analytics)

### 6.1. Thống kê chéo (Cross Statistics)
- **Khách hàng <-> Sản phẩm:** Phân tích khách hàng A thường mua những sản phẩm gì, sản lượng bao nhiêu, doanh thu mang lại (hỗ trợ phân hạng khách hàng).
- **Nhà phân phối <-> Sản phẩm:** Phân tích nhà phân phối B cung cấp những mặt hàng nào, sản lượng nhập khẩu, tổng giá trị nhập hàng (hỗ trợ đánh giá đối tác).
