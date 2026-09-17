package com.storename.erp.order.application;

import com.storename.erp.crm.api.CrmFacade;
import com.storename.erp.crm.application.ReceivableDebtService;
import com.storename.erp.inventory.api.InventoryFacade;
import com.storename.erp.order.application.dto.SalesInvoiceCreateDto;
import com.storename.erp.order.application.dto.SalesInvoiceLineDto;
import com.storename.erp.order.domain.SalesInvoice;
import com.storename.erp.order.domain.SalesInvoiceLine;
import com.storename.erp.order.domain.SalesInvoiceStatus;
import com.storename.erp.order.infrastructure.SalesInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesInvoiceService {
    private final SalesInvoiceRepository invoiceRepository;
    private final ReceivableDebtService debtService;
    private final InventoryFacade inventoryFacade;
    private final CrmFacade crmFacade;

    /**
     * Builds a SalesInvoice aggregate (header + lines) from the create request,
     * validating the advance payment, WITHOUT persisting it.
     * Shared by {@link #createDraft} and {@link #createAndConfirm}.
     */
    private SalesInvoice buildInvoice(SalesInvoiceCreateDto dto, Long branchId) {
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

        if (invoice.getAdvancePayment() != null) {
            if (invoice.getAdvancePayment().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Advance payment cannot be negative");
            }
            if (invoice.getAdvancePayment().compareTo(invoice.getTotalAmount()) > 0) {
                throw new IllegalArgumentException("Advance payment cannot exceed the total invoice amount");
            }
        }

        return invoice;
    }

    /**
     * Applies the effects of confirming an invoice: deducts inventory (cost snapshot per line),
     * raises the customer's receivable debt by the invoice total, then reduces it back by any
     * advance payment already collected, and finally flips the entity to CONFIRMED.
     * Shared by {@link #confirmInvoice} (explicit two-step confirm of an existing DRAFT) and
     * {@link #createAndConfirm} (single-step create, still inside the same DB transaction).
     */
    private void applyConfirmationEffects(SalesInvoice invoice, Long branchId, UUID userId) {
        for (SalesInvoiceLine line : invoice.getLines()) {
            InventoryFacade.SaleCostResult result = inventoryFacade.recordSaleAndGetCost(
                    line.getProductId(), branchId, line.getQuantity(), invoice.getId().toString(), line.getId(), userId);

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
    }

    @Transactional
    public SalesInvoice createDraft(SalesInvoiceCreateDto dto, Long branchId) {
        return invoiceRepository.save(buildInvoice(dto, branchId));
    }

    /**
     * Creates the invoice and confirms it in the same step: no DRAFT row is ever left
     * behind for the user to lose track of. Build + persist (to obtain generated ids for
     * the invoice/lines) + inventory deduction + debt update all happen inside this single
     * {@code @Transactional} boundary, so if anything fails (e.g. inventory/debt service
     * throws), the whole insert rolls back and nothing is saved — the caller gets an error
     * instead of an orphaned, unconfirmed invoice.
     */
    @org.springframework.retry.annotation.Retryable(
        retryFor = {
            org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            org.springframework.dao.DataIntegrityViolationException.class
        },
        maxAttempts = 3,
        backoff = @org.springframework.retry.annotation.Backoff(delay = 100)
    )
    @Transactional
    public SalesInvoice createAndConfirm(SalesInvoiceCreateDto dto, Long branchId, UUID userId) {
        if (!crmFacade.customerExists(dto.getCustomerId())) {
            throw new com.storename.erp.common.exception.ResourceNotFoundException("Khách hàng chưa có tại chi nhánh (chưa đồng bộ từ HQ): " + dto.getCustomerId());
        }
        SalesInvoice invoice = invoiceRepository.save(buildInvoice(dto, branchId));

        applyConfirmationEffects(invoice, branchId, userId);

        log.info("Invoice {} created and confirmed directly (no draft step), totalAmount: {}, previousDebt: {}, remainingDebt: {}",
                invoice.getInvoiceCode(), invoice.getTotalAmount(), invoice.getPreviousDebt(), invoice.getRemainingDebt());

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

        applyConfirmationEffects(invoice, branchId, userId);

        log.info("Invoice {} confirmed, totalAmount: {}, previousDebt: {}, remainingDebt: {}",
                invoice.getInvoiceCode(), invoice.getTotalAmount(), invoice.getPreviousDebt(), invoice.getRemainingDebt());

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
        // Optimize N+1 for customer fetching
        java.util.List<UUID> customerIds = invoices.stream()
                .map(SalesInvoice::getCustomerId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        java.util.Map<UUID, String> customerNameMap = crmFacade.getCustomerNames(customerIds);

        return invoices.stream()
                .map(inv -> com.storename.erp.order.api.dto.SalesInvoiceResponseDto.fromEntity(inv, customerNameMap.get(inv.getCustomerId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public com.storename.erp.order.api.dto.SalesInvoiceResponseDto getInvoiceDto(UUID id, Long branchId) {
        SalesInvoice invoice = getInvoice(id, branchId);
        return com.storename.erp.order.api.dto.SalesInvoiceResponseDto.fromEntity(invoice, getCustomerName(invoice.getCustomerId()));
    }

    private String getCustomerName(UUID customerId) {
        if (customerId == null) return null;
        return crmFacade.getCustomerNames(java.util.List.of(customerId)).get(customerId);
    }
}
