# Ma trận Bao phủ Kế hoạch Kiểm thử (Test Coverage Matrix)

| Module / Tính năng | Level 1 (Unit) | Level 2 (WebMvc) | Level 3 (Integration) | Level 4 (E2E) | Ghi chú |
|---|---|---|---|---|---|
| **Identity & Auth** | ✅ | ✅ | ✅ | ✅ | Đã test JwtTokenProvider (valid/expired), Rbac (403), Context HQ/Branch, UI Login. |
| **Catalog (Product/Supplier)** | ✅ | ✅ | ✅ | ✅ | Test Product/Supplier Write/Read, Product UI list. |
| **CRM (Customer/Debt)** | ✅ | ✅ | ✅ | ✅ | CustomerWrite/Read, ReceivableDebt, Playwright test khách hàng, UI Debt. |
| **Inventory (Stock/Inbound)** | ✅ | ✅ | ✅ | ✅ | StockOnHand logic, InboundReceiptService, StockController, Playwright phiếu nhập. |
| **Order (Sales/Returns)** | ✅ | ✅ | ✅ | ✅ | GoodsReturnService, SalesInvoiceService, Playwright tạo đơn hàng & phiếu trả. |
| **Analytics (Dashboard)** | ✅ | ✅ | ✅ | ✅ | DataAdapter native queries (H2 PgMode), DashboardController, Dashboard UI Playwright. |
| **Architecture / Config** | N/A | N/A | ✅ | N/A | Test ConditionalBeanContext cho HQ và Branch, JpaAuditing Config. |

*Chú thích:*
- **✅**: Đã hoàn thành (Covered).
- **Level 1**: JUnit 5 + Mockito cho Business Logic.
- **Level 2**: `@WebMvcTest` cho HTTP Routing, Security.
- **Level 3**: `@SpringBootTest` / `@DataJpaTest` cho Data layer, Conditional Beans, Native SQL.
- **Level 4**: Playwright Frontend Tests.
