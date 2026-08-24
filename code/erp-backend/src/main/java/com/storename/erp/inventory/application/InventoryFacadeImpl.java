package com.storename.erp.inventory.application;

import com.storename.erp.inventory.api.InventoryFacade;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class InventoryFacadeImpl implements InventoryFacade {

    private final StockOnHandRepository stockOnHandRepository;

    public InventoryFacadeImpl(StockOnHandRepository stockOnHandRepository) {
        this.stockOnHandRepository = stockOnHandRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAvailableQuantity(UUID productId, UUID branchId) {
        return stockOnHandRepository.findByProductIdAndBranchId(productId, branchId)
                .map(StockOnHand::getQuantity)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAverageCost(UUID productId, UUID branchId) {
        return stockOnHandRepository.findByProductIdAndBranchId(productId, branchId)
                .map(StockOnHand::getAvgCost)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public boolean reserveStock(UUID productId, UUID branchId, BigDecimal quantity, UUID referenceId) {
        // Dự phòng cho Sprint 2.2+, hiện tại luôn trả về true
        return true;
    }
}
