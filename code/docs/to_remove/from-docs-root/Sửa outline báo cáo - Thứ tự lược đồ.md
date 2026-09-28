# Sửa outline báo cáo - Thứ tự lược đồ

Sep 24, 2026 · @Thang

Báo cáo hiện tại thiếu 2 lược đồ bắt buộc (Class Diagram, ERD - chỉ có tiêu đề, không có hình) và sắp xếp sai thứ tự quy trình phân tích - thiết kế theo đúng 2 file bài giảng đã cung cấp; nội dung dưới đây chẩn đoán lỗi và đưa ra outline đầy đủ, đúng trình tự.

## 1. Quy trình chuẩn (theo 2 file bài giảng)

Cả 2 file bài giảng ("Mô hình hóa yêu cầu" và "Mô hình hóa dữ liệu") thống nhất một trình tự 3 giai đoạn, mỗi giai đoạn sinh ra nhóm lược đồ riêng — không được trộn lẫn hay đảo thứ tự:

1. **Xác định yêu cầu** (file "Xác định yêu cầu"): khảo sát hiện trạng, xác lập phạm vi, đặc tả yêu cầu bằng văn bản. Giai đoạn này KHÔNG bắt buộc lược đồ UML, là input cho giai đoạn sau (đã có trong báo cáo ở Chương 1).
2. **Mô hình hóa yêu cầu / phân tích** (file "Mô hình hóa yêu cầu"), gồm đúng 3 nhóm theo thứ tự cố định:
   - **Mô hình hóa chức năng**: Use Case Diagram (kèm đặc tả UC) → Activity Diagram (cho từng quy trình nghiệp vụ chính) → Data Flow Diagram (DFD, cấp 0 rồi đến cấp 1 nếu cần)
   - **Mô hình hóa cấu trúc**: Class Diagram (Domain/Analysis Class Diagram)
   - **Mô hình hóa hành vi**: Sequence Diagram cho từng chức năng chính
3. **Mô hình hóa thiết kế – dữ liệu** (file "Mô hình hóa dữ liệu"): Entity-Relationship Diagram (ERD) mức quan niệm.

Điểm mấu chốt nằm ở slide "Sơ đồ tuần tự" (file Mô hình hóa yêu cầu, mục Các bước tiến hành): quy trình vẽ Sequence Diagram gồm 5 bước — (1) dựa Use Case Diagram xác định chức năng, (2) dựa Activity Diagram xác định các bước, (3) **đối chiếu với Class Diagram** để xác định lớp tham gia, (4) vẽ Sequence Diagram, (5) cập nhật lại Class Diagram. Nói cách khác, **Class Diagram phải tồn tại trước khi vẽ được Sequence Diagram đúng nghĩa** — đây là căn cứ trực tiếp cho thấy báo cáo hiện tại sai thứ tự.

**Thứ tự chuẩn cho Chương 3 của báo cáo:**

Use Case Diagram → Activity Diagram → Data Flow Diagram → Class Diagram → Sequence Diagram(s) → ERD

## 2. Đánh giá báo cáo hiện tại

Thứ tự hiện tại trong Chương 3: Usecase (3.1) → Domain/Class (3.2) → ERD (3.3) → \[nhảy số, không có 3.4\] → Activity (3.5) → 4 Sequence (3.6–3.9) → DFD (3.10). Đối chiếu với quy trình chuẩn:

| Mục trong báo cáo | Lược đồ | Tình trạng | Vấn đề |
| --- | --- | --- | --- |
| 3.1 | Usecase Diagram | Có hình | Đúng vị trí (đứng đầu) |
| 3.2 | Domain Model / Class Diagram | **Chỉ có tiêu đề, không có hình** (không xuất hiện trong "Danh sách sơ đồ hình ảnh") | **Thiếu hoàn toàn** + sai vị trí (phải đứng sau Activity/DFD, không phải ngay sau Usecase) |
| 3.3 | ERD | **Chỉ có tiêu đề, không có hình** | **Thiếu hoàn toàn** + sai vị trí (ERD thuộc giai đoạn thiết kế dữ liệu, phải đứng SAU CỪ Class Diagram và Sequence Diagram) |
| — | (số 3.4 bị bỏ trống) | — | Dấu hiệu cho thấy phần nội dung tương ứng đã bị xóa/thiếu khi biên tập |
| 3.5 | Activity Diagram – Sales Invoice | Có hình | Sai vị trí: phải đứng ngay sau Usecase (thuộc nhóm "mô hình hóa chức năng"), không phải sau Class/ERD |
| 3.6 | Sequence – Đăng nhập/JWT | Có hình | Sai vị trí: vẽ trước khi có Class Diagram là trái quy trình 5 bước |
| 3.7 | Sequence – Tạo & xác nhận Sales Invoice | Có hình | Sai vị trí (như trên) |
| 3.8 | Sequence – Tra cứu giá riêng theo KH | Có hình | Sai vị trí (như trên) |
| 3.9 | Sequence – Logical Replication | Có hình | Sai vị trí (như trên) |
| 3.10 | Data Flow Diagram | Có hình (1 cấp, không rõ cấp 0/1) | Sai vị trí: DFD thuộc nhóm "mô hình hóa chức năng" cùng Usecase/Activity, không được đặt sau cùng |

**Tóm tắt 2 lỗi chính:**

1. **Thiếu lược đồ bắt buộc**: Class Diagram và ERD chỉ có tiêu đề mục, không có hình vẽ thực tế.
2. **Sai thứ tự quy trình**: DFD và Activity bị tách rời khỏi nhóm "mô hình hóa chức năng"; các Sequence Diagram được đặt trước Class Diagram thay vì sau; ERD bị đặt ở đầu thay vì cuối chuỗi lược đồ phân tích – thiết kế.

## 3. Outline báo cáo đề xuất (đầy đủ, đúng thứ tự)

Chương 1 và 2 giữ nguyên như bản hiện tại. Chương 3 được thiết kế lại toàn bộ theo đúng quy trình ở mục 1.

### CHƯƠNG 1: TỔNG QUAN

(giữ nguyên: Vấn đề giải quyết, Mục tiêu, Đối tượng nghiên cứu, Phạm vi nghiên cứu)

### CHƯƠNG 2: THIẾT KẾ KIẾN TRÚC CỦA HỆ THỐNG

(giữ nguyên: Yêu cầu nghiệp vụ chi tiết, Yêu cầu phi chức năng, Các phương án kiến trúc, Kiến trúc Module và Façade – Hình 1, Mô hình dữ liệu và phân quyền sở hữu, Bảo mật và xác thực)

### CHƯƠNG 3: PHÂN TÍCH NGHIỆP VỤ VÀ THIẾT KẾ CƠ Sở Dữ LIỆU

**3.1. Use Case Diagram**

- 3.1.1. Sơ đồ Use Case tổng quát (actor Admin, Staff) — giữ hình hiện có
- 3.1.2. Đặc tả các Use Case chính (bảng: tên UC, actor, mô tả, luồng sự kiện, điều kiện thoát) — **bổ sung mới**

**3.2. Activity Diagram** (mô hình hóa chức năng)

- 3.2.1. Activity – Bán hàng (Sales Invoice) — giữ hình hiện có, chuyển từ 3.5 lên đây
- 3.2.2. Activity – Trả hàng (Goods Return, Draft→Confirm) — **bổ sung mới**
- 3.2.3. Activity – Nhập kho (Inbound Receipt, Draft→Confirm) — **bổ sung mới**

**3.3. Data Flow Diagram (DFD)** (mô hình hóa chức năng)

- 3.3.1. DFD cấp 0 (Context Diagram) — giữ hình hiện có, chuyển từ 3.10 lên đây, ghi rõ đây là cấp 0
- 3.3.2. DFD cấp 1 cho tiến trình Bán hàng hoặc Đồng bộ dữ liệu — khuyến nghị bổ sung (không bắt buộc)

**3.4. Class Diagram** (mô hình hóa cấu trúc) — **thiếu hoàn toàn, bắt buộc bổ sung**

- Sơ đồ lớp domain gồm tối thiểu: User, Role, Permission, Branch, Category, Product, Supplier, PriceList, Customer, SalesInvoice, SalesInvoiceLine, GoodsReturn, GoodsReturnLine, InboundReceipt, InboundReceiptLine, StockMovement, CostLayer, ReceivableDebt, ReceivableDebtMovement, IdempotencyRecord
- Thể hiện đầy đủ quan hệ association / aggregation / composition / generalization giữa các lớp, kèm thuộc tính và bản số

**3.5. Sequence Diagram** (mô hình hóa hành vi — vẽ SAU khi có Class Diagram ở 3.4)

- 3.5.1. Đăng nhập và cấp JWT — giữ hình hiện có
- 3.5.2. Tạo và xác nhận Sales Invoice (atomic) — giữ hình hiện có
- 3.5.3. Trả hàng – Goods Return Confirm — **bổ sung mới**, đồng bộ với Activity 3.2.2
- 3.5.4. Nhập kho – Inbound Receipt Confirm (tạo Cost Layer) — **bổ sung mới**, đồng bộ với Activity 3.2.3
- 3.5.5. Tra cứu giá riêng theo khách hàng — giữ hình hiện có
- 3.5.6. Đồng bộ dữ liệu – Logical Replication — giữ hình hiện có

**3.6. ERD (Entity-Relationship Diagram)** — **thiếu hoàn toàn, bắt buộc bổ sung**, đặt sau cùng vì là kết quả thiết kế dữ liệu sau cùng

- Thực thể, thuộc tính, khóa, bản số cho toàn bộ dữ liệu ở 3.4
- Lưu ý đặc thù cần thể hiện rõ: cột `branch_id` (0,1) trên Customer để phân biệt dữ liệu dùng chung toàn hệ thống / riêng từng chi nhánh; phạm vi sở hữu dữ liệu HQ vs Branch theo `setup-replication.sh`

### CHƯƠNG 4: TRIỂN KHAI HỆ THỐNG

(giữ nguyên như bản hiện tại)

**Cập nhật "Danh sách sơ đồ hình ảnh"** theo thứ tự mới ở trên, đánh lại số hình liên tục từ 1 đến hết (dự kiến \~13 hình sau khi bổ sung).

## 4. Ghi chú thực hiện

- **Vì sao Class Diagram phải đứng trước Sequence Diagram**: giáo trình nêu rõ quy trình 5 bước vẽ Sequence Diagram bắt buộc bước 3 là đối chiếu Class Diagram để xác định lớp tham gia; không có Class Diagram thì không có căn cứ để xác định object/lifeline trong Sequence Diagram.
- **Vì sao ERD đặt sau cùng**: ERD thuộc giai đoạn "Mô hình hóa thiết kế – dữ liệu", là bước chuyển thể từ Domain/Class Diagram sang mô hình dữ liệu quan niệm (thực thể, thuộc tính, mối kết hợp, bản số) — không thể làm trước khi chưa có danh sách lớp/thuộc tính ổn định.
- **DFD hiện chỉ có 1 hình, không rõ cấp**: theo lý thuyết DFD có cấp 0/1/2, nên gọi rõ hình hiện có là "DFD cấp 0 – Context Diagram"; có thể bổ sung DFD cấp 1 cho tiến trình chính để đầy đủ hơn nhưng không bắt buộc ở quy mô khóa luận.
- **Thiếu lược đồ cho 2 quy trình nghiệp vụ quan trọng**: mục 2.1.3 (Trả hàng) và 2.1.4 (Nhập kho) trong Chương 2 đã mô tả quy trình Draft–Confirm bằng lời nhưng chưa có Activity/Sequence Diagram minh họa tương ứng — nên bổ sung để đồng bộ với Sales Invoice đã có đầy đủ cả hai.
- **Cập nhật bảng "Danh sách sơ đồ hình ảnh"** ngay sau khi hoàn thiện lại Chương 3, và rà soát lại toàn bộ số thứ tự mục (3.1–3.6) để không bị nhảy số như bản hiện tại (3.3 → 3.5).
- Phạm vi của tài liệu này chỉ là **outline nội dung**; việc vẽ cụ thể từng lược đồ (Class Diagram, ERD, Activity, Sequence bổ sung) do bạn thực hiện dựa trên danh sách lớp/thuộc tính và quy trình nghiệp vụ đã mô tả trong chính báo cáo (Chương 2 và 4).
