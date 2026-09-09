package com.storename.erp.inventory.unit;

import com.storename.erp.inventory.domain.MovementType;
import com.storename.erp.inventory.domain.StockMovement;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

public class StockMovementTest {

    @Test
    public void inbound_positiveQuantity() {
        String receiptId = UUID.randomUUID().toString();
        UUID lineId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        StockMovement movement = StockMovement.inbound(1L, 1L, new BigDecimal("50"), receiptId, lineId, userId);
        
        assertThat(movement.getMovementType()).isEqualTo(MovementType.INBOUND);
        assertThat(movement.getQuantity()).isEqualByComparingTo("50");
        assertThat(movement.getRefType()).isEqualTo("inbound_receipt");
        assertThat(movement.getRefId()).isEqualTo(receiptId);
        assertThat(movement.getRefLineId()).isEqualTo(lineId);
        assertThat(movement.getPerformedBy()).isEqualTo(userId);
    }

    @Test
    public void sale_negativeQuantity() {
        String invoiceId = UUID.randomUUID().toString();
        UUID lineId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        StockMovement movement = StockMovement.sale(1L, 1L, new BigDecimal("30"), invoiceId, lineId, userId);
        
        assertThat(movement.getMovementType()).isEqualTo(MovementType.SALE);
        assertThat(movement.getQuantity()).isEqualByComparingTo("-30");
        assertThat(movement.getRefType()).isEqualTo("sales_invoice");
        assertThat(movement.getRefId()).isEqualTo(invoiceId);
        assertThat(movement.getRefLineId()).isEqualTo(lineId);
        assertThat(movement.getPerformedBy()).isEqualTo(userId);
    }

    @Test
    public void returnGoods_positiveQuantity() {
        String returnId = UUID.randomUUID().toString();
        UUID lineId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        StockMovement movement = StockMovement.returnGoods(1L, 1L, new BigDecimal("10"), returnId, lineId, userId);
        
        assertThat(movement.getMovementType()).isEqualTo(MovementType.RETURN);
        assertThat(movement.getQuantity()).isEqualByComparingTo("10");
        assertThat(movement.getRefType()).isEqualTo("goods_return");
        assertThat(movement.getRefId()).isEqualTo(returnId);
        assertThat(movement.getRefLineId()).isEqualTo(lineId);
        assertThat(movement.getPerformedBy()).isEqualTo(userId);
    }
}
