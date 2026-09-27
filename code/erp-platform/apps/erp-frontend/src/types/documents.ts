import React from 'react';

export type FormMode = 'VIEW' | 'ADD' | 'EDIT';
export type DocStatus = 'DRAFT' | 'CONFIRMED' | 'CANCELLED';

export interface DocumentLine {
  id: string;                 // id tạm phía client
  productId: string;
  productCode?: string;
  productName: string;
  unitOfMeasure?: string;
  quantity: number;
  unitPrice: number;          // với phiếu nhập: đây là giá nhập (map sang unitCost khi gọi api)
}

export interface InfoField   { key: string; label: string; value: React.ReactNode; testId?: string; }
export interface PartnerField {
  key: string; label: string; value: string;
  onChange?: (v: string) => void;   // không có => chỉ đọc (hiển thị dòng chữ)
  required?: boolean; placeholder?: string; testId?: string;
}
export interface SummaryField {
  key: string; label: string; value: number;
  onChange?: (v: number) => void;   // có => ô NumberInput
  tone?: 'default' | 'primary' | 'danger';
  strong?: boolean; testId?: string;
}

/* Type trả về từ Backend cho Phiếu Nhập Lại Hàng Bán (Goods Return) */
export interface GoodsReturnLineResponse {
  id?: string;
  productId: number;
  productCode?: string;
  productName?: string;
  quantity: number;
  unitOfMeasure?: string;
  unitPrice: number;
  lineAmount?: number;
}
export interface GoodsReturnResponse {
  id: string;
  returnCode?: string;
  branchId?: number;
  customerId?: string;
  customerName?: string;
  invoiceId?: string;
  status?: DocStatus;
  totalAmount?: number;
  refundAmount?: number;
  reason?: string;
  createdAt?: string;
  lines?: GoodsReturnLineResponse[];
}
