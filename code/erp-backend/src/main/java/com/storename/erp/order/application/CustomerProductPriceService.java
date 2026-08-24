package com.storename.erp.order.application;

import com.storename.erp.order.domain.CustomerProductPrice;
import com.storename.erp.order.infrastructure.CustomerProductPriceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class CustomerProductPriceService {

    private final CustomerProductPriceRepository priceRepository;

    public CustomerProductPriceService(CustomerProductPriceRepository priceRepository) {
        this.priceRepository = priceRepository;
    }

    @Transactional(readOnly = true)
    public Optional<BigDecimal> getPrice(UUID customerId, UUID productId, UUID branchId) {
        return priceRepository.findByCustomerIdAndProductIdAndBranchId(customerId, productId, branchId)
                .map(CustomerProductPrice::getUnitPrice);
    }
}
