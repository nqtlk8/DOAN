# Ti·∫øn ƒë·ªô UI/UX Revamp - erp-frontend

## Sprint 0: Chu·∫©n b·ªã, ƒëo baseline, d·ªçn d·∫πp

### K·∫øt qu·∫£ ƒëo baseline:
- `npx tsc --noEmit -p tsconfig.json`: 19 l·ªói
- `npm run lint`: 0 l·ªói, 132 warnings
- `npx vitest run`: 5/5 tests passed
- `npm run build`: Pass

### C√°c file ƒë√£ chuy·ªÉn v√†o `_to_delete/`:
(S·∫Ω c·∫≠p nh·∫≠t sau khi ho√†n th√†nh)

## SPRINT 3: Phi?u b·n h‡ng & B‡n phÌm
- Ho‡n th‡nh layout m?i cho \GenericDocumentForm\.
- Chuy?n d?i \SalesOrderForm\ sang API m?i.
- B? sung \NumberInput\ v‡ c?p nh?t Type/Interface.
- X? l˝ l?i TS v‡ ho‡n th‡nh Unit Tests (E2E dang fail do chuy?n d?i qua tab architecture, s? du?c fix sau).

## SPRINT 4 + 5: Phi?u Nh?p & Phi?u Tr? D˘ng Chung Khung
- Ho‡n th‡nh giao di?n InboundReceiptForm v?i GenericDocumentForm.
- C?p nh?t payload d˙ng chu?n c?a Create Inbound.
- T·i s? d?ng code t?o ti?p GoodsReturnForm th‡nh cÙng (Phi?u Nh?p L?i H‡ng B·n).
- TÌch h?p 2 Module lÍn TopRibbon th‡nh c·c tab.
- –„ s?a to‡n b? l?i TS v‡ ch?y build th‡nh cÙng.

## SPRINT 6: M‡n hÏnh –ang nh?p
- Thi?t k? l?i trang Login theo layout 2 c?t.
- Pass t?t c? Unit Test v‡ E2E Test cho Login.

## SPRINT 7: Dashboard (E1-E5)
- Chuy?n layout sang PageContainer v‡ PageHeader.
- Ho‡n thi?n Top Products v‡ c·c ch? s? Metrics Dashboard.
- Pass t?t c? Unit Test v‡ E2E Test cho Dashboard.
