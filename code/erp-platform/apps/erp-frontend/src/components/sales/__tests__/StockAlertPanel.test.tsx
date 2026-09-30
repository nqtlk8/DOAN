import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { StockAlertPanel } from '../StockAlertPanel';
import type { StockAlertSummaryDto } from '../../../types/analytics';

describe('StockAlertPanel', () => {
    it('renders loading state', () => {
        render(<StockAlertPanel isLoading={true} isError={false} onRetry={vi.fn()} />);
        expect(screen.getByText('Đang tải dữ liệu cảnh báo...')).toBeInTheDocument();
    });

    it('renders error state and handles retry', () => {
        const onRetry = vi.fn();
        render(<StockAlertPanel isLoading={false} isError={true} onRetry={onRetry} />);
        
        expect(screen.getByText('Lỗi khi tải dữ liệu cảnh báo tồn kho.')).toBeInTheDocument();
        const retryButton = screen.getByRole('button', { name: /thử lại/i });
        fireEvent.click(retryButton);
        expect(onRetry).toHaveBeenCalled();
    });

    it('renders no alerts state', () => {
        const summary: StockAlertSummaryDto = {
            negativeCount: 0,
            lowStockCount: 0,
            generatedAt: new Date().toISOString(),
            alerts: []
        };
        render(<StockAlertPanel isLoading={false} isError={false} summary={summary} onRetry={vi.fn()} />);
        expect(screen.getByText('Không có cảnh báo tồn kho')).toBeInTheDocument();
    });

    it('renders alerts correctly', () => {
        const summary: StockAlertSummaryDto = {
            negativeCount: 1,
            lowStockCount: 1,
            generatedAt: new Date().toISOString(),
            alerts: [
                {
                    productId: 1,
                    productCode: 'SP01',
                    productName: 'Sản phẩm 1',
                    branchId: 1,
                    branchName: 'Chi nhánh 1',
                    currentQuantity: -5,
                    minQuantityThreshold: null,
                    alertType: 'NEGATIVE_STOCK'
                },
                {
                    productId: 2,
                    productCode: 'SP02',
                    productName: 'Sản phẩm 2',
                    branchId: 1,
                    branchName: 'Chi nhánh 1',
                    currentQuantity: 2,
                    minQuantityThreshold: 10,
                    alertType: 'LOW_STOCK'
                }
            ]
        };

        render(<StockAlertPanel isLoading={false} isError={false} summary={summary} onRetry={vi.fn()} />);

        // Header text
        expect(screen.getByText('1 sản phẩm tồn âm · 1 sản phẩm dưới ngưỡng')).toBeInTheDocument();

        // Rows check
        const rows = screen.getAllByRole('row');
        expect(rows).toHaveLength(3); // 1 header + 2 data rows

        // First data row (NEGATIVE_STOCK)
        expect(screen.getByText('Tồn âm')).toBeInTheDocument();
        expect(screen.getByText('Tồn âm')).toHaveClass('badge-danger');
        expect(screen.getByText('-5')).toBeInTheDocument();
        expect(screen.getByText('—')).toBeInTheDocument();

        // Second data row (LOW_STOCK)
        expect(screen.getByText('Sắp hết')).toBeInTheDocument();
        expect(screen.getByText('Sắp hết')).toHaveClass('badge-warning');
        expect(screen.getByText('2')).toBeInTheDocument();
        expect(screen.getByText('10')).toBeInTheDocument();

        // Màu số tồn theo mức cảnh báo (token design system), không phải luôn đỏ
        expect(screen.getByText('-5')).toHaveClass('text-danger');
        expect(screen.getByText('2')).toHaveClass('text-warning');
    });
});
