import os

content = """import React, { useRef } from 'react';
import { Plus, Trash2, Save, X, Printer, Check, Search, LogOut } from 'lucide-react';
import { FormMode, DocStatus, DocumentLine, InfoField, PartnerField, SummaryField } from '../../../types/documents';
import { NumberInput } from '../../../shared/components/Form/NumberInput';
import { formatCurrency, formatDate } from '../../../shared/utils/format';

export type OrderItem = DocumentLine;

export interface GenericDocumentFormProps {
  mode: FormMode;
  docTitle: string;
  docCode: string;
  status?: DocStatus;
  error?: string | null;
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

  onAdd?: () => void;
  onEdit?: () => void;
  onDelete?: () => void;
  onConfirm?: () => void;
  onPrint?: () => void;
  onSave?: () => void;
  onCancel?: () => void;
  onExit?: () => void;
  hideConfirm?: boolean;
  isLoading?: boolean;
}

export const GenericDocumentForm: React.FC<GenericDocumentFormProps> = ({
  mode, docTitle, docCode, status, error, errors = {},
  info, createdDate, onCreatedDateChange,
  partner, summary, lines,
  onAdd, onEdit, onDelete, onConfirm, onPrint, onSave, onCancel, onExit,
  hideConfirm, isLoading
}) => {
  const isView = mode === 'VIEW';
  
  const renderBadge = () => {
    if (!status) return null;
    if (status === 'DRAFT') return <span className="badge-warning px-2">Nháp</span>;
    if (status === 'CONFIRMED') return <span className="badge-success px-2">Ðã xác nh?n</span>;
    if (status === 'CANCELLED') return <span className="badge-danger px-2">Ðã h?y</span>;
    return null;
  };

  const codeDisplay = docCode === 'AUTO-GENERATE' ? <span className="text-ink-subtle">(T? d?ng)</span> : docCode;

  return (
    <div className="flex flex-col h-full bg-surface">
      {/* 1. Thanh tiêu d? */}
      <div className="h-[40px] px-4 border-b border-line flex items-center justify-between shrink-0 bg-surface">
        <div className="flex items-center gap-3">
          <h1 className="text-[15px] font-semibold text-ink">{docTitle}</h1>
          {renderBadge()}
        </div>
        <div className="text-[15px] font-semibold text-primary" data-testid="doc-code">
          S?: {codeDisplay}
        </div>
      </div>
      
      {error && (
        <div className="px-4 py-2 bg-danger-soft text-danger text-[13px] border-b border-danger/20 font-medium">
          {error}
        </div>
      )}

      {/* 2. Header 3 kh?i */}
      <div className="grid grid-cols-[1fr_1.5fr_1fr] gap-x-6 p-3 bg-surface border-b border-line shrink-0">
        
        {/* Kh?i trái: Thông tin chung */}
        <div className="flex flex-col gap-1.5">
          <div className="grid grid-cols-[96px_minmax(0,1fr)] gap-x-2 items-center h-7">
            <label className="erp-label text-right">Ngày</label>
            {isView || !onCreatedDateChange ? (
              <div className="text-[13px] text-ink truncate px-2">{formatDate(createdDate)}</div>
            ) : (
              <input 
                type="date" 
                className="erp-input h-7" 
                value={createdDate} 
                onChange={e => onCreatedDateChange(e.target.value)} 
              />
            )}
          </div>
          {info.map(f => (
            <div key={f.key} className="grid grid-cols-[96px_minmax(0,1fr)] gap-x-2 items-center h-7">
              <label className="erp-label text-right">{f.label}</label>
              <div className="text-[13px] text-ink truncate px-2" data-testid={f.testId}>{f.value}</div>
            </div>
          ))}
        </div>

        {/* Kh?i gi?a: Ð?i tác */}
        <div className="flex flex-col gap-1.5">
          <div className="grid grid-cols-[96px_minmax(0,1fr)] gap-x-2 items-center h-7">
            <label className="erp-label text-right">
              {partner.label} {partner.required && <span className="text-danger">*</span>}
            </label>
            {isView ? (
              <div className="text-[13px] text-ink truncate px-2 font-medium">{partner.displayName}</div>
            ) : (
              <div className="flex gap-1 h-7">
                <div className="flex-1 min-w-0">
                  {partner.renderCombobox(!!errors['partner'])}
                </div>
                {partner.onAdvancedSearch && (
                  <button 
                    type="button" 
                    onClick={partner.onAdvancedSearch}
                    className="w-7 h-7 flex items-center justify-center border border-line rounded bg-surface hover:bg-slate-50 shrink-0 text-ink-muted"
                  >
                    <Search size={14} />
                  </button>
                )}
              </div>
            )}
          </div>
          {partner.fields.map(f => (
            <div key={f.key} className="grid grid-cols-[96px_minmax(0,1fr)] gap-x-2 items-center h-7">
              <label className="erp-label text-right">
                {f.label} {f.required && !isView && <span className="text-danger">*</span>}
              </label>
              {isView || !f.onChange ? (
                <div className="text-[13px] text-ink truncate px-2" data-testid={f.testId}>{f.value}</div>
              ) : (
                <input 
                  type="text" 
                  className="erp-input h-7" 
                  value={f.value} 
                  onChange={e => f.onChange!(e.target.value)}
                  placeholder={f.placeholder}
                  data-testid={f.testId}
                />
              )}
            </div>
          ))}
        </div>

        {/* Kh?i ph?i: Summary */}
        <div className="flex flex-col gap-1.5">
          {summary.map(f => {
            let toneClass = 'text-ink';
            if (f.tone === 'primary') toneClass = 'text-primary';
            else if (f.tone === 'danger') toneClass = 'text-danger';

            return (
              <div key={f.key} className="grid grid-cols-[96px_minmax(0,1fr)] gap-x-2 items-center h-7">
                <label className={erp-label text-right }>{f.label}</label>
                {isView || !f.onChange ? (
                  <div 
                    className={	ext-[13px] truncate px-2 text-right tabular-nums  }
                    data-testid={f.testId}
                  >
                    {formatCurrency(f.value)}
                  </div>
                ) : (
                  <NumberInput
                    value={f.value}
                    onChange={f.onChange}
                    className="h-7"
                    allowNegative={true}
                    testId={f.testId}
                  />
                )}
              </div>
            );
          })}
        </div>
        
      </div>

      {/* 3. B?ng hàng hóa */}
      <div className="flex-1 overflow-auto bg-slate-50 min-h-0 relative">
        <table className="w-full text-[13px] border-collapse relative">
          <thead className="sticky top-0 z-20 bg-slate-100 border-b border-line shadow-sm">
            <tr className="h-8">
              <th className="font-semibold text-ink-muted px-2 border-r border-line w-12">STT</th>
              <th className="font-semibold text-ink-muted px-2 border-r border-line text-left">Mã hàng</th>
              <th className="font-semibold text-ink-muted px-2 border-r border-line text-left">Tên hàng</th>
              <th className="font-semibold text-ink-muted px-2 border-r border-line text-left w-[80px]">ÐVT</th>
              <th className="font-semibold text-ink-muted px-2 border-r border-line text-right w-[100px]">S? lu?ng</th>
              <th className="font-semibold text-ink-muted px-2 border-r border-line text-right w-[120px]">{lines.priceLabel || 'Ðon giá'}</th>
              <th className="font-semibold text-ink-muted px-2 border-r border-line text-right w-[120px]">Thành ti?n</th>
              {!isView && <th className="font-semibold text-ink-muted px-2 w-10"></th>}
            </tr>
          </thead>
          <tbody className="bg-surface">
            {lines.items.map((item, index) => {
              const qtyError = !!errors[line__quantity];
              return (
                <tr key={item.id} className="h-8 border-b border-line last:border-0 hover:bg-slate-50 transition-colors" data-testid={${lines.testIdPrefix}-line-row}>
                  <td className="px-2 text-center text-ink-subtle border-r border-line">{index + 1}</td>
                  <td className="px-2 border-r border-line">{item.productCode}</td>
                  <td className="p-0 border-r border-line relative">
                    {isView ? (
                      <div className="px-2 truncate">{item.productName}</div>
                    ) : (
                      <div className="absolute inset-0">
                        {lines.renderProductCombobox(item, index, !!errors[line__product])}
                      </div>
                    )}
                  </td>
                  <td className="px-2 border-r border-line truncate">{item.unitOfMeasure}</td>
                  <td className="p-0 border-r border-line relative">
                    {isView ? (
                      <div className="px-2 text-right tabular-nums">{formatCurrency(item.quantity)}</div>
                    ) : (
                      <div className="absolute inset-0">
                        <NumberInput
                          value={item.quantity}
                          onChange={(v) => lines.onUpdate(item.id, 'quantity', v)}
                          variant="cell"
                          testId={${lines.testIdPrefix}-line-quantity}
                          className={qtyError ? 'bg-danger-soft' : ''}
                        />
                      </div>
                    )}
                  </td>
                  <td className="px-2 border-r border-line text-right tabular-nums">{formatCurrency(item.unitPrice)}</td>
                  <td className="px-2 border-r border-line text-right tabular-nums font-medium text-primary" data-testid={${lines.testIdPrefix}-line-total}>
                    {formatCurrency(item.quantity * item.unitPrice)}
                  </td>
                  {!isView && (
                    <td className="p-0 text-center">
                      <button 
                        type="button" 
                        onClick={() => lines.onRemove(item.id)}
                        className="w-full h-full flex items-center justify-center text-danger hover:bg-danger-soft transition-colors"
                        data-testid={${lines.testIdPrefix}-line-delete}
                      >
                        <Trash2 size={14} />
                      </button>
                    </td>
                  )}
                </tr>
              );
            })}
            {!isView && (
              <tr className="h-8 bg-surface border-b border-line">
                <td colSpan={8} className="px-2">
                  <button 
                    type="button" 
                    onClick={lines.onAdd}
                    className="flex items-center gap-1 text-[13px] font-medium text-primary hover:underline"
                    data-testid={${lines.testIdPrefix}-add-line}
                  >
                    <Plus size={14} /> Dòng m?i
                  </button>
                </td>
              </tr>
            )}
          </tbody>
          <tfoot className="sticky bottom-0 z-20 bg-slate-100 border-t border-line font-semibold shadow-sm text-ink text-[13px]">
            <tr className="h-8">
              <td colSpan={6} className="px-2 text-right border-r border-line uppercase">T?ng c?ng</td>
              <td className="px-2 text-right text-primary tabular-nums" data-testid={${lines.testIdPrefix}-grand-total}>
                {formatCurrency(lines.items.reduce((s, i) => s + i.quantity * i.unitPrice, 0))}
              </td>
              {!isView && <td className=""></td>}
            </tr>
          </tfoot>
        </table>
      </div>

      {/* 4. Thanh nút du?i cùng */}
      <div className="h-[40px] px-4 border-t border-line bg-surface flex items-center justify-end gap-2 shrink-0">
        {isView && onAdd && (
          <button data-testid="btn-add" onClick={onAdd} className="btn btn-secondary flex items-center gap-1.5 px-3">
            <Plus size={14} /> Thêm m?i <kbd className="text-[11px] text-ink-subtle ml-1 font-sans">F2</kbd>
          </button>
        )}
        {isView && onEdit && status !== 'CANCELLED' && (
          <button data-testid="btn-edit" onClick={onEdit} className="btn btn-secondary flex items-center gap-1.5 px-3">
            S?a <kbd className="text-[11px] text-ink-subtle ml-1 font-sans">F3</kbd>
          </button>
        )}
        {isView && onDelete && status !== 'CANCELLED' && (
          <button data-testid="btn-delete" onClick={onDelete} className="btn btn-danger flex items-center gap-1.5 px-3">
            Xóa
          </button>
        )}
        {isView && onConfirm && !hideConfirm && status === 'DRAFT' && (
          <button data-testid="btn-confirm" onClick={onConfirm} disabled={isLoading} className="btn btn-primary flex items-center gap-1.5 px-3">
            {isLoading ? <Loader2 size={14} className="animate-spin" /> : <Check size={14} />} Xác nh?n
          </button>
        )}
        {isView && onPrint && status !== 'DRAFT' && (
          <button data-testid="btn-print" onClick={onPrint} className="btn btn-secondary flex items-center gap-1.5 px-3">
            <Printer size={14} /> In <kbd className="text-[11px] text-ink-subtle ml-1 font-sans">F7</kbd>
          </button>
        )}
        {!isView && onSave && (
          <button data-testid="btn-save" onClick={onSave} disabled={isLoading} className="btn btn-primary flex items-center gap-1.5 px-3">
            {isLoading ? <Loader2 size={14} className="animate-spin" /> : <Save size={14} />} Luu <kbd className="text-[11px] text-primary/70 ml-1 font-sans">F4</kbd>
          </button>
        )}
        {!isView && onCancel && (
          <button data-testid="btn-cancel" onClick={onCancel} className="btn btn-secondary flex items-center gap-1.5 px-3">
            <X size={14} /> H?y <kbd className="text-[11px] text-ink-subtle ml-1 font-sans">Esc</kbd>
          </button>
        )}
        {onExit && (
          <button data-testid="btn-exit" onClick={onExit} className="btn btn-secondary flex items-center gap-1.5 px-3">
            <LogOut size={14} /> Thoát <kbd className="text-[11px] text-ink-subtle ml-1 font-sans">F8</kbd>
          </button>
        )}
      </div>
    </div>
  );
};
"""

with open('src/components/common/document/GenericDocumentForm.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
