package com.storename.erp.analytics.application;

import com.storename.erp.analytics.api.dto.StockAlertDto;
import com.storename.erp.analytics.api.dto.StockAlertSummaryDto;
import com.storename.erp.analytics.application.port.AnalyticsDataPort;
import com.storename.erp.analytics.domain.InventoryAlertConfig;
import com.storename.erp.analytics.domain.StockAlertType;
import com.storename.erp.analytics.domain.StockLevel;
import com.storename.erp.analytics.infrastructure.InventoryAlertConfigRepository;
import com.storename.erp.branch.api.BranchFacade;
import com.storename.erp.catalog.api.CatalogFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tính danh sách cảnh báo tồn kho hiện tại cho dashboard HQ (docs/fix-dashboard/business-rules.md).
 *
 * <p>Quy tắc: tồn âm luôn cảnh báo {@code NEGATIVE_STOCK} kể cả khi chưa cấu hình ngưỡng (D-05);
 * có cấu hình đang bật và {@code 0 <= tồn <= ngưỡng} thì cảnh báo {@code LOW_STOCK} (M-08);
 * cấu hình chưa có movement nào được coi là tồn 0. Mỗi cặp (sản phẩm, chi nhánh) tối đa 1 cảnh báo.</p>
 *
 * <p>Vì sao tính trực tiếp khi gọi API thay vì job định kỳ (D-07, D-08): luôn đúng theo dữ liệu hiện tại và
 * không cần lưu lịch sử. Tên sản phẩm/chi nhánh lấy qua Facade, gọi batch 1 lần, không JOIN SQL chéo context.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockAlertService {

    private final AnalyticsDataPort analyticsDataPort;
    private final InventoryAlertConfigRepository inventoryAlertConfigRepository;
    private final CatalogFacade catalogFacade;
    private final BranchFacade branchFacade;

    @Transactional(readOnly = true)
    public StockAlertSummaryDto getStockAlerts(Long branchId) {
        log.info("Fetching stock alerts for branchId: {}", branchId);

        // 1. levels
        List<StockLevel> levels = analyticsDataPort.getStockLevels(branchId);
        Map<String, BigDecimal> stockMap = new HashMap<>();
        for (StockLevel level : levels) {
            stockMap.put(level.getProductId() + "_" + level.getBranchId(), level.getQuantity());
        }

        // 2. configs
        List<InventoryAlertConfig> configs;
        if (branchId == null) {
            configs = inventoryAlertConfigRepository.findByIsActiveTrue();
        } else {
            configs = inventoryAlertConfigRepository.findByIsActiveTrueAndBranchId(branchId);
        }

        Map<String, InventoryAlertConfig> configMap = new HashMap<>();
        for (InventoryAlertConfig config : configs) {
            configMap.put(config.getProductId() + "_" + config.getBranchId(), config);
        }

        Set<String> processedKeys = new HashSet<>();
        List<StockAlertDto> alerts = new ArrayList<>();
        int negativeCount = 0;
        int lowStockCount = 0;

        Set<Long> productIdsToFetch = new HashSet<>();
        Set<Long> branchIdsToFetch = new HashSet<>();

        // 3. check all stock levels
        for (StockLevel level : levels) {
            String key = level.getProductId() + "_" + level.getBranchId();
            BigDecimal qty = level.getQuantity();
            InventoryAlertConfig config = configMap.get(key);
            BigDecimal threshold = config != null ? config.getMinQuantityThreshold() : null;

            if (qty.compareTo(BigDecimal.ZERO) < 0) {
                alerts.add(createAlert(level.getProductId(), level.getBranchId(), qty, threshold, StockAlertType.NEGATIVE_STOCK));
                negativeCount++;
                productIdsToFetch.add(level.getProductId());
                branchIdsToFetch.add(level.getBranchId());
            } else if (config != null && qty.compareTo(config.getMinQuantityThreshold()) <= 0) {
                alerts.add(createAlert(level.getProductId(), level.getBranchId(), qty, threshold, StockAlertType.LOW_STOCK));
                lowStockCount++;
                productIdsToFetch.add(level.getProductId());
                branchIdsToFetch.add(level.getBranchId());
            }
            processedKeys.add(key);
        }

        // 4. check remaining configs (qty = 0)
        for (InventoryAlertConfig config : configs) {
            String key = config.getProductId() + "_" + config.getBranchId();
            if (!processedKeys.contains(key)) {
                if (config.getMinQuantityThreshold().compareTo(BigDecimal.ZERO) >= 0) {
                    alerts.add(createAlert(config.getProductId(), config.getBranchId(), BigDecimal.ZERO, config.getMinQuantityThreshold(), StockAlertType.LOW_STOCK));
                    lowStockCount++;
                    productIdsToFetch.add(config.getProductId());
                    branchIdsToFetch.add(config.getBranchId());
                }
            }
        }

        // 6. enrich names
        Map<Long, CatalogFacade.ProductBasicInfo> productInfoMap = catalogFacade.getProductBasicInfo(productIdsToFetch);
        Map<Long, String> branchNameMap = branchFacade.getBranchNames(branchIdsToFetch);

        for (StockAlertDto alert : alerts) {
            CatalogFacade.ProductBasicInfo productInfo = productInfoMap.get(alert.getProductId());
            if (productInfo != null) {
                alert.setProductCode(productInfo.getCode());
                alert.setProductName(productInfo.getName());
            } else {
                alert.setProductCode("#" + alert.getProductId());
                alert.setProductName("#" + alert.getProductId());
            }

            String branchName = branchNameMap.get(alert.getBranchId());
            alert.setBranchName(branchName != null ? branchName : "#" + alert.getBranchId());
        }

        // sort
        alerts.sort((a, b) -> {
            if (a.getAlertType() != b.getAlertType()) {
                return a.getAlertType() == StockAlertType.NEGATIVE_STOCK ? -1 : 1;
            }
            return a.getCurrentQuantity().compareTo(b.getCurrentQuantity());
        });

        return StockAlertSummaryDto.builder()
                .negativeCount(negativeCount)
                .lowStockCount(lowStockCount)
                .generatedAt(OffsetDateTime.now())
                .alerts(alerts)
                .build();
    }

    private StockAlertDto createAlert(Long productId, Long branchId, BigDecimal currentQuantity, BigDecimal threshold, StockAlertType type) {
        StockAlertDto dto = new StockAlertDto();
        dto.setProductId(productId);
        dto.setBranchId(branchId);
        dto.setCurrentQuantity(currentQuantity);
        dto.setMinQuantityThreshold(threshold);
        dto.setAlertType(type);
        return dto;
    }
}
