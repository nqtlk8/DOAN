import { render, screen, fireEvent, within } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Dashboard } from '../Dashboard';
import type { DashboardMetricsDto, ProductPerformanceDto } from '../../../types/analytics';

const hooks = vi.hoisted(() => ({ metrics: vi.fn() }));

vi.mock('../../../hooks/useDashboardMetrics', () => ({ useDashboardMetrics: hooks.metrics }));
vi.mock('../../../hooks/useStockAlerts', () => ({
  useStockAlerts: () => ({ data: undefined, isLoading: false, isError: false, refetch: vi.fn(), dataUpdatedAt: 0 }),
}));
vi.mock('../StockAlertPanel', () => ({ StockAlertPanel: () => null }));
vi.mock('../../../api/ApiService', () => ({
  ApiService: { Branch: { getAll: () => Promise.resolve([]) }, Analytics: { exportExcel: vi.fn() } },
}));

const product = (i: number, over: Partial<ProductPerformanceDto> = {}): ProductPerformanceDto => ({
  productId: i,
  productName: `Sản phẩm ${i}`,
  quantitySold: 2,
  revenue: 1_000_000 - i,
  cogs: 600_000,
  grossProfit: 400_000 - i,
  costIncomplete: false,
  ...over,
});

const metrics = (products: ProductPerformanceDto[]): DashboardMetricsDto => ({
  totalRevenue: 0,
  grossProfit: 0,
  inventoryTurnoverRatio: 0,
  totalReceivableDebt: 0,
  topSellingProducts: products,
  slowMovingProducts: [],
});

const renderDashboard = () =>
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <Dashboard />
    </QueryClientProvider>,
  );

describe('Dashboard — chi tiết top sản phẩm', () => {
  beforeEach(() => {
    hooks.metrics.mockReset();
  });

  it('U-DB-01: có cột Giá vốn và Lợi nhuận gộp cạnh Doanh thu, hiển thị đúng số', () => {
    hooks.metrics.mockReturnValue({ data: metrics([product(1)]), isLoading: false, isError: false, refetch: vi.fn(), isFetching: false, dataUpdatedAt: 0 });
    renderDashboard();
    const table = screen.getByTestId('top-products-table');
    const headers = within(table).getAllByRole('columnheader').map((h) => h.textContent);
    expect(headers).toEqual(['#', 'Sản phẩm', 'SL bán', 'Doanh thu', 'Giá vốn', 'Lợi nhuận gộp']);
    expect(within(table).getByTestId('top-product-cogs')).toHaveTextContent('600.000');
    expect(within(table).getByTestId('top-product-profit')).toHaveTextContent('399.999');
  });

  it('U-DB-02: sản phẩm thiếu giá vốn hiển thị "—" và có ghi chú; lợi nhuận âm tô đỏ', () => {
    hooks.metrics.mockReturnValue({
      data: metrics([product(1, { costIncomplete: true }), product(2, { grossProfit: -50_000 })]),
      isLoading: false, isError: false, refetch: vi.fn(), isFetching: false, dataUpdatedAt: 0,
    });
    renderDashboard();
    const cogs = screen.getAllByTestId('top-product-cogs');
    const profit = screen.getAllByTestId('top-product-profit');
    expect(cogs[0]).toHaveTextContent('—');
    expect(profit[0]).toHaveTextContent('—');
    expect(screen.getByTestId('top-products-missing-cost')).toBeInTheDocument();
    expect(profit[1]).toHaveClass('text-danger');
  });

  it('U-DB-03: mặc định Top 10; chọn Top 50 / Tất cả gọi API với topLimit 50 / 0; bảng hiện đủ số dòng', () => {
    const many = Array.from({ length: 25 }, (_, i) => product(i + 1));
    hooks.metrics.mockReturnValue({ data: metrics(many), isLoading: false, isError: false, refetch: vi.fn(), isFetching: false, dataUpdatedAt: 0 });
    renderDashboard();

    expect(hooks.metrics).toHaveBeenLastCalledWith(undefined, expect.any(Number), expect.any(Number), 10);
    fireEvent.change(screen.getByTestId('dashboard-top-limit'), { target: { value: '50' } });
    expect(hooks.metrics).toHaveBeenLastCalledWith(undefined, expect.any(Number), expect.any(Number), 50);
    fireEvent.change(screen.getByTestId('dashboard-top-limit'), { target: { value: '0' } });
    expect(hooks.metrics).toHaveBeenLastCalledWith(undefined, expect.any(Number), expect.any(Number), 0);

    expect(screen.getAllByTestId('top-product-row')).toHaveLength(25);
    expect(screen.getByTestId('top-products-count')).toHaveTextContent('25 sản phẩm');
    expect(screen.getByText('Top 10 sản phẩm theo doanh thu')).toBeInTheDocument();
  });
});
