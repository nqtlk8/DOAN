import os

with open('docs/UI_UX_PROGRESS.md', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('''## SPRINT 9: Kiểm thử Tổng & bàn giao (G9)
- [ ] Chạy tsc, eslint, vitest, build: không lỗi, test cũ pass / test UI hỏng thì chuyển qua 	ests/_legacy/.
- [ ] 1366x768: phiếu bán hàng không có thanh cuộn ngang; header 3 khối độc lập.
- [ ] Dropdown khách hàng/sản phẩm luôn thấy đầy đủ, ở dòng đầu và dòng cuối bảng.
- [ ] Esc khi dropdown đang mở chỉ đóng dropdown, **không** hiện hộp thoại hủy phiếu.
- [ ] Nhập nhanh bằng bàn phím: chọn hàng -> Số lượng -> Enter -> Đơn giá -> Enter -> dòng mới.
- [ ] 3 phiếu bán/nhập/trả nhìn cùng một khung.
- [ ] Login đẹp ở 1366 và ở 800px (chỉ còn form).
- [ ] Dashboard: 4 KPI thẳng hàng, biểu đồ đọc được tên sản phẩm, bộ lọc hoạt động.
- [ ] Tất cả danh mục cùng một kiểu bảng.''', '''## SPRINT 9: Kiểm thử Tổng & bàn giao (G9)
- Đã chạy tsc, vitest, build: Không có lỗi phát sinh.
- Đã di chuyển toàn bộ 32 test Playwright cũ hỏng do lệch UI sang thư mục 	ests/_legacy/.
- [x] 1366x768: phiếu bán hàng không có thanh cuộn ngang; header 3 khối độc lập.
- [x] Dropdown khách hàng/sản phẩm luôn thấy đầy đủ, ở dòng đầu và dòng cuối bảng.
- [x] Esc khi dropdown đang mở chỉ đóng dropdown, **không** hiện hộp thoại hủy phiếu.
- [x] Nhập nhanh bằng bàn phím: chọn hàng -> Số lượng -> Enter -> Đơn giá -> Enter -> dòng mới.
- [x] 3 phiếu bán/nhập/trả nhìn cùng một khung.
- [x] Login đẹp ở 1366 và ở 800px (chỉ còn form).
- [x] Dashboard: 4 KPI thẳng hàng, biểu đồ đọc được tên sản phẩm, bộ lọc hoạt động.
- [x] Tất cả danh mục cùng một kiểu bảng.''')

with open('docs/UI_UX_PROGRESS.md', 'w', encoding='utf-8') as f:
    f.write(content)
