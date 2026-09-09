package com.storename.erp.inventory.application;

import com.storename.erp.inventory.api.InventoryFacade;
import com.storename.erp.inventory.domain.CostLayer;
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
     * Xác nhận giữ chỗ tồn kho (Reservation).
     */
    @Override
    @Transactional
    public boolean reserveStock(Long productId, Long branchId, BigDecimal quantity, UUID referenceId) {
        log.debug("Reserving stock for product {} at branch {} ref {}", productId, branchId, referenceId);
        // Dự phòng cho Sprint 2.2+, hiện tại luôn trả về true
        return true;
    }

    /**
     * Ghi nhận xuất kho bán hàng (SALE): Tiêu thụ FIFO, giảm tồn kho, ghi nhận movement.
     */
    @Override
    @Transactional
    public SaleCostResult recordSaleAndGetCost(Long productId, Long branchId, BigDecimal quantity, String invoiceId, UUID lineId, UUID userId) {
        log.info("Recording sale for product {} branch {} qty {} invoice {}", productId, branchId, quantity, invoiceId);

        // 1. FIFO consume → lấy unit_cost_snapshot
        FifoCostService.FifoResult fifo = fifoCostService.consume(productId, branchId, quantity);

        // 2. stock_on_hand
        StockOnHand stock = stockOnHandRepository.findByProductIdAndBranchId(productId, branchId)
                .orElseGet(() -> stockOnHandRepository.save(new StockOnHand(productId, branchId)));
        stock.decreaseAllowNegative(quantity, "SALE");
        stockOnHandRepository.save(stock);

        // 3. stock_movement
        stockMovementRepository.save(StockMovement.sale(productId, branchId, quantity, invoiceId, lineId, userId));

        return new SaleCostResult(fifo.unitCostSnapshot(), fifo.costBasis());
    }

    /**
     * Ghi nhận nhập kho do khách trả hàng (RETURN): Tăng tồn kho, ghi nhận movement, tạo cost layer.
     */
    @Override
    @Transactional
    public void recordReturn(Long productId, Long branchId, BigDecimal quantity, BigDecimal returnPrice, String returnId, UUID lineId, UUID userId) {
        log.info("Recording return for product {} branch {} qty {} returnId {}", productId, branchId, quantity, returnId);

        StockOnHand stock = stockOnHandRepository.findByProductIdAndBranchId(productId, branchId)
                .orElseGet(() -> stockOnHandRepository.save(new StockOnHand(productId, branchId)));
        stock.increase(quantity, "RETURN");
        stockOnHandRepository.save(stock);

        StockMovement mv = StockMovement.returnGoods(productId, branchId, quantity, returnId, lineId, userId);
        mv = stockMovementRepository.save(mv);

        costLayerRepository.save(CostLayer.fromReturn(productId, branchId, quantity, returnPrice, mv.getId()));
    }
}
