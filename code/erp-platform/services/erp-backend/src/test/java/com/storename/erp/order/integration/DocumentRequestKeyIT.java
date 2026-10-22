package com.storename.erp.order.integration;

import com.storename.erp.crm.application.CashReceiptService;
import com.storename.erp.crm.application.ReceivableDebtService;
import com.storename.erp.crm.application.dto.CashReceiptCreateDto;
import com.storename.erp.crm.domain.CashReceipt;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.domain.ReceivableDebtMovementType;
import com.storename.erp.crm.infrastructure.CashReceiptRepository;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import com.storename.erp.inventory.application.InboundReceiptService;
import com.storename.erp.inventory.application.dto.InboundReceiptCreateDto;
import com.storename.erp.inventory.infrastructure.InboundReceiptRepository;
import com.storename.erp.order.application.GoodsReturnService;
import com.storename.erp.order.application.dto.GoodsReturnCreateDto;
import com.storename.erp.order.infrastructure.GoodsReturnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * request_key trên phiếu trả, phiếu nhập, phiếu thu: cùng khóa lập phiếu chỉ ra một chứng từ, kể cả khi
 * gọi thẳng service (không qua IdempotencyAspect, tức bản ghi idempotency đã mất).
 */
@SpringBootTest
@ActiveProfiles({"branch", "test"})
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:branch_request_key_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
class DocumentRequestKeyIT {

    private static final long BRANCH = 3001L;
    private static final UUID USER = UUID.randomUUID();

    @Autowired private GoodsReturnService goodsReturnService;
    @Autowired private GoodsReturnRepository goodsReturnRepo;
    @Autowired private InboundReceiptService inboundService;
    @Autowired private InboundReceiptRepository inboundRepo;
    @Autowired private CashReceiptService cashReceiptService;
    @Autowired private CashReceiptRepository cashReceiptRepo;
    @Autowired private ReceivableDebtService debtService;
    @Autowired private CustomerRepository customerRepo;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setCustomerCode("C-RK-" + UUID.randomUUID().toString().substring(0, 8));
        customer.setName("Khách kiểm thử request_key");
        customer = customerRepo.save(customer);
    }

    private static String key() {
        return "user:" + UUID.randomUUID();
    }

    private GoodsReturnCreateDto returnDto() {
        GoodsReturnCreateDto dto = new GoodsReturnCreateDto();
        dto.setCustomerId(customer.getId());
        GoodsReturnCreateDto.GoodsReturnLineDto line = new GoodsReturnCreateDto.GoodsReturnLineDto();
        line.setProductId(1L);
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(new BigDecimal("1000"));
        line.setUnitOfMeasure("PCS");
        dto.setLines(List.of(line));
        return dto;
    }

    private InboundReceiptCreateDto inboundDto() {
        InboundReceiptCreateDto dto = new InboundReceiptCreateDto();
        InboundReceiptCreateDto.InboundReceiptLineDto line = new InboundReceiptCreateDto.InboundReceiptLineDto();
        line.setProductId(1L);
        line.setQuantity(BigDecimal.TEN);
        line.setUnitCost(new BigDecimal("500"));
        line.setUnitOfMeasure("PCS");
        dto.setLines(List.of(line));
        return dto;
    }

    @Test
    @DisplayName("Phiếu trả: cùng khóa lập phiếu -> trả lại phiếu cũ")
    void goodsReturn_sameKey_returnsExisting() {
        String key = key();
        UUID first = goodsReturnService.createDraft(BRANCH, returnDto(), key);
        UUID second = goodsReturnService.createDraft(BRANCH, returnDto(), key);

        assertThat(second).isEqualTo(first);
        assertThat(goodsReturnRepo.findById(first).orElseThrow().getRequestKey()).isEqualTo(key);
        assertThat(goodsReturnRepo.findByBranchId(BRANCH).stream()
                .filter(r -> key.equals(r.getRequestKey())).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Phiếu nhập: cùng khóa lập phiếu -> trả lại phiếu cũ")
    void inboundReceipt_sameKey_returnsExisting() {
        String key = key();
        UUID first = inboundService.createDraft(BRANCH, inboundDto(), key);
        UUID second = inboundService.createDraft(BRANCH, inboundDto(), key);

        assertThat(second).isEqualTo(first);
        assertThat(inboundRepo.findById(first).orElseThrow().getRequestKey()).isEqualTo(key);
    }

    @Test
    @DisplayName("Phiếu nhập: khóa của chi nhánh khác -> 403, không trả lộ phiếu")
    void inboundReceipt_keyFromOtherBranch_isForbidden() {
        String key = key();
        inboundService.createDraft(BRANCH, inboundDto(), key);

        assertThatThrownBy(() -> inboundService.createDraft(BRANCH + 1, inboundDto(), key))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Phiếu thu: cùng khóa lập phiếu -> một phiếu, công nợ chỉ giảm một lần")
    void cashReceipt_sameKey_debtReducedOnce() {
        debtService.increaseDebt(customer.getId(), BRANCH, new BigDecimal("10000"),
                ReceivableDebtMovementType.INVOICE, "TEST", UUID.randomUUID().toString(), USER, "nợ ban đầu");
        CashReceiptCreateDto dto = new CashReceiptCreateDto();
        dto.setCustomerId(customer.getId());
        dto.setAmount(new BigDecimal("3000"));
        String key = key();

        CashReceipt first = cashReceiptService.create(dto, BRANCH, USER, key);
        CashReceipt second = cashReceiptService.create(dto, BRANCH, USER, key);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(debtService.getCurrentDebt(customer.getId(), BRANCH)).isEqualByComparingTo("7000");
        assertThat(cashReceiptRepo.findById(first.getId()).orElseThrow().getRequestKey()).isEqualTo(key);
    }

    @Test
    @DisplayName("Không có khóa (null): mỗi lần gọi tạo một chứng từ như trước")
    void nullKey_createsNewDocumentEachTime() {
        UUID first = inboundService.createDraft(BRANCH, inboundDto(), null);
        UUID second = inboundService.createDraft(BRANCH, inboundDto(), null);

        assertThat(second).isNotEqualTo(first);
    }
}
