---
date: 2026-09-17
---

# Sprint 14: Cleanup & Documentation

## 1. Mục tiêu Sprint (Goal)
- Rà soát và loại bỏ các đoạn code dư thừa (dead code), code trùng lặp hoặc các log hệ thống không đúng chuẩn.
- Đóng gói toàn bộ tài liệu (Documentation) để chuẩn bị cho giai đoạn bàn giao hệ thống ERP (Handoff) cho đội ngũ vận hành.

## 2. Thành quả đạt được (Done)
- Đã xóa bỏ file `DashboardApi.ts` trong thư mục `apps/erp-frontend/src/services/` do là code trùng lặp (duplicate) không còn được sử dụng.
- Đã loại bỏ các lệnh `console.log` ở màn hình và context liên quan đến Authentication (UI Login) để dọn dẹp log rác ở console trình duyệt.
- Cập nhật `LogExecutionTimeAspect.java` tại Backend để thay thế lệnh `System.out.println` cũ bằng chuẩn logging `@Slf4j` (`log.info()`), tuân thủ quy tắc "Lean Log System Strategy".
- Chuẩn hóa và tổng hợp toàn bộ lộ trình tài liệu vào `docs/sprints/README.md`.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
Quyết định: Không áp dụng xử lý các Tech Debt kiến trúc diện rộng (ví dụ: gộp chung `CustomerRepository` vào `CrmFacade` hay refactor TypeScript Interface toàn diện cho phần chưa chạm tới) tại Sprint này.
Lý do: Để đảm bảo an toàn, hạn chế tối đa các thay đổi cấu trúc ở những function đang hoạt động ổn định (If it ain't broke, don't fix it) ngay sát thềm bàn giao hệ thống. Các thay đổi kiến trúc cần được lập kế hoạch vào Phase sau thay vì vội vàng ở Sprint tổng kết.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. `LogExecutionTimeAspect.java`: Xem cách chuyển đổi từ `System.out` sang `Slf4j`.
2. `docs/sprints/README.md`: Index tài liệu tổng quan toàn bộ tiến trình 14 Sprints.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Các tồn đọng kiến trúc chưa giải quyết trong Sprint 14: 
  - Backend Controller đang inject xen kẽ cả Facade và Repository trực tiếp.
  - Phân trang (Pagination) chưa được áp dụng cho màn hình Chi tiết sao kê công nợ.
- Đây sẽ là những gợi ý tuyệt vời cho lộ trình Phase 2.

## 6. Việc chưa làm / Out of scope
- Nâng cấp phiên bản các thư viện (Dependency Bump).
- Viết Unit Test cho Frontend React (bằng Jest/Testing Library). Hiện tại hệ thống ưu tiên độ bao phủ Test cho Backend (129/129 pass).

## 7. Cách chạy & Cách verify (Reproduce)
- Build toàn bộ dự án:
  - Backend: `mvnw clean install -DskipTests` (hoặc chạy test `mvnw test`).
  - Frontend: `npm run build` trong `apps/erp-frontend`.
- Kiểm tra log màn hình Backend khi chạy không còn các thông báo dạng `... executed in ... ms` từ System.out mà thay bằng format JSON log của Spring/Slf4j.

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Project đã hoàn tất đầy đủ 14 Sprints và sẵn sàng cho việc Deploy lên Staging.
- Bất kỳ nâng cấp nào tiếp theo sẽ được chuyển qua các bộ quy trình (Backlog) mới. Cảm ơn đã đồng hành!
