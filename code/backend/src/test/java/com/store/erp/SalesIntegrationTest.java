package com.store.erp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.erp.dto.QuotationItemRequest;
import com.store.erp.dto.QuotationRequest;
import com.store.erp.dto.SalesOrderItemRequest;
import com.store.erp.dto.SalesOrderRequest;
import com.store.erp.entity.Customer;
import com.store.erp.entity.InventoryTransaction;
import com.store.erp.entity.Product;
import com.store.erp.entity.Quotation;
import com.store.erp.entity.SalesOrder;
import com.store.erp.repos.CustomerRepository;
import com.store.erp.repos.InventoryTransactionRepository;
import com.store.erp.repos.ProductRepository;
import com.store.erp.repos.QuotationRepository;
import com.store.erp.repos.SalesOrderRepository;
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
public class SalesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerRepository customerRepository;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private QuotationRepository quotationRepository;

    @MockBean
    private SalesOrderRepository salesOrderRepository;

    @MockBean
    private InventoryTransactionRepository inventoryTransactionRepository;

    private UUID validCustomerId = UUID.randomUUID();
    private UUID validProductId = UUID.randomUUID();
    
    private AtomicInteger mockStock;

    @BeforeEach
    void setUp() {
        mockStock = new AtomicInteger(50);
        
        Customer customer = new Customer();
        customer.setId(validCustomerId);
        customer.setName("Test Customer");

        Product product = new Product();
        product.setId(validProductId);
        product.setCode("PROD-1001");
        product.setName("Test Product");
        product.setPrice(new BigDecimal("150.00"));

        when(customerRepository.findById(validCustomerId)).thenReturn(Optional.of(customer));
        when(productRepository.findById(validProductId)).thenReturn(Optional.of(product));
        when(productRepository.searchProducts(eq("PROD-1001"), eq(null), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(product)));
        
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(invocation -> {
            Quotation q = invocation.getArgument(0);
            q.setId(UUID.randomUUID());
            return q;
        });

        when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(invocation -> {
            SalesOrder so = invocation.getArgument(0);
            so.setId(UUID.randomUUID());
            return so;
        });
        
        when(inventoryTransactionRepository.save(any(InventoryTransaction.class))).thenAnswer(invocation -> {
            InventoryTransaction tx = invocation.getArgument(0);
            if ("SALES_OUT".equals(tx.getTransactionType())) {
                mockStock.addAndGet(-tx.getQuantity());
            }
            return tx;
        });
        
        when(inventoryTransactionRepository.getStockQuantityByProductId(any()))
                .thenAnswer(inv -> mockStock.get());
    }

    // TC-QT-01
    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateQuotation_Success() throws Exception {
        QuotationRequest req = new QuotationRequest();
        req.setCustomerId(validCustomerId);
        req.setValidUntil(LocalDate.parse("2026-12-31"));
        req.setRemarks("Special discount");
        
        QuotationItemRequest item = new QuotationItemRequest();
        item.setProductId(validProductId);
        item.setQuantity(10);
        item.setUnitPrice(new BigDecimal("140.00"));
        item.setDiscountPercent(new BigDecimal("5.0"));
        
        req.setItems(List.of(item));

        mockMvc.perform(post("/api/quotations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.quotationCode").exists());
    }

    // TC-QT-02: Missing customerId
    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateQuotation_Fail_MissingCustomerId() throws Exception {
        QuotationRequest req = new QuotationRequest();
        req.setValidUntil(LocalDate.parse("2026-12-31"));
        
        QuotationItemRequest item = new QuotationItemRequest();
        item.setProductId(validProductId);
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("100.00"));
        
        req.setItems(List.of(item));

        mockMvc.perform(post("/api/quotations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // TC-QT-03: Empty items
    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateQuotation_Fail_EmptyItems() throws Exception {
        QuotationRequest req = new QuotationRequest();
        req.setCustomerId(validCustomerId);
        req.setValidUntil(LocalDate.parse("2026-12-31"));
        req.setItems(new ArrayList<>());

        mockMvc.perform(post("/api/quotations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // TC-QT-04: Negative quantity
    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateQuotation_Fail_NegativeQuantity() throws Exception {
        QuotationRequest req = new QuotationRequest();
        req.setCustomerId(validCustomerId);
        req.setValidUntil(LocalDate.parse("2026-12-31"));
        
        QuotationItemRequest item = new QuotationItemRequest();
        item.setProductId(validProductId);
        item.setQuantity(-5);
        item.setUnitPrice(new BigDecimal("140.00"));
        
        req.setItems(List.of(item));

        mockMvc.perform(post("/api/quotations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // TC-SO-01: E2E Create Sales Order and check inventory
    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateSalesOrder_Success_And_InventoryDeducted() throws Exception {
        // Giao dịch 1: Check initial stock
        mockMvc.perform(get("/api/products").param("code", "PROD-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].stockQuantity").value(50));
        
        // Giao dịch 2: Create Sales Order
        SalesOrderRequest req = new SalesOrderRequest();
        req.setCustomerId(validCustomerId);
        req.setExpectedDeliveryDate(LocalDate.parse("2026-08-01"));
        
        SalesOrderItemRequest item = new SalesOrderItemRequest();
        item.setProductId(validProductId);
        item.setQuantity(10);
        item.setUnitPrice(new BigDecimal("133.00"));
        
        req.setItems(List.of(item));

        mockMvc.perform(post("/api/sales-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
        
        // Giao dịch 3: Check updated stock (50 - 10 = 40)
        mockMvc.perform(get("/api/products").param("code", "PROD-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].stockQuantity").value(40));
    }

    // TC-SO-02: Missing items
    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateSalesOrder_Fail_MissingItems() throws Exception {
        SalesOrderRequest req = new SalesOrderRequest();
        req.setCustomerId(validCustomerId);
        req.setExpectedDeliveryDate(LocalDate.parse("2026-08-01"));
        
        mockMvc.perform(post("/api/sales-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // TC-SO-03: Insufficient stock
    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateSalesOrder_Fail_InsufficientStock() throws Exception {
        SalesOrderRequest req = new SalesOrderRequest();
        req.setCustomerId(validCustomerId);
        req.setExpectedDeliveryDate(LocalDate.parse("2026-08-01"));
        
        SalesOrderItemRequest item = new SalesOrderItemRequest();
        item.setProductId(validProductId);
        item.setQuantity(1000);
        item.setUnitPrice(new BigDecimal("133.00"));
        
        req.setItems(List.of(item));

        mockMvc.perform(post("/api/sales-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest()); // Or 409
    }
}
