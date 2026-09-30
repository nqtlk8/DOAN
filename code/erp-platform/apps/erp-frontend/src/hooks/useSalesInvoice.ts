import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ApiService } from '../api/ApiService';
import { notify } from '../shared/notifications/notification';
import type { components } from '@erp/api-contract';

export const useSalesInvoice = () => {
  const queryClient = useQueryClient();

  // Form bán hàng chỉ cần mutation; danh sách hóa đơn do SalesList tự query khi mở tab "Danh sách phiếu".

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
    createMutation,
    confirmMutation
  };
};
