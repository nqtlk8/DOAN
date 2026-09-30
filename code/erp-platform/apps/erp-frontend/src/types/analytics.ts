export interface ProductPerformanceDto {
    productId: number;
    productName: string;
    quantitySold: number;
    revenue: number;
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
