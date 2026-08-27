package com.storename.erp.order.unit;

import com.storename.erp.order.application.CustomerProductPriceService;
import com.storename.erp.order.domain.CustomerProductPrice;
import com.storename.erp.order.infrastructure.CustomerProductPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerProductPriceUnitTest {

    @Mock
    private CustomerProductPriceRepository priceRepo;

    private CustomerProductPriceService priceService;

    @BeforeEach
    void setUp() {
        priceService = new CustomerProductPriceService(priceRepo);
    }

    @Test
    void testGetPrice_WhenExists_ReturnsPrice() {
        UUID customerId = UUID.randomUUID();
        Long productId = (long)(Math.random() * 100000L);
        Long branchId = (long)(Math.random() * 100000L);
        BigDecimal expectedPrice = new BigDecimal("150.00");

        CustomerProductPrice price = new CustomerProductPrice();
        price.setUnitPrice(expectedPrice);

        when(priceRepo.findByCustomerIdAndProductIdAndBranchId(customerId, productId, branchId))
                .thenReturn(Optional.of(price));

        Optional<BigDecimal> result = priceService.getPrice(customerId, productId, branchId);

        assertTrue(result.isPresent());
        assertEquals(expectedPrice, result.get());
    }

    @Test
    void testGetPrice_WhenNotExists_ReturnsEmpty() {
        UUID customerId = UUID.randomUUID();
        Long productId = (long)(Math.random() * 100000L);
        Long branchId = (long)(Math.random() * 100000L);

        when(priceRepo.findByCustomerIdAndProductIdAndBranchId(customerId, productId, branchId))
                .thenReturn(Optional.empty());

        Optional<BigDecimal> result = priceService.getPrice(customerId, productId, branchId);

        assertFalse(result.isPresent());
    }
}
