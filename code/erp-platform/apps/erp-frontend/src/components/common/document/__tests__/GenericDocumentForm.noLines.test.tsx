import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { GenericDocumentForm, type GenericDocumentFormProps } from '../GenericDocumentForm';

const makeProps = (over: Partial<GenericDocumentFormProps> = {}): GenericDocumentFormProps => ({
  mode: 'ADD',
  docTitle: 'Phiếu thu',
  docCode: 'AUTO-GENERATE',
  info: [],
  createdDate: '2026-10-18',
  testIdPrefix: 'receipt',
  partner: {
    label: 'Khách hàng',
    displayName: 'Nguyễn Văn A',
    renderCombobox: () => <input data-testid="partner-combo" />,
    fields: [],
  },
  summary: [],
  ...over,
});

describe('GenericDocumentForm — phiếu không có bảng hàng (phiếu thu)', () => {
  it('U-GDF-N01: không truyền lines thì không vẽ bảng hàng và hiển thị children', () => {
    render(
      <GenericDocumentForm {...makeProps()}>
        <div data-testid="extra">Nội dung phụ</div>
      </GenericDocumentForm>,
    );
    expect(screen.getByTestId('extra')).toBeInTheDocument();
    expect(screen.queryByRole('table')).toBeNull();
    expect(screen.queryByTestId('receipt-add-line')).toBeNull();
  });

  it('U-GDF-N02: nút phụ trong khối tổng tiền gọi onClick và bị khóa khi disabled', () => {
    const onClick = vi.fn();
    const { rerender } = render(
      <GenericDocumentForm
        {...makeProps({
          summary: [{ key: 'amount', label: 'Số tiền thu', value: 0, onChange: vi.fn(), action: { label: 'Thu hết', onClick, testId: 'pay-all' } }],
        })}
      />,
    );
    fireEvent.click(screen.getByTestId('pay-all'));
    expect(onClick).toHaveBeenCalledTimes(1);

    rerender(
      <GenericDocumentForm
        {...makeProps({
          summary: [{ key: 'amount', label: 'Số tiền thu', value: 0, onChange: vi.fn(), action: { label: 'Thu hết', onClick, disabled: true, testId: 'pay-all' } }],
        })}
      />,
    );
    expect(screen.getByTestId('pay-all')).toBeDisabled();
  });

  it('U-GDF-N03: chế độ VIEW không hiện nút phụ; nhãn và giới hạn ngày được áp dụng', () => {
    const { rerender } = render(
      <GenericDocumentForm
        {...makeProps({
          mode: 'VIEW',
          summary: [{ key: 'amount', label: 'Số tiền thu', value: 5, onChange: vi.fn(), action: { label: 'Thu hết', onClick: vi.fn(), testId: 'pay-all' } }],
        })}
      />,
    );
    expect(screen.queryByTestId('pay-all')).toBeNull();

    rerender(<GenericDocumentForm {...makeProps({ createdDateLabel: 'Ngày thu', createdDateMax: '2026-10-18', onCreatedDateChange: vi.fn() })} />);
    expect(screen.getByText('Ngày thu')).toBeInTheDocument();
    expect(screen.getByTestId('doc-date')).toHaveAttribute('max', '2026-10-18');
  });
});
