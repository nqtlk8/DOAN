package com.storename.erp.inventory.infrastructure;

import com.storename.erp.inventory.domain.InboundReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InboundReceiptRepository extends JpaRepository<InboundReceipt, UUID> {
    boolean existsByReceiptCode(String receiptCode);

    /** Chứng từ đã tạo với khóa lập phiếu này (gửi lại cùng Idempotency-Key -> trả lại chứng từ cũ). */
    java.util.Optional<com.storename.erp.inventory.domain.InboundReceipt> findByRequestKey(String requestKey);
    java.util.List<InboundReceipt> findByBranchId(Long branchId);

    /**
     * Đọc và khóa ghi phiếu (SELECT ... FOR UPDATE). Sửa nháp và xác nhận cùng khóa phiếu trước, nên hai thao tác
     * này không chạy xen nhau: xác nhận không thể tác động kho / công nợ theo nội dung đang bị sửa dở.
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT r FROM InboundReceipt r WHERE r.id = :id")
    java.util.Optional<InboundReceipt> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);
}
