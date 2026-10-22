import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { NumberInput } from '../NumberInput';

describe('NumberInput', () => {
  it('formats number correctly and strips on change', () => {
    const onChange = vi.fn();
    render(<NumberInput value={1000} onChange={onChange} data-testid="num" />);
    
    const input = screen.getByTestId('num') as HTMLInputElement;
    expect(input.value).toBe('1.000');
    
    fireEvent.change(input, { target: { value: '10005' } });
    expect(onChange).toHaveBeenCalledWith(10005);
  });
  
  it('bỏ dấu "-" khi không cho phép số âm', () => {
    const onChange = vi.fn();
    render(<NumberInput value={0} onChange={onChange} data-testid="num" allowNegative={false} />);

    const input = screen.getByTestId('num');
    fireEvent.change(input, { target: { value: '-5' } });
    expect(onChange).toHaveBeenLastCalledWith(5);
  });

  it('cho phép gõ số thập phân kiểu Việt Nam khi maxFractionDigits > 0', () => {
    const onChange = vi.fn();
    const { rerender } = render(<NumberInput value={0} onChange={onChange} data-testid="num" maxFractionDigits={3} />);
    const input = screen.getByTestId('num') as HTMLInputElement;
    fireEvent.focus(input);
    fireEvent.change(input, { target: { value: '1,' } });
    expect(onChange).toHaveBeenLastCalledWith(1);
    rerender(<NumberInput value={1} onChange={onChange} data-testid="num" maxFractionDigits={3} />);
    expect(input.value).toBe('1,'); // không mất dấu phẩy khi đang gõ
    fireEvent.change(input, { target: { value: '1,5' } });
    expect(onChange).toHaveBeenLastCalledWith(1.5);
  });

  it('cho phép gõ dấu "-" đầu tiên khi allowNegative', () => {
    const onChange = vi.fn();
    const { rerender } = render(<NumberInput value={0} onChange={onChange} data-testid="num" allowNegative />);
    const input = screen.getByTestId('num') as HTMLInputElement;
    fireEvent.focus(input);
    fireEvent.change(input, { target: { value: '-' } });
    rerender(<NumberInput value={0} onChange={onChange} data-testid="num" allowNegative />);
    expect(input.value).toBe('-');
    fireEvent.change(input, { target: { value: '-12000' } });
    expect(onChange).toHaveBeenLastCalledWith(-12000);
  });
});
