import { useQuery } from '@tanstack/react-query';
import { ApiService } from '../api/ApiService';
import { DASHBOARD_REFETCH_INTERVAL_MS } from '../config/dashboard';

/**
 * Custom hook to fetch dashboard metrics.
 * Refetches automatically every DASHBOARD_REFETCH_INTERVAL_MS, but not in the background.
 *
 * @param branchId - Optional branch ID to filter by
 * @param startDateKey - Optional start date key (YYYYMMDD)
 * @param endDateKey - Optional end date key (YYYYMMDD)
 */
export const useDashboardMetrics = (branchId?: number, startDateKey?: number, endDateKey?: number) => {
    return useQuery({
        queryKey: ['dashboardMetrics', branchId, startDateKey, endDateKey],
        queryFn: () => ApiService.Analytics.getDashboardMetrics(branchId, startDateKey, endDateKey),
        refetchInterval: DASHBOARD_REFETCH_INTERVAL_MS,
        refetchIntervalInBackground: false,
    });
};
