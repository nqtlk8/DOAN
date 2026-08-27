import { useQuery } from '@tanstack/react-query';
import { getFeaturedProducts } from '../api/productApi';

export const useFeaturedProducts = (rootSlug: string, limit: number = 12) => {
  return useQuery({
    queryKey: ['featuredProducts', rootSlug, limit],
    queryFn: () => getFeaturedProducts(rootSlug, limit),
    enabled: !!rootSlug,
  });
};
