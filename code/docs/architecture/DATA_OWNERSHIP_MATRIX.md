# Data Ownership Matrix

Cập nhật: 2026-10-10. Danh sách bảng replicate được cấu hình ở `scripts/replication-tables.conf`. Khi đổi bảng nào ở đây phải sửa cả file đó và danh sách bảng chi nhánh được ghi trong `db/migration-branch/R__branch_db_security.sql`.

"Branch Write = No" được ép ở mức DB: role ứng dụng `erp_app` ở chi nhánh chỉ có SELECT trên các bảng này.

| Table | Owner | HQ Write | Branch Write | Replication Direction | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| branch | HQ | Yes | No | HQ -> Branch | TP1, TP2 có sẵn từ `R__reference_data.sql` |
| role | HQ | Yes | No | HQ -> Branch | STAFF, ADMIN từ `R__reference_data.sql` |
| permission | HQ | Yes | No | HQ -> Branch | |
| role_permission | HQ | Yes | No | HQ -> Branch | |
| user_account | HQ | Yes | No | HQ -> Branch | admin, staff_tp1, staff_tp2 từ `R__reference_data.sql` |
| user_branch_role | HQ | Yes | No | HQ -> Branch | |
| category | HQ | Yes | No | HQ -> Branch | Cây 2 cấp có sẵn từ `R__reference_data.sql` (giống web-public) |
| product | HQ | Yes | No | HQ -> Branch | |
| supplier | HQ | Yes | No | HQ -> Branch | **Dùng chung toàn hệ thống** (không có `branch_id`) |
| price_list | HQ | Yes | No | HQ -> Branch | Giá niêm yết theo (sản phẩm, chi nhánh); app lấy dòng `effective_date <= now` mới nhất. Giá bán ưu tiên `customer_product_price` trước |
| customer | HQ | Yes | No | HQ -> Branch | `branch_id` NULL = dùng chung, có giá trị = khách riêng của chi nhánh |
| inventory_alert_config | HQ | Yes | No | HQ -> Branch | |
| dim_date | HQ | Yes | No | HQ -> Branch | |
| sales_invoice | Branch | No | Yes | Branch -> HQ | |
| sales_invoice_line | Branch | No | Yes | Branch -> HQ | |
| goods_return | Branch | No | Yes | Branch -> HQ | |
| goods_return_line | Branch | No | Yes | Branch -> HQ | |
| inbound_receipt | Branch | No | Yes | Branch -> HQ | |
| inbound_receipt_line | Branch | No | Yes | Branch -> HQ | |
| stock_movement | Branch | No | Yes | Branch -> HQ | |
| cost_layer | Branch | No | Yes | Branch -> HQ | |
| receivable_debt_movement | Branch | No | Yes | Branch -> HQ | Sổ cái công nợ. Thêm vào replication 2026-10-01 |
| customer_product_price | Branch | No | Yes | Branch -> HQ | Giá riêng theo khách (giá hiện hành). Ghi tự động khi xác nhận hóa đơn có giá khác giá đang áp dụng (`CustomerProductPriceService.applyFromInvoice`). Thêm vào replication 2026-10-10 (V9) |
| customer_product_price_history | Branch | No | Yes | Branch -> HQ | Lịch sử thay đổi giá riêng theo khách, append-only. Thêm 2026-10-10 (V9) |
| stock_on_hand | Branch | No | Yes | Branch -> HQ (row filter branch_id) | |
| receivable_debt | Branch | No | Yes | Branch -> HQ (row filter branch_id) | |
| idempotency_record | Local | Yes | Yes | None | Khóa Idempotency-Key của từng instance |
| fact_sales | HQ | Yes | No* | None (DW only) | *Chi nhánh được ghi cục bộ nhưng không dùng |
| fact_stock_movement | HQ | Yes | No* | None (DW only) | như trên |

Ở mức ứng dụng, HQ không có API ghi bảng do chi nhánh sở hữu (`opening-balance` từ chối khi JWT không có branchId — `ReplicationOwnershipHqTest`). Ở mức DB, chiều ngược lại (HQ ghi bảng chi nhánh) chưa bị chặn bằng quyền — xem `DATABASE_REPLICATION.md` mục 9.
