import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ApiService } from '../api/ApiService';
import { notify } from '../shared/notifications/notification';
import type { Product } from '../types/catalog';

export const useProducts = () => {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ['products'],
    queryFn: () => ApiService.Catalog.getProducts(),
  });

  const createMutation = useMutation({
    mutationFn: (data: any) => ApiService.Catalog.createProduct(data),
    onSuccess: () => {
      notify.success('Đã tạo sản phẩm thành công');
      queryClient.invalidateQueries({ queryKey: ['products'] });
    }
  });

  const updateMutation = useMutation({
    mutationFn: (data: { id: number; payload: any }) => ApiService.Catalog.updateProduct(data.id.toString(), data.payload),
    onSuccess: () => {
      notify.success('Đã cập nhật sản phẩm thành công');
      queryClient.invalidateQueries({ queryKey: ['products'] });
    }
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => ApiService.Catalog.deleteProduct(id.toString()),
    onSuccess: () => {
      notify.success('Đã xóa sản phẩm thành công');
      queryClient.invalidateQueries({ queryKey: ['products'] });
    }
  });

  return {
    query,
    createMutation,
    updateMutation,
    deleteMutation
  };
};
