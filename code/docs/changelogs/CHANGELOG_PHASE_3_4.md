# CHANGELOG: ĐỒNG BỘ DỮ LIỆU & REFACTOR HỆ THỐNG (PHASE 3 & 4)

## 1. Backend (Database & API)
- **Chuẩn hóa Flyway Migrations**: Dọn sạch hoàn toàn các ký tự mã hóa lỗi (BOM) và các lệnh meta-commands của pg_dump (\restrict, \connect) trong V1__init_schema.sql và V2__seed_test_data.sql. Đảm bảo file mã hóa đúng UTF-8 NoBOM.
- **Sửa lỗi Database Security**: Cập nhật V9__branch_db_security.sql để phân quyền user đúng (erp_user thay vì pp_user), xóa bỏ các bảng rác không tồn tại.
- **Phục hồi Module**: Khôi phục lại toàn bộ logic và schema của các module quan trọng bị xóa nhầm trước đó: Khôi phục Supplier (Catalog), khôi phục FactSales, FactStockMovement, DimDate, InventoryAlertConfig và InventoryAlertLog (Analytics & Inventory).
- **Audit Đồng Bộ Data Contract**: Xác minh 100% (26 bảng) khớp tuyệt đối giữa cấu trúc Database Schema (PostgreSQL) và Java JPA Entities. Giải quyết dứt điểm các lỗi mapping (BaseEntity inheritance).

## 2. Frontend (React & Facade)
- **Quy chuẩn API Service**: Refactor toàn bộ src/api/ApiService.ts. Áp dụng bóc tách dữ liệu thống nhất es.data.data dựa theo chuẩn bọc { success, data, message, errors } của Backend.
- **Nâng cấp Xử lý Lỗi UI**: Thay thế toàn bộ mã lert() thủ công bằng 	oast.success() / 	oast.error() (thông qua thư viện eact-hot-toast) mang lại trải nghiệm mượt mà, đồng bộ. Cập nhật SalesOrderForm nhận đúng mã ID từ DTO esponse.id.
- **Tích hợp Chức năng Mới (Admin Pages)**: Đấu nối thành công các trang Quản lý Kho (StockList), Công nợ (DebtList), Hệ thống Chi nhánh (BranchList), và Trả hàng (GoodsReturnModule) vào thanh điều hướng TopRibbon.tsx.
- **Xử lý Dữ liệu Mảng**: Chuẩn hóa cách fallback dữ liệu khi API trả về rỗng trong tất cả các Component List (esponse || [] thay vì esponse?.data || []).

## 3. Documents (Tài liệu Hợp đồng)
- **DATA_CONTRACT.md**: Báo cáo Audit toàn diện, minh bạch chi tiết mọi bảng và cột.
- **API_CONTRACT.md**: Chuẩn hóa giao tiếp JSON giữa React và Spring Boot thông qua phân tích OpenAPI.
- **ISSUES_CONFIRMED.md**: Báo cáo dập tắt các cảnh báo False Positive.
