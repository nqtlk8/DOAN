package com.storename.erp.inventory.api;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Facade interface cung cấp API giao tiếp cho các Bounded Context khác (Sales, Purchasing).
 * Tuân thủ kiến trúc Modular Monolith: Module ngoài chỉ gọi Facade, không truy cập trực tiếp Repository hoặc Entity của Inventory.
 */
public interface InventoryFacade {

    /**
     * Lấy số lượng tồn kho khả dụng của một sản phẩm tại một chi nhánh.
     * @param productId ID của sản phẩm (tham chiếu sang Catalog module).
     * @param branchId ID của chi nhánh.
     * @return Số lượng tồn kho hiện tại (0 nếu không có dữ liệu).
     */
    BigDecimal getAvailableQuantity(Long productId, Long branchId);

    /**
     * Ghi nhận xuất kho bán hàng (SALE): Tiêu thụ FIFO, giảm tồn kho, ghi nhận movement.
     * Không cho tồn âm: ném {@link com.storename.erp.inventory.domain.StockInsufficientException}
     * (HTTP 409, mã INSUFFICIENT_STOCK) nếu tồn kho không đủ.
     * @return Kết quả giá vốn (unitCostSnapshot, costBasis)
     */
    SaleCostResult recordSaleAndGetCost(Long productId, Long branchId, BigDecimal quantity, String invoiceId, UUID lineId, UUID userId);

    /**
     * Ghi nhận nhập kho do khách trả hàng (RETURN): Tăng tồn kho, ghi nhận movement, tạo cost layer.
     * @param unitCost GIÁ VỐN của hàng trả (snapshot giá vốn lúc bán), KHÔNG phải giá bán.
     */
    void recordReturn(Long productId, Long branchId, BigDecimal quantity, BigDecimal unitCost, String returnId, UUID lineId, UUID userId);

    /**
     * Giá vốn của lớp giá vốn tạo gần nhất của sản phẩm tại chi nhánh (kể cả lớp đã dùng hết).
     * @return null nếu sản phẩm chưa từng có lớp giá vốn.
     */
    BigDecimal getLatestLayerUnitCost(Long productId, Long branchId);

    record SaleCostResult(BigDecimal unitCostSnapshot, String costBasis) {}
}
