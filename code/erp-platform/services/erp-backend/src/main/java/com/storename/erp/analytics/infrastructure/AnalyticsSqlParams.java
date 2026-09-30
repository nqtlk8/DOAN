package com.storename.erp.analytics.infrastructure;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.sql.Types;

/**
 * Tạo tham số SQL cho các truy vấn analytics.
 *
 * <p>Vì sao cần: PostgreSQL không suy ra được kiểu của tham số null không kiểu trong biểu thức
 * {@code (:branchId IS NULL OR branch_id = :branchId)} và báo "could not determine data type of parameter $1"
 * (lỗi 500 khi dashboard chọn "Tất cả chi nhánh"). Gán {@link Types#BIGINT} cho {@code branchId} để driver gửi
 * null có kiểu. Mọi truy vấn có {@code :branchId} phải tạo tham số qua lớp này.</p>
 */
public final class AnalyticsSqlParams {

    private AnalyticsSqlParams() {
    }


    public static MapSqlParameterSource create() {
        return new MapSqlParameterSource();
    }

    public static MapSqlParameterSource withBranchId(Long branchId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("branchId", branchId, Types.BIGINT);
        return params;
    }
}
