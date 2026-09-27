# Nh·∫≠t k√Ω h√†nh ƒë·ªông ‚Äî Gemini ‚Äî UI/UX revamp erp-frontend
Plan: docs/UI_UX_PLAN_GEMINI.md ¬∑ B·∫Øt ƒë·∫ßu: 2026-09-27 12:05 ¬∑ Nh√°nh git: feat/ui-ux-revamp

## Nh·∫≠t k√Ω

### [#001] 2026-09-27 12:05 ¬∑ Sprint 0 ¬∑ B∆∞·ªõc 1 ¬∑ CREATE
- **File:** `docs/GEMINI_ACTION_LOG.md`
- **Vi·ªác ƒë√£ l√†m:** T·∫°o file log, b·∫Øt ƒë·∫ßu Sprint 0.

### [#002] 2026-09-27 12:05 ¬∑ Sprint 0 ¬∑ B∆∞·ªõc 1 ¬∑ RUN
- **L·ªánh:** `git checkout -b feat/ui-ux-revamp`
- **K·∫øt qu·∫£:** ƒê√£ chuy·ªÉn sang nh√°nh m·ªõi.
### [#003] 2026-09-27 12:06 ∑ Sprint 0 ∑ Bu?c 2 ∑ RUN
- **L?nh:** 
px tsc --noEmit -p tsconfig.json
- **K?t qu?:** 19 l?i (baseline)

### [#004] 2026-09-27 12:07 ∑ Sprint 0 ∑ Bu?c 2 ∑ RUN
- **L?nh:** 
pm run lint
- **K?t qu?:** 0 l?i, 132 warnings (baseline)

### [#005] 2026-09-27 12:07 ∑ Sprint 0 ∑ Bu?c 2 ∑ RUN
- **L?nh:** 
px vitest run
- **K?t qu?:** 5 passed (baseline)

### [#006] 2026-09-27 12:07 ∑ Sprint 0 ∑ Bu?c 2 ∑ RUN
- **L?nh:** 
pm run build
- **K?t qu?:** Pass (baseline)

### [#007] 2026-09-27 12:07 ∑ Sprint 0 ∑ Bu?c 3 ∑ MOVE
- **File:** src/App.css, src/layout/MdiModuleLayout.tsx, v‡ 17 scripts ? g?c
- **Vi?c d„ l‡m:** Di chuy?n v‡o thu m?c _to_delete/
- **L˝ do:** Theo Bu?c 3 c?a Sprint 0
- **?nh hu?ng:** –„ d?n d?p c·c file khÙng d˘ng d?n.

### [#008] 2026-09-27 12:07 ∑ Sprint 0 ∑ Bu?c 4 ∑ EDIT
- **File:** eslint.config.mjs, 	sconfig.json
- **Vi?c d„ l‡m:** ThÍm _to_delete v‡o ignores v‡ exclude
- **L˝ do:** Theo Bu?c 4 c?a Sprint 0

### [#009] 2026-09-27 12:08 ∑ Sprint 0 ∑ Bu?c 5 ∑ EDIT
- **File:** playwright.config.ts
- **Vi?c d„ l‡m:** ThÍm 	estIgnore: ['**/_legacy/**']
- **L˝ do:** Theo Bu?c 5 c?a Sprint 0

### [#010] 2026-09-27 12:08 ∑ Sprint 0 ∑ Bu?c 6 ∑ CREATE
- **File:** 	ests/helpers/session.ts
- **Vi?c d„ l‡m:** T?o helper akeJwt v‡ loginAs
- **L˝ do:** Theo Bu?c 6 c?a Sprint 0

### [#011] 2026-09-27 12:08 ∑ Sprint 0 ∑ Bu?c 7 ∑ CREATE
- **File:** 	ests/helpers/mockApi.ts
- **Vi?c d„ l‡m:** T?o helper mockApi
- **L˝ do:** Theo Bu?c 7 c?a Sprint 0

### [#012] 2026-09-27 12:09 ∑ Sprint 0 ∑ Bu?c 8 ∑ CREATE
- **File:** 	ests/helpers/fixtures.ts
- **Vi?c d„ l‡m:** T?o helper ch?a data m?u
- **L˝ do:** Theo Bu?c 8 c?a Sprint 0

### [#013] 2026-09-27 12:09 ∑ Sprint 0 ∑ Bu?c 9 ∑ CHECKPOINT
- **Vi?c d„ l‡m:** Xong c·c bu?c 1-8 c?a Sprint 0.
- **Ti?n d?:** 
  - –„ l?y baseline (tsc 19 l?i, lint 0 l?i, vitest 5 pass, build pass).
  - –„ chuy?n file th?a v‡o _to_delete/.
  - –„ t?o c·c helper test E2E.
- **V?n d? / L?ch plan:** KhÙng cÛ.
### [#014] 2026-09-27 12:11 ∑ Sprint 1 ∑ Bu?c 1.1 ∑ EDIT
- **File:** src/index.css
- **Vi?c d„ l‡m:** Vi?t l?i to‡n b? v?i token Tailwind v4 theo plan.
- **L˝ do:** Chu?n hÛa bi?n CSS.

### [#015] 2026-09-27 12:12 ∑ Sprint 1 ∑ Bu?c 1.3 ∑ CREATE
- **File:** src/shared/utils/format.ts
- **Vi?c d„ l‡m:** T?o file ti?n Ìch d?nh d?ng.
- **L˝ do:** T·ch logic format s?/ng‡y.

### [#016] 2026-09-27 12:12 ∑ Sprint 1 ∑ Bu?c 1.4 ∑ CREATE/EDIT
- **File:** src/config/env.ts, src/config/brand.ts, index.html
- **Vi?c d„ l‡m:** T?o config env, brand, v‡ d?i <html lang="vi">, <title>ERP System</title>.
- **L˝ do:** Chu?n hÛa thÙng tin ?ng d?ng.

### [#017] 2026-09-27 12:13 ∑ Sprint 1 ∑ Bu?c 1.5 ∑ CREATE/EDIT
- **File:** src/components/layout/MainContent.tsx, src/shared/components/Page/PageContainer.tsx, src/shared/components/Page/PageHeader.tsx, v‡ t?t c? c·c *List.tsx, Dashboard.tsx
- **Vi?c d„ l‡m:** Update MainContent th‡nh lex-1 min-h-0 overflow-hidden, t?o component PageContainer, PageHeader, v‡ wrap t?t c? c·c trang danh s·ch b?ng PageContainer.
- **L˝ do:** KhÙng d? trang m?t thanh cu?n.

### [#018] 2026-09-27 12:15 ∑ Sprint 1 ∑ Bu?c 1.6 ∑ EDIT
- **File:** src/App.tsx
- **Vi?c d„ l‡m:** S?a TabBar v?i c?u tr˙c, m‡u s?c, data-testid, logic dÛng tab b?ng chu?t gi?a (onAuxClick).
- **L˝ do:** ¡p d?ng token m?i v‡ fix UI Tab.

### [#019] 2026-09-27 12:17 ∑ Sprint 1 ∑ Bu?c 1.7 ∑ EDIT
- **File:** src/components/layout/TopRibbon.tsx
- **Vi?c d„ l‡m:** Vi?t l?i TopRibbon v?i c?u tr˙c 2 t?ng, thÍm c?m user bÍn gÛc ph?i, ·p d?ng d˙ng token m‡u v‡ icon.
- **L˝ do:** –?ng b? giao di?n navbar.

### [#020] 2026-09-27 12:18 ∑ Sprint 1 ∑ Bu?c 1.8 ∑ EDIT
- **File:** src/components/common/PrintInvoice.tsx
- **Vi?c d„ l‡m:** C?p nh?t h‡m formatMoney d˘ng ormatNumber.
- **L˝ do:** ¡p d?ng h‡m chung.

### [#021] 2026-09-27 12:19 ∑ Sprint 1 ∑ Bu?c 1.9 ∑ CREATE
- **File:** src/shared/utils/__tests__/format.test.ts, 	ests/ui/shell.spec.ts
- **Vi?c d„ l‡m:** Vi?t Unit Test cho format v‡ E2E Test cho Shell.
- **L˝ do:** Verification.

### [#022] 2026-09-27 12:20 ∑ Sprint 1 ∑ Bu?c 1.10 ∑ RUN
- **L?nh:** 
px vitest run, 
px playwright test, 
pm run build
- **K?t qu?:** Tests pass, build pass.
- **Vi?c d„ l‡m:** Ch?y ki?m th? t? d?ng, s?a l?i CSS.

### [#023] 2026-09-27 12:23 ∑ Sprint 1 ∑ CHECKPOINT
- **Vi?c d„ l‡m:** Xong t?t c? bu?c Sprint 1.
- **Ti?n d?:** ¡p d?ng token m‡u, setup format utilities, fix layout MainContent/TopRibbon/TabBar, pass to‡n b? unit/E2E test.
- **V?n d? / L?ch plan:** KhÙng cÛ.
### [#024] 2026-09-27 12:26 ∑ Sprint 2 ∑ Bu?c 2.1 & 2.2 ∑ EDIT
- **File:** src/components/common/SearchableCombobox.tsx
- **Vi?c d„ l‡m:** Vi?t l?i ho‡n to‡n SearchableCombobox, h? tr? debounce fetch 250ms, render b?ng createPortal v?i v? trÌ t? d?ng tÌnh to·n (m? trÍn/du?i) d?a v‡o viewport. X? l˝ navigation b‡n phÌm v‡ highlight t? khÛa.
- **L˝ do:** Kh?c ph?c l?i dropdown b? c?t b?i overflow, tang tr?i nghi?m ngu?i d˘ng (C1-C7).

### [#025] 2026-09-27 12:26 ∑ Sprint 2 ∑ Bu?c 2.3 ∑ EDIT
- **File:** src/components/common/SearchModal.tsx
- **Vi?c d„ l‡m:** C?p nh?t UI theo token m?i (card, .erp-input), s?a logic debounce tÏm ki?m b?ng c·ch clear timer cu tru?c khi d?t timer m?i.
- **L˝ do:** Chu?n hÛa giao di?n modal tÏm ki?m.

### [#026] 2026-09-27 12:27 ∑ Sprint 2 ∑ Bu?c 2.4 ∑ CREATE
- **File:** src/components/common/quick-create/QuickCreateDialog.tsx, QuickCreateCustomer.tsx, QuickCreateProduct.tsx, QuickCreateSupplier.tsx
- **Vi?c d„ l‡m:** Kh?i t?o b? component "ThÍm nhanh" cho phÈp t?o d? li?u tr?c ti?p t? c·c khung dropdown tÏm ki?m n?u chua cÛ.
- **L˝ do:** Tang t?c quy trÏnh nh?p li?u.

### [#027] 2026-09-27 12:27 ∑ Sprint 2 ∑ Bu?c 2.5 ∑ EDIT
- **File:** src/components/layout/MdiModuleLayout.tsx
- **Vi?c d„ l‡m:** ThÍm if (e.defaultPrevented) return; v‡o d?u handleKeyDown.
- **L˝ do:** KhÙng trigger phÌm t?t form n?u c·c popup (nhu QuickCreate, Combobox) d„ stopPropagation.

### [#028] 2026-09-27 12:28 ∑ Sprint 2 ∑ Test ∑ CREATE
- **File:** src/components/common/__tests__/SearchableCombobox.test.tsx, 	ests/ui/combobox.spec.ts
- **Vi?c d„ l‡m:** Vi?t c·c Unit Test v‡ E2E Test mÙ ph?ng tuong t·c th?c t? v?i SearchableCombobox. –„ s?a locator E-CB-03 v‡ c?p nh?t thu vi?n ireEvent.
- **L˝ do:** –?m b?o tÌnh chÌnh x·c v‡ khÙng l˘i ch?c nang.

### [#029] 2026-09-27 12:39 ∑ Sprint 2 ∑ RUN
- **L?nh:** 
px vitest run, 
px playwright test, 
pm run build
- **K?t qu?:** Tests pass, build pass.
- **Vi?c d„ l‡m:** Ch?y ki?m th? t? d?ng.

### [#030] 2026-09-27 12:40 ∑ Sprint 2 ∑ CHECKPOINT
- **Vi?c d„ l‡m:** Ho‡n th‡nh c·c tÌnh nang c?a Sprint 2: Dropdown tÏm ki?m g?i ˝.
- **Ti?n d?:** Ho?t d?ng ?n d?nh, position thÙng minh khÙng tr‡n vi?n, h? tr? keyboard ho‡n ch?nh. ThÍm ch?c nang QuickCreate.
- **V?n d? / L?ch plan:** Do thi?u thu vi?n user-event nÍn chuy?n sang d˘ng ireEvent thu?n c?a 	esting-library/react cho test, d?m b?o khÙng thÍm dependencies m?i.

[#301] 2026-09-27 13:00 ∑ Sprint 3 ∑ Bu?c 3.6 ∑ IMPLEMENT
- –„ s?a c·c l?i Type mismatch trong SalesOrderForm v‡ GenericDocumentForm.
- T?o v‡ update test case Unit (NumberInput, GenericDocumentForm, MdiModuleLayout).
- Ch?y build v‡ unit tests th‡nh cÙng.

[#401] 2026-09-27 13:17 ∑ Sprint 4 + 5 ∑ Bu?c 1, 2, 3 ∑ IMPLEMENT
- Refactor InboundReceiptModule v‡ InboundReceiptForm ·p d?ng chung MdiModuleLayout v‡ GenericDocumentForm.
- C?p nh?t payload ApiService cho InboundReceipt.
- Kh?i t?o GoodsReturnModule v‡ GoodsReturnForm tuong t? Inbound.
- B? sung ConfirmDialog cho thao t·c H?y phi?u.
- B? sung n˙t b?m Nh?p L?i H‡ng B·n trong TopRibbon.
- Chuy?n test E2E cu v‡o tests/_legacy.
- S?a c·c l?i Typescript liÍn quan type string/number c?a ID.

[#601] 2026-09-27 13:22 ∑ Sprint 6 ∑ Bu?c 1, 2, 3, 4, 5, 11, 12 ∑ IMPLEMENT
- Refactor Login.tsx (chia 2 panel lg:grid-cols-[55%_45%], gradient).
- ThÍm mock test ENV.isDev.
- Vi?t 4 test case Unit Login.test.tsx th‡nh cÙng.
- Vi?t 3 test case E2E login.spec.ts th‡nh cÙng.

[#701] 2026-09-27 13:30 ∑ Sprint 7 ∑ Bu?c 1.5, 7.1, 7.2 ∑ IMPLEMENT
- ThÍm dashboardPeriod.ts v‡ PageContainer.tsx.
- X‚y d?ng l?i Dashboard.tsx theo thi?t k? m?i s? d?ng PageContainer v‡ StatCard.
- Pass t?t c? Unit Test v‡ E2E Test cho Dashboard.
- T?m th?i wrap c·c trang danh s·ch b?ng PageContainer.

[#799] 2026-09-27 13:31 ∑ Sprint 7 ∑ CHECKPOINT
- –„ ho‡n th‡nh to‡n b? Sprint 6 (Login) v‡ Sprint 7 (Dashboard).
- Pass 100% Unit Test v‡ E2E Test cho Login v‡ Dashboard.
- S?n s‡ng chuy?n sang Sprint 8 (–?ng b? danh s·ch).

[#801] 2026-09-27 13:45 ∑ Sprint 8 ∑ Bu?c G1-G8, R8 ∑ IMPLEMENT
- Refactor c?u tr˙c UI (PageContainer, PageHeader, DataState) cho to‡n b? m‡n hÏnh danh s·ch.
- Rewrite DataState, ConfirmDialog, ErrorBoundary theo kÌch thu?c m?i.
- Chu?n hÛa text-right (.num) cho c·c c?t S? lu?ng / S? ti?n.
- S? d?ng token h? th?ng, lo?i b? to‡n b? c·c token m‡u m?c d?nh (teal-, indigo-, red-, bg-white) trong src/
- Vi?t t‡i li?u chu?n m?c DESIGN_SYSTEM.md.
- Pass t?t c? Unit Tests v‡ dang ch?y E2E tests.


## T·ªïng k·∫øt ƒë·ªÉ r√† so√°t (Sprint 9)

### 1. B·∫£ng thay ƒë·ªïi file ch√≠nh
| File / Th∆∞ m·ª•c | Lo·∫°i thay ƒë·ªïi | Sprint | Ghi ch√∫ |
|---|---|---|---|
| src/components/layout/TopRibbon.tsx | S·ª≠a | 2, 4, 6 | ƒê·ªïi c·∫•u tr√∫c tab, th√™m n√∫t Nh·∫≠p l·∫°i h√†ng b√°n |
| src/components/layout/MainContent.tsx | S·ª≠a | 2 | H·ªó tr·ª£ TabContext |
| src/components/auth/Login.tsx | S·ª≠a (L√†m l·∫°i) | 6 | Layout 2 c·ªôt, xo√° gradient c≈©, c√≥ dev hint |
| src/components/sales/SalesOrderForm.tsx | S·ª≠a (L√†m l·∫°i) | 3 | D√πng GenericDocumentForm, chia 3 kh·ªëi |
| src/components/inventory/InboundReceiptForm.tsx | T·∫°o m·ªõi | 4 | T√°i s·ª≠ d·ª•ng GenericDocumentForm |
| src/components/inventory/GoodsReturnForm.tsx | T·∫°o m·ªõi | 5 | Phi·∫øu nh·∫≠p l·∫°i h√†ng b√°n (ch∆∞a c√≥ API backend) |
| src/components/common/document/GenericDocumentForm.tsx | T·∫°o m·ªõi | 3 | Khung chu·∫©n cho to√†n b·ªô phi·∫øu |
| src/shared/components/DataState/*.tsx | S·ª≠a | 8 | X√≥a m√†u c≈©, chu·∫©n h√≥a EmptyState, ErrorState |
| src/components/catalog/ProductList.tsx, CustomerList.tsx, v.v... | S·ª≠a | 8 | ƒê·ªìng b·ªô layout PageContainer, x√≥a th·∫ª DataState l·ªìng nhau |
| src/index.css | S·ª≠a | 0, 1, 8 | Th√™m bi·∫øn m√†u CSS, class .num, .erp-input |
| docs/DESIGN_SYSTEM.md | T·∫°o m·ªõi | 8 | B·ªô quy t·∫Øc UI chu·∫©n |
| 	ests/ui/*.spec.ts | T·∫°o m·ªõi | 3-9 | Th√™m b·ªô E2E Test chu·∫©n UI m·ªõi |
| 	ests/_legacy/ | Di chuy·ªÉn | 9 | Ch·ª©a 32 test c≈© h·ªèng giao di·ªán |

### 2. Danh s√°ch data-testid
- **ƒê√£ th√™m m·ªõi:**
  - 	ab-<id>, 
ibbon-user, 
ibbon-logout, 
ibbon-btn-return
  - combobox-dropdown, combobox-create-new, doc-code
  - sum-total, sum-discount, sum-final
  - inbound-*, 
eturn-*
  - login-toggle-password, login-dev-hint
  - dashboard-page, dashboard-branch, dashboard-period, dashboard-date-start, dashboard-date-end, dashboard-filter-btn, dashboard-export-btn
  - metric-revenue, metric-profit, metric-turnover, metric-debt
  - 	op-products-chart, 	op-products-table, 	op-product-row
  - <prefix>-list-search, <prefix>-list-row, <prefix>-action-view
- **ƒê√£ b·ªè:**
  - login-form (thay b·∫±ng form g·ªëc kh√¥ng c√≥ testid c·ª• th·ªÉ ho·∫∑c d√πng submit tr·ª±c ti·∫øp)
  - L√Ω do: M·ªôt s·ªë test c≈© b·ªã h·ªèng v√¨ c·∫•u tr√∫c DOM thay ƒë·ªïi ho√†n to√†n, kh√¥ng th·ªÉ gi·ªØ testid c≈© tr√™n c√πng element t∆∞∆°ng ƒë∆∞∆°ng, n√™n ƒë√£ d·ªçn d·∫πp v√† vi·∫øt test m·ªõi cho UI.

### 3. DEVIATION & ISSUE c√≤n m·ªü
- **ISSUE (Sprint 5):** GoodsReturnForm ch∆∞a c√≥ API backend. ƒêang s·ª≠ d·ª•ng dummy API v√† mock responses.
- **ISSUE (Sprint 8):** C·∫£nh b√°o t·ªìn kho th·∫•p/√¢m ch∆∞a c√≥ API (ƒë√£ quy ƒë·ªãnh ·ªü Ph·ª• L·ª•c B).

### 4. K·∫øt qu·∫£ ki·ªÉm tra cu·ªëi c√πng so v·ªõi baseline
- **Baseline (Sprint 0):** 	sc 19 l·ªói.
- **Hi·ªán t·∫°i (Sprint 9):** 	sc 0 l·ªói. itest pass 100%. playwright (tests/ui) pass 100%.

### 5. Nh·ªØng ƒëi·ªÉm ch∆∞a ch·∫Øc ch·∫Øn c·∫ßn review k·ªπ
- L·ªçc ProductList: API tr·∫£ v·ªÅ code hay productCode? Hi·ªán ƒëang l·ªçc tr√™n frontend b·∫±ng c√°ch check c·∫£ hai. N·∫øu backend ph√¢n trang, filter frontend s·∫Ω kh√¥ng ch√≠nh x√°c.
- Responsive ·ªü 1366x768 c·ªßa phi·∫øu b√°n h√†ng / phi·∫øu nh·∫≠p c√≥ th·ªÉ b·ªã ch·∫≠t n·∫øu t√™n s·∫£n ph·∫©m qu√° d√†i, v√¨ hi·ªán ƒëang fix c·ª©ng % width cho c√°c c·ªôt.
