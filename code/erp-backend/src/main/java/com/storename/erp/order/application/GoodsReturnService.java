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
 * Service quản lý khách trả hàng (Goods Return).
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
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    /**
     * Tạo phiếu trả hàng nháp.
     */
    @Transactional
    public UUID createDraft(UUID branchId, GoodsReturnCreateDto dto) {
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
     * Xác nhận phiếu trả hàng: cộng lại kho (InboundReceipt) và trừ công nợ (nếu có hoá đơn gốc).
     */
    @org.springframework.retry.annotation.Retryable(
        retryFor = org.springframework.orm.ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @org.springframework.retry.annotation.Backoff(delay = 100)
    )
    @Transactional
    public void confirmReturn(UUID returnId, UUID branchId, UUID userId) {
        GoodsReturn goodsReturn = returnRepository.findById(returnId)
                .orElseThrow(() -> new IllegalArgumentException("Return not found"));

        if (!goodsReturn.getBranchId().equals(branchId)) {
            throw new IllegalArgumentException("Return does not belong to this branch");
        }

        goodsReturn.confirm(userId);

        // 1. Nhập kho trả hàng
        InboundReceiptCreateDto inDto = new InboundReceiptCreateDto();
        inDto.setReceiptCode("IN-RET-" + goodsReturn.getReturnCode());
        inDto.setNote("Nhập kho từ phiếu trả hàng " + goodsReturn.getReturnCode());
        
        inDto.setLines(goodsReturn.getLines().stream().map(l -> {
            InboundReceiptCreateDto.LineDto ld = new InboundReceiptCreateDto.LineDto();
            ld.setProductId(l.getProductId());
            ld.setQuantity(l.getQuantity());
            // Giá vốn nhập kho hoàn trả tạm lấy theo đơn giá trả, hoặc lấy từ lịch sử (ở đây lấy unitPrice của hàng trả)
            ld.setUnitCost(l.getUnitPrice());
            ld.setUnitOfMeasure(l.getUnitOfMeasure());
            return ld;
        }).collect(Collectors.toList()));

        UUID receiptId = inboundReceiptService.createDraft(branchId, inDto);
        inboundReceiptService.confirmReceipt(receiptId, branchId, userId);

        // 2. Trừ công nợ (Chỉ trừ nếu trả hàng dựa trên hoá đơn gốc)
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
                
                // Lấy tổng số lượng đã trả trước đó của SP này trong Hoá đơn
                java.math.BigDecimal previousReturned = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(grl.quantity), 0) FROM goods_return_line grl " +
                    "JOIN goods_return gr ON gr.id = grl.return_id " +
                    "WHERE gr.invoice_id = ? AND gr.status = 'CONFIRMED' AND grl.product_id = ?",
                    java.math.BigDecimal.class, invoice.getId(), returnLine.getProductId());
                
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
}
