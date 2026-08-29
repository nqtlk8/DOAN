# Ma trận bao phủ Test (Test Coverage Matrix)

| Tính năng / Module | Unit | Slice (H2) | Integration | E2E | Ghi chú |
|---|---|---|---|---|---|
| JwtTokenProvider.validateToken | ✅ | — | ✅ (filter chain) | ✅ (cross-instance) | |
| RedisConfig (Bean tồn tại đúng Profile) | — | — | ✅ | — | Không cần E2E vì đã verify đủ ở Integration |
| AuthController /auth/login chỉ tồn tại ở HQ | — | ✅ (WebMvcTest) | ✅ (403 ở Branch) | — | |
| Idempotency AOP - HQ (Redis) | ✅ (logic thuần) | — | ✅ | — | |
| Idempotency AOP - Branch (non-Redis) | ✅ (logic thuần) | — | ✅ | — | Cơ chế thay thế xem docs sprint 0.1 mục 5 |
| **Inventory - Costing Strategy** | ✅ | — | — | — | Logic toán học thuần |
| **Inventory - StockOnHand Invariant** | ✅ | — | — | — | Verify logic cộng trừ cơ bản |
| **Inventory - Transaction Rollback** | — | — | ✅ | — | Verify Outbound receipt không commit nếu fail |
| **Inventory - Race Condition (N=20)** | — | — | ✅ | — | Dùng `@SpringBootTest` giả lập tranh chấp Optimistic Lock |
| **Order - Customer Price Fallback** | ✅ | — | — | — | Unit test logic trả về price fallback |
| **Order - Debt Reconciliation** | — | — | ✅ | — | Verify flow SalesInvoice kéo theo CRM Debt |
| **Order - Snapshot Immutability** | — | — | ✅ | — | Verify data hoá đơn không đổi khi update sau |
| **Order - Transaction Rollback** | — | — | ✅ | — | Verify stock và debt không đổi khi lỗi ở order |
| **Common - Idempotency Flow** | — | — | ✅ | — | Tích hợp vào `SalesInvoiceController` |
| **Procurement - Stock Transfer Reconciliation** | — | — | ✅ | — | Verify tổng tồn kho không đổi |
| **Procurement - PO Isolation** | — | — | ✅ | — | Verify PO không ảnh hưởng stock (nếu chưa receipt) |
| **Procurement - PO Auto-Match** | — | — | ✅ | — | Verify InboundReceipt confirm trigger PO nhận |
| **Inventory - Idempotency Inbound** | — | — | ✅ | — | Verify InboundReceipt idempotency |
| **Order - Customer Order Auto Split** | — | — | ✅ | — | Đơn hàng sinh ra hoá đơn tự động |
| **Order - Customer Order Concurrency** | — | — | ✅ | — | Xử lý đa luồng đơn hàng với tồn kho |
| **Order - Goods Return Full Flow** | — | — | ✅ | — | Trả hàng cộng kho & trừ nợ (nếu có HĐ) |
| **Order - Goods Return Rollback** | — | — | ✅ | — | Đảm bảo không cộng kho ảo khi lỗi |
