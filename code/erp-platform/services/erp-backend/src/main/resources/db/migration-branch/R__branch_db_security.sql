-- ============================================================================
-- R__branch_db_security — Quyền của user ứng dụng trên DB CHI NHÁNH.
-- CHỈ chạy ở Branch (application-branch.yml: locations += classpath:db/migration-branch).
--
-- Nguyên tắc "mặc định chỉ đọc": app chi nhánh (${app_db_user}) chỉ được GHI vào các
-- bảng do chi nhánh sở hữu (giao dịch, tồn kho, công nợ, idempotency). Mọi bảng khác —
-- gồm cả bảng thêm sau này — chỉ được đọc. Master data do HQ ghi và đổ xuống qua
-- logical replication; apply worker chạy bằng chủ subscription (superuser) nên không
-- bị ảnh hưởng bởi các lệnh REVOKE ở đây.
--
-- Chạy lại mỗi lần migrate: ${flyway:timestamp} làm checksum thay đổi mỗi lần, nên quyền
-- luôn được áp lại sau khi các migration V__ tạo bảng mới.
-- Lần chạy: ${flyway:timestamp}
--
-- Danh sách bảng chi nhánh được ghi phải khớp TRANSACTION_TABLES + SNAPSHOT_TABLES trong
-- scripts/replication-tables.conf (cộng các bảng cục bộ không replicate).
-- ============================================================================

DO $$
DECLARE
    app_user   text := '${app_db_user}';
    is_super   boolean;
    branch_owned_tables text[] := ARRAY[
        -- replicate Branch -> HQ
        'sales_invoice', 'sales_invoice_line',
        'goods_return', 'goods_return_line',
        'inbound_receipt', 'inbound_receipt_line',
        'stock_movement', 'cost_layer',
        'stock_on_hand', 'receivable_debt', 'receivable_debt_movement',
        -- cục bộ, không replicate
        'idempotency_record', 'customer_product_price',
        'fact_sales', 'fact_stock_movement'
    ];
    t text;
BEGIN
    SELECT rolsuper INTO is_super FROM pg_roles WHERE rolname = app_user;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'R__branch_db_security: role "%" (spring.datasource.username) không tồn tại', app_user;
    END IF;

    IF app_user = current_user OR is_super THEN
        -- Chủ bảng / superuser bỏ qua mọi kiểm tra quyền: REVOKE không có tác dụng.
        RAISE WARNING 'R__branch_db_security: app dùng role "%" (owner hoặc superuser) -> KHÔNG thể khóa ghi master data. Cấu hình app dùng role erp_app (docker/postgres/init).', app_user;
        RETURN;
    END IF;

    EXECUTE format('GRANT USAGE ON SCHEMA public TO %I', app_user);
    EXECUTE format('REVOKE INSERT, UPDATE, DELETE, TRUNCATE ON ALL TABLES IN SCHEMA public FROM %I', app_user);
    EXECUTE format('GRANT SELECT ON ALL TABLES IN SCHEMA public TO %I', app_user);
    EXECUTE format('GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO %I', app_user);

    FOREACH t IN ARRAY branch_owned_tables LOOP
        EXECUTE format('GRANT INSERT, UPDATE, DELETE ON TABLE %I TO %I', t, app_user);
    END LOOP;
END $$;
