-- internal_url = URL cổng SPA chi nhánh mà TRÌNH DUYỆT truy cập được (dùng để chặn STAFF đăng nhập nhầm cổng).
-- Branch DB bị REVOKE UPDATE trên bảng branch/customer (V9) -> chỉ chạy khi có quyền (HQ); branch nhận qua replication.
DO $$
BEGIN
  IF has_table_privilege(current_user, 'branch', 'UPDATE') THEN
    UPDATE branch SET internal_url = 'http://localhost:81' WHERE code = 'TP1';
    UPDATE branch SET internal_url = 'http://localhost:82' WHERE code = 'TP2';
  END IF;

  IF has_table_privilege(current_user, 'customer', 'UPDATE') THEN
    UPDATE customer SET branch_id = NULL WHERE customer_code IN ('KH-001','KH-002','KH-003');
  END IF;
END $$;
