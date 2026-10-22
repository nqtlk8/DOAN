package com.storename.erp.crm.application;

import com.storename.erp.common.exception.BusinessConflictException;
import com.storename.erp.common.exception.ResourceNotFoundException;
import com.storename.erp.crm.application.dto.CashReceiptCreateDto;
import com.storename.erp.crm.application.dto.CashReceiptResponseDto;
import com.storename.erp.crm.domain.CashReceipt;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.domain.ReceivableDebt;
import com.storename.erp.crm.domain.ReceivableDebtMovementType;
import com.storename.erp.crm.infrastructure.CashReceiptRepository;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Phiếu thu tiền khách hàng.
 *
 * <p>Lập phiếu và giảm công nợ trong CÙNG một transaction: lỗi ở bất kỳ bước nào (vd thu vượt nợ)
 * thì không có phiếu nào được lưu. Công nợ tính theo (khách, chi nhánh), không theo từng hóa đơn.</p>
 */
@Service
@Slf4j
public class CashReceiptService {

    /** ref_type của biến động công nợ khi lập phiếu thu (movement_type = PAYMENT). */
    public static final String REF_RECEIPT = "CASH_RECEIPT";
    /** ref_type của biến động công nợ khi hủy phiếu thu (movement_type = ADJUSTMENT). */
    public static final String REF_RECEIPT_CANCEL = "CASH_RECEIPT_CANCEL";
    /** Mã lỗi (HTTP 409): khách không còn nợ tại chi nhánh. */
    public static final String NO_DEBT = "RECEIPT_NO_DEBT";

    private final CashReceiptRepository receiptRepository;
    private final CustomerRepository customerRepository;
    private final ReceivableDebtService debtService;
    private final ZoneId businessZone;

    public CashReceiptService(CashReceiptRepository receiptRepository,
                              CustomerRepository customerRepository,
                              ReceivableDebtService debtService,
                              @Value("${analytics.business-zone:Asia/Ho_Chi_Minh}") String businessZone) {
        this.receiptRepository = receiptRepository;
        this.customerRepository = customerRepository;
        this.debtService = debtService;
        this.businessZone = ZoneId.of(businessZone);
    }

    @Retryable(
        retryFor = {
            org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            org.springframework.dao.DataIntegrityViolationException.class,
            org.springframework.dao.PessimisticLockingFailureException.class
        },
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    @Transactional
    public CashReceipt create(CashReceiptCreateDto dto, Long branchId, UUID userId) {
        return create(dto, branchId, userId, null);
    }

    /**
     * Lập phiếu thu. {@code requestKey} (khóa lập phiếu, xem {@code IdempotencyContext}) được ghi vào phiếu
     * trong cùng transaction với việc giảm công nợ: gửi lại cùng khóa thì trả lại phiếu đã lập, công nợ không
     * bị trừ lần thứ hai.
     */
    @Retryable(
        retryFor = {
            org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            org.springframework.dao.DataIntegrityViolationException.class,
            org.springframework.dao.PessimisticLockingFailureException.class
        },
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    @Transactional
    public CashReceipt create(CashReceiptCreateDto dto, Long branchId, UUID userId, String requestKey) {
        requireBranch(branchId);
        if (requestKey != null) {
            java.util.Optional<CashReceipt> existing = receiptRepository.findByRequestKey(requestKey);
            if (existing.isPresent()) {
                if (!existing.get().getBranchId().equals(branchId)) {
                    throw new AccessDeniedException("Receipt does not belong to this branch");
                }
                log.info("Cash receipt {} already created with this request key, returning it", existing.get().getReceiptCode());
                return existing.get();
            }
        }
        BigDecimal amount = dto.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền thu phải lớn hơn 0");
        }
        LocalDate today = LocalDate.now(businessZone);
        LocalDate receiptDate = dto.getReceiptDate() != null ? dto.getReceiptDate() : today;
        if (receiptDate.isAfter(today)) {
            throw new IllegalArgumentException("Ngày thu không được sau ngày hôm nay");
        }
        UUID customerId = dto.getCustomerId();
        if (customerId == null || !customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Khách hàng chưa có tại chi nhánh (chưa đồng bộ từ HQ): " + customerId);
        }

        // Kiểm tra sớm để báo lỗi dễ hiểu; đảm bảo thật sự nằm ở decreaseDebt (khóa dòng công nợ, cấm nợ âm).
        BigDecimal currentDebt = debtService.getCurrentDebt(customerId, branchId);
        if (currentDebt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessConflictException(NO_DEBT, "Khách hàng không còn nợ tại chi nhánh này");
        }
        if (amount.compareTo(currentDebt) > 0) {
            throw new BusinessConflictException(ReceivableDebtService.DEBT_NEGATIVE, String.format(
                    "Số tiền thu %s vượt số nợ hiện tại %s", plain(amount), plain(currentDebt)));
        }

        CashReceipt receipt = new CashReceipt();
        receipt.setReceiptCode(newCode());
        receipt.setBranchId(branchId);
        receipt.setCustomerId(customerId);
        receipt.setReceiptDate(receiptDate);
        receipt.setAmount(amount);
        receipt.setNote(blankToNull(dto.getNote()));
        receipt.setRequestKey(requestKey);
        // Snapshot tạm (thỏa CHECK debt_after = debt_before - amount nếu bị flush sớm); ghi lại sau khi khóa công nợ.
        receipt.snapshotDebt(currentDebt, currentDebt.subtract(amount));
        receipt = receiptRepository.save(receipt);

        ReceivableDebt debt = debtService.decreaseDebt(customerId, branchId, amount,
                ReceivableDebtMovementType.PAYMENT, REF_RECEIPT, receipt.getId().toString(), userId,
                movementNote("Phiếu thu " + receipt.getReceiptCode(), receipt.getNote()));
        BigDecimal debtAfter = debt.getTotalDebt();
        receipt.snapshotDebt(debtAfter.add(amount), debtAfter);

        log.info("Cash receipt {} created: customer={}, branch={}, amount={}, debt {} -> {}",
                receipt.getReceiptCode(), customerId, branchId, amount, receipt.getDebtBefore(), debtAfter);
        return receiptRepository.save(receipt);
    }

    /** Hủy phiếu thu: tăng lại công nợ đúng số tiền đã thu (ADJUSTMENT). Phiếu giữ lại để truy vết. */
    @Transactional
    public CashReceipt cancel(UUID receiptId, String reason, Long branchId, UUID userId) {
        requireBranch(branchId);
        CashReceipt receipt = receiptRepository.findForUpdate(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu thu: " + receiptId));
        if (!receipt.getBranchId().equals(branchId)) {
            throw new AccessDeniedException("Phiếu thu thuộc chi nhánh khác");
        }
        String cleanReason = blankToNull(reason);
        receipt.cancel(userId, cleanReason);

        debtService.increaseDebt(receipt.getCustomerId(), branchId, receipt.getAmount(),
                ReceivableDebtMovementType.ADJUSTMENT, REF_RECEIPT_CANCEL, receipt.getId().toString(), userId,
                movementNote("Hủy phiếu thu " + receipt.getReceiptCode(), cleanReason));

        log.info("Cash receipt {} cancelled by {}: debt +{}", receipt.getReceiptCode(), userId, receipt.getAmount());
        return receiptRepository.save(receipt);
    }

    @Transactional(readOnly = true)
    public List<CashReceiptResponseDto> list(Long branchId) {
        List<CashReceipt> receipts = branchId == null
                ? receiptRepository.findAllByOrderByCreatedAtDesc()
                : receiptRepository.findByBranchIdOrderByCreatedAtDesc(branchId);
        Map<UUID, Customer> customers = customersOf(receipts);
        return receipts.stream()
                .map(r -> CashReceiptResponseDto.from(r, customers.get(r.getCustomerId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CashReceiptResponseDto get(UUID receiptId, Long branchId) {
        CashReceipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu thu: " + receiptId));
        if (branchId != null && !receipt.getBranchId().equals(branchId)) {
            throw new AccessDeniedException("Phiếu thu thuộc chi nhánh khác");
        }
        Customer customer = customerRepository.findById(receipt.getCustomerId()).orElse(null);
        return CashReceiptResponseDto.from(receipt, customer);
    }

    /** Mã phiếu theo id, dùng cho cột "Chứng từ" của sao kê công nợ. */
    @Transactional(readOnly = true)
    public Map<UUID, String> getReceiptCodes(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return receiptRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(CashReceipt::getId, CashReceipt::getReceiptCode, (a, b) -> a));
    }

    private Map<UUID, Customer> customersOf(List<CashReceipt> receipts) {
        Set<UUID> ids = receipts.stream().map(CashReceipt::getCustomerId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return customerRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity(), (a, b) -> a));
    }

    private String newCode() {
        for (int i = 0; i < 5; i++) {
            String code = "PT" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            if (!receiptRepository.existsByReceiptCode(code)) return code;
        }
        throw new IllegalStateException("Không sinh được mã phiếu thu, vui lòng thử lại");
    }

    static void requireBranch(Long branchId) {
        if (branchId == null) {
            throw new AccessDeniedException("Thao tác này cần token gắn với một chi nhánh");
        }
    }

    private static String movementNote(String head, String detail) {
        return detail == null ? head : head + " - " + detail;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
