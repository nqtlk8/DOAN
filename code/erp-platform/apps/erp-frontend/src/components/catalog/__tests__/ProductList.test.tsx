import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { ProductList } from '../ProductList';
import { useAuth } from '../../../context/AuthContext';
import { useProducts } from '../../../hooks/useProducts';

vi.mock('../../../context/AuthContext', () => ({
  useAuth: vi.fn(),
  ROLES: { ADMIN: 'ADMIN', STAFF: 'STAFF' }
}));

vi.mock('../../../hooks/useProducts', () => ({
  useProducts: vi.fn(),
}));

// Mock queryClient
vi.mock('@tanstack/react-query', () => ({
  useQueryClient: vi.fn().mockReturnValue({ invalidateQueries: vi.fn() })
}));

describe('ProductList', () => {
  it('renders products', () => {
    (useAuth as any).mockReturnValue({
      hasRole: (role: string) => true, // Admin
    });

    (useProducts as any).mockReturnValue({
      query: {
        data: [{ id: '1', code: 'PRD-1', name: 'Product 1', basePrice: 100 }],
        isLoading: false,
        isError: false,
      },
      createMutation: {},
      updateMutation: {},
      deleteMutation: {}
    });

    render(<ProductList />);

    expect(screen.getByText('Danh Mục Sản Phẩm')).toBeInTheDocument();
    expect(screen.getByText('Product 1')).toBeInTheDocument();
  });
});

