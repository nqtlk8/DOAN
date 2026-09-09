package com.storename.erp.inventory.domain;

/**
 * Enum đại diện cho các loại giao dịch phát sinh làm thay đổi số lượng tồn kho.
 * Được sử dụng trong StockMovement.
 */
public enum MovementType {
    /** Nhập kho từ nhà cung cấp */
    INBOUND, 
    
    /** Xuất kho bán hàng */
    SALE, 
    
    /** Khách hàng trả lại hàng hóa */
    RETURN, 
    
    /** Điều chỉnh tồn kho thủ công (kiểm kê) */
    ADJUSTMENT
}
