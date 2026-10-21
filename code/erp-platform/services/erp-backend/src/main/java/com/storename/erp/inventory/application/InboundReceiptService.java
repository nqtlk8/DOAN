package com.storename.erp.inventory.application;

import com.storename.erp.common.exception.ResourceNotFoundException;
import com.storename.erp.inventory.application.dto.InboundReceiptCreateDto;
import com.storename.erp.inventory.domain.*;
import com.storename.erp.inventory.infrastructure.CostLayerRepository;
import com.storename.erp.inventory.infrastructure.InboundReceiptRepository;
import com.storename.erp.inventory.infrastructure.StockMovementRepository;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.UUID;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboundReceiptService {

    private final InboundReceiptRepository inboundRepo;
    private final StockOnHandRepository stockRepo;
    private final StockMovementRepository movementRepo;
    private final CostLayerRepository costLayerRepo;

    @Transactional
    public UUID createDraft(Long branchId, InboundReceiptCreateDto dto) {
        return createDraft(branchId, dto, null);
    }

    /**
     * Tạo phiếu nhập DRAFT. {@code requestKey} (khóa lập phiếu, xem {@code IdempotencyContext}) được ghi vào
     * phiếu trong cùng transaction: gửi lại cùng khóa thì trả lại phiếu đã lưu, không tạo phiếu thứ hai.
     */
    @Transactional
    public UUID createDraft(Long branchId, InboundReceiptCreateDto dto, String requestKey) {
        requireBranch(branchId);
        if (requestKey != null) {
            java.util.Optional<InboundReceipt> existing = inboundRepo.findByRequestKey(requestKey);
            if (existing.isPresent()) {
                if (!existing.get().getBranchId().equals(branchId)) {
                    throw new AccessDeniedException("Receipt does not belong to this branch");
                }
                log.info("Inbound receipt {} already created with this request key, returning it", existing.get().getReceiptCode());
                return existing.get().getId();
            }
        }
        String code = dto.getReceiptCode();
        if (code == null || code.trim().isEmpty()) {
            code = "NK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } else if (inboundRepo.existsByReceiptCode(code)) {
            throw new IllegalArgumentException("Receipt code already exists");
        }

        InboundReceipt receipt = new InboundReceipt(branchId, code, dto.getNote());
        receipt.setSupplierId(dto.getSupplierId());
        receipt.setRequestKey(requestKey);
        
        for (InboundReceiptCreateDto.InboundReceiptLineDto lineDto : dto.getLines()) {
            InboundReceiptLine line = new InboundReceiptLine(
                    lineDto.getProductId(),
                    lineDto.getQuantity(),
                    lineDto.getUnitCost(),
                    lineDto.getUnitOfMeasure()
            );
            receipt.addLine(line);
        }

        receipt = inboundRepo.save(receipt);
        log.info("Created DRAFT inbound receipt {} for branch {}", receipt.getId(), branchId);
        return receipt.getId();
    }

    /**
     * Sửa phiếu nhập khi còn nháp: thay nhà cung cấp, ghi chú và toàn bộ dòng hàng. Chưa tác động kho.
     * Khóa phiếu trước khi kiểm tra trạng thái (cùng khóa với {@link #confirmReceipt}), nên không thể sửa một
     * phiếu đang được xác nhận. Mã phiếu không đổi (bỏ qua {@code receiptCode} trong request).
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
    public InboundReceipt updateDraft(UUID receiptId, Long branchId, InboundReceiptCreateDto dto) {
        requireBranch(branchId);
        InboundReceipt receipt = inboundRepo.findByIdForUpdate(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found: " + receiptId));
        if (!receipt.getBranchId().equals(branchId)) {
            throw new AccessDeniedException("Receipt does not belong to this branch");
        }
        List<InboundReceiptLine> newLines = dto.getLines().stream()
                .map(l -> new InboundReceiptLine(l.getProductId(), l.getQuantity(), l.getUnitCost(), l.getUnitOfMeasure()))
                .toList();
        receipt.updateDraft(dto.getSupplierId(), dto.getNote(), newLines);
        receipt = inboundRepo.save(receipt);
        log.info("Updated DRAFT inbound receipt {} ({} lines)", receipt.getReceiptCode(), newLines.size());
        return receipt;
    }

    /**
     * Xác nhận phiếu nhập: tăng tồn, ghi biến động INBOUND, tạo lớp giá vốn NORMAL.
     * Dòng tồn được khóa ghi (findForUpdate) theo thứ tự productId tăng dần, cùng thứ tự với bán
     * hàng và trả hàng, nên không khóa chéo (deadlock) với các giao dịch đó. Hai phiếu cùng tạo dòng
     * tồn đầu tiên của một sản phẩm: một phiếu vi phạm unique (product_id, branch_id) và được thử lại.
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
    public void confirmReceipt(UUID receiptId, Long branchId, UUID userId) {
        log.info("Confirming inbound receipt {}, branch={}, userId={}", receiptId, branchId, userId);
        requireBranch(branchId);
        // Khóa phiếu: hai lần xác nhận đồng thời (kể cả của hai người khác nhau) xếp hàng, lần sau thấy CONFIRMED
        // và nhận 409 INVALID_STATUS; cũng không thể xác nhận xen giữa lúc phiếu đang được sửa.
        InboundReceipt receipt = inboundRepo.findByIdForUpdate(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found: " + receiptId));

        if (!receipt.getBranchId().equals(branchId)) {
            throw new AccessDeniedException("Receipt does not belong to this branch");
        }

        receipt.confirm(userId);

        List<InboundReceiptLine> lockOrder = receipt.getLines().stream()
                .sorted(Comparator.comparing(InboundReceiptLine::getProductId))
                .toList();
        for (InboundReceiptLine line : lockOrder) {
            // In a real system we'd convert units, but for now we trust line.getQuantity()
            // Or if we need conversion, we still do it, but requirement says no unit conversion.
            BigDecimal convertedQty = line.getQuantity();
            
            StockOnHand stock = stockRepo.findForUpdate(line.getProductId(), branchId)
                    .orElseGet(() -> stockRepo.saveAndFlush(new StockOnHand(line.getProductId(), branchId)));

            stock.increase(convertedQty, "INBOUND_RECEIPT");
            stockRepo.save(stock); 

            StockMovement mv = StockMovement.inbound(line.getProductId(), branchId,
                    convertedQty, receipt.getId().toString(), line.getId(), userId);
            mv = movementRepo.save(mv);

            costLayerRepo.save(CostLayer.fromInbound(
                    line.getProductId(), branchId, convertedQty, line.getUnitCost(), mv.getId()));
        }
        
        inboundRepo.save(receipt);
    }

    @Transactional(readOnly = true)
    public List<InboundReceipt> getReceiptsByBranch(Long branchId) {
        // branchId null = ADMIN toàn hệ thống (HQ): xem mọi chi nhánh, giống tồn kho và công nợ.
        return branchId == null ? inboundRepo.findAll() : inboundRepo.findByBranchId(branchId);
    }

    @Transactional(readOnly = true)
    public InboundReceipt getReceipt(UUID id, Long branchId) {
        InboundReceipt receipt = inboundRepo.findById(id).orElseThrow(() -> new com.storename.erp.common.exception.ResourceNotFoundException("Receipt not found: " + id));
        if (branchId != null && !receipt.getBranchId().equals(branchId)) {
            throw new AccessDeniedException("Document belongs to another branch");
        }
        return receipt;
    }

    private static void requireBranch(Long branchId) {
        if (branchId == null) {
            throw new AccessDeniedException("Thao tác này cần token gắn với một chi nhánh");
        }
    }
}
