import apiClient, { type ApiResponse } from './apiClient';
import type { Customer, Product, Distributor } from '../types/catalog';

export const CatalogService = {
  searchCustomers: async (name: string): Promise<Customer[]> => {
    try {
      const response = await apiClient.get<ApiResponse<Customer[]>>('/customers', {
        params: { name }
      });
      // Fallback in case the server returns an array instead of ApiResponse
      if (Array.isArray(response.data)) {
        return response.data;
      }
      const payload: any = response.data?.data;
      console.log('[DEBUG] searchCustomers payload:', payload);
      return Array.isArray(payload?.items) ? payload.items : (Array.isArray(payload) ? payload : []);
    } catch (error) {
      console.error('Failed to fetch customers', error);
      // Mock data if API fails
      return [
        { id: 'C001', name: 'Nguyễn Văn A', phone: '0901234567', address: 'Hà Nội' },
        { id: 'C002', name: 'Trần Thị B', phone: '0987654321', address: 'Hồ Chí Minh' },
        { id: 'C003', name: 'Công ty TNHH XYZ', phone: '0241234567', address: 'Đà Nẵng' }
      ].filter(c => c.name.toLowerCase().includes(name.toLowerCase()));
    }
  },

  searchProducts: async (name: string): Promise<Product[]> => {
    try {
      const response = await apiClient.get<ApiResponse<Product[]>>('/products', {
        params: { name }
      });
      if (Array.isArray(response.data)) {
        return response.data;
      }
      const payload: any = response.data?.data;
      console.log('[DEBUG] searchProducts payload:', payload);
      return Array.isArray(payload?.items) ? payload.items : (Array.isArray(payload) ? payload : []);
    } catch (error) {
      console.error('Failed to fetch products', error);
      // Mock data if API fails
      return [
        { id: 'P001', name: 'Sản phẩm 1', basePrice: 100000 },
        { id: 'P002', name: 'Sản phẩm 2', basePrice: 250000 },
        { id: 'P003', name: 'Sản phẩm 3', basePrice: 500000 }
      ].filter(p => p.name.toLowerCase().includes(name.toLowerCase()));
    }
  },

  getCustomers: async (): Promise<Customer[]> => {
    try {
      const response = await apiClient.get<ApiResponse<Customer[]>>('/customers');
      if (Array.isArray(response.data)) {
        return response.data;
      }
      const payload: any = response.data?.data;
      console.log('[DEBUG] getCustomers payload:', payload);
      return Array.isArray(payload?.items) ? payload.items : (Array.isArray(payload) ? payload : []);
    } catch (error) {
      console.error('Failed to fetch customers', error);
      return [];
    }
  },

  getProducts: async (): Promise<Product[]> => {
    try {
      const response = await apiClient.get<ApiResponse<Product[]>>('/products');
      if (Array.isArray(response.data)) {
        return response.data;
      }
      const payload: any = response.data?.data;
      console.log('[DEBUG] getProducts payload:', payload);
      return Array.isArray(payload?.items) ? payload.items : (Array.isArray(payload) ? payload : []);
    } catch (error) {
      console.error('Failed to fetch products', error);
      return [];
    }
  },

  searchDistributors: async (name: string): Promise<Distributor[]> => {
    try {
      const response = await apiClient.get<ApiResponse<Distributor[]>>('/distributors', {
        params: { name }
      });
      if (Array.isArray(response.data)) {
        return response.data;
      }
      const payload: any = response.data?.data;
      console.log('[DEBUG] searchDistributors payload:', payload);
      return Array.isArray(payload?.items) ? payload.items : (Array.isArray(payload) ? payload : []);
    } catch (error) {
      console.error('Failed to fetch distributors', error);
      // Mock data if API fails
      return [
        { id: 'D001', name: 'Nhà phân phối A', phone: '0909999999', address: 'Hà Nội' },
        { id: 'D002', name: 'Nhà phân phối B', phone: '0988888888', address: 'Hồ Chí Minh' }
      ].filter(d => d.name.toLowerCase().includes(name.toLowerCase()));
    }
  },

  getDistributors: async (): Promise<Distributor[]> => {
    try {
      const response = await apiClient.get<ApiResponse<Distributor[]>>('/distributors');
      if (Array.isArray(response.data)) {
        return response.data;
      }
      const payload: any = response.data?.data;
      console.log('[DEBUG] getDistributors payload:', payload);
      return Array.isArray(payload?.items) ? payload.items : (Array.isArray(payload) ? payload : []);
    } catch (error) {
      console.error('Failed to fetch distributors', error);
      return [];
    }
  },

  createCustomer: async (customer: Partial<Customer>): Promise<Customer> => {
    const response = await apiClient.post<ApiResponse<Customer>>('/customers', customer);
    return response.data.data;
  },
  
  updateCustomer: async (id: string, customer: Partial<Customer>): Promise<Customer> => {
    const response = await apiClient.put<ApiResponse<Customer>>(`/customers/${id}`, customer);
    return response.data.data;
  },
  
  deleteCustomer: async (id: string): Promise<void> => {
    await apiClient.delete<ApiResponse<void>>(`/customers/${id}`);
  },

  createProduct: async (product: Partial<Product>): Promise<Product> => {
    const response = await apiClient.post<ApiResponse<Product>>('/products', product);
    return response.data.data;
  },
  
  updateProduct: async (id: string, product: Partial<Product>): Promise<Product> => {
    const response = await apiClient.put<ApiResponse<Product>>(`/products/${id}`, product);
    return response.data.data;
  },
  
  deleteProduct: async (id: string): Promise<void> => {
    await apiClient.delete<ApiResponse<void>>(`/products/${id}`);
  },

  createDistributor: async (distributor: Partial<Distributor>): Promise<Distributor> => {
    const response = await apiClient.post<ApiResponse<Distributor>>('/distributors', distributor);
    return response.data.data;
  },
  
  updateDistributor: async (id: string, distributor: Partial<Distributor>): Promise<Distributor> => {
    const response = await apiClient.put<ApiResponse<Distributor>>(`/distributors/${id}`, distributor);
    return response.data.data;
  },
  
  deleteDistributor: async (id: string): Promise<void> => {
    await apiClient.delete<ApiResponse<void>>(`/distributors/${id}`);
  }
};
