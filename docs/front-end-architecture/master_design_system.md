# ERP Master Design System: "Data-Dense Swiss & Soft UI"

This document serves as the single source of truth for the Enterprise ERP redesign. The goal is to maximize data density and readability for power users (8+ hours/day) while minimizing cognitive load through structured Minimalism (Swiss Style) and approachable depth (Soft UI Evolution).

## 1. Design Philosophy
- **Clarity over Decoration:** Every visual element must serve a functional purpose. Remove unnecessary lines, backgrounds, and colors.
- **Data-Density without Clutter:** Use spacing, typography, and subtle contrast to group information, allowing more data on screen without feeling overwhelming.
- **High Readability:** Strong contrast ratios and highly legible typography to prevent eye strain.
- **Predictability:** Consistent interaction patterns across all modules (Sales, Purchasing, Inventory, Accounting).

## 2. Color Palette (Tailwind CSS)

### Foundation (Surfaces & Borders)
We use a cool, neutral slate to reduce eye strain compared to stark blacks and grays.
- **Background App:** `bg-slate-50` (Soft off-white to separate panels).
- **Background Surface (Cards/Modals):** `bg-white` (Elevated elements).
- **Borders (Dividers/Tables):** `border-slate-200` (Subtle structure).
- **Text Primary:** `text-slate-900` (Main data, headings).
- **Text Secondary:** `text-slate-500` (Labels, metadata, helper text).
- **Text Disabled:** `text-slate-400`.

### Brand & Interactive (Swiss Blue)
A highly visible, trustworthy blue for primary actions.
- **Primary Action (Buttons):** `bg-blue-600` (Hover: `bg-blue-700`).
- **Interactive Links:** `text-blue-600`.
- **Focus Rings:** `ring-blue-500/50`.

### Semantic / Status
Used strictly for status indicators and feedback, never for decoration.
- **Success:** `text-emerald-700` / `bg-emerald-50` / `border-emerald-200`
- **Warning:** `text-amber-700` / `bg-amber-50` / `border-amber-200`
- **Error:** `text-rose-700` / `bg-rose-50` / `border-rose-200`
- **Info:** `text-sky-700` / `bg-sky-50` / `border-sky-200`

## 3. Typography (Swiss Style)

**Font Family:** `Inter` (or `system-ui`) - chosen for exceptional legibility at small sizes and clear tabular numbers.

### Font Hierarchy (Tailwind)
Enterprise ERPs require a smaller base font to maximize screen real estate.
- **Base/Data (Body):** `text-sm` (14px), `font-normal`, `leading-5`. Used for table cells and standard text.
- **Metadata/Labels (Body Small):** `text-xs` (12px), `font-medium`, `text-slate-500`, uppercase tracking for section headers (`tracking-wider`, `uppercase`).
- **Headings:**
  - **H1 (Page Title):** `text-2xl` (24px), `font-semibold`, `tracking-tight`, `text-slate-900`.
  - **H2 (Section/Card Title):** `text-lg` (18px), `font-semibold`.
  - **H3 (Subsection):** `text-base` (16px), `font-medium`.

*Crucial Detail:* Always use `tabular-nums` for numbers in tables to ensure columns align perfectly.

## 4. Spacing & Layout (8pt Grid System)

Strict adherence to a 4pt/8pt grid ensures a rhythmic, structured UI.

- **Micro (2-4px):** Inside inputs, between tightly coupled data (`gap-1`).
- **Tight (8px):** Padding inside table cells, between list items (`p-2`, `gap-2`).
- **Standard (16px):** Standard card padding, form group spacing (`p-4`, `gap-4`).
- **Loose (24px-32px):** Page margins, spacing between major page sections (`p-6`, `p-8`).

**Layout Pattern:**
Use a persistent sidebar (dark or light) with a top header. The main content area should have a max-width (e.g., `max-w-7xl`) for readability on ultrawide monitors, or be fluid for dashboards.

## 5. Shadows & Depth (Soft UI Evolution)

Shadows are used sparingly to communicate elevation and interactive states, not for aesthetics.

- **Level 0 (Flat):** `shadow-none` - Main layout panels, sidebars.
- **Level 1 (Cards/Tables):** `shadow-sm` + `border border-slate-200` - The default for data containers on the `slate-50` background.
- **Level 2 (Dropdowns/Popovers):** `shadow-md` + `border border-slate-100` - Elevated menus.
- **Level 3 (Modals/Dialogs):** `shadow-xl` + `border border-slate-100` - Focus-demanding overlays.

## 6. Components for Data Density

- **Data Tables:**
  - `table-fixed` for predictable column widths.
  - Tight vertical padding (`py-2` or `py-1.5`) to fit more rows.
  - Hover states on rows (`hover:bg-slate-50`) to help tracking across wide tables.
  - Sticky headers for infinite scrolling.
- **Forms:**
  - Stacked labels above inputs (`flex-col`) for faster scanning.
  - Distinct input borders (`border-slate-300`, focus: `ring-2 ring-blue-500`).
  - Read-only fields should look distinct (e.g., `bg-slate-50`, no border).
- **Action Density:**
  - Group secondary actions into an ellipsis (`...`) Dropdown to save horizontal space.
  - Use icon buttons (with tooltips!) for repetitive row actions (edit, delete).

## 7. Enterprise Anti-Patterns to Avoid 🚫

1. **"The Wall of Text" (Poor Hierarchy):**
   - *Avoid:* Presenting all data with the same font weight and color.
   - *Fix:* Use `text-slate-500` for labels, `font-medium` for key values, and spacing to create chunks.
2. **"Mystery Meat Navigation":**
   - *Avoid:* Relying solely on icons for primary actions or sidebar menus without tooltips or text labels.
   - *Fix:* Always pair icons with text labels for primary navigation. Use tooltips aggressively.
3. **Over-use of Modals:**
   - *Avoid:* Opening a modal, which opens another modal (modal stacking).
   - *Fix:* Use **Slide-over / Drawers** (Right-side panels) for complex data entry or details viewing while keeping the context of the main table visible.
4. **Low Contrast Text (Aesthetic over Accessibility):**
   - *Avoid:* Using light gray (`text-slate-300`) on white backgrounds because it looks "clean."
   - *Fix:* Ensure at least WCAG AA contrast ratio. `text-slate-500` is the lightest text allowed on white.
5. **Wasting Vertical Space:**
   - *Avoid:* Massive padding and huge headers (like a consumer app).
   - *Fix:* Keep headers compact. Users need to see as many rows of data as possible.

---
*Generated by Antigravity - Architecture Agent*
