import apiClient from '../api/axiosInstance';

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

export const getDashboardMetrics = async (branchId?: number, startDateKey?: number, endDateKey?: number): Promise<DashboardMetricsDto> => {
    const params = new URLSearchParams();
    if (branchId) params.append('branchId', branchId.toString());
    if (startDateKey) params.append('startDateKey', startDateKey.toString());
    if (endDateKey) params.append('endDateKey', endDateKey.toString());
    
    const response = await apiClient.get(`/api/v1/analytics/dashboard?${params.toString()}`);
    return response.data;
};

export const exportDashboardExcel = async (branchId?: number, startDateKey?: number, endDateKey?: number) => {
    const params = new URLSearchParams();
    if (branchId) params.append('branchId', branchId.toString());
    if (startDateKey) params.append('startDateKey', startDateKey.toString());
    if (endDateKey) params.append('endDateKey', endDateKey.toString());
    
    const response = await apiClient.get(`/api/v1/analytics/export/excel?${params.toString()}`, {
        responseType: 'blob'
    });
    
    // Download the file
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'dashboard_report.xlsx');
    document.body.appendChild(link);
    link.click();
    link.parentNode?.removeChild(link);
};
