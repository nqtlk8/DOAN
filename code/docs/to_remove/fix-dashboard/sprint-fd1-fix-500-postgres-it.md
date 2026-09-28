2026-09-28 SPRINT FD-1

## 1. Mục tiêu Sprint (Goal)
- Giải quyết lỗi `HTTP 500: ERROR: could not determine data type of parameter $1` trên Dashboard khi tham số `branchId` là `null` (do PostgreSQL yêu cầu định nghĩa kiểu rõ ràng).
- Thiết lập hạ tầng chạy Integration Test (IT) trên PostgreSQL bằng `maven-failsafe-plugin`.
- Tách biệt logic xử lý múi giờ và chuyển đổi ngày (M-06) ra khỏi lớp Data Adapter, đưa lên lớp Service.

## 2. Thành quả đạt được (Done)
- **Cấu hình maven-failsafe-plugin:** Đã cấu hình plugin trong `pom.xml` để nhận diện và chạy các bài test `*IT.java` (tại `code/erp-platform/services/erp-backend/pom.xml`).
- **Sửa lỗi 500:** 
  - Tạo class helper `AnalyticsSqlParams` để map chuẩn kiểu `Types.BIGINT` cho tham số có thể `null` (tại `code/erp-platform/services/erp-backend/src/main/java/com/storename/erp/analytics/infrastructure/AnalyticsSqlParams.java`).
  - Áp dụng vào `AnalyticsDataAdapter` (tại `code/erp-platform/services/erp-backend/src/main/java/com/storename/erp/analytics/infrastructure/AnalyticsDataAdapter.java`).
- **Xử lý timezone (M-06):**
  - Thêm config `analytics.business-zone` và `analytics.storage-zone` vào `application.yml` (tại `code/erp-platform/services/erp-backend/src/main/resources/application.yml`).
  - Tạo property class `AnalyticsProperties` để nạp config (tại `code/erp-platform/services/erp-backend/src/main/java/com/storename/erp/analytics/AnalyticsProperties.java`).
  - Cập nhật `DashboardService` và `AnalyticsDataPort` để chuyển đổi `startDateKey/endDateKey` sang `LocalDateTime` dựa trên múi giờ (tại `code/erp-platform/services/erp-backend/src/main/java/com/storename/erp/analytics/application/DashboardService.java` và `code/erp-platform/services/erp-backend/src/main/java/com/storename/erp/analytics/application/port/AnalyticsDataPort.java`).
- **Cập nhật Unit/Integration Test:**
  - Viết bài IT đỏ `AnalyticsDataAdapterPostgresIT` (tại `code/erp-platform/services/erp-backend/src/test/java/com/storename/erp/analytics/infrastructure/AnalyticsDataAdapterPostgresIT.java`).
  - Sửa `DashboardServiceTest` và `DashboardControllerTest` để cover thêm case validate ngày (tại `code/erp-platform/services/erp-backend/src/test/java/com/storename/erp/analytics/application/DashboardServiceTest.java` và `code/erp-platform/services/erp-backend/src/test/java/com/storename/erp/analytics/api/DashboardControllerTest.java`).
  - Xóa file test cũ trên H2: `AnalyticsDataAdapterTest.java`.

## 3. Quyết định kiến trúc & Lý do
- **Quyết định:** Sử dụng tham số có định kiểu `Types.BIGINT` cho `branchId` thông qua `AnalyticsSqlParams`.
  - **Lý do:** Khắc phục triệt để lỗi PostgreSQL không nội suy được kiểu dữ liệu khi `branchId` truyền vào là null.
  - **Đã cân nhắc:** Sử dụng ép kiểu trong string SQL `CAST(:branchId AS BIGINT)` - bỏ qua vì phải lặp lại nhiều lần trong nhiều query và làm code SQL khó đọc hơn.
- **Quyết định:** Chuyển đổi logic datetime (từ `startDateKey/endDateKey` sang `fromTs/toTs`) ở tầng `DashboardService`.
  - **Lý do:** Lớp Data Adapter chỉ nên tập trung vào việc tương tác với DB (dùng `LocalDateTime`), giữ cho Service layer phụ trách các config business (như lấy config múi giờ từ properties).
  - **Đã cân nhắc:** Thực hiện format datetime trực tiếp trong DataAdapter - bỏ qua vì khiến DataAdapter bị dính chặt với các thư viện đọc config/properties.
- **Quyết định:** Xoá `AnalyticsDataAdapterTest.java` trên H2.
  - **Lý do:** Database H2 bỏ qua lỗi nội suy kiểu của PostgreSQL, dẫn đến test luôn pass nhưng runtime lại lỗi (nguyên nhân gây P2). Do đó, chỉ giữ lại bài test IT trên Postgres thật.

## 4. Hướng dẫn đọc code theo thứ tự
1. `pom.xml`: Khai báo `maven-failsafe-plugin`.
2. `application.yml` & `AnalyticsProperties.java`: Cấu hình múi giờ (timezone) cơ bản.
3. `DashboardService.java`: Logic chuyển đổi `startDateKey/endDateKey` -> `LocalDateTime fromTs/toTs`.
4. `AnalyticsDataPort.java` & `AnalyticsDataAdapter.java`: API và Adapter sử dụng tham số `LocalDateTime` và `branchId` có chỉ định type.
5. `AnalyticsSqlParams.java`: Helper sinh `MapSqlParameterSource`.
6. Các file test: `DashboardServiceTest.java`, `DashboardControllerTest.java`, `AnalyticsDataAdapterPostgresIT.java`.

## 5. Rủi ro / Nợ kỹ thuật đã biết
- Môi trường chạy Agent (hiện tại) không hỗ trợ Docker đầy đủ nên các Test Integration trên Testcontainers (`FlywayPostgresIntegrationTest`, `PostgresContainerSmokeTest`, `AnalyticsDataAdapterPostgresIT`) không thể connect được docker (Lỗi: `IllegalStateException: Could not find a valid Docker environment`). Cần kiểm chứng lại trên môi trường CI/CD chuẩn hoặc máy dev có Docker Desktop. (Đây là Tech Debt: IT cũ và mới đều fail do môi trường Docker, cần khắc phục môi trường Docker hoặc tắt testcontainers khi dev chay local không có docker).
- Quá trình seed dữ liệu cần điều chỉnh ở tương lai (Seed V2/V13 tạo branch=1).
- (Bổ sung ở FD-6) Rule Circuit Breaker yêu cầu quan sát test đỏ trước khi sửa. Ở sprint này **chưa quan sát được test đỏ trên PostgreSQL** vì Testcontainers không chạy. Bằng chứng root cause khi đó là log người dùng gửi (`could not determine data type of parameter $1`). Review ngày 2026-09-28 đã tái hiện trên PostgreSQL thật: câu lệnh chuẩn bị với tham số không kiểu báo đúng lỗi này, còn tham số kiểu `bigint` thì chạy được.

## 6. Việc chưa làm / Out of scope
- ⚠️ KHÔNG thay đổi các DTO (Data Transfer Object).
- ⚠️ KHÔNG sửa đổi công thức tính toán chỉ số kinh doanh.
- ⚠️ KHÔNG can thiệp vào tiến trình logic replication (phạm vi của sprint sau).
- ⚠️ KHÔNG đổi version Spring Boot hay version của các dependencies khác.
- Các IT test cũ bị lỗi do Docker environment được giữ nguyên, không sửa trong sprint này.

## 7. Cách chạy & Cách verify
Lệnh kiểm thử toàn bộ unit test (pass):
```bash
.\mvnw.cmd test
```
*(Lưu ý: Chạy `.\mvnw.cmd verify` sẽ fail nếu máy dev chưa mở/cài Docker do Testcontainers)*

Lệnh chạy tay bằng Docker Compose để verify API HTTP 200:
```bash
docker compose up -d --build hq-app
```
(Sau khi service start, truy cập API: `GET /api/v1/analytics/dashboard?startDateKey=20260901&endDateKey=20260927` mà không truyền `branchId` sẽ trả về `200 OK`).

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Sprint FD-2 sẽ tiếp quản để xử lý Migration V21.
- Cần tiếp tục dựa vào thiết lập múi giờ (M-06) trong `AnalyticsProperties` nếu các Query của snapshot cần timezone.

---
- [x] 1. Mục tiêu Sprint
- [x] 2. Thành quả đạt được (có trỏ đường dẫn file cụ thể)
- [x] 3. Quyết định kiến trúc & Lý do (có nêu phương án đã bỏ + lý do)
- [x] 4. Hướng dẫn đọc code theo thứ tự
- [x] 5. Rủi ro / Nợ kỹ thuật đã biết
- [x] 6. Việc chưa làm / Out of scope
- [x] 7. Cách chạy & Cách verify
- [x] 8. Điểm nối cho Sprint tiếp theo
- [x] Đã thêm link vào `docs/fix-dashboard/README.md`
- [x] Không có tính từ mơ hồ tự đánh giá chất lượng
