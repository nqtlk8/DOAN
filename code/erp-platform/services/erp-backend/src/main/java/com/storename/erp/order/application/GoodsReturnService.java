package com.storename.erp.order.application;

import com.storename.erp.common.exception.ResourceNotFoundException;
import com.storename.erp.crm.api.CrmFacade;
import com.storename.erp.crm.application.ReceivableDebtService;
import com.storename.erp.inventory.api.InventoryFacade;
import com.storename.erp.order.application.dto.GoodsReturnCreateDto;
import com.storename.erp.order.domain.GoodsReturn;
import com.storename.erp.order.domain.GoodsReturnLine;
import com.storename.erp.order.domain.SalesInvoice;
import com.storename.erp.order.domain.SalesInvoiceLine;
import com.storename.erp.order.infrastructure.GoodsReturnRepository;
import com.storename.erp.order.infrastructure.SalesInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
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
        return createDraft(branchId, dto, null);
    }

    /**
     * Tạo phiếu trả DRAFT. {@code requestKey} (khóa lập phiếu, xem {@code IdempotencyContext}) được ghi vào
     * phiếu trong cùng transaction: gửi lại cùng khóa thì trả lại phiếu đã lưu, không tạo phiếu thứ hai.
     */
    @Transactional
    public UUID createDraft(Long branchId, GoodsReturnCreateDto dto, String requestKey) {
        SalesInvoiceService.requireBranch(branchId);
        if (requestKey != null) {
            java.util.Optional<GoodsReturn> existing = returnRepository.findByRequestKey(requestKey);
            if (existing.isPresent()) {
                if (!existing.get().getBranchId().equals(branchId)) {
                    throw new AccessDeniedException("Return does not belong to this branch");
                }
                log.info("Goods return {} already created with this request key, returning it", existing.get().getReturnCode());
                return existing.get().getId();
            }
        }
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
        goodsReturn.setRequestKey(requestKey);

        for (GoodsReturnCreateDto.GoodsReturnLineDto lineDto : dto.getLines()) {
            GoodsReturnLine line = new GoodsReturnLine();
            line.setProductId(lineDto.getProductId());
            line.setQuantity(lineDto.getQuantity());
            line.setUnitPrice(lineDto.getUnitPrice());
            line.setUnitOfMeasure(lineDto.getUnitOfMeasure());
            goodsReturn.addLine(line);
        }

        // Kiểm tra sớm ngay khi lưu nháp (cùng quy tắc với bước xác nhận) để không để lại phiếu nháp
        // không thể xác nhận được. Bước xác nhận vẫn kiểm tra lại dưới khóa hóa đơn, vì giữa hai bước
        // có thể đã có phiếu trả khác được xác nhận.
        if (dto.getInvoiceId() != null) {
            SalesInvoice invoice = invoiceRepository.findById(dto.getInvoiceId())
                    .orElseThrow(() -> new IllegalArgumentException("Original invoice not found"));
            checkInvoiceReturnable(invoice, goodsReturn);
        }

        goodsReturn.calculateTotal();
        goodsReturn = returnRepository.save(goodsReturn);
        log.info("Created DRAFT Goods Return {} for branch {}", goodsReturn.getId(), branchId);
        return goodsReturn.getId();
    }

    /**
     * Sửa phiếu trả khi còn nháp: thay khách, hóa đơn gốc, lý do, ghi chú và toàn bộ dòng hàng. Chưa nhập kho, chưa
     * giảm công nợ. Kiểm tra lại số lượng trả theo hóa đơn gốc như lúc lưu nháp.
     *
     * <p>Khóa phiếu trả trước khi kiểm tra trạng thái (cùng khóa với {@link #confirmReturn}). Phiếu đã xác nhận ->
     * 409 {@code INVALID_STATUS}. Mã phiếu không đổi.</p>
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
    public GoodsReturn updateDraft(UUID returnId, Long branchId, GoodsReturnCreateDto dto) {
        SalesInvoiceService.requireBranch(branchId);
        GoodsReturn goodsReturn = returnRepository.findByIdForUpdate(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return not found: " + returnId));
        if (!goodsReturn.getBranchId().equals(branchId)) {
            throw new AccessDeniedException("Return does not belong to this branch");
        }
        if (goodsReturn.getStatus() != com.storename.erp.order.domain.GoodsReturnStatus.DRAFT) {
            throw new com.storename.erp.common.exception.BusinessConflictException(SalesInvoice.INVALID_STATUS,
                    "Chỉ phiếu trả nháp (DRAFT) mới được sửa. Phiếu " + goodsReturn.getReturnCode()
                            + " đã được xác nhận (đã nhập kho, giảm công nợ).");
        }
        if (!crmFacade.customerExists(dto.getCustomerId())) {
            throw new IllegalArgumentException("Customer not found");
        }

        goodsReturn.setCustomerId(dto.getCustomerId());
        goodsReturn.setInvoiceId(dto.getInvoiceId());
        goodsReturn.setReason(dto.getReason());
        goodsReturn.setNote(dto.getNote());
        goodsReturn.getLines().clear();
        for (GoodsReturnCreateDto.GoodsReturnLineDto lineDto : dto.getLines()) {
            GoodsReturnLine line = new GoodsReturnLine();
            line.setProductId(lineDto.getProductId());
            line.setQuantity(lineDto.getQuantity());
            line.setUnitPrice(lineDto.getUnitPrice());
            line.setUnitOfMeasure(lineDto.getUnitOfMeasure());
            goodsReturn.addLine(line);
        }

        // Cùng quy tắc với lưu nháp: không để lại phiếu nháp không xác nhận được.
        if (dto.getInvoiceId() != null) {
            SalesInvoice invoice = invoiceRepository.findById(dto.getInvoiceId())
                    .orElseThrow(() -> new IllegalArgumentException("Original invoice not found"));
            checkInvoiceReturnable(invoice, goodsReturn);
        }

        goodsReturn.calculateTotal();
        goodsReturn = returnRepository.save(goodsReturn);
        log.info("Updated DRAFT Goods Return {} ({} lines)", goodsReturn.getReturnCode(), goodsReturn.getLines().size());
        return goodsReturn;
    }

    /**
     * Xác nhận phiếu trả hàng: cập nhật tồn kho, cost layer và trừ công nợ.
     *
     * <p>Có hóa đơn gốc: số lượng trả được kiểm tra theo từng cặp (sản phẩm, ĐVT), cộng dồn mọi dòng
     * của phiếu này và các phiếu trả đã xác nhận trước đó, so với tổng số lượng đã bán của cặp đó
     * trên hóa đơn (hóa đơn có thể có nhiều dòng cùng sản phẩm). Không có hóa đơn gốc: hàng trả
     * được nhập lại kho như nhập hàng bình thường (nghiệp vụ thực tế luôn có hóa đơn).
     *
     * <p>Công nợ chỉ giảm tối đa bằng số dư hiện có; phần còn lại (khách đã trả đủ) được ghi vào
     * {@code refundAmount} là tiền hoàn lại cho khách.
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
    public void confirmReturn(UUID returnId, Long branchId, UUID userId) {
        SalesInvoiceService.requireBranch(branchId);
        // Khóa phiếu trả trước (thứ tự khóa: phiếu trả -> hóa đơn gốc -> dòng tồn -> dòng công nợ): hai lần xác
        // nhận đồng thời xếp hàng, lần sau nhận 409 INVALID_STATUS; không thể xác nhận xen giữa lúc đang sửa.
        GoodsReturn goodsReturn = returnRepository.findByIdForUpdate(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return not found: " + returnId));

        if (!goodsReturn.getBranchId().equals(branchId)) {
            throw new AccessDeniedException("Return does not belong to this branch");
        }

        SalesInvoice invoice = null;
        if (goodsReturn.getInvoiceId() != null) {
            invoice = invoiceRepository.findByIdForUpdate(goodsReturn.getInvoiceId())
                    .orElseThrow(() -> new IllegalArgumentException("Original invoice not found"));

            checkInvoiceReturnable(invoice, goodsReturn);
        }

        goodsReturn.confirm(userId);

        // Hàng trả nhập lại kho theo GIÁ VỐN đã snapshot lúc bán (không theo giá bán),
        // để giá trị tồn kho và giá vốn các lần bán sau không bị đội lên.
        // Thứ tự productId tăng dần: cùng thứ tự khóa dòng tồn với bán hàng và nhập kho.
        List<GoodsReturnLine> lockOrder = goodsReturn.getLines().stream()
                .sorted(java.util.Comparator.comparing(GoodsReturnLine::getProductId))
                .toList();
        for (GoodsReturnLine line : lockOrder) {
            BigDecimal unitCost = resolveReturnUnitCost(invoice, goodsReturn.getCustomerId(), branchId, line);
            line.setUnitCost(unitCost);
            inventoryFacade.recordReturn(line.getProductId(), branchId, line.getQuantity(), unitCost, goodsReturn.getId().toString(), line.getId(), userId);
        }

        BigDecimal total = goodsReturn.getTotalAmount();
        BigDecimal debtReduced = debtService.decreaseDebtUpToBalance(goodsReturn.getCustomerId(), branchId, total,
                com.storename.erp.crm.domain.ReceivableDebtMovementType.RETURN,
                "GOODS_RETURN", goodsReturn.getId().toString(), userId, "Khách trả hàng " + goodsReturn.getReturnCode());
        goodsReturn.setRefundAmount(total.subtract(debtReduced));
        log.info("Goods return {}: debt reduced by {}, cash refund {}", goodsReturn.getReturnCode(),
                debtReduced, goodsReturn.getRefundAmount());

        returnRepository.save(goodsReturn);
        log.info("Confirmed Goods Return {}", returnId);
    }

    /** Hóa đơn gốc phải cùng chi nhánh, cùng khách, đã xác nhận, và số lượng trả không vượt số đã bán. */
    private void checkInvoiceReturnable(SalesInvoice invoice, GoodsReturn goodsReturn) {
        if (!invoice.getBranchId().equals(goodsReturn.getBranchId()) || !invoice.getCustomerId().equals(goodsReturn.getCustomerId())) {
            throw new IllegalArgumentException("Invoice does not belong to the correct branch/customer");
        }
        if (invoice.getStatus() != com.storename.erp.order.domain.SalesInvoiceStatus.CONFIRMED) {
            throw new IllegalArgumentException("Cannot return goods for unconfirmed invoice");
        }
        validateReturnQuantities(invoice, goodsReturn);
    }

    /** Khóa so khớp dòng trả với dòng hóa đơn: cùng sản phẩm và cùng ĐVT (không quy đổi đơn vị). */
    private record ItemKey(Long productId, String unitOfMeasure) {}

    /**
     * Tổng SL đã trả (phiếu CONFIRMED trước đó) + tổng SL trả trên phiếu này ≤ tổng SL đã bán,
     * tính cho từng cặp (sản phẩm, ĐVT). Vi phạm -> IllegalArgumentException (HTTP 400).
     */
    void validateReturnQuantities(SalesInvoice invoice, GoodsReturn goodsReturn) {
        java.util.Map<ItemKey, BigDecimal> requested = new java.util.LinkedHashMap<>();
        for (GoodsReturnLine l : goodsReturn.getLines()) {
            requested.merge(new ItemKey(l.getProductId(), l.getUnitOfMeasure()), l.getQuantity(), BigDecimal::add);
        }
        java.util.Map<ItemKey, BigDecimal> sold = new java.util.HashMap<>();
        for (SalesInvoiceLine l : invoice.getLines()) {
            sold.merge(new ItemKey(l.getProductId(), l.getUnitOfMeasure()), l.getQuantity(), BigDecimal::add);
        }
        for (var entry : requested.entrySet()) {
            ItemKey key = entry.getKey();
            BigDecimal soldQty = sold.get(key);
            if (soldQty == null) {
                throw new IllegalArgumentException("Product not found in original invoice: product " + key.productId()
                        + " (" + key.unitOfMeasure() + ")");
            }
            BigDecimal previous = returnRepository.getTotalReturnedQuantity(invoice.getId(), key.productId(), key.unitOfMeasure());
            if (previous == null) previous = BigDecimal.ZERO;
            if (previous.add(entry.getValue()).compareTo(soldQty) > 0) {
                throw new IllegalArgumentException(String.format(
                        "Total returned quantity exceeds invoice sold quantity: product %d (%s) sold %s, returned before %s, returning %s",
                        key.productId(), key.unitOfMeasure(), plain(soldQty), plain(previous), plain(entry.getValue())));
            }
        }
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }

    /**
     * Giá vốn của một dòng hàng trả:
     * 1. Có hóa đơn gốc -> giá vốn bình quân (theo SL) các dòng hóa đơn khớp (cùng sản phẩm + ĐVT) có giá vốn.
     * 2. Không có / dòng chưa có giá vốn -> unit_cost lần bán gần nhất sản phẩm đó cho khách đó tại chi nhánh.
     * 3. Không có lần bán nào -> giá vốn lớp gần nhất của sản phẩm tại chi nhánh; không có nữa -> 0.
     */
    BigDecimal resolveReturnUnitCost(SalesInvoice invoice, UUID customerId, Long branchId, GoodsReturnLine line) {
        if (invoice != null) {
            List<SalesInvoiceLine> matched = invoice.getLines().stream()
                    .filter(l -> l.getProductId().equals(line.getProductId())
                            && java.util.Objects.equals(l.getUnitOfMeasure(), line.getUnitOfMeasure())
                            && l.getUnitCost() != null && l.getQuantity() != null)
                    .toList();
            if (!matched.isEmpty()) {
                BigDecimal first = matched.get(0).getUnitCost();
                if (matched.stream().allMatch(l -> l.getUnitCost().compareTo(first) == 0)) {
                    return first; // một dòng, hoặc nhiều dòng cùng giá vốn
                }
                BigDecimal qty = BigDecimal.ZERO;
                BigDecimal cost = BigDecimal.ZERO;
                for (SalesInvoiceLine l : matched) {
                    qty = qty.add(l.getQuantity());
                    cost = cost.add(l.getQuantity().multiply(l.getUnitCost()));
                }
                if (qty.compareTo(BigDecimal.ZERO) > 0) {
                    return cost.divide(qty, 4, java.math.RoundingMode.HALF_UP);
                }
            }
        }
        if (customerId != null) {
            List<BigDecimal> recent = invoiceRepository.findRecentSaleUnitCosts(
                    customerId, branchId, line.getProductId(), org.springframework.data.domain.Pageable.ofSize(1));
            if (!recent.isEmpty() && recent.get(0) != null) return recent.get(0);
        }
        BigDecimal latestLayer = inventoryFacade.getLatestLayerUnitCost(line.getProductId(), branchId);
        return latestLayer != null ? latestLayer : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public java.util.List<com.storename.erp.order.api.dto.GoodsReturnResponseDto> getReturnsByBranch(Long branchId) {
        // branchId null = ADMIN toàn hệ thống (HQ): xem mọi chi nhánh.
        java.util.List<com.storename.erp.order.domain.GoodsReturn> returns = branchId == null
                ? returnRepository.findAll() : returnRepository.findByBranchId(branchId);
        
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
        com.storename.erp.order.domain.GoodsReturn goodsReturn = returnRepository.findById(id).orElseThrow(() -> new com.storename.erp.common.exception.ResourceNotFoundException("Return not found: " + id));
        if (branchId != null && !goodsReturn.getBranchId().equals(branchId)) {
            throw new org.springframework.security.access.AccessDeniedException("Document belongs to another branch");
        }
        String customerName = null;
        if (goodsReturn.getCustomerId() != null) {
            customerName = crmFacade.getCustomerNames(java.util.List.of(goodsReturn.getCustomerId()))
                    .get(goodsReturn.getCustomerId());
        }
                
        return com.storename.erp.order.api.dto.GoodsReturnResponseDto.fromEntity(goodsReturn, customerName);
    }
}

