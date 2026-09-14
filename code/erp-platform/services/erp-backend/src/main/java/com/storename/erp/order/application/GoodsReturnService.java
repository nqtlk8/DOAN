package com.storename.erp.order.application;

import com.storename.erp.crm.application.ReceivableDebtService;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import com.storename.erp.inventory.api.InventoryFacade;
import com.storename.erp.order.application.dto.GoodsReturnCreateDto;
import com.storename.erp.order.domain.GoodsReturn;
import com.storename.erp.order.domain.GoodsReturnLine;
import com.storename.erp.order.infrastructure.GoodsReturnRepository;
import com.storename.erp.order.infrastructure.SalesInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service quản lý khách trả hàng (Goods Return).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsReturnService {

    private final GoodsReturnRepository returnRepository;
    private final CustomerRepository customerRepository;
    private final ReceivableDebtService debtService;
    private final SalesInvoiceRepository invoiceRepository;
    private final InventoryFacade inventoryFacade;

    /**
     * Tạo phiếu trả hàng nhập.
     */
    @Transactional
    public UUID createDraft(Long branchId, GoodsReturnCreateDto dto) {
        String code = dto.getReturnCode(); 
        if (code == null || code.trim().isEmpty()) { 
            code = "TH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(); 
        } else if (returnRepository.existsByReturnCode(code)) {
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
     * Xác nhận phiếu trả hàng: cập nhật tồn kho, cost layer và trừ công nợ.
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

        for (GoodsReturnLine line : goodsReturn.getLines()) {
            inventoryFacade.recordReturn(line.getProductId(), branchId, line.getQuantity(), line.getUnitPrice(), goodsReturn.getId().toString(), line.getId(), userId);
        }

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
                
                java.math.BigDecimal previousReturned = returnRepository.getTotalReturnedQuantity(invoice.getId(), returnLine.getProductId());
                
                if (previousReturned == null) previousReturned = java.math.BigDecimal.ZERO;
                
                if (previousReturned.add(returnLine.getQuantity()).compareTo(invoiceLine.getQuantity()) > 0) {
                    throw new IllegalArgumentException("Total returned quantity exceeds invoice sold quantity");
                }
            }
        }

        if (goodsReturn.getCustomer() != null) {
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

