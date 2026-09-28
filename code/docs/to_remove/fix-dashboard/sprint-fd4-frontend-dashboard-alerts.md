Ngày hoàn thành: 2026-09-28
Mã sprint: FD-4

## 1. Mục tiêu Sprint (Goal)
Cập nhật Frontend (React) để hiển thị Dashboard hoàn chỉnh theo API mới từ backend. Sprint này tập trung vào việc bổ sung panel cảnh báo tồn kho, tích hợp danh sách sản phẩm bán chậm, sửa tên các thẻ hiển thị (tổng công nợ phải thu), và hỗ trợ tự động làm mới dữ liệu sau mỗi 60 giây. Sprint này tiếp nối phần API đã làm ở FD-3.

## 2. Thành quả đạt được (Done)
- Cập nhật kiểu dữ liệu liên quan đến Analytics (bổ sung `ProductPerformanceDto`, `SlowMovingProductDto`, `StockAlertDto`...). File: `code/erp-platform/apps/erp-frontend/src/types/analytics.ts`.
- Bổ sung `getStockAlerts` vào API service. File: `code/erp-platform/apps/erp-frontend/src/api/ApiService.ts`.
- Thêm file cấu hình chu kỳ tự làm mới dashboard và map hiển thị mức cảnh báo. File: `code/erp-platform/apps/erp-frontend/src/config/dashboard.ts`.
- Tạo 2 hook dùng `useQuery` để lấy metrics và cảnh báo tồn kho. File: `code/erp-platform/apps/erp-frontend/src/hooks/useDashboardMetrics.ts` và `code/erp-platform/apps/erp-frontend/src/hooks/useStockAlerts.ts`.
- Tạo mới UI component hiển thị bảng cảnh báo tồn kho (`NEGATIVE_STOCK` và `LOW_STOCK`). File: `code/erp-platform/apps/erp-frontend/src/components/sales/StockAlertPanel.tsx`.
- Tích hợp 2 hook mới, đổi tên nhãn "Công nợ quá hạn" thành "Tổng công nợ phải thu", bổ sung bảng sản phẩm bán chậm và label hiển thị giờ cập nhật. File: `code/erp-platform/apps/erp-frontend/src/components/sales/Dashboard.tsx`.
- Viết test Vitest cho component cảnh báo. File: `code/erp-platform/apps/erp-frontend/src/components/sales/__tests__/StockAlertPanel.test.tsx`.
- Cập nhật test UI (Playwright) với mock API mới và assert text bảng cảnh báo. File: `code/erp-platform/apps/erp-frontend/tests/ui/dashboard.spec.ts`.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions)
- Quyết định: Sử dụng 2 query (và 2 custom hooks: `useDashboardMetrics` và `useStockAlerts`) độc lập để gọi 2 API.
  Lý do: Để tách biệt lỗi. Nếu API cảnh báo tồn kho có vấn đề, lỗi sẽ chỉ hiển thị ở panel cảnh báo, không làm hỏng hiển thị của toàn bộ khối KPI metrics. Hơn nữa cảnh báo tồn kho không phụ thuộc vào kỳ báo cáo (startDateKey, endDateKey) nên có bộ param (queryKey) hoàn toàn khác với KPIs.
  Đã cân nhắc: Gộp 2 API thành 1 query lớn trên frontend bằng Promise.all. Bỏ vì nếu 1 API bị lỗi sẽ làm sập (hoặc buộc phải xử lý rườm rà) cả phần UI.
- Quyết định: Cài đặt chu kỳ tự động refresh 60 giây (M-09) trực tiếp thông qua thuộc tính `refetchInterval` của TanStack Query và tắt `refetchIntervalInBackground`.
  Lý do: Đảm bảo dashboard tự cập nhật cho người dùng (đặc biệt khi đang cắm màn hình rời), mà không gây tốn tài nguyên hoặc spam request ngầm khi họ đang chuyển sang tab khác.

## 4. Hướng dẫn đọc code theo thứ tự
1. Đọc `code/erp-platform/apps/erp-frontend/src/types/analytics.ts` để biết các DTO trả về từ backend hiện tại.
2. Đọc `code/erp-platform/apps/erp-frontend/src/config/dashboard.ts` để xem cấu hình tự động làm mới và map CSS lớp cho UI cảnh báo.
3. Đọc 2 file hook trong `code/erp-platform/apps/erp-frontend/src/hooks/use*.ts` để xem cách `refetchInterval` được áp dụng vào TanStack Query.
4. Xem thiết kế thẻ hiển thị cảnh báo tại `code/erp-platform/apps/erp-frontend/src/components/sales/StockAlertPanel.tsx`.
5. Cuối cùng, xem cách `Dashboard.tsx` (tại thư mục tương ứng) ghép các component lại và hiển thị bảng sản phẩm bán chậm.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Các file test cũ (thư mục `tests/_legacy/`) được update "chữa cháy" (tìm và thay tên `totalOverdueDebt` thành `totalReceivableDebt`) để vượt qua điều kiện kiểm tra (lỗi không còn reference tới tên cũ), nhưng chúng có thể không chạy ổn định nếu không được maintain kĩ.
- Việc tính "Sản phẩm bán chậm" có thể gây lấp đầy giao diện hoặc kéo dài chiều dọc trang Dashboard nếu số lượng bản ghi trả ra bị trôi lớn (dù API backend đã limit top 10). Nếu backend không giới hạn, màn hình sẽ bị quá tải DOM node. Nên xử lý pagination ở sprint sau nếu thiết kế mở rộng.

## 6. Việc chưa làm / Out of scope
- Không thêm tính năng thông báo (Websocket / Toast / Push Notification) cho client về cảnh báo tồn kho khi xảy ra thay đổi theo thời gian thực (real-time notification là ngoài scope, chỉ hỗ trợ periodic refetch 60s).
- Không thêm tính năng phân trang (pagination) cho bảng cảnh báo và sản phẩm bán chậm (hiện đang dùng list nguyên bản).

## 7. Cách chạy & Cách verify
Tại thư mục `code/erp-platform/apps/erp-frontend`:
- **Chạy Linter:**
  ```bash
  npm run lint
  ```
  Kết quả mong đợi: Pass (hoặc 0 errors, vài warnings tuỳ phiên bản eslint rules không ảnh hưởng build).
- **Chạy Typecheck & Build:**
  ```bash
  npm run build
  ```
  Kết quả mong đợi: Build script kết thúc thành công mà không gặp lỗi TypeScript.
- **Chạy Vitest (Component Test):**
  ```bash
  npx vitest run src/components/sales/__tests__/StockAlertPanel.test.tsx
  ```
  Kết quả mong đợi: Các test renders trạng thái load, error, không có cảnh báo và có cảnh báo đều xanh (pass).
- **Chạy E2E (Playwright Test):**
  ```bash
  npm run test:e2e
  ```
  Kết quả mong đợi: (Tùy thuộc vào việc môi trường có hỗ trợ Playwright không), các mock và assert hiển thị UI sẽ pass thành công.

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- API, test và frontend đều đã hoàn tất đối với việc hiển thị Cảnh báo Tồn Kho và Fix chỉ số Dashboard (lỗi Postgres 500, thêm replication...).
- Sprint FD-5 tiếp theo cần ghép chạy End-to-End toàn bộ hệ thống bằng Docker, đặc biệt thử nghiệm tính năng seed replication giữa nhánh và HQ và kiểm tra thực tế trên luồng tích hợp của app. Nên bắt đầu bằng việc đọc kĩ `code/scripts/` liên quan tới replicate snapshot table.

## Checklist
- [x] 1. Mục tiêu Sprint
- [x] 2. Thành quả đạt được (có trỏ đường dẫn file cụ thể)
- [x] 3. Quyết định kiến trúc & Lý do (có nêu phương án đã bỏ + lý do)
- [x] 4. Hướng dẫn đọc code theo thứ tự
- [x] 5. Rủi ro / Nợ kỹ thuật đã biết
- [x] 6. Việc chưa làm / Out of scope
- [x] 7. Cách chạy & Cách verify
- [x] 8. Điểm nối cho Sprint tiếp theo
- [x] Đã thêm link vào `code/docs/fix-dashboard/README.md`
- [x] Không có tính từ mơ hồ tự đánh giá chất lượng
