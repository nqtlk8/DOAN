package com.storename.erp.order.application;

import com.storename.erp.common.exception.BusinessConflictException;
import com.storename.erp.common.exception.ResourceNotFoundException;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
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
    private final CustomerProductPriceService customerPriceService;

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
     * saves edited line prices as the customer's own price (customer_product_price),
     * raises the customer's receivable debt by the invoice total, then reduces it back by any
     * advance payment already collected, and finally flips the entity to CONFIRMED.
     * Shared by {@link #confirmInvoice} (explicit two-step confirm of an existing DRAFT) and
     * {@link #createAndConfirm} (single-step create, still inside the same DB transaction).
     */
    private void applyConfirmationEffects(SalesInvoice invoice, Long branchId, UUID userId) {
        // Khóa dòng tồn theo thứ tự productId tăng dần (cùng thứ tự với nhập kho và trả hàng):
        // hai hóa đơn chứa cùng các sản phẩm nhưng khác thứ tự dòng không thể khóa chéo nhau (deadlock).
        List<SalesInvoiceLine> lockOrder = invoice.getLines().stream()
                .sorted(Comparator.comparing(SalesInvoiceLine::getProductId))
                .toList();
        for (SalesInvoiceLine line : lockOrder) {
            InventoryFacade.SaleCostResult result = inventoryFacade.recordSaleAndGetCost(
                    line.getProductId(), branchId, line.getQuantity(), invoice.getId().toString(), line.getId(), userId);

            line.setUnitCost(result.unitCostSnapshot());
            line.setCostBasis(result.costBasis());
        }

        // Giá đã sửa trên hóa đơn -> lưu làm giá riêng của khách cho các đơn sau.
        // Cùng transaction: xác nhận thất bại thì giá cũng không được lưu.
        customerPriceService.applyFromInvoice(invoice, userId);

        // 1. Ghi nhận công nợ từ đơn bán (khóa ghi dòng công nợ của khách tại chi nhánh)
        debtService.increaseDebt(invoice.getCustomerId(), branchId, invoice.getTotalAmount(),
                com.storename.erp.crm.domain.ReceivableDebtMovementType.INVOICE,
                "SALES_INVOICE", invoice.getId().toString(), userId, "Bán hàng " + invoice.getInvoiceCode());
        // Nợ cũ đọc SAU khi đã khóa dòng công nợ: hai hóa đơn cùng khách xác nhận đồng thời
        // không thể cùng chụp một số nợ cũ.
        BigDecimal currentDebt = debtService.getCurrentDebt(invoice.getCustomerId(), branchId)
                .subtract(invoice.getTotalAmount());

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
        requireBranch(branchId);
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
            org.springframework.dao.DataIntegrityViolationException.class,
            org.springframework.dao.PessimisticLockingFailureException.class
        },
        maxAttempts = 3,
        backoff = @org.springframework.retry.annotation.Backoff(delay = 100)
    )
    @Transactional
    public SalesInvoice createAndConfirm(SalesInvoiceCreateDto dto, Long branchId, UUID userId) {
        requireBranch(branchId);
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
            org.springframework.dao.DataIntegrityViolationException.class,
            org.springframework.dao.PessimisticLockingFailureException.class
        },
        maxAttempts = 3,
        backoff = @org.springframework.retry.annotation.Backoff(delay = 100)
    )
    @Transactional
    public SalesInvoice confirmInvoice(UUID invoiceId, UUID userId, Long branchId) {
        requireBranch(branchId);
        SalesInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        if (!invoice.getBranchId().equals(branchId)) {
            throw new SecurityException("Cannot confirm invoice of different branch");
        }

        if (invoice.getStatus() != SalesInvoiceStatus.DRAFT) {
            throw new BusinessConflictException(SalesInvoice.INVALID_STATUS,
                    "Hóa đơn " + invoice.getInvoiceCode() + " không ở trạng thái DRAFT (hiện tại: " + invoice.getStatus() + ")");
        }

        applyConfirmationEffects(invoice, branchId, userId);

        log.info("Invoice {} confirmed, totalAmount: {}, previousDebt: {}, remainingDebt: {}",
                invoice.getInvoiceCode(), invoice.getTotalAmount(), invoice.getPreviousDebt(), invoice.getRemainingDebt());

        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public List<SalesInvoice> getInvoicesByBranch(Long branchId) {
        // branchId null = ADMIN toàn hệ thống (HQ): xem mọi chi nhánh, giống tồn kho và công nợ.
        return branchId == null ? invoiceRepository.findAll() : invoiceRepository.findByBranchId(branchId);
    }

    @Transactional(readOnly = true)
    public SalesInvoice getInvoice(UUID id, Long branchId) {
        SalesInvoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
        if (branchId != null && !invoice.getBranchId().equals(branchId)) {
            throw new AccessDeniedException("Document belongs to another branch");
        }
        return invoice;
    }

    /** Thao tác ghi chứng từ phải gắn với một chi nhánh (token HQ không có chi nhánh thì không được ghi). */
    static void requireBranch(Long branchId) {
        if (branchId == null) {
            throw new AccessDeniedException("Thao tác này cần token gắn với một chi nhánh");
        }
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
