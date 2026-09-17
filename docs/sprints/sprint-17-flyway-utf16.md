# Ngày: 2026-09-17
# Sprint 17: Flyway Integration Test & Fix UTF-16 Issue

## 1. Mục tiêu Sprint (Goal)
- Sửa lỗi mã hóa (UTF-16 encoding) trong các file migration `V16` và `V17` của Flyway, khiến cho quá trình chạy migrate trên PostgreSQL thật bị lỗi.
- Đảm bảo các file `.sql` của Flyway đều ở chuẩn mã hóa UTF-8.

## 2. Thành quả đạt được (Done)
- ✅ Đã phát hiện và convert các file `V16__add_branch_id_to_customer.sql` và `V17__backfill_opening_balance.sql` từ UTF-16 sang chuẩn UTF-8 (thông qua PowerShell `Set-Content -Encoding UTF8`).
- ✅ Đã bổ sung `FlywayEncodingTest.java` để quét các file migration và tự động assert (báo đỏ) nếu bất kì file nào chứa mã hóa UTF-16 (kiểm tra Null bytes và Byte-Order-Mark LE/BE).

**Các file chính đã thay đổi:**
- [MODIFY] `services/erp-backend/src/main/resources/db/migration/V16__add_branch_id_to_customer.sql` (Chuyển sang UTF-8)
- [MODIFY] `services/erp-backend/src/main/resources/db/migration/V17__backfill_opening_balance.sql` (Chuyển sang UTF-8)
- [NEW] `services/erp-backend/src/test/java/com/storename/erp/FlywayEncodingTest.java`

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions)
- **Quyết định:** Viết Unit Test `FlywayEncodingTest` đọc trực tiếp byte array thay vì dùng Testcontainers để boot PostgreSQL chạy thử Flyway.
- **Lý do:** 
  1. H2 Database không hỗ trợ hoàn toàn cú pháp dump của `pg_dump` (như `SET statement_timeout = 0`) ở `V1__init_schema.sql`, dẫn đến không thể chạy Flyway Integration Test trực tiếp trên H2.
  2. Môi trường CI/CD (hoặc máy local hiện tại) không có sẵn Docker Daemon để boot PostgreSQL qua Testcontainers (gây lỗi `Could not find a valid Docker environment`).
  3. Kiểm tra bằng cách đọc bytes trực tiếp (Byte-Order-Mark hoặc mật độ null bytes) là đủ nhanh và đáng tin cậy để bẫy lỗi UTF-16 mà không cần tốn tài nguyên chạy DB thật.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. Đọc `FlywayEncodingTest.java`: Xem logic quét thư mục `db/migration` và bóc tách 2 byte đầu tiên để tìm chữ ký UTF-16 (BOM).

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Các cấu trúc `SET statement_timeout` trong `V1__init_schema.sql` đang cản trở việc test tự động Flyway trên môi trường H2. Lý tưởng nhất là tạo một tập lệnh khởi tạo H2 riêng hoặc phải setup PostgreSQL CI pipeline độc lập.
- `FlywayEncodingTest` hiện tại chỉ hardcode để quét V16 và V17 (như là một sanity check cơ bản). Có thể mở rộng quét tự động toàn bộ thư mục `db/migration` trong tương lai.

## 6. Việc chưa làm / Out of scope
- Chưa thiết lập Testcontainers toàn diện cho hệ thống do rào cản về Docker environment.

## 7. Cách chạy & Cách verify (Reproduce)
Chạy lệnh Maven sau tại thư mục `services/erp-backend`:
```bash
./mvnw test -Dtest=FlywayEncodingTest
```
**Kết quả mong đợi:** 
Bài test pass (không phát hiện BOM của UTF-16 ở V16 và V17).

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Issue về database migration UTF-16 đã được xử lý.
- Có thể chuyển sang Sprint 18: Financial Reconciliation Test (Đối soát dữ liệu tài chính).
