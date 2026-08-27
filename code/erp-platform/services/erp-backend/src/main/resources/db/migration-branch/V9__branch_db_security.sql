-- Revoke INSERT/UPDATE/DELETE from erp_user on master data tables
REVOKE INSERT, UPDATE, DELETE ON TABLE branch, category, product, price_list, customer FROM erp_user;

-- Ensure erp_user still has SELECT
GRANT SELECT ON TABLE branch, category, product, price_list, customer TO erp_user;
