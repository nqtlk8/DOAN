package com.storename.erp.inventory.application;

import com.storename.erp.catalog.api.CatalogFacade;
import com.storename.erp.inventory.api.InventoryFacade;
import com.storename.erp.inventory.domain.CostLayer;
import com.storename.erp.inventory.domain.StockInsufficientException;
import com.storename.erp.inventory.domain.StockMovement;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.CostLayerRepository;
import com.storename.erp.inventory.infrastructure.StockMovementRepository;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Implementation of InventoryFacade, handling cross-domain communication
 * for inventory operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryFacadeImpl implements InventoryFacade {

    private final StockOnHandRepository stockOnHandRepository;
    private final StockMovementRepository stockMovementRepository;
    private final CostLayerRepository costLayerRepository;
    private final FifoCostService fifoCostService;
    private final CatalogFacade catalogFacade;

    /**
     * Lấy số lượng tồn kho khả dụng của một sản phẩm tại một chi nhánh.
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAvailableQuantity(Long productId, Long branchId) {
        log.debug("Getting available quantity for product {} at branch {}", productId, branchId);
        return stockOnHandRepository.findByProductIdAndBranchId(productId, branchId)
                .map(StockOnHand::getQuantity)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Ghi nhận xuất kho bán hàng (SALE): Tiêu thụ FIFO, giảm tồn kho, ghi nhận movement.
     */
    @Override
    @Transactional
    public SaleCostResult recordSaleAndGetCost(Long productId, Long branchId, BigDecimal quantity, String invoiceId, UUID lineId, UUID userId) {
        log.info("Recording sale for product {} branch {} qty {} invoice {}", productId, branchId, quantity, invoiceId);

        // 1. Khóa dòng tồn rồi kiểm tra: không cho tồn âm — phải nhập hàng trước khi bán.
        StockOnHand stock = stockOnHandRepository.findForUpdate(productId, branchId).orElse(null);
        BigDecimal available = stock != null ? stock.getQuantity() : BigDecimal.ZERO;
        if (stock == null || available.compareTo(quantity) < 0) {
            throw insufficientStock(productId, branchId, available, quantity);
        }

        // 2. FIFO consume → lấy unit_cost_snapshot (ném lỗi nếu lớp giá vốn không đủ)
        FifoCostService.FifoResult fifo;
        try {
            fifo = fifoCostService.consume(productId, branchId, quantity);
        } catch (StockInsufficientException e) {
            log.error("Lệch tồn kho/lớp giá vốn: product={} branch={} stock={} layers={}",
                    productId, branchId, available, e.getAvailable());
            throw insufficientStock(productId, branchId, e.getAvailable(), quantity);
        }

        // 3. stock_on_hand
        stock.decrease(quantity, "SALE");
        stockOnHandRepository.save(stock);

        // 4. stock_movement
        stockMovementRepository.save(StockMovement.sale(productId, branchId, quantity, invoiceId, lineId, userId));

        return new SaleCostResult(fifo.unitCostSnapshot(), fifo.costBasis());
    }

    /**
     * Ghi nhận nhập kho do khách trả hàng (RETURN): Tăng tồn kho, ghi nhận movement, tạo cost layer
     * theo GIÁ VỐN của hàng trả (unitCost), không phải giá bán.
     */
    @Override
    @Transactional
    public void recordReturn(Long productId, Long branchId, BigDecimal quantity, BigDecimal unitCost, String returnId, UUID lineId, UUID userId) {
        log.info("Recording return for product {} branch {} qty {} returnId {}", productId, branchId, quantity, returnId);

        // Khóa ghi dòng tồn như khi bán/nhập; chưa có dòng thì tạo và flush ngay để giao dịch song song
        // vi phạm unique (product_id, branch_id) sớm và được thử lại (@Retryable của caller).
        StockOnHand stock = stockOnHandRepository.findForUpdate(productId, branchId)
                .orElseGet(() -> stockOnHandRepository.saveAndFlush(new StockOnHand(productId, branchId)));
        stock.increase(quantity, "RETURN");
        stockOnHandRepository.save(stock);

        StockMovement mv = StockMovement.returnGoods(productId, branchId, quantity, returnId, lineId, userId);
        mv = stockMovementRepository.save(mv);

        costLayerRepository.save(CostLayer.fromReturn(productId, branchId, quantity,
                unitCost != null ? unitCost : BigDecimal.ZERO, mv.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getLatestLayerUnitCost(Long productId, Long branchId) {
        return costLayerRepository.findFirstByProductIdAndBranchIdOrderByCreatedAtDesc(productId, branchId)
                .map(CostLayer::getUnitCost)
                .orElse(null);
    }

    /** Lỗi không đủ hàng kèm mã + tên sản phẩm để nhân viên biết dòng nào thiếu. */
    private StockInsufficientException insufficientStock(Long productId, Long branchId, BigDecimal available, BigDecimal requested) {
        String label = "sản phẩm #" + productId;
        try {
            CatalogFacade.ProductBasicInfo info = catalogFacade.getProductBasicInfo(java.util.List.of(productId)).get(productId);
            if (info != null) label = "[" + info.getCode() + "] " + info.getName();
        } catch (RuntimeException e) {
            log.warn("Không lấy được tên sản phẩm {}: {}", productId, e.getMessage());
        }
        return new StockInsufficientException(label, productId, branchId, available, requested);
    }
}
