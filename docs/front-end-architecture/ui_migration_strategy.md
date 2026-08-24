# UI/UX Design & Migration Strategy
*Tài liệu tổng hợp thiết kế và chiến lược chuyển đổi giao diện từ dự án cũ sang hệ thống Backend/Frontend mới.*

## 1. Tổng quan (Overview)
Hệ thống hiện tại được chia làm 2 phần giao diện chính với các triết lý thiết kế đặc thù:
- **Public Web (VLXDgiatot):** Hướng tới người dùng cuối, khách hàng mua vật liệu xây dựng.
- **ERP (Nội bộ):** Hướng tới nhân viên bán hàng, kế toán, quản trị viên (Power users).

Mục tiêu của giai đoạn 3 là tái cấu trúc (refactor) lại 2 giao diện này và tích hợp chặt chẽ với hệ thống Backend mới (`erp-backend`), sử dụng `erp-frontend` (hoặc các project tương tự) làm nền tảng.

## 2. Giao diện Public Web (Utilitarian Minimalism)
Được thiết kế dựa trên triết lý **Tối giản thực dụng** và **Khả năng truy cập tối đa**.

### 2.1. Đặc điểm cốt lõi:
- **Trực diện, không đánh đố:** Không sử dụng các hiệu ứng ẩn (hover) phức tạp, thao tác thẳng thắn để phù hợp với người dùng lao động.
- **Màu sắc (Tailwind):** 
  - Nền: `bg-slate-50` (Trắng xám nhẹ)
  - Chữ: `text-slate-800` (Đen xám đậm)
  - Nổi bật/Hành động (CTA): `bg-orange-600` (Cam công trường) cho các nút Mua hàng, Gọi điện.
  - Cảnh báo/Giảm giá: `bg-red-600`
- **Typography:** 
  - Tiêu đề (Heading): Font `Rubik`, in đậm.
  - Nội dung (Body): Font `Nunito Sans`. 
  - Chữ to, giá tiền kích thước từ 20px-24px.
- **Thành phần (Components):** 
  - Nút bấm to (min-height 48px), viền rõ.
  - Product Card cứng cáp (Border 1px solid `#E2E8F0`), luôn hiện nút CTA.
  - Icon nét liền (Solid), kích thước 24x24px, đi kèm text.

## 3. Giao diện ERP (Data-Dense Swiss & Soft UI)
Được thiết kế để tối đa hóa mật độ dữ liệu (Data density) và giảm tải nhận thức cho người dùng thao tác >8 tiếng/ngày.

### 3.1. Đặc điểm cốt lõi:
- **Tối giản, giảm nhiễu:** Loại bỏ màu sắc, viền mảng không cần thiết. Khung nền `bg-slate-50`, card nội dung `bg-white`.
- **Màu sắc:** Primary action là Xanh dương Thụy Sĩ (`bg-blue-600`), Text màu `slate-900` và `slate-500`.
- **Typography:** Font `Inter` (hoặc `system-ui`). Dùng `tabular-nums` cho số liệu trong bảng.
  - Chữ cỡ nhỏ (14px) cho dữ liệu, (12px) cho label (chữ hoa `tracking-wider`).
- **Layout (Grid 8pt):** Mật độ cao. Padding tight (`gap-2`, `p-2`) cho bảng, form.

### 3.2. Kiến trúc MDI Layout (Multiple Document Interface)
Giao diện hoạt động như một hệ điều hành Desktop thu nhỏ:
- **Workspace Tabs:** Hỗ trợ mở nhiều tab chức năng cùng lúc.
- **Cấu trúc Form 3 Cột:** Tận dụng bề ngang màn hình (Thông tin Nội bộ - Khách hàng - Tài chính).
- **State Machine (VIEW - ADD - EDIT):** 
  - Quản lý chặt chẽ trạng thái của Form.
  - Chế độ VIEW khóa toàn bộ input.
- **Bottom Toolbar & Hotkeys:** 
  - Thanh công cụ dính (sticky) ở dưới cùng (`F2`: Thêm, `F3`: Sửa, `F4`: Lưu, `F8`: Xóa, `ESC`: Hủy).
  - Component `MdiModuleLayout` bọc ngoài các Module (như `SalesModule`), tự động xử lý Hotkeys và Sidebar chức năng (Chuyển đổi giữa FORM và LIST).

## 4. Chiến lược đọc và copy định dạng sang Frontend mới hiệu quả
Để refactor giao diện từ `code/web-public` và `code/erp` sang hệ thống mới (`erp-frontend`...), cần thực hiện theo các bước:

### Bước 1: Setup Infrastructure (Tailwind & Fonts)
- **Copy file cấu hình Tailwind:** Mang cấu hình màu (slate, orange, blue) và phông chữ (Inter, Rubik, Nunito Sans) từ `tailwind.config.js` cũ sang project mới.
- **Global CSS:** Mang các định nghĩa CSS toàn cục (reset, font-family) từ `index.css` / `App.css`.

### Bước 2: Bóc tách và tái cấu trúc Components độc lập (Dumb Components)
- Đọc các component cũ trong `src/components/ui/` hoặc `src/components/common/`.
- Tách biệt hoàn toàn UI khỏi Logic (API Calls).
- Sử dụng các thư viện như Radix UI hoặc Headless UI kết hợp Tailwind để build lại các component (Button, Card, Input) chuẩn theo Document thiết kế.

### Bước 3: Porting MDI Layout & Hotkeys (Cho ERP)
- Copy component `MdiModuleLayout.tsx` (từ `code/erp/src/components/layout/`) nguyên vẹn sang frontend mới. Component này đã thiết kế rất tốt việc quản lý Hotkeys và Bottom Toolbar.
- Đảm bảo Global State cho Tabs được setup bằng Redux / Zustand hoặc React Context ở hệ thống mới.

### Bước 4: Tích hợp API (Smart Components)
- Ở dự án cũ, API calls có thể bị mix trong Component. Ở dự án mới, tuân thủ **API Facade Pattern** (theo `AGENTS.md`).
- Xây dựng các Custom Hook (ví dụ `usePurchaseOrder`, `useSalesReturn`) để gọi API từ backend mới (qua axios interceptor có Auth), sau đó truyền dữ liệu xuống Dumb Components.
- Refactor lại Form 3 cột cho tương thích với schema dữ liệu từ `erp-backend`.

## Tổng kết nhiệm vụ
AI Frontend Agent và các lập trình viên khi bắt tay vào code **phải đọc kỹ tài liệu này** cùng với các file config gốc (tailwind, css). Tuyệt đối không tự bịa ra UI Component mới mà phải sử dụng lại các pattern đã được định hình (Utilitarian Minimalism cho web và Swiss & Soft UI cho ERP).
