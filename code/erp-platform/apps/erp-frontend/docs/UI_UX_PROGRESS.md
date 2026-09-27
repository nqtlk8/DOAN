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
- X? lı l?i TS và hoàn thành Unit Tests (E2E dang fail do chuy?n d?i qua tab architecture, s? du?c fix sau).

## SPRINT 4 + 5: Phi?u Nh?p & Phi?u Tr? Dùng Chung Khung
- Hoàn thành giao di?n InboundReceiptForm v?i GenericDocumentForm.
- C?p nh?t payload dúng chu?n c?a Create Inbound.
- Tái s? d?ng code t?o ti?p GoodsReturnForm thành công (Phi?u Nh?p L?i Hàng Bán).
- Tích h?p 2 Module lên TopRibbon thành các tab.
- Ğã s?a toàn b? l?i TS và ch?y build thành công.

## SPRINT 6: Màn hình Ğang nh?p
- Thi?t k? l?i trang Login theo layout 2 c?t.
- Pass t?t c? Unit Test và E2E Test cho Login.
