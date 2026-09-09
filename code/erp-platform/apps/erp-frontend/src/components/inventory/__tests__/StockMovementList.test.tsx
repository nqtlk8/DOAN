import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { StockMovementList } from '../StockMovementList';
import { useStockMovements } from '../../../hooks/useStockMovements';

vi.mock('../../../hooks/useStockMovements', () => ({
  useStockMovements: vi.fn(),
}));

describe('StockMovementList', () => {
  it('renders loading state', () => {
    (useStockMovements as any).mockReturnValue({
      data: undefined,
      isLoading: true,
      isError: false,
    });

    render(<StockMovementList productId='123' />);
    expect(screen.getByText('Lịch Sử Movement')).toBeInTheDocument();
  });

  it('renders movements', () => {
    (useStockMovements as any).mockReturnValue({
      data: [
        { id: '1', movementType: 'INBOUND', quantity: 100, createdAt: new Date().toISOString() },
        { id: '2', movementType: 'SALE', quantity: -20, createdAt: new Date().toISOString() }
      ],
      isLoading: false,
      isError: false,
    });

    render(<StockMovementList productId='123' />);
    expect(screen.getByText('INBOUND')).toBeInTheDocument();
    expect(screen.getByText('+100')).toBeInTheDocument();
    expect(screen.getByText('SALE')).toBeInTheDocument();
    expect(screen.getByText('-20')).toBeInTheDocument();
  });
});

