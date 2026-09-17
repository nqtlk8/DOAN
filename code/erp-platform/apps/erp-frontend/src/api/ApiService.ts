import axios from 'axios';
import axiosInstance from './axiosInstance';
import type { components } from '@erp/api-contract';

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
    create: (payload: components['schemas']['SalesInvoiceCreateDto']): Promise<components['schemas']['SalesInvoiceCreateResponseDto']> =>
      axiosInstance.post('/api/v1/sales-invoices', payload).then((res: any) => res.data.data),
    confirm: (id: string): Promise<components['schemas']['SalesInvoiceCreateResponseDto']> =>
      axiosInstance.post('/api/v1/sales-invoices/' + id + '/confirm').then((res: any) => res.data.data),
  },
  InboundReceipt: {
    getAll: (): Promise<components['schemas']['InboundReceiptResponseDto'][]> => axiosInstance.get('/api/v1/inventory/inbound').then((res: any) => res.data.data),
    getById: (id: string): Promise<components['schemas']['InboundReceiptResponseDto']> => axiosInstance.get('/api/v1/inventory/inbound/' + id).then((res: any) => res.data.data),
    create: (payload: components['schemas']['InboundReceiptCreateDto']): Promise<components['schemas']['InboundReceiptCreateResponseDto']> =>
      axiosInstance.post('/api/v1/inventory/inbound', payload).then((res: any) => res.data.data),
    confirm: (id: string) =>
      axiosInstance.post('/api/v1/inventory/inbound/' + id + '/confirm').then((res: any) => res.data.data),
  },
  GoodsReturn: {
    getAll: (): Promise<components['schemas']['GoodsReturnResponseDto'][]> => axiosInstance.get('/api/v1/goods-returns').then((res: any) => res.data.data),
    getById: (id: string): Promise<components['schemas']['GoodsReturnResponseDto']> => axiosInstance.get('/api/v1/goods-returns/' + id).then((res: any) => res.data.data),
    create: (payload: components['schemas']['GoodsReturnCreateDto']): Promise<components['schemas']['GoodsReturnCreateResponseDto']> =>
      axiosInstance.post('/api/v1/goods-returns', payload).then((res: any) => res.data.data),
    confirm: (id: string): Promise<components['schemas']['GoodsReturnCreateResponseDto']> =>
      axiosInstance.post('/api/v1/goods-returns/' + id + '/confirm').then((res: any) => res.data.data),
  },
  Catalog: {
    getProducts: (): Promise<components['schemas']['ProductResponseDto'][]> => axiosInstance.get('/api/v1/catalog/products').then((res: any) => res.data.data),
    createProduct: (payload: components['schemas']['ProductCreateDto']): Promise<components['schemas']['ProductResponseDto']> =>
      axiosInstance.post('/api/v1/catalog/products', payload).then((res: any) => res.data.data),
    updateProduct: (id: string, payload: components['schemas']['ProductUpdateDto']): Promise<components['schemas']['ProductResponseDto']> =>
      axiosInstance.put('/api/v1/catalog/products/' + id, payload).then((res: any) => res.data.data),
    deleteProduct: (id: string) => axiosInstance.delete('/api/v1/catalog/products/' + id).then((res: any) => res.data.data),
    searchProducts: (query: string): Promise<components['schemas']['ProductResponseDto'][]> =>
      axiosInstance.get('/api/v1/catalog/products?search=' + encodeURIComponent(query)).then((res: any) => res.data.data),

    getCustomers: (): Promise<components['schemas']['CustomerResponseDto'][]> => 
      axiosInstance.get('/api/v1/customers').then((res: any) => res.data.data),
    getCustomerById: (id: string): Promise<components['schemas']['CustomerResponseDto']> => 
      axiosInstance.get('/api/v1/customers/' + id).then((res: any) => res.data.data),
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
    get: (customerId: string, productId: string) =>
      axiosInstance.get(`/api/v1/customer-prices/${customerId}/product/${productId}`).then((res: any) => res.data.data),
  },
  Analytics: {
    getDashboardMetrics: (branchId?: number, startDateKey?: number, endDateKey?: number) => {
      const params = new URLSearchParams();
      if (branchId) params.append('branchId', branchId.toString());
      if (startDateKey) params.append('startDateKey', startDateKey.toString());
      if (endDateKey) params.append('endDateKey', endDateKey.toString());
      return axiosInstance.get(`/api/v1/analytics/dashboard?${params.toString()}`).then((res: any) => res.data.data);
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
    getAll: (): Promise<components['schemas']['BranchResponseDto'][]> => axiosInstance.get('/api/v1/branches').then((res: any) => res.data.data),
    getById: (id: string): Promise<components['schemas']['BranchResponseDto']> => axiosInstance.get('/api/v1/branches/' + id).then((res: any) => res.data.data),
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
  }
};
