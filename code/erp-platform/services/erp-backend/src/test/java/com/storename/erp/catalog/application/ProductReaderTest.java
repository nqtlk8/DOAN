package com.storename.erp.catalog.application;

import com.storename.erp.catalog.application.dto.ProductResponseDto;
import com.storename.erp.catalog.domain.Product;
import com.storename.erp.catalog.infrastructure.PriceListRepository;
import com.storename.erp.catalog.infrastructure.ProductRepository;
import com.storename.erp.common.security.JwtAuthDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductReaderTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PriceListRepository priceListRepository;

    @InjectMocks
    private ProductReader productReader;

    private Product mockProduct;

    @BeforeEach
    void setUp() {
        mockProduct = Product.builder()
                .id(10L)
                .code("PRD-01")
                .name("Test Product")
                .isActive(true)
                .build();
    }

    @Test
    void testAdminNullBranchId_ReturnsProductsWithoutException() {
        // Mock SecurityContext with null branchId (Admin)
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("admin", null);
        auth.setDetails(new JwtAuthDetails(null, "token123"));
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(productRepository.findAll()).thenReturn(Collections.singletonList(mockProduct));

        List<ProductResponseDto> results = productReader.getAllProductsWithBranchPrice();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(10L, results.get(0).getId());
        assertNull(results.get(0).getPrice());
        
        verify(priceListRepository, never()).findByProductIdAndBranchId(anyLong(), anyLong());
    }
}
