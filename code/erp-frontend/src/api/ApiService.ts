import axiosInstance from './axiosInstance';

// Central API Facade following Document-Driven Development guidelines
export const ApiService = {
  Order: {
    getAll: () => axiosInstance.get('/orders').then(res => res.data),
    getById: (id: string) => axiosInstance.get(`/orders/${id}`).then(res => res.data),
    create: (payload: any) => axiosInstance.post('/orders', payload).then(res => res.data),
    update: (id: string, payload: any) => axiosInstance.put(`/orders/${id}`, payload).then(res => res.data),
    delete: (id: string) => axiosInstance.delete(`/orders/${id}`).then(res => res.data),
  },
  Purchasing: {
    createOrder: (payload: any) => axiosInstance.post('/purchase-orders', payload).then(res => res.data),
    getOrders: () => axiosInstance.get('/purchase-orders').then(res => res.data),
  },
  Sales: {
    createReturn: (payload: any) => axiosInstance.post('/sales-returns', payload).then(res => res.data),
  },
  Catalog: {
    getProducts: () => axiosInstance.get('/products').then(res => res.data),
  },
};
