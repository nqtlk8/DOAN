import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ApiService } from '../api/ApiService';
import { notify } from '../shared/notifications/notification';
import type { Supplier } from '../types/catalog';

export const useSuppliers = () => {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ['suppliers'],
    queryFn: () => ApiService.Catalog.getSuppliers(),
  });

  const createMutation = useMutation({
    mutationFn: (data: any) => ApiService.Catalog.createSupplier(data),
    onSuccess: () => {
      notify.success('Đã tạo nhà cung cấp thành công');
      queryClient.invalidateQueries({ queryKey: ['suppliers'] });
    }
  });

  const updateMutation = useMutation({
    mutationFn: (data: { id: string; payload: any }) => ApiService.Catalog.updateSupplier(data.id, data.payload),
    onSuccess: () => {
      notify.success('Đã cập nhật nhà cung cấp thành công');
      queryClient.invalidateQueries({ queryKey: ['suppliers'] });
    }
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => ApiService.Catalog.deleteSupplier(id),
    onSuccess: () => {
      notify.success('Đã xóa nhà cung cấp thành công');
      queryClient.invalidateQueries({ queryKey: ['suppliers'] });
    }
  });

  return {
    query,
    createMutation,
    updateMutation,
    deleteMutation
  };
};
