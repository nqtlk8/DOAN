import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { CustomerList } from '../CustomerList';
import { useAuth } from '../../../context/AuthContext';
import { useCustomers } from '../../../hooks/useCustomers';

vi.mock('../../../context/AuthContext', () => ({
  useAuth: vi.fn(),
  ROLES: { ADMIN: 'ADMIN', STAFF: 'STAFF' }
}));

vi.mock('../../../hooks/useCustomers', () => ({
  useCustomers: vi.fn(),
}));

describe('CustomerList', () => {
  it('renders customer list and add button for ADMIN', () => {
    (useAuth as any).mockReturnValue({
      hasRole: (role: string) => role === 'ADMIN',
    });

    (useCustomers as any).mockReturnValue({
      customers: [{ id: '1', code: 'CUS-001', name: 'Test Customer', isActive: true }],
      isLoading: false,
      isError: false,
      createCustomer: vi.fn(),
      updateCustomer: vi.fn(),
      deleteCustomer: vi.fn()
    });

    render(<CustomerList />);

    expect(screen.getByText('Danh Mục Khách Hàng')).toBeInTheDocument();
    expect(screen.getByTestId('customer-create-button')).toBeInTheDocument();
    expect(screen.getByText('Test Customer')).toBeInTheDocument();
    expect(screen.getByText('CUS-001')).toBeInTheDocument();
  });

  it('hides add button for non-ADMIN', () => {
    (useAuth as any).mockReturnValue({
      hasRole: (role: string) => false,
    });

    (useCustomers as any).mockReturnValue({
      customers: [],
      isLoading: false,
      isError: false,
    });

    render(<CustomerList />);

    expect(screen.queryByTestId('customer-create-button')).not.toBeInTheDocument();
  });
});

