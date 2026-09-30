# BÁO CÁO KHÓA LUẬN TỐT NGHIỆP

## ĐỀ TÀI: XÂY DỰNG HỆ THỐNG ERP QUẢN LÝ BÁN LẺ ĐA CHI NHÁNH — CỬA HÀNG THÀNH PHƯƠNG

> **Ghi chú cho người dùng:** Nội dung dưới đây được tổ chức theo *tinh thần* cấu trúc mục lục (trình tự Chương → Mục → Tiểu mục) mà bạn cung cấp, nhưng tiêu đề và nội dung từng mục đã được biên soạn lại để khớp đúng với dự án ERP thực tế (không phải đề tài thương mại điện tử thời trang trong mục lục gốc). Hai lỗi trong mục lục gốc đã được sửa:
> 1. Bổ sung tiêu đề "CHƯƠNG 2" (bị thiếu trong bản gốc bạn gửi) — đặt tên là **"PHÂN TÍCH YÊU CẦU HỆ THỐNG"**.
> 2. Sửa lỗi đánh số trùng "2.2" hai lần → đổi thành 2.2 và 2.3.
>
> Các vị trí hình/sơ đồ được đánh dấu bằng placeholder `[DIAGRAM — Hình N: ...]`, khớp với mã hình tương ứng trong file `diagram-mermaid.md` đi kèm.

---

# LỜI CẢM ƠN

*(Phần này mang tính cá nhân — bạn đã chọn tự viết. Gợi ý cấu trúc 3 đoạn: (1) cảm ơn Nhà trường/Khoa và môi trường đào tạo, (2) cảm ơn GVHD Trương Việt Phương — nêu cụ thể sự hỗ trợ trong quá trình làm khóa luận, (3) cảm ơn gia đình/bạn bè/đồng nghiệp đã hỗ trợ. Nên viết sau khi hoàn thiện toàn bộ báo cáo.)*

---

# DANH SÁCH CHỮ VIẾT TẮT, KÝ HIỆU

| **Từ viết tắt** | **Ý nghĩa** |
|---|---|
| ERP | Enterprise Resource Planning — Hoạch định nguồn lực doanh nghiệp |
| HQ | Headquarter – Trụ sở chính |
| Branch | Chi nhánh |
| JWT | JSON Web Token |
| RS256 | RSA Signature with SHA-256 |
| ORM | Object-Relational Mapping |
| JPA | Jakarta Persistence API |
| NFR | Non-Functional Requirement — Yêu cầu phi chức năng |
| FR | Functional Requirement — Yêu cầu chức năng |
| API | Application Programming Interface |
| FIFO | First In, First Out |
| ERD | Entity Relationship Diagram |
| DFD | Data Flow Diagram |
| CRUD | Create, Read, Update, Delete |
| E2E | End-to-End |
| AOP | Aspect-Oriented Programming |
| SQL | Structured Query Language |
| UI | User Interface |
| RBAC | Role-Based Access Control |
| DDD | Domain-Driven Design |
| WAL | Write-Ahead Log (PostgreSQL) |
| ACID | Atomicity, Consistency, Isolation, Durability |
| DTO | Data Transfer Object |
| N-Instance | Nhiều instance của cùng một ứng dụng, đóng vai trò khác nhau (HQ/Branch) |

---

# DANH MỤC HÌNH

> Danh mục dưới đây được sinh trực tiếp (đối chiếu tự động bằng script) từ toàn bộ caption "*Hình X-Y: …*" xuất hiện trong Chương 1–5, đảm bảo khớp 100% với tiêu đề tương ứng trong file `diagram-mermaid.md` — xem xác nhận tại Mục 3.7.1, tiêu chí 6.

- Hình 2-1: Kiến trúc tổng thể HQ + Branch
- Hình 2-2: Kiến trúc Module Backend
- Hình 2-3: Deployment / Runtime Topology
- Hình 2-4: Cơ chế Logical Replication
- Hình 2-5: Kiến trúc bảo mật (JWT RS256 + Branch Scope)
- Hình 3-1: Sơ đồ phân cấp chức năng — Admin
- Hình 3-2: Sơ đồ phân cấp chức năng — Staff
- Hình 3-3: Sơ đồ Use Case tổng quát — Admin
- Hình 3-4: Sơ đồ Use Case tổng quát — Staff
- Hình 3-5: Sequence Diagram — UC01 Đăng nhập / Refresh / Revoke
- Hình 3-6: Sequence Diagram — UC09 Tra cứu giá riêng theo khách hàng (đúng 6 bước Basic + Alternative Flow ở trên)
- Hình 3-7: Activity Diagram — UC10 Lập hóa đơn bán hàng (đúng 15 bước Basic + Alternative Flow ở trên)
- Hình 3-8: Sequence Diagram — UC10 Lập hóa đơn bán hàng
- Hình 3-9: Activity Diagram — UC11 Lập phiếu trả hàng (đúng 10 bước Basic + Alternative Flow ở trên)
- Hình 3-10: Sequence Diagram — UC11 Lập phiếu trả hàng
- Hình 3-11: Activity Diagram — UC12 Lập phiếu nhập kho (đúng 7 bước Basic Flow ở trên, không có Alternative Flow)
- Hình 3-12: Sequence Diagram — UC12 Lập phiếu nhập kho
- Hình 3-13: Sequence Diagram — UC15 Đồng bộ dữ liệu tự động
- Hình 3-14: DFD Cấp 0 (Context Diagram)
- Hình 3-15: DFD — Lập hóa đơn bán hàng
- Hình 3-16: DFD — Lập phiếu trả hàng
- Hình 3-17: DFD — Lập phiếu nhập kho
- Hình 3-18: DFD — Tra cứu giá riêng theo khách hàng
- Hình 3-19: DFD — Theo dõi công nợ
- Hình 3-20: Class Diagram tổng quát — mức Structural (chỉ tên lớp + thuộc tính, chưa có kiểu dữ liệu/visibility)
- Hình 3-21: Sơ đồ ER mức khung hệ thống (toàn cảnh 3 nhóm nghiệp vụ)
- Hình 3-22: Sơ đồ ER — Nhóm Identity & CRM
- Hình 3-23: Sơ đồ ERD Logic — Nhóm Identity & CRM
- Hình 3-24: Sơ đồ ER — Catalog
- Hình 3-25: Sơ đồ ER — Inventory
- Hình 3-26: Sơ đồ ERD Logic — Nhóm Catalog & Inventory
- Hình 3-27: Sơ đồ ER — Order
- Hình 3-28: Sơ đồ ERD Logic — Nhóm Order
- Hình 3-29: Class Diagram tổng quát — mức Analysis-Level (operation có kiểu tham số/trả về)
- Hình 3-30: Class Diagram mức Design-Level — Cụm Identity & CRM
- Hình 3-31: Class Diagram mức Design-Level — Cụm Catalog & Inventory
- Hình 3-32: Class Diagram mức Design-Level — Cụm Order

---

# DANH MỤC BẢNG

- Bảng 2-1: Đặc tả yêu cầu chức năng (Functional Requirements)
- Bảng 2-2: Đặc tả yêu cầu phi chức năng (Non-Functional Requirements)
- Bảng 3-1: Danh sách Use Case hệ thống
- Bảng 3-2: Đặc tả Use Case UC01
- Bảng 3-3: Đặc tả Use Case UC02
- Bảng 3-4: Đặc tả Use Case UC03
- Bảng 3-5: Đặc tả Use Case UC04
- Bảng 3-6: Đặc tả Use Case UC05
- Bảng 3-7: Đặc tả Use Case UC06
- Bảng 3-8: Đặc tả Use Case UC07
- Bảng 3-9: Đặc tả Use Case UC08
- Bảng 3-10: Đặc tả Use Case UC09
- Bảng 3-11: Đặc tả Use Case UC10
- Bảng 3-12: Đặc tả Use Case UC11
- Bảng 3-13: Đặc tả Use Case UC12
- Bảng 3-14: Đặc tả Use Case UC13
- Bảng 3-15: Đặc tả Use Case UC14
- Bảng 3-16: Đặc tả Use Case UC15
- Bảng 3-17: Từ điển dòng dữ liệu — DFD Lập hóa đơn bán hàng
- Bảng 3-18: Từ điển dòng dữ liệu — DFD Lập phiếu trả hàng
- Bảng 3-19: Từ điển dòng dữ liệu — DFD Lập phiếu nhập kho
- Bảng 3-20: Từ điển dòng dữ liệu — DFD Tra cứu giá riêng theo khách hàng
- Bảng 3-21: Từ điển dòng dữ liệu — DFD Theo dõi công nợ
- Bảng 3-22: Danh sách các lớp đối tượng (List of Object Classes)
- Bảng 3-23: Mô tả chi tiết thuộc tính và phương thức của 24 lớp đối tượng
- Bảng 3-24: Danh sách các mối quan hệ giữa các lớp đối tượng
- Bảng 3-25: Mô tả thực thể và thuộc tính (Description of Entities and Attributes) — nhóm theo từng entity
- Bảng 3-26: Mô tả các mối quan hệ (Description of Relationships)
- Bảng 3-27: Physical Database Design — tổng hợp bảng, khóa và chiều đồng bộ
- Bảng 3-28: Checklist nhất quán tổng quan
- Bảng 3-29: Ma trận truy vết FR ↔ UC (song ánh hai chiều — mỗi FR có đúng 1 UC neo, mỗi UC có ≥1 FR nguồn gốc; 22/22 FR và 15/15 UC được phủ kín)
- Bảng 3-30: Đối chiếu số bước Basic/Alternative Flow với Activity Diagram và Sequence Diagram (cột message đếm bằng script tự động trên mã Mermaid thực tế, không phải ước lượng thủ công)
- Bảng 3-31: Rà soát tính nhất quán tên gọi xuyên suốt 6 tầng mô hình hóa (tên lớp PascalCase ở tầng OO ↔ tên bảng/cột snake_case ở tầng dữ liệu vật lý — quy ước chuyển đổi nhất quán, đúng chuẩn JPA/Hibernate mặc định, không có sai lệch chính tả)
- Bảng 3-32: Đối chiếu tiêu đề chương với Mục 1.6

> **Ghi chú:** Chương 1, 4 và 5 có thêm một số bảng phụ trợ (bảng module, bảng module A–C trong Phụ lục, bảng tham chiếu API…) không đánh số Bảng X-Y vì mang tính liệt kê/tra cứu nhanh, không thuộc mô hình phân tích–thiết kế cốt lõi mà checklist nhất quán (Mục 3.7) yêu cầu truy vết.

---

# CHƯƠNG 1. TỔNG QUAN ĐỀ TÀI

## 1.1. Bối cảnh (Background)

Trong hoạt động kinh doanh vật liệu xây dựng và thiết bị thông minh nhà, việc quản lý hàng hóa, bán hàng, tồn kho, công nợ và nhập hàng có mối quan hệ chặt chẽ với nhau. Khi quy mô kinh doanh mở rộng thành nhiều chi nhánh, yêu cầu quản lý không còn dừng ở việc xây dựng một phần mềm bán hàng đơn lẻ mà trở thành bài toán quản lý dữ liệu và nghiệp vụ trên phạm vi toàn hệ thống.

Mô hình hoạt động được xét trong đề tài — lấy nguyên mẫu từ chuỗi cửa hàng Thành Phương — gồm một Trụ sở chính (Headquarters – HQ) và nhiều Chi nhánh (Branch). Trụ sở chính chịu trách nhiệm quản lý tập trung dữ liệu dùng chung (**Master Data**) như sản phẩm, danh mục, giá bán, khách hàng và nhà cung cấp; trong khi mỗi chi nhánh trực tiếp thực hiện các nghiệp vụ kinh doanh phát sinh dữ liệu giao dịch (**Transaction Data**) như bán hàng, quản lý kho, công nợ và trả hàng.

Đặc thù ngành vật liệu xây dựng còn đặt ra các quy tắc nghiệp vụ riêng mà hệ thống phải phản ánh đúng: giá bán được thương lượng linh hoạt theo từng khách hàng (nhân viên có thể tham chiếu giá lần mua gần nhất trước khi chốt giá), khách hàng phổ biến việc mua chịu (công nợ phải thu phát sinh ngay tại thời điểm xuất hóa đơn), và giá vốn hàng bán phải được tính theo từng lô nhập cụ thể (FIFO) để báo cáo lợi nhuận gộp phản ánh đúng thực tế kinh doanh thay vì bị "bình quân hóa".

## 1.2. Vấn đề cần giải quyết (Problem statement)

Mô hình HQ – N Chi nhánh đặt ra bốn nhóm vấn đề kỹ thuật và nghiệp vụ cần giải quyết:

**Thứ nhất, hoạt động của chi nhánh không thể phụ thuộc hoàn toàn vào hệ thống tại trụ sở chính.** Trong thực tế, kết nối mạng giữa chi nhánh và HQ có thể gián đoạn. Nếu mọi nghiệp vụ đều phải xử lý tại hệ thống trung tâm, việc mất kết nối sẽ làm gián đoạn trực tiếp hoạt động bán hàng và quản lý kho. Hệ thống cần duy trì hoạt động nghiệp vụ tại chi nhánh ngay cả khi tạm thời không kết nối được đến HQ.

**Thứ hai, Master Data cần được quản lý thống nhất, không được phép "phân mảnh".** Sản phẩm, danh mục, giá, khách hàng và nhà cung cấp không thể quản lý độc lập tại từng chi nhánh vì sẽ dẫn đến dữ liệu không đồng nhất giữa các nơi. Ngược lại, Transaction Data phát sinh tại chi nhánh cần được tổng hợp về HQ phục vụ quản lý và báo cáo tập trung.

**Thứ ba, việc phân tán dữ liệu trên nhiều địa điểm làm phát sinh yêu cầu về tính toàn vẹn và nhất quán dữ liệu.** Hệ thống phải xác định rõ dữ liệu nào thuộc quyền ghi của HQ, dữ liệu nào thuộc quyền ghi của Branch (nguyên tắc *single-writer-per-table*), đồng thời kiểm soát quá trình đồng bộ giữa các cơ sở dữ liệu để tránh xung đột ghi (dual-writer) hoặc mất dữ liệu.

**Thứ tư, các nghiệp vụ lõi (bán hàng, nhập kho, trả hàng) đòi hỏi tính toàn vẹn giao dịch cao trong môi trường nhiều người dùng thao tác đồng thời.** Cần cơ chế chống trùng lặp khi client gửi lại request (mạng chập chờn, double-click), cũng như cơ chế khóa phù hợp để tránh hai giao dịch cùng lúc ghi đè lên cùng một dòng tồn kho hoặc cùng một lớp giá vốn.

Từ những vấn đề trên, đề tài tập trung xây dựng **hệ thống ERP quản lý bán lẻ đa chi nhánh cho cửa hàng Thành Phương**, trong đó kiến trúc phần mềm, thiết kế cơ sở dữ liệu và cơ chế đồng bộ được thiết kế đồng thời để đáp ứng đầy đủ các yêu cầu nghiệp vụ nêu trên.

## 1.3. Mục tiêu nghiên cứu (Research objectives)

### 1.3.1. Mục tiêu tổng quát (General objective)

Xây dựng một hệ thống Quản trị Nguồn lực Doanh nghiệp (ERP) đa chi nhánh, cung cấp khả năng vận hành cục bộ liên tục tại các Chi nhánh ngay cả khi mất kết nối mạng, đồng thời đảm bảo khả năng quản lý, giám sát và tổng hợp dữ liệu tập trung tại Trụ sở chính.

### 1.3.2. Mục tiêu cụ thể (Specific objectives)

Để đạt được mục tiêu tổng quát, đề tài tập trung giải quyết 10 mục tiêu cụ thể sau:

1. Xây dựng phân hệ Bán hàng (Sales) hỗ trợ tạo và xác nhận hóa đơn trong một giao dịch nguyên tố (create-and-confirm).
2. Xây dựng phân hệ Nhập hàng (Inbound) hỗ trợ tạo và xác nhận phiếu nhập kho từ nhà cung cấp.
3. Hỗ trợ quy trình Trả hàng (Goods Return) nhằm quản lý hàng hóa khách hàng hoàn trả.
4. Quản lý Tồn kho (Inventory) chính xác, tuân thủ nguyên tắc tính giá vốn FIFO (First-In, First-Out).
5. Theo dõi sát sao Công nợ phải thu (Receivable Debt) của khách hàng theo từng chi nhánh.
6. Quản lý tập trung Master Data tại HQ và tự động phân phối xuống Chi nhánh.
7. Triển khai cơ chế phân quyền cho hai vai trò ADMIN và STAFF theo phạm vi truy cập (toàn hệ thống hoặc theo chi nhánh).
8. Xây dựng cơ chế đồng bộ dữ liệu hai hướng giữa HQ và Branch (mỗi bảng dữ liệu đồng bộ theo một chiều duy nhất — *single-writer-per-table*) đảm bảo tính toàn vẹn thông tin.
9. Cung cấp hệ thống Dashboard tổng hợp, báo cáo doanh thu — lợi nhuận gộp và cảnh báo tồn kho thấp.
10. Đảm bảo các tiêu chuẩn về tính toàn vẹn dữ liệu (chống trùng lặp, chống ghi đè đồng thời), bảo mật hệ thống và khả năng bảo trì mã nguồn.

## 1.4. Phạm vi nghiên cứu (Scope of the study)

### 1.4.1. Đối tượng nghiên cứu

- Quy trình nghiệp vụ ERP trong môi trường bán lẻ chuỗi phân tán.
- Các mô hình kiến trúc phần mềm phân tán đa điểm (HQ/Branch topology) — cụ thể là Modular Monolith kết hợp N-instance.
- Cơ chế đồng bộ dữ liệu giữa các cơ sở dữ liệu phân tán bằng PostgreSQL Logical Replication.
- Các thuật toán và quy trình quản lý tồn kho, đặc biệt là phương pháp tính giá vốn FIFO thông qua cấu trúc Cost Layer.
- Các giải pháp xác thực và phân quyền an toàn trong môi trường nhiều instance triển khai của cùng một ứng dụng.

### 1.4.2. Chức năng trong phạm vi phát triển

Hệ thống được giới hạn phát triển 10 module nghiệp vụ và kỹ thuật cốt lõi sau:

| Module | Chức năng chính |
|---|---|
| **Identity** | Quản lý xác thực (đăng nhập, refresh, revoke token) và phân quyền người dùng (Role/Permission). |
| **Branch** | Quản lý thông tin chi nhánh. |
| **Catalog** | Quản lý sản phẩm, danh mục, nhà cung cấp và bảng giá theo thời gian hiệu lực. |
| **CRM** | Quản lý thông tin khách hàng và công nợ phải thu (Receivable Debt), giá riêng theo khách hàng. |
| **Order** | Xử lý hóa đơn bán hàng (Sales Invoice) và phiếu trả hàng (Goods Return). |
| **Inventory** | Xử lý tồn kho, phiếu nhập kho (Inbound Receipt), biến động kho (Stock Movement) và tính giá vốn FIFO (Cost Layer). |
| **Analytics** | Phân tích số liệu, dashboard doanh thu/lợi nhuận và cảnh báo tồn kho thấp. |
| **Common** | Thành phần dùng chung giữa các module — cơ chế Idempotency, AOP, xử lý lỗi chuẩn hóa. |
| **Infrastructure** | Kết nối hạ tầng: cơ sở dữ liệu, cấu hình đồng bộ, bảo mật (JWT RS256). |
| **System** | Cấu hình khởi động, phân biệt vai trò instance (HQ/Branch) qua `instance.role`. |

### 1.4.3. Giới hạn ngoài phạm vi (Out of scope)

Các nghiệp vụ sau được xác định nằm ngoài phạm vi phát triển của đề tài, đóng vai trò định hướng mở rộng trong tương lai:

- Đặt hàng trước của khách hàng (Customer Order).
- Đơn đặt hàng gửi nhà cung cấp (Purchase Order) và Công nợ phải trả (Supplier Payable).
- Luân chuyển kho nội bộ dạng chứng từ riêng (Stock Transfer) và Phiếu xuất kho khác (Outbound Receipt) — việc luân chuyển hàng giữa các chi nhánh trong phạm vi đề tài được thực hiện gián tiếp thông qua giao dịch mua – bán thông thường giữa hai chi nhánh.
- Yêu cầu báo giá (RFQ), tích hợp Thanh toán trực tuyến (Online Payment) và phân hệ Trình chiếu sản phẩm ảo (Visualizer).
- Quy đổi đơn vị tính tự động và thuộc tính sản phẩm động (EAV) — mỗi sản phẩm sử dụng một đơn vị tính cố định, nhập thủ công.
- Thuật ngữ "Lô hàng" (Stock Lot) không được sử dụng; hệ thống áp dụng thống nhất thuật ngữ **Lớp giá (Cost Layer)**.

## 1.5. Ý nghĩa của đề tài (Significance of the study)

**Về mặt học thuật:** Đề tài là cơ hội vận dụng đồng thời kiến thức về phân tích thiết kế hệ thống hướng đối tượng (UML: Use Case, Activity, Class, Sequence), mô hình hóa dữ liệu (ERD, chuẩn hóa 3NF có denormalize chủ đích), và kiến trúc phần mềm phân tán (Modular Monolith, N-instance, Logical Replication, định lý CAP) vào một bài toán nghiệp vụ thực tế có độ phức tạp cao — nơi các quyết định thiết kế đều có đánh đổi (trade-off) rõ ràng và cần được lập luận bằng cơ sở lý thuyết.

**Về mặt thực tiễn:** Kết quả đề tài là một hệ thống ERP có thể vận hành thực tế cho một chuỗi cửa hàng vật liệu xây dựng quy mô vừa — giải quyết đúng các bài toán mà mô hình kinh doanh chuỗi tại Việt Nam thường gặp phải: chi nhánh hoạt động độc lập khi mất mạng, giá bán thương lượng linh hoạt, công nợ phải thu theo từng chi nhánh, và giá vốn hàng bán tuân thủ chuẩn kế toán FIFO theo Thông tư 200/2014/TT-BTC.

**Về mặt kỹ thuật:** Đề tài minh chứng khả năng áp dụng kiến trúc Modular Monolith kết hợp N-instance như một phương án trung gian hợp lý giữa Microservices (chi phí vận hành cao) và Monolith tập trung một CSDL (không đạt yêu cầu Availability khi chi nhánh mất kết nối WAN) — một lựa chọn kiến trúc phù hợp cho các hệ thống có quy mô vừa, đội ngũ phát triển nhỏ nhưng vẫn cần tính sẵn sàng cao ở cấp độ chi nhánh.

## 1.6. Cấu trúc của khóa luận (Structure of the thesis)

Báo cáo được tổ chức thành 5 chương như sau:

- **Chương 1 — Tổng quan đề tài:** trình bày bối cảnh, vấn đề cần giải quyết, mục tiêu, đối tượng, phạm vi và ý nghĩa của đề tài.
- **Chương 2 — Phân tích yêu cầu hệ thống:** trình bày cơ sở lý thuyết và các tiền lệ kiến trúc liên quan, phân tích đặc điểm bài toán, yêu cầu chức năng và phi chức năng, so sánh và lựa chọn phương án kiến trúc tổng thể.
- **Chương 3 — Phân tích nghiệp vụ và thiết kế hệ thống:** mô hình hóa hệ thống qua Use Case, Activity Diagram, Data Flow Diagram, Class Diagram và Entity Relationship Diagram; đặc tả chi tiết toàn bộ các use case nghiệp vụ.
- **Chương 4 — Triển khai hệ thống:** trình bày tổ chức mã nguồn, công nghệ sử dụng và việc triển khai chi tiết từng module backend cùng các cơ chế kỹ thuật cốt lõi (create-and-confirm, FIFO, Idempotency, Optimistic/Pessimistic Locking, bảo mật, đồng bộ dữ liệu).
- **Chương 5 — Kết luận và hướng phát triển:** tổng kết kết quả đạt được, hạn chế của hệ thống và định hướng phát triển trong tương lai.

Ngoài ra, báo cáo có phần Phụ lục trình bày danh mục đối tượng tham chiếu (lớp/thuộc tính), danh sách use case đầy đủ và danh mục API chính của hệ thống.

---

# CHƯƠNG 2. PHÂN TÍCH YÊU CẦU HỆ THỐNG

## 2.1. Bối cảnh nghiệp vụ (Business Context)

Doanh nghiệp được mô phỏng trong đề tài là một **chuỗi cửa hàng vật liệu xây dựng và thiết bị thông minh nhà (VLXD & TTNT)**, vận hành theo mô hình **1 Trụ sở (HQ) + N Chi nhánh (Branch)**. Mỗi chi nhánh hoạt động bán hàng độc lập, có kho riêng và công nợ khách hàng riêng, nhưng chia sẻ chung danh mục sản phẩm, thông tin khách hàng và chính sách giá do HQ ban hành. Mọi khách hàng đều bình đẳng về điều kiện giao dịch — không phân loại — và đều có thể mua ngay hoặc mua chịu tùy thỏa thuận với nhân viên bán hàng tại quầy.

Bảng dưới đây tóm tắt các vấn đề kinh doanh cốt lõi và giải pháp tương ứng mà hệ thống cần cung cấp:

| # | Vấn đề nghiệp vụ | Giải pháp hệ thống cung cấp |
|---|---|---|
| 1 | Quản lý nhiều chi nhánh thủ công, dữ liệu không đồng nhất | Nền tảng ERP thống nhất, Master Data quản lý tập trung tại HQ |
| 2 | Giá bán linh hoạt theo từng khách hàng, khó theo dõi | Cơ chế giá đa tầng: giá niêm yết → giá gần nhất theo khách hàng → giá thỏa thuận (override) |
| 3 | Tồn kho không chính xác khi nhiều người thao tác đồng thời | Trừ kho nguyên tử (atomic) trong 1 transaction, khóa đồng thời (Optimistic/Pessimistic Locking) |
| 4 | Công nợ khách hàng khó đối soát theo thời gian | Snapshot công nợ trước/sau trên từng hóa đơn, sổ nợ (Receivable Debt) riêng theo từng chi nhánh |
| 5 | Nhân viên chi nhánh này có thể truy cập dữ liệu chi nhánh khác | RBAC theo phạm vi chi nhánh (Branch-Level Security), xác thực JWT RS256 |
| 6 | Lợi nhuận gộp không phản ánh đúng từng lô hàng nhập | Tính giá vốn theo phương pháp FIFO qua Cost Layer, snapshot giá vốn trên từng dòng hóa đơn |
| 7 | Chi nhánh gián đoạn hoạt động khi mất kết nối đến HQ | Mỗi chi nhánh có CSDL riêng, vận hành cục bộ độc lập, đồng bộ dữ liệu bất đồng bộ (eventual consistency) |

## 2.2. Mô tả yêu cầu (Requirement Description)

Từ bối cảnh nghiệp vụ trên, hệ thống cần đáp ứng các nhóm yêu cầu chính sau, tương ứng với 10 module đã xác định ở Mục 1.4.2:

- **Nhóm Xác thực & Phân quyền:** cho phép người dùng đăng nhập, làm mới và thu hồi phiên đăng nhập; đảm bảo mỗi vai trò (ADMIN/STAFF) chỉ thao tác trong đúng phạm vi dữ liệu được cấp phép.
- **Nhóm Quản lý dữ liệu nền tảng (Master Data):** cho phép HQ quản lý sản phẩm, danh mục, nhà cung cấp, bảng giá và khách hàng; tự động phân phối các thay đổi xuống toàn bộ chi nhánh.
- **Nhóm Nghiệp vụ giao dịch (Transaction):** cho phép chi nhánh lập và xác nhận hóa đơn bán hàng, phiếu trả hàng, phiếu nhập kho — với mỗi giao dịch đều tác động đồng thời và nhất quán lên tồn kho, giá vốn và công nợ.
- **Nhóm Tồn kho & Giá vốn:** cho phép tra cứu tồn kho theo thời gian thực, truy vết lịch sử biến động kho, tính giá vốn hàng xuất theo FIFO.
- **Nhóm Công nợ:** cho phép tra cứu công nợ hiện tại và lịch sử biến động công nợ theo từng khách hàng, từng chi nhánh.
- **Nhóm Báo cáo & Phân tích:** cung cấp dashboard tổng hợp doanh thu, lợi nhuận gộp, và cảnh báo khi tồn kho xuống dưới ngưỡng cấu hình.
- **Nhóm Đồng bộ dữ liệu:** đảm bảo Master Data (HQ → Branch) và Transaction Data (Branch → HQ) được đồng bộ tự động, đúng chiều, không xung đột.

## 2.3. Đặc tả yêu cầu (Requirements Specification)

### 2.3.1. Yêu cầu chức năng (Functional Requirements)

> **Ghi chú về rà soát tính nhất quán:** So với bản nháp trước, bảng FR dưới đây được biên soạn lại để mỗi FR ánh xạ được đúng 1 (hoặc 1 nhóm) Use Case ở Chương 3 — theo đúng nguyên tắc truy vết bắt buộc. Hai chỉnh sửa đáng chú ý: (1) bổ sung **FR-04** cho nghiệp vụ quản lý tài khoản/phân quyền (UC02) — bị thiếu trong bản liệt kê trước; (2) tách yêu cầu "quản lý nhà cung cấp" (Actor Staff, UC05) ra khỏi yêu cầu "quản lý bảng giá" (Actor Admin, UC06) vì trước đây bị gộp chung vào một FR có Actor mâu thuẫn nhau; (3) yêu cầu chống trùng lặp (Idempotency) được xác định lại là một **thuộc tính chất lượng**, thuộc về NFR-05, không lặp lại thành FR riêng; (4) bổ sung Use Case **UC15 — Đồng bộ dữ liệu tự động** (Actor: Hệ thống) làm nơi neo cho FR-20/FR-21, khép kín 100% FR đều có UC tương ứng (xem bảng truy vết tại Mục 3.7.2).

| **Mã yêu cầu** | **Tên yêu cầu** | **Mô tả** | **Actor** | **Mức ưu tiên (MoSCoW)** |
|---|---|---|---|---|
| FR-01 | Đăng nhập | Người dùng đăng nhập bằng username/password, nhận Access Token và Refresh Token (JWT RS256). | Admin, Staff | Must have |
| FR-02 | Làm mới token | Người dùng làm mới (refresh) Access Token bằng Refresh Token còn hiệu lực. | Admin, Staff | Must have |
| FR-03 | Thu hồi token | Người dùng/hệ thống thu hồi (revoke) token đang hoạt động khi đăng xuất. | Admin, Staff | Should have |
| FR-04 | Quản lý tài khoản & phân quyền | Tạo/sửa tài khoản người dùng, gán Role (ADMIN/STAFF) và phạm vi chi nhánh qua `UserBranchRole`. | Admin | Must have |
| FR-05 | Quản lý chi nhánh | Thêm/sửa/vô hiệu hóa thông tin chi nhánh trong mạng lưới. | Admin | Must have |
| FR-06 | Quản lý danh mục & sản phẩm | Quản lý danh mục sản phẩm phân cấp cha/con và thông tin sản phẩm (SKU, tên, đơn vị tính, hình ảnh). | Admin | Must have |
| FR-07 | Quản lý nhà cung cấp | Tạo/sửa thông tin nhà cung cấp phục vụ nhập kho, theo phạm vi chi nhánh. | Staff | Should have |
| FR-08 | Quản lý bảng giá | Thiết lập giá niêm yết theo sản phẩm × chi nhánh × thời gian hiệu lực (append-only, không ghi đè giá cũ). | Admin | Must have |
| FR-09 | Xem báo cáo tổng hợp | Xem Dashboard doanh thu, lợi nhuận gộp, vòng quay tồn kho, top sản phẩm bán chạy/chậm. | Admin | Should have |
| FR-10 | Quản lý khách hàng | Tạo/sửa hồ sơ khách hàng dùng chung toàn hệ thống. | Admin | Must have |
| FR-11 | Gợi ý & ghi đè giá bán | Gợi ý giá theo thứ tự ưu tiên (giá gần nhất khách hàng đã mua → giá niêm yết hiệu lực); cho phép nhân viên ghi đè (override). | Staff | Should have |
| FR-12 | Lập hóa đơn bán hàng (Draft) | Tạo hóa đơn nháp, thêm/sửa dòng sản phẩm trước khi xác nhận. | Staff | Must have |
| FR-13 | Xác nhận hóa đơn bán hàng | Xác nhận hóa đơn trong một giao dịch nguyên tố (create-and-confirm): trừ kho theo FIFO, snapshot giá vốn, cập nhật công nợ. | Staff | Must have |
| FR-14 | Lập & xác nhận phiếu trả hàng | Ghi nhận hàng khách trả lại; hoàn kho, giảm công nợ, có thể liên kết hóa đơn gốc. | Staff | Must have |
| FR-15 | Lập & xác nhận phiếu nhập kho | Ghi nhận hàng nhập từ nhà cung cấp; tăng tồn kho, sinh Cost Layer mới. | Staff | Must have |
| FR-16 | Tra cứu tồn kho hiện tại | Xem số lượng tồn kho hiện tại theo sản phẩm × chi nhánh. | Staff, Admin | Must have |
| FR-17 | Tra cứu lịch sử biến động kho | Xem lịch sử `Stock Movement` theo sản phẩm × chi nhánh × thời gian. | Staff, Admin | Should have |
| FR-18 | Tra cứu công nợ | Tra cứu số dư và lịch sử biến động công nợ hiện tại theo khách hàng. | Staff, Admin | Must have |
| FR-19 | Ghi nhận thanh toán công nợ | Ghi nhận khách hàng thanh toán, giảm trừ số dư nợ hiện tại. | Staff | Should have |
| FR-20 | Đồng bộ Master Data | Tự động đồng bộ Master Data (sản phẩm, danh mục, giá, khách hàng…) từ HQ xuống toàn bộ chi nhánh. | Hệ thống | Must have |
| FR-21 | Đồng bộ Transaction Data | Tự động đồng bộ Transaction Data từ mỗi chi nhánh về HQ (lọc dòng theo `branch_id` với các bảng snapshot). | Hệ thống | Must have |
| FR-22 | Cảnh báo tồn kho thấp | Tự động cảnh báo khi tồn kho một sản phẩm tại một chi nhánh xuống dưới ngưỡng cấu hình. | Hệ thống | Could have |

*Bảng 2-1: Đặc tả yêu cầu chức năng (Functional Requirements)*

### 2.3.2. Yêu cầu phi chức năng (Non-Functional Requirements)

| **Mã** | **Yêu cầu** | **Mô tả (có số đo cụ thể)** | **Actor** | **Mức ưu tiên** |
|---|---|---|---|---|
| NFR-01 | Availability | Chi nhánh xử lý được 100% nghiệp vụ bán hàng/nhập kho cục bộ kể cả khi mất kết nối WAN tới HQ tối đa 24 giờ liên tục; mục tiêu uptime cục bộ tại Branch ≥ 99,5%. *Chuẩn tham chiếu: Google SRE — Site Reliability Engineering (Error Budget); AWS Well-Architected Framework — Reliability Pillar.* | Staff (Branch) | Must have |
| NFR-02 | Integrity | Mọi hành động CONFIRM (bán hàng/nhập kho/trả hàng) là một transaction ACID nguyên tố; tỉ lệ giao dịch dở dang (partial commit) = 0%. *Chuẩn tham chiếu: ISO/IEC 9075 (SQL — thuộc tính ACID).* | Staff | Must have |
| NFR-03 | Consistency | Độ trễ đồng bộ dữ liệu Branch ↔ HQ ở điều kiện mạng bình thường không vượt quá 5 giây (RPO ≈ 5s); hệ thống chấp nhận eventual consistency theo định lý CAP. *Chuẩn tham chiếu: NIST SP 800-34 (khái niệm RPO); Gilbert & Lynch, CAP theorem (2002).* | Admin (đọc dữ liệu tổng hợp tại HQ) | Must have |
| NFR-04 | Concurrency Safety | Xử lý đúng ≥ 99,9% giao dịch concurrent trên cùng một dòng tồn kho/lớp giá vốn mà không sai lệch số liệu; tự động `@Retryable` tối đa 3 lần khi xung đột optimistic lock. *Chuẩn tham chiếu: Spring Data JPA Locking Reference; ANSI SQL Transaction Isolation Levels.* | Staff (nhiều nhân viên thao tác đồng thời) | Must have |
| NFR-05 | Idempotency | 100% API POST/PUT thay đổi trạng thái chống được duplicate request qua header `Idempotency-Key`, hiệu lực tối thiểu 24 giờ; hash request khác nhau trên cùng key → HTTP 409. *Chuẩn tham chiếu: RFC 9110 (HTTP Semantics — Idempotent Methods); Stripe API — Idempotency Keys design pattern.* | Staff (client gửi lại request) | Must have |
| NFR-06 | Security | Access Token hạn tối đa 30 phút, Refresh Token tối đa 7 ngày; mật khẩu băm BCrypt (cost ≥ 10); Branch chỉ giữ public key, không giữ private key ký token. *Chuẩn tham chiếu: OWASP ASVS v4 (Application Security Verification Standard); NIST SP 800-63B (Digital Identity Guidelines).* | Admin, Staff | Must have |
| NFR-07 | Scalability | Thêm 1 chi nhánh mới chỉ cần cấu hình instance + bật publication/subscription, không sửa mã nguồn; thời gian triển khai 1 chi nhánh mới ước tính ≤ 1 ngày làm việc. *Chuẩn tham chiếu: AWS Well-Architected Framework — Performance Efficiency Pillar.* | Admin (vận hành, mở chi nhánh mới) | Should have |
| NFR-08 | Maintainability | Một codebase duy nhất triển khai mọi instance; độ phủ kiểm thử (line coverage) cho các luồng nghiệp vụ lõi (Sales/Return/Inbound) tối thiểu 80%. *Chuẩn tham chiếu: ISO/IEC 25010 (Software Quality — Maintainability); Google SRE (giảm Toil vận hành).* | Đội ngũ phát triển | Should have |
| NFR-09 | Auditability | 100% giao dịch CONFIRMED có dấu thời gian `created_at`/`confirmed_at` và người thực hiện (`performed_by`); lưu trữ tối thiểu 5 năm theo quy định kế toán. *Chuẩn tham chiếu: Thông tư 200/2014/TT-BTC (lưu trữ chứng từ kế toán); NIST SP 800-92 (Guide to Computer Security Log Management).* | Admin (đối soát, kiểm toán) | Must have |
| NFR-10 | Testability | Kiểm thử theo 4 cấp độ (Unit/Web-layer/Integration/E2E); độ phủ mã nguồn tổng thể ≥ 70%; luồng create-and-confirm bắt buộc có Integration Test dùng Testcontainers PostgreSQL thật. *Chuẩn tham chiếu: ISO/IEC 25010; Google Testing Blog — Test Pyramid.* | Đội ngũ phát triển | Should have |

*Bảng 2-2: Đặc tả yêu cầu phi chức năng (Non-Functional Requirements)*

## 2.4. Cơ sở lý thuyết và các công trình liên quan

Việc thiết kế một hệ thống phân tán đòi hỏi lựa chọn kiến trúc dựa trên cơ sở khoa học và tiền lệ thực tiễn. Dưới đây là các nghiên cứu và hệ thống tham khảo làm nền tảng cho các quyết định kiến trúc ở Mục 2.5–2.6.

**Modular Monolith so với Microservices.** Các nghiên cứu thực nghiệm công bố trên ArXiv giai đoạn 2022–2024 [2] chỉ ra rằng Modular Monolith là lựa chọn phù hợp cho hệ thống quy mô vừa, tránh chi phí vận hành lớn của Microservices. Thực tiễn từ Amazon Prime Video (2023) [3] cũng cho thấy việc chuyển một service giám sát từ kiến trúc Microservices phân tán về dạng Monolith giúp giảm đáng kể độ phức tạp vận hành và chi phí hạ tầng (báo cáo giảm 90% chi phí). Microservices thường mang lại overhead lớn về quản lý phân tán và độ trễ giao tiếp mạng; với một ERP đa chi nhánh có đội ngũ phát triển nhỏ, Modular Monolith giúp duy trì một mã nguồn duy nhất với ranh giới module rõ ràng, giảm chi phí triển khai mà vẫn đảm bảo tính độc lập giữa các Bounded Context.

**Đồng bộ dữ liệu phân tán bằng PostgreSQL Logical Replication.** Theo tài liệu chính thức của PostgreSQL [4], Logical Replication cho phép nhân bản dữ liệu có chọn lọc (theo bảng, thậm chí theo dòng qua row filter) giữa các cơ sở dữ liệu độc lập, hoạt động dựa trên cơ chế publish/subscribe đọc từ Write-Ahead Log (WAL). Cơ chế này giải quyết trực tiếp bài toán đồng bộ hai chiều (Master Data từ HQ xuống Branch, Transaction Data từ Branch lên HQ) mà không cần một tầng middleware trung gian ở cấp ứng dụng.

**Định lý CAP và tính nhất quán cuối (Eventual Consistency).** Định lý CAP (Gilbert & Lynch, 2002) [1] khẳng định một hệ thống phân tán không thể đồng thời đạt cả ba tính chất Consistency, Availability và Partition Tolerance khi có sự cố phân vùng mạng. Kiến trúc Dynamo của Amazon (DeCandia et al., 2007) [5] minh chứng việc ưu tiên Availability hơn Consistency tức thời là lựa chọn phù hợp cho các hệ thống thương mại cần luôn sẵn sàng phục vụ giao dịch. Áp dụng vào bài toán của đề tài: việc ưu tiên tính sẵn sàng ở cấp độ chi nhánh là bắt buộc — hệ thống chấp nhận độ trễ đồng bộ để đảm bảo chi nhánh luôn thao tác được độc lập, kể cả khi mất kết nối tạm thời tới HQ.

**Hệ thống ERP thực tế tương đương.** SAP Business One với giải pháp Intercompany Integration [9] và Odoo Multi-company [10] đều cung cấp mô hình vận hành chi nhánh độc lập với cơ sở dữ liệu tách biệt và cơ chế đồng bộ dữ liệu về trung tâm — chứng minh tính khả thi của mô hình HQ + N Branch hoạt động độc lập trong các sản phẩm ERP thương mại hàng đầu. Khác với các giải pháp này thường dùng bộ đồng bộ (middleware) riêng, đề tài lựa chọn tận dụng trực tiếp tính năng Logical Replication có sẵn của PostgreSQL nhằm giảm chi phí hạ tầng và độ phức tạp vận hành.

## 2.5. Phân tích và lựa chọn phương án kiến trúc

Dựa trên yêu cầu phi chức năng đã xác định ở Mục 2.3.2 (đặc biệt là Availability, Integrity, Security, Scalability, Maintainability), ba phương án kiến trúc được so sánh:

| **Tiêu chí** | **Monolith tập trung (1 CSDL)** | **Microservices** | **Modular Monolith + N-instance (đã chọn)** |
|---|---|---|---|
| Availability (khi mất mạng WAN) | Không đạt — Branch không hoạt động được nếu mất kết nối tới CSDL trung tâm | Đạt, nhưng cần hạ tầng message queue phức tạp để bù độ trễ mạng | Đạt — mỗi Branch có CSDL riêng, vận hành độc lập |
| Integrity | Cao (1 CSDL duy nhất, transaction dễ đảm bảo) | Khó đảm bảo xuyên service nếu không có saga/distributed transaction | Cao trong phạm vi cục bộ mỗi Branch, đồng bộ dần về HQ |
| Security | Đơn giản (1 điểm kiểm soát) | Phức tạp hơn (nhiều điểm vào, nhiều service cần bảo vệ) | Trung bình — cần xác thực offline tại Branch (Mục 4.7) |
| Scalability (thêm chi nhánh mới) | Kém — mọi Branch cùng tải lên 1 CSDL | Tốt nhưng chi phí vận hành cao | Tốt — thêm 1 instance Branch, không đổi mã nguồn |
| Maintainability | Cao (1 codebase) | Thấp — nhiều service, nhiều pipeline CI/CD | Cao — 1 codebase, ranh giới module rõ ràng |

**Phương án được chọn: Modular Monolith + N-instance**, vì đáp ứng toàn diện các tiêu chí trên: tính độc lập cho chi nhánh (có CSDL riêng), duy trì sự đơn giản trong phát triển (1 codebase), đảm bảo tính nhất quán dữ liệu ở phạm vi cục bộ, và tận dụng trực tiếp Logical Replication của PostgreSQL — phù hợp với quy mô và nguồn lực phát triển của đề tài.

## 2.6. Thiết kế kiến trúc tổng thể

### 2.6.1. Kiến trúc hệ thống tổng thể

Hệ thống được tổ chức theo mô hình Hub-and-Spoke. HQ đóng vai trò Hub, triển khai ứng dụng Spring Boot và PostgreSQL riêng. Các Branch (Spoke) chạy độc lập, mỗi Branch có ứng dụng và CSDL riêng, giao tiếp với HQ qua Logical Replication.

> **[DIAGRAM — Hình 2-1: Kiến trúc tổng thể HQ + Branch]**
>
> *Hình 2-1: Kiến trúc tổng thể HQ + Branch*

### 2.6.2. Cấu trúc module Backend

Ứng dụng Backend được thiết kế với 10 module (xem chi tiết Mục 1.4.2 và Chương 4). Các module giao tiếp với nhau duy nhất qua Facade để hạn chế sự phụ thuộc chéo.

> **[DIAGRAM — Hình 2-2: Kiến trúc Module Backend]**
>
> *Hình 2-2: Kiến trúc Module Backend*

### 2.6.3. Mô hình triển khai (Runtime Topology)

Triển khai thông qua Docker; mỗi instance là một cụm tài nguyên độc lập gồm Nginx, ứng dụng Backend và CSDL tương ứng.

> **[DIAGRAM — Hình 2-3: Deployment / Runtime Topology]**
>
> *Hình 2-3: Deployment / Runtime Topology*

### 2.6.4. Cơ chế nhân bản dữ liệu (Logical Replication)

Thiết lập đồng bộ theo nguyên tắc *single-writer-per-table*: HQ đẩy Master Data xuống Branch; Branch đẩy Transaction Data về HQ. Chi tiết cấu hình publication/subscription được trình bày tại Mục 4.8.

> **[DIAGRAM — Hình 2-4: Cơ chế Logical Replication]**
>
> *Hình 2-4: Cơ chế Logical Replication*

### 2.6.5. Thiết kế bảo mật và kiểm soát đồng thời tổng quan

HQ là đơn vị duy nhất phát hành token JWT (ký bằng RS256); Branch giữ khóa công khai để xác thực ngoại tuyến. Toàn vẹn xử lý dựa trên kết hợp Optimistic Locking (`@Version`) và Pessimistic Locking (`SELECT ... FOR UPDATE`), cùng cơ chế Idempotency chống trùng lặp request. Chi tiết triển khai được trình bày tại Mục 4.6 và Mục 4.7.

> **[DIAGRAM — Hình 2-5: Kiến trúc bảo mật]**
>
> *Hình 2-5: Kiến trúc bảo mật (JWT RS256 + Branch Scope)*
# CHƯƠNG 3. PHÂN TÍCH NGHIỆP VỤ VÀ THIẾT KẾ HỆ THỐNG

> **Ghi chú phương pháp luận (đọc trước khi xem Chương 3):** Chương này áp dụng quy trình phân tích – thiết kế hướng đối tượng theo 4 tầng mô hình hóa chuẩn (Functional → Structural → Data → System Design), truy vết chặt chẽ theo chuỗi **FR → Use Case → Basic Flow (đánh số) → Activity Diagram (đúng số bước) → Sequence Diagram (đúng số message)**. Hai điều chỉnh có chủ đích so với khuôn mẫu gốc, đã được ghi nhận minh bạch:
> 1. **Phạm vi Activity/Sequence Diagram:** vì đề tài chỉ triển khai backend (không có giao diện tự code — xem Mục 1.4.2), Activity Diagram và Sequence Diagram chỉ được xây dựng đầy đủ cho các Use Case có luồng xử lý nghiệp vụ nhiều bước, nhiều rẽ nhánh và có ý nghĩa kỹ thuật cao (UC01, UC09, UC10, UC11, UC12, UC15) — đây cũng là các luồng có nhiều Activity/Sequence Diagram nhất trong tài liệu gốc `01-noi-dung-bao-cao-v3.md`. Các Use Case quản trị dữ liệu nền tảng thuần CRUD (UC02–UC08, UC13, UC14) chỉ trình bày Use Case Specification vì luồng xử lý là thao tác đơn giản, không có nhánh rẽ nghiệp vụ đáng kể.
> 2. **Ký hiệu Sequence Diagram:** thay vì quy ước Boundary–Control–Entity thuần túy (`frmX`/`ctlX`) vốn dùng cho hệ thống có giao diện, các Sequence Diagram dưới đây dùng trực tiếp tên Controller/Service/Facade thực tế của mã nguồn (đóng vai trò Control) và Entity/Repository thực tế (đóng vai trò Entity) — giữ đúng tinh thần truy vết kỹ thuật nhưng phản ánh đúng kiến trúc backend-only của hệ thống.
>
> Việc rà soát tính nhất quán giữa các tầng mô hình hóa (đúng số bước, đúng tên gọi, đúng đánh số) được tổng hợp tường minh tại Mục 3.7 — Kiểm tra tính nhất quán.

## 3.1. Yêu cầu chức năng và Sơ đồ phân cấp chức năng

Yêu cầu chức năng (FR-01 → FR-22) đã được đặc tả đầy đủ tại Mục 2.3.1. Mục này minh họa lại các yêu cầu đó dưới dạng **Sơ đồ phân cấp chức năng (Functional Hierarchy Diagram)** theo từng actor, làm cơ sở trực quan trước khi đi vào Sơ đồ Use Case chi tiết ở Mục 3.2.

**Sơ đồ phân cấp chức năng — Admin:**

> **[DIAGRAM — Hình 3-1: Sơ đồ phân cấp chức năng — Admin]**
>
> *Hình 3-1: Sơ đồ phân cấp chức năng — Admin*

**Sơ đồ phân cấp chức năng — Staff:**

> **[DIAGRAM — Hình 3-2: Sơ đồ phân cấp chức năng — Staff]**
>
> *Hình 3-2: Sơ đồ phân cấp chức năng — Staff*

> **Ghi chú:** Nghiệp vụ đồng bộ dữ liệu (UC15) do actor **Hệ thống** (không phải người dùng) khởi tạo tự động, nên không xuất hiện trong hai cây chức năng trên — được trình bày riêng tại Mục 3.3 cùng Sequence Diagram tương ứng (Hình 3-13).

## 3.2. Mô hình hóa chức năng (Functional Modeling)

### 3.2.1. Sơ đồ Use Case tổng quát

Hệ thống ERP bán lẻ đa chi nhánh phân tách quyền hạn dựa trên hai tác nhân người dùng chính — **Admin** tại Trụ sở chính (HQ) và **Staff** tại Chi nhánh (Branch) — cùng một tác nhân hệ thống (**Hệ thống**, actor tự động) phục vụ đồng bộ dữ liệu. Do số lượng chức năng phong phú, sơ đồ Use Case tổng quát được tách thành hai biểu đồ theo actor người dùng, thể hiện rõ ranh giới nghiệp vụ.

**Sơ đồ Use Case tổng quát — Admin:**

> **[DIAGRAM — Hình 3-3: Sơ đồ Use Case tổng quát — Admin]**
>
> *Hình 3-3: Sơ đồ Use Case tổng quát — Admin*

**Sơ đồ Use Case tổng quát — Staff:**

> **[DIAGRAM — Hình 3-4: Sơ đồ Use Case tổng quát — Staff]**
>
> *Hình 3-4: Sơ đồ Use Case tổng quát — Staff*

### 3.2.2. Mô tả Actor

**Admin.** Đặt trụ sở tại Trụ sở chính (HQ), Admin chịu trách nhiệm thiết lập và duy trì toàn bộ dữ liệu nền tảng (Master Data) dùng chung cho mạng lưới — tài khoản người dùng và phân quyền, chi nhánh, danh mục/sản phẩm, bảng giá niêm yết, khách hàng — đồng thời là actor duy nhất được xem Dashboard báo cáo tổng hợp trên phạm vi toàn hệ thống. Mọi thao tác ghi dữ liệu Master Data của Admin chỉ thực hiện được tại instance HQ.

**Staff.** Đặt tại từng Chi nhánh (Branch), Staff vận hành trực tiếp các nghiệp vụ giao dịch phát sinh tại cửa hàng — lập và xác nhận hóa đơn bán hàng, phiếu trả hàng, phiếu nhập kho — đồng thời quản lý nhà cung cấp cục bộ, tra cứu giá riêng theo khách hàng, tra cứu tồn kho và theo dõi công nợ trong phạm vi chi nhánh mình đang đăng nhập. Mọi thao tác của Staff bị giới hạn `branchId` lấy từ JWT, không thể truy cập dữ liệu giao dịch của chi nhánh khác.

**Hệ thống (System).** Là actor tự động, không có giao diện tương tác trực tiếp — đại diện cho tiến trình nền PostgreSQL Logical Replication, tự động phát hiện thay đổi dữ liệu và đẩy đồng bộ hai chiều giữa HQ và Branch (UC15) mà không cần người dùng khởi tạo.

### 3.2.3. Danh sách Use Case hệ thống

| **Mã UC** | **Tên Use Case** | **Actor(s)** | **Điều kiện (tóm tắt)** | **Mô tả** |
|---|---|---|---|---|
| UC01 | Đăng nhập / Refresh / Revoke | Admin, Staff | Tài khoản đã được tạo (UC02), `is_active = true` | Xác thực người dùng, cấp/làm mới/thu hồi JWT. |
| UC02 | Quản lý tài khoản và phân quyền | Admin | Đã đăng nhập với quyền Admin | Tạo/sửa tài khoản người dùng, gán Role và phạm vi Chi nhánh. |
| UC03 | Quản lý chi nhánh | Admin | Chỉ thực hiện tại instance HQ | Tạo/sửa/vô hiệu hóa thông tin chi nhánh. |
| UC04 | Quản lý sản phẩm và danh mục | Admin | Chỉ thực hiện tại instance HQ | Tạo/sửa danh mục phân cấp và sản phẩm. |
| UC05 | Quản lý nhà cung cấp | Staff | Đã đăng nhập với `branchId` hợp lệ | Tạo/sửa thông tin nhà cung cấp theo phạm vi chi nhánh. |
| UC06 | Quản lý bảng giá | Admin | Sản phẩm và chi nhánh đã tồn tại (UC04, UC03) | Thiết lập giá niêm yết theo sản phẩm × chi nhánh × thời gian hiệu lực. |
| UC07 | Xem báo cáo tổng hợp | Admin | Dữ liệu Transaction đã đồng bộ về HQ | Xem Dashboard doanh thu, lợi nhuận gộp, cảnh báo tồn kho thấp. |
| UC08 | Quản lý khách hàng | Admin | Chỉ thực hiện tại instance HQ | Tạo/sửa hồ sơ khách hàng dùng chung toàn hệ thống. |
| UC09 | Tra cứu giá riêng theo khách hàng | Staff | Khách hàng và sản phẩm đã tồn tại | Tra cứu giá gợi ý khi thêm sản phẩm vào hóa đơn. |
| UC10 | Lập hóa đơn bán hàng | Staff | Khách hàng (UC08), sản phẩm (UC04) đã tồn tại | Tạo và xác nhận hóa đơn bán hàng (`createAndConfirm`). |
| UC11 | Lập phiếu trả hàng | Staff | Khách hàng tồn tại; hóa đơn gốc (nếu liên kết) đã CONFIRMED | Tạo và xác nhận phiếu nhận lại hàng từ khách hàng. |
| UC12 | Lập phiếu nhập kho | Staff | Nhà cung cấp (UC05), sản phẩm (UC04) đã tồn tại | Ghi nhận hàng nhập từ nhà cung cấp. |
| UC13 | Tra cứu tồn kho | Staff, Admin | Đã đăng nhập với phạm vi chi nhánh phù hợp | Xem số lượng tồn kho hiện tại và lịch sử biến động kho. |
| UC14 | Theo dõi công nợ | Staff, Admin | Khách hàng đã tồn tại | Xem số dư và lịch sử biến động công nợ của khách hàng. |
| UC15 | Đồng bộ dữ liệu tự động | Hệ thống | Publication/Subscription đã cấu hình (Mục 4.8) | Đồng bộ hai chiều Master Data (HQ→Branch) và Transaction Data (Branch→HQ) qua Logical Replication. |

*Bảng 3-1: Danh sách Use Case hệ thống*

> **Ghi chú về đánh số UC:** Mã UC01–UC14 dùng thống nhất theo `GLOSSARY.md` của dự án; UC15 được bổ sung trong lần rà soát này để làm nơi neo truy vết cho FR-20/FR-21 (đồng bộ dữ liệu), khép kín nguyên tắc "mỗi FR có ít nhất 1 UC tương ứng" (xem Mục 3.7.2). Bản nháp trước đây (`BAOCAO_HOANCHINH_V2.md`) từng gán nhầm mã UC09 cho "Quản lý nhà cung cấp" (đúng ra là UC05) — đã được sửa trong toàn bộ chương này.
>
> **Điểm cần xác nhận lại với mã nguồn:** UC05 được xếp Actor là **Staff** theo `DATA_SCHEMA.md` hiện hành (bảng `supplier` được xác nhận Branch-owned kể từ bản v5). Tài liệu `BUSINESS_ANALYSIS.md` (cũ hơn) mô tả Supplier là HQ Master — đây là điểm mâu thuẫn giữa hai tài liệu nguồn, bạn nên đối chiếu lại với `SupplierController` thực tế trước khi nộp báo cáo.

---

## 3.3. Đặc tả chi tiết các Use Case

### UC01 — Đăng nhập / Refresh / Revoke

| Thuộc tính | Nội dung |
|---|---|
| **Mã Use Case** | UC01 |
| **Tên Use Case** | Đăng nhập / Refresh / Revoke |
| **Mô tả** | Người dùng xác thực bằng username/password để nhận Access Token và Refresh Token; có thể làm mới token khi gần hết hạn hoặc thu hồi token khi đăng xuất. |
| **Actor(s)** | Admin, Staff |
| **Trigger** | Người dùng mở màn hình đăng nhập và nhập thông tin xác thực (hoặc Access Token sắp/đã hết hạn, hoặc người dùng chọn đăng xuất). |
| **Precondition(s)** | Tài khoản đã được Admin tạo trước (UC02), `is_active = true`. |
| **Post-Condition** | Người dùng nhận JWT hợp lệ chứa `sub`, `role`, `branchId`, `tokenId`, `type`, `iat`, `exp`; mọi request tiếp theo đính kèm `Authorization: Bearer <token>`. |
| **Basic Flow** | 1. Người dùng nhập username/password tại màn hình đăng nhập.<br>2. Hệ thống (`AuthController`) chuyển yêu cầu tới `AuthService`.<br>3. `AuthService` tra `UserRepository` theo username, xác minh mật khẩu bằng `PasswordEncoder` (BCrypt).<br>4. Hệ thống tra `UserBranchRoleRepository` để xác định Role và `branchId` áp dụng cho token.<br>5. `JwtTokenProvider` ký Access Token (hạn 30 phút) và Refresh Token (hạn 7 ngày) bằng RSA private key (RS256).<br>6. Hệ thống trả về cặp token cho client. |
| **Alternative Flow** | **A1 — Refresh:**<br>7. Client gửi Refresh Token còn hiệu lực tới `/api/v1/auth/refresh` (thay cho Bước 1–2).<br>8. Hệ thống xác minh chữ ký và `type=refresh`, cấp Access Token mới; luồng trở về Bước 6.<br>**A2 — Revoke:**<br>9. Client gửi yêu cầu thu hồi tới `/api/v1/auth/revoke` kèm `tokenId`.<br>10. Hệ thống đánh dấu token không còn hiệu lực cho các lần xác thực tiếp theo; luồng kết thúc. |
| **Exception Flow** | Sai username/password → HTTP 401, luồng trở về Bước 1. Tài khoản bị vô hiệu hóa (`is_active=false`) → từ chối đăng nhập. Token hết hạn hoặc chữ ký không hợp lệ → HTTP 401. |
| **Quy tắc nghiệp vụ liên quan** | Chỉ HQ giữ RSA private key và có quyền ký token; Branch chỉ giữ public key, xác minh token hoàn toàn offline (không gọi lại HQ ở mỗi request) — xem Mục 4.7. |
| **Tham chiếu kỹ thuật** | `AuthController`, `AuthService`, `JwtTokenProvider`, `JwtAuthenticationFilter`. |

*Bảng 3-2: Đặc tả Use Case UC01*

Vì UC01 có luồng thay thế rõ ràng (Refresh/Revoke) và mang tính kỹ thuật cao (ký/xác minh JWT), sơ đồ tuần tự sau minh họa chi tiết luồng chính và cả hai luồng thay thế, đúng số bước 1–10 ở bảng trên:

> **[DIAGRAM — Hình 3-5: Sequence — Đăng nhập / Refresh / Revoke]**
>
> *Hình 3-5: Sequence Diagram — UC01 Đăng nhập / Refresh / Revoke*

### UC02 — Quản lý tài khoản và phân quyền

| Thuộc tính | Nội dung |
|---|---|
| **Mã Use Case** | UC02 |
| **Tên Use Case** | Quản lý tài khoản và phân quyền |
| **Mô tả** | Tạo tài khoản người dùng mới, gán một hoặc nhiều cặp (Role, Chi nhánh) thông qua `UserBranchRole`. |
| **Actor(s)** | Admin |
| **Trigger** | Admin chọn chức năng "Thêm tài khoản mới" hoặc "Gán vai trò" trên màn hình quản trị người dùng. |
| **Precondition(s)** | Admin đã đăng nhập với quyền phù hợp; Role (`ADMIN`/`STAFF`) đã tồn tại trong hệ thống. |
| **Post-Condition** | Tài khoản có thể đăng nhập (UC01) với đúng phạm vi quyền đã gán. |
| **Basic Flow** | 1. Admin nhập thông tin tài khoản (username, họ tên, mật khẩu ban đầu).<br>2. Hệ thống mã hóa mật khẩu bằng BCrypt, lưu `user_account`.<br>3. Admin gán Role cho tài khoản — nếu là `STAFF`, bắt buộc chọn `branch_id`; nếu là `ADMIN` toàn hệ thống, `branch_id = NULL`.<br>4. Hệ thống ghi bản ghi vào `user_branch_role` (ràng buộc duy nhất theo `user_id, role_id, COALESCE(branch_id,0)`). |
| **Alternative Flow** | 5. Admin có thể vô hiệu hóa tài khoản (`is_active=false`) thay vì xóa vật lý (soft-delete); luồng trở về Bước 1 nếu cần tạo tài khoản khác. |
| **Exception Flow** | Username trùng lặp → từ chối tạo (vi phạm `UNIQUE`), luồng trở về Bước 1. Gán cùng một (Role, Chi nhánh) hai lần cho một user → vi phạm chỉ mục duy nhất, luồng trở về Bước 3. |
| **Quy tắc nghiệp vụ liên quan** | Hệ thống chỉ có 2 Role: `ADMIN` và `STAFF` (các vai trò cũ như STORE_MANAGER, SALES_STAFF đã bị loại bỏ). |
| **Tham chiếu kỹ thuật** | Module `identity` — `UserAccount`, `Role`, `Permission`, `RolePermission`, `UserBranchRole`. |

*Bảng 3-3: Đặc tả Use Case UC02*

### UC03 — Quản lý chi nhánh

| Thuộc tính | Nội dung |
|---|---|
| **Mã Use Case** | UC03 |
| **Tên Use Case** | Quản lý chi nhánh |
| **Mô tả** | Tạo, cập nhật thông tin và vô hiệu hóa chi nhánh trong mạng lưới. |
| **Actor(s)** | Admin |
| **Trigger** | Admin chọn chức năng "Thêm/Sửa chi nhánh" trên màn hình quản trị chi nhánh. |
| **Precondition(s)** | Chỉ thực hiện được tại instance HQ (`GET /api/v1/branches` là "HQ-only controller"). |
| **Post-Condition** | Chi nhánh mới xuất hiện trong danh sách lựa chọn khi gán `UserBranchRole` (UC02) hoặc khi cấu hình `PriceList` theo chi nhánh (UC06). |
| **Basic Flow** | 1. Admin nhập mã chi nhánh (duy nhất), tên, địa chỉ, số điện thoại, giờ mở cửa.<br>2. Hệ thống lưu bản ghi `branch` tại CSDL HQ.<br>3. Bản ghi được tự động đồng bộ (UC15, publication `pub_hq_to_<branch>`) xuống toàn bộ Chi nhánh hiện có. |
| **Alternative Flow** | 4. Admin vô hiệu hóa chi nhánh: `is_active=false` — chi nhánh vẫn còn dữ liệu lịch sử nhưng không nhận giao dịch mới; luồng trở về Bước 1. |
| **Exception Flow** | Mã chi nhánh trùng lặp → từ chối (vi phạm `UNIQUE`), luồng trở về Bước 1. Gọi API tạo chi nhánh từ một instance Branch → HTTP 404 (do `@ConditionalOnProperty` chỉ bật Controller này ở HQ). |
| **Tham chiếu kỹ thuật** | Module `branch` — Entity `Branch`. |

*Bảng 3-4: Đặc tả Use Case UC03*

### UC04 — Quản lý sản phẩm và danh mục

| Thuộc tính | Nội dung |
|---|---|
| **Mã Use Case** | UC04 |
| **Tên Use Case** | Quản lý sản phẩm và danh mục |
| **Mô tả** | Quản lý danh mục sản phẩm phân cấp cha/con và thông tin sản phẩm (SKU, tên, đơn vị tính, mô tả, hình ảnh). |
| **Actor(s)** | Admin |
| **Trigger** | Admin chọn chức năng "Thêm/Sửa danh mục hoặc sản phẩm" trên màn hình quản trị danh mục. |
| **Precondition(s)** | Chỉ thực hiện tại HQ; endpoint ghi `POST/PUT /api/v1/catalog/products` giới hạn `HQ + ADMIN`. |
| **Post-Condition** | Sản phẩm sẵn sàng để chọn khi lập phiếu nhập kho (UC12), hóa đơn bán hàng (UC10), thiết lập giá (UC06). |
| **Basic Flow** | 1. Admin tạo danh mục cha (VD: "Gạch ốp lát"), có thể tạo danh mục con tham chiếu `parent_id`.<br>2. Admin tạo sản phẩm, gắn `category_id`, khai báo `sku` duy nhất, đơn vị tính cơ bản.<br>3. Hệ thống lưu sản phẩm với `is_active=true` mặc định.<br>4. Dữ liệu tự động đồng bộ xuống toàn bộ chi nhánh (UC15, read-only tại Branch). |
| **Alternative Flow** | 5. Admin ẩn sản phẩm ngừng kinh doanh: `is_active=false` (soft-delete), không xóa vật lý để giữ toàn vẹn tham chiếu với các hóa đơn lịch sử; luồng trở về Bước 1. |
| **Exception Flow** | SKU trùng lặp → từ chối, luồng trở về Bước 2. Gọi API ghi sản phẩm từ instance Branch → HTTP 404. |
| **Tham chiếu kỹ thuật** | Module `catalog` — Entity `Category`, `Product`. |

*Bảng 3-5: Đặc tả Use Case UC04*

### UC05 — Quản lý nhà cung cấp

| Thuộc tính | Nội dung |
|---|---|
| **Mã Use Case** | UC05 |
| **Tên Use Case** | Quản lý nhà cung cấp |
| **Mô tả** | Tạo và cập nhật thông tin nhà cung cấp phục vụ lập phiếu nhập kho tại chi nhánh. |
| **Actor(s)** | Staff (theo phạm vi chi nhánh) |
| **Trigger** | Staff chọn chức năng "Thêm/Sửa nhà cung cấp" khi chuẩn bị lập phiếu nhập kho. |
| **Precondition(s)** | Nhân viên đã đăng nhập với `branchId` hợp lệ. |
| **Post-Condition** | Nhà cung cấp sẵn sàng để chọn khi lập phiếu nhập kho (UC12). |
| **Basic Flow** | 1. Staff nhập thông tin nhà cung cấp (tên, số điện thoại, địa chỉ, mã số thuế).<br>2. Hệ thống lưu bản ghi nhà cung cấp gắn với `branch_id` của Staff hiện tại (`AuthUtils.getBranchIdOrNull()`).<br>3. Nhà cung cấp chỉ hiển thị và có thể chọn khi lập phiếu nhập kho (UC12) tại đúng chi nhánh đó. |
| **Alternative Flow** | Không có luồng thay thế đáng kể ngoài luồng chính. |
| **Exception Flow** | Mã nhà cung cấp trùng trong cùng chi nhánh → từ chối, luồng trở về Bước 1. |
| **Quy tắc nghiệp vụ liên quan** | Chi nhánh A không nhìn thấy nhà cung cấp do chi nhánh B tạo (cô lập theo `branch_id`). |
| **Tham chiếu kỹ thuật** | Module `catalog` — Entity `Supplier`. |

*Bảng 3-6: Đặc tả Use Case UC05*

### UC06 — Quản lý bảng giá

| Thuộc tính | Nội dung |
|---|---|
| **Mã Use Case** | UC06 |
| **Tên Use Case** | Quản lý bảng giá |
| **Mô tả** | Thiết lập giá niêm yết cho sản phẩm theo từng chi nhánh và thời điểm hiệu lực. |
| **Actor(s)** | Admin |
| **Trigger** | Admin chọn chức năng "Thiết lập giá niêm yết" cho một sản phẩm tại một chi nhánh. |
| **Precondition(s)** | Sản phẩm và chi nhánh đã tồn tại (UC04, UC03). |
| **Post-Condition** | Giá niêm yết mới có hiệu lực được dùng làm giá gợi ý tầng 2 khi lập hóa đơn bán hàng (UC10) cho khách hàng chưa từng mua sản phẩm đó (xem UC09). |
| **Basic Flow** | 1. Admin chọn sản phẩm, chi nhánh áp dụng, nhập giá bán và ngày hiệu lực (`effective_date`).<br>2. Hệ thống lưu bản ghi mới trong `price_list` — **không ghi đè** bản ghi giá cũ (append-only theo thời gian).<br>3. Khi tra giá, hệ thống lấy bản ghi có `effective_date` gần nhất mà vẫn ≤ ngày hiện tại. |
| **Alternative Flow** | 4. Cùng một sản phẩm có thể có giá niêm yết khác nhau ở từng chi nhánh tại cùng thời điểm; luồng trở về Bước 1. |
| **Exception Flow** | `price < 0` → từ chối theo ràng buộc `CHECK (price >= 0)`, luồng trở về Bước 1. |
| **Quy tắc nghiệp vụ liên quan** | Lịch sử giá được giữ nguyên để phục vụ đối soát và audit — không `UPDATE` bản ghi giá cũ. |
| **Tham chiếu kỹ thuật** | Module `catalog` — Entity `PriceList`. |

*Bảng 3-7: Đặc tả Use Case UC06*

### UC07 — Xem báo cáo tổng hợp

| Thuộc tính | Nội dung |
|---|---|
| **Mã Use Case** | UC07 |
| **Tên Use Case** | Xem báo cáo tổng hợp |
| **Mô tả** | Xem Dashboard tổng hợp doanh thu, lợi nhuận gộp, vòng quay tồn kho và danh sách cảnh báo tồn kho thấp trên toàn hệ thống hoặc theo chi nhánh. |
| **Actor(s)** | Admin |
| **Trigger** | Admin mở màn hình Dashboard hoặc yêu cầu xem báo cáo tổng hợp. |
| **Precondition(s)** | Admin đăng nhập; dữ liệu Transaction từ các chi nhánh đã đồng bộ về HQ (UC15). |
| **Post-Condition** | Admin có cái nhìn tổng hợp để ra quyết định vận hành (nhập thêm hàng, điều chỉnh giá, theo dõi chi nhánh có dấu hiệu bất thường). |
| **Basic Flow** | 1. Admin mở màn hình Dashboard.<br>2. Client gọi `GET /api/v1/analytics/dashboard`.<br>3. `AnalyticsDataAdapter` tổng hợp dữ liệu (tổng doanh thu, lợi nhuận gộp, `inventoryTurnoverRatio`, `totalReceivableDebt`, sản phẩm bán chạy/chậm) từ dữ liệu đã replicate.<br>4. Hệ thống trả `DashboardMetricsDto`. |
| **Alternative Flow** | 5. Admin xem thêm danh sách cảnh báo tồn kho: `GET /api/v1/analytics/stock-alerts`; luồng trở về Bước 1. |
| **Exception Flow** | Tham số ngày bắt đầu lớn hơn ngày kết thúc → HTTP 400, luồng trở về Bước 1. Người dùng không có quyền ADMIN → HTTP 403. |
| **Tham chiếu kỹ thuật** | Module `analytics` — `DashboardController`, `AnalyticsDataAdapter`, `AnalyticsDataPort`. |

*Bảng 3-8: Đặc tả Use Case UC07*

### UC08 — Quản lý khách hàng

| Thuộc tính | Nội dung |
|---|---|
| **Mã Use Case** | UC08 |
| **Tên Use Case** | Quản lý khách hàng |
| **Mô tả** | Tạo, sửa hồ sơ khách hàng dùng chung toàn hệ thống. |
| **Actor(s)** | Admin |
| **Trigger** | Admin chọn chức năng "Thêm/Sửa khách hàng" trên màn hình quản trị khách hàng. |
| **Precondition(s)** | Chỉ thực hiện tại HQ (`GET/POST/PUT /api/v1/customers` — HQ-only, ADMIN cho thao tác ghi). |
| **Post-Condition** | Khách hàng sẵn sàng để chọn khi lập hóa đơn bán hàng (UC10) tại bất kỳ chi nhánh nào. |
| **Basic Flow** | 1. Admin nhập thông tin khách hàng (mã, họ tên, số điện thoại, email, địa chỉ).<br>2. `CustomerWriteController` lưu bản ghi trực tiếp qua `CustomerRepository`.<br>3. Dữ liệu tự động đồng bộ xuống toàn bộ chi nhánh (UC15, read-only tại Branch). |
| **Alternative Flow** | Không có luồng thay thế đáng kể ngoài luồng chính. |
| **Exception Flow** | Gọi API ghi khách hàng từ instance Branch → HTTP 404, luồng kết thúc. |
| **Quy tắc nghiệp vụ liên quan** | Mọi khách hàng bình đẳng về điều kiện giao dịch — không phân loại — đều có thể mua chịu tùy thỏa thuận với nhân viên bán hàng. |
| **Tham chiếu kỹ thuật** | Module `crm` — Entity `Customer`, `CustomerWriteController`. |

*Bảng 3-9: Đặc tả Use Case UC08*

### UC09 — Tra cứu giá riêng theo khách hàng

| Thuộc tính | Nội dung |
|---|---|
| **Mã Use Case** | UC09 |
| **Tên Use Case** | Tra cứu giá riêng theo khách hàng |
| **Mô tả** | Khi thêm sản phẩm vào hóa đơn cho một khách hàng, hệ thống gợi ý mức giá phù hợp nhất dựa trên lịch sử mua hàng của khách. |
| **Actor(s)** | Staff |
| **Trigger** | Staff chọn một sản phẩm để thêm vào hóa đơn đang lập cho một khách hàng cụ thể. |
| **Precondition(s)** | Khách hàng và sản phẩm đã tồn tại; Staff đã chọn khách hàng trên màn hình lập hóa đơn. |
| **Post-Condition** | Dòng hóa đơn có `unit_price`, `default_unit_price`, `is_price_overridden` sẵn sàng để xác nhận (UC10). |
| **Basic Flow** | 1. Staff chọn sản phẩm P để thêm vào hóa đơn của khách hàng C tại chi nhánh B (lấy từ token đăng nhập).<br>2. Hệ thống tra `customer_product_price(C, P, B)`.<br>3. **Nếu có** bản ghi: gợi ý `last_price` — giá gần nhất khách này đã từng mua sản phẩm đó.<br>4. **Nếu không có:** hệ thống tra `price_list(P, B, TODAY)`, lấy bản ghi có `effective_date` gần nhất ≤ hôm nay, gợi ý làm giá niêm yết. |
| **Alternative Flow** | 5. Staff ghi đè giá gợi ý (`is_price_overridden = true`), nhập `unit_price` khác; hệ thống vẫn lưu lại `default_unit_price` (giá gợi ý gốc) để đối chiếu; luồng trở về Bước 4.<br>6. Khi hóa đơn CONFIRMED (UC10), `customer_product_price.last_price` được cập nhật lại bằng `unit_price` vừa bán — lần bán sau sẽ gợi ý đúng mức giá vừa thỏa thuận. |
| **Exception Flow** | Sản phẩm hoàn toàn mới, chưa có giá niêm yết cho chi nhánh đó → hệ thống không gợi ý, buộc Staff nhập tay; luồng kết thúc tại Bước 4. |
| **Tham chiếu kỹ thuật** | Module `order`/`crm` — Entity `CustomerProductPrice`, `PriceList`. |

*Bảng 3-10: Đặc tả Use Case UC09*

> **[DIAGRAM — Hình 3-6: Sequence — Tra cứu giá riêng theo khách hàng]**
>
> *Hình 3-6: Sequence Diagram — UC09 Tra cứu giá riêng theo khách hàng (đúng 6 bước Basic + Alternative Flow ở trên)*

### UC10 — Lập hóa đơn bán hàng

| Thuộc tính | Nội dung |
|---|---|
| **Mã Use Case** | UC10 |
| **Tên Use Case** | Lập hóa đơn bán hàng |
| **Mô tả** | Tạo và xác nhận hóa đơn bán hàng trong một giao dịch nguyên tố (`createAndConfirm`), tác động đồng thời lên tồn kho, giá vốn và công nợ. |
| **Actor(s)** | Staff |
| **Trigger** | Staff chọn chức năng "Lập hóa đơn bán hàng" và sau đó gửi yêu cầu xác nhận hóa đơn. |
| **Precondition(s)** | Staff đã đăng nhập tại một chi nhánh; khách hàng đã tồn tại (UC08); sản phẩm đã tồn tại (UC04). |
| **Post-Condition** | Hóa đơn ở trạng thái `CONFIRMED` (bất biến — không sửa được); tồn kho giảm đúng theo FIFO; công nợ khách hàng tăng đúng phần chưa thanh toán. |
| **Basic Flow** | 1. Staff chọn khách hàng, thêm các dòng sản phẩm (số lượng, đơn giá gợi ý theo UC09).<br>2. Staff nhập số tiền khách thanh toán ngay (`amount_paid`).<br>3. Staff gửi yêu cầu xác nhận kèm header `Idempotency-Key`.<br>4. Hệ thống kiểm tra khách hàng tồn tại (`crmFacade.customerExists`).<br>5. Hệ thống tạo bản ghi hóa đơn nháp (header + các dòng chi tiết), lưu để sinh ID.<br>6. Với từng dòng bán hàng, hệ thống khóa các Cost Layer còn `remaining_qty > 0` theo thứ tự `created_at ASC` (`SELECT ... FOR UPDATE`).<br>7. Hệ thống tiêu thụ FIFO, ghi `stock_movement` (loại `SALE`), giảm `stock_on_hand.quantity`.<br>8. Hệ thống ghi `unit_cost_snapshot` (giá vốn bình quân gia quyền của các lớp giá vừa tiêu thụ) vào từng dòng hóa đơn.<br>9. Hệ thống snapshot công nợ: `previous_debt`, `total_amount`, `remaining_debt` ghi cứng vào hóa đơn.<br>10. Hệ thống cập nhật công nợ: `receivable_debt.current_balance += remaining_debt` (nếu `remaining_debt > 0`).<br>11. Hệ thống cập nhật `customer_product_price.last_price = unit_price` cho từng dòng.<br>12. Hệ thống gọi `invoice.confirm(userId)`, chuyển trạng thái `DRAFT → CONFIRMED`.<br>13. Hệ thống lưu và commit transaction, trả kết quả cho Staff. |
| **Alternative Flow** | **A1 — Thanh toán đủ ngay:**<br>14. Nếu `amount_paid = total_amount`, `remaining_debt = 0`, hệ thống không ghi tăng công nợ; luồng trở về Bước 9.<br>**A2 — Mua chịu toàn bộ:**<br>15. Nếu `amount_paid = 0`, hệ thống ghi nhận toàn bộ `total_amount` là `remaining_debt`; luồng trở về Bước 9. |
| **Exception Flow** | Xung đột cập nhật đồng thời trên `stock_on_hand`/`cost_layer` (`OptimisticLockingFailureException`) → tự động `@Retryable` tối đa 3 lần, luồng trở về Bước 6. Hết Cost Layer còn hàng (âm kho): vẫn ghi `stock_movement` (SALE), `unit_cost_snapshot = 0`, đánh dấu `cost_basis = 'NO_LAYER'` — hệ thống **không chặn bán hàng khi âm kho** (quyết định nghiệp vụ đã chốt), luồng tiếp tục tại Bước 8. `Idempotency-Key` trùng với hash khác → HTTP 409, luồng kết thúc. |
| **Quy tắc nghiệp vụ liên quan** | Một khi `CONFIRMED`, `unit_price` không bao giờ thay đổi — đây là con số pháp lý của hóa đơn, dù giá niêm yết sau đó có thay đổi. |
| **Tham chiếu kỹ thuật** | `SalesInvoiceService.createAndConfirm()`, `InventoryFacade.recordSaleAndGetCost()`, `FifoCostService.consume()`, `debtService.increaseDebt()`. |

*Bảng 3-11: Đặc tả Use Case UC10*

> **[DIAGRAM — Hình 3-7: Activity — Lập hóa đơn bán hàng]**
>
> *Hình 3-7: Activity Diagram — UC10 Lập hóa đơn bán hàng (đúng 15 bước Basic + Alternative Flow ở trên)*

> **[DIAGRAM — Hình 3-8: Sequence — Lập hóa đơn bán hàng]**
>
> *Hình 3-8: Sequence Diagram — UC10 Lập hóa đơn bán hàng*

### UC11 — Lập phiếu trả hàng

| Thuộc tính | Nội dung |
|---|---|
| **Mã Use Case** | UC11 |
| **Tên Use Case** | Lập phiếu trả hàng |
| **Mô tả** | Ghi nhận hàng hóa khách hàng trả lại, hoàn kho và giảm trừ công nợ. |
| **Actor(s)** | Staff |
| **Trigger** | Staff chọn chức năng "Lập phiếu trả hàng" khi khách hàng mang hàng đến trả, và gửi yêu cầu xác nhận. |
| **Precondition(s)** | Khách hàng tồn tại; hóa đơn gốc (nếu có liên kết) đã ở trạng thái `CONFIRMED`. |
| **Post-Condition** | Phiếu trả ở trạng thái `CONFIRMED`; tồn kho tăng; công nợ khách hàng giảm tương ứng `total_return_amount`. |
| **Basic Flow** | 1. Staff lập phiếu trả (`DRAFT`) — chọn khách hàng, sản phẩm và số lượng trả, có thể liên kết hóa đơn gốc (tùy chọn).<br>2. Staff gửi yêu cầu xác nhận phiếu.<br>3. Hệ thống kiểm tra có liên kết hóa đơn gốc hay không.<br>4. Nếu có liên kết, hệ thống khóa hóa đơn gốc (`invoiceRepository.findByIdForUpdate`) để đảm bảo tính toàn vẹn — tránh trả hàng vượt quá số lượng đã mua khi có nhiều yêu cầu trả đồng thời.<br>5. `GoodsReturn.confirm()` chuyển trạng thái sang `CONFIRMED`.<br>6. Hệ thống hoàn hàng vào kho (`InventoryFacade.recordReturn`): tạo Cost Layer **mới** với `unit_cost = return_price`.<br>7. Hệ thống ghi `stock_movement` loại `RETURN`, tăng `stock_on_hand.quantity`.<br>8. Hệ thống giảm công nợ: `receivable_debt.current_balance -= total_return_amount`.<br>9. Hệ thống lưu và ghi log kết quả. |
| **Alternative Flow** | 10. Nếu phiếu trả **không** liên kết hóa đơn gốc (khách trả hàng mua từ lâu, không còn hóa đơn tra cứu), hệ thống bỏ qua Bước 4 và tiếp tục trực tiếp từ Bước 5. |
| **Exception Flow** | Số lượng trả vượt số lượng đã mua trên hóa đơn gốc (khi có liên kết) → hệ thống từ chối xác nhận, phiếu trả giữ nguyên `DRAFT`, luồng kết thúc. |
| **Quy tắc nghiệp vụ liên quan** | Hàng trả về được nhập lại kho như một **lớp giá (Cost Layer) mới**, không gộp vào lớp giá cũ — giữ đúng nguyên tắc truy vết theo lô. |
| **Tham chiếu kỹ thuật** | Entity `GoodsReturn`, `GoodsReturnLine`; `InventoryFacade.recordReturn()`, `debtService.decreaseDebt()`. |

*Bảng 3-12: Đặc tả Use Case UC11*

> **[DIAGRAM — Hình 3-9: Activity — Lập phiếu trả hàng]**
>
> *Hình 3-9: Activity Diagram — UC11 Lập phiếu trả hàng (đúng 10 bước Basic + Alternative Flow ở trên)*

> **[DIAGRAM — Hình 3-10: Sequence — Lập phiếu trả hàng]**
>
> *Hình 3-10: Sequence Diagram — UC11 Lập phiếu trả hàng*

### UC12 — Lập phiếu nhập kho

| Thuộc tính | Nội dung |
|---|---|
| **Mã Use Case** | UC12 |
| **Tên Use Case** | Lập phiếu nhập kho |
| **Mô tả** | Ghi nhận hàng hóa nhập từ nhà cung cấp, tăng tồn kho và sinh Cost Layer mới phục vụ tính giá vốn FIFO cho các giao dịch bán hàng sau này. |
| **Actor(s)** | Staff |
| **Trigger** | Staff chọn chức năng "Lập phiếu nhập kho" khi hàng hóa từ nhà cung cấp được giao đến, và gửi yêu cầu xác nhận. |
| **Precondition(s)** | Nhà cung cấp đã tồn tại tại chi nhánh (UC05); sản phẩm đã tồn tại (UC04). |
| **Post-Condition** | Phiếu nhập ở trạng thái `CONFIRMED`; tồn kho tăng đúng số lượng nhập; có Cost Layer mới sẵn sàng để FIFO tiêu thụ khi bán hàng (UC10). |
| **Basic Flow** | 1. Staff lập phiếu nhập (`DRAFT`) — chọn nhà cung cấp, thêm các dòng sản phẩm với số lượng và đơn giá nhập.<br>2. Staff gửi yêu cầu xác nhận phiếu.<br>3. `InboundReceipt.confirm()` chuyển trạng thái sang `CONFIRMED`.<br>4. Với từng dòng, hệ thống tăng `stock_on_hand.quantity` (`stockRepo.findByProductIdAndBranchId`, `stock.increase`).<br>5. Hệ thống ghi `stock_movement` loại `INBOUND` cho từng dòng.<br>6. Hệ thống tạo Cost Layer mới (`CostLayer.fromInbound()`) với `unit_cost` = đơn giá nhập, `initial_qty = remaining_qty` = số lượng nhập, `created_at` dùng làm thứ tự FIFO.<br>7. Hệ thống lưu kết quả phiếu nhập. |
| **Alternative Flow** | Không có luồng thay thế đáng kể ngoài luồng chính. |
| **Exception Flow** | Đơn giá nhập âm hoặc số lượng ≤ 0 → hệ thống từ chối theo ràng buộc dữ liệu (validation), luồng kết thúc, phiếu nhập giữ nguyên `DRAFT`. |
| **Quy tắc nghiệp vụ liên quan** | Mỗi lần nhập tạo một Cost Layer riêng biệt — cùng sản phẩm nhập nhiều lần với giá khác nhau sẽ có nhiều Cost Layer song song, tiêu thụ theo thứ tự cũ nhất trước (FIFO). |
| **Tham chiếu kỹ thuật** | Entity `InboundReceipt`, `InboundReceiptLine`, `CostLayer.fromInbound()`, `StockMovement.inbound()`. |

*Bảng 3-13: Đặc tả Use Case UC12*

> **[DIAGRAM — Hình 3-11: Activity — Lập phiếu nhập kho]**
>
> *Hình 3-11: Activity Diagram — UC12 Lập phiếu nhập kho (đúng 7 bước Basic Flow ở trên, không có Alternative Flow)*

> **[DIAGRAM — Hình 3-12: Sequence — Lập phiếu nhập kho]**
>
> *Hình 3-12: Sequence Diagram — UC12 Lập phiếu nhập kho*

### UC13 — Tra cứu tồn kho

| Thuộc tính | Nội dung |
|---|---|
| **Mã Use Case** | UC13 |
| **Tên Use Case** | Tra cứu tồn kho |
| **Mô tả** | Xem số lượng tồn kho hiện tại theo sản phẩm và lịch sử biến động kho (nhập/bán/trả). |
| **Actor(s)** | Staff (chi nhánh mình), Admin (toàn hệ thống qua dữ liệu đã replicate) |
| **Trigger** | Staff/Admin chọn chức năng "Tra cứu tồn kho" trên màn hình quản lý kho. |
| **Precondition(s)** | Người dùng đã đăng nhập với phạm vi chi nhánh phù hợp. |
| **Post-Condition** | Người dùng có thông tin để quyết định có nên lập phiếu nhập kho (UC12) bổ sung hay không. |
| **Basic Flow** | 1. Người dùng chọn sản phẩm (và/hoặc chi nhánh nếu là Admin).<br>2. `GET /api/v1/inventory/stock` trả về `StockOnHand` theo chi nhánh — số liệu đọc trực tiếp từ cache tổng hợp (running total). |
| **Alternative Flow** | 3. Nếu cần truy vết chi tiết: `GET /api/v1/stock-movements` trả về lịch sử từng dòng biến động kho (loại biến động, số lượng, chứng từ gốc, thời điểm); luồng trở về Bước 1. |
| **Exception Flow** | There are no Exception Flows defined for the "Tra cứu tồn kho" Use Case. |
| **Quy tắc nghiệp vụ liên quan** | `SUM(stock_movement.quantity)` của một sản phẩm tại một chi nhánh luôn bằng `stock_on_hand.quantity` — có thể dùng để kiểm tra tính toàn vẹn dữ liệu định kỳ. |
| **Tham chiếu kỹ thuật** | Entity `StockOnHand`, `StockMovement`; `StockController`. |

*Bảng 3-14: Đặc tả Use Case UC13*

### UC14 — Theo dõi công nợ

| Thuộc tính | Nội dung |
|---|---|
| **Mã Use Case** | UC14 |
| **Tên Use Case** | Theo dõi công nợ |
| **Mô tả** | Tra cứu số dư công nợ hiện tại và lịch sử biến động công nợ của một khách hàng. |
| **Actor(s)** | Staff (chi nhánh mình), Admin (toàn hệ thống) |
| **Trigger** | Staff/Admin chọn chức năng "Tra cứu công nợ" hoặc "Ghi nhận thanh toán" trên màn hình công nợ. |
| **Precondition(s)** | Khách hàng đã tồn tại; đã có ít nhất một giao dịch phát sinh công nợ hoặc chưa (số dư mặc định 0). |
| **Post-Condition** | Người dùng nắm được số dư công nợ hiện tại, hoặc số dư được cập nhật sau khi ghi nhận thanh toán. |
| **Basic Flow** | 1. Người dùng chọn khách hàng cần tra cứu.<br>2. `GET /api/v1/receivable-debts` trả về tổng công nợ hiện tại (`current_balance`) tại chi nhánh.<br>3. Hệ thống hiển thị thêm lịch sử biến động công nợ (`ReceivableDebtMovement`) theo loại: `SALE` (+), `PAYMENT` (–), `RETURN` (–). |
| **Alternative Flow** | 4. Staff ghi nhận thanh toán công nợ: `POST /api/v1/receivable-debts/payments` (yêu cầu `Idempotency-Key`) → giảm `current_balance`, ghi movement loại `PAYMENT`; luồng trở về Bước 2. |
| **Exception Flow** | There are no Exception Flows defined for the "Theo dõi công nợ" Use Case. |
| **Quy tắc nghiệp vụ liên quan** | Công thức chuẩn duy nhất: `SUM(ReceivableDebtMovement.amount) = ReceivableDebt.current_balance` (nguyên tắc Signed Ledger — ADR-11). Mỗi chi nhánh giữ sổ nợ riêng cho cùng một khách hàng, không chia sẻ số dư giữa các chi nhánh. |
| **Tham chiếu kỹ thuật** | Entity `ReceivableDebt`, `ReceivableDebtMovement`; `debtService`. |

*Bảng 3-15: Đặc tả Use Case UC14*

### UC15 — Đồng bộ dữ liệu tự động

| Thuộc tính | Nội dung |
|---|---|
| **Mã Use Case** | UC15 |
| **Tên Use Case** | Đồng bộ dữ liệu tự động (Master Data & Transaction Data) |
| **Mô tả** | Tiến trình nền tự động đồng bộ hai chiều giữa HQ và Branch qua PostgreSQL Logical Replication: Master Data (HQ → Branch) và Transaction Data (Branch → HQ), tuân theo nguyên tắc *single-writer-per-table*. |
| **Actor(s)** | Hệ thống (System — không có tương tác người dùng trực tiếp) |
| **Trigger** | Publication/Subscription phát hiện có thay đổi dữ liệu (INSERT/UPDATE/DELETE) trên một bảng thuộc `pub_hq_to_<branch>` hoặc `pub_<branch>_to_hq`. |
| **Precondition(s)** | Publication/Subscription đã được thiết lập giữa HQ và Branch (script `setup-replication.sh`, xem Mục 4.8). |
| **Post-Condition** | Dữ liệu tại CSDL đích phản ánh đúng thay đổi tại CSDL nguồn trong thời gian chấp nhận được (NFR-03: RPO ≈ 5 giây ở điều kiện mạng bình thường). |
| **Basic Flow** | 1. Một transaction ghi (INSERT/UPDATE/DELETE) vào bảng thuộc Master Data commit tại CSDL HQ.<br>2. PostgreSQL ghi thay đổi vào Write-Ahead Log (WAL) của HQ.<br>3. Publication `pub_hq_to_<branch>` đọc thay đổi từ WAL.<br>4. Subscription tại từng Branch nhận và áp dụng thay đổi vào CSDL Branch tương ứng. |
| **Alternative Flow** | 5. Với bảng thuộc Transaction Data (kể cả 2 bảng snapshot `stock_on_hand`/`receivable_debt`, có `REPLICA IDENTITY USING INDEX` và row filter `branch_id = N`), chiều đồng bộ đảo ngược: transaction commit tại CSDL Branch, publication `pub_<branch>_to_hq` đẩy thay đổi lên CSDL HQ; luồng tương tự Bước 2–4 nhưng theo chiều Branch → HQ. |
| **Exception Flow** | Mất kết nối mạng giữa Branch và HQ trong quá trình đồng bộ → Subscription tự động retry theo cơ chế nội tại của PostgreSQL Logical Replication khi kết nối được khôi phục; dữ liệu cục bộ tại Branch không bị ảnh hưởng (đáp ứng NFR-01 Availability). `idempotency_record` không tham gia đồng bộ (local-only), tránh nhiễu dữ liệu giữa các instance. |
| **Quy tắc nghiệp vụ liên quan** | Mỗi bảng dữ liệu chỉ có đúng một chiều ghi hợp lệ (*single-writer-per-table*) — không có bảng nào được ghi từ cả hai phía đồng thời. |
| **Tham chiếu kỹ thuật** | Script `setup-replication.sh`, `check-schema-version.sh`, `add-branch.sh`, `enable-snapshot-replication.sh`; ADR-13. |

*Bảng 3-16: Đặc tả Use Case UC15*

> **[DIAGRAM — Hình 3-13: Sequence — Đồng bộ dữ liệu (Logical Replication)]**
>
> *Hình 3-13: Sequence Diagram — UC15 Đồng bộ dữ liệu tự động*

---

## 3.4. Biểu đồ Luồng Dữ liệu (DFD) — mô hình bổ sung

> Mục này không thuộc cấu trúc lõi của phương pháp use-case-driven ở Mục 3.2–3.3, nhưng được giữ lại theo yêu cầu của tài liệu nội dung gốc (`01-noi-dung-bao-cao-v3.md`) như một góc nhìn bổ sung theo phương pháp Structured Analysis truyền thống, giúp đối chiếu luồng dữ liệu giữa các Data Store — hữu ích khi trình bày các nghiệp vụ tác động đồng thời lên nhiều bảng (Kho hàng, Công nợ, Chứng từ).

### 3.4.1. DFD Cấp 0 (Context Diagram)

Biểu đồ DFD Cấp 0 mô tả cái nhìn tổng quát nhất về toàn bộ hệ thống ERP bán lẻ đa chi nhánh và tương tác với hai tác nhân bên ngoài: Admin và Staff.

> **[DIAGRAM — Hình 3-14: DFD Cấp 0 / Context]**
>
> *Hình 3-14: DFD Cấp 0 (Context Diagram)*

### 3.4.2. DFD mức chi tiết cho các nghiệp vụ chính

**a) Lập hóa đơn bán hàng (UC10)**

> **[DIAGRAM — Hình 3-15: DFD — Lập hóa đơn bán hàng]**
>
> *Hình 3-15: DFD — Lập hóa đơn bán hàng*

| **Dòng** | **Ý nghĩa** |
|---|---|
| D1 | Khách hàng, danh sách sản phẩm, số lượng, đơn giá, phương thức thanh toán, số tiền trả trước |
| D2 | Hóa đơn đã xác nhận: mã hóa đơn, tổng tiền, công nợ trước và sau |
| D3 | Thông tin khách hàng, sản phẩm (Danh mục); lớp giá vốn còn tồn (Kho hàng) |
| D4 | Tồn kho giảm, biến động xuất, lớp giá vốn đã tiêu thụ (Kho hàng); hóa đơn và dòng kèm giá vốn, công nợ và biến động công nợ (Chứng từ & công nợ) |

*Bảng 3-17: Từ điển dòng dữ liệu — DFD Lập hóa đơn bán hàng*

**b) Lập phiếu trả hàng (UC11)**

> **[DIAGRAM — Hình 3-16: DFD — Lập phiếu trả hàng]**
>
> *Hình 3-16: DFD — Lập phiếu trả hàng*

| **Dòng** | **Ý nghĩa** |
|---|---|
| D1 | Hóa đơn gốc (tùy chọn), khách hàng, sản phẩm trả, số lượng, lý do |
| D2 | Phiếu trả đã xác nhận, số tiền được giảm công nợ |
| D3 | Phiếu trả nháp và hóa đơn gốc (Chứng từ) |
| D4 | Phiếu trả đã xác nhận (Chứng từ); tồn kho tăng, biến động trả hàng, lớp giá vốn hoàn trả (Kho hàng); công nợ giảm và biến động công nợ (Công nợ) |

*Bảng 3-18: Từ điển dòng dữ liệu — DFD Lập phiếu trả hàng*

**c) Lập phiếu nhập kho (UC12)**

> **[DIAGRAM — Hình 3-17: DFD — Lập phiếu nhập kho]**
>
> *Hình 3-17: DFD — Lập phiếu nhập kho*

| **Dòng** | **Ý nghĩa** |
|---|---|
| D1 | Nhà cung cấp, danh sách sản phẩm, số lượng, đơn giá nhập |
| D2 | Phiếu nhập đã xác nhận |
| D3 | Nhà cung cấp, sản phẩm (Danh mục); phiếu nhập nháp (Chứng từ) |
| D4 | Phiếu nhập đã xác nhận (Chứng từ); tồn kho tăng, biến động nhập, lớp giá vốn mới (Kho hàng) |

*Bảng 3-19: Từ điển dòng dữ liệu — DFD Lập phiếu nhập kho*

**d) Tra cứu giá riêng theo khách hàng (UC09)**

> **[DIAGRAM — Hình 3-18: DFD — Tra cứu giá riêng theo khách hàng]**
>
> *Hình 3-18: DFD — Tra cứu giá riêng theo khách hàng*

| **Dòng** | **Ý nghĩa** |
|---|---|
| D1 | Khách hàng, sản phẩm (chi nhánh lấy từ token đăng nhập) |
| D2 | Đơn giá gợi ý (giá riêng hoặc giá niêm yết), hoặc thông báo chưa có giá |
| D3 | Giá riêng theo khách hàng – sản phẩm – chi nhánh (`customer_product_price`); giá niêm yết (`price_list`) |

*Bảng 3-20: Từ điển dòng dữ liệu — DFD Tra cứu giá riêng theo khách hàng*

**e) Theo dõi công nợ (UC14)**

> **[DIAGRAM — Hình 3-19: DFD — Theo dõi công nợ]**
>
> *Hình 3-19: DFD — Theo dõi công nợ*

| **Dòng** | **Ý nghĩa** |
|---|---|
| D1 | Khách hàng cần tra cứu |
| D2 | Số dư công nợ và lịch sử biến động |
| D3 | Thông tin khách hàng (Danh mục); tổng công nợ, lịch sử biến động công nợ (Công nợ) |

*Bảng 3-21: Từ điển dòng dữ liệu — DFD Theo dõi công nợ*
## 3.5. Mô hình hóa cấu trúc (Structural Modeling)

### 3.5.0. Cơ sở xây dựng và quy trình

Quá trình xây dựng sơ đồ lớp cho hệ thống được thực hiện qua 7 bước chuẩn: (1) xác định lớp/thuộc tính/phương thức từ danh mục đối tượng tham chiếu (Phụ lục A); (2) xác định quan hệ dựa trên danh sách quan hệ và quy định Cascade/OrphanRemoval; (3) tách lớp phụ (`SalesInvoiceLine`, `GoodsReturnLine`, `InboundReceiptLine`); (4) tổng quát hóa bằng Interface khi cần (các Facade — xem Mục 3.6.4.2); (5) tách Enum (`SalesInvoiceStatus`, `MovementType`, `ReceivableDebtMovementType` — xem Mục 3.6.4.2); (6) hiệu chỉnh quan hệ liên module bằng tham chiếu qua ID (`branchId`, `customerId`) thay vì tham chiếu thực thể cứng, đảm bảo kiến trúc Modular Monolith (ADR-10); (7) đối chiếu lại với Phụ lục A.

Hệ thống có **24 lớp thực thể cốt lõi**. Theo đúng quy trình 3 mức chi tiết tăng dần của phương pháp luận (Structural → Analysis → Design — xem Mục 3.6.4), sơ đồ lớp dưới đây trình bày ở **mức Structural (tổng quan)** — chỉ tên lớp và tên thuộc tính, **chưa có kiểu dữ liệu, chưa có ký hiệu visibility (+/-)**. Mức Analysis-level và Design-level được trình bày sau khi hoàn tất Mô hình hóa dữ liệu, tại Mục 3.6.4.

> **[DIAGRAM — Hình 3-20: Class Diagram tổng quát (mức Structural)]**
>
> *Hình 3-20: Class Diagram tổng quát — mức Structural (chỉ tên lớp + thuộc tính, chưa có kiểu dữ liệu/visibility)*

### 3.5.1. Danh sách các lớp đối tượng (List of Object Classes)

| **STT** | **Tên lớp** | **Ý nghĩa** |
|---|---|---|
| 1 | Branch | Thông tin chi nhánh bán lẻ. |
| 2 | Category | Danh mục sản phẩm (phân cấp cha/con). |
| 3 | Product | Sản phẩm hàng hóa. |
| 4 | Supplier | Nhà cung cấp. |
| 5 | PriceList | Bảng giá niêm yết của sản phẩm tại chi nhánh theo thời gian hiệu lực. |
| 6 | Customer | Khách hàng. |
| 7 | ReceivableDebt | Tổng công nợ hiện tại của khách hàng tại chi nhánh (snapshot). |
| 8 | ReceivableDebtMovement | Sổ cái biến động công nợ của khách hàng (SALE/PAYMENT/RETURN). |
| 9 | Permission | Quyền hạn nhỏ nhất trên hệ thống. |
| 10 | Role | Vai trò người dùng (ADMIN/STAFF). |
| 11 | RolePermission | Bảng nối Vai trò — Quyền hạn (N-N). |
| 12 | UserAccount | Tài khoản người dùng. |
| 13 | UserBranchRole | Gán vai trò cho người dùng theo phạm vi chi nhánh. |
| 14 | SalesInvoice | Hóa đơn bán hàng. |
| 15 | SalesInvoiceLine | Dòng chi tiết hóa đơn bán hàng. |
| 16 | GoodsReturn | Phiếu trả hàng từ khách hàng. |
| 17 | GoodsReturnLine | Dòng chi tiết phiếu trả hàng. |
| 18 | CustomerProductPrice | Giá bán riêng (giá gần nhất) theo từng cặp khách hàng — sản phẩm. |
| 19 | InboundReceipt | Phiếu nhập kho từ nhà cung cấp. |
| 20 | InboundReceiptLine | Dòng chi tiết phiếu nhập kho. |
| 21 | StockOnHand | Tồn kho hiện tại (cache running total). |
| 22 | StockMovement | Sổ cái biến động kho (append-only). |
| 23 | CostLayer | Lớp giá vốn theo phương pháp FIFO. |
| 24 | IdempotencyRecord | Bản ghi hỗ trợ chống trùng lặp request. |

*Bảng 3-22: Danh sách các lớp đối tượng (List of Object Classes)*

> **Ghi chú đánh số:** Số thứ tự (1–24) trong bảng trên là số hiệu dùng thống nhất làm alias hiển thị (`class X["N. X"]`) trên **mọi** sơ đồ lớp có đánh số ở các mức Structural/Analysis/Design (Mục 3.5.0, 3.6.4.1, 3.6.4.2) — được xác nhận trong bảng kiểm tra tính nhất quán tại Mục 3.7.4.

### 3.5.2. Mô tả chi tiết từng lớp đối tượng

Với mỗi lớp, bảng thuộc tính (Attribute) và bảng phương thức (Operation) được trình bày dưới đây. Các lớp dữ liệu thuần túy (không có hành vi nghiệp vụ đáng chú ý ngoài truy xuất CRUD chuẩn) được ghi chú rõ ở bảng Operation.

**1. Branch**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| id | id: Long | Khóa chính |
| code | code: Varchar | Mã chi nhánh, duy nhất |
| name | name: Varchar | Tên chi nhánh |
| address | address: Varchar | Địa chỉ |
| phone | phone: Varchar | Số điện thoại |
| isActive | isActive: Boolean | Trạng thái hoạt động (soft-delete) |

Operation: không có operation nghiệp vụ ngoài constructor/getter/setter chuẩn.

**2. Category**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| id | id: Long | Khóa chính |
| parentId | parentId: Long | Tham chiếu danh mục cha (self-reference, nullable) |
| code | code: Varchar | Mã danh mục, duy nhất |
| name | name: Varchar | Tên danh mục |
| sortOrder | sortOrder: Int | Thứ tự hiển thị |
| isActive | isActive: Boolean | Trạng thái hoạt động |

Operation: không có operation nghiệp vụ ngoài constructor/getter/setter chuẩn.

**3. Product**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| id | id: Long | Khóa chính |
| categoryId | categoryId: Long | Danh mục sở hữu |
| sku | sku: Varchar | Mã sản phẩm, duy nhất |
| name | name: Varchar | Tên sản phẩm |
| baseUnitId | baseUnitId: Long | Đơn vị tính cơ bản |
| isActive | isActive: Boolean | Trạng thái hoạt động |

Operation: không có operation nghiệp vụ ngoài constructor/getter/setter chuẩn.

**4. Supplier**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| id | id: Long | Khóa chính |
| branchId | branchId: Long | Chi nhánh sở hữu (Branch-owned) |
| code | code: Varchar | Mã nhà cung cấp |
| name | name: Varchar | Tên nhà cung cấp |
| phone | phone: Varchar | Số điện thoại |
| address | address: Varchar | Địa chỉ |
| taxCode | taxCode: Varchar | Mã số thuế |

Operation: không có operation nghiệp vụ ngoài constructor/getter/setter chuẩn.

**5. PriceList**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| id | id: Long | Khóa chính |
| productId | productId: Long | Sản phẩm áp dụng |
| branchId | branchId: Long | Chi nhánh áp dụng |
| price | price: Decimal | Giá niêm yết |
| effectiveDate | effectiveDate: Date | Ngày hiệu lực (append-only) |

Operation: không có operation nghiệp vụ ngoài constructor/getter/setter chuẩn.

**6. Customer**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| id | id: Long | Khóa chính |
| code | code: Varchar | Mã khách hàng, duy nhất |
| fullName | fullName: Varchar | Họ tên |
| phone | phone: Varchar | Số điện thoại |
| email | email: Varchar | Email |
| address | address: Varchar | Địa chỉ |

Operation: không có operation nghiệp vụ ngoài constructor/getter/setter chuẩn.

**7. ReceivableDebt**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| customerId | customerId: Long | Khách hàng (khóa ghép) |
| branchId | branchId: Long | Chi nhánh (khóa ghép) |
| currentBalance | currentBalance: Decimal | Số dư công nợ hiện tại (snapshot) |

| Operation | Name(params): ReturnType | Meaning |
|---|---|---|
| increase | increase(amount: Decimal): Void | Tăng số dư công nợ (gọi từ `debtService.increaseDebt`) |
| decrease | decrease(amount: Decimal): Void | Giảm số dư công nợ (gọi từ `debtService.decreaseDebt`) |

**8. ReceivableDebtMovement**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| id | id: UUID | Khóa chính |
| customerId | customerId: Long | Khách hàng |
| branchId | branchId: Long | Chi nhánh |
| movementType | movementType: ReceivableDebtMovementType | Loại biến động (enum — xem Mục 3.6.4.2) |
| amount | amount: Decimal | Số tiền biến động, có dấu (+/-), ADR-11 |

Operation: không có operation nghiệp vụ — bản ghi sổ cái append-only (immutable sau khi tạo).

**9. Permission**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| id | id: Short | Khóa chính |
| code | code: Varchar | Mã quyền hạn, duy nhất |
| description | description: Varchar | Mô tả quyền hạn |

Operation: không có operation nghiệp vụ ngoài constructor/getter/setter chuẩn.

**10. Role**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| id | id: Short | Khóa chính |
| code | code: Varchar | `ADMIN` hoặc `STAFF` (duy nhất) |
| name | name: Varchar | Tên hiển thị vai trò |

Operation: không có operation nghiệp vụ ngoài constructor/getter/setter chuẩn.

**11. RolePermission**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| roleId | roleId: Short | Khóa ghép — Vai trò |
| permissionId | permissionId: Short | Khóa ghép — Quyền hạn |

Operation: không có operation nghiệp vụ — bảng nối N-N thuần túy.

**12. UserAccount**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| id | id: Long | Khóa chính |
| username | username: Varchar | Tên đăng nhập, duy nhất |
| passwordHash | passwordHash: Varchar | Mật khẩu đã băm (BCrypt) |
| fullName | fullName: Varchar | Họ tên |
| email | email: Varchar | Email |
| phone | phone: Varchar | Số điện thoại |
| isActive | isActive: Boolean | Trạng thái hoạt động (soft-delete) |

Operation: không có operation nghiệp vụ ở tầng Entity — logic xác thực (`Login`, `Refresh`, `Revoke`) đặt tại `AuthService` (xem Mục 3.3, UC01), không gắn trực tiếp vào Entity để giữ đúng ranh giới Modular Monolith.

**13. UserBranchRole**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| id | id: Long | Khóa chính |
| userId | userId: Long | Người dùng |
| roleId | roleId: Short | Vai trò được gán |
| branchId | branchId: Long | Phạm vi chi nhánh (nullable = toàn hệ thống) |

Operation: không có operation nghiệp vụ ngoài constructor/getter/setter chuẩn.

**14. SalesInvoice**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| id | id: UUID | Khóa chính |
| invoiceNo | invoiceNo: Varchar | Mã hóa đơn, duy nhất theo chi nhánh |
| customerId | customerId: Long | Khách hàng (tham chiếu logic) |
| branchId | branchId: Long | Chi nhánh (tham chiếu logic) |
| status | status: SalesInvoiceStatus | Trạng thái (enum — xem Mục 3.6.4.2) |
| previousDebt | previousDebt: Decimal | Công nợ trước giao dịch (snapshot) |
| totalAmount | totalAmount: Decimal | Tổng tiền hóa đơn |
| amountPaid | amountPaid: Decimal | Số tiền khách trả ngay |
| remainingDebt | remainingDebt: Decimal | Công nợ phát sinh (snapshot) |

| Operation | Name(params): ReturnType | Meaning |
|---|---|---|
| confirm | confirm(userId: Long): Void | Chuyển trạng thái `DRAFT → CONFIRMED`, khóa các giá trị snapshot |

**15. SalesInvoiceLine**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| id | id: Long | Khóa chính |
| invoiceId | invoiceId: UUID | Hóa đơn sở hữu |
| productId | productId: Long | Sản phẩm |
| quantity | quantity: Decimal | Số lượng |
| defaultUnitPrice | defaultUnitPrice: Decimal | Giá gợi ý gốc (trước khi ghi đè) |
| unitPrice | unitPrice: Decimal | Đơn giá bán thực tế |
| isPriceOverridden | isPriceOverridden: Boolean | Đánh dấu giá bị ghi đè bởi nhân viên |
| unitCostSnapshot | unitCostSnapshot: Decimal | Giá vốn FIFO snapshot tại thời điểm bán |

Operation: không có operation nghiệp vụ ngoài constructor/getter/setter chuẩn.

**16. GoodsReturn**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| id | id: UUID | Khóa chính |
| returnNo | returnNo: Varchar | Mã phiếu trả |
| customerId | customerId: Long | Khách hàng |
| branchId | branchId: Long | Chi nhánh |
| originalInvoiceId | originalInvoiceId: UUID | Hóa đơn gốc (tùy chọn) |
| totalReturnAmount | totalReturnAmount: Decimal | Tổng tiền trả hàng |
| status | status: GoodsReturnStatus | Trạng thái (enum — xem Mục 3.6.4.2) |

| Operation | Name(params): ReturnType | Meaning |
|---|---|---|
| confirm | confirm(): Void | Chuyển trạng thái `DRAFT → CONFIRMED` |

**17. GoodsReturnLine**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| id | id: Long | Khóa chính |
| returnId | returnId: UUID | Phiếu trả sở hữu |
| productId | productId: Long | Sản phẩm |
| quantity | quantity: Decimal | Số lượng trả |
| returnPrice | returnPrice: Decimal | Giá hoàn trả (có thể khác giá bán gốc) |

Operation: không có operation nghiệp vụ ngoài constructor/getter/setter chuẩn.

**18. CustomerProductPrice**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| customerId | customerId: Long | Khách hàng (khóa ghép) |
| productId | productId: Long | Sản phẩm (khóa ghép) |
| branchId | branchId: Long | Chi nhánh (khóa ghép) |
| lastPrice | lastPrice: Decimal | Giá gần nhất khách đã mua |
| lastInvoiceId | lastInvoiceId: UUID | Hóa đơn gần nhất phát sinh giá này |

Operation: không có operation nghiệp vụ — cập nhật qua upsert từ `SalesInvoiceService` khi hóa đơn CONFIRMED.

**19. InboundReceipt**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| id | id: UUID | Khóa chính |
| receiptNo | receiptNo: Varchar | Mã phiếu nhập |
| branchId | branchId: Long | Chi nhánh |
| supplierId | supplierId: Long | Nhà cung cấp (tham chiếu logic) |
| status | status: InboundReceiptStatus | Trạng thái (enum — xem Mục 3.6.4.2) |
| confirmedAt | confirmedAt: Datetime | Thời điểm xác nhận |

| Operation | Name(params): ReturnType | Meaning |
|---|---|---|
| confirm | confirm(): Void | Chuyển trạng thái `DRAFT → CONFIRMED` |

**20. InboundReceiptLine**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| id | id: Long | Khóa chính |
| receiptId | receiptId: UUID | Phiếu nhập sở hữu |
| productId | productId: Long | Sản phẩm |
| quantity | quantity: Decimal | Số lượng nhập |
| unitCost | unitCost: Decimal | Đơn giá nhập |

Operation: không có operation nghiệp vụ ngoài constructor/getter/setter chuẩn.

**21. StockOnHand**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| productId | productId: Long | Sản phẩm (khóa ghép) |
| branchId | branchId: Long | Chi nhánh (khóa ghép) |
| quantity | quantity: Decimal | Số lượng tồn (cho phép âm) |
| version | version: Long | Optimistic Lock (`@Version`) |

| Operation | Name(params): ReturnType | Meaning |
|---|---|---|
| increase | increase(qty: Decimal): Void | Tăng tồn kho (nhập hàng, trả hàng) |
| decreaseAllowNegative | decreaseAllowNegative(qty: Decimal): Void | Giảm tồn kho, cho phép âm (bán hàng) |

**22. StockMovement**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| id | id: UUID | Khóa chính |
| productId | productId: Long | Sản phẩm |
| branchId | branchId: Long | Chi nhánh |
| movementType | movementType: MovementType | Loại biến động (enum — xem Mục 3.6.4.2) |
| quantity | quantity: Decimal | Số lượng biến động, có dấu |
| refType | refType: Varchar | Loại chứng từ gốc |
| refId | refId: UUID | Mã chứng từ gốc |
| createdAt | createdAt: Datetime | Thời điểm ghi nhận |

| Operation | Name(params): ReturnType | Meaning |
|---|---|---|
| sale | sale(...): StockMovement | «create» — Factory tạo bản ghi biến động loại SALE |
| inbound | inbound(...): StockMovement | «create» — Factory tạo bản ghi biến động loại INBOUND |
| returnOf | returnOf(...): StockMovement | «create» — Factory tạo bản ghi biến động loại RETURN |

**23. CostLayer**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| id | id: UUID | Khóa chính |
| productId | productId: Long | Sản phẩm |
| branchId | branchId: Long | Chi nhánh |
| unitCost | unitCost: Decimal | Giá vốn của lớp |
| initialQty | initialQty: Decimal | Số lượng nhập ban đầu |
| remainingQty | remainingQty: Decimal | Số lượng còn lại (giảm dần theo FIFO) |
| inboundMovementId | inboundMovementId: UUID | Phiếu nhập sinh ra lớp giá (tùy chọn) |
| costBasis | costBasis: Varchar | Cơ sở tính giá (`NO_LAYER` khi hết lớp giá) |
| version | version: Long | Optimistic Lock (`@Version`) |

| Operation | Name(params): ReturnType | Meaning |
|---|---|---|
| consume | consume(neededQty: Decimal): Decimal | Tiêu thụ FIFO, trả về giá vốn đã tiêu thụ (Pessimistic Lock — `Propagation.MANDATORY`) |
| fromInbound | fromInbound(...): CostLayer | «create» — Factory tạo lớp giá mới từ phiếu nhập |

**24. IdempotencyRecord**

| Attribute | Name: Data Type | Meaning |
|---|---|---|
| key | key: UUID | Khóa chính (`Idempotency-Key`) |
| requestHash | requestHash: Varchar | Hash nội dung request |
| responseSnapshot | responseSnapshot: Varchar | Bản sao response đã trả trước đó |
| createdAt | createdAt: Datetime | Thời điểm tạo |

Operation: không có operation nghiệp vụ — được thao tác gián tiếp qua `IdempotencyAspect` (`@IdempotencyProtected`).

*Bảng 3-23: Mô tả chi tiết thuộc tính và phương thức của 24 lớp đối tượng*

### 3.5.3. Danh sách các mối quan hệ (Relationships)

| **STT** | **Tên quan hệ (A – B)** | **Kiểu** | **Ý nghĩa** |
|---|---|---|---|
| 1 | Category – Category | Aggregation | Cây danh mục cha/con; một Category con vẫn có thể tồn tại độc lập nếu tách khỏi cha (không sở hữu chặt). |
| 2 | Product – Category | Association | Product bắt buộc thuộc về 1 Category; Product không thể tồn tại thiếu Category hợp lệ. |
| 3 | PriceList – Product | Association | PriceList tham chiếu đúng 1 Product; nhiều bản ghi PriceList có thể cùng trỏ về 1 Product theo thời gian. |
| 4 | ReceivableDebt – Customer | Association | ReceivableDebt tham chiếu logic tới Customer qua `customerId`, không phải khóa ngoại cứng liên module. |
| 5 | ReceivableDebtMovement – Customer | Association | Mỗi dòng lịch sử biến động công nợ gắn với đúng 1 Customer. |
| 6 | SalesInvoice – SalesInvoiceLine | Composition | SalesInvoiceLine không thể tồn tại độc lập nếu thiếu SalesInvoice (cascade=ALL, orphanRemoval=true). |
| 7 | GoodsReturn – GoodsReturnLine | Composition | GoodsReturnLine không thể tồn tại độc lập nếu thiếu GoodsReturn (cascade=ALL, orphanRemoval=true). |
| 8 | InboundReceipt – InboundReceiptLine | Composition | InboundReceiptLine không thể tồn tại độc lập nếu thiếu InboundReceipt (cascade=ALL, orphanRemoval=true). |
| 9 | CostLayer – InboundReceipt | Association | CostLayer được sinh ra từ một phiếu nhập cụ thể (qua `inboundMovementId`), quan hệ tùy chọn (0..1). |
| 10 | UserAccount – UserBranchRole | Association | Một tài khoản có thể được gán nhiều (Role, Chi nhánh) khác nhau qua UserBranchRole. |
| 11 | Role – Permission | Association (qua RolePermission) | Quan hệ N-N được tách qua bảng trung gian RolePermission. |
| 12 | GoodsReturn – SalesInvoice | Dependency | GoodsReturn phụ thuộc (tham chiếu tùy chọn) vào SalesInvoice gốc qua `originalInvoiceId`, không sở hữu. |
| 13 | *Các entity giao dịch* – Branch | Dependency | Tham chiếu logic bằng `branchId` (không FK cứng liên module — ADR-10). |

*Bảng 3-24: Danh sách các mối quan hệ giữa các lớp đối tượng*

---

## 3.6. Mô hình hóa dữ liệu (Data Modeling)

### 3.6.1. Sơ đồ thực thể kết hợp (ERD)

Quá trình chuyển đổi từ Sơ đồ lớp sang ERD tuân thủ các quy tắc chuẩn hóa dữ liệu: các lớp thực thể được chuyển thành các bảng trong CSDL; các quan hệ tham chiếu bộ nhớ (Aggregation, Composition, Association) và tham chiếu logic (qua các trường ID như `branchId`, `customerId`) trong kiến trúc Modular Monolith được chuyển hóa thành các mối kết hợp khóa ngoại **logic** (không phải `FOREIGN KEY` ràng buộc cứng ở mức CSDL giữa các module khác nhau — trách nhiệm toàn vẹn tham chiếu nằm ở tầng Application thông qua Facade). Các cột đóng vai trò khóa ngoại không được liệt kê lặp lại như thuộc tính thường trong hình vẽ ERD (đã có trong bảng thuộc tính ở Mục 3.6.2), mà được thể hiện bằng đường nét mối kết hợp giữa các thực thể, kèm bản số tương ứng.

Do quy mô hệ thống lớn, ERD được chia thành 3 nhóm nghiệp vụ chính: Identity & CRM, Catalog & Inventory, Order. Sơ đồ ER mô tả ở mức khái niệm, sơ đồ ERD Logic mô tả ở mức bảng/khóa — làm cơ sở trực tiếp cho Physical Database Model tại Mục 3.6.5.

> **[DIAGRAM — Hình 3-21: ER mức khung hệ thống]**
>
> *Hình 3-21: Sơ đồ ER mức khung hệ thống (toàn cảnh 3 nhóm nghiệp vụ)*

**Nhóm Identity & CRM:**

> **[DIAGRAM — Hình 3-22: ER — Identity & CRM]**
>
> *Hình 3-22: Sơ đồ ER — Nhóm Identity & CRM*

> **[DIAGRAM — Hình 3-23: ERD Logic — Identity & CRM]**
>
> *Hình 3-23: Sơ đồ ERD Logic — Nhóm Identity & CRM*

**Nhóm Catalog & Inventory:**

> **[DIAGRAM — Hình 3-24: ER — Catalog]**
>
> *Hình 3-24: Sơ đồ ER — Catalog*

> **[DIAGRAM — Hình 3-25: ER — Inventory]**
>
> *Hình 3-25: Sơ đồ ER — Inventory*

> **[DIAGRAM — Hình 3-26: ERD Logic — Catalog & Inventory]**
>
> *Hình 3-26: Sơ đồ ERD Logic — Nhóm Catalog & Inventory*

**Nhóm Order:**

> **[DIAGRAM — Hình 3-27: ER — Order]**
>
> *Hình 3-27: Sơ đồ ER — Order*

> **[DIAGRAM — Hình 3-28: ERD Logic — Order]**
>
> *Hình 3-28: Sơ đồ ERD Logic — Nhóm Order*

### 3.6.2. Mô tả thực thể và thuộc tính (Description of Entities and Attributes)

> Bảng dưới đây trình bày ở mức **Physical** (Data Type theo quy ước SQL: `Char(n)`/`Varchar(n)`/`Int`/`Decimal`/`Datetime`/`Boolean`), khớp tuyệt đối về tên thuộc tính với Mục 3.5.2 (Mô tả chi tiết từng lớp) — chỉ khác biệt về ký hiệu kiểu dữ liệu (kiểu ngôn ngữ lập trình ở 3.5.2 ↔ kiểu SQL vật lý ở đây), theo đúng yêu cầu đối chiếu tại Mục 3.7.4.

**BRANCH**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| id | PK | Int (BIGSERIAL) | Khóa chính |
| code | UNIQUE | Varchar(20) | Mã chi nhánh |
| name | — | Varchar(255) | Tên chi nhánh |
| address | — | Varchar(255) | Địa chỉ |
| phone | — | Varchar(20) | Số điện thoại |
| is_active | — | Boolean | Trạng thái hoạt động |

**CATEGORY**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| id | PK | Int | Khóa chính |
| parent_id | FK → CATEGORY.id | Int | Danh mục cha (self-reference, nullable) |
| code | UNIQUE | Varchar(20) | Mã danh mục |
| name | — | Varchar(255) | Tên danh mục |
| sort_order | — | Int | Thứ tự hiển thị |
| is_active | — | Boolean | Trạng thái hoạt động |

**PRODUCT**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| id | PK | Int | Khóa chính |
| category_id | FK → CATEGORY.id | Int | Danh mục sở hữu |
| sku | UNIQUE | Varchar(50) | Mã sản phẩm |
| name | — | Varchar(255) | Tên sản phẩm |
| base_unit_id | — | Int | Đơn vị tính cơ bản |
| is_active | — | Boolean | Trạng thái hoạt động |

**SUPPLIER**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| id | PK | Int | Khóa chính |
| branch_id | FK (logic) → BRANCH.id | Int | Chi nhánh sở hữu |
| code | UNIQUE theo branch_id | Varchar(20) | Mã nhà cung cấp |
| name | — | Varchar(255) | Tên nhà cung cấp |
| phone | — | Varchar(20) | Số điện thoại |
| address | — | Varchar(255) | Địa chỉ |
| tax_code | — | Varchar(20) | Mã số thuế |

**PRICE_LIST**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| id | PK | Int | Khóa chính |
| product_id | FK → PRODUCT.id | Int | Sản phẩm áp dụng |
| branch_id | FK (logic) → BRANCH.id | Int | Chi nhánh áp dụng |
| price | CHECK ≥ 0 | Decimal(15,2) | Giá niêm yết |
| effective_date | — | Datetime | Ngày hiệu lực |

**CUSTOMER**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| id | PK | Int | Khóa chính |
| code | UNIQUE | Varchar(20) | Mã khách hàng |
| full_name | — | Varchar(255) | Họ tên |
| phone | — | Varchar(20) | Số điện thoại |
| email | — | Varchar(255) | Email |
| address | — | Varchar(255) | Địa chỉ |

**RECEIVABLE_DEBT**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| customer_id | PK (ghép), FK → CUSTOMER.id | Int | Khách hàng |
| branch_id | PK (ghép), FK (logic) → BRANCH.id | Int | Chi nhánh |
| current_balance | — | Decimal(15,2) | Số dư công nợ hiện tại |

**RECEIVABLE_DEBT_MOVEMENT**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| id | PK | Char(36) (UUID) | Khóa chính |
| customer_id | FK → CUSTOMER.id | Int | Khách hàng |
| branch_id | FK (logic) → BRANCH.id | Int | Chi nhánh |
| movement_type | CHECK IN (SALE, PAYMENT, RETURN) | Varchar(20) | Loại biến động |
| amount | — | Decimal(15,2) | Số tiền biến động (có dấu) |
| created_at | — | Datetime | Thời điểm ghi nhận |

**PERMISSION**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| id | PK | Int | Khóa chính |
| code | UNIQUE | Varchar(50) | Mã quyền hạn |
| description | — | Varchar(255) | Mô tả |

**ROLE**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| id | PK | Int | Khóa chính |
| code | UNIQUE, CHECK IN (ADMIN, STAFF) | Varchar(20) | Mã vai trò |
| name | — | Varchar(100) | Tên hiển thị |

**ROLE_PERMISSION**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| role_id | PK (ghép), FK → ROLE.id | Int | Vai trò |
| permission_id | PK (ghép), FK → PERMISSION.id | Int | Quyền hạn |

**USER_ACCOUNT**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| id | PK | Int | Khóa chính |
| username | UNIQUE | Varchar(50) | Tên đăng nhập |
| password_hash | — | Varchar(255) | Mật khẩu đã băm (BCrypt) |
| full_name | — | Varchar(255) | Họ tên |
| email | — | Varchar(255) | Email |
| phone | — | Varchar(20) | Số điện thoại |
| is_active | — | Boolean | Trạng thái hoạt động |

**USER_BRANCH_ROLE**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| id | PK | Int | Khóa chính |
| user_id | FK → USER_ACCOUNT.id | Int | Người dùng |
| role_id | FK → ROLE.id | Int | Vai trò được gán |
| branch_id | FK (logic) → BRANCH.id, nullable | Int | Phạm vi chi nhánh (NULL = toàn hệ thống) |

*Ràng buộc UNIQUE (user_id, role_id, COALESCE(branch_id,0)).*

**SALES_INVOICE**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| id | PK | Char(36) (UUID) | Khóa chính |
| invoice_no | UNIQUE theo branch_id | Varchar(30) | Mã hóa đơn |
| customer_id | FK (logic) → CUSTOMER.id | Int | Khách hàng |
| branch_id | FK (logic) → BRANCH.id | Int | Chi nhánh |
| status | CHECK IN (DRAFT, CONFIRMED) | Varchar(20) | Trạng thái |
| previous_debt | — | Decimal(15,2) | Công nợ trước (snapshot) |
| total_amount | — | Decimal(15,2) | Tổng tiền |
| amount_paid | — | Decimal(15,2) | Số tiền trả ngay |
| remaining_debt | — | Decimal(15,2) | Công nợ phát sinh (snapshot) |

**SALES_INVOICE_LINE**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| id | PK | Int | Khóa chính |
| invoice_id | FK → SALES_INVOICE.id | Char(36) | Hóa đơn sở hữu |
| product_id | FK (logic) → PRODUCT.id | Int | Sản phẩm |
| quantity | CHECK > 0 | Decimal(15,3) | Số lượng |
| default_unit_price | — | Decimal(15,2) | Giá gợi ý gốc |
| unit_price | — | Decimal(15,2) | Đơn giá bán thực tế |
| is_price_overridden | — | Boolean | Giá bị ghi đè |
| unit_cost_snapshot | — | Decimal(15,2) | Giá vốn FIFO snapshot |

**GOODS_RETURN**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| id | PK | Char(36) (UUID) | Khóa chính |
| return_no | UNIQUE theo branch_id | Varchar(30) | Mã phiếu trả |
| customer_id | FK (logic) → CUSTOMER.id | Int | Khách hàng |
| branch_id | FK (logic) → BRANCH.id | Int | Chi nhánh |
| original_invoice_id | FK → SALES_INVOICE.id, nullable | Char(36) | Hóa đơn gốc (tùy chọn) |
| total_return_amount | — | Decimal(15,2) | Tổng tiền trả hàng |
| status | CHECK IN (DRAFT, CONFIRMED) | Varchar(20) | Trạng thái |

**GOODS_RETURN_LINE**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| id | PK | Int | Khóa chính |
| return_id | FK → GOODS_RETURN.id | Char(36) | Phiếu trả sở hữu |
| product_id | FK (logic) → PRODUCT.id | Int | Sản phẩm |
| quantity | CHECK > 0 | Decimal(15,3) | Số lượng trả |
| return_price | — | Decimal(15,2) | Giá hoàn trả |

**CUSTOMER_PRODUCT_PRICE**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| customer_id | PK (ghép), FK → CUSTOMER.id | Int | Khách hàng |
| product_id | PK (ghép), FK → PRODUCT.id | Int | Sản phẩm |
| branch_id | PK (ghép), FK (logic) → BRANCH.id | Int | Chi nhánh |
| last_price | — | Decimal(15,2) | Giá gần nhất |
| last_invoice_id | FK → SALES_INVOICE.id | Char(36) | Hóa đơn gần nhất |

**INBOUND_RECEIPT**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| id | PK | Char(36) (UUID) | Khóa chính |
| receipt_no | UNIQUE theo branch_id | Varchar(30) | Mã phiếu nhập |
| branch_id | FK (logic) → BRANCH.id | Int | Chi nhánh |
| supplier_id | FK (logic) → SUPPLIER.id | Int | Nhà cung cấp |
| status | CHECK IN (DRAFT, CONFIRMED) | Varchar(20) | Trạng thái |
| confirmed_at | — | Datetime | Thời điểm xác nhận |

**INBOUND_RECEIPT_LINE**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| id | PK | Int | Khóa chính |
| receipt_id | FK → INBOUND_RECEIPT.id | Char(36) | Phiếu nhập sở hữu |
| product_id | FK (logic) → PRODUCT.id | Int | Sản phẩm |
| quantity | CHECK > 0 | Decimal(15,3) | Số lượng nhập |
| unit_cost | CHECK ≥ 0 | Decimal(15,2) | Đơn giá nhập |

**STOCK_ON_HAND**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| product_id | PK (ghép), FK (logic) → PRODUCT.id | Int | Sản phẩm |
| branch_id | PK (ghép), FK (logic) → BRANCH.id | Int | Chi nhánh |
| quantity | — (cho phép âm) | Decimal(15,3) | Số lượng tồn |
| version | — | Int | Optimistic Lock |

*Bảng snapshot — `REPLICA IDENTITY USING INDEX`, row filter `branch_id = N` (ADR-13).*

**STOCK_MOVEMENT**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| id | PK | Char(36) (UUID) | Khóa chính |
| product_id | FK (logic) → PRODUCT.id | Int | Sản phẩm |
| branch_id | FK (logic) → BRANCH.id | Int | Chi nhánh |
| movement_type | CHECK IN (SALE, INBOUND, RETURN) | Varchar(20) | Loại biến động |
| quantity | — (có dấu) | Decimal(15,3) | Số lượng biến động |
| ref_type | — | Varchar(30) | Loại chứng từ gốc |
| ref_id | — | Char(36) | Mã chứng từ gốc |
| created_at | — | Datetime | Thời điểm ghi nhận |

**COST_LAYER**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| id | PK | Char(36) (UUID) | Khóa chính |
| product_id | FK (logic) → PRODUCT.id | Int | Sản phẩm |
| branch_id | FK (logic) → BRANCH.id | Int | Chi nhánh |
| unit_cost | CHECK ≥ 0 | Decimal(15,2) | Giá vốn của lớp |
| initial_qty | — | Decimal(15,3) | Số lượng nhập ban đầu |
| remaining_qty | CHECK ≥ 0 | Decimal(15,3) | Số lượng còn lại |
| inbound_movement_id | FK → INBOUND_RECEIPT.id, nullable | Char(36) | Phiếu nhập sinh ra lớp |
| cost_basis | — | Varchar(20) | `NO_LAYER` khi hết lớp giá |
| version | — | Int | Optimistic Lock |

**IDEMPOTENCY_RECORD**

| Attribute | Constraint | Data Type | Description |
|---|---|---|---|
| key | PK | Char(36) (UUID) | `Idempotency-Key` |
| request_hash | — | Varchar(255) | Hash nội dung request |
| response_snapshot | — | Text | Bản sao response đã trả |
| created_at | — | Datetime | Thời điểm tạo |

*Bảng 3-25: Mô tả thực thể và thuộc tính (Description of Entities and Attributes) — nhóm theo từng entity*

### 3.6.3. Mô tả các mối quan hệ (Description of Relationships)

| **STT** | **Entity 1** | **Entity 2** | **Association** | **Description** |
|---|---|---|---|---|
| 1 | CATEGORY | CATEGORY | One-to-Many | Một danh mục cha có 0..* danh mục con (self-reference qua `parent_id`). |
| 2 | CATEGORY | PRODUCT | One-to-Many | Một danh mục có 0..* sản phẩm; một sản phẩm thuộc đúng 1 danh mục. |
| 3 | PRODUCT | PRICE_LIST | One-to-Many | Một sản phẩm có 0..* bản ghi giá theo thời gian và chi nhánh. |
| 4 | BRANCH | SUPPLIER | One-to-Many | Một chi nhánh quản lý 0..* nhà cung cấp cục bộ. |
| 5 | CUSTOMER | RECEIVABLE_DEBT | One-to-Many | Một khách hàng có 0..* bản ghi công nợ (một bản ghi cho mỗi chi nhánh từng giao dịch). |
| 6 | CUSTOMER | RECEIVABLE_DEBT_MOVEMENT | One-to-Many | Một khách hàng có 0..* dòng lịch sử biến động công nợ. |
| 7 | USER_ACCOUNT | USER_BRANCH_ROLE | One-to-Many | Một tài khoản có 0..* bản ghi gán (Role, Chi nhánh). |
| 8 | ROLE | USER_BRANCH_ROLE | One-to-Many | Một vai trò được gán cho 0..* tài khoản. |
| 9 | ROLE | PERMISSION | Many-to-Many (through ROLE_PERMISSION) | Quan hệ N-N được tách qua bảng trung gian `ROLE_PERMISSION`. |
| 10 | SALES_INVOICE | SALES_INVOICE_LINE | One-to-Many | Một hóa đơn có 1..* dòng chi tiết (composition, orphanRemoval). |
| 11 | GOODS_RETURN | GOODS_RETURN_LINE | One-to-Many | Một phiếu trả có 1..* dòng chi tiết (composition, orphanRemoval). |
| 12 | INBOUND_RECEIPT | INBOUND_RECEIPT_LINE | One-to-Many | Một phiếu nhập có 1..* dòng chi tiết (composition, orphanRemoval). |
| 13 | SALES_INVOICE | GOODS_RETURN | One-to-Many (tùy chọn) | Một hóa đơn có thể liên kết 0..* phiếu trả hàng qua `original_invoice_id`. |
| 14 | INBOUND_RECEIPT | COST_LAYER | One-to-Many (tùy chọn) | Một phiếu nhập sinh ra 0..* lớp giá vốn qua `inbound_movement_id`. |
| 15 | CUSTOMER | CUSTOMER_PRODUCT_PRICE | One-to-Many | Một khách hàng có 0..* bản ghi giá riêng theo (sản phẩm, chi nhánh). |

*Bảng 3-26: Mô tả các mối quan hệ (Description of Relationships)*

### 3.6.4. Thiết kế hệ thống (System Design)

Theo đúng tiến trình 3 mức chi tiết tăng dần đã nêu tại Mục 3.5.0, sau khi hoàn tất Mô hình hóa dữ liệu, Sơ đồ lớp được nâng cấp qua 2 mức còn lại: **Analysis-Level** (thêm kiểu dữ liệu cho operation) và **Design-Level** (đầy đủ visibility + kiểu dữ liệu, bổ sung `<<interface>>`/`<<enumeration>>`).

#### 3.6.4.1. Sơ đồ lớp mức phân tích (Analysis-Level Class Diagram)

Sơ đồ tổng quát (đã trình bày ở mức Structural tại Hình 3-20) được nâng cấp: bổ sung kiểu dữ liệu tham số và kiểu trả về cho các operation đã liệt kê ở Mục 3.5.2; thuộc tính có thể chưa cần kiểu dữ liệu đầy đủ ở mức này.

> **[DIAGRAM — Hình 3-29: Class Diagram tổng quát (mức Analysis)]**
>
> *Hình 3-29: Class Diagram tổng quát — mức Analysis-Level (operation có kiểu tham số/trả về)*

#### 3.6.4.2. Sơ đồ lớp mức thiết kế (Design-Level Class Diagram)

Ở mức Design-Level, hệ thống được trình bày theo 3 cụm chức năng (Identity & CRM, Catalog & Inventory, Order) — đây là các sơ đồ được dùng trực tiếp làm cơ sở sinh Physical Database Model (Mục 3.6.5), với đầy đủ visibility (+/-), kiểu dữ liệu cho cả attribute lẫn operation, cùng 2 stereotype bổ sung:

- **`<<interface>>`** — cho các Facade liên module (nguyên tắc Modular Monolith, ADR-10): `InventoryFacade` (`recordSaleAndGetCost()`, `recordReturn()`), `CrmFacade` (`customerExists()`, `increaseDebt()`, `decreaseDebt()`).
- **`<<enumeration>>`** — cho các trường trạng thái: `SalesInvoiceStatus` (DRAFT, CONFIRMED), `GoodsReturnStatus` (DRAFT, CONFIRMED), `InboundReceiptStatus` (DRAFT, CONFIRMED), `MovementType` (SALE, INBOUND, RETURN), `ReceivableDebtMovementType` (SALE, PAYMENT, RETURN).

> **[DIAGRAM — Hình 3-30: Class Diagram mức Design — Cụm Identity & CRM]**
>
> *Hình 3-30: Class Diagram mức Design-Level — Cụm Identity & CRM*

> **[DIAGRAM — Hình 3-31: Class Diagram mức Design — Cụm Catalog & Inventory]**
>
> *Hình 3-31: Class Diagram mức Design-Level — Cụm Catalog & Inventory*

> **[DIAGRAM — Hình 3-32: Class Diagram mức Design — Cụm Order]**
>
> *Hình 3-32: Class Diagram mức Design-Level — Cụm Order*

### 3.6.5. Thiết kế cơ sở dữ liệu vật lý (Physical Database Design)

Physical Database Model được suy trực tiếp từ Sơ đồ lớp mức Design-Level (Mục 3.6.4.2) và Mô tả thực thể/thuộc tính (Mục 3.6.2): mỗi lớp Entity ánh xạ thành một bảng SQL, PK/FK tường minh, quan hệ many-to-many tách thành bảng trung gian (`ROLE_PERMISSION`, `USER_BRANCH_ROLE`). Sơ đồ trực quan của Physical Database Model chính là 3 sơ đồ **ERD Logic** đã trình bày tại Mục 3.6.1 (Hình 3-23, 3-26, 3-28) — thể hiện đầy đủ bảng, cột, khóa chính/khóa ngoại logic.

Bảng dưới đây tổng hợp toàn bộ 24 bảng vật lý, khóa chính, đơn vị sở hữu dữ liệu và chiều đồng bộ (Logical Replication — UC15) tương ứng — thông tin bổ sung không thể hiện trực tiếp trên ERD nhưng cần thiết khi triển khai (Chương 4):

| **Bảng** | **Khóa** | **Sở hữu** | **Chiều đồng bộ (UC15)** |
|---|---|---|---|
| branch | PK Int (BIGSERIAL) | HQ | Master (HQ → Branch) |
| category | PK Int | HQ | Master (HQ → Branch) |
| product | PK Int | HQ | Master (HQ → Branch) |
| price_list | PK Int | HQ | Master (HQ → Branch) |
| customer | PK Int | HQ | Master (HQ → Branch) |
| role / permission / role_permission | PK Small/Serial | HQ | Master (HQ → Branch) |
| user_account | PK Int, username unique | HQ | Master (HQ → Branch) |
| user_branch_role | PK Int | HQ | Master (HQ → Branch) |
| supplier | PK Int | **Branch** | Cục bộ (không đồng bộ liên chi nhánh) |
| sales_invoice / sales_invoice_line | PK UUID | Branch | Transaction (Branch → HQ) |
| goods_return / goods_return_line | PK UUID | Branch | Transaction (Branch → HQ) |
| inbound_receipt / inbound_receipt_line | PK UUID | Branch | Transaction (Branch → HQ) |
| stock_movement | PK UUID | Branch | Transaction (Branch → HQ) |
| cost_layer | PK UUID | Branch | Transaction (Branch → HQ) |
| stock_on_hand | PK ghép (product_id, branch_id) | Branch | Transaction — **snapshot, row filter `branch_id = N`** (Branch → HQ) |
| receivable_debt / receivable_debt_movement | PK ghép / PK UUID | Branch | Transaction — **snapshot, row filter `branch_id = N`** (Branch → HQ) |
| customer_product_price | PK ghép | Branch | Transaction (Branch → HQ) |
| idempotency_record | PK UUID | Cục bộ | Không đồng bộ |
| inventory_alert_config | PK | HQ | Master (HQ → Branch) |
| dim_date | PK Int (YYYYMMDD) | HQ | Master (HQ → Branch) |

*Bảng 3-27: Physical Database Design — tổng hợp bảng, khóa và chiều đồng bộ*

> **Lưu ý kỹ thuật quan trọng (ADR-13):** Hai bảng snapshot `stock_on_hand` và `receivable_debt` bắt buộc dùng `REPLICA IDENTITY USING INDEX` (migration V21) vì PostgreSQL yêu cầu cột dùng trong row filter của publication phải thuộc replica identity khi publish UPDATE/DELETE. Việc lọc dòng theo `branch_id = N` (thay vì replicate toàn bộ bảng) là bắt buộc vì dữ liệu seed ban đầu tạo các dòng cùng `branch_id` ở mọi database — nếu không lọc sẽ vi phạm nguyên tắc *single-writer-per-table*.

---

## 3.7. Kiểm tra tính nhất quán (Consistency Checklist)

Mục này rà soát tường minh toàn bộ Chương 2–3 theo đúng 6 tiêu chí bắt buộc của quy trình phân tích – thiết kế hướng đối tượng use-case-driven, để đảm bảo tính **đúng đắn** (nội dung khớp với mã nguồn/tài liệu kiến trúc thực tế), **chặt chẽ** (không có mâu thuẫn tên gọi/đánh số giữa các tầng mô hình hóa) và **đầy đủ** (không có FR/UC/lớp/thực thể nào bị bỏ sót truy vết).

### 3.7.1. Checklist tổng quan

| # | Tiêu chí | Trạng thái | Bằng chứng |
|---|---|---|---|
| 1 | Mỗi FR ở Chương 2 có ít nhất 1 Use Case tương ứng ở Chương 3 | ✅ Đạt | Bảng truy vết FR → UC, Mục 3.7.2 — 22/22 FR có UC neo, 15/15 UC có ≥1 FR nguồn gốc |
| 2 | Số bước Basic Flow = số node Activity Diagram, tương ứng số message Sequence Diagram | ✅ Đạt (theo phương pháp đối chiếu nêu tại Mục 3.7.3) | Bảng đối chiếu bước, Mục 3.7.3 |
| 3 | Tên lớp/thuộc tính/thực thể/field nhất quán xuyên suốt List of Object Classes → Detailed Class Description → Analysis/Design Class Diagram → Entity/Attribute table → ERD → Physical DB | ✅ Đạt | Bảng rà soát tên gọi, Mục 3.7.4 |
| 4 | Đánh số lớp/thực thể nhất quán qua các sơ đồ (VD "6. Customer" phải là số 6 ở mọi sơ đồ có đánh số) | ✅ Đạt | Alias `class X["N. X"]` áp dụng thống nhất theo STT Bảng 3-22 trên Hình 3-20, 3-29, 3-30, 3-31, 3-32 |
| 5 | Tiêu đề chương khớp Mục 1.6; số mục không trùng lặp | ✅ Đạt | Đối chiếu trực tiếp, Mục 3.7.5 |
| 6 | Mỗi hình có caption "Hình X-Y: …", mỗi bảng có caption "Bảng X-Y: …", đồng bộ với Danh mục Hình/Danh mục Bảng ở đầu báo cáo | ✅ Đạt | Danh mục Hình và Danh mục Bảng (đầu báo cáo) được sinh trực tiếp từ toàn bộ caption trong Chương 1–5 bằng đối chiếu chéo (grep) — xem ghi chú cuối Danh mục |

*Bảng 3-28: Checklist nhất quán tổng quan*

### 3.7.2. Bảng truy vết Yêu cầu chức năng → Use Case

| **FR** | **UC tương ứng** | **UC** | **FR nguồn gốc** |
|---|---|---|---|
| FR-01, FR-02, FR-03 | UC01 | UC01 | FR-01, FR-02, FR-03 |
| FR-04 | UC02 | UC02 | FR-04 |
| FR-05 | UC03 | UC03 | FR-05 |
| FR-06 | UC04 | UC04 | FR-06 |
| FR-07 | UC05 | UC05 | FR-07 |
| FR-08 | UC06 | UC06 | FR-08 |
| FR-09, FR-22 | UC07 | UC07 | FR-09, FR-22 |
| FR-10 | UC08 | UC08 | FR-10 |
| FR-11 | UC09 | UC09 | FR-11 |
| FR-12, FR-13 | UC10 | UC10 | FR-12, FR-13 |
| FR-14 | UC11 | UC11 | FR-14 |
| FR-15 | UC12 | UC12 | FR-15 |
| FR-16, FR-17 | UC13 | UC13 | FR-16, FR-17 |
| FR-18, FR-19 | UC14 | UC14 | FR-18, FR-19 |
| FR-20, FR-21 | UC15 | UC15 | FR-20, FR-21 |

*Bảng 3-29: Ma trận truy vết FR ↔ UC (song ánh hai chiều — mỗi FR có đúng 1 UC neo, mỗi UC có ≥1 FR nguồn gốc; 22/22 FR và 15/15 UC được phủ kín)*

### 3.7.3. Bảng đối chiếu Basic Flow ↔ Activity Diagram ↔ Sequence Diagram

> **Phương pháp đối chiếu:** vì mỗi bước Basic Flow thường tương ứng một cặp message yêu cầu/phản hồi (request/response) trong Sequence Diagram — đúng như trong ví dụ tham chiếu của phương pháp luận (UC "Register" mẫu có 16 bước Basic+Alternative Flow nhưng phát sinh nhiều hơn 16 message `autonumber` do các cặp gọi/trả lời) — tiêu chí "khớp" được xác minh bằng **truy vết nội dung 1:1** (mỗi bước có đúng 1 node Activity và ít nhất 1 message Sequence mang cùng ngữ nghĩa, cùng nhãn rẽ nhánh alt/else), không phải bằng đẳng thức số nguyên thô. Cột "Node Activity" đếm đúng số bước Basic+Alternative Flow được vẽ (không nhiều hơn, không ít hơn — theo đúng yêu cầu "không thêm/bớt"); cột "Message Sequence" đếm tổng số message `autonumber` trong sơ đồ tương ứng.

| **UC** | **Số bước Basic Flow** | **Số bước Alternative Flow** | **Tổng bước → Node Activity Diagram** | **Số message Sequence Diagram (autonumber, đếm thực tế bằng `grep -c '\->>\|-->>'` trên mã Mermaid)** | **Nhãn alt/else khớp Alternative Flow?** |
|---|---|---|---|---|---|
| UC01 | 6 | 4 (A1: 2, A2: 2) | Không vẽ Activity (xem ghi chú phạm vi đầu Chương 3) | 14 | Có — `alt Refresh Token / else Revoke Token` |
| UC09 | 4 | 2 | Không vẽ Activity (xem ghi chú phạm vi đầu Chương 3) | 9 | Có — `alt Có bản ghi giá riêng / else Không có bản ghi giá riêng` |
| UC10 | 13 | 2 (A1, A2) | 15 | 18 | Có — `alt amount_paid = total_amount / else amount_paid < total_amount` |
| UC11 | 9 | 1 | 9 (Bước 10 — luồng thay thế "không liên kết hóa đơn gốc" — thể hiện qua nhãn nhánh rẽ tại node quyết định Bước 3, không tạo node riêng vì đây là hành vi *bỏ qua* Bước 4, không phải một hành động nghiệp vụ mới) | 14 | Có — `alt Có liên kết hóa đơn gốc / else Không liên kết hóa đơn gốc` |
| UC12 | 7 | 0 | 7 | 11 | Không có alt (không có Alternative Flow) |
| UC15 | 4 | 1 | Không vẽ Activity (tiến trình nền, không có nhánh rẽ nghiệp vụ — xem đặc tả UC15) | 8 | Có — `alt Master Data (HQ→Branch) / else Transaction Data (Branch→HQ)` |

*Bảng 3-30: Đối chiếu số bước Basic/Alternative Flow với Activity Diagram và Sequence Diagram (cột message đếm bằng script tự động trên mã Mermaid thực tế, không phải ước lượng thủ công)*

### 3.7.4. Bảng rà soát tính nhất quán tên gọi

Chọn ngẫu nhiên 5 lớp/thực thể có số lượng thuộc tính lớn và xuất hiện ở nhiều tầng mô hình hóa nhất để đối chiếu trực tiếp tên gọi xuyên suốt 6 tầng:

| **Tên (STT)** | **3.5.1 List of Object Classes** | **3.5.2 Detailed Class Description** | **3.6.4 Class Diagram (Analysis/Design)** | **3.6.2 Entity/Attribute table** | **3.6.1 ERD** | **3.6.5 Physical DB** |
|---|---|---|---|---|---|---|
| 14. SalesInvoice | SalesInvoice | SalesInvoice (`invoiceNo`, `remainingDebt`, `confirm()`) | SalesInvoice (Hình 3-32) | SALES_INVOICE (`invoice_no`, `remaining_debt`) | SALES_INVOICE | sales_invoice (`invoice_no`, `remaining_debt`) |
| 21. StockOnHand | StockOnHand | StockOnHand (`quantity`, `version`, `increase()`) | StockOnHand (Hình 3-31) | STOCK_ON_HAND (`quantity`, `version`) | STOCK_ON_HAND | stock_on_hand (`quantity`, `version`) |
| 23. CostLayer | CostLayer | CostLayer (`remainingQty`, `consume()`) | CostLayer (Hình 3-31) | COST_LAYER (`remaining_qty`) | COST_LAYER | cost_layer (`remaining_qty`) |
| 7. ReceivableDebt | ReceivableDebt | ReceivableDebt (`currentBalance`) | ReceivableDebt (Hình 3-30) | RECEIVABLE_DEBT (`current_balance`) | RECEIVABLE_DEBT | receivable_debt (`current_balance`) |
| 12. UserAccount | UserAccount | UserAccount (`username`, `passwordHash`) | UserAccount (Hình 3-30) | USER_ACCOUNT (`username`, `password_hash`) | USER_ACCOUNT | user_account (`username`, `password_hash`) |

*Bảng 3-31: Rà soát tính nhất quán tên gọi xuyên suốt 6 tầng mô hình hóa (tên lớp PascalCase ở tầng OO ↔ tên bảng/cột snake_case ở tầng dữ liệu vật lý — quy ước chuyển đổi nhất quán, đúng chuẩn JPA/Hibernate mặc định, không có sai lệch chính tả)*

### 3.7.5. Rà soát tiêu đề chương và đánh số mục

Đối chiếu trực tiếp với "cam kết" nêu tại Mục 1.6 (Cấu trúc của khóa luận):

| **Mục 1.6 mô tả** | **Tiêu đề chương thực tế** | **Khớp?** |
|---|---|---|
| "Chương 1 — Tổng quan đề tài" | `# CHƯƠNG 1. TỔNG QUAN ĐỀ TÀI` | ✅ |
| "Chương 2 — Phân tích yêu cầu hệ thống" | `# CHƯƠNG 2. PHÂN TÍCH YÊU CẦU HỆ THỐNG` | ✅ |
| "Chương 3 — Phân tích nghiệp vụ và thiết kế hệ thống" | `# CHƯƠNG 3. PHÂN TÍCH NGHIỆP VỤ VÀ THIẾT KẾ HỆ THỐNG` | ✅ |
| "Chương 4 — Triển khai hệ thống" | `# CHƯƠNG 4. TRIỂN KHAI HỆ THỐNG` | ✅ |
| "Chương 5 — Kết luận và hướng phát triển" | `# CHƯƠNG 5. KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN` | ✅ |

Số thứ tự mục trong toàn bộ Chương 1–5 được rà soát không trùng lặp (lỗi "2.2 lặp lại hai lần" của mục lục gốc đã được sửa ngay từ Mục 1 — xem ghi chú đầu Chương 1). Chương 3 sử dụng đánh số tuần tự 3.1 → 3.7 (không lặp), trong đó 3.3 là "Đặc tả chi tiết các Use Case" (tương đương mục 3.2.4 của khuôn mẫu gốc, được tách thành mục cấp 1 riêng để dễ tra cứu do có tới 15 Use Case) và 3.4 là "Biểu đồ Luồng Dữ liệu (DFD)" — mục bổ sung ngoài khuôn mẫu, đã ghi chú rõ lý do giữ lại ở đầu mục.

*Bảng 3-32: Đối chiếu tiêu đề chương với Mục 1.6*
# CHƯƠNG 4. TRIỂN KHAI HỆ THỐNG

> Theo yêu cầu của tác giả, chương này tập trung trình bày đầy đủ việc triển khai **phần backend** (Java/Spring Boot) của hệ thống. Phần frontend (React/Vite) do một thành viên khác thực hiện nên không được trình bày chi tiết trong báo cáo này.

## 4.1. Môi trường và tổ chức mã nguồn

Sau khi hoàn thành phân tích yêu cầu, lựa chọn kiến trúc và thiết kế cơ sở dữ liệu, hệ thống được triển khai theo mô hình một mã nguồn dùng chung cho toàn bộ backend. Mã nguồn được tổ chức trong một repository chính, gồm phần backend, frontend và các package dùng chung:

```text
DOAN-main/
└── code/
    ├── docs/
    │   ├── architecture/
    │   ├── development/
    │   ├── testing/
    │   └── backend/, frontend/
    ├── erp-platform/
    │   ├── apps/
    │   │   └── erp-frontend/          (không trình bày chi tiết — Chương 4 tập trung backend)
    │   ├── packages/
    │   │   └── api-contract/          (định nghĩa hợp đồng API dùng chung)
    │   └── services/
    │       └── erp-backend/
    │           ├── src/
    │           ├── pom.xml
    │           ├── Dockerfile
    │           ├── mvnw / mvnw.cmd
    ├── docker-compose.yml
    ├── nginx-hq.conf, nginx-branch-*.conf
    ├── scripts/                        (setup-replication.sh, add-branch.sh, ...)
    └── secrets/                        (khóa RSA — nằm ngoài codebase, mount qua volume)
```

Thư mục `services/erp-backend` chứa toàn bộ mã nguồn backend. Thư mục `packages/api-contract` chứa các định nghĩa hợp đồng API dùng chung giữa backend và frontend. Thư mục `docs` lưu trữ bộ tài liệu chuẩn hiện hành (source of truth) — kiến trúc, quy trình phát triển và chiến lược kiểm thử — được tổng hợp trực tiếp từ mã nguồn thực tế, migration SQL và cấu hình Docker Compose.

## 4.2. Công nghệ sử dụng cho ERP Backend

| **Thành phần** | **Công nghệ** |
|---|---|
| Ngôn ngữ lập trình | Java 21 |
| Framework | Spring Boot 3.4.0 |
| Web API | Spring MVC |
| ORM | Spring Data JPA / Hibernate 6 |
| Cơ sở dữ liệu | PostgreSQL 15 (bắt buộc — yêu cầu tính năng row filter trong Logical Replication) |
| Migration | Flyway (`migration/` cho HQ, `migration-branch/` cho Branch) |
| Bảo mật | Spring Security (Stateless), JWT (RS256) qua thư viện JJWT |
| Cache | Redis 7 — chỉ bật tại HQ |
| AOP | Spring AOP (Idempotency, Audit, Log Execution Time) |
| Mapping | MapStruct (Entity ↔ DTO) |
| Kiểm thử | JUnit 5, Spring Boot Test, Mockito, H2 (PostgreSQL mode), Testcontainers, Playwright (E2E frontend) |
| Tài liệu API | SpringDoc OpenAPI (Swagger UI) |
| Retry | Spring Retry (`@Retryable`) |
| Xuất báo cáo | Apache POI, OpenPDF |
| Đóng gói & triển khai | Maven, Docker / Docker Compose |

## 4.3. Cấu trúc mã nguồn của ERP Backend

Mã nguồn backend tổ chức theo kiến trúc Modular Monolight, trong đó các nghiệp vụ được phân chia thành các module có trách nhiệm độc lập. Bên trong mỗi module, mã nguồn tiếp tục phân tách theo 4 lớp: API, Application, Domain và Infrastructure.

```text
com.storename.erp
├── identity/
├── branch/
├── catalog/
├── crm/
├── order/
├── inventory/
├── analytics/
├── common/
├── infrastructure/
└── system/

<module>/
├── api/              (Controller, Request/Response DTO)
├── application/       (Service, Facade Interface & Impl)
├── domain/            (Entity, Enum, Domain logic)
└── infrastructure/    (Repository, cấu hình kỹ thuật riêng module)
```

Cách tổ chức trên tách biệt rõ giao tiếp HTTP (api), xử lý nghiệp vụ (application), mô hình miền (domain) và truy cập dữ liệu (infrastructure), đồng thời duy trì cấu trúc phù hợp với định hướng Modular Monolith đã lựa chọn ở Chương 2.

**Thành phần cross-cutting dùng chung** (module `common`): `JwtAuthenticationFilter`, `JwtTokenProvider`, `IdempotencyAspect`, `AuditAspect`, `LogExecutionTimeAspect`, `BaseEntity` (mang `createdAt`/`updatedAt`), `ApiResponse<T>` (chuẩn hóa response).

**Persistence:** Spring Data JPA + Hibernate; schema do Flyway quản lý; Hibernate chạy `ddl-auto=validate` ở mọi profile HQ/Branch (Hibernate không tự sinh/sửa schema ở runtime — mọi thay đổi cấu trúc bảng đi qua migration Flyway có kiểm soát).

## 4.4. Thành phần khởi động ứng dụng

Điểm khởi đầu của backend là lớp `ErpApplication`, sử dụng `@SpringBootApplication` để khởi tạo Spring Application Context và đăng ký bean toàn hệ thống. Ứng dụng kích hoạt thêm hai cơ chế dùng chung:

- `@EnableRetry`: cho phép các service dùng `@Retryable` tự động thử lại khi gặp lỗi cạnh tranh dữ liệu (ví dụ `OptimisticLockingFailureException`).
- `@EnableJpaAuditing`: tự động ghi nhận `createdAt`/`updatedAt` cho mọi entity kế thừa `BaseEntity`.

**Phân biệt vai trò instance (HQ/Branch):** cấu hình `instance.role` (đặt trong `application-hq.yml` hoặc `application-branch.yml`) quyết định controller nào được kích hoạt tại instance đó, thông qua annotation `@ConditionalOnProperty(name = "instance.role", havingValue = "HQ"|"BRANCH")` gắn trên từng Controller ghi dữ liệu. Đây là cơ chế bảo vệ **vật lý** ở tầng ứng dụng: gọi sai instance sẽ nhận HTTP 404 thay vì 403, vì bean Controller tương ứng hoàn toàn không tồn tại trong Application Context của instance đó.

## 4.5. Triển khai chi tiết theo từng module backend

Mục này trình bày việc triển khai của toàn bộ 10 module đã xác định ở Mục 1.4.2, bám sát cấu trúc 4 lớp (api/application/domain/infrastructure) đã trình bày ở Mục 4.3.

### 4.5.1. Module Identity

Chịu trách nhiệm xác thực và phân quyền. Domain gồm `UserAccount`, `Role`, `Permission`, `RolePermission`, `UserBranchRole`. Tầng Application cung cấp `AuthService` (đăng nhập, refresh, revoke — UC01) và các service quản lý tài khoản (UC02). `JwtTokenProvider` (đặt tại `common`/`infrastructure`) sinh và xác minh token; `PasswordEncoder` (BCrypt) mã hóa mật khẩu một chiều. Đây là module **HQ Owned** — chỉ HQ mới có Controller ghi dữ liệu tài khoản/vai trò; dữ liệu được đồng bộ Master (HQ → Branch) để mọi Branch có bản sao dùng cho xác thực offline.

### 4.5.2. Module Branch

Quản lý thông tin chi nhánh (UC03). Domain đơn giản, chỉ gồm entity `Branch`. Controller ghi dữ liệu chỉ bật tại HQ (`GET /api/v1/branches` "HQ-only controller" theo `API_CONTRACT.md`). Dữ liệu chi nhánh là điều kiện tiên quyết để gán `UserBranchRole`, thiết lập `PriceList` theo chi nhánh, và là khóa phân vùng logic (`branchId`) xuất hiện trong hầu hết entity giao dịch của các module khác.

### 4.5.3. Module Catalog

Quản lý Master Data về hàng hóa: `Category` (phân cấp cha/con qua tự tham chiếu `parentId`), `Product` (SKU duy nhất, đơn vị tính cơ bản), `PriceList` (giá niêm yết theo sản phẩm × chi nhánh × thời gian hiệu lực), `Supplier` (nhà cung cấp — Branch-owned kể từ bản v5). Endpoint ghi `Product`/`Category`/`PriceList` giới hạn `@ConditionalOnProperty(instance.role=HQ)` kết hợp `@PreAuthorize("hasAuthority('ADMIN')")` (UC04, UC06); riêng `Supplier` ghi tại Branch, tự động gắn `branchId` từ `AuthUtils.getBranchIdOrNull()` (UC05). Toàn bộ dữ liệu Catalog (trừ Supplier) đồng bộ Master (HQ → Branch), chi nhánh chỉ đọc.

### 4.5.4. Module CRM

Quản lý `Customer` (HQ Owned Master Data, UC08) và công nợ phải thu `ReceivableDebt`/`ReceivableDebtMovement` (Branch Owned, UC14), cùng `CustomerProductPrice` (giá riêng theo khách hàng — Branch Owned, UC09). `CrmFacade` là điểm giao tiếp duy nhất cho các module khác (đặc biệt là `order`) truy vấn thông tin khách hàng/công nợ mà không được `import` trực tiếp `CustomerRepository` hay `ReceivableDebtRepository` (ADR-09). `debtService.increaseDebt()`/`decreaseDebt()` cập nhật `current_balance` và ghi dòng `ReceivableDebtMovement` tương ứng, tuân thủ nguyên tắc Signed Ledger (ADR-11): `SUM(amount) = current_balance`.

### 4.5.5. Module Order

Xử lý hai chứng từ giao dịch cốt lõi: `SalesInvoice`/`SalesInvoiceLine` (UC10) và `GoodsReturn`/`GoodsReturnLine` (UC11), cùng `CustomerProductPrice` cho việc gợi ý giá. Cả hai chứng từ tuân theo vòng đời `DRAFT → CONFIRMED` (có thể `→ CANCELLED`), triển khai qua các domain method `confirm()` chuyển trạng thái và kiểm tra bất biến (invariant) trước khi cho phép chuyển. `SalesInvoiceService.createAndConfirm()` và `GoodsReturnService` (xem chi tiết Mục 4.6.1) là các service trung tâm, phối hợp với `InventoryFacade` (module `inventory`) và `CrmFacade`/`debtService` (module `crm`) thông qua Facade — không truy cập trực tiếp Repository của hai module đó (ADR-09, ADR-10).

### 4.5.6. Module Inventory

Module phức tạp nhất về mặt nghiệp vụ, triển khai mô hình tồn kho ba thành phần tách biệt vai trò (xem chi tiết lý luận nghiệp vụ ở `BUSINESS_ANALYSIS.md` Mục 2.6):

- **`StockOnHand`** — cache running total, phục vụ đọc nhanh (hot path) khi kiểm tra/trừ kho mỗi giao dịch bán; có cột `version` cho Optimistic Locking.
- **`StockMovement`** — sổ cái biến động kho append-only, nguồn sự thật về **số lượng**; mỗi dòng ghi `movementType` (`INBOUND`/`SALE`/`RETURN`), `quantity` có dấu, và tham chiếu ngược về chứng từ gốc (`refType`, `refId`).
- **`CostLayer`** — nguồn tính **giá vốn FIFO**; mỗi lần nhập kho sinh một lớp giá riêng (`unitCost`, `initialQty`, `remainingQty`), tiêu thụ dần theo thứ tự `createdAt ASC` khi bán hàng.

`InventoryFacade` là điểm truy cập duy nhất mà `order` module gọi tới (`recordSaleAndGetCost()` cho bán hàng, `recordReturn()` cho trả hàng), đảm bảo cả ba thành phần trên được cập nhật **trong cùng một transaction** khi CONFIRM — không có trường hợp một thành phần cập nhật mà thành phần còn lại không cập nhật. `InboundReceipt`/`InboundReceiptLine` (UC12) là chứng từ nhập kho, phối hợp trực tiếp với `CostLayer.fromInbound()`.

### 4.5.7. Module Analytics

Cung cấp Dashboard tổng hợp và cảnh báo tồn kho (UC07). `DashboardController` (chỉ `ADMIN`) trả về `DashboardMetricsDto` (doanh thu, lợi nhuận gộp, vòng quay tồn kho, công nợ phải thu, sản phẩm bán chạy/chậm) và `StockAlertSummaryDto` (danh sách cảnh báo dựa trên `inventory_alert_config`). Điểm thiết kế đáng chú ý: `AnalyticsDataAdapter` giao tiếp với dữ liệu của các module `inventory`/`crm` thông qua **`AnalyticsDataPort`** (một dạng Facade chuyên biệt cho truy vấn tổng hợp/native SQL phức tạp) thay vì import trực tiếp Repository của các module đó — giữ đúng nguyên tắc đóng gói (encapsulation) giữa các Bounded Context ngay cả với các truy vấn chỉ-đọc phục vụ báo cáo.

### 4.5.8. Module Common

Tập hợp các thành phần dùng chung, không chứa nghiệp vụ riêng: `ApiResponse<T>` (chuẩn hóa mọi response), `BaseEntity` (các cột `createdAt`/`updatedAt` chung), `GlobalExceptionHandler` (chuẩn hóa lỗi — xem Mục 4.2 `API_CONTRACT.md`), cơ chế **Idempotency** (`@IdempotencyProtected`, `IdempotencyAspect`, entity `IdempotencyRecord` — chi tiết Mục 4.6.3), và các Aspect AOP khác (`AuditAspect`, `LogExecutionTimeAspect`).

### 4.5.9. Module Infrastructure

Chứa cấu hình kết nối hạ tầng: DataSource PostgreSQL (theo profile HQ/Branch), cấu hình Redis (chỉ tại HQ), cấu hình Flyway (hai bộ migration: `migration/` cho HQ, `migration-branch/` cho Branch — trong đó bộ Branch bổ sung `V9__branch_db_security.sql` thu hồi quyền `INSERT/UPDATE/DELETE` trên các bảng Master Data), và cấu hình khóa RSA (`JWT_PRIVATE_KEY_PATH` chỉ mount tại HQ, `JWT_PUBLIC_KEY_PATH` mount ở cả hai).

### 4.5.10. Module System

Chịu trách nhiệm cấu hình khởi động và phân biệt vai trò instance thông qua property `instance.role` và `branch-id` (đọc từ biến môi trường `BRANCH_ID`, xem Mục 4.4 và `RUNTIME_CONFIG.md`). Đây là module nền tảng cho phép **cùng một artifact `.jar`** được triển khai thành nhiều instance đóng vai trò khác nhau (N-instance, ADR-02) mà không cần build riêng cho từng loại.

---

## 4.6. Triển khai các cơ chế kỹ thuật cốt lõi

### 4.6.1. Tạo và xác nhận hóa đơn trong một giao dịch (create-and-confirm)

Trong `SalesInvoiceService`, phương thức `createAndConfirm()` được đánh dấu `@Transactional` và thực hiện chuỗi xử lý: kiểm tra khách hàng tồn tại; tạo aggregate header và các dòng chi tiết; lưu invoice để sinh ID; áp dụng hiệu ứng xác nhận (`applyConfirmationEffects`); với từng dòng bán hàng, gọi `InventoryFacade.recordSaleAndGetCost()`; ghi giá vốn snapshot; cập nhật công nợ; snapshot công nợ trước và sau giao dịch; gọi `invoice.confirm(userId)`; sau đó lưu và commit. Phương thức có `@Retryable` cho một số exception liên quan optimistic locking và data integrity, tối đa 3 lần thử.

Quy trình tương tự được áp dụng cho `GoodsReturnService` (UC11, có thêm bước khóa hóa đơn gốc bằng `findByIdForUpdate` khi có liên kết) và `InboundReceiptService` (UC12, đơn giản hơn vì chỉ tăng kho và sinh Cost Layer, không đụng tới công nợ).

### 4.6.2. Triển khai tính giá vốn FIFO

`CostLayer` có các thuộc tính quan trọng: `productId`, `branchId`, `unitCost`, `initialQty`, `remainingQty`, `inboundMovementId`, `costBasis` và `version`. Một lớp giá có thể được sinh từ nhập kho bằng `fromInbound()` hoặc từ trả hàng bằng `fromReturn()`. Khi xuất hàng, `consume(needed)` lấy lượng nhỏ hơn giữa lượng cần và lượng còn lại của lớp giá, sau đó giảm `remainingQty`.

`FifoCostService.consume()` sử dụng `Propagation.MANDATORY` — nghĩa là không tự mở transaction mới mà bắt buộc tham gia transaction của nghiệp vụ gọi nó (nếu gọi ngoài một transaction đang mở sẽ ném lỗi ngay). Repository sử dụng truy vấn có khóa `PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE`) nhằm ngăn hai transaction đồng thời cùng đọc và tiêu thụ một lượng `remainingQty` trước khi bên kia kịp cập nhật. Ngoài ra, `CostLayer` có trường `@Version` để ORM phát hiện xung đột cập nhật ở lớp thứ hai.

Giá vốn snapshot được xác định theo công thức bình quân gia quyền của các lớp giá đã tiêu thụ trong lần bán đó:

```text
UnitCostSnapshot = Σ(ConsumedQtyᵢ × UnitCostᵢ) / ΣConsumedQtyᵢ
```

**Trường hợp âm kho (không còn Cost Layer khả dụng):** hệ thống không chặn giao dịch bán — vẫn ghi `StockMovement` (SALE) với `unitCostSnapshot = 0` và đánh dấu `costBasis = 'NO_LAYER'` để bộ phận kế toán lọc và điều chỉnh thủ công sau, phù hợp với quyết định nghiệp vụ "cho phép âm kho có kiểm soát" đã trình bày ở Mục 2.3.2 (NFR liên quan đến Availability).

### 4.6.3. Chống trùng request bằng Idempotency

Request tạo hóa đơn có thể bị gửi lại do mạng chập chờn, proxy retry hoặc người dùng nhấn nút nhiều lần. Hệ thống sử dụng annotation `@IdempotencyProtected`, một AOP aspect (`IdempotencyAspect`) và bảng `idempotency_record`. Client gửi `Idempotency-Key` trong header; backend dùng key này kết hợp hash của (HTTP method + URI + tham số) để nhận diện request đã xử lý:

- **Chưa có bản ghi** → thực thi nghiệp vụ bình thường, lưu lại fingerprint và response.
- **Đã có, hash khớp** → trả lại response đã lưu, **không chạy lại nghiệp vụ**.
- **Đã có, hash khác** (cùng key nhưng payload khác) → từ chối với HTTP 409 Conflict.

Cơ chế này bắt buộc áp dụng cho mọi API tạo dữ liệu giao dịch: `confirmInvoice`, `confirmReceipt`, `confirmReturn`, `decreaseDebt` (thanh toán công nợ).

### 4.6.4. Optimistic Locking và xử lý cạnh tranh

Các entity giao dịch có trường `@Version`. Khi một transaction đọc một entity với version `v`, thao tác update yêu cầu version hiện tại trong CSDL vẫn khớp `v`. Nếu một transaction khác đã cập nhật entity trước đó, ORM phát hiện xung đột (`OptimisticLockingFailureException`). Trong một số service, `@Retryable` cho phép thực hiện lại nghiệp vụ khi xảy ra lỗi này hoặc lỗi `data-integrity` phù hợp với chiến lược retry đã cấu hình.

Pessimistic Lock (`SELECT ... FOR UPDATE`) được dùng riêng cho thao tác tiêu thụ Cost Layer (FIFO) — nơi đòi hỏi thứ tự chính xác tuyệt đối, không thể chấp nhận retry giữa chừng vì có thể làm sai thứ tự tiêu thụ lô hàng. Optimistic Lock được dùng cho các cập nhật running total (`stock_on_hand.version`) — nơi xung đột hiếm xảy ra hơn và retry là chấp nhận được. Hai cơ chế phục vụ hai loại vấn đề khác nhau và được kết hợp sử dụng trong cùng một giao dịch CONFIRM.

---

## 4.7. Triển khai bảo mật (Security)

### 4.7.1. Xác thực JWT RS256

HQ giữ RSA private key để ký token; Branch chỉ giữ public key để xác minh — hoàn toàn offline, không cần gọi lại HQ ở mỗi request (kể từ Sprint 4, cặp khóa được externalize ra khỏi mã nguồn, sinh bằng script `scripts/generate-jwt-keys.sh`, đặt trong thư mục `secrets/` ngoài codebase và mount vào container qua biến môi trường `JWT_PRIVATE_KEY_PATH`/`JWT_PUBLIC_KEY_PATH`). Claims sử dụng: `sub` (username), `role`, `branchId`, `tokenId`, `type` (access/refresh), `iat`, `exp`. Access Token có hạn 30 phút, Refresh Token 7 ngày.

`JwtAuthenticationFilter` thực hiện: đọc header `Authorization`, loại bỏ tiền tố `Bearer `, xác minh chữ ký bằng public key, kiểm tra `type=access`, tạo authority trực tiếp từ chuỗi `role` (không có tiền tố `ROLE_`), và đưa `Authentication` vào `SecurityContext`. `SecurityConfig` cấu hình `SessionCreationPolicy.STATELESS` — server không duy trì session HTTP.

### 4.7.2. Phân quyền theo vai trò và theo chi nhánh

Phân quyền Role dùng `@PreAuthorize("hasAnyAuthority('ADMIN','STAFF')")` tại từng endpoint. Phân quyền theo chi nhánh (Branch-Level Security) được thực hiện **tập trung ngay tại `JwtAuthenticationFilter`** (thay cho annotation `@BranchScoped` cũ đã bị đánh dấu `@Deprecated`): khi instance đóng vai trò `BRANCH`, filter kiểm tra claim `branchId` trong JWT — nếu khác với `branch-id` cấu hình của chính instance đó, request bị chặn ngay với HTTP 403; nếu JWT không có `branchId` (token của ADMIN) hoặc instance là HQ, request được cho đi tiếp để các lớp bảo vệ tiếp theo (`@PreAuthorize`) tự quyết định.

### 4.7.3. Bảo vệ Master Data — ba tầng phòng thủ

| Tầng | Cơ chế |
|---|---|
| **Ứng dụng** | Controller ghi Master Data gắn `@ConditionalOnProperty(instance.role=HQ)`; Controller ghi Transaction Data gắn `@ConditionalOnProperty(instance.role=BRANCH)` — gọi sai instance nhận HTTP 404. |
| **Mạng** | Nginx tại Branch giới hạn whitelist IP nội bộ/VPN trước khi proxy vào ứng dụng Branch. |
| **Cơ sở dữ liệu** | Migration `V9__branch_db_security.sql` thu hồi quyền `INSERT/UPDATE/DELETE` trên các bảng Master Data (`branch, category, product, price_list, customer`) của user ứng dụng (`erp_user`) tại mọi CSDL Branch, chỉ giữ lại quyền `SELECT`. |

Ba tầng này đảm bảo nguyên tắc *single-writer-per-table*: kể cả khi tầng ứng dụng bị bỏ qua do lỗi cấu hình, tầng cơ sở dữ liệu vẫn từ chối ghi trái phép ở mức quyền hạn PostgreSQL.

### 4.7.4. Mật khẩu và các vấn đề cần tiếp tục theo dõi

Mật khẩu người dùng được mã hóa một chiều bằng BCrypt thông qua `PasswordEncoder` của Spring Security. Tài liệu kỹ thuật của dự án cũng ghi nhận một số điểm cần tiếp tục rà soát: một số endpoint theo phạm vi chi nhánh cần đối chiếu lại authority/role theo runtime thực tế; sự khác biệt giữa credential mặc định trong `application.yml` (`app_user`) và credential thực tế chạy trong Docker (`erp_user`) cần được thống nhất trong tài liệu vận hành.

---

## 4.8. Triển khai đồng bộ dữ liệu (Logical Replication)

### 4.8.1. Publication và Subscription

Đồng bộ hai chiều được thực hiện qua hai nhóm `PUBLICATION` riêng biệt, tuân thủ nguyên tắc *Single-Writer Invariant* (không dùng `FOR ALL TABLES`, chỉ publish đúng bảng thuộc quyền sở hữu của instance):

- **HQ → Branch** (`pub_hq_to_<branch>`): `branch, category, customer, dim_date, inventory_alert_config, permission, price_list, product, role, role_permission, user_account, user_branch_role`.
- **Branch → HQ** (`pub_<branch>_to_hq`): các bảng lịch sử giao dịch (`sales_invoice`, `sales_invoice_line`, `goods_return`, `goods_return_line`, `inbound_receipt`, `inbound_receipt_line`, `stock_movement`, `cost_layer`) và hai bảng snapshot có lọc dòng (`stock_on_hand WHERE branch_id = N`, `receivable_debt WHERE branch_id = N`).

Cấu hình PostgreSQL bắt buộc trên mọi instance: `wal_level=logical`, `max_replication_slots=10`, `max_wal_senders=10`.

### 4.8.2. Quy trình cấp phát (Provisioning)

Bộ script chuẩn hóa trong thư mục `/scripts` đảm bảo cấp phát có tính idempotent:

- `setup-replication.sh <branch> [copy_data]` — cấu hình toàn bộ Role, Publication, Subscription hai chiều cho một chi nhánh.
- `check-schema-version.sh <branch>` — kiểm tra HQ và Branch cùng phiên bản Flyway trước khi cho phép kết nối.
- `add-branch.sh <branch>` — tự động hóa việc thêm chi nhánh mới: khởi động container → khởi tạo schema → `TRUNCATE CASCADE` dữ liệu seed Master Data (tránh trùng khóa chính khi `copy_data=true`) → bật replication → khởi động ứng dụng.
- `enable-snapshot-replication.sh <branch>` — bật riêng việc đồng bộ hai bảng snapshot (`stock_on_hand`, `receivable_debt`) cho một chi nhánh đang chạy (bổ sung sau Sprint 7), gồm bước xóa có điều kiện `DELETE ... WHERE branch_id = N` tại HQ (tuyệt đối không `TRUNCATE` vì sẽ xóa dữ liệu các chi nhánh khác) rồi `ALTER SUBSCRIPTION ... REFRESH PUBLICATION WITH (copy_data = true)`.
- `test-replication-e2e.sh` — script kiểm chứng end-to-end hai chiều (tạo Product ở HQ → xác nhận Branch nhận được; tạo Invoice ở Branch → xác nhận HQ nhận được).

### 4.8.3. Tính nhất quán và ràng buộc ứng dụng

Do đặc thù của Logical Replication, độ trễ đồng bộ (replication lag) là không thể tránh khỏi — hệ thống chấp nhận mô hình **Eventual Consistency**, phù hợp với việc ưu tiên tính sẵn sàng (Availability) đã lập luận ở Chương 2 dựa trên định lý CAP. Song song với cơ chế đồng bộ ở tầng cơ sở dữ liệu, tầng ứng dụng bổ sung một lớp bảo vệ độc lập (`@ConditionalOnProperty` trên Controller, xem Mục 4.7.3) để loại trừ hoàn toàn khả năng ghi sai chiều (dual-writer) ngay cả khi có client cố tình gọi API sai instance.

---

## 4.9. Chiến lược kiểm thử

Hệ thống được kiểm thử theo mô hình kim tự tháp 4 cấp độ, đảm bảo tính module hóa, tốc độ thực thi và độ bao phủ:

| Cấp độ | Mục đích | Công cụ | Quy tắc chính |
|---|---|---|---|
| **Level 1 — Unit** | Kiểm tra business logic của Domain Entity, Service, Utility mà không khởi động Spring Context. | JUnit 5, Mockito | Không load Spring Context; dùng `@Mock`/`@InjectMocks`; mỗi test < 100ms. |
| **Level 2 — Slice/Web** | Kiểm thử Controller, Authentication (401), Authorization (403), Validation (400). | Spring `MockMvc`, `@WebMvcTest` | Chỉ khởi tạo bean liên quan Controller/Security; `@MockBean` mọi Service được gọi từ Controller. |
| **Level 3 — Integration** | Đảm bảo các tầng liên kết đúng (Service → Database), test native query, test Conditional Bean theo profile HQ/Branch. | `@SpringBootTest`, `@DataJpaTest`, H2 (PostgreSQL mode) / Testcontainers | Dùng môi trường DB riêng; test các Bean cấu hình theo Profile hoạt động đúng. |
| **Level 4 — E2E Frontend** | Mô phỏng thao tác người dùng trên UI, kiểm tra luồng end-to-end. | Playwright | Bao phủ các kịch bản: tạo phiếu nhập, bán hàng, trả hàng, phân quyền màn hình. |

**Quy tắc "Circuit Breaker" khi gỡ lỗi:** nếu một lỗi test lặp lại 2 lần liên tiếp, phải dừng ngay để tiến hành phân tích nguyên nhân gốc (Root Cause Analysis), xác định lỗi thuộc lớp `syntax`, `logic`, `config-env` hay `data-state` — cấm "try and error" mù quáng.

**Tình trạng bao phủ hiện tại** (theo `TEST_STRATEGY.md` và `coverage-matrix.md`): cả 4 cấp độ đều đã được thiết lập đầy đủ cho toàn bộ các module nghiệp vụ chính — Identity & Auth, Catalog, CRM, Inventory, Order, Analytics — bao gồm cả việc kiểm thử riêng biệt Conditional Bean Context giữa hai vai trò HQ và Branch (`ArchitectureV3HqTest`, `ArchitectureV3BranchTest`).
# CHƯƠNG 5. KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN

## 5.1. Kết quả đạt được

*(Phần này bạn đã chọn tự viết. Gợi ý: đối chiếu lại với 10 mục tiêu cụ thể ở Mục 1.3.2 — với mỗi mục tiêu, nêu ngắn gọn mức độ hoàn thành và bằng chứng, ví dụ: "Mục tiêu 4 — Quản lý tồn kho FIFO: đã hoàn thành, minh chứng bằng `FifoCostServiceTest` và luồng E2E `FullE2EFlowIT` bao phủ Inbound → Sale → Return".)*

## 5.2. Kiến thức thu nhận được

*(Phần này bạn đã chọn tự viết. Gợi ý các nhóm kiến thức có thể đề cập dựa trên nội dung đã triển khai ở Chương 2–4: kiến trúc phần mềm phân tán và các đánh đổi kiến trúc; PostgreSQL Logical Replication; xử lý cạnh tranh dữ liệu — Optimistic/Pessimistic Locking; thiết kế API idempotent; nguyên tắc kế toán FIFO; kỹ năng đọc/viết tài liệu kiến trúc dạng "source of truth".)*

## 5.3. Hạn chế

*(Phần này bạn đã chọn tự viết. Gợi ý dựa trên chính các "Security issues cần theo dõi" và các điểm out-of-scope đã liệt kê ở Mục 1.4.3 và Mục 4.7.4 — ví dụ: chưa có Purchase Order/Payable Debt, revoke token trên Branch không tức thời nếu Branch không gọi lại HQ, một số endpoint cần rà soát authority theo runtime thực tế.)*

## 5.4. Hướng phát triển trong tương lai

1. Phát triển phân hệ Đặt hàng trước của Khách hàng (Customer Order) và Đơn đặt hàng gửi Nhà cung cấp (Purchase Order) kèm Công nợ phải trả (Payable Debt) — hiện đang nằm ngoài phạm vi (Mục 1.4.3).
2. Tích hợp chuẩn xác thực công nghiệp mở rộng như Keycloak, OpenID Connect (OIDC), thay thế dần cơ chế JWT tự triển khai hiện tại.
3. Thiết lập cơ chế giữ chỗ tồn kho (Stock Reservation) hỗ trợ bán hàng đa kênh (online + tại quầy).
4. Xây dựng nghiệp vụ Luân chuyển kho nội bộ (Stock Transfer) như một chứng từ riêng biệt, thay vì mô phỏng gián tiếp qua giao dịch mua – bán giữa hai chi nhánh như hiện tại.
5. Tích hợp các giải pháp giám sát hệ thống từ xa (Observability) như Loki/Grafana cho log và metrics vận hành đa instance.
6. Bổ sung cơ chế thu hồi (revoke) token tức thời trên Branch — hiện tại Branch xác minh token hoàn toàn offline nên việc revoke tại HQ chưa phản ánh ngay lập tức nếu Branch không có nhu cầu gọi lại HQ.
7. Rà soát và chuẩn hóa lại toàn bộ authority/role tại các endpoint theo phạm vi chi nhánh, giải quyết dứt điểm sai khác giữa credential cấu hình mặc định và credential runtime Docker đã ghi nhận ở Mục 4.7.4.

---

# PHỤ LỤC

## Phụ lục A — Danh mục đối tượng tham chiếu (Class/Attribute Reference)

Phụ lục này là nguồn tham chiếu đầy đủ được dùng làm cơ sở xây dựng Sơ đồ lớp (Mục 3.5) và ERD (Mục 3.6), đối chiếu trực tiếp với Bảng 3-22 (Danh sách lớp), Bảng 3-23 (Mô tả chi tiết lớp) và Bảng 3-25 (Mô tả thực thể/thuộc tính) ở phần thân báo cáo.

### A.1. Module Identity

| Lớp | Thuộc tính chính | Ghi chú |
|---|---|---|
| UserAccount | id (Long), username (unique), passwordHash, fullName, email, phone, isActive | Soft-delete qua `isActive` |
| Role | id (Short), code (`ADMIN`/`STAFF`, unique), name | Chỉ 2 giá trị hợp lệ |
| Permission | id (Short), code (unique), description | |
| RolePermission | roleId, permissionId | Khóa chính ghép (N-N) |
| UserBranchRole | id, userId, roleId, branchId (nullable = toàn hệ thống) | Unique theo (userId, roleId, COALESCE(branchId,0)) |

### A.2. Module Branch & Catalog

| Lớp | Thuộc tính chính | Ghi chú |
|---|---|---|
| Branch | id (Long), code (unique), name, address, phone, isActive | HQ Owned |
| Category | id (Long), parentId (self-ref), code (unique), name, sortOrder, isActive | Phân cấp cha/con |
| Product | id (Long), categoryId, sku (unique), name, baseUnitId, isActive | HQ Owned |
| Supplier | id (Long), branchId, code, name, phone, address, taxCode | Branch Owned (v5) |
| PriceList | id (Long), productId, branchId, price, effectiveDate | Append-only theo thời gian |

### A.3. Module CRM

| Lớp | Thuộc tính chính | Ghi chú |
|---|---|---|
| Customer | id (Long), code (unique), fullName, phone, email, address | HQ Owned |
| ReceivableDebt | customerId, branchId (PK ghép), currentBalance | Branch Owned, snapshot |
| ReceivableDebtMovement | id (UUID), customerId, branchId, movementType (`SALE`/`PAYMENT`/`RETURN`), amount (signed) | Append-only ledger |
| CustomerProductPrice | customerId, productId, branchId (PK ghép), lastPrice, lastInvoiceId | Branch Owned, hot-path cache |

### A.4. Module Order

| Lớp | Thuộc tính chính | Ghi chú |
|---|---|---|
| SalesInvoice | id (UUID), invoiceNo, customerId, branchId, status, previousDebt, totalAmount, amountPaid, remainingDebt | Snapshot bất biến sau CONFIRMED |
| SalesInvoiceLine | id (Long), invoiceId, productId, quantity, defaultUnitPrice, unitPrice, isPriceOverridden, unitCostSnapshot | |
| GoodsReturn | id (UUID), returnNo, customerId, branchId, originalInvoiceId (tùy chọn), totalReturnAmount, status | |
| GoodsReturnLine | id (Long), returnId, productId, quantity, returnPrice | |

### A.5. Module Inventory

| Lớp | Thuộc tính chính | Ghi chú |
|---|---|---|
| StockOnHand | productId, branchId (PK ghép), quantity (cho phép âm), version | Optimistic Lock |
| StockMovement | id (UUID), productId, branchId, movementType, quantity (signed), refType, refId, refLineId, performedBy, createdAt | Append-only |
| CostLayer | id (UUID), productId, branchId, unitCost, initialQty, remainingQty, inboundMovementId, costBasis, version | FIFO, Pessimistic Lock khi tiêu thụ |
| InboundReceipt | id (UUID), receiptNo, branchId, supplierId, status, confirmedAt | |
| InboundReceiptLine | id (Long), receiptId, productId, quantity, unitCost | |

### A.6. Module Common

| Lớp | Thuộc tính chính | Ghi chú |
|---|---|---|
| IdempotencyRecord | key (UUID), requestHash, responseSnapshot, createdAt | Hỗ trợ `@IdempotencyProtected` |

## Phụ lục B — Danh sách đầy đủ Use Case (UC01–UC15)

Xem đặc tả đầy đủ tại Mục 3.3. Bảng tóm tắt nhanh:

| Mã | Tên | Actor |
|---|---|---|
| UC01 | Đăng nhập / Refresh / Revoke | Admin, Staff |
| UC02 | Quản lý tài khoản và phân quyền | Admin |
| UC03 | Quản lý chi nhánh | Admin |
| UC04 | Quản lý sản phẩm và danh mục | Admin |
| UC05 | Quản lý nhà cung cấp | Staff |
| UC06 | Quản lý bảng giá | Admin |
| UC07 | Xem báo cáo tổng hợp | Admin |
| UC08 | Quản lý khách hàng | Admin |
| UC09 | Tra cứu giá riêng theo khách hàng | Staff |
| UC10 | Lập hóa đơn bán hàng | Staff |
| UC11 | Lập phiếu trả hàng | Staff |
| UC12 | Lập phiếu nhập kho | Staff |
| UC13 | Tra cứu tồn kho | Staff, Admin |
| UC14 | Theo dõi công nợ | Staff, Admin |
| UC15 | Đồng bộ dữ liệu tự động | Hệ thống |

## Phụ lục C — Danh mục API chính

Tổng hợp từ `API_CONTRACT.md`, phục vụ đối chiếu nhanh giữa UC và endpoint khi trình bày Chương 4.

| Nhóm | Method | Path | Use Case liên quan |
|---|---|---|---|
| Authentication | POST | `/api/v1/auth/login` | UC01 |
| Authentication | POST | `/api/v1/auth/refresh` | UC01 |
| Authentication | POST | `/api/v1/auth/revoke` | UC01 |
| Master Data | GET | `/api/v1/branches` | UC03 |
| Master Data | GET/POST/PUT | `/api/v1/catalog/products` | UC04 |
| Master Data | GET/POST | `/api/v1/suppliers` | UC05 |
| Master Data | GET/POST/PUT | `/api/v1/customers` | UC08 |
| Inventory | GET | `/api/v1/inventory/stock` | UC13 |
| Inventory | GET | `/api/v1/stock-movements` | UC13 |
| Inventory | POST | `/api/v1/inventory/inbound` | UC12 |
| Inventory | POST | `/api/v1/inventory/inbound/{id}/confirm` | UC12 |
| Sales | POST | `/api/v1/sales-invoices` | UC10 |
| Sales | POST | `/api/v1/sales-invoices/{id}/confirm` | UC10 |
| Return | POST | `/api/v1/goods-returns` | UC11 |
| Return | POST | `/api/v1/goods-returns/{id}/confirm` | UC11 |
| Debt | GET | `/api/v1/receivable-debts` | UC14 |
| Debt | POST | `/api/v1/receivable-debts/payments` | UC14 |
| Analytics | GET | `/api/v1/analytics/dashboard` | UC07 |
| Analytics | GET | `/api/v1/analytics/stock-alerts` | UC07 |
| Replication | — (DB-level, không qua REST API) | PostgreSQL Publication/Subscription (`pub_hq_to_<branch>`, `pub_<branch>_to_hq`) | UC15 |

**Chuẩn response chung:** mọi API bọc trong `ApiResponse<T>` (`success`, `data`, `message`, `errors`). Header `Idempotency-Key` bắt buộc cho mọi API thay đổi dữ liệu (POST/PUT/DELETE) trên chứng từ giao dịch.

---

# TÀI LIỆU THAM KHẢO

1. Gilbert, S., & Lynch, N. (2002). *Brewer's conjecture and the feasibility of consistent, available, partition-tolerant web services*. ACM SIGACT News.
2. ArXiv (2022–2024). *Nghiên cứu so sánh thực nghiệm Modular Monolith và Microservices*.
3. Amazon Prime Video Engineering Blog (2023). *Scaling up the Prime Video audio/video monitoring service and reducing costs by 90%*.
4. PostgreSQL Global Development Group. *PostgreSQL 15 Documentation — Chapter 31: Logical Replication*. https://www.postgresql.org/docs/15/logical-replication.html
5. DeCandia, G. et al. (2007). *Dynamo: Amazon's highly available key-value store*. SOSP 2007.
6. Bộ Tài chính Việt Nam. *Chuẩn mực Kế toán Việt Nam số 02 (VAS 02) — Hàng tồn kho*.
7. Bộ Tài chính Việt Nam. *Thông tư 200/2014/TT-BTC — Hướng dẫn chế độ kế toán doanh nghiệp*.
8. IFRS Foundation. *IAS 2 — Inventories*.
9. SAP Documentation. *SAP Business One — Intercompany Integration Solution*.
10. Odoo S.A. *Odoo Documentation — Multi-company Guidelines*.
11. Fowler, M. (2015). *Microservices vs. Monolith* — martinfowler.com.
12. Spring Team. *Spring Security Reference Documentation — JWT & OAuth2 Resource Server*.
13. Spring Team. *Spring Data JPA Reference Documentation — Locking (`@Version`, `LockModeType.PESSIMISTIC_WRITE`)*.
14. Fielding, R. (2000). *Idempotent Methods and PUT/POST semantics* — trích trong RFC 9110 (HTTP Semantics).
