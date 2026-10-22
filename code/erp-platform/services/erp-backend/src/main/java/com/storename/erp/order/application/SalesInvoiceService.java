package com.storename.erp.order.application;

import com.storename.erp.common.exception.ResourceNotFoundException;
import com.storename.erp.crm.api.CrmFacade;
import com.storename.erp.crm.application.ReceivableDebtService;
import com.storename.erp.inventory.api.InventoryFacade;
import com.storename.erp.order.application.dto.SalesInvoiceCreateDto;
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
     * Used by {@link #createDraft}.
     */
    private SalesInvoice buildInvoice(SalesInvoiceCreateDto dto, Long branchId) {
        SalesInvoice invoice = new SalesInvoice();
        invoice.setBranchId(branchId);
        String code = dto.getInvoiceCode();
        if (code == null || code.trim().isEmpty()) {
            code = "HD" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        invoice.setInvoiceCode(code);
        applyDraftContent(invoice, dto);
        return invoice;
    }

    /**
     * Ghi nội dung phiếu nháp từ request: khách, ghi chú, trả trước và thay toàn bộ dòng hàng, rồi tính lại tổng
     * và kiểm tra khoản trả trước. Dùng chung cho lưu nháp ({@link #createDraft}) và sửa nháp ({@link #updateDraft}).
     */
    private void applyDraftContent(SalesInvoice invoice, SalesInvoiceCreateDto dto) {
        invoice.setCustomerId(dto.getCustomerId());
        invoice.setNote(dto.getNote());
        invoice.setAdvancePayment(dto.getAdvancePayment() != null ? dto.getAdvancePayment() : BigDecimal.ZERO);

        invoice.getLines().clear();
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
    }

    /**
     * Applies the effects of confirming an invoice: deducts inventory (cost snapshot per line),
     * saves edited line prices as the customer's own price (customer_product_price),
     * raises the customer's receivable debt by the invoice total, then reduces it back by any
     * advance payment already collected, and finally flips the entity to CONFIRMED.
     * Used by {@link #confirmInvoice} (inside one DB transaction).
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

    /** Lưu nháp không kèm khóa lập phiếu (script nội bộ, test). */
    @Transactional
    public SalesInvoice createDraft(SalesInvoiceCreateDto dto, Long branchId) {
        return createDraft(dto, branchId, null);
    }

    /**
     * Bước 1 — Lưu hóa đơn ở trạng thái DRAFT. Chưa trừ kho, chưa ghi công nợ, chưa lưu giá riêng của khách:
     * mọi tác động nằm ở {@link #confirmInvoice} (giống phiếu nhập và phiếu trả).
     *
     * <p>{@code requestKey} (khóa lập phiếu, xem {@code IdempotencyContext}) được ghi vào hóa đơn trong cùng
     * transaction. Gửi lại cùng khóa thì trả lại hóa đơn đã lưu thay vì tạo hóa đơn thứ hai.</p>
     */
    @Transactional
    public SalesInvoice createDraft(SalesInvoiceCreateDto dto, Long branchId, String requestKey) {
        requireBranch(branchId);
        if (requestKey != null) {
            java.util.Optional<SalesInvoice> existing = invoiceRepository.findByRequestKey(requestKey);
            if (existing.isPresent()) {
                SalesInvoice invoice = existing.get();
                requireSameBranch(invoice.getBranchId(), branchId);
                log.info("Invoice {} already created with this request key, returning it", invoice.getInvoiceCode());
                return invoice;
            }
        }
        if (!crmFacade.customerExists(dto.getCustomerId())) {
            throw new com.storename.erp.common.exception.ResourceNotFoundException("Khách hàng chưa có tại chi nhánh (chưa đồng bộ từ HQ): " + dto.getCustomerId());
        }
        String requestedCode = dto.getInvoiceCode();
        if (requestedCode != null && !requestedCode.trim().isEmpty() && invoiceRepository.existsByInvoiceCode(requestedCode)) {
            throw new IllegalArgumentException("Invoice code already exists");
        }
        SalesInvoice invoice = buildInvoice(dto, branchId);
        invoice.setRequestKey(requestKey);
        invoice = invoiceRepository.save(invoice);
        log.info("Created DRAFT sales invoice {} for branch {}, totalAmount: {}", invoice.getInvoiceCode(), branchId, invoice.getTotalAmount());
        return invoice;
    }

    /**
     * Sửa hóa đơn khi còn nháp: ghi đè khách, ghi chú, trả trước và toàn bộ dòng hàng. Vẫn chưa trừ kho, chưa ghi
     * công nợ, chưa lưu giá riêng của khách (mọi tác động vẫn nằm ở {@link #confirmInvoice}).
     *
     * <p>Khóa hóa đơn ({@code findByIdForUpdate}) trước khi kiểm tra trạng thái, cùng khóa với bước xác nhận: sửa và
     * xác nhận đồng thời thì chạy lần lượt, nên xác nhận luôn tác động kho / công nợ đúng theo nội dung đã lưu.
     * Hóa đơn đã xác nhận -> 409 {@code INVALID_STATUS} (điều chỉnh bằng phiếu trả hàng). Mã hóa đơn không đổi.</p>
     */
    @org.springframework.retry.annotation.Retryable(
        retryFor = {
            org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            org.springframework.dao.PessimisticLockingFailureException.class
        },
        maxAttempts = 3,
        backoff = @org.springframework.retry.annotation.Backoff(delay = 100)
    )
    @Transactional
    public SalesInvoice updateDraft(UUID invoiceId, SalesInvoiceCreateDto dto, Long branchId) {
        requireBranch(branchId);
        SalesInvoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
        requireSameBranch(invoice.getBranchId(), branchId);
        if (invoice.getStatus() != SalesInvoiceStatus.DRAFT) {
            throw new com.storename.erp.common.exception.BusinessConflictException(SalesInvoice.INVALID_STATUS,
                    "Chỉ hóa đơn nháp (DRAFT) mới được sửa. Hóa đơn " + invoice.getInvoiceCode()
                            + " đã xác nhận (đã trừ kho, ghi công nợ); điều chỉnh bằng phiếu trả hàng.");
        }
        if (!crmFacade.customerExists(dto.getCustomerId())) {
            throw new ResourceNotFoundException("Khách hàng chưa có tại chi nhánh (chưa đồng bộ từ HQ): " + dto.getCustomerId());
        }
        applyDraftContent(invoice, dto);
        invoice = invoiceRepository.save(invoice);
        log.info("Updated DRAFT sales invoice {}, totalAmount: {}", invoice.getInvoiceCode(), invoice.getTotalAmount());
        return invoice;
    }

    /**
     * Bước 2 — Xác nhận hóa đơn DRAFT: trừ kho theo FIFO (chụp giá vốn từng dòng), lưu giá riêng của khách,
     * tăng công nợ theo tổng hóa đơn rồi giảm theo khoản trả trước, chụp nợ cũ / nợ còn lại.
     *
     * <p>Thứ tự khóa giống phiếu trả: hóa đơn ({@code findByIdForUpdate}) → dòng tồn (productId tăng dần)
     * → dòng công nợ. Khóa hóa đơn làm hai lần xác nhận đồng thời xếp hàng; lần sau thấy CONFIRMED và nhận
     * 409 {@code INVALID_STATUS}, nên kho và công nợ chỉ bị tác động một lần. Lỗi ở bất kỳ bước nào thì
     * toàn bộ rollback và hóa đơn vẫn là DRAFT.</p>
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
    public SalesInvoice confirmInvoice(UUID invoiceId, Long branchId, UUID userId) {
        requireBranch(branchId);
        SalesInvoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
        requireSameBranch(invoice.getBranchId(), branchId);
        if (invoice.getStatus() != SalesInvoiceStatus.DRAFT) {
            throw new com.storename.erp.common.exception.BusinessConflictException(SalesInvoice.INVALID_STATUS,
                    "Chỉ hóa đơn DRAFT mới được xác nhận (hiện tại: " + invoice.getStatus() + ")");
        }
        if (!crmFacade.customerExists(invoice.getCustomerId())) {
            throw new ResourceNotFoundException("Khách hàng chưa có tại chi nhánh (chưa đồng bộ từ HQ): " + invoice.getCustomerId());
        }

        applyConfirmationEffects(invoice, branchId, userId);

        log.info("Invoice {} confirmed, totalAmount: {}, previousDebt: {}, remainingDebt: {}",
                invoice.getInvoiceCode(), invoice.getTotalAmount(), invoice.getPreviousDebt(), invoice.getRemainingDebt());

        return invoiceRepository.save(invoice);
    }

    private static void requireSameBranch(Long documentBranchId, Long branchId) {
        if (!documentBranchId.equals(branchId)) {
            throw new AccessDeniedException("Invoice does not belong to this branch");
        }
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
