package com.storename.erp.order.domain;

/** Nguồn của giá bán đang áp dụng cho (khách, sản phẩm, chi nhánh). */
public enum PriceSource {
    /** Giá riêng của khách (customer_product_price) còn hiệu lực. */
    CUSTOMER,
    /** Giá niêm yết của chi nhánh (price_list). */
    LIST,
    /** Không có giá nào — nhân viên nhập tay. */
    NONE
}
