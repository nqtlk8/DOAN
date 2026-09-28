# Plan & Prompt cho Gemini â€“ HoÃ n thiá»‡n bÃ¡o cÃ¡o

Sep 24, 2026 Â· @Thang

Gemini hoÃ n thiá»‡n ChÆ°Æ¡ng 3 qua 9 sprint (S0â€“S8), má»—i sprint lÃ  má»™t cuá»™c trÃ² chuyá»‡n má»›i, chá»‰ Ä‘Ã­nh kÃ¨m Ä‘Ãºng file cáº§n thiáº¿t vÃ  má»i tÃªn lá»›p/báº£ng/bÆ°á»›c nghiá»‡p vá»¥ pháº£i trÃ­ch dáº«n nguá»“n â€” cÃ¡ch nÃ y giá»¯ context nhá» vÃ  cháº·n áº£o giÃ¡c.

## 1. Má»¥c tiÃªu

ChÆ°Æ¡ng 3 má»›i gá»“m 6 má»¥c theo Ä‘Ãºng trÃ¬nh tá»± bÃ i giáº£ng: **Use Case â†’ Activity â†’ DFD â†’ Class â†’ Sequence â†’ ERD**. ChÆ°Æ¡ng 1, 2, 4 giá»¯ nguyÃªn. Gemini pháº£i bá»• sung 8 háº¡ng má»¥c cÃ²n thiáº¿u vÃ  rÃ  soÃ¡t 6 hÃ¬nh Ä‘ang cÃ³.

| Má»¥c | Ná»™i dung | Hiá»‡n tráº¡ng | Gemini cáº§n lÃ m | Sprint |
| --- | --- | --- | --- | --- |
| 3.1.1 | Use Case tá»•ng quÃ¡t (Admin, Staff) | CÃ³ (HÃ¬nh 2) | RÃ  soÃ¡t tÃªn UC, mÃ£ UC, sá»‘ UC má»—i sÆ¡ Ä‘á»“ theo quy chuáº©n | S1 |
| 3.1.2 | Äáº·c táº£ Use Case chÃ­nh | **Thiáº¿u** | Viáº¿t má»›i theo máº«u bÃ i giáº£ng | S1 |
| 3.2.1 | Activity â€“ BÃ¡n hÃ ng (Sales Invoice) | CÃ³ (HÃ¬nh 3) | Chuyá»ƒn vá»‹ trÃ­, rÃ  soÃ¡t kÃ½ hiá»‡u | S2 |
| 3.2.2 | Activity â€“ Tráº£ hÃ ng (Goods Return) | **Thiáº¿u** | Váº½ má»›i | S2 |
| 3.2.3 | Activity â€“ Nháº­p kho (Inbound Receipt) | **Thiáº¿u** | Váº½ má»›i | S2 |
| 3.3.1 | DFD cáº¥p 0 | CÃ³ (HÃ¬nh 8, chÆ°a rÃµ cáº¥p) | Chuyá»ƒn vá»‹ trÃ­, Ä‘áº·t Ä‘Ãºng tÃªn cáº¥p | S3 |
| 3.3.2 | DFD cho tá»«ng yÃªu cáº§u chÃ­nh (máº«u D1â€“D6 cá»§a bÃ i giáº£ng) | **Thiáº¿u** | Váº½ má»›i + thuáº­t toÃ¡n xá»­ lÃ½ | S3 |
| 3.4 | Class Diagram + 3 báº£ng mÃ´ táº£ | **Thiáº¿u** | Váº½ má»›i | S4 |
| 3.5.1, 3.5.2, 3.5.5, 3.5.6 | 4 Sequence Ä‘ang cÃ³ (ÄÄƒng nháº­p/JWT, Sales Invoice, GiÃ¡ riÃªng, Replication) | CÃ³ (HÃ¬nh 4â€“7) | Äá»‘i chiáº¿u tÃªn lá»›p vá»›i 3.4, sá»­a chá»— lá»‡ch | S5 |
| 3.5.3 | Sequence â€“ Tráº£ hÃ ng | **Thiáº¿u** | Váº½ má»›i | S5 |
| 3.5.4 | Sequence â€“ Nháº­p kho | **Thiáº¿u** | Váº½ má»›i | S5 |
| 3.6 | ERD (kÃ½ hiá»‡u Chen) + tá»« Ä‘iá»ƒn dá»¯ liá»‡u | **Thiáº¿u** | Váº½ má»›i | S6 |
| â€” | ÄÃ¡nh sá»‘ má»¥c, danh sÃ¡ch hÃ¬nh | Nháº£y sá»‘ 3.3 â†’ 3.5 | ÄÃ¡nh láº¡i toÃ n bá»™ | S7 |

So vá»›i outline trÆ°á»›c, má»¥c 3.3.2 Ä‘Æ°á»£c bá»• sung vÃ¬ bÃ i giáº£ng dÃ¹ng máº«u DFD cho tá»«ng yÃªu cáº§u (lÆ°u trá»¯, tra cá»©u, tÃ­nh toÃ¡n, bÃ¡o biá»ƒu) vá»›i cÃ¡c dÃ²ng D1â€“D6 vÃ  thuáº­t toÃ¡n xá»­ lÃ½.

**Sáº£n pháº©m cá»§a má»—i hÃ¬nh:** mÃ£ PlantUML hoáº·c Mermaid do Gemini sinh (báº¡n tá»± render báº±ng PlantText, plantuml.com hoáº·c draw.io), má»™t cÃ¢u dáº«n, chÃº thÃ­ch hÃ¬nh vÃ  1â€“2 Ä‘oáº¡n giáº£i thÃ­ch tiáº¿ng Viá»‡t.

## 2. CÃ¡ch thá»±c hiá»‡n

Sprint 0 táº¡o **FACTS.md** â€” báº£n ghi sá»± tháº­t rÃºt tá»« mÃ£ nguá»“n vÃ  bÃ¡o cÃ¡o. Má»i sprint sau chá»‰ Ä‘Æ°á»£c láº¥y tÃªn lá»›p, thuá»™c tÃ­nh, phÆ°Æ¡ng thá»©c tá»« file nÃ y, nÃªn Gemini khÃ´ng cÃ²n chá»— Ä‘á»ƒ bá»‹a.

### NguyÃªn táº¯c chá»‘ng áº£o giÃ¡c

1. **Má»™t sprint = má»™t cuá»™c trÃ² chuyá»‡n má»›i.** KhÃ´ng lÃ m 2 sprint trong cÃ¹ng má»™t chat.
2. **Chá»‰ Ä‘Ã­nh kÃ¨m Ä‘Ãºng file sprint cáº§n** (xem cá»™t Äáº§u vÃ o á»Ÿ má»¥c 6). KhÃ´ng Ä‘Ã­nh kÃ¨m cáº£ repo hay cáº£ file bÃ¡o cÃ¡o.
3. **Thá»© báº­c nguá»“n sá»± tháº­t:** mÃ£ nguá»“n (entity, Flyway migration, service) â†’ FACTS.md Ä‘Ã£ duyá»‡t â†’ bÃ¡o cÃ¡o hiá»‡n táº¡i. BÃ i giáº£ng chá»‰ lÃ  nguá»“n quy chuáº©n kÃ½ hiá»‡u, khÃ´ng pháº£i nguá»“n dá»¯ liá»‡u nghiá»‡p vá»¥.
4. **TrÃ­ch dáº«n báº¯t buá»™c:** má»—i pháº§n tá»­ ghi nguá»“n `[file:dÃ²ng]`, `[BC x.y]` hoáº·c `[FACTS E05]`. KhÃ´ng cÃ³ nguá»“n thÃ¬ ghi `[Cáº¦N XÃC NHáº¬N: â€¦]`.
5. **HANDOFF â‰¤ 250 tá»«** cuá»‘i má»—i sprint: chá»‰ ghi Ä‘iá»u Ä‘Ã£ chá»‘t (mÃ£ UC, tÃªn lá»›p, tÃªn hÃ¬nh) vÃ  viá»‡c cÃ²n má»Ÿ. Sprint sau dÃ¡n HANDOFF thay vÃ¬ dÃ¡n láº¡i toÃ n bá»™ káº¿t quáº£ cÅ©.
6. **Cá»•ng duyá»‡t:** báº¡n kiá»ƒm tra checklist, xá»­ lÃ½ háº¿t `[Cáº¦N XÃC NHáº¬N]` cá»§a sprint Ä‘Ã³ rá»“i má»›i sang sprint sau.
7. **Nháº­n hÃ¬nh qua mÃ£, khÃ´ng qua áº£nh:** vá»›i 6 hÃ¬nh Ä‘ang cÃ³, báº¡n dÃ¡n mÃ£ nguá»“n Mermaid/draw.io náº¿u cÃ²n giá»¯; náº¿u chá»‰ cÃ³ áº£nh, Ä‘Ã­nh kÃ¨m áº£nh vÃ  yÃªu cáº§u Gemini chÃ©p láº¡i danh sÃ¡ch pháº§n tá»­ trÆ°á»›c khi nháº­n xÃ©t.

### Luá»“ng sprint

```mermaid
flowchart LR
  S0[S0 FACTS.md] --> S1[S1 Use Case]
  S1 --> S2[S2 Activity]
  S2 --> S3[S3 DFD]
  S3 --> S4[S4 Class]
  S4 --> S5[S5 Sequence]
  S5 --> S6[S6 ERD]
  S6 --> S7[S7 Láº¯p rÃ¡p]
  S7 --> S8[S8 QA]
  S5 -.cáº­p nháº­t lá»›p.-> S4
```

MÅ©i tÃªn nÃ©t Ä‘á»©t lÃ  bÆ°á»›c 5 trong quy trÃ¬nh váº½ Sequence cá»§a bÃ i giáº£ng: phÃ¡t hiá»‡n phÆ°Æ¡ng thá»©c má»›i thÃ¬ quay láº¡i cáº­p nháº­t Class Diagram.

### ThÆ° má»¥c lÃ m viá»‡c

| File | Vai trÃ² | Ai táº¡o |
| --- | --- | --- |
| `FACTS.md` | Nguá»“n sá»± tháº­t duy nháº¥t: entity, quan há»‡, phÆ°Æ¡ng thá»©c, quy táº¯c nghiá»‡p vá»¥ cÃ³ mÃ£ ID | Gemini (S0), báº¡n duyá»‡t |
| `HANDOFF.md` | Nháº­t kÃ½ chuyá»ƒn giao, cá»™ng dá»“n tá»«ng sprint | Báº¡n dÃ¡n tá»« má»¥c E cá»§a Gemini |
| `BC_ch1-2.md` | ChÆ°Æ¡ng 1â€“2 bÃ¡o cÃ¡o xuáº¥t ra Markdown (nháº¹ hÆ¡n .docx) | Báº¡n |
| `out/S1_usecase.md` â€¦ `out/S6_erd.md` | Káº¿t quáº£ tá»«ng sprint | Gemini |
| `CH3_final.md` | ChÆ°Æ¡ng 3 hoÃ n chá»‰nh Ä‘á»ƒ dÃ¡n vÃ o Word | Gemini (S7) |

**CÃ´ng cá»¥:** khuyÃªn dÃ¹ng Gemini CLI má»Ÿ táº¡i thÆ° má»¥c `erp-platform/` (tham chiáº¿u file báº±ng `@Ä‘Æ°á»ng_dáº«n`). Náº¿u dÃ¹ng Gemini web hoáº·c AI Studio thÃ¬ Ä‘Ã­nh kÃ¨m tá»«ng file theo báº£ng Äáº§u vÃ o. Äá»ƒ láº¥y danh sÃ¡ch entity cáº§n Ä‘Ã­nh kÃ¨m, cháº¡y trong repo: `grep -rl "@Entity" services/erp-backend/src/main/java`.

## 3. Prompt há»‡ thá»‘ng

DÃ¡n vÃ o Gem Instructions (Gemini web), `GEMINI.md` á»Ÿ thÆ° má»¥c gá»‘c (Gemini CLI) hoáº·c System Instructions (AI Studio). Náº¿u khÃ´ng dÃ¹ng Ä‘Æ°á»£c cÃ¡c chá»— Ä‘Ã³, dÃ¡n lÃªn Ä‘áº§u má»—i sprint.

```text
VAI TRÃ’
Báº¡n lÃ  trá»£ lÃ½ phÃ¢n tÃ­ch thiáº¿t káº¿ há»‡ thá»‘ng, há»— trá»£ hoÃ n thiá»‡n khÃ³a luáº­n vá» há»‡ thá»‘ng ERP bÃ¡n láº» Ä‘a chi nhÃ¡nh theo mÃ´ hÃ¬nh HQ/Branch (Spring Boot, PostgreSQL, Modular Monolith, JWT RS256, PostgreSQL Logical Replication).
NgÃ´n ngá»¯ Ä‘áº§u ra: tiáº¿ng Viá»‡t há»c thuáº­t. TÃªn lá»›p, báº£ng, cá»™t, phÆ°Æ¡ng thá»©c giá»¯ nguyÃªn nhÆ° trong mÃ£ nguá»“n.

Má»¤C TIÃŠU Tá»”NG
HoÃ n thiá»‡n ChÆ°Æ¡ng 3 theo trÃ¬nh tá»± báº¯t buá»™c:
3.1 Use Case (+ Ä‘áº·c táº£) -> 3.2 Activity -> 3.3 DFD -> 3.4 Class Diagram -> 3.5 Sequence -> 3.6 ERD.
Má»—i phiÃªn chá»‰ lÃ m ÄÃšNG sprint Ä‘Æ°á»£c giao.

QUY Táº®C CHá»NG áº¢O GIÃC (báº¯t buá»™c)
1. Chá»‰ dÃ¹ng thÃ´ng tin trong cÃ¡c file Ä‘Ã­nh kÃ¨m á»Ÿ phiÃªn nÃ y. KhÃ´ng dÃ¹ng kiáº¿n thá»©c chung Ä‘á»ƒ Ä‘oÃ¡n tÃªn lá»›p, thuá»™c tÃ­nh, báº£ng, API hay bÆ°á»›c nghiá»‡p vá»¥.
2. Má»—i tÃªn lá»›p, thuá»™c tÃ­nh, báº£ng, phÆ°Æ¡ng thá»©c, bÆ°á»›c nghiá»‡p vá»¥ pháº£i kÃ¨m nguá»“n: [tÃªn_file:dÃ²ng], [BC x.y] (má»¥c bÃ¡o cÃ¡o) hoáº·c [FACTS <ID>].
3. KhÃ´ng tÃ¬m tháº¥y nguá»“n -> ghi [Cáº¦N XÃC NHáº¬N: <cÃ¢u há»i cá»¥ thá»ƒ>] rá»“i lÃ m tiáº¿p. Tuyá»‡t Ä‘á»‘i khÃ´ng tá»± Ä‘iá»n.
4. BÃ i giáº£ng chá»‰ lÃ  nguá»“n QUY CHUáº¨N KÃ HIá»†U, khÃ´ng pháº£i nguá»“n dá»¯ liá»‡u nghiá»‡p vá»¥.
5. MÃ£ nguá»“n mÃ¢u thuáº«n vá»›i bÃ¡o cÃ¡o -> theo mÃ£ nguá»“n vÃ  ghi vÃ o má»¥c PhÃ¡t hiá»‡n.
6. KhÃ´ng viáº¿t pháº§n ngoÃ i pháº¡m vi sprint. KhÃ´ng tÃ³m táº¯t láº¡i sprint trÆ°á»›c.
7. Thiáº¿u file Ä‘áº§u vÃ o cáº§n thiáº¿t -> Dá»ªNG, liá»‡t kÃª file cáº§n bá»• sung, khÃ´ng suy Ä‘oÃ¡n.
8. Vá»›i hÃ¬nh Ä‘Æ°á»£c Ä‘Ã­nh kÃ¨m dáº¡ng áº£nh: trÆ°á»›c háº¿t chÃ©p láº¡i danh sÃ¡ch pháº§n tá»­ Ä‘á»c Ä‘Æ°á»£c, Ä‘Ã¡nh dáº¥u [KHÃ”NG Äá»ŒC RÃ•] náº¿u má», rá»“i má»›i nháº­n xÃ©t.

Äá»ŠNH Dáº NG Äáº¦U RA Má»–I SPRINT (Ä‘Ãºng thá»© tá»±)
A. Káº¿t quáº£ chÃ­nh: mÃ£ sÆ¡ Ä‘á»“ (PlantUML hoáº·c Mermaid, má»—i sÆ¡ Ä‘á»“ má»™t khá»‘i mÃ£ riÃªng), báº£ng, Ä‘oáº¡n vÄƒn bÃ¡o cÃ¡o kÃ¨m chÃº thÃ­ch "HÃ¬nh x: ...".
B. Báº£ng truy váº¿t: Pháº§n tá»­ | Nguá»“n.
C. Danh sÃ¡ch [Cáº¦N XÃC NHáº¬N] vÃ  PhÃ¡t hiá»‡n (mÃ¢u thuáº«n).
D. Tá»± kiá»ƒm tra: cháº¡y checklist cá»§a sprint, Ä‘Ã¡nh [x] hoáº·c [ ] tá»«ng dÃ²ng, giáº£i thÃ­ch dÃ²ng [ ].
E. HANDOFF (tá»‘i Ä‘a 250 tá»«): Ä‘Ã£ chá»‘t gÃ¬ (mÃ£ UC, tÃªn lá»›p, tÃªn hÃ¬nh), viá»‡c cÃ²n má»Ÿ. KhÃ´ng láº·p láº¡i ná»™i dung chi tiáº¿t.
```

## 4. Skill prompts 01â€“04

Má»—i skill lÃ  má»™t khá»‘i quy chuáº©n dÃ¡n kÃ¨m prompt sprint tÆ°Æ¡ng á»©ng. Quy táº¯c kÃ½ hiá»‡u láº¥y nguyÃªn tá»« 2 file bÃ i giáº£ng, nÃªn káº¿t quáº£ khá»›p vá»›i tiÃªu chÃ­ cháº¥m.

### SKILL-01 Â· TrÃ­ch xuáº¥t sá»± tháº­t (dÃ¹ng á»Ÿ S0)

```text
[SKILL-01: TRÃCH XUáº¤T Sá»° THáº¬T]
Nhiá»‡m vá»¥: Ä‘á»c file Ä‘Ã­nh kÃ¨m vÃ  láº­p FACTS. KHÃ”NG phÃ¢n tÃ­ch, KHÃ”NG thiáº¿t káº¿, KHÃ”NG bÃ¬nh luáº­n.
1. ENTITY (E01..): má»i lá»›p cÃ³ @Entity/@Table hoáº·c báº£ng trong file Flyway migration.
   Ghi: tÃªn lá»›p | tÃªn báº£ng | module (package) | cÃ¡c trÆ°á»ng {tÃªn, kiá»ƒu, rÃ ng buá»™c: @Id, nullable, unique, @Version, @Enumerated} | nguá»“n [file:dÃ²ng].
2. QUAN Há»† (R01..): @ManyToOne/@OneToMany/@OneToOne/@ManyToMany hoáº·c cá»™t *_id.
   Ghi báº£n sá»‘ theo annotation (optional=false = báº¯t buá»™c; cascade/orphanRemoval = sá»Ÿ há»¯u vÃ²ng Ä‘á»i).
   Chá»‰ lÆ°u ID mÃ  khÃ´ng cÃ³ annotation -> ghi "tham chiáº¿u logic qua <cá»™t>".
3. ENUM (N01..): tráº¡ng thÃ¡i chá»©ng tá»« (DRAFT, CONFIRMED...), vai trÃ² (ADMIN, STAFF), costBasis...
4. PHÆ¯Æ NG THá»¨C (M01..): phÆ°Æ¡ng thá»©c nghiá»‡p vá»¥ cá»§a Service/Facade/Entity: tÃªn | lá»›p | annotation (@Transactional, propagation, @Retryable, @PreAuthorize) | nguá»“n.
5. ENDPOINT (P01..): method | path | quyá»n | controller | nguá»“n.
6. QUY Táº®C NGHIá»†P Vá»¤ (B01..): cÃ¢u mÃ´ táº£ ngáº¯n láº¥y tá»« bÃ¡o cÃ¡o hoáº·c code, vÃ­ dá»¥ "createAndConfirm cháº¡y trong má»™t transaction [BC 2.1.2]".
7. ACTOR (A01..): chá»‰ láº¥y tá»« bÃ¡o cÃ¡o hoáº·c enum vai trÃ².
Äá»‹nh dáº¡ng: báº£ng Markdown theo tá»«ng nhÃ³m, ID liÃªn tá»¥c. Cuá»‘i báº£ng ghi tá»•ng sá»‘ pháº§n tá»­ má»—i nhÃ³m.
```

### SKILL-02 Â· Use Case (dÃ¹ng á»Ÿ S1)

```text
[SKILL-02: USE CASE THEO QUY CHUáº¨N BÃ€I GIáº¢NG]
CÃ¡c bÆ°á»›c: (1) xÃ¡c Ä‘á»‹nh Actor (ai/há»‡ thá»‘ng nÃ o tÆ°Æ¡ng tÃ¡c) -> (2) xÃ¡c Ä‘á»‹nh UC (actor dÃ¹ng chá»©c nÄƒng gÃ¬) -> (3) xÃ¡c Ä‘á»‹nh quan há»‡.
Quan há»‡: Actor-UC = association; UC-UC = <<include>> (báº¯t buá»™c, mÅ©i tÃªn nÃ©t Ä‘á»©t trá» vá» UC Ä‘Æ°á»£c dÃ¹ng), <<extend>> (cÃ³ Ä‘iá»u kiá»‡n, trá» vá» UC gá»‘c, ghi extension point), generalization; Actor-Actor = generalization.
LÆ°u Ã½ báº¯t buá»™c:
- TÃªn actor lÃ  danh tá»«; tÃªn UC = Ä‘á»™ng tá»« + danh tá»«; má»—i actor ná»‘i Ã­t nháº¥t 1 UC.
- CÃ¡c UC trong má»™t sÆ¡ Ä‘á»“ cÃ¹ng má»©c Ä‘á»™; tá»‘i Ä‘a khoáº£ng 10 UC má»—i sÆ¡ Ä‘á»“, nhiá»u hÆ¡n thÃ¬ tÃ¡ch sÆ¡ Ä‘á»“ con theo module.
- Gá»™p thÃªm/xÃ³a/sá»­a thÃ nh "Quáº£n lÃ½ <Ä‘á»‘i tÆ°á»£ng>"; táº­p trung cÃ¢u há»i WHAT, khÃ´ng mÃ´ táº£ HOW.
- Ghi mÃ£ UC (UC01...) trÃªn hÃ¬nh; cÃ³ system boundary; khÃ´ng Ä‘Æ°á»ng ná»‘i báº¯t chÃ©o (Ä‘Æ°á»£c váº½ 1 actor á»Ÿ 2 vá»‹ trÃ­).
Äáº·c táº£ má»—i UC (CÃ¡ch 1 cá»§a bÃ i giáº£ng), dáº¡ng báº£ng:
TÃªn UC | MÃ£ UC | MÃ´ táº£ tÃ³m táº¯t | Actor | YÃªu cáº§u trÆ°á»›c khi thá»±c hiá»‡n | CÃ¡c bÆ°á»›c thá»±c hiá»‡n | Äiá»u kiá»‡n thoÃ¡t | Äiá»u kiá»‡n sau khi thá»±c hiá»‡n | YÃªu cáº§u Ä‘áº·c biá»‡t.
Má»—i bÆ°á»›c thá»±c hiá»‡n pháº£i trá» vá» FACTS (M.., P.., B..).
Xuáº¥t sÆ¡ Ä‘á»“ báº±ng PlantUML (@startuml, left to right direction, rectangle cho system boundary).
```

### SKILL-03 Â· Activity (dÃ¹ng á»Ÿ S2)

```text
[SKILL-03: ACTIVITY DIAGRAM THEO QUY CHUáº¨N BÃ€I GIáº¢NG]
KÃ½ hiá»‡u: initial node, activity (hÃ¬nh chá»¯ nháº­t bo gÃ³c), control flow, decision/merge (hÃ¬nh thoi), fork/join (thanh Ä‘en), activity final, flow final, swimlane, note.
LÆ°u Ã½ báº¯t buá»™c:
- Má»—i hoáº¡t Ä‘á»™ng cÃ³ Ä‘Ãºng 1 Ä‘áº§u vÃ o vÃ  1 Ä‘áº§u ra (gá»™p nhÃ¡nh báº±ng merge node).
- TÃªn hoáº¡t Ä‘á»™ng = Ä‘á»™ng tá»« + danh tá»«; khÃ´ng trÃ¹ng tÃªn trong má»™t sÆ¡ Ä‘á»“.
- Má»i nhÃ¡nh ra tá»« decision pháº£i cÃ³ guard [Ä‘iá»u kiá»‡n] Ä‘áº§y Ä‘á»§.
- Háº¡n cháº¿ Ä‘Æ°á»ng cáº¯t nhau.
Swimlane: tÃ¡c nhÃ¢n nghiá»‡p vá»¥ (NhÃ¢n viÃªn chi nhÃ¡nh) | Há»‡ thá»‘ng. Chá»‰ tÃ¡ch thÃªm lane khi FACTS cÃ³ thÃ nh pháº§n tÆ°Æ¡ng á»©ng.
Má»—i hoáº¡t Ä‘á»™ng cá»§a lane Há»‡ thá»‘ng pháº£i trá» vá» FACTS M.. hoáº·c B.. trong báº£ng truy váº¿t.
NhÃ¡nh lá»—i (rollback, khÃ´ng Ä‘á»§ tá»“n, trÃ¹ng Idempotency-Key) chá»‰ váº½ khi cÃ³ trong FACTS.
Xuáº¥t PlantUML cÃº phÃ¡p activity má»›i (|lane|, :hoáº¡t Ä‘á»™ng;, if/then/else, fork).
```

### SKILL-04 Â· DFD (dÃ¹ng á»Ÿ S3)

```text
[SKILL-04: DFD THEO MáºªU BÃ€I GIáº¢NG]
Cáº¥p sÆ¡ Ä‘á»“: cáº¥p 0 = toÃ n bá»™ pháº§n má»m lÃ  má»™t khá»‘i xá»­ lÃ½; cáº¥p 1 phÃ¢n rÃ£ cáº¥p 0, pháº£i giá»¯ Ä‘á»§ tÃ¡c nhÃ¢n, thiáº¿t bá»‹, luá»“ng dá»¯ liá»‡u, xá»­ lÃ½, bá»™ nhá»› phá»¥ cá»§a cáº¥p 0.
Máº«u cho tá»«ng yÃªu cáº§u: khá»‘i NgÆ°á»i dÃ¹ng, Xá»­ lÃ½ <tÃªn>, Thiáº¿t bá»‹ nháº­p, Thiáº¿t bá»‹ xuáº¥t, Bá»™ nhá»› phá»¥ (2 váº¡ch ngang).
D1 = dá»¯ liá»‡u ngÆ°á»i dÃ¹ng nháº­p; D2 = dá»¯ liá»‡u xuáº¥t cho ngÆ°á»i dÃ¹ng; D3 = dá»¯ liá»‡u Ä‘á»c tá»« bá»™ nhá»›; D4 = dá»¯ liá»‡u ghi xuá»‘ng bá»™ nhá»›; D5 = tá»« thiáº¿t bá»‹ nháº­p; D6 = ra thiáº¿t bá»‹ xuáº¥t.
Theo loáº¡i yÃªu cáº§u:
- LÆ°u trá»¯: D1 theo biá»ƒu máº«u; D3 = danh má»¥c chá»n + dá»¯ liá»‡u kiá»ƒm tra há»£p lá»‡; D2 = danh má»¥c + káº¿t quáº£ thÃ nh cÃ´ng/tháº¥t báº¡i; thÃ´ng thÆ°á»ng D4 = D1 (+D5) (+ID tá»± sinh).
- Tra cá»©u: D1 = tiÃªu chÃ­ tÃ¬m; D3 = danh sÃ¡ch Ä‘á»‘i tÆ°á»£ng tÃ¬m tháº¥y; D2, D6 thÆ°á»ng trÃ¹ng D3; D4 thÆ°á»ng khÃ´ng cÃ³.
- TÃ­nh toÃ¡n: D3 = dá»¯ liá»‡u + tham sá»‘ tÃ­nh; D4 = káº¿t quáº£ tÃ­nh; D2, D6 thÆ°á»ng gá»“m D3 vÃ  D4.
- BÃ¡o biá»ƒu: D1 thÆ°á»ng cÃ³ yáº¿u tá»‘ thá»i gian; D2 = bÃ¡o biá»ƒu; D6 thÆ°á»ng giá»‘ng D2.
Má»—i DFD kÃ¨m: báº£ng "Ã nghÄ©a tá»«ng dÃ²ng dá»¯ liá»‡u" (D1..D6, dÃ²ng nÃ o khÃ´ng cÃ³ ghi "khÃ´ng cÃ³") vÃ  "Thuáº­t toÃ¡n xá»­ lÃ½" (BÆ°á»›c 1..n).
Xuáº¥t Mermaid flowchart LR (báº¡n cÃ³ thá»ƒ váº½ láº¡i báº±ng draw.io).
```

## 5. Skill prompts 05â€“08

### SKILL-05 Â· Class Diagram (dÃ¹ng á»Ÿ S4)

```text
[SKILL-05: CLASS DIAGRAM THEO QUY CHUáº¨N BÃ€I GIáº¢NG]
KÃ½ hiá»‡u lá»›p: 3 ngÄƒn tÃªn / thuá»™c tÃ­nh / phÆ°Æ¡ng thá»©c. Táº§m vá»±c: - private, # protected, + public. Gáº¡ch dÆ°á»›i = static; in nghiÃªng = abstract.
Quan há»‡ (chá»n theo ngá»¯ nghÄ©a, pháº£i cÃ³ cÄƒn cá»© trong FACTS R..):
- Association: liÃªn káº¿t, khÃ´ng sá»Ÿ há»¯u.
- Aggregation (thoi rá»—ng): toÃ n thá»ƒ - bá»™ pháº­n, bá»™ pháº­n tá»“n táº¡i Ä‘á»™c láº­p.
- Composition (thoi Ä‘áº·c): bá»™ pháº­n bá»‹ há»§y theo toÃ n thá»ƒ (vÃ­ dá»¥ chá»©ng tá»« - dÃ²ng chá»©ng tá»« náº¿u code cÃ³ cascade/orphanRemoval).
- Generalization (tam giÃ¡c rá»—ng), Dependency (nÃ©t Ä‘á»©t), Realization (nÃ©t Ä‘á»©t + tam giÃ¡c rá»—ng).
CÃ¡c bÆ°á»›c (bÃ i giáº£ng): (1) xÃ¡c Ä‘á»‹nh lá»›p + thuá»™c tÃ­nh + phÆ°Æ¡ng thá»©c; (2) xÃ¡c Ä‘á»‹nh quan há»‡; (3) tÃ¡ch lá»›p phá»¥ cho thuá»™c tÃ­nh phá»©c táº¡p; (4) tá»•ng quÃ¡t hÃ³a lá»›p cÃ³ Ä‘áº·c Ä‘iá»ƒm chung; (5) tÃ¡ch lá»›p con theo thuá»™c tÃ­nh phÃ¢n loáº¡i; (6) hiá»‡u chá»‰nh quan há»‡; (7) kiá»ƒm tra, bá»• sung phÆ°Æ¡ng thá»©c, láº­p báº£n Ä‘áº·c táº£.
Pháº¡m vi: lá»›p miá»n (domain) tá»« FACTS E..; khÃ´ng Ä‘Æ°a Controller, Repository, DTO. PhÆ°Æ¡ng thá»©c chá»‰ láº¥y tá»« FACTS M.. (vÃ­ dá»¥ confirm(), consume()). Enum váº½ vá»›i <<enumeration>>.
Báº£n sá»‘ ghi á»Ÿ hai Ä‘áº§u quan há»‡ (1, 0..1, 1..*, 0..*).
Káº¿t quáº£ báº¯t buá»™c (theo slide Káº¾T QUáº¢):
(a) SÆ¡ Ä‘á»“ lá»›p (Mermaid classDiagram; náº¿u quÃ¡ 20 lá»›p thÃ¬ thÃªm 1 sÆ¡ Ä‘á»“ tá»•ng quÃ¡t chá»‰ tÃªn lá»›p + cÃ¡c sÆ¡ Ä‘á»“ chi tiáº¿t theo package).
(b) Danh sÃ¡ch lá»›p: STT | TÃªn lá»›p/quan há»‡ | Loáº¡i | Ã nghÄ©a/ghi chÃº.
(c) Báº£ng thuá»™c tÃ­nh tá»«ng lá»›p: STT | TÃªn thuá»™c tÃ­nh | Kiá»ƒu | RÃ ng buá»™c | Ã nghÄ©a/ghi chÃº.
(d) Báº£ng quan há»‡: STT | TÃªn quan há»‡ | Kiá»ƒu | RÃ ng buá»™c | Ã nghÄ©a/ghi chÃº.
```

### SKILL-06 Â· Sequence Diagram (dÃ¹ng á»Ÿ S5)

```text
[SKILL-06: SEQUENCE DIAGRAM THEO QUY CHUáº¨N BÃ€I GIáº¢NG]
CÃ¡c bÆ°á»›c (bÃ i giáº£ng): (1) xÃ¡c Ä‘á»‹nh chá»©c nÄƒng tá»« Use Case; (2) xÃ¡c Ä‘á»‹nh cÃ¡c bÆ°á»›c tá»« Activity; (3) Ä‘á»‘i chiáº¿u Class Diagram Ä‘á»ƒ chá»n lá»›p tham gia; (4) váº½ Sequence; (5) cáº­p nháº­t láº¡i Class Diagram.
ThÃ nh pháº§n: Ä‘á»‘i tÆ°á»£ng "tÃªn : Lá»›p", lifeline, activation, thÃ´ng Ä‘iá»‡p.
Loáº¡i thÃ´ng Ä‘iá»‡p: Ä‘á»“ng bá»™ (mÅ©i tÃªn Ä‘áº·c), khÃ´ng Ä‘á»“ng bá»™ (mÅ©i tÃªn há»Ÿ), tá»± gá»i (self), tráº£ vá» (nÃ©t Ä‘á»©t), <<create>>, <<destroy>>.
RÃ ng buá»™c chá»‘ng áº£o giÃ¡c:
- Lifeline chá»‰ Ä‘Æ°á»£c lÃ  actor tá»« UC, lá»›p trong Class Diagram 3.4, hoáº·c Controller/Service/Facade/Repository cÃ³ tÃªn trong FACTS.
- Má»—i thÃ´ng Ä‘iá»‡p gá»i hÃ m pháº£i lÃ  phÆ°Æ¡ng thá»©c cÃ³ tháº­t [FACTS M..]; khÃ´ng cÃ³ thÃ¬ ghi [Cáº¦N XÃC NHáº¬N].
- DÃ¹ng alt cho nhÃ¡nh lá»—i/rollback, loop cho tá»«ng dÃ²ng chá»©ng tá»«, opt cho bÆ°á»›c tÃ¹y chá»n; ghi chÃº pháº¡m vi @Transactional báº±ng note hoáº·c group.
- Thá»© tá»± thÃ´ng Ä‘iá»‡p pháº£i khá»›p thá»© tá»± dÃ²ng lá»‡nh trong phÆ°Æ¡ng thá»©c nguá»“n.
Xuáº¥t Mermaid sequenceDiagram (autonumber). Cuá»‘i sprint liá»‡t kÃª: phÆ°Æ¡ng thá»©c/lá»›p má»›i phÃ¡t hiá»‡n cáº§n cáº­p nháº­t vÃ o 3.4.
```

### SKILL-07 Â· ERD kÃ½ hiá»‡u Chen (dÃ¹ng á»Ÿ S6)

```text
[SKILL-07: ERD THEO QUY CHUáº¨N BÃ€I GIáº¢NG]
KÃ½ hiá»‡u: thá»±c thá»ƒ = hÃ¬nh chá»¯ nháº­t, tÃªn danh tá»«; thuá»™c tÃ­nh = vÃ²ng trÃ²n rá»—ng; khÃ³a = vÃ²ng trÃ²n Ä‘áº·c; má»‘i káº¿t há»£p = hÃ¬nh thoi, tÃªn Ä‘á»™ng tá»«; báº£n sá»‘ (min,max) ghi trÃªn tá»«ng nhÃ¡nh.
Thuá»™c tÃ­nh: Ä‘Æ¡n trá»‹, tá»•ng há»£p, Ä‘a trá»‹ {..}. Má»Ÿ rá»™ng: thá»±c thá»ƒ yáº¿u, má»‘i káº¿t há»£p Ä‘á»‡ quy, cáº¥u trÃºc phÃ¢n cáº¥p (t/p, e/o), táº­p con.
CÃ¡c bÆ°á»›c xÃ¢y dá»±ng: B1 phÃ¢n hoáº¡ch dá»¯ liá»‡u theo phÃ¢n há»‡ xá»­ lÃ½ (dÃ¹ng module: identity, branch, catalog, crm, order, inventory); B2 ER cho tá»«ng phÃ¢n há»‡; B3 tá»•ng há»£p thÃ nh ER tá»•ng quÃ¡t (xÃ³a tá»« Ä‘á»“ng nghÄ©a/Ä‘a nghÄ©a); B4 chuáº©n hÃ³a; B5 kiá»ƒm tra láº§n cuá»‘i.
Quy táº¯c mÃ´ hÃ¬nh hÃ³a (kiá»ƒm tra tá»«ng quy táº¯c, ghi káº¿t quáº£):
QT1 má»—i thuá»™c tÃ­nh chá»‰ mÃ´ táº£ má»™t thá»±c thá»ƒ -> KHÃ”NG váº½ cá»™t khÃ³a ngoáº¡i (customer_id, branch_id...) thÃ nh thuá»™c tÃ­nh; thá»ƒ hiá»‡n báº±ng má»‘i káº¿t há»£p.
QT2 Ä‘áº·c trÆ°ng phá»¥ thuá»™c nhiá»u thá»±c thá»ƒ -> thuá»™c tÃ­nh cá»§a má»‘i káº¿t há»£p.
QT3 cÃ¡c nhÃ¡nh cá»§a má»‘i káº¿t há»£p pháº£i báº¯t buá»™c, náº¿u khÃ´ng thÃ¬ tÃ¡ch thÃ nh nhiá»u má»‘i káº¿t há»£p.
QT4 Ä‘áº·c trÆ°ng phá»¥ thuá»™c má»™t thuá»™c tÃ­nh -> tÃ¡ch thá»±c thá»ƒ áº©n.
Má»‘i káº¿t há»£p hay thá»±c thá»ƒ: dÃ²ng chá»©ng tá»« cÃ³ Ä‘á»‹nh danh riÃªng trong code -> thá»±c thá»ƒ; khÃ´ng cÃ³ -> má»‘i káº¿t há»£p cÃ³ thuá»™c tÃ­nh.
Dá»¯ liá»‡u chá»‰ láº¥y tá»« FACTS + file Flyway migration; báº£n sá»‘ suy tá»« nullable/optional trong code.
Xuáº¥t Mermaid erDiagram (hoáº·c mÃ´ táº£ dáº¡ng báº£ng Ä‘á»ƒ váº½ draw.io náº¿u quÃ¡ lá»›n), kÃ¨m tá»« Ä‘iá»ƒn dá»¯ liá»‡u.
```

### SKILL-08 Â· Kiá»ƒm tra chÃ©o (dÃ¹ng á»Ÿ S5, S8)

```text
[SKILL-08: KIá»‚M TRA CHÃ‰O]
KhÃ´ng sÃ¡ng tÃ¡c thÃªm. Chá»‰ Ä‘á»‘i chiáº¿u vÃ  bÃ¡o lá»—i.
1. Má»i tÃªn lá»›p, thuá»™c tÃ­nh, phÆ°Æ¡ng thá»©c, báº£ng trong káº¿t quáº£ pháº£i tá»“n táº¡i trong FACTS (so khá»›p chÃ­nh táº£, hoa thÆ°á»ng).
2. Báº£n sá»‘ vÃ  loáº¡i quan há»‡ khá»›p vá»›i annotation trong FACTS R...
3. CÃ¹ng má»™t khÃ¡i niá»‡m pháº£i cÃ¹ng tÃªn á»Ÿ UC, Activity, DFD, Class, Sequence, ERD.
4. Ma tráº­n phá»§: UC | Activity | DFD | Sequence | Lá»›p liÃªn quan. UC nghiá»‡p vá»¥ chÃ­nh thiáº¿u hÃ¬nh nÃ o thÃ¬ bÃ¡o.
5. Kiá»ƒm tra kÃ½ hiá»‡u theo SKILL-02..07 (Ä‘áº·t tÃªn, guard, báº£n sá»‘, sá»‘ UC má»—i sÆ¡ Ä‘á»“...).
Xuáº¥t báº£ng: Lá»—i | Vá»‹ trÃ­ (má»¥c, hÃ¬nh) | Má»©c Ä‘á»™ (NghiÃªm trá»ng/Nháº¹) | Sá»­a Ä‘á» xuáº¥t | Nguá»“n Ä‘á»‘i chiáº¿u.
```

## 6. Káº¿ hoáº¡ch sprint

S0 tÃ¡ch lÃ m 2 phiÃªn (S0a, S0b) vÃ¬ Ä‘á»c mÃ£ nguá»“n lÃ  bÆ°á»›c tá»‘n context nháº¥t. Má»—i dÃ²ng dÆ°á»›i Ä‘Ã¢y lÃ  má»™t cuá»™c trÃ² chuyá»‡n má»›i.

| Sprint | Má»¥c tiÃªu | ÄÃ­nh kÃ¨m (chá»‰ nhá»¯ng file nÃ y) | Skill | Äáº§u ra | Cá»•ng duyá»‡t cá»§a báº¡n |
| --- | --- | --- | --- | --- | --- |
| S0a | FACTS pháº§n A | `domain/` cá»§a identity, branch, catalog, crm; Flyway migration liÃªn quan; `setup-replication.sh`; BC 2.1.1, 2.1.7, 2.5, 2.6 | 01 | `FACTS.md` pháº§n A | Sá»‘ entity = sá»‘ file @Entity trong 4 module |
| S0b | FACTS pháº§n B | `domain/` + `application/` cá»§a order, inventory; `common/` (idempotency); BC 2.1.2â€“2.1.6, 4.5 | 01 | `FACTS.md` pháº§n B | Sales, Return, Inbound, FIFO, Debt Ä‘á»u cÃ³ M.. |
| S1 | 3.1 Use Case + Ä‘áº·c táº£ | FACTS.md; BC 1.4, 2.1, 3.1; hÃ¬nh UC hiá»‡n cÃ³ | 02 | `out/S1_usecase.md` | Danh sÃ¡ch UC chá»‘t vÃ  mÃ£ UC |
| S2 | 3.2 Activity | FACTS.md; HANDOFF; BC 2.1.2â€“2.1.4; hÃ¬nh Activity hiá»‡n cÃ³ | 03 | `out/S2_activity.md` | 3 sÆ¡ Ä‘á»“ render Ä‘Æ°á»£c |
| S3 | 3.3 DFD | FACTS.md; HANDOFF; hÃ¬nh DFD hiá»‡n cÃ³ | 04 | `out/S3_dfd.md` | D1â€“D6 má»—i hÃ¬nh Ä‘á»§ nghÄ©a |
| S4 | 3.4 Class | FACTS.md; HANDOFF | 05 | `out/S4_class.md` | Má»i lá»›p cÃ³ E.., má»i quan há»‡ cÃ³ R.. |
| S5 | 3.5 Sequence | FACTS.md; HANDOFF; `out/S4_class.md`; service Goods Return, Inbound Receipt; 4 hÃ¬nh Sequence cÅ© | 06, 08 | `out/S5_sequence.md` + danh sÃ¡ch cáº­p nháº­t lá»›p | ThÃ´ng Ä‘iá»‡p = phÆ°Æ¡ng thá»©c tháº­t |
| S6 | 3.6 ERD | FACTS.md; HANDOFF; `out/S4_class.md` (báº£n Ä‘Ã£ cáº­p nháº­t); Flyway migration; `setup-replication.sh` | 07 | `out/S6_erd.md` | QT1â€“QT4 Ä‘áº¡t |
| S7 | Láº¯p rÃ¡p ChÆ°Æ¡ng 3 | `out/S1`â€“`S6`; HANDOFF | â€” | `CH3_final.md` + danh sÃ¡ch hÃ¬nh | ÄÃ¡nh sá»‘ liÃªn tá»¥c |
| S8 | QA toÃ n chÆ°Æ¡ng | `CH3_final.md`; FACTS.md | 08 | Báº£ng lá»—i | 0 lá»—i nghiÃªm trá»ng, 0 \[Cáº¦N XÃC NHáº¬N\] |

**CÃ¡ch ghÃ©p prompt má»—i phiÃªn:** Prompt há»‡ thá»‘ng (náº¿u chÆ°a cÃ i sáºµn) + Skill tÆ°Æ¡ng á»©ng + ná»™i dung HANDOFF.md + prompt sprint bÃªn dÆ°á»›i.

### Sprint 0a â€” FACTS pháº§n A

```text
[SPRINT 0a â€“ FACTS PHáº¦N A]
Ãp dá»¥ng SKILL-01 cho 4 module: identity, branch, catalog, crm.
File Ä‘Ã­nh kÃ¨m: toÃ n bá»™ lá»›p trong <module>/domain/ cá»§a 4 module, file Flyway migration táº¡o cÃ¡c báº£ng cá»§a 4 module, setup-replication.sh, bÃ¡o cÃ¡o má»¥c 2.1.1, 2.1.7, 2.5, 2.6.
Nhiá»‡m vá»¥ thÃªm:
- Tá»« setup-replication.sh, láº­p báº£ng Sá»ž Há»®U Dá»® LIá»†U (O01..): báº£ng | thuá»™c publication nÃ o | chiá»u Ä‘á»“ng bá»™ (HQ->Branch hoáº·c Branch->HQ).
- Ghi rÃµ cá»™t branch_id cá»§a customer cÃ³ nullable hay khÃ´ng.
- Liá»‡t kÃª cÃ¡c claim JWT vÃ  vai trÃ² tá»« code identity.
Báº¯t Ä‘áº§u ID tá»« E01, R01, N01, M01, P01, B01, A01, O01.
Checklist:
[ ] Sá»‘ entity liá»‡t kÃª báº±ng sá»‘ file @Entity Ä‘Ã­nh kÃ¨m
[ ] Má»i trÆ°á»ng cÃ³ kiá»ƒu dá»¯ liá»‡u
[ ] Má»i fact cÃ³ nguá»“n [file:dÃ²ng]
[ ] KhÃ´ng cÃ³ cÃ¢u phÃ¢n tÃ­ch hay Ä‘á» xuáº¥t thiáº¿t káº¿
HANDOFF: ghi ID cuá»‘i cÃ¹ng má»—i nhÃ³m Ä‘á»ƒ S0b ná»‘i tiáº¿p.
```

### Sprint 0b â€” FACTS pháº§n B

```text
[SPRINT 0b â€“ FACTS PHáº¦N B]
Ãp dá»¥ng SKILL-01 cho: order, inventory, common (idempotency).
File Ä‘Ã­nh kÃ¨m: domain/ vÃ  application/ cá»§a order, inventory; lá»›p IdempotencyRecord, aspect @IdempotencyProtected; Flyway migration liÃªn quan; bÃ¡o cÃ¡o má»¥c 2.1.2-2.1.6 vÃ  4.5.
Bá»‘i cáº£nh: <dÃ¡n HANDOFF cá»§a S0a â€“ ID cuá»‘i má»—i nhÃ³m>. ÄÃ¡nh ID ná»‘i tiáº¿p, khÃ´ng Ä‘Ã¡nh láº¡i tá»« Ä‘áº§u.
Trá»ng tÃ¢m pháº£i Ä‘á»§:
- SalesInvoice + dÃ²ng; GoodsReturn + dÃ²ng; InboundReceipt + dÃ²ng; StockMovement; StockOnHand; CostLayer; ReceivableDebt; ReceivableDebtMovement; giÃ¡ riÃªng theo khÃ¡ch hÃ ng (tÃªn lá»›p Ä‘Ãºng theo code).
- PhÆ°Æ¡ng thá»©c: createAndConfirm, applyConfirmationEffects, confirm cá»§a Goods Return vÃ  Inbound Receipt, recordSaleAndGetCost, FifoCostService.consume, CostLayer.fromInbound / fromReturn / consume.
- Vá»›i má»—i phÆ°Æ¡ng thá»©c confirm: liá»‡t kÃª THá»¨ Tá»° cÃ¡c lá»i gá»i bÃªn trong (dÃ²ng code), vÃ¬ S2 vÃ  S5 sáº½ dá»±ng Activity/Sequence tá»« Ä‘Ã¢y.
Checklist:
[ ] CÃ³ Ä‘á»§ 3 quy trÃ¬nh xÃ¡c nháº­n: bÃ¡n hÃ ng, tráº£ hÃ ng, nháº­p kho
[ ] Má»—i quy trÃ¬nh cÃ³ chuá»—i lá»i gá»i theo thá»© tá»± kÃ¨m [file:dÃ²ng]
[ ] Ghi Ä‘Æ°á»£c lock (PESSIMISTIC_WRITE, @Version) vÃ  @Retryable náº¿u cÃ³
```

Sau S0b: báº¡n gá»™p A + B thÃ nh má»™t `FACTS.md`, tá»± má»Ÿ vÃ i file code Ä‘á»‘i chiáº¿u ngáº«u nhiÃªn 5 fact. Sai 1 fact thÃ¬ cháº¡y láº¡i sprint Ä‘Ã³.

### Sprint 1 â€” Use Case

```text
[SPRINT 1 â€“ Má»¤C 3.1 USE CASE]
Ãp dá»¥ng SKILL-02. File Ä‘Ã­nh kÃ¨m: FACTS.md, bÃ¡o cÃ¡o má»¥c 1.4, 2.1, 3.1, hÃ¬nh Use Case hiá»‡n cÃ³ (mÃ£ hoáº·c áº£nh).
Nhiá»‡m vá»¥:
1. ChÃ©p láº¡i danh sÃ¡ch actor vÃ  UC trong hÃ¬nh hiá»‡n cÃ³.
2. Äá»‘i chiáº¿u quy chuáº©n SKILL-02, liá»‡t kÃª vi pháº¡m (tÃªn, má»©c Ä‘á»™, sá»‘ lÆ°á»£ng, thiáº¿u mÃ£ UC, CRUD tÃ¡ch rá»i).
3. Láº­p danh sÃ¡ch UC chá»‘t: MÃ£ | TÃªn | Actor | Module | Nguá»“n. Chá»‰ giá»¯ UC cÃ³ trong FACTS hoáº·c pháº¡m vi BC 1.4.
   Danh sÃ¡ch gá»£i Ã½ cáº§n kiá»ƒm chá»©ng (loáº¡i bá» náº¿u khÃ´ng cÃ³ nguá»“n): ÄÄƒng nháº­p; Quáº£n lÃ½ tÃ i khoáº£n vÃ  phÃ¢n quyá»n; Quáº£n lÃ½ chi nhÃ¡nh; Quáº£n lÃ½ sáº£n pháº©m vÃ  danh má»¥c; Quáº£n lÃ½ nhÃ  cung cáº¥p; Quáº£n lÃ½ báº£ng giÃ¡; Quáº£n lÃ½ khÃ¡ch hÃ ng; Láº­p hÃ³a Ä‘Æ¡n bÃ¡n hÃ ng; Láº­p phiáº¿u tráº£ hÃ ng; Láº­p phiáº¿u nháº­p kho; Tra cá»©u tá»“n kho; Theo dÃµi cÃ´ng ná»£; Tra cá»©u giÃ¡ riÃªng theo khÃ¡ch hÃ ng.
4. Náº¿u hÆ¡n 10 UC: 1 sÆ¡ Ä‘á»“ tá»•ng quÃ¡t (nhÃ³m theo module) + sÆ¡ Ä‘á»“ con cho Admin vÃ  Staff.
5. Viáº¿t Ä‘áº·c táº£ cho cÃ¡c UC nghiá»‡p vá»¥ chÃ­nh (tá»‘i thiá»ƒu: ÄÄƒng nháº­p, Láº­p hÃ³a Ä‘Æ¡n bÃ¡n hÃ ng, Láº­p phiáº¿u tráº£ hÃ ng, Láº­p phiáº¿u nháº­p kho, Quáº£n lÃ½ khÃ¡ch hÃ ng).
6. Viáº¿t Ä‘oáº¡n vÄƒn cho má»¥c 3.1.1 vÃ  3.1.2.
Checklist:
[ ] TÃªn UC Ä‘á»™ng tá»« + danh tá»«, cÃ³ mÃ£ UC trÃªn hÃ¬nh
[ ] KhÃ´ng UC nÃ o thiáº¿u nguá»“n
[ ] include/extend Ä‘Ãºng chiá»u mÅ©i tÃªn
[ ] Má»—i Ä‘áº·c táº£ Ä‘á»§ 9 trÆ°á»ng
HANDOFF báº¯t buá»™c chá»©a báº£ng MÃ£ UC | TÃªn UC Ä‘Ã£ chá»‘t.
```

### Sprint 2 â€” Activity

```text
[SPRINT 2 â€“ Má»¤C 3.2 ACTIVITY]
Ãp dá»¥ng SKILL-03. File Ä‘Ã­nh kÃ¨m: FACTS.md, bÃ¡o cÃ¡o má»¥c 2.1.2-2.1.4, hÃ¬nh Activity bÃ¡n hÃ ng hiá»‡n cÃ³.
Bá»‘i cáº£nh: <dÃ¡n HANDOFF.md>.
Nhiá»‡m vá»¥:
1. 3.2.1 BÃ¡n hÃ ng: chÃ©p láº¡i hÃ¬nh hiá»‡n cÃ³, Ä‘á»‘i chiáº¿u vá»›i chuá»—i lá»i gá»i createAndConfirm trong FACTS, chá»‰ ra bÆ°á»›c thiáº¿u/sai thá»© tá»±/vi pháº¡m kÃ½ hiá»‡u. Chá»‰ váº½ láº¡i náº¿u cÃ³ lá»—i.
2. 3.2.2 Tráº£ hÃ ng (Goods Return): váº½ má»›i theo mÃ´ hÃ¬nh Draft -> Confirm, bÃ¡m chuá»—i lá»i gá»i trong FACTS.
3. 3.2.3 Nháº­p kho (Inbound Receipt): váº½ má»›i theo Draft -> Confirm, thá»ƒ hiá»‡n táº¡o Stock Movement vÃ  Cost Layer má»›i.
4. Má»—i sÆ¡ Ä‘á»“ kÃ¨m cÃ¢u dáº«n, chÃº thÃ­ch hÃ¬nh, 1-2 Ä‘oáº¡n giáº£i thÃ­ch, ghi rÃµ thuá»™c UC nÃ o (mÃ£ UC).
Checklist:
[ ] Má»—i hoáº¡t Ä‘á»™ng 1 vÃ o 1 ra, tÃªn Ä‘á»™ng tá»« + danh tá»«, khÃ´ng trÃ¹ng
[ ] Má»i nhÃ¡nh decision cÃ³ guard
[ ] Má»i hoáº¡t Ä‘á»™ng lane Há»‡ thá»‘ng cÃ³ M.. hoáº·c B..
[ ] Tráº¡ng thÃ¡i chá»©ng tá»« dÃ¹ng Ä‘Ãºng giÃ¡ trá»‹ enum trong FACTS
```

### Sprint 3 â€” DFD

```text
[SPRINT 3 â€“ Má»¤C 3.3 DFD]
Ãp dá»¥ng SKILL-04. File Ä‘Ã­nh kÃ¨m: FACTS.md, hÃ¬nh DFD hiá»‡n cÃ³.
Bá»‘i cáº£nh: <dÃ¡n HANDOFF.md>.
Nhiá»‡m vá»¥:
1. ChÃ©p láº¡i pháº§n tá»­ cá»§a hÃ¬nh DFD hiá»‡n cÃ³ vÃ  xÃ¡c Ä‘á»‹nh nÃ³ lÃ  cáº¥p máº¥y. Náº¿u lÃ  má»©c toÃ n há»‡ thá»‘ng -> Ä‘áº·t tÃªn "DFD cáº¥p 0" vÃ  sá»­a kÃ½ hiá»‡u náº¿u sai.
2. 3.3.2: láº­p DFD theo máº«u D1-D6 cho tá»«ng yÃªu cáº§u chÃ­nh, ghi rÃµ loáº¡i yÃªu cáº§u:
   - Láº­p hÃ³a Ä‘Æ¡n bÃ¡n hÃ ng (lÆ°u trá»¯ + tÃ­nh toÃ¡n giÃ¡ vá»‘n, cÃ´ng ná»£)
   - Láº­p phiáº¿u tráº£ hÃ ng (lÆ°u trá»¯)
   - Láº­p phiáº¿u nháº­p kho (lÆ°u trá»¯)
   - Tra cá»©u giÃ¡ riÃªng theo khÃ¡ch hÃ ng (tra cá»©u)
   - Theo dÃµi cÃ´ng ná»£ (tra cá»©u) â€“ chá»‰ lÃ m náº¿u FACTS cÃ³ chá»©c nÄƒng tÆ°Æ¡ng á»©ng
3. Má»—i DFD: sÆ¡ Ä‘á»“ + báº£ng Ã½ nghÄ©a D1-D6 + thuáº­t toÃ¡n xá»­ lÃ½ tá»«ng bÆ°á»›c (trá» vá» M.. / B..).
Checklist:
[ ] Bá»™ nhá»› phá»¥ ghi Ä‘Ãºng tÃªn báº£ng cÃ³ trong FACTS
[ ] D4 cá»§a yÃªu cáº§u lÆ°u trá»¯ nháº¥t quÃ¡n vá»›i D1
[ ] TÃªn xá»­ lÃ½ trÃ¹ng tÃªn UC Ä‘Ã£ chá»‘t á»Ÿ S1
```

## 7. Prompt Sprint 4â€“8

### Sprint 4 â€” Class Diagram

```text
[SPRINT 4 â€“ Má»¤C 3.4 CLASS DIAGRAM]
Ãp dá»¥ng SKILL-05. File Ä‘Ã­nh kÃ¨m: FACTS.md (duy nháº¥t).
Bá»‘i cáº£nh: <dÃ¡n HANDOFF.md>.
Nhiá»‡m vá»¥:
1. Äi qua 7 bÆ°á»›c cá»§a SKILL-05, má»—i bÆ°á»›c ghi ngáº¯n quyáº¿t Ä‘á»‹nh vÃ  cÄƒn cá»© (E.., R..).
2. NhÃ³m lá»›p theo package tÆ°Æ¡ng á»©ng module (identity, branch, catalog, crm, order, inventory).
3. Quyáº¿t Ä‘á»‹nh composition hay aggregation cho tá»«ng cáº·p chá»©ng tá»« - dÃ²ng chá»©ng tá»« dá»±a trÃªn cascade/orphanRemoval trong FACTS; náº¿u FACTS khÃ´ng nÃ³i rÃµ -> [Cáº¦N XÃC NHáº¬N].
4. Quan há»‡ liÃªn module chá»‰ lÆ°u ID (tham chiáº¿u logic qua Facade): váº½ association nÃ©t liá»n kÃ¨m note "tham chiáº¿u qua ID", khÃ´ng tá»± biáº¿n thÃ nh composition.
5. Xuáº¥t (a) sÆ¡ Ä‘á»“, (b) danh sÃ¡ch lá»›p, (c) báº£ng thuá»™c tÃ­nh tá»«ng lá»›p, (d) báº£ng quan há»‡, (e) Ä‘oáº¡n vÄƒn má»Ÿ Ä‘áº§u má»¥c 3.4.
Checklist:
[ ] Má»i lá»›p miá»n trong FACTS cÃ³ máº·t, hoáº·c cÃ³ lÃ½ do loáº¡i trá»«
[ ] Má»i quan há»‡ cÃ³ R.. vÃ  báº£n sá»‘ hai Ä‘áº§u
[ ] KhÃ´ng cÃ³ Controller/Repository/DTO
[ ] PhÆ°Æ¡ng thá»©c chá»‰ láº¥y tá»« M..
HANDOFF: danh sÃ¡ch tÃªn lá»›p chá»‘t (Ä‘Ãºng chÃ­nh táº£) â€“ S5 vÃ  S6 dÃ¹ng danh sÃ¡ch nÃ y.
```

### Sprint 5 â€” Sequence Diagram

```text
[SPRINT 5 â€“ Má»¤C 3.5 SEQUENCE]
Ãp dá»¥ng SKILL-06, sau Ä‘Ã³ SKILL-08 cho pháº§n káº¿t quáº£ cá»§a sprint nÃ y.
File Ä‘Ã­nh kÃ¨m: FACTS.md, out/S4_class.md, service + controller cá»§a Goods Return vÃ  Inbound Receipt, 4 hÃ¬nh Sequence hiá»‡n cÃ³ (mÃ£ hoáº·c áº£nh).
Bá»‘i cáº£nh: <dÃ¡n HANDOFF.md>.
Nhiá»‡m vá»¥:
1. Vá»›i 4 hÃ¬nh cÅ© (ÄÄƒng nháº­p/JWT, Táº¡o vÃ  xÃ¡c nháº­n Sales Invoice, Tra cá»©u giÃ¡ riÃªng, Logical Replication): chÃ©p lifeline + thÃ´ng Ä‘iá»‡p, Ä‘á»‘i chiáº¿u Class Diagram vÃ  FACTS, chá»‰ bÃ¡o chá»— lá»‡ch vÃ  Ä‘Æ°a báº£n sá»­a cho chá»— Ä‘Ã³.
   LÆ°u Ã½: hÃ¬nh Logical Replication lÃ  luá»“ng háº¡ táº§ng, lifeline lÃ  HQ DB / Branch DB / publication / subscription theo setup-replication.sh, khÃ´ng Ã©p vÃ o lá»›p miá»n.
2. 3.5.3 Tráº£ hÃ ng â€“ Confirm Goods Return: váº½ má»›i, khá»›p Activity 3.2.2.
3. 3.5.4 Nháº­p kho â€“ Confirm Inbound Receipt: váº½ má»›i, thá»ƒ hiá»‡n táº¡o StockMovement vÃ  CostLayer.fromInbound, khá»›p Activity 3.2.3.
4. Sáº¯p xáº¿p thá»© tá»± má»¥c: 3.5.1 ÄÄƒng nháº­p, 3.5.2 Sales Invoice, 3.5.3 Tráº£ hÃ ng, 3.5.4 Nháº­p kho, 3.5.5 GiÃ¡ riÃªng, 3.5.6 Replication.
5. BÆ°á»›c 5 cá»§a bÃ i giáº£ng: xuáº¥t danh sÃ¡ch phÆ°Æ¡ng thá»©c/lá»›p cáº§n cáº­p nháº­t vÃ o Class Diagram (dáº¡ng báº£ng Lá»›p | ThÃªm/Sá»­a | Ná»™i dung | Nguá»“n). Báº¡n sáº½ sá»­a out/S4_class.md trÆ°á»›c khi sang S6.
Checklist:
[ ] Má»i lifeline cÃ³ trong Class Diagram hoáº·c FACTS
[ ] Má»i thÃ´ng Ä‘iá»‡p lÃ  phÆ°Æ¡ng thá»©c M.. hoáº·c endpoint P..
[ ] CÃ³ alt cho nhÃ¡nh lá»—i/rollback, loop cho tá»«ng dÃ²ng chá»©ng tá»«
[ ] Thá»© tá»± thÃ´ng Ä‘iá»‡p khá»›p chuá»—i lá»i gá»i trong FACTS
```

### Sprint 6 â€” ERD

```text
[SPRINT 6 â€“ Má»¤C 3.6 ERD]
Ãp dá»¥ng SKILL-07. File Ä‘Ã­nh kÃ¨m: FACTS.md, out/S4_class.md (báº£n Ä‘Ã£ cáº­p nháº­t sau S5), Flyway migration, setup-replication.sh.
Bá»‘i cáº£nh: <dÃ¡n HANDOFF.md>.
Nhiá»‡m vá»¥:
1. B1-B2: ER riÃªng cho tá»«ng phÃ¢n há»‡ (identity, branch, catalog, crm, order, inventory).
2. B3: tá»•ng há»£p ER tá»•ng quÃ¡t; liá»‡t kÃª thá»±c thá»ƒ chung giá»¯a cÃ¡c phÃ¢n há»‡ (Branch, Product, Customer...).
3. B4-B5: kiá»ƒm tra QT1-QT4, ghi káº¿t quáº£ tá»«ng quy táº¯c.
4. Thá»ƒ hiá»‡n Ä‘áº·c thÃ¹ Ä‘á» tÃ i: má»‘i káº¿t há»£p Customer - Branch cÃ³ báº£n sá»‘ (0,1) phÃ­a Customer (branch_id NULL = khÃ¡ch dÃ¹ng chung); giáº£i thÃ­ch báº±ng note.
5. Tá»« Ä‘iá»ƒn dá»¯ liá»‡u: Thá»±c thá»ƒ | Thuá»™c tÃ­nh | Kiá»ƒu | KhÃ³a | RÃ ng buá»™c | Sá»Ÿ há»¯u (HQ/Branch theo O..).
6. Äoáº¡n vÄƒn má»¥c 3.6: giáº£i thÃ­ch cÃ¡ch Ä‘i tá»« Class Diagram sang ERD vÃ  phÃ¢n quyá»n sá»Ÿ há»¯u dá»¯ liá»‡u.
Checklist:
[ ] KhÃ´ng cÃ³ khÃ³a ngoáº¡i váº½ thÃ nh thuá»™c tÃ­nh (QT1)
[ ] Má»i nhÃ¡nh cÃ³ (min,max)
[ ] Thá»±c thá»ƒ khá»›p tÃªn lá»›p S4 hoáº·c cÃ³ báº£ng Ã¡nh xáº¡ tÃªn
[ ] CÃ³ cá»™t Sá»Ÿ há»¯u cho má»i thá»±c thá»ƒ
```

### Sprint 7 â€” Láº¯p rÃ¡p ChÆ°Æ¡ng 3

```text
[SPRINT 7 â€“ Láº®P RÃP CHÆ¯Æ NG 3]
File Ä‘Ã­nh kÃ¨m: out/S1_usecase.md ... out/S6_erd.md (báº£n cuá»‘i), HANDOFF.md.
KhÃ´ng táº¡o ná»™i dung ká»¹ thuáº­t má»›i; chá»‰ sáº¯p xáº¿p, ná»‘i máº¡ch vÄƒn, Ä‘Ã¡nh sá»‘.
Nhiá»‡m vá»¥:
1. GhÃ©p theo thá»© tá»±: 3.1 Use Case (3.1.1, 3.1.2) -> 3.2 Activity (3.2.1-3.2.3) -> 3.3 DFD (3.3.1, 3.3.2) -> 3.4 Class -> 3.5 Sequence (3.5.1-3.5.6) -> 3.6 ERD.
2. Äoáº¡n má»Ÿ Ä‘áº§u ChÆ°Æ¡ng 3 (3-4 cÃ¢u) nÃªu trÃ¬nh tá»±: mÃ´ hÃ¬nh hÃ³a chá»©c nÄƒng -> cáº¥u trÃºc -> hÃ nh vi -> thiáº¿t káº¿ dá»¯ liá»‡u.
3. Má»—i má»¥c cÃ³ cÃ¢u chuyá»ƒn tiáº¿p ná»‘i sang má»¥c sau (vÃ­ dá»¥ Activity lÃ  cÄƒn cá»© Ä‘á»ƒ váº½ Sequence).
4. ÄÃ¡nh sá»‘ hÃ¬nh liÃªn tá»¥c toÃ n bÃ¡o cÃ¡o, báº¯t Ä‘áº§u HÃ¬nh 2 (HÃ¬nh 1 = Kiáº¿n trÃºc module vÃ  FaÃ§ade á»Ÿ ChÆ°Æ¡ng 2); xuáº¥t báº£ng Danh sÃ¡ch sÆ¡ Ä‘á»“ hÃ¬nh áº£nh má»›i.
5. Xuáº¥t báº£ng Ã¡nh xáº¡: HÃ¬nh cÅ© (sá»‘, tÃªn) -> HÃ¬nh má»›i (sá»‘, tÃªn, má»¥c) Ä‘á»ƒ báº¡n thay trong Word.
Checklist:
[ ] KhÃ´ng nháº£y sá»‘ má»¥c, khÃ´ng nháº£y sá»‘ hÃ¬nh
[ ] Má»i hÃ¬nh cÃ³ cÃ¢u dáº«n trÆ°á»›c vÃ  giáº£i thÃ­ch sau
[ ] KhÃ´ng cÃ²n [Cáº¦N XÃC NHáº¬N] chÆ°a xá»­ lÃ½ (náº¿u cÃ²n, liá»‡t kÃª ra, khÃ´ng xÃ³a)
```

### Sprint 8 â€” QA toÃ n chÆ°Æ¡ng

```text
[SPRINT 8 â€“ QA]
Ãp dá»¥ng SKILL-08 cho toÃ n bá»™ CH3_final.md. File Ä‘Ã­nh kÃ¨m: CH3_final.md, FACTS.md.
Nhiá»‡m vá»¥:
1. Cháº¡y 5 má»¥c kiá»ƒm tra cá»§a SKILL-08.
2. Láº­p ma tráº­n phá»§ UC x hÃ¬nh cho má»i UC nghiá»‡p vá»¥ chÃ­nh.
3. Kiá»ƒm tra thá»© tá»± má»¥c Ä‘Ãºng: Use Case -> Activity -> DFD -> Class -> Sequence -> ERD.
4. Chá»‰ xuáº¥t báº£ng lá»—i vÃ  Ä‘á» xuáº¥t sá»­a; khÃ´ng viáº¿t láº¡i chÆ°Æ¡ng.
Káº¿t luáº­n cuá»‘i: "Sáº´N SÃ€NG Ná»˜P" chá»‰ khi 0 lá»—i nghiÃªm trá»ng vÃ  0 [Cáº¦N XÃC NHáº¬N]; ngÆ°á»£c láº¡i ghi "Cáº¦N Sá»¬A" kÃ¨m sá»‘ lá»—i.
```

## 8. Checklist nghiá»‡m thu trÆ°á»›c khi ná»™p

- [ ] Thá»© tá»± ChÆ°Æ¡ng 3 Ä‘Ãºng: Use Case â†’ Activity â†’ DFD â†’ Class â†’ Sequence â†’ ERD
- [ ] Class Diagram (3.4) cÃ³ hÃ¬nh + danh sÃ¡ch lá»›p + báº£ng thuá»™c tÃ­nh + báº£ng quan há»‡
- [ ] ERD (3.6) cÃ³ hÃ¬nh kÃ½ hiá»‡u Chen + tá»« Ä‘iá»ƒn dá»¯ liá»‡u, thá»ƒ hiá»‡n sá»Ÿ há»¯u HQ/Branch
- [ ] CÃ³ Ä‘áº·c táº£ Use Case cho cÃ¡c UC nghiá»‡p vá»¥ chÃ­nh
- [ ] Tráº£ hÃ ng vÃ  Nháº­p kho Ä‘á»u cÃ³ Activity + Sequence
- [ ] DFD ghi rÃµ cáº¥p; cÃ³ DFD theo máº«u D1â€“D6 kÃ¨m thuáº­t toÃ¡n xá»­ lÃ½
- [ ] CÃ¹ng má»™t khÃ¡i niá»‡m cÃ¹ng tÃªn á»Ÿ má»i hÃ¬nh (Ä‘Ã£ qua S8)
- [ ] KhÃ´ng cÃ²n `[Cáº¦N XÃC NHáº¬N]` trong báº£n cuá»‘i
- [ ] Má»¥c Ä‘Ã¡nh sá»‘ 3.1â€“3.6 liÃªn tá»¥c, danh sÃ¡ch hÃ¬nh cáº­p nháº­t theo báº£ng Ã¡nh xáº¡ cá»§a S7
- [ ] Báº¡n tá»± render láº¡i toÃ n bá»™ mÃ£ PlantUML/Mermaid vÃ  xem báº±ng máº¯t tá»«ng hÃ¬nh trÆ°á»›c khi chÃ¨n vÃ o Word

Náº¿u Gemini tráº£ vá» tÃªn lá»›p hoáº·c phÆ°Æ¡ng thá»©c báº¡n khÃ´ng nháº­n ra, hÃ£y tÃ¬m nÃ³ trong code trÆ°á»›c khi cháº¥p nháº­n â€” Ä‘Ã¢y lÃ  dáº¥u hiá»‡u áº£o giÃ¡c phá»• biáº¿n nháº¥t.

