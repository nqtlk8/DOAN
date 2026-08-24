# Kịch bản kiểm thử (Test Cases) - Module User

Tài liệu này định nghĩa các kịch bản kiểm thử (Test Cases) cho Module User (Authentication & Authorization) dựa trên tài liệu thiết kế API và cơ sở dữ liệu. Phương pháp kiểm thử áp dụng là Black-box Testing, tập trung vào hành vi và hợp đồng giao tiếp (Contracts). Không phụ thuộc vào mã nguồn của Dev.

## 1. Frontend Test Cases (Giao diện & Tích hợp API)

| Mã TC | Tên Test Case | Mô tả / Các bước thực hiện | Kết quả mong đợi |
| :--- | :--- | :--- | :--- |
| **TC_USER_FE_01** | Login Success with Admin | 1. Nhập username `admin`, password `admin`.<br>2. Submit form. | Chuyển hướng tới Admin Dashboard. `accessToken` và `refreshToken` được lưu vào `localStorage`. UI xử lý phân quyền cho Admin. |
| **TC_USER_FE_02** | Login Success with Sales | 1. Nhập username `sales`, password `sales`.<br>2. Submit form. | Chuyển hướng tới Sales Dashboard. Token được lưu vào `localStorage`. UI giới hạn quyền hiển thị (chỉ xem Menu/Dữ liệu của Sales). |
| **TC_USER_FE_03** | Login Failed with Empty Fields | 1. Bỏ trống username hoặc password.<br>2. Submit form. | Nút submit có thể bị vô hiệu hóa, hoặc hệ thống hiển thị lỗi Validation phía Client trước khi gọi API. Nếu bypass để gọi API, phải báo 400 Bad Request. |
| **TC_USER_FE_04** | Login Failed with Incorrect Credentials | 1. Nhập username hoặc password sai.<br>2. Submit form. | Hiển thị thông báo "Invalid username or password" (từ mã lỗi API `AUTH_001`). Tuyệt đối không lưu token. |
| **TC_USER_FE_05** | Axios Interceptor - Token Injection | 1. Đã đăng nhập.<br>2. Điều hướng và thực hiện một request đến API Private. | Axios Interceptor tự động gắn `Bearer <access_token>` vào headers của request. |
| **TC_USER_FE_06** | Axios Interceptor - Token Refresh on 401 | 1. Mô phỏng access_token hết hạn (API trả về HTTP 401).<br>2. Quan sát luồng mạng. | Interceptor chặn lỗi 401, tự động dùng refresh_token gọi API cấp lại access_token, sau đó retry lại request ban đầu một cách trong suốt với User. |

## 2. Backend Test Cases (API Contract & Business Logic)

| Mã TC | Tên Test Case | Mô tả / Các bước thực hiện | Kết quả mong đợi |
| :--- | :--- | :--- | :--- |
| **TC_USER_BE_01** | API Login Success - Valid Admin | Gửi POST `/api/auth/login` với payload `{ "username": "admin", "password": "admin" }`. | HTTP 200 OK. Định dạng chuẩn có `success: true`. Payload `data` chứa `accessToken`, `refreshToken` và `roles: ["ROLE_ADMIN"]` hợp lệ. |
| **TC_USER_BE_02** | API Login Success - Valid Sales | Gửi POST `/api/auth/login` với payload `{ "username": "sales", "password": "sales" }`. | HTTP 200 OK. Định dạng chuẩn có `success: true`. Payload `data` chứa `roles: ["ROLE_SALES"]` hợp lệ. |
| **TC_USER_BE_03** | API Login Failure - Invalid Password | Gửi POST `/api/auth/login` với username đúng, password sai. | HTTP 401 Unauthorized. Có mảng `errors` trả về code `AUTH_001` và detail `Bad credentials`. |
| **TC_USER_BE_04** | API Login Failure - Missing Fields | Gửi POST `/api/auth/login` thiếu trường `username` hoặc `password`. | HTTP 400 Bad Request. Có thông báo lỗi ở mảng `errors` chỉ định rõ trường bị thiếu (thuộc tính `field`). |
| **TC_USER_BE_05** | Password Hashing Validation | Gửi request Login. Kiểm tra quá trình truy vấn xác thực ở Backend. | Hệ thống bắt buộc dùng BCrypt kiểm tra Hash của mật khẩu từ DB. Tuyệt đối không query trực tiếp Plaintext (SQL Injection / Data Exposure protection). |

## 3. Database Constraints & Row-Level Security (RLS) Test Cases

| Mã TC | Tên Test Case | Mô tả / Các bước thực hiện | Kết quả mong đợi |
| :--- | :--- | :--- | :--- |
| **TC_USER_DB_01** | RLS - Admin Select All | 1. Kích hoạt session cho `ROLE_ADMIN` (`app.current_role`).<br>2. `SELECT * FROM users`. | Trả về danh sách chứa toàn bộ người dùng trong hệ thống (Policy `admin_select_all`). |
| **TC_USER_DB_02** | RLS - Sales Select Own | 1. Kích hoạt session cho `ROLE_SALES` và gán `app.current_user_id`.<br>2. `SELECT * FROM users`. | Chỉ trả về đúng 1 bản ghi của chính tài khoản Sales đó (Policy `sales_select_own`). Không thấy user khác. |
| **TC_USER_DB_03** | Constraint - Username Unique | INSERT user mới với trường `username` là `admin` (đã tồn tại). | Thất bại. Postgres văng lỗi Unique Constraint Violation. |
| **TC_USER_DB_04** | Constraint - Role Check | INSERT user mới với trường `role` mang giá trị `ROLE_GUEST`. | Thất bại. Postgres văng lỗi Check Constraint Violation do ràng buộc `role IN ('ROLE_ADMIN', 'ROLE_SALES')`. |

## 4. Kiểm thử Data Integrity & Race Condition (Concurrency)

| Mã TC | Tên Test Case | Mô tả / Các bước thực hiện | Kết quả mong đợi |
| :--- | :--- | :--- | :--- |
| **TC_USER_RC_01** | Concurrent Login Requests (1000 rqs) | Dùng tool tải (vd: K6, JMeter) gửi **đồng thời 1000 POST requests** tới `/api/auth/login` cùng lúc với thông tin đúng. | Mọi requests đều được xử lý chính xác (HTTP 200 OK) hoặc bị cản bởi Rate Limiting. Hệ thống **không crash**, không OOM, token không bị sinh chéo (Cross-state/Thread-safe). |
| **TC_USER_RC_02** | Concurrent User Creation | Gửi **đồng thời 100 requests API (cùng 1 milisecond)** để đăng ký/tạo user mới với **cùng một username**. | Data Integrity được giữ nguyên: **Chỉ đúng 1 request thành công**, 99 requests còn lại bị hệ thống từ chối (Unique Constraint Exception). Tuyệt đối không lọt qua 2 bản ghi trùng username. |
| **TC_USER_RC_03** | Token Generation Under High Load | Mô phỏng 5000 users đăng nhập đồng thời vào hệ thống. | Quá trình hashing và verify BCrypt (chiếm CPU) cần diễn ra đồng bộ, không xảy ra race condition. Token JWT sinh ra phải chính xác tuyệt đối theo Role của từng người dùng, không bị nhầm lẫn Session. |

## 5. Integration Test Cases (E2E System Integration)

| Mã TC | Tên Test Case | Mô tả / Các bước thực hiện | Kết quả mong đợi |
| :--- | :--- | :--- | :--- |
| **TC_INT_E2E_01** | Happy Path Integration - Login Flow | 1. Frontend: Người dùng gửi form login hợp lệ.<br>2. Backend: Truy vấn Postgres đối chiếu Hash BCrypt.<br>3. Backend: Sinh và trả về JWT Token.<br>4. Frontend: Nhận Token lưu LocalStorage và đổi UI (chuyển sang màn hình Dashboard). | Toàn bộ luồng xuyên suốt thành công, không bị tắc nghẽn ở CORS hay Proxy. Dữ liệu Token lưu chính xác và UI điều hướng đúng quyền hạn. |
| **TC_INT_E2E_02** | Bad Path Integration - Fake Token Manipulation | 1. Frontend: Người dùng can thiệp sửa đổi Token trên LocalStorage thành chuỗi giả.<br>2. Frontend: Dùng token giả gọi API (ví dụ: lấy danh sách Đơn hàng).<br>3. Backend: Spring Security bắt lỗi xác thực và trả về HTTP 401.<br>4. Frontend: Axios Interceptor hứng lỗi 401. | Axios Interceptor tự động xóa LocalStorage và điều hướng người dùng văng ra màn hình Login. Đảm bảo an toàn bảo mật hệ thống. |
