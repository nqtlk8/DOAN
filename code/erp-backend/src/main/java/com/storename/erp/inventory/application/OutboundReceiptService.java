package com.storename.erp.inventory.application;

import com.storename.erp.inventory.application.dto.OutboundReceiptCreateDto;
import com.storename.erp.inventory.domain.OutboundReceipt;
import com.storename.erp.inventory.domain.OutboundReceiptLine;
import com.storename.erp.inventory.domain.StockInsufficientException;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.domain.event.StockDecreasedEvent;
import com.storename.erp.inventory.infrastructure.OutboundReceiptRepository;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundReceiptService {

    private final OutboundReceiptRepository outboundRepo;
    private final StockOnHandRepository stockRepo;
    private final UnitConversionService unitConversionService;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    @Transactional
    public UUID createDraft(UUID branchId, OutboundReceiptCreateDto dto) {
        if (outboundRepo.existsByReceiptCode(dto.getReceiptCode())) {
            throw new IllegalArgumentException("Receipt code already exists");
        }

        OutboundReceipt receipt = new OutboundReceipt(branchId, dto.getReceiptCode(), dto.getReason(), dto.getNote());
        for (OutboundReceiptCreateDto.LineDto lineDto : dto.getLines()) {
            OutboundReceiptLine line = new OutboundReceiptLine(
                    lineDto.getProductId(),
                    lineDto.getQuantity(),
                    lineDto.getUnitOfMeasure()
            );
            receipt.addLine(line);
        }

        receipt = outboundRepo.save(receipt);
        return receipt.getId();
    }

    public void confirmReceipt(UUID receiptId, UUID branchId, UUID userId) {
        OutboundReceipt receipt = outboundRepo.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found"));

        if (!receipt.getBranchId().equals(branchId)) {
            throw new IllegalArgumentException("Receipt does not belong to this branch");
        }

        receipt.confirm(userId);

        for (OutboundReceiptLine line : receipt.getLines()) {
            BigDecimal convertedQty = unitConversionService.convert(line.getQuantity(), line.getUnitOfMeasure(), "BASE_UNIT");
            
            StockOnHand stock = stockRepo.findByProductIdAndBranchId(line.getProductId(), branchId)
                    .orElseGet(() -> new StockOnHand(line.getProductId(), branchId));

            if ("SALES_INVOICE".equals(receipt.getReason())) {
                stock.decreaseAllowNegative(convertedQty, receipt.getReason());
            } else {
                stock.decrease(convertedQty, receipt.getReason());
            }
            stockRepo.save(stock); // Trigger optimistic locking update

            eventPublisher.publishEvent(new StockDecreasedEvent(this, line.getProductId(), branchId, convertedQty, stock.getAvgCost()));
        }
        
        outboundRepo.save(receipt);
    }
}
