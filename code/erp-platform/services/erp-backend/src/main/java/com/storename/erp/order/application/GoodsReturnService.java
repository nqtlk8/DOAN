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
 * Service quÃ¡ÂºÂ£n lÃƒÂ½ khÃƒÂ¡ch trÃ¡ÂºÂ£ hÃƒÂ ng (Goods Return).
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
     * TÃ¡ÂºÂ¡o phiÃ¡ÂºÂ¿u trÃ¡ÂºÂ£ hÃƒÂ ng nhÃƒÂ¡p.
     */
    @Transactional
    public UUID createDraft(Long branchId, GoodsReturnCreateDto dto) {
        if (returnRepository.existsByReturnCode(dto.getReturnCode())) {
            throw new IllegalArgumentException("Return code already exists");
        }

        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        GoodsReturn goodsReturn = new GoodsReturn();
        goodsReturn.setBranchId(branchId);
        goodsReturn.setCustomer(customer);
        goodsReturn.setReturnCode(dto.getReturnCode());
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
     * XÃƒÂ¡c nhÃ¡ÂºÂ­n phiÃ¡ÂºÂ¿u trÃ¡ÂºÂ£ hÃƒÂ ng: cÃ¡Â»â„¢ng lÃ¡ÂºÂ¡i kho (InboundReceipt) vÃƒÂ  trÃ¡Â»Â« cÃƒÂ´ng nÃ¡Â»Â£ (nÃ¡ÂºÂ¿u cÃƒÂ³ hoÃƒÂ¡ Ã„â€˜Ã†Â¡n gÃ¡Â»â€˜c).
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

        // 1. NhÃ¡ÂºÂ­p kho trÃ¡ÂºÂ£ hÃƒÂ ng
        InboundReceiptCreateDto inDto = new InboundReceiptCreateDto();
        inDto.setReceiptCode("IN-RET-" + goodsReturn.getReturnCode());
        inDto.setNote("NhÃ¡ÂºÂ­p kho tÃ¡Â»Â« phiÃ¡ÂºÂ¿u trÃ¡ÂºÂ£ hÃƒÂ ng " + goodsReturn.getReturnCode());
        
        inDto.setLines(goodsReturn.getLines().stream().map(l -> {
            InboundReceiptCreateDto.LineDto ld = new InboundReceiptCreateDto.LineDto();
            ld.setProductId(l.getProductId());
            ld.setQuantity(l.getQuantity());
            // GiÃƒÂ¡ vÃ¡Â»â€˜n nhÃ¡ÂºÂ­p kho hoÃƒÂ n trÃ¡ÂºÂ£ tÃ¡ÂºÂ¡m lÃ¡ÂºÂ¥y theo Ã„â€˜Ã†Â¡n giÃƒÂ¡ trÃ¡ÂºÂ£, hoÃ¡ÂºÂ·c lÃ¡ÂºÂ¥y tÃ¡Â»Â« lÃ¡Â»â€¹ch sÃ¡Â»Â­ (Ã¡Â»Å¸ Ã„â€˜ÃƒÂ¢y lÃ¡ÂºÂ¥y unitPrice cÃ¡Â»Â§a hÃƒÂ ng trÃ¡ÂºÂ£)
            ld.setUnitCost(l.getUnitPrice());
            ld.setUnitOfMeasure(l.getUnitOfMeasure());
            return ld;
        }).collect(Collectors.toList()));

        UUID receiptId = inboundReceiptService.createDraft(branchId, inDto);
        inboundReceiptService.confirmReceipt(receiptId, branchId, userId);

        // 2. TrÃ¡Â»Â« cÃƒÂ´ng nÃ¡Â»Â£ (ChÃ¡Â»â€° trÃ¡Â»Â« nÃ¡ÂºÂ¿u trÃ¡ÂºÂ£ hÃƒÂ ng dÃ¡Â»Â±a trÃƒÂªn hoÃƒÂ¡ Ã„â€˜Ã†Â¡n gÃ¡Â»â€˜c)
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
                
                // LÃ¡ÂºÂ¥y tÃ¡Â»â€¢ng sÃ¡Â»â€˜ lÃ†Â°Ã¡Â»Â£ng Ã„â€˜ÃƒÂ£ trÃ¡ÂºÂ£ trÃ†Â°Ã¡Â»â€ºc Ã„â€˜ÃƒÂ³ cÃ¡Â»Â§a SP nÃƒÂ y trong HoÃƒÂ¡ Ã„â€˜Ã†Â¡n
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

