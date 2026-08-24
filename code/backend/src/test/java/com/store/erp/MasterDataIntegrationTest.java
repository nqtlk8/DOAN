package com.store.erp;

import com.store.erp.entity.Customer;
import com.store.erp.entity.Product;
import com.store.erp.repos.CustomerRepository;
import com.store.erp.repos.InventoryTransactionRepository;
import com.store.erp.repos.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class MasterDataIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerRepository customerRepository;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private InventoryTransactionRepository inventoryTransactionRepository;

    @BeforeEach
    void setUp() {
        Customer customer1 = new Customer();
        customer1.setId(UUID.randomUUID());
        customer1.setName("Acme Corp");
        customer1.setEmail("contact@acme.com");
        customer1.setPhone("123456789");

        Customer customer2 = new Customer();
        customer2.setId(UUID.randomUUID());
        customer2.setName("Global Tech");

        when(customerRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(customer1, customer2)));
        
        when(customerRepository.findByNameContainingIgnoreCase(eq("Acme"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(customer1)));
        
        when(customerRepository.findByNameContainingIgnoreCase(eq("Unknown"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setCode("PROD-1001");
        product.setName("Office Chair");
        product.setPrice(new BigDecimal("150.00"));

        when(productRepository.searchProducts(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));
                
        when(productRepository.searchProducts(eq("PROD-1001"), eq("Chair"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));

        when(inventoryTransactionRepository.getStockQuantityByProductId(any()))
                .thenReturn(50);
    }

    // TC-MD-01
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetCustomers_NoFilter() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    // TC-MD-02
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetCustomers_WithFilter() throws Exception {
        mockMvc.perform(get("/api/customers").param("name", "Acme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("Acme Corp"));
        
        mockMvc.perform(get("/api/customers").param("name", "Unknown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    // TC-MD-03
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetProducts_CheckStockAndPrice() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].stockQuantity").value(50))
                .andExpect(jsonPath("$.data.items[0].basePrice").value(150.00));
    }

    // TC-MD-04
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetProducts_WithFilter() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("code", "PROD-1001")
                .param("name", "Chair"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].code").value("PROD-1001"));
    }
}
