package com.storename.erp.procurement.application;

import com.storename.erp.inventory.api.InventoryFacade;
import com.storename.erp.inventory.application.InboundReceiptService;
import com.storename.erp.inventory.application.OutboundReceiptService;
import com.storename.erp.inventory.application.dto.InboundReceiptCreateDto;
import com.storename.erp.inventory.application.dto.OutboundReceiptCreateDto;
import com.storename.erp.procurement.application.dto.StockTransferCreateDto;
import com.storename.erp.procurement.domain.StockTransfer;
import com.storename.erp.procurement.domain.StockTransferLine;
import com.storename.erp.procurement.infrastructure.StockTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockTransferService {

    private final StockTransferRepository transferRepository;
    private final OutboundReceiptService outboundReceiptService;
    private final InboundReceiptService inboundReceiptService;
    private final InventoryFacade inventoryFacade;

    @Transactional
    public UUID requestTransfer(UUID fromBranchId, StockTransferCreateDto dto) {
        if (transferRepository.existsByTransferCode(dto.getTransferCode())) {
            throw new IllegalArgumentException("Transfer code already exists");
        }

        if (fromBranchId.equals(dto.getToBranchId())) {
            throw new IllegalArgumentException("Cannot transfer to the same branch");
        }

        StockTransfer transfer = new StockTransfer();
        transfer.setFromBranchId(fromBranchId);
        transfer.setToBranchId(dto.getToBranchId());
        transfer.setTransferCode(dto.getTransferCode());
        transfer.setNote(dto.getNote());

        for (StockTransferCreateDto.LineDto lineDto : dto.getLines()) {
            StockTransferLine line = new StockTransferLine();
            line.setProductId(lineDto.getProductId());
            line.setQuantity(lineDto.getQuantity());
            line.setUnitOfMeasure(lineDto.getUnitOfMeasure());
            transfer.addLine(line);
        }

        transfer = transferRepository.save(transfer);
        log.info("Requested stock transfer {} from branch {} to branch {}", transfer.getId(), fromBranchId, transfer.getToBranchId());
        return transfer.getId();
    }

    @org.springframework.retry.annotation.Retryable(
        retryFor = org.springframework.orm.ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @org.springframework.retry.annotation.Backoff(delay = 100)
    )
    @Transactional
    public void shipTransfer(UUID transferId, UUID branchId, UUID userId) {
        StockTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found"));

        if (!transfer.getFromBranchId().equals(branchId)) {
            throw new IllegalArgumentException("Not authorized to ship from this branch");
        }

        // Create Outbound Receipt for fromBranch
        OutboundReceiptCreateDto outDto = new OutboundReceiptCreateDto();
        outDto.setReceiptCode("OUT-TRF-" + transfer.getTransferCode());
        outDto.setReason("STOCK_TRANSFER");
        outDto.setNote("Transfer to branch " + transfer.getToBranchId());
        outDto.setLines(transfer.getLines().stream().map(l -> {
            OutboundReceiptCreateDto.LineDto ld = new OutboundReceiptCreateDto.LineDto();
            ld.setProductId(l.getProductId());
            ld.setQuantity(l.getQuantity());
            ld.setUnitOfMeasure(l.getUnitOfMeasure());
            return ld;
        }).collect(Collectors.toList()));

        UUID outboundReceiptId = outboundReceiptService.createDraft(branchId, outDto);
        outboundReceiptService.confirmReceipt(outboundReceiptId, branchId, userId);

        transfer.ship(outboundReceiptId);
        transferRepository.save(transfer);
        log.info("Shipped stock transfer {} via outbound receipt {}", transferId, outboundReceiptId);
    }

    @Transactional
    public void receiveTransfer(UUID transferId, UUID branchId, UUID userId) {
        StockTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found"));

        if (!transfer.getToBranchId().equals(branchId)) {
            throw new IllegalArgumentException("Not authorized to receive at this branch");
        }

        // Create Inbound Receipt for toBranch
        InboundReceiptCreateDto inDto = new InboundReceiptCreateDto();
        inDto.setReceiptCode("IN-TRF-" + transfer.getTransferCode());
        inDto.setNote("Transfer from branch " + transfer.getFromBranchId());
        // For stock transfer, unit cost should ideally be transferred from outbound, but to keep it simple we set it to 0 or retrieve it.
        // We will assume 0 for internal transfers or fetch from average cost.
        inDto.setLines(transfer.getLines().stream().map(l -> {
            InboundReceiptCreateDto.LineDto ld = new InboundReceiptCreateDto.LineDto();
            ld.setProductId(l.getProductId());
            ld.setQuantity(l.getQuantity());
            // Giá mua vào sản phẩm đã ghi nhận (giá vốn trung bình tại chi nhánh xuất)
            java.math.BigDecimal unitCost = inventoryFacade.getAverageCost(l.getProductId(), transfer.getFromBranchId());
            ld.setUnitCost(unitCost);
            ld.setUnitOfMeasure(l.getUnitOfMeasure());
            return ld;
        }).collect(Collectors.toList()));

        UUID inboundReceiptId = inboundReceiptService.createDraft(branchId, inDto);
        inboundReceiptService.confirmReceipt(inboundReceiptId, branchId, userId);

        transfer.receive(inboundReceiptId);
        transferRepository.save(transfer);
        log.info("Received stock transfer {} via inbound receipt {}", transferId, inboundReceiptId);
    }
}
