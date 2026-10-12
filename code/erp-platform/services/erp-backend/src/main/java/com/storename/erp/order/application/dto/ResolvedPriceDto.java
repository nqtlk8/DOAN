package com.storename.erp.order.application.dto;

import com.storename.erp.order.domain.PriceSource;

import java.math.BigDecimal;

/**
 * Giá bán đang áp dụng cho một sản phẩm khi bán cho một khách tại chi nhánh hiện tại.
 *
 * @param productId     sản phẩm
 * @param unitPrice     giá áp dụng (theo thứ tự ưu tiên CUSTOMER > LIST); null khi source = NONE
 * @param source        nguồn của unitPrice
 * @param listPrice     giá niêm yết chi nhánh (null nếu chưa có) — để hiển thị tham khảo
 * @param customerPrice giá riêng của khách còn hiệu lực (null nếu không có)
 */
public record ResolvedPriceDto(
        Long productId,
        BigDecimal unitPrice,
        PriceSource source,
        BigDecimal listPrice,
        BigDecimal customerPrice
) {}
