# Tổng quan Hệ thống ERP - Cửa hàng Vật liệu Xây dựng

Hệ thống ERP quản lý chuỗi cửa hàng vật liệu xây dựng với mô hình kiến trúc phân tán (Headquarter - HQ và các Chi nhánh - Branch). Hệ thống bao gồm 2 phần chính: Backend (Spring Boot 3) và Frontend (React + Vite).

## 1. Các Module Nghiệp vụ (Bounded Contexts)
Hệ thống được chia thành các module cốt lõi tuân thủ Domain-Driven Design (DDD):
- **Identity & Security**: Quản lý tài khoản, Role (ADMIN, STAFF), Quyền hạn (Permissions), và gán tài khoản vào Chi nhánh. Dùng JWT cho xác thực.
- **Catalog**: Quản lý Master Data gồm Danh mục (Category), Sản phẩm (Product), Bảng giá (PriceList), và Nhà cung cấp (Supplier).
- **CRM**: Quản lý thông tin Khách hàng (Customer) và Công nợ phải thu (ReceivableDebt). Tất cả khách hàng đều bình đẳng và có thể mua chịu (không phân biệt CustomerType).
- **Sales & Order**: Quản lý Hóa đơn Bán hàng (SalesInvoice) và Trả hàng (GoodsReturn).
- **Inventory**: Quản lý Tồn kho tức thời (StockOnHand), Lịch sử di chuyển kho (StockMovement), Nhập kho (InboundReceipt), và Lớp giá vốn FIFO (CostLayer). Hệ thống áp dụng nghiêm ngặt nguyên tắc tính giá vốn FIFO (nhập trước xuất trước).
- **Analytics**: Phân tích dữ liệu, Dashboard, và các báo cáo (Doanh thu, Lợi nhuận, Biến động kho, Cảnh báo tồn kho).

## 2. Kiến trúc & Thiết kế Kỹ thuật
- **Kiến trúc Tổng thể**: CQRS-lite và Bounded Contexts. Các module giao tiếp với nhau thông qua Facade (ví dụ: InventoryFacade) để tránh phụ thuộc vòng và lỗi vi phạm SOLID/DRY.
- **Bảo mật & Phân quyền (RBAC)**: Phân quyền theo Chi nhánh (@BranchScoped) - nhân viên chi nhánh nào chỉ thấy dữ liệu chi nhánh đó. Cơ chế Idempotency (@IdempotencyProtected) chống click đúp hoặc gửi trùng request.
- **Database Schema**: Hỗ trợ môi trường phân tán. Dữ liệu Master (do HQ quản lý) được đồng bộ xuống các Branch. Dữ liệu giao dịch (Branch sở hữu) hoạt động độc lập và có thể đồng bộ ngược lên HQ bằng Logical Replication.
- **Cơ chế tính giá vốn FIFO**: Khi có hàng nhập (Inbound), hệ thống tạo CostLayer. Khi bán hàng (Sales), hệ thống duyệt qua các CostLayer còn hàng theo thời gian nhập để trừ dần (FIFO) nhằm tính Lợi nhuận Gộp chính xác nhất. Mọi thay đổi số lượng kho đều được ghi nhận Append-only vào StockMovement.
- **Frontend**: Ứng dụng Single Page Application viết bằng React, TypeScript, TailwindCSS. Tách biệt UI component và Data Fetching thông qua Custom Hooks (e.g. useCustomers, useStockMovements) kết hợp với mô hình ApiService tập trung.

## 3. Quy chuẩn Code & Testing
- **Backend**: Yêu cầu tài liệu (Javadoc) cho logic nghiệp vụ, trả về chuẩn ApiResponse<T>, không lộ Domain Entity ra controller (sử dụng DTO), và bắt buộc log AOP (@Slf4j).
- **Frontend**: UI theo hướng cấu hình (Config-Driven), tái sử dụng component (DRY), và quản lý quyền truy cập giao diện linh hoạt dựa vào AuthContext.
- **Kiểm thử**: Các test case (Unit, Integration) phải đảm bảo chạy trên cả thiết lập giả lập (MockMvc, H2 DB), tuân thủ chuẩn tự động xóa Database sau mỗi Test (setUp) và không bị trùng lặp port.

