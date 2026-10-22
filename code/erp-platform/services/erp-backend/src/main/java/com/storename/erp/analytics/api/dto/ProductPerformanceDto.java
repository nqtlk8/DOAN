package com.storename.erp.analytics.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPerformanceDto {
    private Long productId;
    private String productName;
    private BigDecimal quantitySold;
    private BigDecimal revenue;
    /** Giá vốn thuần = SUM(quantity × unit_cost) bán − phần hàng trả (unit_cost null tính 0). */
    private BigDecimal cogs;
    /** Lợi nhuận gộp = revenue − cogs. */
    private BigDecimal grossProfit;
    /** true nếu có dòng bán/trả thiếu unit_cost: giá vốn bị thấp, lợi nhuận bị cao hơn thực tế. */
    private boolean costIncomplete;

    /**
     * Giữ constructor 4 tham số: FactSalesRepository dùng JPQL {@code SELECT new ProductPerformanceDto(productId, name,
     * qty, revenue)} và Hibernate kiểm tra câu truy vấn này lúc khởi động (thiếu constructor -> app không chạy).
     */
    public ProductPerformanceDto(Long productId, String productName, BigDecimal quantitySold, BigDecimal revenue) {
        this.productId = productId;
        this.productName = productName;
        this.quantitySold = quantitySold;
        this.revenue = revenue;
    }
}
