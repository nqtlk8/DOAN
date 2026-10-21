import axios from 'axios';
import axiosInstance from './axiosInstance';
import type { components } from '@erp/api-contract';
import type { GoodsReturnCreatePayload, GoodsReturnResponse, ResolvedPriceDto } from '../types/documents';
import type { DashboardMetricsDto, StockAlertSummaryDto } from '../types/analytics';
import type { CashReceiptCreatePayload, CashReceiptCreateResponse, CashReceiptDto } from '../types/cashReceipt';
import { cancelKey, confirmKey, documentWriteConfig, updateKey } from '../shared/idempotency/submitKey';

export const ApiService = {
  Auth: {
    login: (username: string, password: string) =>
      axios.post('/api/v1/auth/login', { username, password }).then(res => res.data),
    revoke: (refreshToken: string) =>
      axios.post('/api/v1/auth/revoke', { refreshToken }).then(res => res.data),
  },
  SalesInvoice: {
    getAll: (): Promise<components['schemas']['SalesInvoiceResponseDto'][]> => axiosInstance.get('/api/v1/sales-invoices').then((res: any) => res.data.data),
    getById: (id: string): Promise<components['schemas']['SalesInvoiceResponseDto']> => axiosInstance.get('/api/v1/sales-invoices/' + id).then((res: any) => res.data.data),
    /** Bước 1: lưu hóa đơn nháp. `idempotencyKey` = key của lần lập phiếu (useSubmitKey). */
    create: (
      payload: components['schemas']['SalesInvoiceCreateDto'],
      idempotencyKey: string,
    ): Promise<components['schemas']['SalesInvoiceCreateResponseDto']> =>
      axiosInstance.post('/api/v1/sales-invoices', payload, documentWriteConfig(idempotencyKey)).then((res: any) => res.data.data),
    /**
     * Sửa hóa đơn khi còn nháp (DRAFT): ghi đè đầu phiếu và toàn bộ dòng hàng. Chưa trừ kho / ghi công nợ.
     * Backend trả 409 INVALID_STATUS nếu hóa đơn đã xác nhận. `editSession` = key của lần bấm "Sửa".
     */
    update: (
      id: string,
      payload: components['schemas']['SalesInvoiceCreateDto'],
      editSession: string,
    ): Promise<components['schemas']['SalesInvoiceCreateResponseDto']> =>
      axiosInstance
        .put('/api/v1/sales-invoices/' + id, payload, documentWriteConfig(updateKey('sales-invoice', id, editSession)))
        .then((res: any) => res.data.data),
    /** Bước 2: xác nhận (trừ kho, ghi công nợ). */
    confirm: (id: string): Promise<void> =>
      axiosInstance
        .post('/api/v1/sales-invoices/' + id + '/confirm', undefined, documentWriteConfig(confirmKey('sales-invoice', id)))
        .then((res: any) => res.data.data),
  },
  InboundReceipt: {
    getAll: (): Promise<components['schemas']['InboundReceiptResponseDto'][]> => axiosInstance.get('/api/v1/inventory/inbound').then((res: any) => res.data.data),
    getById: (id: string): Promise<components['schemas']['InboundReceiptResponseDto']> => axiosInstance.get('/api/v1/inventory/inbound/' + id).then((res: any) => res.data.data),
    create: (
      payload: components['schemas']['InboundReceiptCreateDto'],
      idempotencyKey: string,
    ): Promise<components['schemas']['InboundReceiptCreateResponseDto']> =>
      axiosInstance.post('/api/v1/inventory/inbound', payload, documentWriteConfig(idempotencyKey)).then((res: any) => res.data.data),
    /** Sửa phiếu nhập khi còn nháp (DRAFT). 409 INVALID_STATUS nếu đã xác nhận. */
    update: (
      id: string,
      payload: components['schemas']['InboundReceiptCreateDto'],
      editSession: string,
    ): Promise<components['schemas']['InboundReceiptCreateResponseDto']> =>
      axiosInstance
        .put('/api/v1/inventory/inbound/' + id, payload, documentWriteConfig(updateKey('inbound-receipt', id, editSession)))
        .then((res: any) => res.data.data),
    confirm: (id: string) =>
      axiosInstance
        .post('/api/v1/inventory/inbound/' + id + '/confirm', undefined, documentWriteConfig(confirmKey('inbound-receipt', id)))
        .then((res: any) => res.data.data),
  },
  GoodsReturn: {
    getAll: (): Promise<GoodsReturnResponse[]> => axiosInstance.get('/api/v1/goods-returns').then((res: any) => res.data.data),
    getById: (id: string): Promise<GoodsReturnResponse> => axiosInstance.get('/api/v1/goods-returns/' + id).then((res: any) => res.data.data),
    /** Backend trả về UUID (chuỗi) của phiếu vừa tạo trong `data`. */
    create: (payload: GoodsReturnCreatePayload, idempotencyKey: string): Promise<string> =>
      axiosInstance.post('/api/v1/goods-returns', payload, documentWriteConfig(idempotencyKey)).then((res: any) => res.data.data),
    /** Sửa phiếu trả khi còn nháp (DRAFT). 409 INVALID_STATUS nếu đã xác nhận. */
    /** Backend trả về UUID (chuỗi) của phiếu trong `data`. */
    update: (id: string, payload: GoodsReturnCreatePayload, editSession: string): Promise<string> =>
      axiosInstance
        .put('/api/v1/goods-returns/' + id, payload, documentWriteConfig(updateKey('goods-return', id, editSession)))
        .then((res: any) => res.data.data),
    confirm: (id: string): Promise<void> =>
      axiosInstance
        .post('/api/v1/goods-returns/' + id + '/confirm', undefined, documentWriteConfig(confirmKey('goods-return', id)))
        .then((res: any) => res.data.data),
  },
  Catalog: {
    getCategories: (): Promise<components['schemas']['CategoryResponseDto'][]> =>
      axiosInstance.get('/api/v1/catalog/categories').then((res: any) => res.data.data),
    getProducts: (): Promise<components['schemas']['ProductResponseDto'][]> => axiosInstance.get('/api/v1/catalog/products').then((res: any) => res.data.data),
    /**
     * Danh sách sản phẩm kèm giá niêm yết. Nhân viên luôn nhận giá của chi nhánh mình (lấy từ token);
     * quản trị viên ở HQ không gắn chi nhánh nên truyền `branchId` để chọn bảng giá cần xem.
     */
    getProductsWithPrice: (branchId?: number): Promise<components['schemas']['ProductResponseDto'][]> =>
      axiosInstance
        .get('/api/v1/catalog/products', { params: { withBranchPrice: true, ...(branchId ? { branchId } : {}) } })
        .then((res: any) => res.data.data),
    createProduct: (payload: components['schemas']['ProductCreateDto']): Promise<components['schemas']['ProductResponseDto']> =>
      axiosInstance.post('/api/v1/catalog/products', payload).then((res: any) => res.data.data),
    updateProduct: (id: string, payload: components['schemas']['ProductUpdateDto']): Promise<components['schemas']['ProductResponseDto']> =>
      axiosInstance.put('/api/v1/catalog/products/' + id, payload).then((res: any) => res.data.data),
    deleteProduct: (id: string) => axiosInstance.delete('/api/v1/catalog/products/' + id).then((res: any) => res.data.data),
    searchProducts: (query: string): Promise<components['schemas']['ProductResponseDto'][]> =>
      axiosInstance.get('/api/v1/catalog/products?withBranchPrice=true&search=' + encodeURIComponent(query)).then((res: any) => res.data.data),

    getCustomers: (): Promise<components['schemas']['CustomerResponseDto'][]> => 
      axiosInstance.get('/api/v1/customers').then((res: any) => res.data.data),
    createCustomer: (payload: components['schemas']['CustomerCreateDto']): Promise<components['schemas']['CustomerResponseDto']> =>
      axiosInstance.post('/api/v1/customers', payload).then((res: any) => res.data.data),
    updateCustomer: (id: string, payload: components['schemas']['CustomerUpdateDto']): Promise<components['schemas']['CustomerResponseDto']> =>
      axiosInstance.put('/api/v1/customers/' + id, payload).then((res: any) => res.data.data),
    deleteCustomer: (id: string) =>
      axiosInstance.delete('/api/v1/customers/' + id).then((res: any) => res.data.data),
    searchCustomers: (query: string): Promise<components['schemas']['CustomerResponseDto'][]> =>
      axiosInstance.get('/api/v1/customers?search=' + encodeURIComponent(query)).then((res: any) => res.data.data),

    getSuppliers: (): Promise<components['schemas']['SupplierResponseDto'][]> => axiosInstance.get('/api/v1/suppliers').then((res: any) => res.data.data),
    createSupplier: (payload: components['schemas']['SupplierCreateDto']): Promise<components['schemas']['SupplierResponseDto']> =>
      axiosInstance.post('/api/v1/suppliers', payload).then((res: any) => res.data.data),
    updateSupplier: (id: string, payload: components['schemas']['SupplierUpdateDto']): Promise<components['schemas']['SupplierResponseDto']> =>
      axiosInstance.put('/api/v1/suppliers/' + id, payload).then((res: any) => res.data.data),
    deleteSupplier: (id: string) =>
      axiosInstance.delete('/api/v1/suppliers/' + id).then((res: any) => res.data.data),
  },
  CustomerPrice: {
    /** Giá áp dụng cho khách tại chi nhánh hiện tại: giá riêng của khách > giá niêm yết. */
    resolve: (customerId: string, productIds: Array<string | number>): Promise<ResolvedPriceDto[]> =>
      axiosInstance
        .get(`/api/v1/customer-prices/${customerId}/resolve`, { params: { productIds: productIds.join(',') } })
        .then((res: any) => res.data.data ?? []),
  },
  Analytics: {
    getDashboardMetrics: (branchId?: number, startDateKey?: number, endDateKey?: number, topLimit?: number): Promise<DashboardMetricsDto> => {
      const params = new URLSearchParams();
      if (topLimit != null) params.append('topLimit', topLimit.toString());
      if (branchId) params.append('branchId', branchId.toString());
      if (startDateKey) params.append('startDateKey', startDateKey.toString());
      if (endDateKey) params.append('endDateKey', endDateKey.toString());
      return axiosInstance.get(`/api/v1/analytics/dashboard?${params.toString()}`).then((res: any) => res.data.data);
    },
    getStockAlerts: (branchId?: number): Promise<StockAlertSummaryDto> => {
      const params = new URLSearchParams();
      if (branchId) params.append('branchId', branchId.toString());
      return axiosInstance.get(`/api/v1/analytics/stock-alerts?${params.toString()}`).then((res: any) => res.data.data);
    },
    exportExcel: (branchId?: number, startDateKey?: number, endDateKey?: number) => {
      const params = new URLSearchParams();
      if (branchId) params.append('branchId', branchId.toString());
      if (startDateKey) params.append('startDateKey', startDateKey.toString());
      if (endDateKey) params.append('endDateKey', endDateKey.toString());
      return axiosInstance.get(`/api/v1/analytics/export/excel?${params.toString()}`, { responseType: 'blob' })
        .then((res: any) => res.data);
    },
  },
  Branch: {
    getAll: (): Promise<any[]> => axiosInstance.get('/api/v1/branches').then((res: any) => res.data.data),
  },
  Inventory: {
    getStockMovements: (productId: string): Promise<components['schemas']['StockMovementResponseDto'][]> => axiosInstance.get(`/api/v1/stock-movements?productId=${productId}`).then((res: any) => res.data.data),
  },
  Stock: {
    getAll: (): Promise<components['schemas']['StockOnHandResponseDto'][]> => axiosInstance.get('/api/v1/inventory/stock').then((res: any) => res.data.data),
  },
  Debt: {
    getAll: (): Promise<components['schemas']['ReceivableDebtResponseDto'][]> => axiosInstance.get('/api/v1/receivable-debts').then((res: any) => res.data.data),
    getMovements: (customerId: string): Promise<components['schemas']['ReceivableDebtMovementResponseDto'][]> => axiosInstance.get('/api/v1/receivable-debts/' + customerId + '/movements').then((res: any) => res.data.data),
    getBalance: (customerId: string): Promise<number> => axiosInstance.get('/api/v1/receivable-debts/' + customerId + '/balance').then((res: any) => res.data.data),
  },
  /** Phiếu thu tiền khách hàng — chỉ có ở chi nhánh (STAFF). */
  CashReceipt: {
    getAll: (): Promise<CashReceiptDto[]> => axiosInstance.get('/api/v1/cash-receipts').then((res: any) => res.data.data),
    getById: (id: string): Promise<CashReceiptDto> => axiosInstance.get('/api/v1/cash-receipts/' + id).then((res: any) => res.data.data),
    create: (payload: CashReceiptCreatePayload, idempotencyKey: string): Promise<CashReceiptCreateResponse> =>
      axiosInstance.post('/api/v1/cash-receipts', payload, documentWriteConfig(idempotencyKey)).then((res: any) => res.data.data),
    cancel: (id: string, reason?: string): Promise<CashReceiptDto> =>
      axiosInstance
        .post('/api/v1/cash-receipts/' + id + '/cancel', { reason: reason?.trim() || null }, documentWriteConfig(cancelKey('cash-receipt', id)))
        .then((res: any) => res.data.data),
  },
};
