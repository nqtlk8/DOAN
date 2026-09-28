# Test Cases - Quản lý Bán hàng (Sales)

## A. Bối cảnh & Phạm vi
Chia chức năng Bán hàng thành 5 nhóm:
1. Mở module và khởi tạo phiếu bán hàng
2. Chọn khách hàng
3. Thêm/chỉnh sửa sản phẩm và tính tiền
4. Lưu hóa đơn nháp
5. Xem danh sách và xem chi tiết hóa đơn

## B. Nhóm 1: Mở module và khởi tạo phiếu bán hàng
| ID | Tên test case | Điều kiện tiền đề | Các bước thực hiện | Kết quả mong đợi | Loại |
|---|---|---|---|---|---|
| TC-SALE-01 | Mở chức năng Bán hàng | User đăng nhập, có quyền ERP | Mở tab Chức năng -> Chọn Bán Hàng | Tab Bán Hàng mở; form mode ADD trống | Happy path |
| TC-SALE-02 | Form bán hàng khởi tạo đúng giá trị mặc định | Đã mở Bán Hàng | Kiểm tra form | Mã phiếu AUTO-GENERATE; thanh toán CASH; ngày lập = hiện tại | Happy path |
| TC-SALE-03 | Tạo phiếu mới từ nút Thêm | Đang ở form VIEW | Nhấn Thêm | Form chuyển sang ADD; reset khách hàng & sản phẩm | Happy path |
| TC-SALE-04 | Hủy phiếu mới và xác nhận | Mode ADD, có dữ liệu | Hủy/Thoát -> Xác nhận | Phiếu đóng/quay lại view | Happy path |
| TC-SALE-05 | Hủy phiếu mới nhưng chọn Không | Mode ADD, có dữ liệu | Hủy -> Không | Dialog đóng; giữ nguyên dữ liệu | Edge case |
| TC-SALE-06 | Thoát khỏi phiếu mới có dữ liệu chưa lưu | Mode ADD | Nhập liệu -> Thoát | Cảnh báo mất thay đổi | Edge case |
| TC-SALE-07 | Thoát khỏi form VIEW | Đang xem hóa đơn | Nhấn Thoát | Đóng tab trực tiếp, không hỏi | Happy path |

## C. Nhóm 2: Chọn khách hàng
| ID | Tên test case | Điều kiện tiền đề | Các bước thực hiện | Kết quả mong đợi | Loại |
|---|---|---|---|---|---|
| TC-SALE-08 | Mở tìm kiếm khách hàng | Form ADD | Click ô khách hàng | Combobox hiển thị | Happy path |
| TC-SALE-09 | Tìm khách hàng theo tên | API trả kết quả | Nhập "Nguyễn" | Hiển thị list phù hợp | Happy path |
| TC-SALE-10 | Tìm khách hàng không có kết quả | API trả [] | Nhập chuỗi sai | Trạng thái không có dữ liệu, không crash | Edge case |
| TC-SALE-11 | API tìm khách hàng lỗi | API trả 500 | Nhập từ khóa | Hiển thị lỗi, form vẫn usable | Negative |
| TC-SALE-12 | API tìm khách hàng loading | API delay | Nhập từ khóa | UI hiển thị loading | Edge case |
| TC-SALE-13 | Chọn khách hàng | API trả hợp lệ | Chọn customer | Điền tên & thông tin liên quan | Happy path |
| TC-SALE-14 | Giữ đúng Customer ID nội bộ | Có UUID | Chọn customer -> submit | Request dùng customer.id, không dùng code | Negative |
| TC-SALE-15 | Customer không có phone/address | Thiếu optional fields | Chọn customer | Không crash, để trống field thiếu | Edge case |
| TC-SALE-16 | Bỏ customer sau khi đã chọn | Đã chọn | Xóa lựa chọn | Trở về trạng thái trống | Edge case |

## D. Nhóm 3: Thêm sản phẩm
| ID | Tên test case | Điều kiện tiền đề | Các bước thực hiện | Kết quả mong đợi | Loại |
|---|---|---|---|---|---|
| TC-SALE-17 | Thêm một dòng sản phẩm | Mode ADD | Nhấn thêm dòng | 1 dòng mới, SL=1, giá=0 | Happy path |
| TC-SALE-18 | Tìm sản phẩm | API trả data | Mở combobox -> nhập từ khóa | Hiển thị list | Happy path |
| TC-SALE-19 | API sản phẩm trả lỗi | API 500 | Tìm sản phẩm | Hiển thị lỗi, không crash | Negative |
| TC-SALE-20 | Không tìm thấy sản phẩm | API trả [] | Nhập sai | Hiển thị rỗng | Edge case |
| TC-SALE-21 | Chọn sản phẩm | Hợp lệ | Chọn sản phẩm | Fill productId, tên, đơn giá | Happy path |
| TC-SALE-22 | Xóa dòng sản phẩm | > 0 dòng | Nhấn xóa dòng | Loại khỏi form, update tổng tiền | Happy path |
| TC-SALE-23 | Xóa hết sản phẩm | Nhiều dòng | Xóa hết | List rỗng, tổng tiền = 0 | Edge case |
| TC-SALE-24 | Thêm nhiều sản phẩm | Form cho phép | Thêm 3 sp khác nhau | Độc lập dữ liệu từng dòng | Happy path |

## E. Nhóm 4: Số lượng và tính tiền
| ID | Tên test case | Điều kiện tiền đề | Các bước thực hiện | Kết quả mong đợi | Loại |
|---|---|---|---|---|---|
| TC-SALE-25 | Nhập số lượng hợp lệ | Có sản phẩm | SL = 5 | Cập nhật tổng = giá * 5 | Happy path |
| TC-SALE-26 | Quantity bằng 0 | Có sản phẩm | Nhập 0 | Báo lỗi, chặn submit | Negative |
| TC-SALE-27 | Quantity âm | Có sản phẩm | Nhập -1 | Báo lỗi, chặn submit | Negative |
| TC-SALE-28 | Quantity rỗng | Có sản phẩm | Xóa quantity | Báo lỗi hoặc chặn | Negative |
| TC-SALE-29 | Quantity thập phân | Có hỗ trợ | Nhập 1.5 | Xử lý đúng business | Edge case |
| TC-SALE-30 | Đơn giá bằng 0 | Có sản phẩm | Giá = 0 | Tổng = 0, không crash | Edge case |
| TC-SALE-31 | Tổng tiền nhiều dòng | >1 dòng | Nhập SL, giá | Tổng = sum(line total) | Happy path |
| TC-SALE-32 | Xóa sản phẩm cập nhật tổng tiền | Nhiều dòng | Xóa 1 dòng | Tính lại đúng tổng | Happy path |

## F. Nhóm 5: Validation trước khi lưu
| ID | Tên test case | Điều kiện tiền đề | Các bước thực hiện | Kết quả mong đợi | Loại |
|---|---|---|---|---|---|
| TC-SALE-33 | Lưu khi chưa chọn khách hàng | Form trống khách | Lưu | Không gọi API, báo lỗi | Negative |
| TC-SALE-34 | Lưu khi chưa có sản phẩm | Form 0 items | Lưu | Không gọi API, báo lỗi | Negative |
| TC-SALE-35 | Một dòng chưa chọn sản phẩm | Có line, ko product | Lưu | Báo lỗi tại dòng đó | Negative |
| TC-SALE-36 | Một dòng quantity = 0 | Có line | Nhập Q=0 -> Lưu | Không gửi API, báo lỗi | Negative |
| TC-SALE-37 | Nhiều dòng có lỗi | >1 line lỗi | Lưu | Hiển thị tất cả lỗi | Negative |
| TC-SALE-38 | Dữ liệu hợp lệ | Đúng chuẩn | Lưu | Cho phép gọi API | Happy path |

## G. Nhóm 6: API Contract
| ID | Tên test case | Điều kiện tiền đề | Các bước thực hiện | Kết quả mong đợi | Loại |
|---|---|---|---|---|---|
| TC-SALE-39 | Payload có invoiceCode | Hợp lệ | Submit | Chứa invoiceCode | Happy path |
| TC-SALE-40 | Payload dùng lines | Có sản phẩm | Submit | Gửi lines thay vì items | Negative |
| TC-SALE-41 | Payload dùng UUID customer | Đã chọn customer | Submit | customerId === id | Negative |
| TC-SALE-42 | Payload có productName | Đã chọn product | Submit | Mỗi line có productName | Negative |
| TC-SALE-43 | Payload có unitOfMeasure | Đã chọn product | Submit | Mỗi line có UOM | Negative |
| TC-SALE-44 | Không gửi field thừa | Hợp lệ | Submit | Không có discount, etc | Negative |
| TC-SALE-45 | Không gửi field items | Hợp lệ | Submit | Không có items | Negative |
| TC-SALE-46 | API trả 201 | Mock 201 | Submit | Toast success, sang VIEW | Happy path |
| TC-SALE-47 | API trả 400 | Mock 400 | Submit | Báo lỗi, giữ form | Negative |
| TC-SALE-48 | API trả 500 | Mock 500 | Submit | Báo lỗi, giữ form | Negative |
| TC-SALE-49 | Network error | Fail | Submit | Báo lỗi mạng | Edge case |
| TC-SALE-50 | Double-click Lưu | API delay | Click nhanh | Chỉ 1 request được gửi | Edge case |

## H. Nhóm 7: Xử lý response Create
| ID | Tên test case | Điều kiện tiền đề | Các bước thực hiện | Kết quả mong đợi | Loại |
|---|---|---|---|---|---|
| TC-SALE-51 | Response trả UUID | Mock data=UUID | Submit | Frontend lấy đúng data | Happy path |
| TC-SALE-52 | Không đọc sai response shape | Mock | Submit | Không fail khi lấy response.id thay vì UUID | Negative |
| TC-SALE-53 | Success thiếu data | data=null | Submit | Xử lý an toàn | Edge case |
| TC-SALE-54 | Success=false (HTTP 200) | Mock | Submit | UI xử lý như lỗi | Negative |

## I. Nhóm 8: Xem danh sách
| ID | Tên test case | Điều kiện tiền đề | Các bước thực hiện | Kết quả mong đợi | Loại |
|---|---|---|---|---|---|
| TC-SALE-55 | Hiển thị danh sách | API trả data | Mở list | Hiện bảng đúng | Happy path |
| TC-SALE-56 | Danh sách rỗng | API [] | Mở list | Empty state | Edge case |
| TC-SALE-57 | API danh sách lỗi | API 500 | Mở list | Error state + Retry | Negative |
| TC-SALE-58 | Loading danh sách | Delay | Mở list | Loading state | Edge case |
| TC-SALE-59 | Retry sau lỗi | 500 -> 200 | Retry | Hiện lại danh sách | Happy path |
| TC-SALE-60 | Double click | Có record | Click đúp | Mở form VIEW detail | Happy path |
| TC-SALE-61 | API chi tiết lỗi | GET 500 | Click đúp | Toast lỗi, không crash | Negative |
| TC-SALE-62 | Search hóa đơn | Có record | Nhập từ khóa | Thay đổi danh sách | Edge case |
| TC-SALE-63 | Filter trạng thái | Đa dạng | Chọn filter | Lọc đúng | Edge case |

## J. Nhóm 9: Xem chi tiết
| ID | Tên test case | Điều kiện tiền đề | Các bước thực hiện | Kết quả mong đợi | Loại |
|---|---|---|---|---|---|
| TC-SALE-64 | Mở chi tiết | GET trả invoice | Double click | Form VIEW hiển thị đúng | Happy path |
| TC-SALE-65 | Chi tiết nhiều dòng | 5 lines | Mở chi tiết | Hiện đủ 5 lines | Happy path |
| TC-SALE-66 | Chi tiết customer null | API trả null customer | Mở detail | Không crash | Edge case |
| TC-SALE-67 | Response thiếu field | API thiếu field | Mở detail | Xử lý an toàn | Edge case |

## K. Nhóm 10: Trạng thái giao dịch
| ID | Tên test case | Điều kiện tiền đề | Các bước thực hiện | Kết quả mong đợi | Loại |
|---|---|---|---|---|---|
| TC-SALE-68 | Hiển thị DRAFT | Status=DRAFT | Mở detail | Hiển thị "Nháp" | Happy path |
| TC-SALE-69 | Hiển thị CONFIRMED | Status=CONFIRMED | Mở detail | Hiển thị "Đã xác nhận" | Happy path |
| TC-SALE-70 | Hiển thị CANCELLED | Status=CANCELLED | Mở detail | Hiển thị "Đã hủy" | Edge case |
| TC-SALE-71 | Status bất thường | Status lạ | Mở detail | Fallback, không crash | Edge case |

## L. Nhóm 11: In hóa đơn
| ID | Tên test case | Điều kiện tiền đề | Các bước thực hiện | Kết quả mong đợi | Loại |
|---|---|---|---|---|---|
| TC-SALE-72 | Mở preview | Có data | Nhấn In | Modal preview hiện | Happy path |
| TC-SALE-73 | Đóng preview | Preview mở | Đóng | Đóng modal | Happy path |
| TC-SALE-74 | Gọi print | Preview mở | In ngay | window.print() được gọi | Happy path |
| TC-SALE-75 | Print không có item | Hóa đơn trống | Nhấn In | Không crash | Edge case |
