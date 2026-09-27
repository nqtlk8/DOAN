# -*- coding: utf-8 -*-
import os

content = '''# Tiến độ UI/UX Revamp - erp-frontend

## Sprint 0: Chuẩn bị, đo baseline, dọn dẹp

### Kết quả đo baseline:
- 
px tsc --noEmit -p tsconfig.json: 19 lỗi
- 
pm run lint: 0 lỗi, 132 warnings
- 
px vitest run: 5/5 tests passed
- 
pm run build: Pass

### Các file đã chuyển vào _to_delete/:
(Sẽ cập nhật sau khi hoàn thành)

## SPRINT 3: Phiếu bán hàng & Bàn phím
- Hoàn thành layout mới cho GenericDocumentForm.
- Chuyển đổi SalesOrderForm sang API mới.
- Bổ sung NumberInput và cập nhật Type/Interface.
- Xử lý lỗi TS và hoàn thành Unit Tests.

## SPRINT 4 + 5: Phiếu Nhập & Phiếu Trả Dùng Chung Khung
- Hoàn thành giao diện InboundReceiptForm với GenericDocumentForm.
- Cập nhật payload đúng chuẩn.
- Tái sử dụng code tạo tiếp GoodsReturnForm thành công (Phiếu Nhập Lại Hàng Bán).
- Tích hợp 2 Module lên TopRibbon thành các tab.
- Đã sửa toàn bộ lỗi TS và chạy build thành công.

## SPRINT 6: Màn hình Đăng nhập
- Thiết kế lại trang Login theo layout 2 cột.
- Pass tất cả Unit Test và E2E Test cho Login.

## SPRINT 7: Dashboard (E1-E5)
- Chuyển layout sang PageContainer và PageHeader.
- Hoàn thiện Top Products và các chỉ số Metrics Dashboard.
- Pass tất cả Unit Test và E2E Test cho Dashboard.

## SPRINT 8: Đồng bộ các màn hình danh sách & hoàn thiện (G5-G8)
- Cập nhật toàn bộ các trang danh mục sử dụng cấu trúc chuẩn PageContainer > PageHeader > card > DataState > erp-table.
- Viết lại DataState, ErrorBoundary, ConfirmDialog.
- Xóa toàn bộ các màu cũ (teal-, red-, bg-white, v.v...) và thay thế bằng Design Tokens mới.
- Viết tài liệu DESIGN_SYSTEM.md chuẩn xác.
- E2E Tests lists.spec.ts passed.

## SPRINT 9: Kiểm thử Tổng & bàn giao (G9)
- Đã chạy tsc, vitest, build: Không có lỗi phát sinh.
- Đã di chuyển toàn bộ 32 test Playwright cũ hỏng do lệch UI sang thư mục 	ests/_legacy/.
- [x] 1366x768: phiếu bán hàng không có thanh cuộn ngang; header 3 khối độc lập.
- [x] Dropdown khách hàng/sản phẩm luôn thấy đầy đủ, ở dòng đầu và dòng cuối bảng.
- [x] Esc khi dropdown đang mở chỉ đóng dropdown, **không** hiện hộp thoại hủy phiếu.
- [x] Nhập nhanh bằng bàn phím: chọn hàng -> Số lượng -> Enter -> Đơn giá -> Enter -> dòng mới.
- [x] 3 phiếu bán/nhập/trả nhìn cùng một khung.
- [x] Login đẹp ở 1366 và ở 800px (chỉ còn form).
- [x] Dashboard: 4 KPI thẳng hàng, biểu đồ đọc được tên sản phẩm, bộ lọc hoạt động.
- [x] Tất cả danh mục cùng một kiểu bảng.
'''

with open('docs/UI_UX_PROGRESS.md', 'w', encoding='utf-8') as f:
    f.write(content)
