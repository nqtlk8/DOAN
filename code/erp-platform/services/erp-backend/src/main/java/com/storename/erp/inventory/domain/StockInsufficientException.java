package com.storename.erp.inventory.domain;

import com.storename.erp.common.exception.BusinessConflictException;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Không đủ tồn kho để xuất bán. Hệ thống không cho tồn âm: phải nhập hàng trước khi bán.
 * Được GlobalExceptionHandler trả về HTTP 409 với mã {@value #CODE}.
 */
@Getter
public class StockInsufficientException extends BusinessConflictException {

    public static final String CODE = "INSUFFICIENT_STOCK";

    private final Long productId;
    private final Long branchId;
    private final BigDecimal available;
    private final BigDecimal requested;

    public StockInsufficientException(Long productId, Long branchId, BigDecimal available, BigDecimal requested) {
        this("sản phẩm #" + productId, productId, branchId, available, requested);
    }

    /** @param productLabel tên hiển thị của sản phẩm, vd "[PRD-1] Bồn cầu TOTO" */
    public StockInsufficientException(String productLabel, Long productId, Long branchId,
                                      BigDecimal available, BigDecimal requested) {
        super(CODE, String.format("Không đủ hàng: %s — tồn kho còn %s, cần %s. Vui lòng nhập hàng trước khi bán.",
                productLabel, plain(available), plain(requested)));
        this.productId = productId;
        this.branchId = branchId;
        this.available = available;
        this.requested = requested;
    }

    private static String plain(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }
}
