import axios from 'axios';
import { NormalizedApiError } from './ApiError';

export function normalizeApiError(error: unknown): NormalizedApiError {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status;
    const responseData = error.response?.data;

    // 1. Lấy message từ backend (ApiResponse format)
    let message = 'Không thể kết nối đến máy chủ.';
    
    if (responseData) {
      if (typeof responseData.message === 'string' && responseData.message.trim() !== '') {
        // Business message từ hệ thống
        message = responseData.message;
      } else if (responseData.error && typeof responseData.error === 'string') {
        // Spring default error message
        message = responseData.error;
      }
    } else if (error.message) {
       if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
         message = 'Kết nối mạng quá hạn. Vui lòng kiểm tra lại đường truyền.';
       } else if (error.message.includes('Network Error')) {
         message = 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.';
       }
    }

    // 2. Chuyển đổi mã HTTP thành message business thân thiện nếu không có message cụ thể
    if (!responseData || !responseData.message) {
      switch (status) {
        case 400:
          message = 'Dữ liệu gửi lên không hợp lệ.';
          break;
        case 401:
          message = 'Phiên đăng nhập đã hết hạn hoặc không hợp lệ.';
          break;
        case 403:
          message = 'Bạn không có quyền thực hiện thao tác này.';
          break;
        case 404:
          message = 'Không tìm thấy dữ liệu yêu cầu.';
          break;
        case 409:
          message = 'Dữ liệu bị xung đột (VD: Đã tồn tại).';
          break;
        case 500:
        case 502:
        case 503:
        case 504:
          message = 'Hệ thống đang gặp sự cố. Vui lòng thử lại sau.';
          break;
      }
    }

    return {
      status: status,
      code: error.code,
      message: message,
      details: responseData?.errors || responseData,
    };
  }

  // Fallback cho Error thông thường
  if (error instanceof Error) {
    return {
      message: error.message,
    };
  }

  return {
    message: 'Đã xảy ra lỗi không xác định.',
  };
}
