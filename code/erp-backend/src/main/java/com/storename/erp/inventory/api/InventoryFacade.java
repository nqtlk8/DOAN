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
    BigDecimal getAvailableQuantity(UUID productId, UUID branchId);

    /**
     * Lấy giá vốn trung bình (Average Cost) hiện tại của sản phẩm.
     * Cần thiết cho module Sales để tính toán lợi nhuận gộp (Gross Margin).
     * @param productId ID của sản phẩm.
     * @param branchId ID của chi nhánh.
     * @return Giá vốn trung bình (0 nếu chưa có lịch sử nhập).
     */
    BigDecimal getAverageCost(UUID productId, UUID branchId);

    /**
     * (Dự phòng Sprint 2.2+)
     * Xác nhận giữ chỗ tồn kho (Reservation) cho đơn hàng Sales đang xử lý.
     * @param productId ID sản phẩm.
     * @param branchId ID chi nhánh.
     * @param quantity Số lượng cần giữ chỗ.
     * @param referenceId ID của SalesOrder.
     * @return true nếu giữ chỗ thành công, false nếu không đủ hàng.
     */
    boolean reserveStock(UUID productId, UUID branchId, BigDecimal quantity, UUID referenceId);
}
