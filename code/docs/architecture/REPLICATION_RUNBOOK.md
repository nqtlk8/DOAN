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
