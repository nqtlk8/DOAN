// Common Pagination Types
export interface PaginatedData<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// 1. Quotation Types
export interface QuotationItemDTO {
  productId: string;
  quantity: number;
  unitPrice: number;
}

export interface CreateQuotationRequest {
  customerId: string;
  validUntil: string;
  items: QuotationItemDTO[];
}

export interface QuotationResponse {
  quotationId: string;
  totalAmount: number;
  status: string; // e.g. "DRAFT"
}

// 2. Sales Order Types
export interface OrderItemDTO {
  productId: string;
  quantity: number;
  unitPrice: number;
  discount: number;
}

export interface CreateOrderRequest {
  customerId: string;
  referenceQuotationId?: string;
  paymentMethod: string;
  items: OrderItemDTO[];
}

export interface OrderResponse {
  orderId: string;
  status: string; // e.g. "CONFIRMED"
  totalAmount: number;
}

// 3. Order List Types
export interface OrderListItem {
  orderId: string;
  code?: string;
  customerId: string;
  customerName?: string;
  totalAmount: number;
  createdAt: string;
  status: string;
}

// 4. Delivery Note Types (No pricing information)
export interface DeliveryNoteItem {
  productId: string;
  productName: string;
  quantity: number;
}

export interface DeliveryNoteResponse {
  orderId: string;
  customerName: string;
  deliveryAddress: string;
  items: DeliveryNoteItem[];
  note: string;
}

// 5. Sales Return Types
export interface ReturnItemDTO {
  productId: string;
  quantity: number;
}

export interface CreateReturnRequest {
  originalOrderId: string;
  reason: string;
  items: ReturnItemDTO[];
}

export interface ReturnResponse {
  returnId: string;
  refundAmount: number;
  status: string; // e.g. "PROCESSED"
}
