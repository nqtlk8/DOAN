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
     * (Dự phòng Sprint 2.2+)
     * Xác nhận giữ chỗ tồn kho (Reservation) cho đơn hàng Sales đang xử lý.
     * @param productId ID sản phẩm.
     * @param branchId ID chi nhánh.
     * @param quantity Số lượng cần giữ chỗ.
     * @param referenceId ID của SalesOrder.
     * @return true nếu giữ chỗ thành công, false nếu không đủ hàng.
     */
    boolean reserveStock(Long productId, Long branchId, BigDecimal quantity, UUID referenceId);

    /**
     * Ghi nhận xuất kho bán hàng (SALE): Tiêu thụ FIFO, giảm tồn kho, ghi nhận movement.
     * @return Kết quả giá vốn (unitCostSnapshot, costBasis)
     */
    SaleCostResult recordSaleAndGetCost(Long productId, Long branchId, BigDecimal quantity, String invoiceId, UUID lineId, UUID userId);

    /**
     * Ghi nhận nhập kho do khách trả hàng (RETURN): Tăng tồn kho, ghi nhận movement, tạo cost layer.
     */
    void recordReturn(Long productId, Long branchId, BigDecimal quantity, BigDecimal returnPrice, String returnId, UUID lineId, UUID userId);

    record SaleCostResult(BigDecimal unitCostSnapshot, String costBasis) {}
}
