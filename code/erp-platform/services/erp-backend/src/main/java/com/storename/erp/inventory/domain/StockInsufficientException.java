package com.storename.erp.inventory.domain;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Exception khi tồn kho không đủ để xuất.
 */
@Getter
public class StockInsufficientException extends RuntimeException {

    private final Long productId;
    private final Long branchId;
    private final BigDecimal available;
    private final BigDecimal requested;

    public StockInsufficientException(Long productId, Long branchId, BigDecimal available, BigDecimal requested) {
        super(String.format("Insufficient stock for product %s at branch %s: available=%s, requested=%s",
                productId, branchId, available, requested));
        this.productId = productId;
        this.branchId = branchId;
        this.available = available;
        this.requested = requested;
    }
}
