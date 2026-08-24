package com.store.erp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.erp.dto.PurchaseOrderItemRequest;
import com.store.erp.dto.PurchaseOrderRequest;
import com.store.erp.entity.Distributor;
import com.store.erp.entity.InventoryTransaction;
import com.store.erp.entity.Product;
import com.store.erp.entity.PurchaseOrder;
import com.store.erp.repos.DistributorRepository;
import com.store.erp.repos.InventoryTransactionRepository;
import com.store.erp.repos.ProductRepository;
import com.store.erp.repos.PurchaseOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PurchasingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DistributorRepository distributorRepository;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private PurchaseOrderRepository purchaseOrderRepository;

    @MockBean
    private InventoryTransactionRepository inventoryTransactionRepository;

    private UUID validDistributorId = UUID.randomUUID();
    private UUID validProductId = UUID.randomUUID();
    
    private AtomicInteger mockStock;

    @BeforeEach
    void setUp() {
        mockStock = new AtomicInteger(50);
        
        Distributor distributor = new Distributor();
        distributor.setId(validDistributorId);
        distributor.setName("Test Distributor");

        Product product = new Product();
        product.setId(validProductId);
        product.setCode("PROD-1001");
        product.setName("Test Product");
        product.setPrice(new BigDecimal("150.00"));

        when(distributorRepository.findById(validDistributorId)).thenReturn(Optional.of(distributor));
        when(productRepository.findById(validProductId)).thenReturn(Optional.of(product));
        when(productRepository.searchProducts(eq("PROD-1001"), eq(null), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(product)));
        
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> {
            PurchaseOrder po = invocation.getArgument(0);
            po.setId(UUID.randomUUID());
            return po;
        });

        when(inventoryTransactionRepository.save(any(InventoryTransaction.class))).thenAnswer(invocation -> {
            InventoryTransaction tx = invocation.getArgument(0);
            if ("PURCHASE_IN".equals(tx.getTransactionType())) {
                mockStock.addAndGet(tx.getQuantity());
            }
            return tx;
        });
        
        when(inventoryTransactionRepository.getStockQuantityByProductId(any()))
                .thenAnswer(inv -> mockStock.get());
    }

    // TC-PO-01: E2E Create Purchase Order and check inventory
    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreatePurchaseOrder_Success_And_InventoryAdded() throws Exception {
        // Giao dịch 1: Check initial stock
        mockMvc.perform(get("/api/products").param("code", "PROD-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].stockQuantity").value(50));
        
        // Giao dịch 2: Create Purchase Order
        PurchaseOrderRequest req = new PurchaseOrderRequest();
        req.setDistributorId(validDistributorId);
        req.setOrderDate(LocalDate.parse("2026-08-01"));
        req.setNotes("First restock");
        
        PurchaseOrderItemRequest item = new PurchaseOrderItemRequest();
        item.setProductId(validProductId);
        item.setQuantity(20);
        item.setUnitPrice(new BigDecimal("100.00"));
        
        req.setItems(List.of(item));

        mockMvc.perform(post("/api/purchase-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
        
        // Giao dịch 3: Check updated stock (50 + 20 = 70)
        mockMvc.perform(get("/api/products").param("code", "PROD-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].stockQuantity").value(70));
    }

    // TC-PO-02: Missing items
    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreatePurchaseOrder_Fail_MissingItems() throws Exception {
        PurchaseOrderRequest req = new PurchaseOrderRequest();
        req.setDistributorId(validDistributorId);
        req.setOrderDate(LocalDate.parse("2026-08-01"));
        req.setItems(new ArrayList<>());
        
        mockMvc.perform(post("/api/purchase-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // TC-PO-03: Invalid Distributor
    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreatePurchaseOrder_Fail_MissingDistributorId() throws Exception {
        PurchaseOrderRequest req = new PurchaseOrderRequest();
        req.setOrderDate(LocalDate.parse("2026-08-01"));
        
        PurchaseOrderItemRequest item = new PurchaseOrderItemRequest();
        item.setProductId(validProductId);
        item.setQuantity(20);
        item.setUnitPrice(new BigDecimal("100.00"));
        
        req.setItems(List.of(item));
        
        mockMvc.perform(post("/api/purchase-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
