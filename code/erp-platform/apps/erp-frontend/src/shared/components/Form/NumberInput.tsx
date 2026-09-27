import React from 'react';
import { formatNumber, parseNumber } from '../../utils/format';

export interface NumberInputProps {
  value: number;
  onChange: (v: number) => void;
  disabled?: boolean;
  className?: string;
  allowNegative?: boolean;
  min?: number;
  'data-testid'?: string;
  onKeyDown?: (e: React.KeyboardEvent<HTMLInputElement>) => void;
  inputRef?: React.Ref<HTMLInputElement>;
  variant?: 'field' | 'cell';
}

export const NumberInput: React.FC<NumberInputProps> = ({
  value,
  onChange,
  disabled,
  className = '',
  allowNegative = false,
  min,
  'data-testid': testId,
  onKeyDown,
  inputRef,
  variant = 'field'
}) => {
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    let parsed = parseNumber(e.target.value);
    
    if (isNaN(parsed)) parsed = 0;
    if (!allowNegative && parsed < 0) parsed = 0;
    if (min !== undefined && parsed < min) parsed = min;
    
    onChange(parsed);
  };

  const handleFocus = (e: React.FocusEvent<HTMLInputElement>) => {
    e.target.select();
  };

  const defaultClasses = variant === 'cell' 
    ? 'w-full h-full border-0 bg-transparent px-2 text-right tabular-nums focus:bg-surface focus:ring-2 focus:ring-inset focus:ring-primary/30 outline-none'
    : 'erp-input text-right tabular-nums';

  return (
    <input
      ref={inputRef}
      type="text"
      inputMode="numeric"
      value={formatNumber(value)}
      onChange={handleChange}
      onFocus={handleFocus}
      onKeyDown={onKeyDown}
      disabled={disabled}
      className={`${defaultClasses} ${className}`}
      data-testid={testId}
    />
  );
};
