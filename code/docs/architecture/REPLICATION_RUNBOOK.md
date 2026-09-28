# PostgreSQL Logical Replication Runbook

## Topology
- **Hub (HQ)**: Master instance for all global reference data.
- **Spoke (Branch)**: Independent instances for branch-level transactions.
- **Replication Direction**:
  - HQ -> Branch (Master Data: Product, Category, Branch, Customer, User, etc.)
  - Branch -> HQ (Transaction Data: SalesInvoice, InboundReceipt, StockMovement, CostLayer, etc.)

## Single-Writer Principle
We use Single-Writer per table. DO NOT use `FOR ALL TABLES`. Only publish tables owned by the respective instance as defined in `DATA_OWNERSHIP_MATRIX.md`.

## Setup Steps
1. Configure `wal_level = logical` in PostgreSQL config on both HQ and Branch.
2. Create replication role `erp_repl`.
3. HQ creates publication for master tables.
4. Branch creates publication for transaction tables.
5. Branch subscribes to HQ publication.
6. HQ subscribes to Branch publication.

## Validating Replication
- Check `pg_stat_replication` on the publisher.
- Check `pg_stat_subscription` on the subscriber.

## Common Issues
- **Conflict**: A duplicate key or constraint violation. Usually means ownership principle was violated (e.g., Branch tried to modify HQ-owned data directly without going through HQ).
- **Lag**: Check network between instances.

## Bật replicate bảng snapshot cho chi nhánh đang chạy
Theo thiết kế Sprint 7 (fix-dashboard), 2 bảng `stock_on_hand` và `receivable_debt` được đưa vào danh sách replicate từ Branch lên HQ có lọc dòng (`WHERE branch_id = N`). Với các chi nhánh đang chạy, quy trình bật như sau:

1. Chạy migration V21 trên mọi database (HQ, TP1, TP2) để thiết lập `REPLICA IDENTITY USING INDEX` cho 2 bảng.
2. Từ terminal, chạy script cấp phát tự động:
   ```bash
   bash code/scripts/enable-snapshot-replication.sh tp1
   bash code/scripts/enable-snapshot-replication.sh tp2
   ```
3. Script trên sẽ:
   - Thêm 2 bảng vào `pub_tp1_to_hq` (với row filter).
   - Xác nhận (prompt) trước khi tự động xoá dữ liệu TẠI HQ: `DELETE FROM stock_on_hand WHERE branch_id = 1; DELETE FROM receivable_debt WHERE branch_id = 1;`. Lưu ý KHÔNG BAO GIỜ được dùng lệnh `TRUNCATE` trên HQ vì nó sẽ xóa luôn dữ liệu của chi nhánh khác đã đồng bộ lên HQ.
   - Chạy `ALTER SUBSCRIPTION sub_hq_from_tp1 REFRESH PUBLICATION WITH (copy_data = true)` để lấy bộ dữ liệu mới nhất.
4. Kiểm tra trên HQ (`hq-db`): `SELECT COUNT(*) FROM stock_on_hand WHERE branch_id = 1` khớp với số liệu ở Branch.
