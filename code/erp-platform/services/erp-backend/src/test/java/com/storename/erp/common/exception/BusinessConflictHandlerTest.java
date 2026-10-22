package com.storename.erp.common.exception;

import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.inventory.domain.StockInsufficientException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Không đủ hàng khi bán -> 409 + mã INSUFFICIENT_STOCK + message tiếng Việt hiển thị thẳng cho người dùng. */
class BusinessConflictHandlerTest {

    @Test
    void insufficientStock_mapsTo409WithCodeAndMessage() {
        StockInsufficientException ex = new StockInsufficientException("[PRD-1] Bồn cầu", 1L, 1L,
                new BigDecimal("3.0000"), new BigDecimal("5"));

        ResponseEntity<ApiResponse<Void>> res = new GlobalExceptionHandler().handleBusinessConflictException(ex);

        assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals("Không đủ hàng: [PRD-1] Bồn cầu — tồn kho còn 3, cần 5. Vui lòng nhập hàng trước khi bán.",
                res.getBody().getMessage());
        assertEquals(List.of("INSUFFICIENT_STOCK"), res.getBody().getErrors());
    }
}
