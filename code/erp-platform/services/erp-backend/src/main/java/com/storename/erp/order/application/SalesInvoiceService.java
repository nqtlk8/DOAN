package com.storename.erp.order.application;

import com.storename.erp.crm.application.ReceivableDebtService;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesInvoiceService {
    private final SalesInvoiceRepository invoiceRepository;
    private final StockOnHandRepository stockRepo;
    private final ReceivableDebtService debtService;

    @Transactional
    public SalesInvoice createDraft(SalesInvoiceCreateDto dto, Long branchId) {
        SalesInvoice invoice = new SalesInvoice();
        invoice.setBranchId(branchId);
        invoice.setCustomerId(dto.getCustomerId());
        invoice.setInvoiceCode(dto.getInvoiceCode());
        invoice.setPaymentMethod(dto.getPaymentMethod());
        invoice.setNote(dto.getNote());
        
        for (var lineDto : dto.getLines()) {
            SalesInvoiceLine line = new SalesInvoiceLine();
            line.setProductId(lineDto.getProductId());
            line.setQuantity(lineDto.getQuantity());
            line.setUnitPrice(lineDto.getUnitPrice());
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
            StockOnHand stock = stockRepo.findByProductIdAndBranchId(
                    line.getProductId(), branchId)
                .orElseGet(() -> stockRepo.save(
                    new StockOnHand(line.getProductId(), branchId)));

            line.setUnitCost(stock.getAvgCost());

            stock.decreaseAllowNegative(line.getQuantity(), "SALES_INVOICE");
            stockRepo.save(stock);
        }

        BigDecimal currentDebt = debtService.getCurrentDebt(invoice.getCustomerId(), branchId);
        BigDecimal newDebt = currentDebt.add(invoice.getTotalAmount());
        
        debtService.increaseDebt(invoice.getCustomerId(), branchId, invoice.getTotalAmount());
        
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
}

