import { useQuery } from '@tanstack/react-query';
import { ApiService } from '../api/ApiService';
import { DASHBOARD_REFETCH_INTERVAL_MS } from '../config/dashboard';

/**
 * Custom hook to fetch stock alerts for the dashboard.
 * Refetches automatically every DASHBOARD_REFETCH_INTERVAL_MS, but not in the background.
 * Alerts are independent of the reporting period.
 *
 * @param branchId - Optional branch ID to filter alerts by
 */
export const useStockAlerts = (branchId?: number) => {
    return useQuery({
        queryKey: ['stockAlerts', branchId],
        queryFn: () => ApiService.Analytics.getStockAlerts(branchId),
        refetchInterval: DASHBOARD_REFETCH_INTERVAL_MS,
        refetchIntervalInBackground: false,
    });
};
