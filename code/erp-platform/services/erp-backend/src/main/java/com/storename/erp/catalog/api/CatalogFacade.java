package com.storename.erp.catalog.api;

import com.storename.erp.catalog.domain.PriceList;
import com.storename.erp.catalog.infrastructure.PriceListRepository;
import com.storename.erp.catalog.infrastructure.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import lombok.Builder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogFacade {
    private final ProductRepository productRepository;
    private final PriceListRepository priceListRepository;

    @Data
    @Builder
    public static class ProductBasicInfo {
        private String code;
        private String name;
    }

    public Map<Long, ProductBasicInfo> getProductBasicInfo(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return Map.of();
        return productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(
                        com.storename.erp.catalog.domain.Product::getId,
                        p -> ProductBasicInfo.builder()
                                .code(p.getCode())
                                .name(p.getName())
                                .build()
                ));
    }

    /**
     * Giá niêm yết đang hiệu lực của chi nhánh cho các sản phẩm: productId -> price.
     * Mỗi sản phẩm lấy dòng price_list có effective_date <= now mới nhất (bỏ qua giá hẹn ngày tương lai).
     * Sản phẩm không có giá thì không có trong map. Không ném lỗi khi price_list có nhiều dòng cho cùng cặp.
     */
    public Map<Long, BigDecimal> getListPrices(Long branchId, Collection<Long> productIds) {
        if (branchId == null || productIds == null || productIds.isEmpty()) return Map.of();
        OffsetDateTime now = OffsetDateTime.now();
        Map<Long, PriceList> latest = new HashMap<>();
        for (PriceList p : priceListRepository.findByBranchIdAndProductIdIn(branchId, productIds)) {
            if (p.getEffectiveDate() != null && p.getEffectiveDate().isAfter(now)) continue;
            Long productId = p.getProduct().getId();
            latest.merge(productId, p, (a, b) -> byEffectiveDate().compare(a, b) >= 0 ? a : b);
        }
        Map<Long, BigDecimal> result = new HashMap<>();
        latest.forEach((productId, p) -> result.put(productId, p.getPrice()));
        return result;
    }

    private static Comparator<PriceList> byEffectiveDate() {
        return Comparator.comparing(PriceList::getEffectiveDate, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(PriceList::getId, Comparator.nullsFirst(Comparator.naturalOrder()));
    }
}
