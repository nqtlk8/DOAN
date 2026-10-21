import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { GoodsReturnForm, type GoodsReturnFormRef } from '../GoodsReturnForm';
import { InboundReceiptForm, type InboundReceiptFormRef } from '../InboundReceiptForm';
import { SalesOrderForm, type SalesOrderFormRef } from '../../sales/SalesOrderForm';
import { ApiError } from '../../../shared/errors/ApiError';
import type { GoodsReturnResponse, InboundReceiptDto, SalesInvoiceDto } from '../../../types/documents';

const api = vi.hoisted(() => ({
  gr: { create: vi.fn(), update: vi.fn(), confirm: vi.fn(), getById: vi.fn() },
  ib: { create: vi.fn(), update: vi.fn(), confirm: vi.fn(), getById: vi.fn() },
  si: { create: vi.fn(), update: vi.fn(), confirm: vi.fn(), getById: vi.fn() },
  getBalance: vi.fn(),
}));

vi.mock('../../../api/ApiService', () => ({
  ApiService: {
    GoodsReturn: api.gr,
    InboundReceipt: api.ib,
    SalesInvoice: api.si,
    Debt: { getBalance: api.getBalance },
    CustomerPrice: { resolve: vi.fn().mockResolvedValue([]) },
    Catalog: {
      searchCustomers: vi.fn().mockResolvedValue([]),
      searchProducts: vi.fn().mockResolvedValue([]),
      getProducts: vi.fn().mockResolvedValue([{ id: 7, code: 'SP07', name: 'Ốc vít' }]),
      getSuppliers: vi.fn().mockResolvedValue([{ id: 's-1', code: 'NCC1', name: 'Công ty A' }]),
    },
  },
}));
vi.mock('../../../context/AuthContext', () => ({ useAuth: () => ({ user: { username: 'staff', role: 'STAFF' } }) }));
vi.mock('../../../context/TabContext', () => ({ useTabs: () => ({ closeTab: vi.fn(), activeTabId: 't' }) }));

const wrap = (ui: React.ReactElement) => {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
};

const draftReturn = (over: Partial<GoodsReturnResponse> = {}): GoodsReturnResponse => ({
  id: 'gr-1',
  returnCode: 'TH0001',
  customerId: 'c-1',
  customerName: 'Nguyễn Văn A',
  status: 'DRAFT',
  reason: 'Lỗi',
  note: 'Ghi chú cũ',
  invoiceId: 'inv-1',
  lines: [{ productId: 7, productCode: 'SP07', productName: 'Ốc vít', quantity: 2, unitPrice: 1000, unitOfMeasure: 'CAI' }],
  ...over,
});

describe('Sửa phiếu nháp — phiếu trả hàng', () => {
  beforeEach(() => vi.clearAllMocks());

  it('nháp: Sửa -> chế độ EDIT, Lưu gọi update (không tạo phiếu mới), rồi Xác nhận như bình thường', async () => {
    api.gr.update.mockResolvedValue(undefined);
    api.gr.getById.mockResolvedValue(draftReturn({ reason: 'Hàng móp' }));
    api.gr.confirm.mockResolvedValue(undefined);
    const onStateChange = vi.fn();
    const ref = React.createRef<GoodsReturnFormRef>();
    wrap(<GoodsReturnForm ref={ref} mode="VIEW" initialData={draftReturn()} onStateChange={onStateChange} />);

    expect(screen.queryByTestId('doc-editing-badge')).toBeNull();
    act(() => ref.current!.handleEdit());
    await waitFor(() => expect(onStateChange).toHaveBeenLastCalledWith('EDIT', false, 'DRAFT'));
    expect(screen.getByTestId('doc-editing-badge')).toBeInTheDocument();

    fireEvent.change(screen.getByTestId('return-reason'), { target: { value: 'Hàng móp' } });
    await act(async () => {
      await ref.current!.handleSubmit();
    });

    expect(api.gr.create).not.toHaveBeenCalled();
    expect(api.gr.update).toHaveBeenCalledTimes(1);
    const [id, payload, editSession] = api.gr.update.mock.calls[0];
    expect(id).toBe('gr-1');
    expect(payload).toMatchObject({
      customerId: 'c-1',
      // Giữ ghi chú và hóa đơn gốc đã lưu: sửa nháp không được làm mất dữ liệu không hiển thị.
      invoiceId: 'inv-1',
      note: 'Ghi chú cũ',
      reason: 'Hàng móp',
      lines: [{ productId: 7, quantity: 2, unitPrice: 1000, unitOfMeasure: 'CAI' }],
    });
    expect(typeof editSession).toBe('string');
    await waitFor(() => expect(onStateChange).toHaveBeenLastCalledWith('VIEW', false, 'DRAFT'));

    await act(async () => {
      await ref.current!.handleConfirm();
    });
    expect(api.gr.confirm).toHaveBeenCalledWith('gr-1');
    await waitFor(() => expect(onStateChange).toHaveBeenLastCalledWith('VIEW', false, 'CONFIRMED'));
  });

  it('dòng phiếu trả chỉ có productId -> hiển thị mã / tên sản phẩm tra từ danh mục', async () => {
    const data = draftReturn({
      lines: [{ productId: 7, quantity: 1, unitPrice: 1000, unitOfMeasure: 'CAI' }],
    });
    wrap(<GoodsReturnForm mode="VIEW" initialData={data} />);
    expect(await screen.findByText('Ốc vít')).toBeInTheDocument();
    expect(screen.getByTestId('return-line-code')).toHaveTextContent('SP07');
  });

  it('phiếu đã xác nhận: Sửa không có tác dụng', async () => {
    const onStateChange = vi.fn();
    const ref = React.createRef<GoodsReturnFormRef>();
    wrap(<GoodsReturnForm ref={ref} mode="VIEW" initialData={draftReturn({ status: 'CONFIRMED' })} onStateChange={onStateChange} />);
    act(() => ref.current!.handleEdit());
    expect(screen.queryByTestId('doc-editing-badge')).toBeNull();
    expect(onStateChange).not.toHaveBeenCalledWith('EDIT', expect.anything(), expect.anything());
  });

  it('Hủy khi đang sửa -> trả về nội dung nháp đã lưu', async () => {
    const ref = React.createRef<GoodsReturnFormRef>();
    wrap(<GoodsReturnForm ref={ref} mode="VIEW" initialData={draftReturn()} />);
    act(() => ref.current!.handleEdit());
    fireEvent.change(screen.getByTestId('return-reason'), { target: { value: 'Đổi ý' } });
    act(() => ref.current!.handleCancel());
    fireEvent.click(await screen.findByRole('button', { name: 'Đồng ý' }));
    await waitFor(() => expect(screen.queryByTestId('doc-editing-badge')).toBeNull());
    expect(screen.getByTestId('return-reason')).toHaveTextContent('Lỗi');
    expect(api.gr.update).not.toHaveBeenCalled();
  });

  it('backend báo INVALID_STATUS (đã xác nhận ở nơi khác) -> bỏ thay đổi, hiện trạng thái mới', async () => {
    api.gr.update.mockRejectedValue(new ApiError({ message: 'x', status: 409, details: ['INVALID_STATUS'] }));
    api.gr.getById.mockResolvedValue(draftReturn({ status: 'CONFIRMED' }));
    const onStateChange = vi.fn();
    const ref = React.createRef<GoodsReturnFormRef>();
    wrap(<GoodsReturnForm ref={ref} mode="VIEW" initialData={draftReturn()} onStateChange={onStateChange} />);
    act(() => ref.current!.handleEdit());
    fireEvent.change(screen.getByTestId('return-reason'), { target: { value: 'Đổi ý' } });
    await act(async () => {
      await ref.current!.handleSubmit();
    });
    await waitFor(() => expect(onStateChange).toHaveBeenLastCalledWith('VIEW', false, 'CONFIRMED'));
    expect(screen.getByTestId('return-reason')).toHaveTextContent('Lỗi');
  });
});

describe('Sửa phiếu nháp — phiếu nhập hàng', () => {
  beforeEach(() => vi.clearAllMocks());

  it('Sửa -> Lưu gọi update với id phiếu', async () => {
    api.ib.update.mockResolvedValue(undefined);
    api.ib.getById.mockResolvedValue({ id: 'ib-1', receiptCode: 'PN0001', status: 'DRAFT' });
    const data = {
      id: 'ib-1',
      receiptCode: 'PN0001',
      supplierId: 's-1',
      status: 'DRAFT',
      note: '',
      lines: [{ productId: 7, quantity: 3, unitCost: 500, unitOfMeasure: 'CAI' }],
    } as unknown as InboundReceiptDto;
    const ref = React.createRef<InboundReceiptFormRef>();
    wrap(<InboundReceiptForm ref={ref} mode="VIEW" initialData={data} />);
    act(() => ref.current!.handleEdit());
    fireEvent.change(await screen.findByTestId('inbound-note'), { target: { value: 'Giao bổ sung' } });
    await act(async () => {
      await ref.current!.handleSubmit();
    });
    expect(api.ib.create).not.toHaveBeenCalled();
    expect(api.ib.update).toHaveBeenCalledWith(
      'ib-1',
      expect.objectContaining({
        supplierId: 's-1',
        note: 'Giao bổ sung',
        lines: [{ productId: 7, quantity: 3, unitCost: 500, unitOfMeasure: 'CAI' }],
      }),
      expect.any(String),
    );
  });
});

describe('Sửa phiếu nháp — phiếu bán hàng', () => {
  beforeEach(() => vi.clearAllMocks());

  it('Sửa -> Lưu gọi update, không tạo hóa đơn mới', async () => {
    api.getBalance.mockResolvedValue(0);
    api.si.update.mockResolvedValue(undefined);
    api.si.getById.mockResolvedValue({ id: 'si-1', invoiceCode: 'HD0001', status: 'DRAFT' });
    const data = {
      id: 'si-1',
      invoiceCode: 'HD0001',
      customerId: 'c-1',
      customerName: 'Nguyễn Văn A',
      status: 'DRAFT',
      note: '',
      advancePayment: 0,
      lines: [{ productId: 7, productName: 'Ốc vít', quantity: 1, unitPrice: 2000, unitOfMeasure: 'CAI' }],
    } as unknown as SalesInvoiceDto;
    const onStateChange = vi.fn();
    const ref = React.createRef<SalesOrderFormRef>();
    wrap(<SalesOrderForm ref={ref} mode="VIEW" initialData={data} onStateChange={onStateChange} />);
    act(() => ref.current!.handleEdit());
    fireEvent.change(await screen.findByTestId('sales-note'), { target: { value: 'Giao chiều' } });
    await act(async () => {
      await ref.current!.handleSubmit();
    });
    expect(api.si.create).not.toHaveBeenCalled();
    expect(api.si.update).toHaveBeenCalledWith(
      'si-1',
      expect.objectContaining({ customerId: 'c-1', note: 'Giao chiều' }),
      expect.any(String),
    );
    await waitFor(() => expect(onStateChange).toHaveBeenLastCalledWith('VIEW', false, 'DRAFT'));
  });
});
