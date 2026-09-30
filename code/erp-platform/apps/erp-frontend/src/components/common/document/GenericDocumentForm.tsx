import React, { useEffect, useRef, useState } from 'react';
import { Plus, Search, Trash2 } from 'lucide-react';
import type {
  DocStatus,
  DocumentLine,
  FormMode,
  InfoField,
  PartnerField,
  SummaryField,
} from '../../../types/documents';
import { NumberInput } from '../../../shared/components/Form/NumberInput';
import { StatusBadge } from '../../../shared/components/StatusBadge';
import { formatCurrency, formatDate, formatNumber } from '../../../shared/utils/format';
import { focusLineCell, lineErrorKey, QTY_FRACTION_DIGITS } from './documentLines';

export type OrderItem = DocumentLine;
export type { FormMode };

export interface GenericDocumentFormProps {
  mode: FormMode;
  docTitle: string;
  docCode: string;
  status?: DocStatus;
  error?: string | null;
  /** key: 'partner', lineErrorKey(i, 'product' | 'quantity') */
  errors?: Record<string, string>;

  info: InfoField[];
  createdDate: string;
  onCreatedDateChange?: (v: string) => void;

  partner: {
    label: string;
    required?: boolean;
    renderCombobox: (hasError: boolean) => React.ReactNode;
    displayName: string;
    onAdvancedSearch?: () => void;
    fields: PartnerField[];
  };

  summary: SummaryField[];

  lines: {
    testIdPrefix: 'sales' | 'inbound' | 'return';
    items: DocumentLine[];
    priceLabel?: string;
    onAdd: () => void;
    onRemove: (id: string) => void;
    onUpdate: (id: string, field: keyof DocumentLine, value: unknown) => void;
    renderProductCombobox: (line: DocumentLine, index: number, hasError: boolean) => React.ReactNode;
  };
}

const lineAmount = (l: DocumentLine) => (Number(l.quantity) || 0) * (Number(l.unitPrice) || 0);

export const GenericDocumentForm: React.FC<GenericDocumentFormProps> = ({
  mode,
  docTitle,
  docCode,
  status,
  error,
  errors = {},
  info,
  createdDate,
  onCreatedDateChange,
  partner,
  summary,
  lines,
}) => {
  const isView = mode === 'VIEW';
  const prefix = lines.testIdPrefix;
  const [activeLineId, setActiveLineId] = useState<string | null>(null);

  // Khi thêm dòng bằng nút "Dòng mới" hoặc Enter ở dòng cuối → focus ô sản phẩm của dòng mới.
  const focusNewLineRef = useRef(false);
  const prevCountRef = useRef(lines.items.length);
  useEffect(() => {
    const count = lines.items.length;
    if (count > prevCountRef.current && focusNewLineRef.current) {
      const last = lines.items[count - 1];
      setActiveLineId(last.id);
      focusLineCell(last.id, 'product');
    }
    focusNewLineRef.current = false;
    prevCountRef.current = count;
  }, [lines.items]);

  const addLineAndFocus = () => {
    focusNewLineRef.current = true;
    lines.onAdd();
  };

  const handlePriceEnter = (index: number) => {
    const next = lines.items[index + 1];
    if (next) focusLineCell(next.id, 'product');
    else addLineAndFocus();
  };

  const renderBadge = () => <StatusBadge status={status} />;

  const codeDisplay = !docCode || docCode === 'AUTO-GENERATE' ? <span className="text-ink-subtle">(Tự động)</span> : docCode;
  const totalQty = lines.items.reduce((s, l) => s + (Number(l.quantity) || 0), 0);
  const grandTotal = lines.items.reduce((s, l) => s + lineAmount(l), 0);
  const colCount = isView ? 7 : 8;

  return (
    <div className="flex flex-col h-full bg-surface">
      {/* 1. Thanh tiêu đề */}
      <div className="h-[40px] px-4 border-b border-line flex items-center justify-between shrink-0 bg-surface">
        <div className="flex items-center gap-3">
          <h1 className="text-[15px] font-semibold text-ink">{docTitle}</h1>
          {renderBadge()}
        </div>
        <div className="text-[15px] font-semibold text-primary" data-testid="doc-code">
          Số: {codeDisplay}
        </div>
      </div>

      {error && (
        <div
          data-testid={`${prefix}-error-msg`}
          role="alert"
          className="px-4 py-2 bg-danger-soft text-danger text-[13px] border-b border-danger/20 font-medium"
        >
          {error}
        </div>
      )}

      {/* 2. Header 3 khối — tỷ lệ 1 : 1.5 : 1 */}
      <div className="grid grid-cols-[1fr_1.5fr_1fr] gap-x-6 p-3 bg-surface border-b border-line shrink-0">
        {/* Khối trái: thông tin chung (chỉ đọc) */}
        <div className="flex flex-col gap-1.5 min-w-0">
          <div className="grid grid-cols-[96px_minmax(0,1fr)] gap-x-2 items-center h-7">
            <label className="erp-label text-right">Ngày</label>
            {isView || !onCreatedDateChange ? (
              <div className="text-[13px] text-ink truncate px-2" data-testid="doc-date">
                {formatDate(createdDate)}
              </div>
            ) : (
              <input
                type="date"
                className="erp-input h-7"
                value={createdDate}
                onChange={(e) => onCreatedDateChange(e.target.value)}
                data-testid="doc-date"
              />
            )}
          </div>
          {info.map((f) => (
            <div key={f.key} className="grid grid-cols-[96px_minmax(0,1fr)] gap-x-2 items-center h-7">
              <label className="erp-label text-right">{f.label}</label>
              <div className="text-[13px] text-ink truncate px-2" data-testid={f.testId}>
                {f.value}
              </div>
            </div>
          ))}
        </div>

        {/* Khối giữa: đối tác */}
        <div className="flex flex-col gap-1.5 min-w-0">
          <div className="grid grid-cols-[96px_minmax(0,1fr)] gap-x-2 items-center h-7">
            <label className="erp-label text-right">
              {partner.label} {partner.required && !isView && <span className="text-danger">*</span>}
            </label>
            {isView ? (
              <div className="text-[13px] text-ink truncate px-2 font-medium" data-testid={`${prefix}-partner-name`}>
                {partner.displayName}
              </div>
            ) : (
              <div className="flex gap-1 h-7 min-w-0">
                <div className="flex-1 min-w-0">{partner.renderCombobox(!!errors.partner)}</div>
                {partner.onAdvancedSearch && (
                  <button
                    type="button"
                    onClick={partner.onAdvancedSearch}
                    aria-label="Tìm kiếm nâng cao"
                    title="Tìm kiếm nâng cao"
                    data-testid={`${prefix}-partner-advanced`}
                    className="btn btn-secondary w-7 px-0 shrink-0 text-ink-muted"
                  >
                    <Search size={14} />
                  </button>
                )}
              </div>
            )}
          </div>
          {errors.partner && !isView && (
            <div className="grid grid-cols-[96px_minmax(0,1fr)] gap-x-2 -mt-1">
              <span />
              <span className="text-[12px] text-danger px-1">{errors.partner}</span>
            </div>
          )}
          {partner.fields.map((f) => (
            <div key={f.key} className="grid grid-cols-[96px_minmax(0,1fr)] gap-x-2 items-center h-7">
              <label className="erp-label text-right">
                {f.label} {f.required && !isView && <span className="text-danger">*</span>}
              </label>
              {isView || !f.onChange ? (
                <div className="text-[13px] text-ink truncate px-2" data-testid={f.testId}>
                  {f.value}
                </div>
              ) : (
                <input
                  type="text"
                  className="erp-input h-7"
                  value={f.value}
                  onChange={(e) => f.onChange!(e.target.value)}
                  placeholder={f.placeholder}
                  data-testid={f.testId}
                />
              )}
            </div>
          ))}
        </div>

        {/* Khối phải: tổng tiền */}
        <div className="flex flex-col gap-1.5 min-w-0">
          {summary.map((f) => {
            let toneClass = 'text-ink';
            if (f.tone === 'primary') toneClass = 'text-primary';
            else if (f.tone === 'danger' && f.value > 0) toneClass = 'text-danger';

            return (
              <div key={f.key} className="grid grid-cols-[96px_minmax(0,1fr)] gap-x-2 items-center h-7">
                <label className={`erp-label text-right ${f.strong ? 'font-semibold text-ink' : ''}`}>{f.label}</label>
                {isView || !f.onChange ? (
                  <div
                    className={`truncate px-2 text-right tabular-nums ${toneClass} ${f.strong ? 'text-[15px] font-semibold' : 'text-[13px]'}`}
                    data-testid={f.testId}
                  >
                    {f.isQuantity ? formatNumber(f.value, QTY_FRACTION_DIGITS) : formatCurrency(f.value)}
                  </div>
                ) : (
                  <NumberInput
                    value={f.value}
                    onChange={f.onChange}
                    className="h-7"
                    allowNegative={!!f.allowNegative}
                    data-testid={f.testId}
                    aria-label={f.label}
                  />
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* 3. Bảng hàng hóa — một <table> duy nhất để thead/tbody/tfoot thẳng cột */}
      <div className="flex-1 overflow-auto bg-slate-50 min-h-0">
        <table className="w-full min-w-[900px] table-fixed text-[13px] border-collapse">
          <colgroup>
            <col style={{ width: 44 }} />
            <col style={{ width: 110 }} />
            <col />
            <col style={{ width: 70 }} />
            <col style={{ width: 96 }} />
            <col style={{ width: 128 }} />
            <col style={{ width: 140 }} />
            {!isView && <col style={{ width: 36 }} />}
          </colgroup>
          <thead className="sticky top-0 z-20 bg-slate-100">
            <tr className="h-8 text-[12px] text-ink-muted">
              <th className="font-semibold px-2 border-b border-r border-line text-center">STT</th>
              <th className="font-semibold px-2 border-b border-r border-line text-left">Mã hàng</th>
              <th className="font-semibold px-2 border-b border-r border-line text-left">Tên hàng</th>
              <th className="font-semibold px-2 border-b border-r border-line text-center">ĐVT</th>
              <th className="font-semibold px-2 border-b border-r border-line text-right">Số lượng</th>
              <th className="font-semibold px-2 border-b border-r border-line text-right">{lines.priceLabel || 'Đơn giá'}</th>
              <th className="font-semibold px-2 border-b border-r border-line text-right">Thành tiền</th>
              {!isView && <th className="border-b border-line" aria-label="Xóa dòng" />}
            </tr>
          </thead>
          <tbody className="bg-surface">
            {lines.items.map((item, index) => {
              const productError = !!errors[lineErrorKey(index, 'product')];
              const qtyError = !!errors[lineErrorKey(index, 'quantity')];
              const isSelected = activeLineId === item.id;
              return (
                <tr
                  key={item.id}
                  data-testid={`${prefix}-line-row`}
                  aria-selected={isSelected}
                  onFocusCapture={() => setActiveLineId(item.id)}
                  onMouseDown={() => setActiveLineId(item.id)}
                  className={`h-7 border-b border-line ${
                    isSelected ? 'bg-primary-soft shadow-[inset_3px_0_0_var(--color-primary)]' : 'hover:bg-slate-50'
                  }`}
                >
                  <td className="px-2 text-center text-ink-subtle border-r border-line">{index + 1}</td>
                  <td className="px-2 border-r border-line truncate" data-testid={`${prefix}-line-code`}>
                    {item.productCode}
                  </td>
                  <td
                    className={`p-0 border-r border-line relative ${productError ? 'ring-1 ring-inset ring-danger bg-danger-soft' : ''}`}
                  >
                    {isView ? (
                      <div className="px-2 truncate">{item.productName}</div>
                    ) : (
                      <div className="absolute inset-0" data-line-id={item.id} data-field="product">
                        {lines.renderProductCombobox(item, index, productError)}
                      </div>
                    )}
                  </td>
                  <td className="px-2 border-r border-line text-center truncate">{item.unitOfMeasure}</td>
                  <td className={`p-0 border-r border-line relative ${qtyError ? 'ring-1 ring-inset ring-danger bg-danger-soft' : ''}`}>
                    {isView ? (
                      <div className="px-2 text-right tabular-nums">{formatNumber(item.quantity, QTY_FRACTION_DIGITS)}</div>
                    ) : (
                      <div className="absolute inset-0">
                        <NumberInput
                          value={item.quantity}
                          onChange={(v) => lines.onUpdate(item.id, 'quantity', v)}
                          variant="cell"
                          maxFractionDigits={QTY_FRACTION_DIGITS}
                          data-testid={`${prefix}-line-quantity`}
                          aria-label={`Số lượng dòng ${index + 1}`}
                          dataAttrs={{ 'data-line-id': item.id, 'data-field': 'quantity' }}
                          onKeyDown={(e) => {
                            if (e.key === 'Enter') {
                              e.preventDefault();
                              focusLineCell(item.id, 'price');
                            }
                          }}
                        />
                      </div>
                    )}
                  </td>
                  <td className="p-0 border-r border-line relative">
                    {isView ? (
                      <div className="px-2 text-right tabular-nums">{formatNumber(item.unitPrice)}</div>
                    ) : (
                      <div className="absolute inset-0">
                        <NumberInput
                          value={item.unitPrice}
                          onChange={(v) => lines.onUpdate(item.id, 'unitPrice', v)}
                          variant="cell"
                          data-testid={`${prefix}-line-price`}
                          aria-label={`${lines.priceLabel || 'Đơn giá'} dòng ${index + 1}`}
                          dataAttrs={{ 'data-line-id': item.id, 'data-field': 'price' }}
                          onKeyDown={(e) => {
                            if (e.key === 'Enter') {
                              e.preventDefault();
                              handlePriceEnter(index);
                            }
                          }}
                        />
                      </div>
                    )}
                  </td>
                  <td
                    className="px-2 border-r border-line text-right tabular-nums font-medium text-ink"
                    data-testid={`${prefix}-line-total`}
                  >
                    {formatNumber(lineAmount(item))}
                  </td>
                  {!isView && (
                    <td className="p-0 text-center">
                      <button
                        type="button"
                        onClick={() => lines.onRemove(item.id)}
                        aria-label={`Xóa dòng ${index + 1}`}
                        className="w-full h-7 flex items-center justify-center text-ink-subtle hover:text-danger hover:bg-danger-soft transition-colors"
                        data-testid={`${prefix}-line-delete`}
                      >
                        <Trash2 size={14} />
                      </button>
                    </td>
                  )}
                </tr>
              );
            })}
            {lines.items.length === 0 && (
              <tr>
                <td colSpan={colCount} className="px-2 py-4 text-center text-ink-subtle border-b border-line">
                  {isView ? 'Phiếu chưa có hàng hóa.' : 'Chưa có hàng hóa. Bấm “Dòng mới” để thêm.'}
                </td>
              </tr>
            )}
            {!isView && (
              <tr className="h-8 bg-surface border-b border-line">
                <td colSpan={colCount} className="px-1">
                  <button
                    type="button"
                    onClick={addLineAndFocus}
                    className="btn btn-ghost h-7 text-primary"
                    data-testid={`${prefix}-add-line`}
                  >
                    <Plus size={14} /> Dòng mới
                  </button>
                </td>
              </tr>
            )}
          </tbody>
          <tfoot className="sticky bottom-0 z-20 bg-slate-100 font-semibold text-ink">
            <tr className="h-8 border-t-2 border-line-strong">
              <td colSpan={4} className="px-2 text-right border-r border-line uppercase text-[12px]">
                Tổng cộng
              </td>
              <td className="px-2 text-right tabular-nums border-r border-line" data-testid={`${prefix}-total-qty`}>
                {formatNumber(totalQty, QTY_FRACTION_DIGITS)}
              </td>
              <td className="border-r border-line" />
              <td
                className="px-2 text-right tabular-nums text-primary text-[14px] border-r border-line"
                data-testid={`${prefix}-grand-total`}
              >
                {formatNumber(grandTotal)}
              </td>
              {!isView && <td />}
            </tr>
          </tfoot>
        </table>
      </div>
    </div>
  );
};
