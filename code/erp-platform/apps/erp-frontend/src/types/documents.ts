import React from 'react';
import type { components } from '@erp/api-contract';

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
  tone?: 'default' | 'primary' | 'danger';   // danger chỉ tô đỏ khi value > 0
  strong?: boolean; testId?: string;
  allowNegative?: boolean;          // cho phép nhập số âm (vd Nợ trước)
  isQuantity?: boolean;             // hiển thị dạng số lượng (không kèm ₫)
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

export interface GoodsReturnCreatePayload {
  customerId: string;
  reason?: string;
  note?: string;
  invoiceId?: string;
  lines: { productId: number; quantity: number; unitPrice: number; unitOfMeasure: string }[];
}

/* Alias ngắn cho DTO sinh từ OpenAPI */
export type ProductDto = components['schemas']['ProductResponseDto'];
export type SupplierDto = components['schemas']['SupplierResponseDto'];
export type CustomerDto = components['schemas']['CustomerResponseDto'];
export type InboundReceiptDto = components['schemas']['InboundReceiptResponseDto'];
export type SalesInvoiceDto = components['schemas']['SalesInvoiceResponseDto'];
