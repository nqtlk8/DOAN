package com.storename.erp.inventory.application;

import com.storename.erp.inventory.domain.CostLayer;
import com.storename.erp.inventory.domain.StockInsufficientException;
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
 * Không cho tồn âm: nếu các lớp còn lại không đủ số lượng cần xuất thì ném
 * {@link StockInsufficientException} (caller rollback, không lớp nào bị trừ).
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
        BigDecimal layerQty = layers.stream().map(CostLayer::getRemainingQty).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (layerQty.compareTo(neededQty) < 0) {
            // Tồn kho (stock_on_hand) và lớp giá vốn phải khớp nhau; thiếu lớp = không đủ hàng để xuất.
            throw new StockInsufficientException(productId, branchId, layerQty, neededQty);
        }
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

        BigDecimal snapshot = totalConsumed.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : totalCost.divide(totalConsumed, 4, RoundingMode.HALF_UP);

        return new FifoResult(snapshot, costBasisOf(consumed), consumed, remaining);
    }

    /** Giá vốn lấy từ lớp có nguồn khác nhau (vd nhập hàng và hàng trả) thì ghi {@value #MIXED}. */
    public static final String MIXED = "MIXED";

    /**
     * Nguồn giá vốn của lần xuất: nguồn chung của mọi lớp đã tiêu thụ (NORMAL hoặc RETURN),
     * hoặc {@value #MIXED} nếu lấy từ nhiều nguồn. Không tiêu thụ lớp nào thì NORMAL.
     */
    static String costBasisOf(List<ConsumedEntry> consumed) {
        java.util.Set<String> bases = new java.util.TreeSet<>();
        for (ConsumedEntry e : consumed) {
            if (e.qty().compareTo(BigDecimal.ZERO) > 0) bases.add(e.layer().getCostBasis());
        }
        if (bases.isEmpty()) return "NORMAL";
        return bases.size() == 1 ? bases.iterator().next() : MIXED;
    }

    public record FifoResult(BigDecimal unitCostSnapshot, String costBasis,
                             List<ConsumedEntry> consumedEntries, BigDecimal unconsumedQty) {}
    public record ConsumedEntry(CostLayer layer, BigDecimal qty) {}
}
