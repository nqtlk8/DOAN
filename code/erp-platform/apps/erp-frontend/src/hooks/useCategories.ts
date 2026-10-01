import { useQuery } from '@tanstack/react-query';
import { ApiService } from '../api/ApiService';
import type { Category } from '../types/catalog';

/**
 * Danh mục sản phẩm cho dropdown. Danh mục ít thay đổi (master data do HQ quản lý),
 * nên giữ cache 10 phút thay vì gọi lại mỗi lần mở form.
 */
export const useCategories = () =>
  useQuery({
    queryKey: ['categories'],
    queryFn: async () => (await ApiService.Catalog.getCategories()) as Category[],
    staleTime: 10 * 60 * 1000,
  });
