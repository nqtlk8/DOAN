# Sprint 1.2: Catalog Module

## 1. Overview
Sprint 1.2 tập trung vào việc xây dựng Module Catalog bao gồm các thực thể cốt lõi: Category (Danh mục), Product (Sản phẩm), AttributeDefinition, CategoryAttribute, ProductAttributeValue và PriceList (Bảng giá). 
Trọng tâm kỹ thuật là chuẩn hóa dữ liệu theo mô hình EAV (Entity-Attribute-Value) 3NF, Denormalize JSONB cho attributes, và thiết lập luồng Logical Replication đồng bộ dữ liệu từ HQ xuống các Branch.

## 2. Business Requirements
- Xây dựng hệ thống quản lý sản phẩm phân cấp (Category tree).
- Hỗ trợ thuộc tính sản phẩm động (Dynamic Attributes) theo từng danh mục.
- Cấu hình bảng giá linh hoạt theo từng chi nhánh (PriceList).
- ProductWriter (Ghi) chỉ hoạt động tại HQ. Dữ liệu Master được quản lý tập trung.
- ProductReader (Đọc) hoạt động ở mọi nơi (HQ và Branch).
- Branch Offline: Dữ liệu Master (Catalog) được đồng bộ thời gian thực từ PostgreSQL HQ xuống PostgreSQL Branch thông qua Logical Replication, đảm bảo Branch đọc dữ liệu local siêu tốc không cần call API lên HQ.

## 3. Architecture & Design (ADR)
- **Denormalize JSONB (EAV)**: Sử dụng mô hình EAV chuẩn 3NF để lưu trữ dữ liệu có cấu trúc cho attributes, sau đó Denormalize (lưu dư thừa) thành dạng `JSONB` trong cột `attributes_cache` ở bảng `product`. Điều này cho phép đọc thông tin sản phẩm và thuộc tính chỉ với 1 query duy nhất, tối ưu Read performance. (Tham khảo: `giai-thich-chuan-hoa-database.md`).
- **PostgreSQL Logical Replication**: 
  - `Master (HQ)`: Thiết lập PUBLICATION cho các bảng của Catalog.
  - `Replica (Branch)`: Thiết lập SUBSCRIPTION để kéo dữ liệu từ Master.
  - Đảm bảo tính khả dụng cao và giảm tải truy vấn cho HQ.
- **DDD Encapsulation**: Refactoring loại bỏ `@Setter` ở class level, sử dụng các domain method cụ thể (như `updateDetails`, `changeActiveState`) để bảo vệ trạng thái nội bộ của entity.

## 4. API Contracts
- `POST /api/v1/catalog/products`: Khởi tạo sản phẩm mới (HQ only, Role: ADMIN). Trả về `ApiResponse<ProductResponseDto>`.
- `PUT /api/v1/catalog/products/{id}`: Cập nhật sản phẩm (HQ only, Role: ADMIN). Kèm theo cơ chế tự động clear cache (`@CacheEvict`). Trả về `ApiResponse<ProductResponseDto>`.
- `GET /api/v1/catalog/products`: Lấy danh sách sản phẩm (HQ/Branch, Role: ADMIN/SALES). Trả về `ApiResponse<List<ProductResponseDto>>`.
- `GET /api/v1/catalog/products/{id}`: Chi tiết sản phẩm (HQ/Branch, Role: ADMIN/SALES). Trả về `ApiResponse<ProductResponseDto>`.
- `GET /api/admin/system/replication-status`: Kiểm tra trạng thái replication (HQ/Branch, Role: ADMIN).

## 5. Security & RBAC
- Chỉ user có role `ADMIN` mới được quyền Create/Update sản phẩm và xem trạng thái hệ thống Replication.
- User có role `ADMIN` hoặc `SALES` đều có thể đọc thông tin Product.
- Áp dụng Bean Validation (`@Valid`, `@NotBlank`, `@NotNull`) chặt chẽ ở cấp độ Controller DTOs.
- `BranchScopedAspect` được sử dụng để intercept và phân lập dữ liệu bảng giá theo chi nhánh (đã được chuẩn bị cho Sprint đọc giá).

## 6. Observability
- Logging (SLF4J) chi tiết tại các Controller và Service (`ProductWriter`, `ProductReader`, `CategoryWriter`).
- Các lỗi về nghiệp vụ như cố tình xóa danh mục đang có sản phẩm sẽ được catch và log cẩn thận trước khi trả HTTP error.

## 7. Testing Strategy
- **Unit/Integration Tests (H2)**: Kiểm tra các luồng CRUD cơ bản, tính toán attributes EAV, chặn xóa danh mục có children/products (`ProductServiceIntegrationTest`, `CategoryWriter`). Cache config được test in-memory.
- **E2E Replication Test (Docker Compose)**: Khởi tạo cluster PostgreSQL 1 Master và 1 Replica. Chạy script tạo Schema, tạo Pub/Sub và dùng Java JDBC kết nối độc lập vào hai node để xác nhận replication lag nhỏ hơn 2 giây. Test `ReplicationE2ETest` đã pass.

## 8. Deployment & Migration
- Chạy schema qua `V2__catalog_schema.sql` bằng công cụ migration.
- Lưu ý môi trường H2 khi test tự động được map kiểu dữ liệu `JSON` thay cho `JSONB` của PostgreSQL để tránh conflict.
- Deploy thực tế yêu cầu admin phải setup publication/subscription manually trên hệ thống Database Cluster. Kịch bản E2E script `run-replication-test.ps1` có thể dùng làm tham khảo.
