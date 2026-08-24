# MDI Module Template Architecture

## 1. Overview
This document outlines the architecture for the standard Module Layout used across the ERP system (MDI - Multiple Document Interface style), specifically focusing on the generic layout template and how specific modules (like Sales) utilize it.

## 2. Core Components

### 2.1. `MdiModuleLayout` Component
The `MdiModuleLayout` is a highly reusable layout wrapper designed to provide a consistent UI structure for all business modules.

**Structure:**
- **Sidebar (Left):** Contextual sidebar that can display sub-navigation, module-specific actions, or a document list (e.g., list of sales orders).
- **Main Content (Center):** The primary working area that renders the `children` passed to the layout.
- **Bottom Toolbar:** A standardized action bar at the bottom of the screen (e.g., Save, Edit, Cancel, Print).

**Extracted Responsibilities:**
- **Bottom Toolbar:** Extracted from individual forms (like `SalesOrderForm`) to ensure a uniform appearance and behavior across all modules. It accepts configuration/props to enable, disable, or hide specific action buttons based on the active view state.
- **Hotkeys Registration (F2-F12):** Global keyboard shortcuts for standard actions (e.g., F2 for Save, F3 for New, F4 for Edit) are registered within this layout layer to decouple them from specific form implementations and avoid redundant code.

### 2.2. `SalesModule` Component (Module Container)
The `SalesModule` acts as the container/orchestrator for the Sales bounded context, utilizing the `MdiModuleLayout`.

**State Management:**
- `activeSubView`: Manages the current active view within the module. Possible states include `FORM` (Data Entry/Detail View) and `LIST` (Data Grid/Overview).
- `currentDocumentId`: Stores the ID of the document currently being viewed or edited (e.g., `orderId`).

**Behavior:**
- Conditionally renders `SalesOrderForm` or `SalesList` as the `children` of `MdiModuleLayout` based on `activeSubView`.
- **Sidebar Integration:** Uses the layout's sidebar area to provide navigation between the `FORM` and `LIST` views.
- **Interaction Logic:** 
    - Implements a callback for the `SalesList` component.
    - When a user double-clicks a row in the `SalesList`, the callback is triggered.
    - The callback updates the state: switches `activeSubView` to `FORM` and sets `currentDocumentId` to the selected `orderId`, thereby rendering the detail form for that specific order.
