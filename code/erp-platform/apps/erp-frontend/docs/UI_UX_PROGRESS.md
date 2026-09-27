# Tiáº¿n Ä‘á»™ UI/UX Revamp - erp-frontend

## Sprint 0: Chuáº©n bá»‹, Ä‘o baseline, dá»n dáº¹p

### Káº¿t quáº£ Ä‘o baseline:
- `npx tsc --noEmit -p tsconfig.json`: 19 lá»—i
- `npm run lint`: 0 lá»—i, 132 warnings
- `npx vitest run`: 5/5 tests passed
- `npm run build`: Pass

### CÃ¡c file Ä‘Ã£ chuyá»ƒn vÃ o `_to_delete/`:
(Sáº½ cáº­p nháº­t sau khi hoÃ n thÃ nh)

## SPRINT 3: Phi?u bán hàng & Bàn phím
- Hoàn thành layout m?i cho \GenericDocumentForm\.
- Chuy?n d?i \SalesOrderForm\ sang API m?i.
- B? sung \NumberInput\ và c?p nh?t Type/Interface.
- X? lý l?i TS và hoàn thành Unit Tests (E2E dang fail do chuy?n d?i qua tab architecture, s? du?c fix sau).

## SPRINT 4 + 5: Phi?u Nh?p & Phi?u Tr? Dùng Chung Khung
- Hoàn thành giao di?n InboundReceiptForm v?i GenericDocumentForm.
- C?p nh?t payload dúng chu?n c?a Create Inbound.
- Tái s? d?ng code t?o ti?p GoodsReturnForm thành công (Phi?u Nh?p L?i Hàng Bán).
- Tích h?p 2 Module lên TopRibbon thành các tab.
- Ðã s?a toàn b? l?i TS và ch?y build thành công.

## SPRINT 6: Màn hình Ðang nh?p
- Thi?t k? l?i trang Login theo layout 2 c?t.
- Pass t?t c? Unit Test và E2E Test cho Login.

## SPRINT 7: Dashboard (E1-E5)
- Chuy?n layout sang PageContainer và PageHeader.
- Hoàn thi?n Top Products và các ch? s? Metrics Dashboard.
- Pass t?t c? Unit Test và E2E Test cho Dashboard.

## SPRINT 8: Ð?ng b? các màn hình danh sách & hoàn thi?n (G5-G8)
- C?p nh?t toàn b? các trang danh m?c s? d?ng c?u trúc chu?n PageContainer > PageHeader > card > DataState > erp-table.
- Vi?t l?i DataState, ErrorBoundary, ConfirmDialog.
- Xóa toàn b? các màu cu (teal-, red-, bg-white, v.v...) và thay th? b?ng Design Tokens m?i.
- Vi?t tài li?u DESIGN_SYSTEM.md chu?n xác.
- E2E Tests lists.spec.ts passed.
