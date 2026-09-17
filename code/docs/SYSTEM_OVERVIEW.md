# Tổng quan Hệ thống ERP - v5 (Post-Refactoring)

Hệ thống ERP quản lý chuỗi cửa hàng vật liệu xây dựng với mô hình kiến trúc phân tán (Headquarter - HQ và các Chi nhánh - Branch). Hệ thống bao gồm 2 phần chính: Backend (Java/Spring Boot 3) và Frontend (React + Vite).

Sau chiến dịch **23-Sprint ERP Remediation Plan**, hệ thống đã đạt độ trưởng thành cực kỳ cao về Kiến trúc và Codebase.

## 1. Các Module Nghiệp vụ (Bounded Contexts)
Hệ thống được chia thành các module cốt lõi tuân thủ Domain-Driven Design (DDD):
- **Identity & Security**: Quản lý tài khoản, Role (ADMIN, STAFF), Quyền hạn (Permissions). Dùng JWT RS256 cho xác thực (HQ ký, Branch giải mã).
- **Branch**: Thông tin mạng lưới chi nhánh.
- **Catalog**: Quản lý Master Data (HQ-owned) gồm Danh mục (Category), Sản phẩm (Product), Bảng giá (PriceList), và Nhà cung cấp (Supplier).
- **CRM**: Quản lý Khách hàng (Customer) và Công nợ phải thu (ReceivableDebt). 
- **Sales & Order**: Đơn Bán hàng (SalesInvoice) và Trả hàng (GoodsReturn).
- **Inventory**: Tồn kho tức thời (StockOnHand), Lịch sử di chuyển kho (StockMovement), Nhập kho (InboundReceipt), và Lớp giá vốn FIFO (CostLayer). 
- **Analytics**: Phân tích dữ liệu, Dashboard, và các báo cáo tập trung tại HQ.

## 2. Điểm nhấn Kiến trúc (Architecture Highlights)
- **Kiến trúc Hexagonal & Facade**: Chấm dứt tình trạng gọi chéo Repository. Các Bounded Context giao tiếp với nhau hoàn toàn qua các `Facade Interface` (VD: `CrmFacade`, `OrderFacade`, `CatalogFacade`).
- **Bảo mật & Phân quyền (RBAC)**: Phân quyền theo Chi nhánh thực thi chặt chẽ qua `AuthUtils.getBranchIdOrNull()` ở mọi endpoint. 
- **Idempotency Chống trùng lặp**: Tất cả các API thay đổi dữ liệu (POST, PUT, DELETE) đều được bọc bởi `@IdempotencyProtected` cùng Header `Idempotency-Key`, đảm bảo tính toàn vẹn 100% khi rớt mạng hoặc người dùng double-click.
- **Cơ chế tính giá vốn FIFO**: Khi nhập hàng (Inbound), hệ thống tạo `CostLayer`. Khi bán hàng (Sales), hệ thống xuất kho trừ dần theo `CostLayer` cũ nhất để tính Lợi nhuận gộp chính xác.
- **Tài chính Nhất quán (Signed Ledger)**: Mọi thao tác làm thay đổi nợ (Bán hàng, Trả Hàng, Thanh toán) đều đẩy về một sổ cái duy nhất (`ReceivableDebtMovement`) với công thức bù trừ Tổng nợ = `SUM(amount)`. Dòng tiền hoàn toàn nhất quán.
- **Database Schema**: Hỗ trợ môi trường phân tán bằng Logical Replication. Toàn bộ tham chiếu ngoại lai giữa các Bounded Context (ví dụ: Từ Đơn hàng sang Khách hàng) đều dùng Native Keys (`UUID`, `Long`) để triệt tiêu Domain Leakage và tối ưu bộ nhớ.

## 3. Quy chuẩn Code & Testing
- **Backend**: Yêu cầu tài liệu (Javadoc) cho logic nghiệp vụ, trả về chuẩn `ApiResponse<T>`, cấm lộ Domain Entity ra controller (buộc dùng DTO), và bắt buộc log AOP (`@Slf4j`).
- **Anti N+1 Queries**: Xử lý dữ liệu hàng loạt thông qua Batch Fetching (`findAllById`) kết hợp `@EntityGraph`. 
- **Kiểm thử E2E & Tích hợp**: Toàn bộ chu trình (Inbound -> Sale -> Payment -> Return) được cover 100% bởi `FullE2EFlowIT`. Không một dòng code nào được merge nếu làm vỡ E2E.
- **Frontend**: Ứng dụng Single Page Application viết bằng React, TypeScript. Tách biệt UI component và Data Fetching thông qua Custom Hooks kết hợp với mô hình ApiService tập trung. Mọi API call đều bị cấm nhúng thẳng trong UI.
