import apiClient, { type ApiResponse } from './apiClient';
import type {
  CreateQuotationRequest,
  QuotationResponse,
  CreateOrderRequest,
  OrderResponse,
  OrderListItem,
  PaginatedData,
  DeliveryNoteResponse,
  CreateReturnRequest,
  ReturnResponse,
} from '../types/sales';

/**
 * API Facade for Sales Bounded Context
 * Keeps UI components decoupled from direct HTTP calls.
 */
export const SalesService = {
  /**
   * Create a new quotation
   */
  createQuotation: async (
    data: CreateQuotationRequest
  ): Promise<ApiResponse<QuotationResponse>> => {
    const response = await apiClient.post<ApiResponse<QuotationResponse>>(
      '/quotations',
      data
    );
    return response.data;
  },

  /**
   * Create a new sales order
   */
  createOrder: async (
    data: CreateOrderRequest
  ): Promise<ApiResponse<OrderResponse>> => {
    const response = await apiClient.post<ApiResponse<OrderResponse>>(
      '/sales-orders',
      data
    );
    return response.data;
  },

  /**
   * Fetch a list of sales orders with pagination and date filters
   */
  getOrders: async (
    startDate: string,
    endDate: string,
    page: number = 0,
    size: number = 20
  ): Promise<OrderListItem[]> => {
    const response = await apiClient.get<ApiResponse<any>>(
      '/sales-orders',
      {
        params: { startDate, endDate, page, size },
      }
    );
    const payload = response.data?.data;
    return Array.isArray(payload?.items) ? payload.items : (Array.isArray(payload) ? payload : []);
  },

  /**
   * Get delivery note for an order (without pricing details)
   */
  getDeliveryNote: async (
    orderId: string
  ): Promise<ApiResponse<DeliveryNoteResponse>> => {
    const response = await apiClient.get<ApiResponse<DeliveryNoteResponse>>(
      `/sales-orders/${orderId}/delivery-note`
    );
    return response.data;
  },

  /**
   * Process a sales return
   */
  createReturn: async (
    data: CreateReturnRequest
  ): Promise<ApiResponse<ReturnResponse>> => {
    const response = await apiClient.post<ApiResponse<ReturnResponse>>(
      '/sales/returns',
      data
    );
    return response.data;
  },
};

export default SalesService;
