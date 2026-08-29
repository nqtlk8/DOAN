package com.storename.erp.inventory.application;

import com.storename.erp.inventory.application.dto.InboundReceiptCreateDto;
import com.storename.erp.inventory.domain.*;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboundReceiptService {

    private final InboundReceiptRepository inboundRepo;
    private final StockOnHandRepository stockRepo;
    private final CostingStrategy costingStrategy;
    private final UnitConversionService unitConversionService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UUID createDraft(Long branchId, InboundReceiptCreateDto dto) {
        String code = dto.getReceiptCode();
        if (code == null || code.trim().isEmpty()) {
            code = "NK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } else if (inboundRepo.existsByReceiptCode(code)) {
            throw new IllegalArgumentException("Receipt code already exists");
        }

        InboundReceipt receipt = new InboundReceipt(branchId, code, dto.getNote());
        receipt.setSupplierId(dto.getSupplierId());
        
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
    public void confirmReceipt(UUID receiptId, Long branchId, UUID userId) {
        InboundReceipt receipt = inboundRepo.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found"));

        if (!receipt.getBranchId().equals(branchId)) {
            throw new IllegalArgumentException("Receipt does not belong to this branch");
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
            stockRepo.save(stock); 

            eventPublisher.publishEvent(new StockIncreasedEvent(this, line.getProductId(), branchId, convertedQty, newAvgCost));
        }
        
        inboundRepo.save(receipt);
    }

    @Transactional(readOnly = true)
    public List<InboundReceipt> getReceiptsByBranch(Long branchId) {
        return inboundRepo.findByBranchId(branchId);
    }

    @Transactional(readOnly = true)
    public InboundReceipt getReceipt(UUID id, Long branchId) {
        InboundReceipt receipt = inboundRepo.findById(id).orElseThrow(() -> new RuntimeException("Receipt not found"));
        if (!receipt.getBranchId().equals(branchId)) {
            throw new RuntimeException("Unauthorized");
        }
        return receipt;
    }
}
