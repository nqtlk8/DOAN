# Front-End Architecture: Sprint 5 Components

## 1. PurchaseOrderForm (New Component)
- **Path:** `src/components/purchasing/PurchaseOrderForm.tsx`
- **Purpose:** UI for creating a new Purchase Order from a distributor.
- **State Management:** Uses custom hook `usePurchaseOrder()` to manage line items, distributor selection, and calculation of total costs (including additional costs).
- **API Facade Call:** `ApiService.Purchasing.createOrder(payload)`
- **Key Features:**
  - Distributor dropdown/search (integration with Partner context).
  - Dynamic table for adding/removing product line items (Product selection).
  - Price and quantity inputs with real-time total calculation.
  - Payment method selection (Cash, Bank Transfer, Credit/Debt).
- **Security:** Visible only to `admin` role.

## 2. SalesOrderForm (Enhancement: Return Mode)
- **Path:** `src/components/sales/SalesReturnForm.tsx` (extracted from or complementing SalesOrderForm)
- **Purpose:** Upgrading existing sales features to support processing customer returns.
- **Design Approach:**
  - Introduce a `SalesReturnForm` component that specifically handles reverse logistics.
  - Require `originalSalesOrderId` input to lookup the original transaction.
  - Lock maximum return quantity based on original purchase quantity.
- **State Management:** Custom hook `useSalesReturn()` to fetch original order details and track return quantities.
- **API Facade Call:** `ApiService.Sales.createReturn(payload)`
- **Key Features:**
  - Dropdown/Input for Return Reason.
  - Selection of Refund Method (Cash return vs Accounts Receivable deduction).
  - Dynamic calculation of Refund Amount.
- **Security:** Accessible by `admin` and `sales` roles.
