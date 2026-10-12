package com.storename.erp.catalog.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storename.erp.catalog.domain.Category;
import com.storename.erp.catalog.domain.PriceList;
import com.storename.erp.catalog.domain.Product;
import com.storename.erp.catalog.infrastructure.CategoryRepository;
import com.storename.erp.catalog.infrastructure.PriceListRepository;
import com.storename.erp.catalog.infrastructure.ProductRepository;
import com.storename.erp.common.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Nhóm 2 — Sản phẩm, bảng giá niêm yết và danh mục công khai (instance HQ).
 * Mã test (TC-Pxx) khớp với danh sách test case trong báo cáo kiểm tra nhóm 2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@TestPropertySource(properties = "instance.role=HQ")
class ProductCatalogRulesIT {

    private static final long BRANCH_ID = 1L;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ProductRepository productRepo;
    @Autowired private CategoryRepository categoryRepo;
    @Autowired private PriceListRepository priceListRepo;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String staffToken;
    private Category leaf;

    @BeforeEach
    void setUp() {
        adminToken = "Bearer " + jwtTokenProvider.generateToken(
                UUID.randomUUID().toString(), "ADMIN", "HQ", UUID.randomUUID().toString());
        staffToken = "Bearer " + jwtTokenProvider.generateToken(
                UUID.randomUUID().toString(), "STAFF", String.valueOf(BRANCH_ID), UUID.randomUUID().toString());
        leaf = newCategory("tc-leaf");
    }

    // ---------------------------------------------------------------- Product

    @Test
    @DisplayName("TC-P01: tạo sản phẩm trùng mã -> 409 PRODUCT_CODE_EXISTS")
    void createProduct_duplicateCode_returns409() throws Exception {
        String body = productBody("TC-P01");
        mockMvc.perform(post("/api/v1/catalog/products").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/catalog/products").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0]").value("PRODUCT_CODE_EXISTS"));
    }

    @Test
    @DisplayName("TC-P02: tạo sản phẩm trùng mã của sản phẩm đã ngừng bán -> 409")
    void createProduct_codeOfDiscontinuedProduct_returns409() throws Exception {
        newProduct("TC-P02", false, leaf);

        mockMvc.perform(post("/api/v1/catalog/products").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(productBody("TC-P02")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value("PRODUCT_CODE_EXISTS"));
    }

    @Test
    @DisplayName("TC-P03: JSON sản phẩm dùng khóa isActive, không có khóa active")
    void productJson_usesIsActiveKey() throws Exception {
        Product discontinued = newProduct("TC-P03", false, leaf);

        mockMvc.perform(get("/api/v1/catalog/products/{id}", discontinued.getId()).header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false))
                .andExpect(jsonPath("$.data.active").doesNotExist());
    }

    @Test
    @DisplayName("TC-P04: xem sản phẩm kèm giá chi nhánh với id không tồn tại -> 404")
    void getProductWithBranchPrice_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/products/{id}", 987654321L)
                        .param("withBranchPrice", "true").header("Authorization", staffToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0]").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("TC-P05: danh sách và chi tiết sản phẩm trả đúng giá niêm yết của chi nhánh trong token")
    void branchPrice_listAndDetail_returnBranchPrice() throws Exception {
        Product product = newProduct("TC-P05", true, leaf);
        priceListRepo.saveAndFlush(newPrice(product, BRANCH_ID, "150000"));
        priceListRepo.saveAndFlush(newPrice(product, 2L, "999999")); // giá chi nhánh khác, không được lấy

        String list = mockMvc.perform(get("/api/v1/catalog/products")
                        .param("withBranchPrice", "true").header("Authorization", staffToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode item = findById(objectMapper.readTree(list).get("data"), product.getId());
        assertNotNull(item, "Sản phẩm phải có trong danh sách");
        assertEquals(0, new BigDecimal("150000").compareTo(item.get("price").decimalValue()));

        String detail = mockMvc.perform(get("/api/v1/catalog/products/{id}", product.getId())
                        .param("withBranchPrice", "true").header("Authorization", staffToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(0, new BigDecimal("150000")
                .compareTo(objectMapper.readTree(detail).at("/data/price").decimalValue()));
    }

    @Test
    @DisplayName("TC-P06: không cho lưu hai giá niêm yết cho cùng (sản phẩm, chi nhánh)")
    void priceList_secondPriceForSameProductAndBranch_isRejected() {
        Product product = newProduct("TC-P06", true, leaf);
        priceListRepo.saveAndFlush(newPrice(product, BRANCH_ID, "100000"));

        assertThrows(DataIntegrityViolationException.class,
                () -> priceListRepo.saveAndFlush(newPrice(product, BRANCH_ID, "120000")));
    }

    // ---------------------------------------------------------------- Public catalog

    @Test
    @DisplayName("TC-P07: API công khai không trả sản phẩm ngừng bán")
    void publicList_excludesDiscontinuedProducts() throws Exception {
        newProduct("TC-P07-ON", true, leaf);
        newProduct("TC-P07-OFF", false, leaf);

        mockMvc.perform(get("/api/v1/public/catalog/products").param("categoryId", leaf.getId().toString())
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].code", hasItem("TC-P07-ON")))
                .andExpect(jsonPath("$.data[*].code", not(hasItem("TC-P07-OFF"))));
    }

    @Test
    @DisplayName("TC-P08: API công khai lọc theo danh mục và phân trang")
    void publicList_appliesCategoryFilterAndPaging() throws Exception {
        Category other = newCategory("tc-other");
        newProduct("TC-P08-A", true, leaf);
        newProduct("TC-P08-B", true, leaf);
        newProduct("TC-P08-C", true, leaf);
        newProduct("TC-P08-X", true, other);

        mockMvc.perform(get("/api/v1/public/catalog/products").param("categoryId", leaf.getId().toString())
                        .param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
        mockMvc.perform(get("/api/v1/public/catalog/products").param("categoryId", leaf.getId().toString())
                        .param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
        mockMvc.perform(get("/api/v1/public/catalog/products").param("categoryId", leaf.getId().toString())
                        .param("size", "100"))
                .andExpect(jsonPath("$.data[*].code", hasItems("TC-P08-A", "TC-P08-B", "TC-P08-C")))
                .andExpect(jsonPath("$.data[*].code", not(hasItem("TC-P08-X"))));
    }

    @Test
    @DisplayName("TC-P09: API công khai tìm theo tên hoặc mã")
    void publicList_appliesSearch() throws Exception {
        newProduct("TC-P09-ZEBRA", true, leaf);
        newProduct("TC-P09-OTHER", true, leaf);

        mockMvc.perform(get("/api/v1/public/catalog/products").param("search", "zebra"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].code").value("TC-P09-ZEBRA"));
    }

    @Test
    @DisplayName("TC-P10: API công khai từ chối tham số phân trang sai -> 400")
    void publicList_invalidPaging_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/public/catalog/products").param("size", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/public/catalog/products").param("size", "101"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/public/catalog/products").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-P11: chi tiết công khai của sản phẩm ngừng bán -> 404, đang bán -> 200")
    void publicDetail_hidesDiscontinuedProduct() throws Exception {
        Product off = newProduct("TC-P11-OFF", false, leaf);
        Product on = newProduct("TC-P11-ON", true, leaf);

        mockMvc.perform(get("/api/v1/public/catalog/products/{id}", off.getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/public/catalog/products/{id}", on.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("TC-P11-ON"));
    }

    // ---------------------------------------------------------------- helpers

    private Category newCategory(String code) {
        return categoryRepo.save(Category.builder().code(code).name("Danh mục " + code).isActive(true).build());
    }

    private Product newProduct(String code, boolean active, Category category) {
        return productRepo.save(Product.builder()
                .code(code).name("Sản phẩm " + code).category(category).baseUnit("cái").isActive(active).build());
    }

    private PriceList newPrice(Product product, long branchId, String price) {
        return PriceList.builder().product(product).branchId(branchId)
                .price(new BigDecimal(price)).effectiveDate(OffsetDateTime.now()).build();
    }

    private String productBody(String code) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "code", code, "name", "Sản phẩm " + code, "categoryId", leaf.getId(), "baseUnit", "cái"));
    }

    private static JsonNode findById(JsonNode array, Long id) {
        for (JsonNode node : array) {
            if (node.get("id").asLong() == id) return node;
        }
        return null;
    }
}
