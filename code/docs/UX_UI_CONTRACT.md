# UX/UI CONTRACT

Tài liệu này quy định hợp đồng giao diện (UI contract) bắt buộc cho toàn bộ các màn hình thuộc `erp-frontend`.

## 1. Trạng thái Loading
Quy tắc hiển thị khi đang chờ phản hồi từ Server.
- **Quy tắc**: `Loading → Spinner/Skeleton`
- **Mô tả**: Khi đang fetch data cho toàn màn hình hoặc một khối dữ liệu lớn, phải hiển thị Skeleton (row skeleton cho bảng, card skeleton cho Dashboard). Khi đang submit mutation (ví dụ bấm nút Lưu), phải disable nút bấm và hiển thị Spinner/Loading text trên chính nút đó (ví dụ "Đang lưu...").

## 2. Trạng thái Empty
Quy tắc hiển thị khi Server trả về kết quả thành công nhưng không có dữ liệu.
- **Quy tắc**: `Request thành công + 0 records = Empty`
- **Mô tả**: KHÔNG ĐƯỢC nhầm lẫn lỗi (Error) thành Empty. Empty State chỉ hiển thị khi `!isLoading && !isError && data.length === 0`. Cần có thông báo rõ ràng như "Chưa có dữ liệu" và kèm theo Call to Action (CTA) nếu phù hợp (Ví dụ: `[+ Thêm mới]`).

## 3. Trạng thái Error
Quy tắc hiển thị khi Server báo lỗi hoặc mất kết nối mạng.
- **Quy tắc**: `Request thất bại = Error`
- **Mô tả**: Nếu request gọi list/data bị thất bại, phải thay thế bảng dữ liệu/card bằng `ErrorState` component kèm nút `[Thử lại]` để gọi lại hàm `refetch()`. 
- **Cấm**: Tuyệt đối KHÔNG hiển thị "Không có dữ liệu" khi API gặp lỗi.

## 4. Trạng thái Success
Quy tắc phản hồi sau khi người dùng thực hiện một hành động (Mutation) thành công.
- **Quy tắc**: `Toast success + UI refresh/update`
- **Mô tả**: Sau khi Create/Update/Delete thành công, hệ thống phải bắn ra một toast thông báo (ví dụ: "Đã cập nhật sản phẩm thành công") thông qua Notification Service tập trung, đồng thời gọi `queryClient.invalidateQueries` để cập nhật lại danh sách trên màn hình thay vì gọi `window.location.reload()`.
