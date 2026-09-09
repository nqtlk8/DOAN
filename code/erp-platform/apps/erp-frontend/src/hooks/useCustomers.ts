import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ApiService } from '../api/ApiService';
import { notify } from '../shared/notifications/notification';
import type { Customer } from '../types/catalog';

export const useCustomers = () => {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ['customers'],
    queryFn: () => ApiService.Catalog.getCustomers(),
  });

  const createMutation = useMutation({
    mutationFn: (data: any) => ApiService.Catalog.createCustomer(data),
    onSuccess: () => {
      notify.success('Đã tạo khách hàng thành công');
      queryClient.invalidateQueries({ queryKey: ['customers'] });
    },
    onError: (err: any) => {
      notify.error(err.message || 'Không thể tạo khách hàng');
    }
  });

  const updateMutation = useMutation({
    mutationFn: (data: { id: string; payload: any }) => ApiService.Catalog.updateCustomer(data.id, data.payload),
    onSuccess: () => {
      notify.success('Đã cập nhật khách hàng thành công');
      queryClient.invalidateQueries({ queryKey: ['customers'] });
    },
    onError: (err: any) => {
      notify.error(err.message || 'Không thể cập nhật khách hàng');
    }
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => ApiService.Catalog.deleteCustomer(id),
    onSuccess: () => {
      notify.success('Đã xóa khách hàng thành công');
      queryClient.invalidateQueries({ queryKey: ['customers'] });
    },
    onError: (err: any) => {
      notify.error(err.message || 'Không thể xóa khách hàng');
    }
  });

  return {
    customers: (query.data || []) as Customer[],
    isLoading: query.isLoading,
    isError: query.isError,
    error: query.error,
    refetch: query.refetch,
    createCustomer: createMutation.mutate,
    isCreating: createMutation.isPending,
    updateCustomer: updateMutation.mutate,
    isUpdating: updateMutation.isPending,
    deleteCustomer: deleteMutation.mutate,
    isDeleting: deleteMutation.isPending
  };
};
