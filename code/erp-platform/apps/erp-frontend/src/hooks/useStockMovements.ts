import { useQuery } from '@tanstack/react-query';
import { ApiService } from '../api/ApiService';
import type { StockMovement } from '../types/inventory';

export const useStockMovements = (productId: string) => {
  return useQuery<StockMovement[]>({
    queryKey: ['stock-movements', productId],
    queryFn: () => ApiService.Inventory.getStockMovements(productId),
    enabled: !!productId
  });
};
