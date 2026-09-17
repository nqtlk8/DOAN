# CI/CD Pipeline cho Test Suite

## Tổng quan Pipeline
Sau khi cài đặt thành công Testcontainers và chia Test Suite thành Fast Tests (H2) và Integration Tests (PostgreSQL), quy trình CI (Github Actions hoặc GitLab CI) cần được nâng cấp để hỗ trợ Docker-in-Docker.

## Các bước (Stages)

1. **Checkout Code**: Lấy code từ repository.
2. **Setup JDK**: Cài đặt JDK 21.
3. **Setup Docker/Testcontainers**: Đảm bảo service Docker Engine đang chạy (Trên GitHub Actions Ubuntu-latest mặc định đã có).
4. **Cache Maven Packages**: Tối ưu hóa thời gian tải thư viện.
5. **Run Fast Tests**: 
   - `mvn test -P fast-tests` (Hoặc bỏ qua `-Dtest=*IT` thông qua Surefire plugin).
   - *Mục tiêu*: Phát hiện sớm các lỗi Java/Mock nhanh nhất có thể.
6. **Run Integration Tests**:
   - `mvn test -Dtest=*IT`
   - Testcontainers sẽ khởi động PostgreSQL.
   - Spring Boot sẽ kích hoạt Flyway và thực thi `V1 -> Vcurrent`.
   - Chạy các Test cho DB Schema, Unique constraints, và Logic tài chính.
7. **Report**: Xuất báo cáo Jacoco Coverage & Test Results.

## Lợi ích
Bằng việc chia luồng này, thời gian lấy feedback cho dev khi viết logic thường rất ngắn (H2), nhưng vẫn cam kết 100% khi merge vào `main` sẽ không vỡ Database (Testcontainers).
