package com.storename.erp.order.application;

import com.storename.erp.crm.application.ReceivableDebtService;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import com.storename.erp.inventory.application.InboundReceiptService;
import com.storename.erp.inventory.application.dto.InboundReceiptCreateDto;
import com.storename.erp.order.application.dto.GoodsReturnCreateDto;
import com.storename.erp.order.domain.GoodsReturn;
import com.storename.erp.order.domain.GoodsReturnLine;
import com.storename.erp.order.infrastructure.GoodsReturnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service quÃƒÂ¡Ã‚ÂºÃ‚Â£n lÃƒÆ’Ã‚Â½ khÃƒÆ’Ã‚Â¡ch trÃƒÂ¡Ã‚ÂºÃ‚Â£ hÃƒÆ’Ã‚Â ng (Goods Return).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsReturnService {

    private final GoodsReturnRepository returnRepository;
    private final CustomerRepository customerRepository;
    private final InboundReceiptService inboundReceiptService;
    private final ReceivableDebtService debtService;
    private final com.storename.erp.order.infrastructure.SalesInvoiceRepository invoiceRepository;


    /**
     * TÃƒÂ¡Ã‚ÂºÃ‚Â¡o phiÃƒÂ¡Ã‚ÂºÃ‚Â¿u trÃƒÂ¡Ã‚ÂºÃ‚Â£ hÃƒÆ’Ã‚Â ng nhÃƒÆ’Ã‚Â¡p.
     */
    @Transactional
    public UUID createDraft(Long branchId, GoodsReturnCreateDto dto) {
        String code = dto.getReturnCode(); if (code == null || code.trim().isEmpty()) { code = "TH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(); } else if (returnRepository.existsByReturnCode(code)) {
            throw new IllegalArgumentException("Return code already exists");
        }

        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        GoodsReturn goodsReturn = new GoodsReturn();
        goodsReturn.setBranchId(branchId);
        goodsReturn.setCustomer(customer);
        goodsReturn.setReturnCode(code);
        goodsReturn.setInvoiceId(dto.getInvoiceId());
        goodsReturn.setReason(dto.getReason());
        goodsReturn.setNote(dto.getNote());

        for (GoodsReturnCreateDto.LineDto lineDto : dto.getLines()) {
            GoodsReturnLine line = new GoodsReturnLine();
            line.setProductId(lineDto.getProductId());
            line.setQuantity(lineDto.getQuantity());
            line.setUnitPrice(lineDto.getUnitPrice());
            line.setUnitOfMeasure(lineDto.getUnitOfMeasure());
            goodsReturn.addLine(line);
        }

        goodsReturn.calculateTotal();
        goodsReturn = returnRepository.save(goodsReturn);
        log.info("Created DRAFT Goods Return {} for branch {}", goodsReturn.getId(), branchId);
        return goodsReturn.getId();
    }

    /**
     * XÃƒÆ’Ã‚Â¡c nhÃƒÂ¡Ã‚ÂºÃ‚Â­n phiÃƒÂ¡Ã‚ÂºÃ‚Â¿u trÃƒÂ¡Ã‚ÂºÃ‚Â£ hÃƒÆ’Ã‚Â ng: cÃƒÂ¡Ã‚Â»Ã¢â€žÂ¢ng lÃƒÂ¡Ã‚ÂºÃ‚Â¡i kho (InboundReceipt) vÃƒÆ’Ã‚Â  trÃƒÂ¡Ã‚Â»Ã‚Â« cÃƒÆ’Ã‚Â´ng nÃƒÂ¡Ã‚Â»Ã‚Â£ (nÃƒÂ¡Ã‚ÂºÃ‚Â¿u cÃƒÆ’Ã‚Â³ hoÃƒÆ’Ã‚Â¡ Ãƒâ€žÃ¢â‚¬ËœÃƒâ€ Ã‚Â¡n gÃƒÂ¡Ã‚Â»Ã¢â‚¬Ëœc).
     */
    @org.springframework.retry.annotation.Retryable(
        retryFor = org.springframework.orm.ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @org.springframework.retry.annotation.Backoff(delay = 100)
    )
    @Transactional
    public void confirmReturn(UUID returnId, Long branchId, UUID userId) {
        GoodsReturn goodsReturn = returnRepository.findById(returnId)
                .orElseThrow(() -> new IllegalArgumentException("Return not found"));

        if (!goodsReturn.getBranchId().equals(branchId)) {
            throw new IllegalArgumentException("Return does not belong to this branch");
        }

        goodsReturn.confirm(userId);

        // 1. NhÃƒÂ¡Ã‚ÂºÃ‚Â­p kho trÃƒÂ¡Ã‚ÂºÃ‚Â£ hÃƒÆ’Ã‚Â ng
        InboundReceiptCreateDto inDto = new InboundReceiptCreateDto();
        inDto.setReceiptCode("IN-RET-" + goodsReturn.getReturnCode());
        inDto.setNote("NhÃƒÂ¡Ã‚ÂºÃ‚Â­p kho tÃƒÂ¡Ã‚Â»Ã‚Â« phiÃƒÂ¡Ã‚ÂºÃ‚Â¿u trÃƒÂ¡Ã‚ÂºÃ‚Â£ hÃƒÆ’Ã‚Â ng " + goodsReturn.getReturnCode());
        
        inDto.setLines(goodsReturn.getLines().stream().map(l -> {
            InboundReceiptCreateDto.LineDto ld = new InboundReceiptCreateDto.LineDto();
            ld.setProductId(l.getProductId());
            ld.setQuantity(l.getQuantity());
            // GiÃƒÆ’Ã‚Â¡ vÃƒÂ¡Ã‚Â»Ã¢â‚¬Ëœn nhÃƒÂ¡Ã‚ÂºÃ‚Â­p kho hoÃƒÆ’Ã‚Â n trÃƒÂ¡Ã‚ÂºÃ‚Â£ tÃƒÂ¡Ã‚ÂºÃ‚Â¡m lÃƒÂ¡Ã‚ÂºÃ‚Â¥y theo Ãƒâ€žÃ¢â‚¬ËœÃƒâ€ Ã‚Â¡n giÃƒÆ’Ã‚Â¡ trÃƒÂ¡Ã‚ÂºÃ‚Â£, hoÃƒÂ¡Ã‚ÂºÃ‚Â·c lÃƒÂ¡Ã‚ÂºÃ‚Â¥y tÃƒÂ¡Ã‚Â»Ã‚Â« lÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¹ch sÃƒÂ¡Ã‚Â»Ã‚Â­ (ÃƒÂ¡Ã‚Â»Ã…Â¸ Ãƒâ€žÃ¢â‚¬ËœÃƒÆ’Ã‚Â¢y lÃƒÂ¡Ã‚ÂºÃ‚Â¥y unitPrice cÃƒÂ¡Ã‚Â»Ã‚Â§a hÃƒÆ’Ã‚Â ng trÃƒÂ¡Ã‚ÂºÃ‚Â£)
            ld.setUnitCost(l.getUnitPrice());
            ld.setUnitOfMeasure(l.getUnitOfMeasure());
            return ld;
        }).collect(Collectors.toList()));

        UUID receiptId = inboundReceiptService.createDraft(branchId, inDto);
        inboundReceiptService.confirmReceipt(receiptId, branchId, userId);

        // 2. TrÃƒÂ¡Ã‚Â»Ã‚Â« cÃƒÆ’Ã‚Â´ng nÃƒÂ¡Ã‚Â»Ã‚Â£ (ChÃƒÂ¡Ã‚Â»Ã¢â‚¬Â° trÃƒÂ¡Ã‚Â»Ã‚Â« nÃƒÂ¡Ã‚ÂºÃ‚Â¿u trÃƒÂ¡Ã‚ÂºÃ‚Â£ hÃƒÆ’Ã‚Â ng dÃƒÂ¡Ã‚Â»Ã‚Â±a trÃƒÆ’Ã‚Âªn hoÃƒÆ’Ã‚Â¡ Ãƒâ€žÃ¢â‚¬ËœÃƒâ€ Ã‚Â¡n gÃƒÂ¡Ã‚Â»Ã¢â‚¬Ëœc)
        if (goodsReturn.getInvoiceId() != null) {
            com.storename.erp.order.domain.SalesInvoice invoice = invoiceRepository.findById(goodsReturn.getInvoiceId())
                    .orElseThrow(() -> new IllegalArgumentException("Original invoice not found"));
            
            if (!invoice.getBranchId().equals(branchId) || !invoice.getCustomerId().equals(goodsReturn.getCustomer().getId())) {
                throw new IllegalArgumentException("Invoice does not belong to this branch or customer");
            }
            if (invoice.getStatus() != com.storename.erp.order.domain.SalesInvoiceStatus.CONFIRMED) {
                throw new IllegalArgumentException("Cannot return goods for unconfirmed invoice");
            }
            
            for (GoodsReturnLine returnLine : goodsReturn.getLines()) {
                com.storename.erp.order.domain.SalesInvoiceLine invoiceLine = invoice.getLines().stream()
                        .filter(l -> l.getProductId().equals(returnLine.getProductId()) && l.getUnitOfMeasure().equals(returnLine.getUnitOfMeasure()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Product not found in original invoice"));
                
                // LÃƒÂ¡Ã‚ÂºÃ‚Â¥y tÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¢ng sÃƒÂ¡Ã‚Â»Ã¢â‚¬Ëœ lÃƒâ€ Ã‚Â°ÃƒÂ¡Ã‚Â»Ã‚Â£ng Ãƒâ€žÃ¢â‚¬ËœÃƒÆ’Ã‚Â£ trÃƒÂ¡Ã‚ÂºÃ‚Â£ trÃƒâ€ Ã‚Â°ÃƒÂ¡Ã‚Â»Ã¢â‚¬Âºc Ãƒâ€žÃ¢â‚¬ËœÃƒÆ’Ã‚Â³ cÃƒÂ¡Ã‚Â»Ã‚Â§a SP nÃƒÆ’Ã‚Â y trong HoÃƒÆ’Ã‚Â¡ Ãƒâ€žÃ¢â‚¬ËœÃƒâ€ Ã‚Â¡n
                java.math.BigDecimal previousReturned = returnRepository.getTotalReturnedQuantity(invoice.getId(), returnLine.getProductId());
                
                if (previousReturned == null) previousReturned = java.math.BigDecimal.ZERO;
                
                if (previousReturned.add(returnLine.getQuantity()).compareTo(invoiceLine.getQuantity()) > 0) {
                    throw new IllegalArgumentException("Total returned quantity exceeds invoice sold quantity");
                }
            }

            debtService.decreaseDebt(goodsReturn.getCustomer().getId(), branchId, goodsReturn.getTotalAmount());
            log.info("Decreased debt for customer {} by {} due to goods return", goodsReturn.getCustomer().getId(), goodsReturn.getTotalAmount());
        }

        returnRepository.save(goodsReturn);
        log.info("Confirmed Goods Return {}", returnId);
    }
    @Transactional(readOnly = true)
    public java.util.List<com.storename.erp.order.domain.GoodsReturn> getReturnsByBranch(Long branchId) {
        return returnRepository.findByBranchId(branchId);
    }

    @Transactional(readOnly = true)
    public com.storename.erp.order.domain.GoodsReturn getReturn(java.util.UUID id, Long branchId) {
        com.storename.erp.order.domain.GoodsReturn goodsReturn = returnRepository.findById(id).orElseThrow(() -> new RuntimeException("Return not found"));
        if (!goodsReturn.getBranchId().equals(branchId)) {
            throw new RuntimeException("Unauthorized");
        }
        return goodsReturn;
    }
}

