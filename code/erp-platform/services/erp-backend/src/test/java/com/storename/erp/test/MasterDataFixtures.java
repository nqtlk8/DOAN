package com.storename.erp.test;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Master data tối thiểu cho các test chạy trên PostgreSQL thật.
 *
 * <p>Từ khi gộp Flyway, {@code db/migration} chỉ tạo schema — không còn seed (danh mục, sản phẩm,
 * khách hàng...). Test cần sản phẩm/danh mục phải tự chèn qua lớp này. Dùng id cố định ở dải 9000+
 * để không đụng sequence identity và để assert được; {@code ON CONFLICT DO NOTHING} cho phép gọi
 * nhiều lần (test thường chạy trong {@code @Transactional} nên dữ liệu tự rollback).</p>
 */
public final class MasterDataFixtures {

    public static final long CATEGORY_ID = 9000L;
    /** Sản phẩm fixture: 9001..9004, mã {@code IT-SP-1}..{@code IT-SP-4}. */
    public static final long PRODUCT_1 = 9001L;
    public static final long PRODUCT_2 = 9002L;
    public static final long PRODUCT_3 = 9003L;
    public static final long PRODUCT_4 = 9004L;

    private MasterDataFixtures() {
    }

    /** Chèn danh mục {@link #CATEGORY_ID} và 4 sản phẩm {@link #PRODUCT_1}..{@link #PRODUCT_4} nếu chưa có. */
    public static void ensureProducts(JdbcTemplate jdbc) {
        jdbc.update("""
                INSERT INTO category (id, code, name, is_active, created_at, updated_at)
                VALUES (?, 'IT-CAT', 'Danh mục test', true, now(), now())
                ON CONFLICT (id) DO NOTHING
                """, CATEGORY_ID);
        for (int i = 1; i <= 4; i++) {
            jdbc.update("""
                    INSERT INTO product (id, code, name, category_id, base_unit, is_active, created_at, updated_at)
                    VALUES (?, ?, ?, ?, 'Cái', true, now(), now())
                    ON CONFLICT (id) DO NOTHING
                    """, CATEGORY_ID + i, productCode(i), "Sản phẩm test " + i, CATEGORY_ID);
        }
    }

    /** Mã sản phẩm fixture thứ {@code index} (1..4). */
    public static String productCode(int index) {
        return "IT-SP-" + index;
    }
}
