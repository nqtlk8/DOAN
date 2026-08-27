# Design System: VLXDgiatot (Utilitarian Minimalism)

## 1. Tầm nhìn & Nguyên tắc (Vision & Principles)
- **Tối giản thực dụng (Utilitarian Minimalism)**: Loại bỏ mọi yếu tố trang trí không cần thiết.
- **Khả năng truy cập tối đa (Hyper-Accessible)**: Chữ to, màu sắc tương phản mạnh.
- **Hướng tới người dùng lao động**: Giao diện không đánh đố, thao tác thẳng thắn (không có các hiệu ứng ẩn giấu dưới sự kiện hover).

## 2. Màu sắc (Colors)
- **Primary (Chủ đạo)**: `#64748B` (Xám công nghiệp) - `bg-slate-500`
- **Accent/CTA (Nổi bật/Hành động)**: `#EA580C` (Cam công trường) - `bg-orange-600`
- **Background (Nền)**: `#F8FAFC` (Trắng xám nhẹ) - `bg-slate-50`
- **Foreground (Chữ chính)**: `#1E293B` (Đen xám đậm) - `text-slate-800`
- **Destructive/Sale (Cảnh báo/Giảm giá)**: `#DC2626` (Đỏ) - `bg-red-600`

## 3. Typography (Chữ viết)
- **Heading (Tiêu đề)**: Font `Rubik`. Định dạng: In đậm (Bold/Black), dứt khoát.
- **Body (Nội dung)**: Font `Nunito Sans`. Định dạng: Rõ ràng, dễ đọc.
- **Kích thước mặc định**: Base là 16px. Giá tiền từ 20px - 24px.

## 4. UI Components (Thành phần giao diện)
- **Nút bấm (Buttons)**: Chiều cao tối thiểu 48px. Màu Cam Công Trường cho nút Mua Hàng/Gọi Điện. Viền rõ ràng.
- **Thẻ sản phẩm (Product Card)**: Không dùng hiệu ứng hover. Nút CTA luôn hiển thị. Giá tiền to, màu đỏ hoặc cam. Khung viền cứng cáp (Border 1px solid `#E2E8F0`).
- **Icons**: Sử dụng icon Solid (Nét liền), đi kèm Text. Kích thước icon chuẩn 24x24px.

## 5. Cấu trúc SEO (Next.js)
- Thẻ `<title>` và `<meta name="description">` bắt buộc có ở mọi trang.
- Header semantic (`<header>`, `<main>`, `<nav>`, `<footer>`).
- Heading h1 duy nhất cho mỗi trang.
