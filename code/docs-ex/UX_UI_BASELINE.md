# UX/UI BASELINE TEST

Tài liệu này ghi nhận kết quả chạy Baseline scripts (`build`, `lint`, `test`) của Phase 1.

## Kết quả các lệnh

### 1. `npm run build`
- **Kết quả**: `PASS`
- **Mô tả**: Ứng dụng build thành công (Vite v8.2.2). Output ra thư mục `dist` trong 621ms. File JS chính khá nặng (~813KB), Vite có cảnh báo về chunk size, nhưng không block build.

### 2. `npm run lint`
- **Kết quả**: `FAIL`
- **Mô tả**: Lỗi `Error [ERR_MODULE_NOT_FOUND]: Cannot find package 'eslint-config-next' imported from eslint.config.mjs`. 
- **Phân tích**: Dự án hiện tại đang dùng Vite (React) nhưng trong file cấu hình `eslint.config.mjs` lại khai báo dùng `eslint-config-next` (chuẩn của Next.js). Cần gỡ bỏ config của Next.js và thay bằng plugin ESLint chuẩn cho Vite/React để vượt qua vòng Lint.

### 3. `npm run test`
- **Kết quả**: `FAIL`
- **Mô tả**: `No test files found, exiting with code 1`
- **Phân tích**: Thư mục dự án hiện chưa có bất kỳ file test nào (`*.test.ts`, `*.spec.tsx`). Lệnh Vitest báo lỗi do không tìm thấy file. Cần bổ sung file test rỗng hoặc tuỳ chỉnh Vitest để nó `pass` khi không có test, hoặc tạm thời ghi nhận đây là baseline.

## Tổng kết Baseline
- **Blocker**: Chưa thể pass 100% các script vì lỗi cấu hình Lint và thiếu Test files. 
- **Hướng giải quyết (Phase 1):** Vì yêu cầu không cho phép sửa mã nguồn ngoài phạm vi UX/UI trừ khi thật sự cần thiết, phần lỗi `eslint` này tuy cản trở CI/CD nhưng không block UI. Tuy nhiên, theo tiêu chí "Chỉ khi phase hiện tại đạt điều kiện PASS mới được thực hiện phase tiếp theo", tôi sẽ tạm ghi nhận baseline này và báo cáo để có thể xin chỉ đạo sửa lại file cấu hình lint/test, hoặc pass baseline dựa trên việc Build thành công.
