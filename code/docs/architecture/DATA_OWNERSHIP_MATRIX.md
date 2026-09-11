# Data Ownership Matrix

| Table | Owner | HQ Write | Branch Write | Replication Direction |
| --- | --- | --- | --- | --- |
| branch | HQ | Yes | No | HQ -> Branch |
| category | HQ | Yes | No | HQ -> Branch |
| product | HQ | Yes | No | HQ -> Branch |
| supplier | HQ | Yes | No | HQ -> Branch |
| price_list | HQ | Yes | No | HQ -> Branch |
| customer | HQ | Yes | No | HQ -> Branch |
| role | HQ | Yes | No | HQ -> Branch |
| permission | HQ | Yes | No | HQ -> Branch |
| role_permission | HQ | Yes | No | HQ -> Branch |
| user_account | HQ | Yes | No | HQ -> Branch |
| user_branch_role| HQ | Yes | No | HQ -> Branch |
| inventory_alert_config | HQ | Yes | No | HQ -> Branch |
| idempotency_record | Local | Yes | Yes | None |
| dim_date | HQ | Yes | No | HQ -> Branch |
| sales_invoice | Branch | No | Yes | Branch -> HQ |
| sales_invoice_line | Branch | No | Yes | Branch -> HQ |
| goods_return | Branch | No | Yes | Branch -> HQ |
| goods_return_line | Branch | No | Yes | Branch -> HQ |
| inbound_receipt | Branch | No | Yes | Branch -> HQ |
| inbound_receipt_line | Branch | No | Yes | Branch -> HQ |
| stock_movement | Branch | No | Yes | Branch -> HQ |
| cost_layer | Branch | No | Yes | Branch -> HQ |
| inventory_alert_log | Local/HQ | Yes | No | None |
| fact_sales | HQ | Yes | No | None (DW only) |
| fact_stock_movement | HQ | Yes | No | None (DW only) |
