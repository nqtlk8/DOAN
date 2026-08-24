# Tài Liệu Kiểm Thử Frontend (Black-box Test Cases)

## Môi trường và Tiêu chí Kiểm thử
- **Môi trường:** Trình duyệt web (Chrome, Edge, Safari).
- **Phương pháp:** Black-box testing (Kiểm thử hộp đen), tập trung vào trải nghiệm người dùng (UX) và giao diện (UI).
- **Phạm vi:** 
  1. Giao diện & Điều hướng.
  2. Form Đơn Bán Hàng / Báo Giá.
  3. Tìm kiếm thông minh (Autocomplete).
  4. Xem trước (Print Preview) & In Hóa Đơn A5.

---

## 1. Giao Diện (UI) & Điều Hướng (Navigation)

### [TC-UI-01] - Kiểm tra tải trang mặc định và giao diện Layout
- **Các bước thực hiện:**
  1. Mở trình duyệt và truy cập vào đường dẫn của ứng dụng (Frontend URL).
  2. Quan sát giao diện tổng thể (Header, Sidebar/Menu, Content).
- **Kết quả mong đợi:** 
  - Ứng dụng tải thành công mà không có lỗi console.
  - Sidebar hoặc Menu hiển thị rõ ràng, phần nội dung chính hiển thị trang mặc định (Dashboard hoặc Tổng quan).

### [TC-UI-02] - Kiểm tra điều hướng menu
- **Các bước thực hiện:**
  1. Click vào từng mục trên thanh điều hướng (Bán hàng, Sản phẩm, Khách hàng, v.v.).
  2. Quan sát sự thay đổi của URL và nội dung hiển thị.
- **Kết quả mong đợi:**
  - URL thay đổi tương ứng.
  - Nội dung trang được tải đầy đủ, đúng chức năng.
  - Mục menu vừa click sẽ được highlight (trạng thái Active) để nhận diện trang hiện tại.

### [TC-UI-03] - Kiểm tra giao diện đáp ứng (Responsive)
- **Các bước thực hiện:**
  1. Thay đổi kích thước cửa sổ trình duyệt (thu nhỏ thành kích thước màn hình tablet, mobile).
  2. Quan sát cấu trúc giao diện và các form nhập liệu.
- **Kết quả mong đợi:**
  - Giao diện tự động căn chỉnh, không bị tràn nội dung (overflow) gây vỡ layout.
  - Các bảng (table) có thanh cuộn ngang hoặc tự thu gọn.
  - Sidebar có thể tự thu gọn thành hamburger menu trên màn hình nhỏ.

---

## 2. Form Tạo Đơn Bán Hàng / Báo Giá

### [TC-SO-01] - Thêm mới một sản phẩm vào đơn hàng hợp lệ
- **Các bước thực hiện:**
  1. Vào màn hình Tạo Đơn Bán Hàng / Báo Giá.
  2. Chọn khách hàng hợp lệ.
  3. Chọn 1 sản phẩm từ danh sách.
  4. Nhập số lượng là `2`.
- **Kết quả mong đợi:** 
  - Sản phẩm được thêm vào danh sách (Grid/Table) của đơn hàng.
  - Đơn giá hiển thị đúng, thành tiền tự động cập nhật (= số lượng * đơn giá).
  - Tổng tiền đơn hàng tăng lên tương ứng.

### [TC-SO-02] - Nhập số lượng sản phẩm là số âm hoặc chữ (Edge case)
- **Các bước thực hiện:**
  1. Tại ô số lượng của một sản phẩm trong đơn, nhập giá trị `-5`.
  2. Xóa và nhập ký tự chữ (ví dụ: `abc`).
  3. Lưu đơn hàng hoặc click ra ngoài ô nhập.
- **Kết quả mong đợi:** 
  - Hệ thống chặn không cho nhập chữ (nếu dùng number input) hoặc báo lỗi ngay lập tức.
  - Nếu nhập số âm, hệ thống tự động reset về `1` hoặc hiển thị cảnh báo đỏ "Số lượng phải lớn hơn 0".
  - Không cho phép lưu đơn hàng khi số lượng không hợp lệ.

### [TC-SO-03] - Cập nhật số tiền Khách trả trước bằng đúng Tổng tiền
- **Các bước thực hiện:**
  1. Thêm sản phẩm để có Tổng tiền (ví dụ: 500,000 VND).
  2. Tại ô "Khách thanh toán / Trả trước", nhập đúng `500,000`.
- **Kết quả mong đợi:**
  - Mục "Tiền thừa" hoặc "Công nợ còn lại" tự động tính bằng `0`.

### [TC-SO-04] - Cập nhật số tiền Khách trả trước lớn hơn Tổng tiền (Edge case)
- **Các bước thực hiện:**
  1. Tổng tiền đơn hàng đang là `500,000`.
  2. Nhập "Khách thanh toán" là `600,000`.
- **Kết quả mong đợi:**
  - Giao diện hiển thị "Tiền thối lại" (hoặc Khách dư) là `100,000`.
  - Lưu đơn thành công.

### [TC-SO-05] - Cập nhật số tiền Khách trả trước nhỏ hơn Tổng tiền (Ghi nợ)
- **Các bước thực hiện:**
  1. Tổng tiền đơn hàng đang là `500,000`.
  2. Nhập "Khách thanh toán" là `200,000`.
- **Kết quả mong đợi:**
  - Giao diện hiển thị "Công nợ" (hoặc Khách nợ) là `300,000`.
  - Khi lưu, đơn hàng ghi nhận công nợ vào tài khoản của Khách hàng đó.

### [TC-SO-06] - Lưu đơn hàng khi chưa chọn sản phẩm (Edge case)
- **Các bước thực hiện:**
  1. Điền thông tin khách hàng nhưng danh sách sản phẩm trống.
  2. Nhấn nút "Lưu đơn hàng" (hoặc "Tạo đơn").
- **Kết quả mong đợi:**
  - Hệ thống ngăn chặn hành động lưu.
  - Hiển thị thông báo (Toast/Alert): "Vui lòng chọn ít nhất 1 sản phẩm trước khi tạo đơn".

### [TC-SO-07] - Chuyển đổi trạng thái từ Báo giá sang Đơn bán hàng
- **Các bước thực hiện:**
  1. Tạo thành công một Đơn Báo Giá (Pre-order).
  2. Vào xem chi tiết đơn báo giá đó và nhấn nút "Chuyển thành Đơn Bán Hàng".
- **Kết quả mong đợi:**
  - Hệ thống hiển thị form xác nhận hoặc chuyển thẳng trạng thái thành Đơn Bán Hàng.
  - Tổng số lượng tồn kho của các sản phẩm tương ứng bị trừ đi.

---

## 3. Chức Năng Tìm Kiếm Thông Minh (Autocomplete)

### [TC-AC-01] - Tìm kiếm Khách hàng/Sản phẩm bằng từ khóa hợp lệ
- **Các bước thực hiện:**
  1. Click vào ô tìm kiếm Khách hàng (hoặc Sản phẩm).
  2. Gõ từ khóa đúng (ví dụ: "Nguyễn Văn A" hoặc mã sản phẩm "SP01").
  3. Dừng gõ một lúc.
- **Kết quả mong đợi:**
  - Hiển thị dropdown liệt kê danh sách kết quả chứa từ khóa tìm kiếm (Tên hoặc Số điện thoại/Mã SKU).
  - Tốc độ phản hồi nhanh (Debounce hoạt động tốt).

### [TC-AC-02] - Tìm kiếm với từ khóa không tồn tại
- **Các bước thực hiện:**
  1. Gõ một chuỗi ký tự bất kỳ không có trong cơ sở dữ liệu (ví dụ: "XQZ999").
- **Kết quả mong đợi:**
  - Dropdown hiển thị thông báo "Không tìm thấy kết quả phù hợp" (No results found).

### [TC-AC-03] - Điều hướng kết quả bằng Bàn phím (Keyboard Navigation)
- **Các bước thực hiện:**
  1. Gõ từ khóa để dropdown hiển thị danh sách gợi ý.
  2. Dùng phím `Mũi tên xuống` để di chuyển highlight xuống các kết quả bên dưới.
  3. Dùng phím `Mũi tên lên` để di chuyển ngược lại.
  4. Nhấn phím `Enter` tại một kết quả đang được highlight.
- **Kết quả mong đợi:**
  - Kết quả được chọn và điền vào ô input. Dropdown đóng lại ngay lập tức.
  - (Đối với Sản phẩm) Sản phẩm đó được add luôn vào danh sách bên dưới.

### [TC-AC-04] - Xóa ký tự và thử lại (Backspace)
- **Các bước thực hiện:**
  1. Gõ từ khóa "Điện thoại".
  2. Sau khi có kết quả, nhấn phím Backspace để xóa dần còn "Điện".
- **Kết quả mong đợi:**
  - Dropdown tự động cập nhật lại danh sách kết quả để phù hợp với từ khóa mới ("Điện").

---

## 4. Xem Trước (Print Preview) & In Hóa Đơn (A5)

### [TC-PR-01] - Mở và đóng Modal Print Preview
- **Các bước thực hiện:**
  1. Tại chi tiết Đơn bán hàng (đã có dữ liệu), nhấn nút "In hóa đơn" hoặc "Print Preview".
  2. Quan sát Modal xuất hiện.
  3. Nhấn phím `ESC` hoặc click nút `X` hoặc click ra ngoài vùng xám (overlay).
- **Kết quả mong đợi:**
  - Modal mở ra mượt mà, hiển thị bản xem trước của hóa đơn.
  - Khi thực hiện bước 3, Modal đóng lại mà không ảnh hưởng tới trạng thái trang hiện tại.

### [TC-PR-02] - Kiểm tra dữ liệu trên Print Preview (Đặc biệt: Ẩn giá tiền)
- **Các bước thực hiện:**
  1. Mở Print Preview của một đơn hàng có Tổng tiền là 500,000.
  2. Kiểm tra thông tin hiển thị trên bản preview.
- **Kết quả mong đợi:**
  - (Theo yêu cầu): Chỉ hiển thị Tên khách, thông tin nhận hàng, Danh sách tên sản phẩm, Mã SKU và Số lượng.
  - **Tuyệt đối KHÔNG hiển thị Đơn giá và Tổng tiền** trên phiếu in giao hàng.

### [TC-PR-03] - Mở Print Preview khi đơn hàng chưa có sản phẩm (Edge case)
- **Các bước thực hiện:**
  1. Đang ở màn hình tạo đơn mới (chưa lưu, chưa có sản phẩm).
  2. Nhấn nút Print Preview (nếu nút được enable).
- **Kết quả mong đợi:**
  - Nếu nút không bị disable, Modal hiện ra báo lỗi "Không có sản phẩm nào để in" hoặc một bản xem trước hoàn toàn trống.
  - UX tốt nhất: Nút Print nên bị vô hiệu hóa (Disabled) hoặc ẩn đi khi chưa có sản phẩm/chưa lưu.

### [TC-PR-04] - Kiểm tra layout khi nhấn In (Print) khổ A5
- **Các bước thực hiện:**
  1. Trong Modal Print Preview, nhấn nút "In".
  2. Hộp thoại in của trình duyệt (Browser Print Dialog) bật lên.
  3. Chọn khổ giấy là A5.
- **Kết quả mong đợi:**
  - Nội dung hóa đơn nằm vừa vặn trên 1 trang A5 (hoặc chia trang đẹp mắt nếu danh sách dài).
  - Không in ra các thành phần giao diện của ứng dụng (ẩn thanh sidebar, menu, nút bấm).
  - Phông chữ dễ đọc, căn lề chuẩn theo A5.
