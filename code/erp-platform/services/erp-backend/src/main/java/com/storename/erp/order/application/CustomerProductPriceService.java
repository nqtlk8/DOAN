package com.storename.erp.order.application;

import com.storename.erp.catalog.api.CatalogFacade;
import com.storename.erp.order.application.dto.ResolvedPriceDto;
import com.storename.erp.order.domain.CustomerProductPrice;
import com.storename.erp.order.domain.CustomerProductPriceHistory;
import com.storename.erp.order.domain.PriceSource;
import com.storename.erp.order.domain.SalesInvoice;
import com.storename.erp.order.domain.SalesInvoiceLine;
import com.storename.erp.order.infrastructure.CustomerProductPriceHistoryRepository;
import com.storename.erp.order.infrastructure.CustomerProductPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Xác định giá bán theo khách và lưu giá riêng của khách.
 *
 * <p>Thứ tự ưu tiên khi xác định giá (cho khách, sản phẩm, chi nhánh):
 * <ol>
 *   <li>Giá riêng của khách ({@code customer_product_price}) còn hiệu lực</li>
 *   <li>Giá niêm yết chi nhánh ({@code price_list}) đang hiệu lực</li>
 *   <li>Không có giá ({@link PriceSource#NONE}) — nhân viên nhập tay</li>
 * </ol>
 *
 * <p>Khi xác nhận hóa đơn, {@link #applyFromInvoice} ghi giá của các dòng khác giá đang áp dụng
 * vào {@code customer_product_price} để đơn sau của khách tự dùng lại.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerProductPriceService {

    private final CustomerProductPriceRepository priceRepository;
    private final CustomerProductPriceHistoryRepository historyRepository;
    private final CatalogFacade catalogFacade;

    /** Giá riêng còn hiệu lực của khách cho một sản phẩm (không tính giá niêm yết). */
    @Transactional(readOnly = true)
    public Optional<BigDecimal> getPrice(UUID customerId, Long productId, Long branchId) {
        LocalDateTime now = LocalDateTime.now();
        return priceRepository.findByCustomerIdAndProductIdAndBranchId(customerId, productId, branchId)
                .filter(p -> p.isActiveAt(now))
                .map(CustomerProductPrice::getUnitPrice);
    }

    /** Giá áp dụng cho từng sản phẩm, giữ thứ tự productIds (bỏ trùng, bỏ null). */
    @Transactional(readOnly = true)
    public List<ResolvedPriceDto> resolvePrices(UUID customerId, Long branchId, Collection<Long> productIds) {
        Set<Long> ids = distinctIds(productIds);
        if (ids.isEmpty()) return List.of();

        Map<Long, BigDecimal> listPrices = catalogFacade.getListPrices(branchId, ids);
        Map<Long, CustomerProductPrice> customerPrices = customerId == null ? Map.of()
                : activeCustomerPrices(customerId, branchId, ids, LocalDateTime.now());

        List<ResolvedPriceDto> result = new ArrayList<>(ids.size());
        for (Long productId : ids) {
            result.add(resolve(productId, customerPrices.get(productId), listPrices.get(productId)));
        }
        return result;
    }

    /**
     * Lưu giá riêng của khách từ một hóa đơn đang được xác nhận. Phải chạy trong transaction của
     * việc xác nhận hóa đơn: nếu xác nhận thất bại thì giá cũng không được lưu.
     *
     * <p>Với mỗi sản phẩm (nếu lặp nhiều dòng thì lấy dòng cuối):
     * <ul>
     *   <li>Khách đã có giá riêng còn hiệu lực và giá mới khác → cập nhật</li>
     *   <li>Khách chưa có giá riêng (hoặc đã hết hạn) và giá mới khác giá niêm yết (hoặc không có giá niêm yết) → tạo / kích hoạt lại</li>
     *   <li>Còn lại (giá mới = giá đang áp dụng) → không ghi</li>
     * </ul>
     * Mỗi lần ghi sinh 1 dòng {@code customer_product_price_history}.
     *
     * @return số sản phẩm đã được tạo/cập nhật giá riêng
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int applyFromInvoice(SalesInvoice invoice, UUID userId) {
        if (invoice.getCustomerId() == null || invoice.getLines() == null || invoice.getLines().isEmpty()) return 0;

        // productId -> giá của dòng cuối cùng chứa sản phẩm đó
        Map<Long, BigDecimal> invoicePrices = new LinkedHashMap<>();
        for (SalesInvoiceLine line : invoice.getLines()) {
            if (line.getProductId() == null || line.getUnitPrice() == null) continue;
            invoicePrices.remove(line.getProductId());
            invoicePrices.put(line.getProductId(), line.getUnitPrice());
        }
        if (invoicePrices.isEmpty()) return 0;

        UUID customerId = invoice.getCustomerId();
        Long branchId = invoice.getBranchId();
        LocalDateTime now = LocalDateTime.now();

        Map<Long, BigDecimal> listPrices = catalogFacade.getListPrices(branchId, invoicePrices.keySet());
        Map<Long, CustomerProductPrice> existing = priceRepository
                .findByCustomerIdAndBranchIdAndProductIdIn(customerId, branchId, invoicePrices.keySet())
                .stream()
                .collect(Collectors.toMap(CustomerProductPrice::getProductId, Function.identity(), (a, b) -> a));

        List<CustomerProductPrice> toSave = new ArrayList<>();
        List<CustomerProductPriceHistory> history = new ArrayList<>();

        invoicePrices.forEach((productId, newPrice) -> {
            CustomerProductPrice current = existing.get(productId);
            BigDecimal listPrice = listPrices.get(productId);
            boolean hasActive = current != null && current.isActiveAt(now);

            BigDecimal appliedPrice = hasActive ? current.getUnitPrice() : listPrice;
            if (appliedPrice != null && appliedPrice.compareTo(newPrice) == 0) return; // không đổi giá

            CustomerProductPrice target = current;
            if (target == null) {
                target = new CustomerProductPrice();
                target.setCustomerId(customerId);
                target.setProductId(productId);
                target.setBranchId(branchId);
            }
            BigDecimal oldPrice = hasActive ? current.getUnitPrice() : null;

            target.setUnitPrice(newPrice);
            target.setEffectiveFrom(now);
            target.setEffectiveTo(null);
            target.setDeleted(false);
            target.setSourceInvoiceId(invoice.getId());
            toSave.add(target);

            history.add(CustomerProductPriceHistory.of(customerId, productId, branchId,
                    oldPrice, newPrice, listPrice, invoice.getId(), userId, now));
        });

        if (toSave.isEmpty()) return 0;
        priceRepository.saveAll(toSave);
        historyRepository.saveAll(history);
        log.info("Invoice {}: saved {} customer price(s) for customer {} at branch {}",
                invoice.getInvoiceCode(), toSave.size(), customerId, branchId);
        return toSave.size();
    }

    private Map<Long, CustomerProductPrice> activeCustomerPrices(UUID customerId, Long branchId,
                                                                 Collection<Long> productIds, LocalDateTime now) {
        return priceRepository.findByCustomerIdAndBranchIdAndProductIdIn(customerId, branchId, productIds).stream()
                .filter(p -> p.isActiveAt(now))
                .collect(Collectors.toMap(CustomerProductPrice::getProductId, Function.identity(), (a, b) -> a));
    }

    private static ResolvedPriceDto resolve(Long productId, CustomerProductPrice customerPrice, BigDecimal listPrice) {
        if (customerPrice != null) {
            return new ResolvedPriceDto(productId, customerPrice.getUnitPrice(), PriceSource.CUSTOMER,
                    listPrice, customerPrice.getUnitPrice());
        }
        if (listPrice != null) {
            return new ResolvedPriceDto(productId, listPrice, PriceSource.LIST, listPrice, null);
        }
        return new ResolvedPriceDto(productId, null, PriceSource.NONE, null, null);
    }

    private static Set<Long> distinctIds(Collection<Long> productIds) {
        Set<Long> ids = new LinkedHashSet<>();
        if (productIds != null) {
            for (Long id : productIds) if (id != null) ids.add(id);
        }
        return ids;
    }
}
