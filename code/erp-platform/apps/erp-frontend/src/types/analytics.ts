export interface ProductPerformanceDto {
    productId: number;
    productName: string;
    quantitySold: number;
    revenue: number;
    /** Giá vốn thuần (FIFO, đã trừ hàng trả). */
    cogs?: number | null;
    /** Lợi nhuận gộp = doanh thu − giá vốn. */
    grossProfit?: number | null;
    /** Có dòng bán/trả thiếu giá vốn → không hiển thị giá vốn/lợi nhuận. */
    costIncomplete?: boolean;
}

export interface SlowMovingProductDto {
    productId: number;
    productCode: string;
    productName: string;
    currentStock: number;
}

export interface DashboardMetricsDto {
    totalRevenue: number;
    grossProfit: number;
    inventoryTurnoverRatio: number;
    totalReceivableDebt: number;
    topSellingProducts: ProductPerformanceDto[];
    slowMovingProducts: SlowMovingProductDto[];
}

export type AlertType = 'NEGATIVE_STOCK' | 'LOW_STOCK';

export interface StockAlertDto {
    productId: number;
    productCode: string;
    productName: string;
    branchId: number;
    branchName: string;
    currentQuantity: number;
    minQuantityThreshold: number | null;
    alertType: AlertType;
}

export interface StockAlertSummaryDto {
    negativeCount: number;
    lowStockCount: number;
    generatedAt: string;
    alerts: StockAlertDto[];
}
