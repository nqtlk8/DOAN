import axiosInstance from './axiosInstance';

export const ApiService = {
  SalesInvoice: {
    getAll: () => axiosInstance.get('/api/v1/sales-invoices').then((res: any) => res.data.data),
    getById: (id: string) => axiosInstance.get('/api/v1/sales-invoices/' + id).then((res: any) => res.data.data),
    create: (payload: any) =>
      axiosInstance.post('/api/v1/sales-invoices', payload).then((res: any) => res.data.data),
    confirm: (id: string) =>
      axiosInstance.post('/api/v1/sales-invoices/' + id + '/confirm').then((res: any) => res.data.data),
  },
  InboundReceipt: {
    getAll: () => axiosInstance.get('/api/v1/inventory/inbound').then((res: any) => res.data.data),
    getById: (id: string) => axiosInstance.get('/api/v1/inventory/inbound/' + id).then((res: any) => res.data.data),
    create: (payload: any) =>
      axiosInstance.post('/api/v1/inventory/inbound', payload).then((res: any) => res.data.data),
    confirm: (id: string) =>
      axiosInstance.post('/api/v1/inventory/inbound/' + id + '/confirm').then((res: any) => res.data.data),
  },
  GoodsReturn: {
    getAll: () => axiosInstance.get('/api/v1/goods-returns').then((res: any) => res.data.data),
    getById: (id: string) => axiosInstance.get('/api/v1/goods-returns/' + id).then((res: any) => res.data.data),
    create: (payload: any) =>
      axiosInstance.post('/api/v1/goods-returns', payload).then((res: any) => res.data.data),
    confirm: (id: string) =>
      axiosInstance.post('/api/v1/goods-returns/' + id + '/confirm').then((res: any) => res.data.data),
  },
  Catalog: {
    getProducts: () => axiosInstance.get('/api/v1/catalog/products').then((res: any) => res.data.data),
    createProduct: (payload: any) =>
      axiosInstance.post('/api/v1/catalog/products', payload).then((res: any) => res.data.data),
    updateProduct: (id: string, payload: any) =>
      axiosInstance.put('/api/v1/catalog/products/' + id, payload).then((res: any) => res.data.data),
    deleteProduct: (id: string) => axiosInstance.delete('/api/v1/catalog/products/' + id).then((res: any) => res.data.data),
    searchProducts: (query: string) =>
      axiosInstance.get('/api/v1/catalog/products?search=' + encodeURIComponent(query)).then((res: any) => res.data.data),

    getCustomers: () => axiosInstance.get('/api/v1/customers').then((res: any) => res.data.data),
    createCustomer: (payload: any) =>
      axiosInstance.post('/api/v1/customers', payload).then((res: any) => res.data.data),
    updateCustomer: (id: string, payload: any) =>
      axiosInstance.put('/api/v1/customers/' + id, payload).then((res: any) => res.data.data),
    deleteCustomer: (id: string) =>
      axiosInstance.delete('/api/v1/customers/' + id).then((res: any) => res.data.data),
    searchCustomers: (query: string) =>
      axiosInstance.get('/api/v1/customers?search=' + encodeURIComponent(query)).then((res: any) => res.data.data),

    getSuppliers: () => axiosInstance.get('/api/v1/suppliers').then((res: any) => res.data.data),
    createSupplier: (payload: any) =>
      axiosInstance.post('/api/v1/suppliers', payload).then((res: any) => res.data.data),
    updateSupplier: (id: string, payload: any) =>
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
    getAll: () => axiosInstance.get('/api/v1/branches').then((res: any) => res.data.data),
    getById: (id: string) => axiosInstance.get('/api/v1/branches/' + id).then((res: any) => res.data.data),
  },
  Inventory: {
    getStockMovements: (productId: string) => axiosInstance.get(`/api/v1/stock-movements?productId=${productId}`).then((res: any) => res.data.data),
  },
  Stock: {
    getAll: () => axiosInstance.get('/api/v1/inventory/stock').then((res: any) => res.data.data),
  },
  Debt: {
    getAll: () => axiosInstance.get('/api/v1/receivable-debts').then((res: any) => res.data.data),
  }
};
