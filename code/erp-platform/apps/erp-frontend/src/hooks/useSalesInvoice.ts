import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ApiService } from '../api/ApiService';
import { notify } from '../shared/notifications/notification';
import type { components } from '@erp/api-contract';

export const useSalesInvoice = () => {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ['salesInvoices'],
    queryFn: () => ApiService.SalesInvoice.getAll(),
  });

  const getById = (id: string) => useQuery({
    queryKey: ['salesInvoices', id],
    queryFn: () => ApiService.SalesInvoice.getById(id),
    enabled: !!id,
  });

  const createMutation = useMutation({
    mutationFn: (data: components['schemas']['SalesInvoiceCreateDto']) => ApiService.SalesInvoice.create(data),
    onSuccess: () => {
      notify.success('Đã tạo đơn bán hàng thành công');
      queryClient.invalidateQueries({ queryKey: ['salesInvoices'] });
    }
  });

  const confirmMutation = useMutation({
    mutationFn: (id: string) => ApiService.SalesInvoice.confirm(id),
    onSuccess: () => {
      notify.success('Đã xác nhận đơn bán hàng thành công');
      queryClient.invalidateQueries({ queryKey: ['salesInvoices'] });
    }
  });

  return {
    query,
    getById,
    createMutation,
    confirmMutation
  };
};
