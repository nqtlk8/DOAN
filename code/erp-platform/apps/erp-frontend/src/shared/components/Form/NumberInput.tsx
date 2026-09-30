import React, { useState } from 'react';
import { formatDraftNumber, formatNumber, parseNumber } from '../../utils/format';

export interface NumberInputProps {
  value: number;
  onChange: (v: number) => void;
  disabled?: boolean;
  className?: string;
  allowNegative?: boolean;
  /** Số chữ số thập phân cho phép (0 = chỉ số nguyên). */
  maxFractionDigits?: number;
  min?: number;
  'data-testid'?: string;
  onKeyDown?: (e: React.KeyboardEvent<HTMLInputElement>) => void;
  onFocus?: (e: React.FocusEvent<HTMLInputElement>) => void;
  inputRef?: React.Ref<HTMLInputElement>;
  variant?: 'field' | 'cell';
  /** Thuộc tính data-* bổ sung (vd data-line-id, data-field cho luồng bàn phím). */
  dataAttrs?: Record<`data-${string}`, string>;
  'aria-label'?: string;
}

/**
 * Ô nhập số kiểu Việt Nam: hiển thị "1.500.000" cả khi đang gõ, cho phép "," thập phân
 * (khi maxFractionDigits > 0) và dấu "-" (khi allowNegative). Tự bôi đen khi focus.
 */
export const NumberInput: React.FC<NumberInputProps> = ({
  value,
  onChange,
  disabled,
  className = '',
  allowNegative = false,
  maxFractionDigits = 0,
  min,
  'data-testid': testId,
  onKeyDown,
  onFocus,
  inputRef,
  variant = 'field',
  dataAttrs,
  'aria-label': ariaLabel,
}) => {
  // Chuỗi đang gõ (chỉ tồn tại khi ô đang được focus) để không mất "1," hoặc "-".
  const [draft, setDraft] = useState<string | null>(null);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const nextDraft = formatDraftNumber(e.target.value, maxFractionDigits, allowNegative);
    let parsed = parseNumber(nextDraft);
    if (!allowNegative && parsed < 0) parsed = 0;
    if (min !== undefined && parsed < min) parsed = min;
    setDraft(nextDraft);
    onChange(parsed);
  };

  const handleFocus = (e: React.FocusEvent<HTMLInputElement>) => {
    setDraft(formatNumber(value, maxFractionDigits));
    e.target.select();
    onFocus?.(e);
  };

  const defaultClasses =
    variant === 'cell'
      ? 'w-full h-full border-0 bg-transparent px-2 text-right tabular-nums focus:bg-surface focus:ring-2 focus:ring-inset focus:ring-primary/30 outline-none'
      : 'erp-input text-right tabular-nums';

  return (
    <input
      ref={inputRef}
      type="text"
      inputMode={maxFractionDigits > 0 ? 'decimal' : 'numeric'}
      value={
        // Giá trị bị đổi từ bên ngoài khi đang gõ (vd chọn sản phẩm khác) → hiển thị giá trị mới.
        draft !== null && parseNumber(draft) === value ? draft : formatNumber(value, maxFractionDigits)
      }
      onChange={handleChange}
      onFocus={handleFocus}
      onBlur={() => setDraft(null)}
      onKeyDown={onKeyDown}
      disabled={disabled}
      className={`${defaultClasses} ${className}`}
      data-testid={testId}
      aria-label={ariaLabel}
      {...dataAttrs}
    />
  );
};
