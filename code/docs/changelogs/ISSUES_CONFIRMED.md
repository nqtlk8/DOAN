# DANH SÁCH LỖI THẬT (ISSUES_CONFIRMED)

## GIAI ĐOẠN 0: XÁC MINH GIẢ THUYẾT BASE ENTITY BỊ BỎ SÓT
- **Phát hiện:** Lớp BaseEntity CÓ tồn tại tại com.storename.erp.common.domain.BaseEntity và chứa đầy đủ các trường: id, ersion, isDeleted, createdAt, updatedAt, createdBy, updatedBy.
- **Danh sách Entity kế thừa:** Customer, ReceivableDebt, InboundReceipt, InboundReceiptLine, StockOnHand, CustomerProductPrice, GoodsReturn, GoodsReturnLine, SalesInvoice, SalesInvoiceLine.
- **Kết luận:** TẤT CẢ các cảnh báo "Orphaned column" hoặc "Nullability" liên quan tới các trường audit (created_at, updated_at, v.v.) của các bảng trên trong file DATA_CONTRACT.md trước đó ĐỀU LÀ FALSE POSITIVE. Nguyên nhân là do tool python trước đó chỉ đọc trực tiếp file entity con mà không scan nội dung từ lớp cha. Tôi đã ĐÁNH DẤU LOẠI BỎ hoàn toàn các lỗi giả này.

## GIAI ĐOẠN 1: SOI LẠI TỪNG "MISMATCH" NGHI VẤN
Sau khi kiểm tra thủ công mã nguồn (parse chính xác các annotation @Column, @JoinColumn thay vì dựa vào thứ tự dòng):
1. **Bảng category:** Không có lỗi. Field parent được map chính xác qua @JoinColumn(name = "parent_id"). Field code được map ngầm định (camelCase) và có @Column(length=50). Báo cáo cũ ghép nhầm code với parent_id là do parse sai cú pháp.
2. **Bảng product, supplier, customer, user_account, ole, permission:** Đều khớp 100% về cột.
3. **Bảng sales_invoice vs goods_return:** Có một cảnh báo nhỏ về kiểu dữ liệu. Cột 	otal_amount của sales_invoice là 
umeric(19,4), nhưng của goods_return lại là 
umeric(38,2). Cả 2 đều dùng BigDecimal trong Entity. Không gây Crash nhưng là 1 Warning về tính nhất quán.

## GIAI ĐOẠN 2: DANH SÁCH LỖI THẬT SAU KHI LOẠI FALSE POSITIVE (ISSUES_CONFIRMED.md)

Qua quá trình rà soát gắt gao bằng tay, xin thông báo: **KHÔNG CÓ LỖI CRITICAL NÀO TỒN TẠI VỀ MẶT SCHEMA MAPPPING**. Tất cả Entity đều ánh xạ đúng và đủ 100% cột bắt buộc (NOT NULL) của DB.

Tuy nhiên, có một số lỗi cấp độ **WARNING / INFO** về tính nhất quán và chặt chẽ của mã nguồn:

### 1. WARNING: Không nhất quán kiểu dữ liệu Numeric (Precision/Scale)
- **Vị trí:** 
  - com.storename.erp.order.domain.GoodsReturn (dòng 23)
  - com.storename.erp.order.domain.GoodsReturnLine (dòng 20, 23)
- **Chi tiết:** Các cột tiền tệ như 	otalAmount, unitPrice được Hibernate tự sinh (ddl-auto) thành 
umeric(38,2) thay vì chuẩn chung 
umeric(19,4) như bảng sales_invoice.
- **Hành động sửa:** Bổ sung @Column(precision = 19, scale = 4) vào các trường tiền tệ của module Trả Hàng.
- **Mức độ ảnh hưởng:** Không gây lỗi Runtime, nhưng làm lệch chuẩn Precision của hệ thống ERP (có thể gây sai số làm tròn khi report).

### 2. INFO: Thiếu annotation @NotNull dù DB là NOT NULL
- **Vị trí:** Đa số các class Entity (Ví dụ: Category.java, Product.java).
- **Chi tiết:** DB định nghĩa là NOT NULL và Hibernate đã dùng @Column(nullable = false), tuy nhiên thiếu vắng @NotNull (Jakarta Validation) ở lớp Model. 
- **Hành động sửa:** (Tùy chọn) Có thể bổ sung @NotNull để Java báo lỗi ngay từ khâu Validate trước khi chạm xuống DB.

---
**KẾT LUẬN GIAI ĐOẠN 2:**
Tất cả các "lỗi ngớ ngẩn" trong báo cáo trước hoàn toàn là do thuật toán parse mã nguồn bằng Regex của Python bị sai lệch ngữ cảnh. Bản thân mã nguồn Java hiện tại đã khớp chặt chẽ với Database.

**Tôi đang CHỜ XÁC NHẬN từ bạn:**
Bạn có muốn tôi bỏ qua Giai đoạn 3 (vì không có lỗi Critical) và tiến thẳng tới GIAI ĐOẠN 4: Rebuild Docker-compose --no-cache để kiểm chứng khởi động Flyway thực tế không?
