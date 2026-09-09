export interface DashboardMetricsDto {
    totalRevenue: number;
    grossProfit: number;
    inventoryTurnoverRatio: number;
    totalOverdueDebt: number;
    topSellingProducts: {
        productId: number;
        productName: string;
        quantitySold: number;
        revenue: number;
    }[];
}
