# Test Cases - Đăng nhập & Khách hàng

## 1. Module Đăng nhập (Auth)
| ID | Tên test case | Điều kiện tiền đề | Các bước thực hiện | Kết quả mong đợi | Loại |
|---|---|---|---|---|---|
| TC-AUTH-01 | Đăng nhập thành công với quyền Admin | Có tài khoản Admin | 1. Nhập username Admin<br>2. Nhập password hợp lệ<br>3. Nhấn Đăng nhập | Đăng nhập thành công, chuyển hướng đến Dashboard | Happy path |
| TC-AUTH-02 | Đăng nhập thành công với quyền Sales | Có tài khoản Sales | 1. Nhập username Sales<br>2. Nhập password hợp lệ<br>3. Nhấn Đăng nhập | Đăng nhập thành công, tự động mở tab Bán Hàng | Happy path |
| TC-AUTH-03 | Đăng nhập thất bại - Sai mật khẩu | Có tài khoản | 1. Nhập username đúng<br>2. Nhập password sai<br>3. Nhấn Đăng nhập | Thông báo lỗi "Sai tài khoản hoặc mật khẩu" | Negative |
| TC-AUTH-04 | Đăng nhập thất bại - Bỏ trống | - | 1. Không nhập username/password<br>2. Nhấn Đăng nhập | Hiển thị lỗi validation (Vui lòng điền đủ thông tin) | Negative |
| TC-AUTH-05 | Lưu thông tin Auth vào localStorage | Có tài khoản | 1. Đăng nhập thành công | access_token và user info được lưu vào localStorage | Happy path |
| TC-AUTH-06 | Token hết hạn xử lý đúng | Đã đăng nhập | 1. API trả về 401 Unauthorized<br>2. Gọi API refresh token thất bại | Xóa localStorage, redirect về Login, hiển thị Toast báo hết hạn phiên đăng nhập | Edge case |

## 2. Module Khách hàng (Customers)
| ID | Tên test case | Điều kiện tiền đề | Các bước thực hiện | Kết quả mong đợi | Loại |
|---|---|---|---|---|---|
| TC-CUST-01 | Mở danh sách khách hàng | User đã đăng nhập | Nhấn tab Khách Hàng trên Ribbon | Hiển thị bảng danh sách khách hàng | Happy path |
| TC-CUST-02 | Thêm khách hàng thành công | Đang ở form Khách Hàng | 1. Chọn Thêm (F2)<br>2. Nhập đủ tên, mã, số điện thoại<br>3. Nhấn Lưu (F4) | Toast thành công, API nhận đúng payload Create | Happy path |
| TC-CUST-03 | Validate khi thiếu trường bắt buộc | Đang ở form Thêm mới | 1. Bỏ trống Tên khách hàng<br>2. Nhấn Lưu | Hiển thị lỗi validation, không gọi API | Negative |
| TC-CUST-04 | Xem chi tiết khách hàng | Có sẵn record | 1. Double click vào một dòng trong danh sách | Form chuyển sang VIEW, hiển thị đầy đủ thông tin khách hàng | Happy path |
| TC-CUST-05 | Chỉnh sửa thông tin khách hàng | Đang ở chế độ VIEW | 1. Nhấn Sửa (F3)<br>2. Sửa thông tin số điện thoại<br>3. Nhấn Lưu | Toast thành công, API nhận đúng payload Update | Happy path |
| TC-CUST-06 | Xóa khách hàng | Đang ở chế độ VIEW | 1. Nhấn Xóa<br>2. Xác nhận đồng ý trong dialog | API gọi xóa thành công, quay về danh sách | Happy path |
| TC-CUST-07 | Tìm kiếm khách hàng trong danh sách | Có danh sách | 1. Gõ tên vào thanh tìm kiếm | Danh sách cập nhật hiển thị đúng kết quả | Happy path |
