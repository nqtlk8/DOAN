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
  
  it('prevents negative if allowNegative is false', () => {
    const onChange = vi.fn();
    render(<NumberInput value={0} onChange={onChange} data-testid="num" allowNegative={false} />);
    
    const input = screen.getByTestId('num');
    fireEvent.change(input, { target: { value: '-5' } });
    expect(onChange).toHaveBeenCalledWith(0);
  });
});
