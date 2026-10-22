import React from 'react';
import { act, render, screen, waitFor, fireEvent } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CashReceiptForm, type CashReceiptFormRef } from '../CashReceiptForm';
import type { CashReceiptDto } from '../../../types/cashReceipt';
import { toInputDate } from '../../../shared/utils/format';
import { ApiError } from '../../../shared/errors/ApiError';

const api = vi.hoisted(() => ({
  getBalance: vi.fn(),
  create: vi.fn(),
  getById: vi.fn(),
  cancel: vi.fn(),
  searchCustomers: vi.fn(),
}));

vi.mock('../../../api/ApiService', () => ({
  ApiService: {
    Debt: { getBalance: api.getBalance },
    CashReceipt: { create: api.create, getById: api.getById, cancel: api.cancel },
    Catalog: { searchCustomers: api.searchCustomers },
  },
}));
vi.mock('../../../context/AuthContext', () => ({ useAuth: () => ({ user: { username: 'staff_tp1', role: 'STAFF' } }) }));
vi.mock('../../../context/TabContext', () => ({ useTabs: () => ({ closeTab: vi.fn(), activeTabId: 'cash-receipt' }) }));

const customer = { id: 'c-1', customerCode: 'KH001', name: 'Nguyễn Văn A', phone: '0901234567', address: '12 Lê Lợi' };

const receipt = (over: Partial<CashReceiptDto> = {}): CashReceiptDto => ({
  id: 'r-1',
  receiptCode: 'PT1A2B3C4D',
  branchId: 1,
  customerId: 'c-1',
  customerCode: 'KH001',
  customerName: 'Nguyễn Văn A',
  customerPhone: '0901234567',
  customerAddress: '12 Lê Lợi',
  receiptDate: '2026-10-18',
  amount: 4_000_000,
  debtBefore: 4_000_000,
  debtAfter: 0,
  note: null,
  status: 'POSTED',
  createdAt: '2026-10-18T09:00:00',
  ...over,
});

const renderForm = (props: React.ComponentProps<typeof CashReceiptForm>) => {
  const ref = React.createRef<CashReceiptFormRef>();
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={client}>
      <CashReceiptForm ref={ref} {...props} />
    </QueryClientProvider>,
  );
  return ref;
};

describe('CashReceiptForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.searchCustomers.mockResolvedValue([]);
  });

  it('U-PT-F10: chọn sẵn khách -> hiện thông tin và số nợ; "Thu hết" -> còn lại 0; Lưu gửi đúng dữ liệu', async () => {
    api.getBalance.mockResolvedValue(4_000_000);
    api.create.mockResolvedValue({ id: 'r-1', receiptCode: 'PT1A2B3C4D' });
    api.getById.mockResolvedValue(receipt());
    const onStateChange = vi.fn();
    const ref = renderForm({ initialCustomer: customer, onStateChange });

    await waitFor(() => expect(screen.getByTestId('receipt-debt')).toHaveTextContent('4.000.000'));
    expect(api.getBalance).toHaveBeenCalledWith('c-1');
    expect(screen.getByTestId('receipt-customer-code')).toHaveTextContent('KH001');
    expect(screen.getByTestId('receipt-customer-phone')).toHaveTextContent('0901234567');
    expect(screen.getByTestId('receipt-customer-address')).toHaveTextContent('12 Lê Lợi');

    fireEvent.click(screen.getByTestId('receipt-pay-all'));
    expect((screen.getByTestId('receipt-amount') as HTMLInputElement).value).toBe('4.000.000');
    expect(screen.getByTestId('receipt-remaining')).toHaveTextContent('0');

    await act(async () => {
      ref.current!.handleSubmit();
    });

    expect(api.create).toHaveBeenCalledWith(
      {
        customerId: 'c-1',
        amount: 4_000_000,
        receiptDate: toInputDate(new Date()),
        note: undefined,
      },
      expect.any(String), // Idempotency-Key của lần lập phiếu
    );
    await waitFor(() => expect(screen.getByTestId('doc-code')).toHaveTextContent('PT1A2B3C4D'));
    expect(screen.getByText('Đã ghi sổ')).toBeInTheDocument();
    expect(onStateChange).toHaveBeenLastCalledWith('VIEW', false, true);
  });

  it('U-PT-F11: số tiền vượt nợ -> báo lỗi, không gọi API lưu', async () => {
    api.getBalance.mockResolvedValue(1_000_000);
    const ref = renderForm({ initialCustomer: customer });
    await waitFor(() => expect(screen.getByTestId('receipt-debt')).toHaveTextContent('1.000.000'));

    fireEvent.change(screen.getByTestId('receipt-amount'), { target: { value: '2000000' } });
    await act(async () => {
      ref.current!.handleSubmit();
    });

    expect(api.create).not.toHaveBeenCalled();
    expect(screen.getByTestId('receipt-field-errors')).toHaveTextContent('vượt số nợ hiện tại');
  });

  it('U-PT-F12: backend từ chối (409) -> hiện lỗi và tải lại số nợ mới', async () => {
    api.getBalance.mockResolvedValueOnce(4_000_000).mockResolvedValueOnce(500_000);
    api.create.mockRejectedValue(new Error('Số tiền thu 4000000 vượt số nợ hiện tại 500000'));
    const ref = renderForm({ initialCustomer: customer });
    await waitFor(() => expect(screen.getByTestId('receipt-debt')).toHaveTextContent('4.000.000'));

    fireEvent.click(screen.getByTestId('receipt-pay-all'));
    await act(async () => {
      ref.current!.handleSubmit();
    });

    await waitFor(() => expect(screen.getByTestId('receipt-debt')).toHaveTextContent('500.000'));
    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.getByTestId('doc-code')).toHaveTextContent('(Tự động)');
  });

  it('U-PT-F15: mất kết nối khi lưu -> báo chưa rõ kết quả; Lưu lại gửi CÙNG key (không tạo phiếu trùng)', async () => {
    api.getBalance.mockResolvedValue(4_000_000);
    api.create
      .mockRejectedValueOnce(new ApiError({ message: 'Kết nối mạng quá hạn.' }))
      .mockResolvedValueOnce({ id: 'r-1', receiptCode: 'PT1A2B3C4D' });
    api.getById.mockResolvedValue(receipt());
    const ref = renderForm({ initialCustomer: customer });
    await waitFor(() => expect(screen.getByTestId('receipt-debt')).toHaveTextContent('4.000.000'));
    fireEvent.click(screen.getByTestId('receipt-pay-all'));

    await act(async () => {
      ref.current!.handleSubmit();
    });
    expect(screen.getByRole('alert')).toHaveTextContent('chưa rõ phiếu đã được lưu hay chưa');

    await act(async () => {
      ref.current!.handleSubmit();
    });
    await waitFor(() => expect(screen.getByTestId('doc-code')).toHaveTextContent('PT1A2B3C4D'));

    expect(api.create).toHaveBeenCalledTimes(2);
    const firstKey = api.create.mock.calls[0][1];
    expect(typeof firstKey).toBe('string');
    expect(api.create.mock.calls[1][1]).toBe(firstKey);
  });

  it('U-PT-F13: xem phiếu đã ghi sổ -> hủy phiếu kèm lý do', async () => {
    api.cancel.mockResolvedValue(receipt({ status: 'CANCELLED', cancelledAt: '2026-10-18T10:00:00', cancelReason: 'Nhập sai' }));
    const onStateChange = vi.fn();
    const ref = renderForm({ initialData: receipt(), onStateChange });

    expect(onStateChange).toHaveBeenLastCalledWith('VIEW', false, true);
    expect(screen.getByTestId('receipt-amount')).toHaveTextContent('4.000.000');
    act(() => ref.current!.handleCancelReceipt());
    fireEvent.change(screen.getByTestId('receipt-cancel-reason'), { target: { value: 'Nhập sai' } });
    await act(async () => {
      fireEvent.click(screen.getByTestId('receipt-cancel-confirm'));
    });

    expect(api.cancel).toHaveBeenCalledWith('r-1', 'Nhập sai');
    await waitFor(() => expect(screen.getByTestId('receipt-cancelled-info')).toHaveTextContent('Lý do: Nhập sai'));
    expect(screen.getByText('Đã hủy')).toBeInTheDocument();
    expect(onStateChange).toHaveBeenLastCalledWith('VIEW', false, false);
  });

  it('U-PT-F14: phiếu đã hủy không cho hủy lại', () => {
    const onStateChange = vi.fn();
    const ref = renderForm({ initialData: receipt({ status: 'CANCELLED' }), onStateChange });
    expect(onStateChange).toHaveBeenLastCalledWith('VIEW', false, false);
    act(() => ref.current!.handleCancelReceipt());
    expect(screen.queryByTestId('receipt-cancel-reason')).toBeNull();
  });
});
