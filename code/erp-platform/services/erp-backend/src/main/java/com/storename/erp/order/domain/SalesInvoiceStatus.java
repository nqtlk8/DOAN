package com.storename.erp.order.domain;

public enum SalesInvoiceStatus {
    DRAFT,
    CONFIRMED
    // Không có trạng thái hủy: hóa đơn đã xác nhận được "hủy" bằng phiếu trả hàng toàn bộ
    // (hoàn kho theo giá vốn đã snapshot, giảm công nợ) để giữ nguyên dấu vết chứng từ.
}
