# Frontend Component Hierarchy

This document outlines the high-level React component hierarchy for the ERP system, specifically highlighting the structure after the MDI Module Template refactoring.

## Main Application Structure

```text
App (Root Provider & Router)
 └── MainLayout (Global Layout - Header, Main Navigation, Footer)
      └── Module Router (Handles routing to specific modules based on URL)
           ├── SalesRoute
           │    └── SalesModule (Container)
           │         └── MdiModuleLayout (Module-level Layout)
           │              ├── Sidebar (Module Navigation & Context)
           │              ├── MainContent (Dynamic rendering based on activeSubView)
           │              │    ├── SalesList (activeSubView: 'LIST')
           │              │    └── SalesOrderForm (activeSubView: 'FORM')
           │              └── BottomToolbar (Global Actions & Hotkeys F2-F12)
           │
           ├── PurchasingRoute
           │    └── PurchasingModule (Container)
           │         └── MdiModuleLayout
           │              ├── Sidebar
           │              ├── MainContent (PurchasingList / PurchaseOrderForm)
           │              └── BottomToolbar
           │
           └── ... (Other Modules)
```

## Component Roles & Responsibilities

### Layouts
- **`MainLayout`**: The top-level layout providing the global navigation shell (Top Header, Main Sidebar menu for app-wide navigation).
- **`MdiModuleLayout`**: The standardized template for individual modules. It handles the placement of module-specific sidebars, main working areas, and a persistent bottom action toolbar. It also registers module-level hotkeys (F2-F12).

### Module Containers
- **`<Domain>Module` (e.g., `SalesModule`)**: Acts as the orchestrator for a specific bounded context. It holds local module state such as `activeSubView` (List vs. Form) and `currentDocumentId`. It handles cross-component communication within the module (e.g., switching to form view when a list item is double-clicked).

### Views & Forms
- **`<Domain>List` (e.g., `SalesList`)**: A data grid component for viewing multiple records. Triggers callbacks to the parent module container on interactions like row double-clicks.
- **`<Domain>Form` (e.g., `SalesOrderForm`)**: A detail component for viewing, creating, or editing a specific record.

### Shared UI Components
- **`BottomToolbar`**: Extracted generic component for actions like Save, Edit, Print. Configurable via props passed down from `MdiModuleLayout`.
- **`Sidebar`**: Contextual sidebar used within `MdiModuleLayout` for sub-navigation or showing contextual lists.
