# Báo cáo Sprint 5 - HQ Dashboard Analytics Support
Ngày hoàn thành: 2026-09-16

## 1. Mục tiêu Sprint
Hỗ trợ lấy báo cáo tổng hợp Analytics cho HQ (Admin) nơi `branchId` bị `null`. `AnalyticsDataAdapter` trước đây dùng JDBC chuẩn khó truyền tham số động kiểu `(:branchId IS NULL OR branch_id = :branchId)` dẫn đến lỗi truy vấn khi `branchId = null`.

## 2. Thành quả đạt được
- Refactor `AnalyticsDataAdapter` sử dụng `NamedParameterJdbcTemplate` của Spring thay cho `JdbcTemplate` gốc.
- Thay thế dấu `?` thành named parameters `:branchId`, `:startDate`, `:endDate`.
- Cập nhật truy vấn SQL để bao gồm `(:branchId IS NULL OR branch_id = :branchId)`.
- Bổ sung test case cho logic truy vấn Null branchId.

## 3. Quyết định kiến trúc & Lý do
- Quyết định: Chuyển sang `NamedParameterJdbcTemplate`.
- Lý do: SQL chuẩn với `JdbcTemplate` dễ gặp lỗi khi truyền nhiều lần cùng 1 tham số (ví dụ check IS NULL) và khó đọc khi có nhiều parameter. Named parameters giúp mã nguồn sạch và tự tài liệu hóa.
- Đã cân nhắc: Build string SQL động (if/else) — bỏ vì dễ dính SQL Injection nếu không cẩn thận và code rườm rà.

## 4. Hướng dẫn đọc code theo thứ tự
1. `AnalyticsDataAdapter.java`: Xem cấu trúc `@RequiredArgsConstructor` đã đổi sang `NamedParameterJdbcTemplate` và các chuỗi SQL.
2. `AnalyticsDataAdapterTest.java`: Xác nhận test chạy tốt với `branchId = null`.

## 5. Rủi ro / Nợ kỹ thuật đã biết
- Truy vấn Dashboard hiện tại vẫn chưa tối ưu ở một số bảng snapshot như `stock_on_hand` khi replicate lên HQ.

## 6. Việc chưa làm / Out of scope
- Chưa xử lý việc `stock_on_hand` và `receivable_debt` có nguy cơ gây race condition khi đồng bộ lên HQ (sẽ xử lý ở Sprint 6).

## 7. Cách chạy & Cách verify
- Chạy unit test: `mvn test -Dtest=AnalyticsDataAdapterTest`

## 8. Điểm nối cho Sprint tiếp theo
Sẽ sửa đổi logic tính toán nợ và tồn kho cho HQ bằng cách dùng aggregation động (ON-THE-FLY) thay vì đọc bảng Snapshot được đồng bộ lên từ Branch.
