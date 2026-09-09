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
      notify.success('Ðã t?o s?n ph?m thành công');
      queryClient.invalidateQueries({ queryKey: ['products'] });
    },
    onError: (err: any) => {
      notify.error(err.message || 'Không th? t?o s?n ph?m');
    }
  });

  const updateMutation = useMutation({
    mutationFn: (data: { id: string; payload: any }) => ApiService.Catalog.updateProduct(data.id, data.payload),
    onSuccess: () => {
      notify.success('Ðã c?p nh?t s?n ph?m thành công');
      queryClient.invalidateQueries({ queryKey: ['products'] });
    },
    onError: (err: any) => {
      notify.error(err.message || 'Không th? c?p nh?t s?n ph?m');
    }
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => ApiService.Catalog.deleteProduct(id),
    onSuccess: () => {
      notify.success('Ðã xóa s?n ph?m thành công');
      queryClient.invalidateQueries({ queryKey: ['products'] });
    },
    onError: (err: any) => {
      notify.error(err.message || 'Không th? xóa s?n ph?m');
    }
  });

  return {
    query,
    createMutation,
    updateMutation,
    deleteMutation
  };
};
