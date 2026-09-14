# ERP Platform - Comprehensive Test Strategy

Tài liệu này định nghĩa chiến lược kiểm thử tiêu chuẩn cho toàn bộ hệ thống ERP, tuân thủ chặt chẽ kiến trúc 4 Level để đảm bảo tính module hóa, tốc độ thực thi, và độ bao phủ (coverage).

## 1. Phân Lớp Kiểm Thử (Testing Pyramid)

Hệ thống được kiểm thử dựa trên 4 cấp độ (Level) từ dưới lên:

### Level 1: Unit Tests (Fast & Isolated)
- **Mục đích**: Kiểm tra business logic của Domain Entities, Services, và Utility classes mà không khởi động Spring Context.
- **Công cụ**: JUnit 5, Mockito (`@ExtendWith(MockitoExtension.class)`).
- **Quy tắc**:
  - KHÔNG load Spring Context.
  - Sử dụng `@Mock` và `@InjectMocks` cho các dependencies.
  - Test phải thực thi siêu nhanh (< 100ms/test).
- **Ví dụ**: `JwtTokenProviderTest`, `InventoryFacadeImplTest`, `StockOnHandTest`.

### Level 2: Slice Tests / Web Layer (Routing & Security)
- **Mục đích**: Kiểm thử các Web layer (Controllers), Authentication (401), Authorization (403), Input Validation (400), và HTTP Status.
- **Công cụ**: Spring `MockMvc`, `@WebMvcTest`.
- **Quy tắc**:
  - CHỈ khởi tạo các Beans liên quan đến Controller và Security. KHÔNG khởi tạo JPA (`@EnableJpaAuditing` phải được tách ra `JpaAuditingConfig`).
  - Phải `@MockBean` JwtAuthenticationFilter hoặc cấu hình `doAnswer()` để filter chain hoạt động chính xác.
  - Mọi service gọi từ Controller đều phải được `@MockBean`.
- **Ví dụ**: `DashboardControllerTest`, `BranchControllerTest`, `StockControllerTest`.

### Level 3: Backend Integration Tests (Database & Context)
- **Mục đích**: Đảm bảo các layer liên kết với nhau đúng đắn (Services -> Database), test native queries, test `@ConditionalOnProperty` (ContextHQ vs ContextBranch).
- **Công cụ**: `@SpringBootTest`, `@DataJpaTest`, H2 (PostgreSQL mode) hoặc Testcontainers.
- **Quy tắc**:
  - Dùng môi trường DB riêng (in-memory H2 PostgreSQL mode hoặc Testcontainers).
  - Dùng để test các query SQL Native phức tạp (ví dụ: Analytics, Dashboard).
  - Test đảm bảo các Beans cấu hình theo Profile hoạt động đúng.
- **Ví dụ**: `AnalyticsDataAdapterTest`, `ArchitectureV3HqTest`, `ArchitectureV3BranchTest`.

### Level 4: E2E Frontend Tests (User Flows)
- **Mục đích**: Mô phỏng thao tác của người dùng trên UI (App Shell), kiểm tra luồng end-to-end từ giao diện đến API giả lập hoặc thực tế.
- **Công cụ**: Playwright.
- **Quy tắc**:
  - Setup interceptors (`page.route`) trước khi `page.goto('/')` để tránh timeout/proxy error.
  - Test phải bao phủ các use case thực tế như: Tạo phiếu nhập, Bán hàng, Trả hàng, Phân quyền màn hình.
- **Ví dụ**: `inbound-receipt.spec.ts`, `dashboard.spec.ts`, `customer.spec.ts`.

## 2. Quy Tắc "Circuit Breaker" & Gỡ Lỗi (Debugging)
- Nếu một lỗi test lặp lại **2 lần liên tiếp**, kỹ sư / AI Agent phải **DỪNG** ngay lập tức và tiến hành Root Cause Analysis (RCA).
- Phải xác định lỗi thuộc lớp nào: `syntax`, `logic`, `config-env`, `data-state`.
- Cấm dùng "try and error" mù quáng (ví dụ: liên tục thay đổi annotation Spring Security mà không hiểu luồng Filter Chain).

## 3. Tình Trạng Hiện Tại (Coverage Status)
- [x] Level 1 (Unit): Mọi module (Catalog, CRM, Inventory, Order, Auth) đều có Unit Test chuẩn Mockito.
- [x] Level 2 (WebMvc): Đã chuẩn hóa toàn bộ Controller Tests (sửa lỗi JPA Metamodel, mock JWT Filter, test mã 403 Forbidden).
- [x] Level 3 (Integration): Tích hợp hoàn chỉnh Test Conditional Bean Context (HQ/Branch) và Native SQL queries với Testcontainers/H2-PgMode.
- [x] Level 4 (Playwright): Đã thiết lập các kịch bản chính cho Dashboard, Khách hàng, Trả hàng, Nhập hàng, Bán hàng, và Login.

## 4. Hành Động Tiếp Theo (Roadmap)
- Liên tục cập nhật và bổ sung E2E testing cho các tính năng mới bằng Playwright.
- Chạy automation pipelines tích hợp 4 mức test trên CI/CD.
