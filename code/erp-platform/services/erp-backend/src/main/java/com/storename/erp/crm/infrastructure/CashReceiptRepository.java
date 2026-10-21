package com.storename.erp.crm.infrastructure;

import com.storename.erp.crm.domain.CashReceipt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashReceiptRepository extends JpaRepository<CashReceipt, UUID> {

    List<CashReceipt> findByBranchIdOrderByCreatedAtDesc(Long branchId);

    List<CashReceipt> findAllByOrderByCreatedAtDesc();

    boolean existsByReceiptCode(String receiptCode);

    /** Chứng từ đã tạo với khóa lập phiếu này (gửi lại cùng Idempotency-Key -> trả lại chứng từ cũ). */
    java.util.Optional<com.storename.erp.crm.domain.CashReceipt> findByRequestKey(String requestKey);

    /**
     * Khóa ghi phiếu thu trước khi hủy: hai lần hủy đồng thời xếp hàng, lần sau thấy trạng thái
     * CANCELLED và bị từ chối. Thứ tự khóa: phiếu thu -> dòng công nợ (giống hóa đơn -> công nợ).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM CashReceipt r WHERE r.id = :id")
    Optional<CashReceipt> findForUpdate(@Param("id") UUID id);
}
