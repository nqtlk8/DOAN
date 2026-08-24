package com.storename.erp.inventory.application;

import com.storename.erp.inventory.application.dto.InboundReceiptCreateDto;
import com.storename.erp.inventory.domain.CostingStrategy;
import com.storename.erp.inventory.domain.InboundReceipt;
import com.storename.erp.inventory.domain.InboundReceiptLine;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.domain.event.StockIncreasedEvent;
import com.storename.erp.inventory.infrastructure.InboundReceiptRepository;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboundReceiptService {

    private final InboundReceiptRepository inboundRepo;
    private final StockOnHandRepository stockRepo;
    private final CostingStrategy costingStrategy;
    private final UnitConversionService unitConversionService;
    private final ApplicationEventPublisher eventPublisher;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Transactional
    public UUID createDraft(UUID branchId, InboundReceiptCreateDto dto) {
        if (inboundRepo.existsByReceiptCode(dto.getReceiptCode())) {
            throw new IllegalArgumentException("Receipt code already exists");
        }

        InboundReceipt receipt = new InboundReceipt(branchId, dto.getReceiptCode(), dto.getNote());
        if (dto.getPurchaseOrderId() != null) {
            receipt.setPurchaseOrderId(dto.getPurchaseOrderId());
        }
        for (InboundReceiptCreateDto.LineDto lineDto : dto.getLines()) {
            InboundReceiptLine line = new InboundReceiptLine(
                    lineDto.getProductId(),
                    lineDto.getQuantity(),
                    lineDto.getUnitCost(),
                    lineDto.getUnitOfMeasure()
            );
            receipt.addLine(line);
        }

        receipt = inboundRepo.save(receipt);
        log.info("Created DRAFT inbound receipt {} for branch {}", receipt.getId(), branchId);
        return receipt.getId();
    }

    @Transactional
    public void confirmReceipt(UUID receiptId, UUID branchId, UUID userId) {
        InboundReceipt receipt = inboundRepo.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found"));

        if (!receipt.getBranchId().equals(branchId)) {
            throw new IllegalArgumentException("Receipt does not belong to this branch");
        }
        
        if (receipt.getPurchaseOrderId() != null) {
            UUID poBranchId = jdbcTemplate.queryForObject(
                "SELECT branch_id FROM supplier_purchase_order WHERE id = ?", 
                UUID.class, receipt.getPurchaseOrderId());
            if (poBranchId == null || !poBranchId.equals(branchId)) {
                throw new IllegalArgumentException("Purchase Order branch does not match receipt branch");
            }
        }

        receipt.confirm(userId);

        for (InboundReceiptLine line : receipt.getLines()) {
            BigDecimal convertedQty = unitConversionService.convert(line.getQuantity(), line.getUnitOfMeasure(), "BASE_UNIT");
            
            StockOnHand stock = stockRepo.findByProductIdAndBranchId(line.getProductId(), branchId)
                    .orElseGet(() -> stockRepo.save(new StockOnHand(line.getProductId(), branchId)));

            BigDecimal newAvgCost = costingStrategy.calculate(
                    stock.getQuantity(), stock.getAvgCost(),
                    convertedQty, line.getUnitCost()
            );

            stock.increase(convertedQty, "INBOUND_RECEIPT");
            stock.updateAvgCost(newAvgCost);
            stockRepo.save(stock); // Trigger optimistic locking update

            eventPublisher.publishEvent(new StockIncreasedEvent(this, line.getProductId(), branchId, convertedQty, newAvgCost));
        }
        
        inboundRepo.save(receipt);
        
        if (receipt.getPurchaseOrderId() != null) {
            eventPublisher.publishEvent(new com.storename.erp.inventory.domain.event.InboundReceiptConfirmedEvent(
                    this, receipt.getId(), receipt.getPurchaseOrderId(), branchId));
        }
        
        log.info("Confirmed inbound receipt {} for branch {}", receipt.getId(), branchId);
    }
}
