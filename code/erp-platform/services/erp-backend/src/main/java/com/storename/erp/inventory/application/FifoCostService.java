package com.storename.erp.inventory.application;

import com.storename.erp.inventory.domain.CostLayer;
import com.storename.erp.inventory.infrastructure.CostLayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

/**
 * Tính giá vốn FIFO và tiêu thụ CostLayer khi bán hàng.
 * Gọi bên trong @Transactional của caller — không tự mở transaction riêng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FifoCostService {

    private final CostLayerRepository costLayerRepo;

    @Transactional(propagation = Propagation.MANDATORY)
    public FifoResult consume(Long productId, Long branchId, BigDecimal neededQty) {
        log.info("Consuming FIFO cost layer for product={}, branch={}, neededQty={}", productId, branchId, neededQty);
        List<CostLayer> layers = costLayerRepo.findAvailableForFifoWithLock(productId, branchId);
        BigDecimal remaining = neededQty;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalConsumed = BigDecimal.ZERO;
        List<ConsumedEntry> consumed = new ArrayList<>();

        for (CostLayer layer : layers) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal taken = layer.consume(remaining);
            remaining = remaining.subtract(taken);
            totalCost = totalCost.add(taken.multiply(layer.getUnitCost()));
            totalConsumed = totalConsumed.add(taken);
            consumed.add(new ConsumedEntry(layer, taken));
            costLayerRepo.save(layer);
        }

        String costBasis = remaining.compareTo(BigDecimal.ZERO) > 0 ? "NO_LAYER" : "NORMAL";
        BigDecimal snapshot = totalConsumed.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : totalCost.divide(totalConsumed, 4, RoundingMode.HALF_UP);

        if ("NO_LAYER".equals(costBasis))
            log.warn("Negative stock: no cost layer for product={} branch={}", productId, branchId);

        return new FifoResult(snapshot, costBasis, consumed, remaining);
    }

    public record FifoResult(BigDecimal unitCostSnapshot, String costBasis,
                             List<ConsumedEntry> consumedEntries, BigDecimal unconsumedQty) {}
    public record ConsumedEntry(CostLayer layer, BigDecimal qty) {}
}
