# Kiến trúc MDI Desktop (MDI Layout Architecture)

Tài liệu này mô tả kiến trúc của giao diện MDI (Multiple Document Interface) Desktop cho dự án ERP.

## 1. Hệ thống Tab Làm Việc (Workspace Tabs)

Hệ thống Tab cho phép người dùng mở nhiều module/chức năng cùng lúc trên cùng một cửa sổ trình duyệt, tương tự như giao diện Desktop chuẩn.

### Cấu trúc `TabContext`
State quản lý các Tab sẽ được lưu trữ trong một Global State (React Context, Redux, hoặc Zustand tùy quyết định kỹ thuật), bao gồm:
- **`tabs`**: Một mảng chứa các đối tượng Tab đang mở. Mỗi đối tượng Tab bao gồm:
  - `id` (string): Định danh duy nhất của Tab (ví dụ: `sales-order-123`, `create-sales-order`).
  - `title` (string): Tiêu đề hiển thị trên thanh Tab (ví dụ: "Đơn hàng #123", "Thêm mới Đơn hàng").
  - `component` (ReactNode / String identifier): Component nội dung (hoặc định danh component) sẽ được render khi Tab này active.
  - `isClosable` (boolean): Cho biết Tab có thể đóng được hay không (thường là `true`, ngoại trừ tab mặc định như Dashboard/Home).
- **`activeTabId`** (string): Lưu trữ `id` của Tab đang được chọn (active) để hiển thị nội dung ra màn hình.

### Hành vi
- **Mở Tab mới**: Khi người dùng click vào một chức năng từ Menu hoặc mở chi tiết chứng từ, hệ thống kiểm tra xem `id` của tab đó đã tồn tại trong `tabs` chưa.
  - Nếu đã tồn tại: Chỉ cần chuyển `activeTabId` thành `id` đó (Focus vào tab cũ).
  - Nếu chưa tồn tại: Thêm đối tượng tab mới vào mảng `tabs` và cập nhật `activeTabId` thành `id` mới.
- **Đóng Tab**: Xóa tab khỏi mảng `tabs`. Nếu tab bị đóng đang là tab active, hệ thống tự động chuyển `activeTabId` sang tab liền kề (bên trái hoặc bên phải) hoặc tab Dashboard mặc định.

---

## 2. Cấu trúc Form 3 Cột (3-Column Form Structure)

Để tận dụng tối đa không gian hiển thị rộng trên màn hình Desktop, phần Header của các Form nhập liệu chính (như Đơn hàng, Phiếu Xuất/Nhập, Chứng từ Kế toán) sẽ được chia làm 3 cột rõ ràng:

1. **Cột 1: Thông tin Nhân viên / Nội bộ**
   - **Người lập phiếu**: Tự động lấy theo User đang đăng nhập.
   - **Chi nhánh / Phòng ban**: Nơi thực hiện giao dịch.
   - **Ngày lập / Giờ lập**: Tự động lấy thời gian hiện tại, có thể cho phép chỉnh sửa tùy nghiệp vụ.
   - **Mã chứng từ / Số phiếu**: Hệ thống tự động sinh (Auto-generate) hoặc nhập tay nếu được cấu hình.
   - **Ghi chú nội bộ**: Textarea để ghi chú.

2. **Cột 2: Thông tin Khách hàng / Đối tác**
   - **Mã Khách hàng / NCC**: Có tính năng tìm kiếm (Autocomplete/Combobox).
   - **Tên Khách hàng / NCC**: Tự động hiển thị theo mã, hoặc có thể nhập cho khách vãng lai.
   - **Địa chỉ**: Tự động fill theo thông tin khách hàng.
   - **Số điện thoại**: Tự động fill.
   - **Người liên hệ**: Thông tin người đại diện giao dịch.

3. **Cột 3: Thông tin Tài chính / Thanh toán**
   - **Điều khoản thanh toán**: Trả ngay, Trả sau 30 ngày, ...
   - **Phương thức thanh toán**: Tiền mặt, Chuyển khoản, Công nợ, Thẻ.
   - **Hạn thanh toán**: Ngày đáo hạn.
   - **Tiền tệ**: VND, USD, ... (kèm theo tỷ giá nếu có).
   - **Trạng thái chứng từ**: Nháp, Chờ duyệt, Đã duyệt, Đã hủy, ...

> [!NOTE]
> Bên dưới cấu trúc 3 cột Header này thường là phần **Lưới chi tiết (DataGrid/Table)** chứa danh sách hàng hóa, vật tư, hoặc chi tiết hạch toán của chứng từ.

---

## 3. State Machine (Chế độ hoạt động của Form)

Form sẽ hoạt động chặt chẽ dựa trên một **State Machine** (Máy trạng thái) với 3 trạng thái cốt lõi:

- **`VIEW` (Chế độ Xem)**: 
  - Là trạng thái mặc định khi người dùng mở một chứng từ đã tồn tại.
  - **Hành vi**: Toàn bộ các trường nhập liệu (Input, Select, DatePicker) và DataGrid đều bị `disabled` hoặc ở chế độ `read-only`. Trạng thái này tránh việc vô tình chỉnh sửa dữ liệu và đảm bảo tính vẹn toàn của chứng từ đang xem.
  
- **`ADD` (Chế độ Thêm mới)**:
  - Được kích hoạt khi người dùng nhấn nút "Thêm mới" hoặc truy cập từ menu tạo mới.
  - **Hành vi**: Form được làm trống (reset). Các trường nhập liệu được `enabled` để người dùng có thể thao tác. Các giá trị mặc định (như ngày giờ hiện tại, thông tin người dùng đang đăng nhập) được điền tự động. Khóa chính (Mã chứng từ) có thể được đánh dấu là "Tự động sinh".

- **`EDIT` (Chế độ Sửa)**:
  - Được kích hoạt khi người dùng nhấn nút "Sửa" khi đang ở trạng thái `VIEW`.
  - **Hành vi**: Dữ liệu chứng từ hiện tại được hiển thị. Hầu hết các trường nhập liệu chuyển sang trạng thái `enabled` cho phép thay đổi. Tuy nhiên, một số trường quan trọng (như Mã chứng từ, Người tạo) vẫn phải giữ nguyên `disabled` không cho phép sửa đổi theo logic nghiệp vụ.

---

## 4. Thanh Bottom Toolbar (Thanh công cụ dưới)

Mỗi Form/Tab sẽ có một thanh công cụ (Toolbar) được ghim (sticky) cố định ở phía dưới cùng (hoặc trên cùng) của màn hình. Các nút thao tác trên thanh này phụ thuộc chặt chẽ vào trạng thái (State) hiện tại của Form.

### Các nút chức năng chính:
- **Thêm (Add)**: Mở form mới, set trạng thái thành `ADD`.
- **Sửa (Edit)**: Chuyển form sang trạng thái `EDIT`.
- **Lưu (Save)**: Gửi dữ liệu (Payload) xuống backend API. Nếu thành công, set form về lại `VIEW`.
- **Hủy (Cancel)**: Hủy bỏ các thao tác đang nhập trong chế độ Thêm/Sửa, reset lại dữ liệu ban đầu, và quay về `VIEW`.
- **Xóa (Delete)**: Gọi API xóa chứng từ (yêu cầu Confirm Dialog).
- **In phiếu (Print)**: Gọi chức năng in (PDF, Excel, HTML).
- **Thoát (Exit / Close)**: Đóng Tab hiện tại.

### Liên kết với State Machine (Ma trận hiển thị):

| Nút Hành Động | Trạng thái `VIEW` | Trạng thái `ADD` | Trạng thái `EDIT` |
| --- | --- | --- | --- |
| **Thêm (F2)** | Hiển thị, Kích hoạt | Ẩn | Ẩn |
| **Sửa (F3)** | Hiển thị, Kích hoạt | Ẩn | Ẩn |
| **Lưu (F4)** | Ẩn (hoặc Disabled) | Hiển thị, Kích hoạt | Hiển thị, Kích hoạt |
| **Hủy (ESC)** | Ẩn (hoặc Disabled) | Hiển thị, Kích hoạt | Hiển thị, Kích hoạt |
| **Xóa (F8)** | Hiển thị, Kích hoạt | Ẩn | Ẩn |
| **In phiếu (F7)**| Hiển thị, Kích hoạt | Ẩn (hoặc Disabled) | Ẩn (hoặc Disabled) |
| **Thoát (F12)** | Hiển thị, Kích hoạt | Kích hoạt (kèm Confirm báo mất dữ liệu) | Kích hoạt (kèm Confirm báo mất dữ liệu) |

> [!TIP]
> Việc gán phím tắt (Hotkeys) cho các nút trên Bottom Toolbar (F2, F3, F4, ESC,...) là cực kỳ quan trọng trong trải nghiệm người dùng ERP (Power users).
