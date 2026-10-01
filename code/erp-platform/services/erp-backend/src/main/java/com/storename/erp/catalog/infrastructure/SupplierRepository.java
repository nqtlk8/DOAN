package com.storename.erp.catalog.infrastructure;

import com.storename.erp.catalog.domain.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Truy cập bảng {@code supplier}.
 *
 * <p>Supplier là master data dùng chung toàn hệ thống: chỉ HQ ghi, replicate HQ → mọi chi nhánh.
 * Không có phạm vi theo chi nhánh (đã bỏ cột {@code branch_id}).</p>
 */
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    /** Supplier đang hoạt động, sắp theo tên — dùng cho người dùng chi nhánh (STAFF). */
    List<Supplier> findByIsActiveTrueOrderByNameAsc();

    /** Toàn bộ supplier kể cả đã ngừng hoạt động — dùng cho ADMIN tại HQ. */
    List<Supplier> findAllByOrderByNameAsc();
}
