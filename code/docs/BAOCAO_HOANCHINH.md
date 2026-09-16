# BÁO CÁO KHÓA LUẬN TỐT NGHIỆP
## ĐỀ TÀI: XÂY DỰNG HỆ THỐNG ERP ĐA CHI NHÁNH TRONG LĨNH VỰC BÁN LẺ VẬT LIỆU XÂY DỰNG

---

## CHƯƠNG 1. TỔNG QUAN ĐỀ TÀI

### 1.1. Bối cảnh và Vấn đề

**1.1.1. Mô hình kinh doanh chuỗi cửa hàng vật liệu xây dựng**
Trong bối cảnh nền kinh tế thị trường phát triển, mô hình chuỗi cửa hàng bán lẻ vật liệu xây dựng và thiết bị nội thất ngày càng mở rộng. Mô hình đặc trưng thường bao gồm một Trụ sở chính (Headquarter - HQ) và nhiều Chi nhánh (Branch) phân tán về mặt địa lý. HQ đóng vai trò trung tâm điều phối, quản lý danh mục sản phẩm dùng chung, chính sách giá và cấu hình người dùng toàn hệ thống. Trong khi đó, các Chi nhánh chịu trách nhiệm vận hành nghiệp vụ trực tiếp bao gồm bán hàng, nhập kho, quản lý tồn kho và theo dõi công nợ khách hàng.

**1.1.2. Các vấn đề nghiệp vụ nổi bật**
Với mô hình phân tán, các doanh nghiệp thường đối mặt với những thách thức lớn về vận hành và công nghệ:
- Sự gián đoạn kết nối mạng giữa Chi nhánh và Trụ sở thường xuyên xảy ra, dẫn đến việc Chi nhánh không thể thực hiện giao dịch nếu phụ thuộc hoàn toàn vào máy chủ trung tâm.
- Dữ liệu tồn kho và công nợ giữa các chi nhánh không được đồng bộ kịp thời, gây sai lệch trong quá trình đối soát và báo cáo tổng hợp.
- Quản lý phân quyền và người dùng thường bị phân mảnh hoặc thiếu nhất quán khi mỗi chi nhánh sử dụng một hệ thống quản lý cục bộ riêng biệt.
- Phương pháp tính giá vốn truyền thống (như bình quân gia quyền) không cho phép truy vết lợi nhuận gộp theo từng giao dịch bán hàng hay lô hàng nhập cụ thể, làm giảm tính minh bạch trong báo cáo tài chính.

### 1.2. Mục tiêu nghiên cứu

**1.2.1. Mục tiêu tổng quát**
Xây dựng một hệ thống Quản trị Nguồn lực Doanh nghiệp (ERP) đa chi nhánh, cung cấp khả năng vận hành cục bộ liên tục tại các Chi nhánh ngay cả khi mất kết nối mạng, đồng thời đảm bảo khả năng quản lý, giám sát và tổng hợp dữ liệu tập trung tại Trụ sở chính.

**1.2.2. Mục tiêu cụ thể**
Để đạt được mục tiêu tổng quát, đề tài tập trung giải quyết 10 mục tiêu cụ thể sau:
1. Xây dựng phân hệ Bán hàng (Sales) hỗ trợ tạo, chỉnh sửa và xác nhận hóa đơn.
2. Xây dựng phân hệ Nhập hàng (Inbound) hỗ trợ tạo và xác nhận phiếu nhập kho từ nhà cung cấp.
3. Hỗ trợ quy trình Trả hàng (Goods Return) nhằm quản lý hàng hóa hoàn trả.
4. Quản lý Tồn kho (Inventory) chính xác, tuân thủ nguyên tắc tính giá vốn FIFO (First-In, First-Out).
5. Theo dõi sát sao Công nợ phải thu (Receivable Debt) của khách hàng theo từng chi nhánh.
6. Quản lý tập trung dữ liệu nền tảng (Master Data) tại HQ và tự động phân phối xuống Chi nhánh.
7. Triển khai cơ chế phân quyền linh hoạt cho các vai trò ADMIN và STAFF theo phạm vi truy cập (HQ hoặc Branch).
8. Xây dựng cơ chế đồng bộ dữ liệu hai chiều giữa HQ và Branch đảm bảo tính toàn vẹn thông tin.
9. Cung cấp hệ thống Dashboard tổng hợp, báo cáo doanh thu và cảnh báo tồn kho thấp.
10. Đảm bảo các tiêu chuẩn về tính toàn vẹn dữ liệu, bảo mật hệ thống và khả năng bảo trì mã nguồn.

### 1.3. Đối tượng nghiên cứu
- Quy trình nghiệp vụ ERP trong môi trường bán lẻ chuỗi phân tán.
- Các mô hình kiến trúc phần mềm phân tán đa điểm (HQ/Branch topology).
- Cơ chế đồng bộ dữ liệu giữa các cơ sở dữ liệu phân tán (Logical Replication).
- Các thuật toán và quy trình quản lý tồn kho, đặc biệt là phương pháp tính giá vốn FIFO.
- Các giải pháp xác thực và phân quyền an toàn trong môi trường nhiều thực thể (N-instance).

### 1.4. Phạm vi nghiên cứu

**1.4.1. Chức năng trong phạm vi phát triển**
Hệ thống được giới hạn phát triển các phân hệ cốt lõi sau:
- Identity: Quản lý xác thực và phân quyền người dùng.
- Branch: Quản lý thông tin chi nhánh.
- Catalog: Quản lý sản phẩm, danh mục và bảng giá.
- CRM: Quản lý thông tin khách hàng và công nợ phải thu.
- Sales (Order): Xử lý hóa đơn bán hàng.
- Goods Return: Xử lý phiếu trả hàng.
- Inbound: Quản lý phiếu nhập kho.
- Inventory: Xử lý tồn kho, ghi nhận biến động kho và tính toán giá vốn FIFO.
- Analytics: Phân tích số liệu, dashboard và cảnh báo.
- Replication & Security: Đồng bộ dữ liệu và bảo mật hệ thống.

**1.4.2. Giới hạn ngoài phạm vi**
Các nghiệp vụ sau đây được xác định nằm ngoài phạm vi phát triển của đề tài (Out of scope) và đóng vai trò như định hướng mở rộng trong tương lai:
- Đặt hàng trước của khách hàng (Customer Order).
- Đơn đặt hàng từ nhà cung cấp (Purchase Order) và Công nợ phải trả (Supplier Payable).
- Luân chuyển kho nội bộ (Stock Transfer) và Phiếu xuất kho khác (Outbound Receipt).
- Yêu cầu báo giá (RFQ) và tích hợp Thanh toán trực tuyến (Online Payment).
- Thuật ngữ Lô hàng (Stock Lot) không được sử dụng, thay vào đó hệ thống áp dụng thuật ngữ thống nhất là Lớp giá (Cost Layer).

### 1.5. Cấu trúc báo cáo
Báo cáo khóa luận được chia thành các chương như sau:
- **Chương 1:** Tổng quan đề tài, trình bày bối cảnh, mục tiêu và phạm vi nghiên cứu.
- **Chương 2:** Phân tích yêu cầu và thiết kế kiến trúc hệ thống, bao gồm cơ sở lý thuyết, phân tích kiến trúc và lựa chọn mô hình triển khai.
- **Chương 3:** Phân tích nghiệp vụ và thiết kế hệ thống, mô tả chi tiết Use Case, luồng hoạt động, cấu trúc dữ liệu và thiết kế phần mềm.
- **Kết luận:** Tổng kết các kết quả đạt được và đề xuất hướng phát triển tiếp theo.

---

## CHƯƠNG 2. PHÂN TÍCH YÊU CẦU VÀ THIẾT KẾ KIẾN TRÚC HỆ THỐNG

### 2.1. Tổng quan Lý thuyết và Công trình Liên quan

Việc thiết kế một hệ thống phân tán đòi hỏi sự lựa chọn kỹ lưỡng dựa trên cơ sở khoa học và thực tiễn. Dưới đây là các nghiên cứu và tiền lệ làm nền tảng cho các quyết định kiến trúc của hệ thống.

**2.1.1. Kiến trúc Monolith và xu hướng Modular Monolith**
- *Cơ sở tham khảo:* Các nghiên cứu thực nghiệm trên ArXiv (2022-2024) chỉ ra rằng Modular Monolith là lựa chọn tối ưu cho hệ thống quy mô vừa, tránh được chi phí vận hành quá lớn của Microservices. Thực tiễn từ dự án Prime Video (Amazon, 2023) cũng cho thấy việc chuyển đổi từ Microservices phân tán về Monolith giúp giảm đáng kể độ phức tạp vận hành và chi phí hạ tầng.
- *Kết luận:* Microservices thường mang lại overhead lớn về quản lý phân tán và độ trễ giao tiếp. Với ERP đa chi nhánh, Modular Monolith giúp duy trì một mã nguồn duy nhất có ranh giới module rõ ràng, giảm chi phí triển khai mà vẫn đảm bảo tính độc lập.
- *Liên hệ hệ thống:* Dự án áp dụng kiến trúc Modular Monolith sử dụng Java Spring Boot. Mã nguồn được triển khai cho cả HQ và Branch, sử dụng cấu hình `@ConditionalOnProperty(name="instance.role")` để kích hoạt các tính năng tương ứng. Các module giao tiếp qua các Facade Interface thay vì truy cập trực tiếp vào Repository của nhau.

**2.1.2. Đồng bộ dữ liệu phân tán (PostgreSQL Logical Replication)**
- *Cơ sở tham khảo:* Theo tài liệu chính thức của PostgreSQL, Logical Replication cho phép nhân bản dữ liệu có chọn lọc giữa các database độc lập. Giải pháp này cũng được EnterpriseDB xác nhận mang lại hiệu quả băng thông lớn và cho phép hệ thống duy trì hoạt động khi mất kết nối mạng WAN.
- *Kết luận:* Logical Replication giải quyết triệt để bài toán đồng bộ dữ liệu hai chiều (từ HQ xuống Branch và ngược lại) mà không phụ thuộc vào middleware cấp ứng dụng, phù hợp hoàn hảo với hệ thống bán lẻ phân tán.
- *Liên hệ hệ thống:* Dự án thiết lập mô hình xuất/nhận dữ liệu riêng biệt. `MASTER_TABLES` được đồng bộ từ HQ xuống Branch, và `TRANSACTION_TABLES` được đồng bộ từ Branch lên HQ. Việc sử dụng UUID làm khóa chính giúp tránh xung đột dữ liệu khi nhiều Branch cùng gửi dữ liệu về.

**2.1.3. Tính nhất quán cuối cùng (Eventual Consistency) trong hệ thống phân tán**
- *Cơ sở tham khảo:* Định lý CAP (Brewer, 2000; Gilbert & Lynch, 2002) khẳng định hệ thống phân tán không thể đạt đồng thời Tính nhất quán, Tính sẵn sàng và Khả năng chịu lỗi phân vùng. Tài liệu về kiến trúc Dynamo (Amazon) chứng minh Eventual Consistency ưu tiên tính sẵn sàng khi có lỗi mạng là phù hợp trong bán lẻ.
- *Kết luận:* Việc ưu tiên tính sẵn sàng (Availability) ở cấp độ liên chi nhánh là bắt buộc. Hệ thống chấp nhận độ trễ trong quá trình đồng bộ để đảm bảo chi nhánh luôn có thể thao tác độc lập.
- *Liên hệ hệ thống:* Hệ thống áp dụng mô hình ưu tiên tính sẵn sàng khi liên thông dữ liệu. Các báo cáo tại HQ có thể không hiển thị theo thời gian thực 100% nhưng đảm bảo dữ liệu sẽ được khớp hoàn toàn khi kết nối được phục hồi.

**2.1.4. Phương pháp tính giá vốn FIFO — Cơ sở kế toán**
- *Cơ sở tham khảo:* Chuẩn mực Kế toán Việt Nam số 02 (VAS 02) và Thông tư 200/2014/TT-BTC quy định FIFO (First-In, First-Out) là phương pháp hợp lệ và tối ưu cho hàng hóa có thể phân biệt. Chuẩn mực Quốc tế IAS 2 (IFRS) cũng có quy định tương đương, ưu tiên FIFO để truy vết chính xác giá trị tồn kho.
- *Kết luận:* Kỹ thuật FIFO phản ánh chính xác luồng di chuyển vật lý của hàng hóa và cho phép tính toán chuẩn xác biên lợi nhuận gộp cho từng giao dịch cụ thể.
- *Liên hệ hệ thống:* Hệ thống áp dụng FIFO bằng cách theo dõi các Lớp giá (Cost Layer). Giá vốn được lưu trữ cố định (snapshot) vào dòng hóa đơn ngay thời điểm xác nhận, đảm bảo bất biến.

**2.1.5. Hệ thống thực tế tương đương**
- *Cơ sở tham khảo:* SAP Business One (với Intercompany Integration Solution) và Odoo Multi-company đều cung cấp giải pháp vận hành chi nhánh độc lập có cơ sở dữ liệu phân tách và đồng bộ dữ liệu về trung tâm.
- *Kết luận:* Mô hình HQ + N Branch hoạt động độc lập đã được chứng minh tính khả thi qua các sản phẩm ERP hàng đầu.
- *Liên hệ hệ thống:* Thay vì sử dụng bộ đồng bộ riêng biệt như SAP, hệ thống tận dụng trực tiếp tính năng Logical Replication của PostgreSQL nhằm giảm chi phí hạ tầng và độ phức tạp vận hành.

### 2.2. Đặc điểm Kiến trúc của Bài toán

**2.2.1. Vận hành địa lý phân tán (Multi-location)**
Hệ thống bao gồm 1 Trụ sở (HQ) và nhiều Chi nhánh (Branch). Mỗi thực thể đóng vai trò như một node trong mạng lưới phân tán.

**2.2.2. Vận hành cục bộ (Local Operation)**
Do đặc thù đường truyền mạng, các Chi nhánh bắt buộc phải hoạt động độc lập. Mỗi chi nhánh cần có Backend và DB riêng để phục vụ giao dịch bán hàng liên tục.

**2.2.3. Chia sẻ dữ liệu gốc và quản lý giao dịch cục bộ**
HQ nắm quyền phân phối dữ liệu nền (Master Data). Chi nhánh sở hữu dữ liệu giao dịch phát sinh nội bộ (Transaction Data) và đẩy dữ liệu về HQ phục vụ phân tích.

### 2.3. Yêu cầu Chức năng và Phi chức năng

**2.3.1. Yêu cầu Chức năng**
- Identity: Đăng nhập, làm mới và thu hồi token.
- Catalog & CRM: Quản lý danh mục, giá bán và hồ sơ, công nợ khách hàng.
- Sales & Return: Lập hóa đơn bán hàng và phiếu trả hàng.
- Inventory & Inbound: Xử lý nhập kho, tồn kho, tính giá vốn FIFO.
- Analytics: Báo cáo, cảnh báo, dashboard tổng hợp.

**2.3.2. Yêu cầu Phi chức năng (NFR)**
- Availability: Chi nhánh hoạt động độc lập với HQ.
- Integrity: Đảm bảo giao dịch toàn vẹn cục bộ trên từng chi nhánh.
- Security: Phân chia phạm vi cấp và xác thực quyền hạn giữa HQ và Branch.
- Scalability: Cho phép dễ dàng thêm chi nhánh mới mà không thay đổi mã nguồn.
- Maintainability: Quản lý duy nhất một codebase.

### 2.4. Phân tích và Lựa chọn Phương án Kiến trúc

Phương án **Modular Monolith + N-instance** được lựa chọn do đáp ứng toàn diện:
- Tính độc lập tuyệt đối cho Chi nhánh (có DB riêng).
- Duy trì sự đơn giản trong phát triển (1 codebase).
- Đảm bảo tính nhất quán dữ liệu ở phạm vi cục bộ mạnh mẽ.
- Ứng dụng xuất sắc mô hình Logical Replication.

### 2.5. Thiết kế Kiến trúc Tổng thể

**2.5.1. Kiến trúc Hệ thống Tổng thể**

*(Hình 2.1: Sơ đồ kiến trúc tổng thể HQ + Branch)*

Hệ thống được tổ chức thành mô hình Hub-and-Spoke. HQ đóng vai trò Hub triển khai ứng dụng Spring Boot, PostgreSQL, Redis. Các Branch (Spoke) chạy độc lập và giao tiếp qua Logical Replication.

**2.5.2. Cấu trúc Module Backend**

*(Hình 2.2: Sơ đồ kiến trúc Module Backend)*

Ứng dụng Backend được thiết kế với 9 module. Các module giao tiếp qua Facade để hạn chế sự phụ thuộc.

**2.5.3. Mô hình Triển khai (Runtime Topology)**

*(Hình 2.3: Sơ đồ triển khai hệ thống - Deployment Diagram)*

Triển khai thông qua Docker, mỗi instance là một cụm tài nguyên độc lập bao gồm Nginx, Ứng dụng Backend và DB tương ứng.

**2.5.4. Cơ chế nhân bản dữ liệu (Logical Replication)**

*(Hình 2.4: Luồng nhân bản dữ liệu Logical Replication)*

Thiết lập cơ chế đồng bộ theo nguyên tắc Single-writer-per-table. 
- HQ Push Master Data xuống Branch.
- Branch Push Transaction Data về HQ.

**2.5.5. Thiết kế Bảo mật và Đồng bộ Tác vụ**

*(Hình 2.5: Kiến trúc bảo mật)*

- **Xác thực:** HQ là đơn vị duy nhất phát hành Token. Branch thực hiện xác thực ngoại tuyến (offline verification) nhằm duy trì hoạt động cục bộ mà không cần gọi về HQ.
- **Toàn vẹn xử lý:** Áp dụng phương pháp khóa lạc quan (Optimistic Locking) để chống ghi đè dữ liệu đồng thời, và lưu vết yêu cầu (Idempotency) để tránh xử lý lặp giao dịch.

---

## CHƯƠNG 3. PHÂN TÍCH NGHIỆP VỤ VÀ THIẾT KẾ HỆ THỐNG

### 3.1. Bối cảnh Hệ thống

*(Hình 3.1: Sơ đồ bối cảnh hệ thống - System Context Diagram)*

Hệ thống cung cấp giao diện tương tác cho hai tác nhân chính: Admin tại HQ và Staff tại các Branch. Khách hàng và Nhà cung cấp tham gia thông qua các tác vụ do hệ thống ghi nhận.

### 3.2. Phân tích Yêu cầu Chức năng (Use Case)

*(Hình 3.2: Sơ đồ Use Case tổng quát)*

Chỉ ra sự phân hóa nhiệm vụ:
- Admin tập trung vào cấu hình, bảo mật và phân tích dữ liệu toàn hệ thống.
- Staff trực tiếp vận hành quy trình bán hàng, trả hàng, nhập kho tại địa điểm.

### 3.3. Phân tích Nghiệp vụ Xác thực (Authentication)

*(Hình 3.3: Sơ đồ hoạt động - Authentication Activity Diagram)*

Minh họa luồng yêu cầu xác thực từ Frontend đến Backend. Trạng thái phản hồi phụ thuộc vào tính đúng đắn của tài khoản và thẩm quyền đối với từng Instance.

*(Hình 3.4: Sơ đồ tuần tự - Authentication Sequence Diagram)*

Đặc tả thứ tự tương tác mã nguồn giữa Controller, AuthService, DB Repository và quá trình cấp phát mã xác thực.

### 3.4. Phân tích Nghiệp vụ Bán hàng (Sales)

*(Hình 3.5: Sơ đồ Use Case Bán hàng)*

Liệt kê các quy trình cốt lõi: Tạo nháp, Chỉnh sửa, Xác nhận hóa đơn.

*(Hình 3.6: Sơ đồ hoạt động Bán hàng)*

Mô tả luồng Xác nhận hóa đơn. Hệ thống kiểm tra tồn kho, trừ dần Lớp giá theo FIFO, ghi nhận biến động, cập nhật công nợ khách hàng và lưu trạng thái thành công. Khi hết Lớp giá, hệ thống linh hoạt chuyển sang chế độ bán âm (negative stock) và ghi nhận giá vốn là 0 để kế toán đối soát.

*(Hình 3.7: Sơ đồ tuần tự Bán hàng)*

Minh họa ranh giới chu trình (Transaction) và thứ tự truy xuất giữa `SalesInvoiceService`, `InventoryFacade` và `FifoCostService`.

*(Hình 3.8: Biểu đồ Lớp - Sales Domain Class Diagram)*

Thể hiện mối quan hệ liên kết khóa ngoại giữa các thực thể nghiệp vụ: Khách hàng, Sản phẩm, Hóa đơn và Lớp giá.

### 3.5. Phân tích Nghiệp vụ Nhập kho và Trả hàng

*(Hình 3.9: Sơ đồ hoạt động Nhập kho và Trả hàng)*

Quy trình Inbound tạo mới các Lớp giá theo đơn giá nhà cung cấp. Quy trình Return giảm công nợ khách hàng và hoàn lại Lớp giá để ưu tiên tiêu thụ cho giao dịch tiếp theo.

### 3.6. Thiết kế Domain Tồn kho

*(Hình 3.10: Thiết kế Domain Tồn kho và Tính truy vết)*

Mô tả sự cấu thành tồn kho thông qua 3 bảng:
- `StockOnHand`: Truy xuất số lượng tồn hiện thời.
- `StockMovement`: Bảng dữ liệu chỉ nối thêm (append-only) để kiểm soát vết kiểm toán.
- `CostLayer`: Phục vụ phương pháp kế toán giá vốn FIFO.

### 3.7. Thiết kế Luồng dữ liệu

*(Hình 3.11: Biểu đồ luồng dữ liệu DFD mức 1)*

Chỉ ra luồng dữ liệu giữa các phân hệ: Identity, Master Data, Sales, Inventory, CRM. Minh họa vòng tuần hoàn đồng bộ dữ liệu từ HQ đến Branch và ngược lại.

### 3.8. Thiết kế Cơ sở dữ liệu

*(Hình 3.12: Sơ đồ thực thể liên kết - ER Diagram)*

Trình bày cấu trúc bảng toàn hệ thống, nhóm theo các phân hệ và làm rõ các liên kết định danh (dùng UUID cho bảng biến động, số tuần tự cho dữ liệu nền).

### 3.9. Kiến trúc Triển khai Phần mềm

*(Hình 3.13: Sơ đồ kiến trúc Module phần mềm)*

Thể hiện cấu trúc thư mục của 9 module nhằm duy trì sự độc lập về nghiệp vụ.

*(Hình 3.14: Sơ đồ Triển khai vật lý)*

Minh họa việc triển khai song song nhiều container chạy ảo hóa cho từng địa điểm chi nhánh và trụ sở.

---

## KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN

### Kết luận
Đề tài đã hoàn thành xuất sắc việc xây dựng một hệ thống ERP đáp ứng yêu cầu vận hành phân tán cho chuỗi cửa hàng vật liệu xây dựng. Việc áp dụng kiến trúc Modular Monolith kết hợp N-instance giải quyết trọn vẹn bài toán hoạt động liên tục, trong khi Logical Replication giải quyết nhu cầu đồng bộ tổng thể. Các yêu cầu phức tạp như quản lý tồn kho FIFO và kiểm soát công nợ đã được hiện thực hóa và tối ưu.

### Hướng phát triển trong tương lai
1. Phát triển phân hệ Đặt hàng từ Khách hàng (Customer Order) và Đặt mua Nhà cung cấp (Purchase Order).
2. Tích hợp chuẩn xác thực công nghiệp mở rộng như Keycloak, OIDC.
3. Thiết lập cơ chế giữ chỗ tồn kho (Stock Reservation) hỗ trợ bán hàng đa kênh.
4. Tích hợp các giải pháp phân tích giám sát hệ thống từ xa như Loki, Grafana.

---

## TÀI LIỆU THAM KHẢO

1. Gilbert, S., & Lynch, N. (2002). *Brewer's conjecture and the feasibility of consistent, available, partition-tolerant web services.* ACM SIGACT News.
2. ArXiv (2022–2024). *Nghiên cứu so sánh thực nghiệm Modular Monolith vs Microservices.*
3. Amazon Prime Video Engineering Blog (2023). *Scaling up the Prime Video audio/video monitoring service and reducing costs by 90%.*
4. PostgreSQL Official Documentation. *Logical Replication.*
5. DeCandia, G. et al. (2007). *Dynamo: Amazon's highly available key-value store.* SOSP 2007.
6. EnterpriseDB. *PostgreSQL Logical Replication for Distributed Retail.*
7. Bộ Tài chính Việt Nam. *Chuẩn mực Kế toán Việt Nam số 02 (VAS 02) — Hàng tồn kho.*
8. Bộ Tài chính Việt Nam. *Thông tư 200/2014/TT-BTC — Hướng dẫn chế độ kế toán doanh nghiệp.*
9. IFRS Foundation. *IAS 2 — Inventories.*
10. SAP Documentation. *SAP Business One — Intercompany Integration Solution.*
11. Odoo Documentation. *Multi-company and Multi-warehouse Architecture.*
