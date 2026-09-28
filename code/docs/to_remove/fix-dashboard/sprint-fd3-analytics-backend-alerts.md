# 2026-09-28 Sprint FD-3

## 1. Mục tiêu Sprint (Goal)
- Implement backend logic tính toán các chỉ số Dashboard: Gross Profit, Inventory Turnover, Slow Moving Products, và tổng nợ phải thu.
- Tách riêng API xuất ra danh sách cảnh báo tồn kho (âm hoặc sắp hết) độc lập với KPI dashboard.
- Sprint này xây dựng trên kết quả của sprint FD-2 (Replicate `stock_on_hand` và `receivable_debt`).

## 2. Thành quả đạt được (Done)
- Cập nhật Data adapter tính toán trực tiếp từ `stock_movement` và `receivable_debt` (với query tổng hợp tránh race-condition). File: `AnalyticsDataPort.java`, `AnalyticsDataAdapter.java`.
- Cập nhật logic Dashboard KPI: tính tỉ lệ vòng quay tồn kho (M-01) và danh sách sản phẩm bán chậm (M-02). File: `DashboardService.java`.
- Đổi tên trường DTO từ `totalOverdueDebt` sang `totalReceivableDebt` (⚠️ BREAKING). File: `DashboardMetricsDto.java`, `ReportExportService.java`.
- Tạo Enum các mức cảnh báo và DTO. File: `StockAlertType.java`, `StockAlertDto.java`, `StockAlertSummaryDto.java`.
- Thêm query lọc cảnh báo kho đang hoạt động theo branch. File: `InventoryAlertConfigRepository.java`.
- Logic tạo và sinh thông tin cảnh báo tồn kho với sự phân bổ ưu tiên giữa tồn âm và sắp hết. File: `StockAlertService.java`.
- Endpoint mới cho phép Frontend kéo cảnh báo định kỳ. File: `StockAlertController.java`.
- Viết Unit Test và Integration Test PostgreSQL cho các module kể trên. Sinh lại `openapi.json` tự động. File: `StockAlertServiceTest.java`, `DashboardServiceTest.java`, `AnalyticsDataAdapterPostgresIT.java`, `StockAlertApiPostgresIT.java`.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
Quyết định: API cảnh báo tách riêng `GET /api/v1/analytics/stock-alerts` (M-12)
Lý do: Tách lỗi/refresh độc lập với KPI. Cảnh báo không phụ thuộc vào kỳ báo cáo (start/endDate) mà lấy trực tiếp tại thời điểm gọi.
Đã cân nhắc: Gộp chung cảnh báo vào DashboardMetricsDto — bỏ vì sẽ bắt các query phải đợi nhau, hơn nữa các màn Dashboard có ngày bắt đầu và kết thúc lọc không cần áp dụng lên cảnh báo.

Quyết định: Tính cảnh báo trực tiếp từ `stock_movement` thay vì snapshot `stock_on_hand` (D-07).
Lý do: `stock_movement` là bảng append-only, tính `SUM(quantity)` chính xác cho mọi thay đổi lịch sử và không có nguy cơ nhận dữ liệu thiếu hoặc cập nhật chồng/race conditions nếu việc replicate từ branch chưa hoàn tất.
Đã cân nhắc: Truy xuất trực tiếp từ `stock_on_hand` — bỏ vì `stock_on_hand` được tạo ra để đọc nhanh trên lưới tồn kho, nếu có delay trong quá trình replicate snapshot, cảnh báo tồn kho có thể bị lỡ nhịp.

Quyết định: Xóa `LowStockAlertJob` và không dùng scheduling.
Lý do: Không có đối tượng nào tiêu thụ kết quả cũ, API được pull trực tiếp trên Dashboard (M-10).
Đã cân nhắc: Giữ job — bỏ vì job rỗng làm tốn bộ nhớ và không người sử dụng.

Quyết định: Không JOIN chéo Context giữa Stock Movement và Analytics Configuration.
Lý do: Bám sát thiết kế DDD Bounded Context, các thông tin định danh sản phẩm, chi nhánh được làm giàu (Enrich) thông qua Map trong Service thay vì gọi SQL JOIN để đảm bảo khả năng tách Service rời rạc khi mở rộng.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. `AnalyticsDataAdapter.java`: Xem các thay đổi trong native query, đặc biệt là cách lấy công nợ, tổng hợp quantity và doanh thu COGS trong cùng một truy vấn.
2. `DashboardService.java`: Nơi các số liệu thô từ Adapter được tính ra `inventoryTurnoverRatio` và Top Products.
3. `StockAlertService.java`: Logic chính của phần cảnh báo: gom nhóm số tồn, duyệt qua các cấu hình và phát sinh `NEGATIVE_STOCK`, `LOW_STOCK` và enrich data.
4. `StockAlertController.java`: Endpoint.
5. Unit tests & IT tests (Postgres).

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Giá vốn của hàng bán bị trả lại (Sales Return) chưa được bù trừ vào lợi nhuận gộp trong kỳ, nên nếu chi nhánh có số lượng đổi/trả nhiều thì Gross Profit có thể sẽ không khớp 100% logic kế toán. Có thể xem xét điều chỉnh ở phase sau.
- Việc tính "Slow Moving Products" đòi hỏi quét qua tất cả Product IDs không nằm trong tập bán ra, nếu database hàng hoá phình quá to thì sẽ phải phân trang giới hạn thay vì duyệt limit 10 trong app tier.

## 6. Việc chưa làm / Out of scope
- Frontend Dashboard và Auto-refresh (dành cho FD-4).
- Cảnh báo qua email, chỉ hiển thị UI (D-01).

## 7. Cách chạy & Cách verify (Reproduce)
- Chạy unit test backend: `.\mvnw.cmd test`
- Chạy integration test backend: `.\mvnw.cmd verify`
- Test kết quả sẽ pass toàn bộ:
  Sprint FD-3 không lưu lại log khi kết thúc. Kết quả chạy gần nhất được ghi ở FD-5: `Tests run: 147, Failures: 0, Errors: 2, Skipped: 1` — 2 lỗi do Testcontainers không kết nối được Docker, nên các IT (`AnalyticsDataAdapterPostgresIT`, `StockAlertApiPostgresIT`) chưa từng chạy. Review ngày 2026-09-28 phát hiện fixture của 2 IT này sai schema; đã sửa ở FD-6 (xem `sprint-fd6-review-fixes.md`).

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Sprint tiếp theo (FD-4) sẽ tiến hành tích hợp Dashboard UI React và StockAlertPanel.
- Phía UI cần thay đổi tên thuộc tính `totalOverdueDebt` -> `totalReceivableDebt`.
- Có API `/api/v1/analytics/stock-alerts` sẵn sàng để `useQuery` với `refetchInterval = 60_000` (theo timer).

---
**Checklist tự kiểm (bổ sung ở FD-6):**
- [x] 1. Mục tiêu Sprint
- [x] 2. Thành quả đạt được (có trỏ đường dẫn file cụ thể)
- [x] 3. Quyết định kiến trúc & Lý do (có nêu phương án đã bỏ + lý do)
- [x] 4. Hướng dẫn đọc code theo thứ tự
- [x] 5. Rủi ro / Nợ kỹ thuật đã biết
- [x] 6. Việc chưa làm / Out of scope
- [x] 7. Cách chạy & Cách verify (bổ sung ở FD-6; IT chưa chạy được lúc kết thúc FD-3)
- [x] 8. Điểm nối cho Sprint tiếp theo
- [x] Đã thêm link vào `code/docs/fix-dashboard/README.md`
- [x] Không có tính từ mơ hồ tự đánh giá chất lượng
