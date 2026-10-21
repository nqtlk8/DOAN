import { MOCK_CATEGORIES } from './mockData';
import { Category } from '../types';

const DELAY = 600; // Mô phỏng trễ mạng 600ms

// Danh mục vẫn là dữ liệu giả: backend chưa có endpoint danh mục công khai (xem Q8 trong bản rà soát).
export const mockApi = {
  // Lấy Menu (Tất cả Root Categories)
  fetchNavCategories: (): Promise<Category[]> => {
    return new Promise(resolve => setTimeout(() => resolve(MOCK_CATEGORIES), DELAY));
  },
};
