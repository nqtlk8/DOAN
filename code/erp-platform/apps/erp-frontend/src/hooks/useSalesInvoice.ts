import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ApiService } from '../api/ApiService';
import { submitWithKey } from '../shared/idempotency/submitKey';
import type { components } from '@erp/api-contract';

export interface CreateSalesInvoiceVariables {
  payload: components['schemas']['SalesInvoiceCreateDto'];
  /** Key của lần lập phiếu (useSubmitKey): mọi lần bấm Lưu của cùng phiếu gửi cùng key. */
  idempotencyKey: string;
}

export interface UpdateSalesInvoiceVariables {
  id: string;
  payload: components['schemas']['SalesInvoiceCreateDto'];
  /** Key của lần bấm "Sửa": Lưu lại sau lỗi mạng gửi cùng key. */
  editSession: string;
}

/**
 * Hóa đơn bán hai bước (giống phiếu nhập, phiếu trả):
 * 1. createDraftMutation — lưu nháp, chưa trừ kho / ghi công nợ;
 * 2. confirmMutation — xác nhận: trừ kho FIFO, ghi công nợ, lưu giá riêng của khách.
 * Thông báo thành công / lỗi do form hiển thị (form biết trạng thái phiếu).
 */
export const useSalesInvoice = () => {
  const queryClient = useQueryClient();

  const createDraftMutation = useMutation({
    mutationFn: ({ payload, idempotencyKey }: CreateSalesInvoiceVariables) =>
      submitWithKey(() => ApiService.SalesInvoice.create(payload, idempotencyKey)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['salesInvoices'] });
    },
  });

  /** Sửa hóa đơn nháp (PUT). Chỉ đổi nội dung nháp, chưa tác động kho / công nợ. */
  const updateDraftMutation = useMutation({
    mutationFn: ({ id, payload, editSession }: UpdateSalesInvoiceVariables) =>
      submitWithKey(() => ApiService.SalesInvoice.update(id, payload, editSession)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['salesInvoices'] });
    },
  });

  const confirmMutation = useMutation({
    mutationFn: (invoiceId: string) => submitWithKey(() => ApiService.SalesInvoice.confirm(invoiceId)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['salesInvoices'] });
      queryClient.invalidateQueries({ queryKey: ['stocks'] });
      queryClient.invalidateQueries({ queryKey: ['debts'] });
      queryClient.invalidateQueries({ queryKey: ['debt-movements'] });
    },
  });

  return {
    createDraftMutation,
    updateDraftMutation,
    confirmMutation,
  };
};
