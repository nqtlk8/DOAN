package com.storename.erp.order.application;

import com.storename.erp.crm.api.CrmFacade;
import com.storename.erp.crm.application.ReceivableDebtService;
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
    private final ReceivableDebtService debtService;
    private final SalesInvoiceRepository invoiceRepository;
    private final InventoryFacade inventoryFacade;
    private final CrmFacade crmFacade;

    /**
     * Tạo phiếu trả hàng nhập.
     */
    @Transactional
    public UUID createDraft(Long branchId, GoodsReturnCreateDto dto) {
        String code = dto.getReturnCode(); 
        if (dto.getReturnCode() == null || dto.getReturnCode().trim().isEmpty()) { 
            code = "TH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(); 
        } else if (returnRepository.existsByReturnCode(code)) {
            throw new IllegalArgumentException("Return code already exists");
        }

        if (!crmFacade.customerExists(dto.getCustomerId())) {
            throw new IllegalArgumentException("Customer not found");
        }

        GoodsReturn goodsReturn = new GoodsReturn();
        goodsReturn.setBranchId(branchId);
        goodsReturn.setCustomerId(dto.getCustomerId());
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
            
            if (!invoice.getBranchId().equals(branchId) || !invoice.getCustomerId().equals(goodsReturn.getCustomerId())) {
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

        if (goodsReturn.getCustomerId() != null) {
            debtService.decreaseDebt(goodsReturn.getCustomerId(), branchId, goodsReturn.getTotalAmount(),
                    com.storename.erp.crm.domain.ReceivableDebtMovementType.RETURN,
                    "GOODS_RETURN", goodsReturn.getId().toString(), userId, "Khách trả hàng " + goodsReturn.getReturnCode());
            log.info("Decreased debt for customer {} by {} due to goods return", goodsReturn.getCustomerId(), goodsReturn.getTotalAmount());
        }

        returnRepository.save(goodsReturn);
        log.info("Confirmed Goods Return {}", returnId);
    }

    @Transactional(readOnly = true)
    public java.util.List<com.storename.erp.order.api.dto.GoodsReturnResponseDto> getReturnsByBranch(Long branchId) {
        java.util.List<com.storename.erp.order.domain.GoodsReturn> returns = returnRepository.findByBranchId(branchId);
        
        // Optimize N+1 for Customer
        java.util.List<java.util.UUID> customerIds = returns.stream()
                .map(com.storename.erp.order.domain.GoodsReturn::getCustomerId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
                
        java.util.Map<java.util.UUID, String> customerNameMap = crmFacade.getCustomerNames(customerIds);

        return returns.stream()
                .map(r -> com.storename.erp.order.api.dto.GoodsReturnResponseDto.fromEntity(r, customerNameMap.get(r.getCustomerId())))
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public com.storename.erp.order.api.dto.GoodsReturnResponseDto getReturnDto(java.util.UUID id, Long branchId) {
        com.storename.erp.order.domain.GoodsReturn goodsReturn = returnRepository.findById(id).orElseThrow(() -> new RuntimeException("Return not found"));
        if (branchId != null && !goodsReturn.getBranchId().equals(branchId)) {
            throw new RuntimeException("Unauthorized");
        }
        String customerName = null;
        if (goodsReturn.getCustomerId() != null) {
            customerName = crmFacade.getCustomerNames(java.util.List.of(goodsReturn.getCustomerId()))
                    .get(goodsReturn.getCustomerId());
        }
                
        return com.storename.erp.order.api.dto.GoodsReturnResponseDto.fromEntity(goodsReturn, customerName);
    }
}

