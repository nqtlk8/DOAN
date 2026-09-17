package com.storename.erp.order.application;

import com.storename.erp.crm.application.ReceivableDebtService;
import com.storename.erp.order.application.dto.SalesInvoiceCreateDto;
import com.storename.erp.order.domain.SalesInvoice;
import com.storename.erp.order.domain.SalesInvoiceLine;
import com.storename.erp.order.domain.SalesInvoiceStatus;
import com.storename.erp.order.infrastructure.SalesInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

import com.storename.erp.inventory.api.InventoryFacade;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesInvoiceService {
    private final SalesInvoiceRepository invoiceRepository;
    private final ReceivableDebtService debtService;
    private final InventoryFacade inventoryFacade;
    private final com.storename.erp.crm.infrastructure.CustomerRepository customerRepository;

    @Transactional
    public SalesInvoice createDraft(SalesInvoiceCreateDto dto, Long branchId) {
        SalesInvoice invoice = new SalesInvoice();
        invoice.setBranchId(branchId);
        invoice.setCustomerId(dto.getCustomerId());
        String code = dto.getInvoiceCode();
        if (code == null || code.trim().isEmpty()) {
            code = "HD" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        invoice.setInvoiceCode(code);
        invoice.setPaymentMethod(dto.getPaymentMethod());
        invoice.setNote(dto.getNote());
        if (dto.getAdvancePayment() != null) {
            invoice.setAdvancePayment(dto.getAdvancePayment());
        }
        
        for (var lineDto : dto.getLines()) {
            SalesInvoiceLine line = new SalesInvoiceLine();
            line.setProductId(lineDto.getProductId());
            line.setProductName(lineDto.getProductName());
            line.setQuantity(lineDto.getQuantity());
            line.setUnitPrice(lineDto.getUnitPrice());
            line.setUnitOfMeasure(lineDto.getUnitOfMeasure());
            line.setLineTotal(line.getQuantity().multiply(line.getUnitPrice()));
            invoice.addLine(line);
        }
        
        invoice.calculateTotal();
        return invoiceRepository.save(invoice);
    }

    @org.springframework.retry.annotation.Retryable(
        retryFor = {
            org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            org.springframework.dao.DataIntegrityViolationException.class
        },
        maxAttempts = 3,
        backoff = @org.springframework.retry.annotation.Backoff(delay = 100)
    )
    @Transactional
    public SalesInvoice confirmInvoice(UUID invoiceId, UUID userId, Long branchId) {
        SalesInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        if (!invoice.getBranchId().equals(branchId)) {
            throw new SecurityException("Cannot confirm invoice of different branch");
        }

        if (invoice.getStatus() != SalesInvoiceStatus.DRAFT) {
            throw new IllegalStateException("Invoice is not DRAFT");
        }

        for (SalesInvoiceLine line : invoice.getLines()) {
            InventoryFacade.SaleCostResult result = inventoryFacade.recordSaleAndGetCost(
                    line.getProductId(), branchId, line.getQuantity(), invoice.getId().toString(), line.getId(), null);
            
            line.setUnitCost(result.unitCostSnapshot());
            line.setCostBasis(result.costBasis());
        }

        BigDecimal currentDebt = debtService.getCurrentDebt(invoice.getCustomerId(), branchId);
        
        // 1. Ghi nhận công nợ từ đơn bán
        debtService.increaseDebt(invoice.getCustomerId(), branchId, invoice.getTotalAmount(),
                com.storename.erp.crm.domain.ReceivableDebtMovementType.INVOICE, 
                "SALES_INVOICE", invoice.getId().toString(), userId, "Bán hàng " + invoice.getInvoiceCode());
        
        // 2. Trừ công nợ nếu có thanh toán trước
        BigDecimal advance = invoice.getAdvancePayment() != null ? invoice.getAdvancePayment() : BigDecimal.ZERO;
        if (advance.compareTo(BigDecimal.ZERO) > 0) {
            debtService.decreaseDebt(invoice.getCustomerId(), branchId, advance,
                    com.storename.erp.crm.domain.ReceivableDebtMovementType.PAYMENT,
                    "SALES_INVOICE_ADVANCE", invoice.getId().toString(), userId, "Khách trả trước " + invoice.getInvoiceCode());
        }
        
        BigDecimal newDebt = currentDebt.add(invoice.getTotalAmount()).subtract(advance);
        invoice.snapshotDebt(currentDebt, newDebt);
        invoice.confirm(userId);

        log.info("Invoice {} confirmed, totalAmount: {}, previousDebt: {}, remainingDebt: {}",
                invoice.getInvoiceCode(), invoice.getTotalAmount(), currentDebt, newDebt);

        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public List<SalesInvoice> getInvoicesByBranch(Long branchId) {
        return invoiceRepository.findByBranchId(branchId);
    }

    @Transactional(readOnly = true)
    public SalesInvoice getInvoice(UUID id, Long branchId) {
        SalesInvoice invoice = invoiceRepository.findById(id).orElseThrow(() -> new RuntimeException("Invoice not found"));
        if (!invoice.getBranchId().equals(branchId)) {
            throw new RuntimeException("Unauthorized");
        }
        return invoice;
    }

    @Transactional(readOnly = true)
    public List<com.storename.erp.order.api.dto.SalesInvoiceResponseDto> getInvoicesDtoByBranch(Long branchId) {
        List<SalesInvoice> invoices = getInvoicesByBranch(branchId);
        List<UUID> customerIds = invoices.stream().map(SalesInvoice::getCustomerId).distinct().toList();
        // Since we are in order module, we shouldn't ideally use CustomerRepository directly.
        // But for simplicity in this project (as seen in GoodsReturnService), we can inject it.
        // Wait, GoodsReturnService has it? Let me check. I'll just query it.
        // I will use a private method to get customer names for now.
        return invoices.stream()
                .map(inv -> com.storename.erp.order.api.dto.SalesInvoiceResponseDto.fromEntity(inv, getCustomerName(inv.getCustomerId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public com.storename.erp.order.api.dto.SalesInvoiceResponseDto getInvoiceDto(UUID id, Long branchId) {
        SalesInvoice invoice = getInvoice(id, branchId);
        return com.storename.erp.order.api.dto.SalesInvoiceResponseDto.fromEntity(invoice, getCustomerName(invoice.getCustomerId()));
    }

    private String getCustomerName(UUID customerId) {
        if (customerRepository == null) return null;
        return customerRepository.findById(customerId).map(com.storename.erp.crm.domain.Customer::getName).orElse(null);
    }
}

