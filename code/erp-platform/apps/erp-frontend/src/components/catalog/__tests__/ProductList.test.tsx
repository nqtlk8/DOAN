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
  useQueryClient: vi.fn().mockReturnValue({ invalidateQueries: vi.fn() }),
  // Danh sách chi nhánh cho ô chọn bảng giá của quản trị viên
  useQuery: vi.fn().mockReturnValue({ data: [{ id: 1, name: 'Chi nhánh Trung Tâm (TP1)' }, { id: 2, name: 'Chi nhánh Quận 7 (TP2)' }] }),
}));

describe('ProductList', () => {
  it('renders products', () => {
    (useAuth as any).mockReturnValue({
      hasRole: (role: string) => true, // Admin
    });

    (useProducts as any).mockReturnValue({
      query: {
        data: [
          { id: '1', code: 'PRD-1', name: 'Product 1', price: 3800000 },
          { id: '2', code: 'PRD-2', name: 'Product 2', price: null },
        ],
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
    // Giá niêm yết của chi nhánh được hiển thị; sản phẩm chưa có giá hiện '—' thay vì 0
    expect(screen.getByText('3.800.000')).toBeInTheDocument();
    expect(screen.getByText('—', { selector: 'td.num' })).toBeInTheDocument();
    // Quản trị viên xem bảng giá của chi nhánh đầu tiên theo mặc định
    expect(screen.getByLabelText('Bảng giá chi nhánh')).toHaveValue('1');
    expect(useProducts).toHaveBeenLastCalledWith(1);
  });
});

