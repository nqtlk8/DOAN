package com.storename.erp.order.application;

import com.storename.erp.crm.application.ReceivableDebtService;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import com.storename.erp.inventory.api.InventoryFacade;
import com.storename.erp.inventory.application.OutboundReceiptService;
import com.storename.erp.inventory.application.dto.OutboundReceiptCreateDto;
import com.storename.erp.order.application.dto.SalesInvoiceCreateDto;
import com.storename.erp.order.application.dto.SalesInvoiceLineDto;
import com.storename.erp.order.domain.SalesInvoice;
import com.storename.erp.order.domain.SalesInvoiceLine;
import com.storename.erp.order.domain.SalesInvoiceStatus;
import com.storename.erp.order.infrastructure.SalesInvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SalesInvoiceService {

    private final SalesInvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final InventoryFacade inventoryFacade;
    private final OutboundReceiptService outboundReceiptService;
    private final ReceivableDebtService debtService;

    public SalesInvoiceService(SalesInvoiceRepository invoiceRepository,
                               CustomerRepository customerRepository,
                               InventoryFacade inventoryFacade,
                               OutboundReceiptService outboundReceiptService,
                               ReceivableDebtService debtService) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.inventoryFacade = inventoryFacade;
        this.outboundReceiptService = outboundReceiptService;
        this.debtService = debtService;
    }

    @Transactional
    public SalesInvoice createDraft(SalesInvoiceCreateDto dto, UUID branchId) {
        if (!customerRepository.existsById(dto.getCustomerId())) {
            throw new RuntimeException("Customer not found");
        }

        SalesInvoice invoice = new SalesInvoice();
        invoice.setBranchId(branchId);
        invoice.setCustomerId(dto.getCustomerId());
        invoice.setInvoiceCode(dto.getInvoiceCode());
        invoice.setPaymentMethod(dto.getPaymentMethod());
        invoice.setNote(dto.getNote());
        
        for (SalesInvoiceLineDto lineDto : dto.getLines()) {
            SalesInvoiceLine line = new SalesInvoiceLine();
            line.setProductId(lineDto.getProductId());
            line.setProductName(lineDto.getProductName());
            line.setQuantity(lineDto.getQuantity());
            line.setUnitPrice(lineDto.getUnitPrice());
            line.setLineTotal(lineDto.getQuantity().multiply(lineDto.getUnitPrice()));
            line.setUnitOfMeasure(lineDto.getUnitOfMeasure());
            invoice.addLine(line);
        }
        
        invoice.calculateTotal();
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public SalesInvoice createFromOrder(com.storename.erp.order.domain.CustomerOrder order) {
        SalesInvoice invoice = new SalesInvoice();
        invoice.setBranchId(order.getBranchId());
        invoice.setCustomerId(order.getCustomer().getId());
        invoice.setInvoiceCode("INV-AUTO-" + order.getOrderCode());
        invoice.setPaymentMethod(com.storename.erp.order.domain.PaymentMethod.CASH);
        invoice.setNote("Auto generated from Order " + order.getOrderCode());

        for (com.storename.erp.order.domain.CustomerOrderLine orderLine : order.getLines()) {
            SalesInvoiceLine line = new SalesInvoiceLine();
            line.setProductId(orderLine.getProductId());
            line.setProductName("Product " + orderLine.getProductId());
            line.setQuantity(orderLine.getQuantity());
            line.setUnitPrice(orderLine.getUnitPrice());
            line.setLineTotal(orderLine.getQuantity().multiply(orderLine.getUnitPrice()));
            line.setUnitOfMeasure(orderLine.getUnitOfMeasure());
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
    public SalesInvoice confirmInvoice(UUID invoiceId, UUID userId, UUID branchId) {
        SalesInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        if (!invoice.getBranchId().equals(branchId)) {
            throw new SecurityException("Cannot confirm invoice of different branch");
        }
        if (invoice.getStatus() != SalesInvoiceStatus.DRAFT) {
            throw new IllegalStateException("Invoice is not DRAFT");
        }

        // Snapshot cost
        for (SalesInvoiceLine line : invoice.getLines()) {
            BigDecimal avgCost = inventoryFacade.getAverageCost(line.getProductId(), branchId);
            line.setUnitCost(avgCost);
        }

        // Tạo OutboundReceipt để trừ kho (OutboundReceiptService sẽ xử lý trừ kho)
        OutboundReceiptCreateDto outDto = new OutboundReceiptCreateDto();
        outDto.setReceiptCode("OUT-" + invoice.getInvoiceCode());
        outDto.setNote("Auto generated for invoice " + invoice.getInvoiceCode());
        outDto.setReason("SALES_INVOICE");
        outDto.setLines(invoice.getLines().stream().map(l -> {
            OutboundReceiptCreateDto.LineDto ol = new OutboundReceiptCreateDto.LineDto();
            ol.setProductId(l.getProductId());
            ol.setQuantity(l.getQuantity());
            ol.setUnitOfMeasure(l.getUnitOfMeasure());
            return ol;
        }).collect(Collectors.toList()));
        
        var outReceiptId = outboundReceiptService.createDraft(branchId, outDto);
        outboundReceiptService.confirmReceipt(outReceiptId, branchId, userId);

        // Update debt
        BigDecimal currentDebt = debtService.getCurrentDebt(invoice.getCustomerId(), branchId);
        BigDecimal newDebt = currentDebt.add(invoice.getTotalAmount());
        
        debtService.increaseDebt(invoice.getCustomerId(), branchId, invoice.getTotalAmount());
        
        invoice.snapshotDebt(currentDebt, newDebt);
        invoice.confirm(userId);

        log.info("Invoice {} confirmed, totalAmount: {}, previousDebt: {}, remainingDebt: {}",
                invoice.getInvoiceCode(), invoice.getTotalAmount(), currentDebt, newDebt);

        return invoiceRepository.save(invoice);
    }
}
