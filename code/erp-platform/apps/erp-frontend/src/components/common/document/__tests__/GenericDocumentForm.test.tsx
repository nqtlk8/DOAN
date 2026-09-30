import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { GenericDocumentForm, type GenericDocumentFormProps } from '../GenericDocumentForm';
import type { DocumentLine } from '../../../../types/documents';

const lineA: DocumentLine = { id: 'a', productId: '101', productCode: 'SP01', productName: 'Sản phẩm 1', unitOfMeasure: 'CAI', quantity: 2, unitPrice: 50000 };
const lineB: DocumentLine = { id: 'b', productId: '102', productCode: 'SP02', productName: 'Sản phẩm 2', unitOfMeasure: 'HOP', quantity: 1, unitPrice: 100000 };

const makeProps = (over: Partial<GenericDocumentFormProps> = {}): GenericDocumentFormProps => ({
  mode: 'ADD',
  docTitle: 'Phiếu bán hàng',
  docCode: 'AUTO-GENERATE',
  info: [],
  createdDate: '2026-09-27',
  partner: {
    label: 'Khách hàng',
    displayName: 'Nguyễn Văn A',
    renderCombobox: () => <input data-testid="partner-combo" />,
    fields: [],
  },
  summary: [],
  lines: {
    testIdPrefix: 'sales',
    items: [lineA, lineB],
    onAdd: vi.fn(),
    onRemove: vi.fn(),
    onUpdate: vi.fn(),
    renderProductCombobox: (line) => <input data-testid={`product-${line.id}`} defaultValue={line.productName} />,
  },
  ...over,
});

describe('GenericDocumentForm', () => {
  it('U-GDF-01: hiển thị nhãn đối tác, số dòng và tổng tiền đúng', () => {
    render(<GenericDocumentForm {...makeProps()} />);
    expect(screen.getByText('Khách hàng')).toBeInTheDocument();
    expect(screen.getAllByTestId('sales-line-row')).toHaveLength(2);
    expect(screen.getByTestId('sales-grand-total')).toHaveTextContent('200.000');
  });

  it('U-GDF-02: bấm "Dòng mới" gọi onAdd', () => {
    const props = makeProps();
    render(<GenericDocumentForm {...props} />);
    fireEvent.click(screen.getByTestId('sales-add-line'));
    expect(props.lines.onAdd).toHaveBeenCalledTimes(1);
  });

  it('U-GDF-03: chế độ VIEW không có nút thêm/xóa dòng và số lượng không kèm ₫', () => {
    render(<GenericDocumentForm {...makeProps({ mode: 'VIEW' })} />);
    expect(screen.queryByTestId('sales-add-line')).toBeNull();
    expect(screen.queryByTestId('sales-line-delete')).toBeNull();
    const firstRow = screen.getAllByTestId('sales-line-row')[0];
    expect(firstRow.textContent).not.toMatch(/2\s*₫/);
  });

  it('U-GDF-04: mã AUTO-GENERATE hiển thị "(Tự động)"', () => {
    render(<GenericDocumentForm {...makeProps()} />);
    expect(screen.getByTestId('doc-code')).toHaveTextContent('(Tự động)');
  });

  it('U-GDF-05: ô đơn giá sửa được và gọi onUpdate(unitPrice)', () => {
    const props = makeProps();
    render(<GenericDocumentForm {...props} />);
    const price = screen.getAllByTestId('sales-line-price')[0] as HTMLInputElement;
    expect(price.value).toBe('50.000');
    fireEvent.change(price, { target: { value: '40000' } });
    expect(props.lines.onUpdate).toHaveBeenCalledWith('a', 'unitPrice', 40000);
  });

  it('U-GDF-06: lỗi dòng (key line_i_quantity) tô đỏ ô số lượng', () => {
    render(<GenericDocumentForm {...makeProps({ errors: { line_1_quantity: 'Số lượng phải > 0' } })} />);
    const qtyCell = screen.getAllByTestId('sales-line-quantity')[1].closest('td')!;
    expect(qtyCell.className).toContain('ring-danger');
  });

  it('U-GDF-07: Enter ở ô số lượng chuyển sang ô đơn giá cùng dòng', async () => {
    render(<GenericDocumentForm {...makeProps()} />);
    const qty = screen.getAllByTestId('sales-line-quantity')[0];
    qty.focus();
    fireEvent.keyDown(qty, { key: 'Enter' });
    await waitFor(() => expect(document.activeElement).toBe(screen.getAllByTestId('sales-line-price')[0]));
  });

  it('U-GDF-08: Enter ở ô đơn giá dòng cuối thêm dòng mới', () => {
    const props = makeProps();
    render(<GenericDocumentForm {...props} />);
    const lastPrice = screen.getAllByTestId('sales-line-price')[1];
    fireEvent.keyDown(lastPrice, { key: 'Enter' });
    expect(props.lines.onAdd).toHaveBeenCalledTimes(1);
  });
});
