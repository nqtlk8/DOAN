/** Phiếu thu tiền khách hàng — khớp CashReceiptResponseDto / CashReceiptCreateDto ở backend. */
export type CashReceiptStatus = 'POSTED' | 'CANCELLED';

export interface CashReceiptDto {
  id: string;
  receiptCode: string;
  branchId: number;
  customerId: string;
  customerCode?: string | null;
  customerName?: string | null;
  customerPhone?: string | null;
  customerAddress?: string | null;
  receiptDate: string; // yyyy-MM-dd
  amount: number;
  debtBefore: number;
  debtAfter: number;
  note?: string | null;
  status: CashReceiptStatus;
  createdAt?: string | null;
  createdBy?: string | null;
  cancelledAt?: string | null;
  cancelledBy?: string | null;
  cancelReason?: string | null;
}

export interface CashReceiptCreatePayload {
  customerId: string;
  amount: number;
  receiptDate: string; // yyyy-MM-dd
  note?: string;
}

export interface CashReceiptCreateResponse {
  id: string;
  receiptCode: string;
}

/** Khách hàng chọn sẵn khi mở phiếu thu từ màn Công nợ. */
export interface ReceiptCustomer {
  id: string;
  customerCode?: string | null;
  name?: string | null;
  phone?: string | null;
  address?: string | null;
}
