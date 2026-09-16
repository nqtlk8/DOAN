package com.storename.erp.inventory.application;

import com.storename.erp.inventory.application.dto.InboundReceiptCreateDto;
import com.storename.erp.inventory.domain.*;
import com.storename.erp.inventory.infrastructure.CostLayerRepository;
import com.storename.erp.inventory.infrastructure.InboundReceiptRepository;
import com.storename.erp.inventory.infrastructure.StockMovementRepository;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InboundReceiptServiceTest {

    @Mock
    private InboundReceiptRepository inboundRepo;
    @Mock
    private StockOnHandRepository stockRepo;
    @Mock
    private StockMovementRepository movementRepo;
    @Mock
    private CostLayerRepository costLayerRepo;

    @InjectMocks
    private InboundReceiptService inboundReceiptService;

    private final Long branchId = 1L;

    @BeforeEach
    void setUp() {
    }

    @Test
    void createDraft_ShouldSaveAndReturnId() {
        InboundReceiptCreateDto dto = new InboundReceiptCreateDto();
        dto.setReceiptCode("NK-TEST");
        InboundReceiptCreateDto.LineDto lineDto = new InboundReceiptCreateDto.LineDto();
        lineDto.setProductId(10L);
        lineDto.setQuantity(new BigDecimal("50.0"));
        lineDto.setUnitCost(new BigDecimal("100.0"));
        lineDto.setUnitOfMeasure("Cai");
        dto.setLines(List.of(lineDto));

        when(inboundRepo.save(any(InboundReceipt.class))).thenAnswer(i -> {
            InboundReceipt receipt = i.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(receipt, "id", UUID.randomUUID());
            return receipt;
        });

        UUID id = inboundReceiptService.createDraft(branchId, dto);

        assertNotNull(id);
        verify(inboundRepo).save(argThat(r -> 
            r.getStatus() == ReceiptStatus.DRAFT &&
            "NK-TEST".equals(r.getReceiptCode())
        ));
    }

    @Test
    void confirmReceipt_ShouldUpdateStockAndCostLayer() {
        UUID receiptId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        InboundReceipt receipt = new InboundReceipt(branchId, "NK-01", null);
        org.springframework.test.util.ReflectionTestUtils.setField(receipt, "id", receiptId);
        
        InboundReceiptLine line = new InboundReceiptLine(10L, new BigDecimal("50.0"), new BigDecimal("100.0"), "Cai");
        org.springframework.test.util.ReflectionTestUtils.setField(line, "id", UUID.randomUUID());
        receipt.addLine(line);

        when(inboundRepo.findById(receiptId)).thenReturn(Optional.of(receipt));
        
        StockOnHand stock = new StockOnHand(10L, branchId);
        when(stockRepo.findByProductIdAndBranchId(10L, branchId)).thenReturn(Optional.of(stock));
        
        when(movementRepo.save(any(StockMovement.class))).thenAnswer(i -> {
            StockMovement m = i.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(m, "id", UUID.randomUUID());
            return m;
        });

        inboundReceiptService.confirmReceipt(receiptId, branchId, userId);

        assertEquals(ReceiptStatus.CONFIRMED, receipt.getStatus());
        assertEquals(0, new BigDecimal("50.0").compareTo(stock.getQuantity()));
        
        verify(stockRepo).save(stock);
        verify(movementRepo).save(any(StockMovement.class));
        verify(costLayerRepo).save(argThat(layer -> 
            layer.getInitialQty().compareTo(new BigDecimal("50.0")) == 0 &&
            layer.getUnitCost().compareTo(new BigDecimal("100.0")) == 0
        ));
        verify(inboundRepo).save(receipt);
    }
}
