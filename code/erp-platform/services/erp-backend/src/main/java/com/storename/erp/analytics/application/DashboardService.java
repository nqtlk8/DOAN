package com.storename.erp.analytics.application;

import com.storename.erp.analytics.AnalyticsProperties;
import com.storename.erp.analytics.api.dto.DashboardMetricsDto;
import com.storename.erp.analytics.api.dto.ProductPerformanceDto;
import com.storename.erp.analytics.api.dto.SlowMovingProductDto;
import com.storename.erp.analytics.application.port.AnalyticsDataPort;
import com.storename.erp.analytics.domain.SalesTotals;
import com.storename.erp.analytics.domain.StockLevel;
import com.storename.erp.catalog.api.CatalogFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {
    
    private final AnalyticsDataPort analyticsDataPort;
    private final AnalyticsProperties analyticsProperties;
    private final CatalogFacade catalogFacade;
    
    /** Số dòng mặc định của bảng top sản phẩm. */
    public static final int DEFAULT_TOP_LIMIT = 10;
    /** Lựa chọn hợp lệ của bảng top sản phẩm; 0 = tất cả (giới hạn an toàn {@link #MAX_TOP_PRODUCTS}). */
    public static final Set<Integer> ALLOWED_TOP_LIMITS = Set.of(0, 10, 20, 50, 100);
    public static final int MAX_TOP_PRODUCTS = 1000;

    @Transactional(readOnly = true)
    public DashboardMetricsDto getDashboardMetrics(Long branchId, Integer startDateKey, Integer endDateKey) {
        return getDashboardMetrics(branchId, startDateKey, endDateKey, DEFAULT_TOP_LIMIT);
    }

    @Transactional(readOnly = true)
    public DashboardMetricsDto getDashboardMetrics(Long branchId, Integer startDateKey, Integer endDateKey, Integer topLimit) {
        log.info("Fetching dashboard metrics for branch: {}, startDate: {}, endDate: {}, top: {}", branchId, startDateKey, endDateKey, topLimit);
        int top = topLimit == null ? DEFAULT_TOP_LIMIT : topLimit;
        if (!ALLOWED_TOP_LIMITS.contains(top)) {
            throw new IllegalArgumentException("topLimit chỉ nhận 10, 20, 50, 100 hoặc 0 (tất cả)");
        }
        
        if (startDateKey > endDateKey) {
            throw new IllegalArgumentException("startDateKey must be less than or equal to endDateKey");
        }
        
        LocalDateTime fromTs;
        LocalDateTime toTs;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(java.time.format.ResolverStyle.STRICT);
            LocalDate startDate = LocalDate.parse(String.valueOf(startDateKey), formatter);
            LocalDate endDate = LocalDate.parse(String.valueOf(endDateKey), formatter);
            
            fromTs = startDate.atStartOfDay(analyticsProperties.getBusinessZone())
                .withZoneSameInstant(analyticsProperties.getStorageZone())
                .toLocalDateTime();
            toTs = endDate.plusDays(1).atStartOfDay(analyticsProperties.getBusinessZone())
                .withZoneSameInstant(analyticsProperties.getStorageZone())
                .toLocalDateTime();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format for startDateKey or endDateKey");
        }

        BigDecimal totalDebt = calculateTotalDebt(branchId);
        SalesTotals salesTotals = analyticsDataPort.getSalesTotals(branchId, fromTs, toTs);
        
        BigDecimal totalRevenue = salesTotals.getRevenue() != null ? salesTotals.getRevenue() : BigDecimal.ZERO;
        BigDecimal cogs = salesTotals.getCogs() != null ? salesTotals.getCogs() : BigDecimal.ZERO;
        BigDecimal grossProfit = totalRevenue.subtract(cogs);

        List<ProductPerformanceDto> topSelling = analyticsDataPort.getTopProductsByRevenue(branchId, fromTs, toTs,
                top == 0 ? MAX_TOP_PRODUCTS : top);

        BigDecimal inventoryValue = analyticsDataPort.getInventoryValue(branchId);
        BigDecimal inventoryTurnover = BigDecimal.ZERO;
        if (inventoryValue != null && inventoryValue.compareTo(BigDecimal.ZERO) > 0) {
            inventoryTurnover = cogs.divide(inventoryValue, 2, RoundingMode.HALF_UP);
        }
        
        List<SlowMovingProductDto> slowMovingProducts = calculateSlowMovingProducts(branchId, fromTs, toTs);
        
        return DashboardMetricsDto.builder()
                .totalRevenue(totalRevenue)
                .grossProfit(grossProfit)
                .inventoryTurnoverRatio(inventoryTurnover)
                .totalReceivableDebt(totalDebt)
                .topSellingProducts(topSelling)
                .slowMovingProducts(slowMovingProducts)
                .build();
    }
    
    private BigDecimal calculateTotalDebt(Long branchId) {
        BigDecimal totalDebt = analyticsDataPort.getTotalReceivableDebt(branchId);
        return totalDebt != null ? totalDebt : BigDecimal.ZERO;
    }
    
    private List<SlowMovingProductDto> calculateSlowMovingProducts(Long branchId, LocalDateTime fromTs, LocalDateTime toTs) {
        List<StockLevel> stockLevels = analyticsDataPort.getStockLevels(branchId);
        List<Long> productIdsSold = analyticsDataPort.getProductIdsSoldInPeriod(branchId, fromTs, toTs);
        Set<Long> soldSet = Set.copyOf(productIdsSold);
        
        // Group stock levels by product ID to sum up across branches if branchId is null
        Map<Long, BigDecimal> currentStockByProduct = stockLevels.stream()
                .collect(Collectors.groupingBy(
                        StockLevel::getProductId,
                        Collectors.mapping(
                                StockLevel::getQuantity,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));
        
        List<Long> slowMovingProductIds = currentStockByProduct.entrySet().stream()
                .filter(entry -> entry.getValue().compareTo(BigDecimal.ZERO) > 0)
                .filter(entry -> !soldSet.contains(entry.getKey()))
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .toList();
                
        Map<Long, CatalogFacade.ProductBasicInfo> productInfoMap = catalogFacade.getProductBasicInfo(slowMovingProductIds);
        
        return slowMovingProductIds.stream().map(productId -> {
            CatalogFacade.ProductBasicInfo info = productInfoMap.get(productId);
            String code = info != null ? info.getCode() : "#" + productId;
            String name = info != null ? info.getName() : "#" + productId;
            return SlowMovingProductDto.builder()
                    .productId(productId)
                    .productCode(code)
                    .productName(name)
                    .currentStock(currentStockByProduct.get(productId))
                    .build();
        }).toList();
    }
}
