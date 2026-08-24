import { useQuery } from '@tanstack/react-query';
import { getProductListing } from '../api/productApi';

export const useProductListing = (rootSlug: string, subSlug: string | null, page: number = 1) => {
  return useQuery({
    queryKey: ['productListing', rootSlug, subSlug, page],
    queryFn: () => getProductListing(rootSlug, subSlug, page),
    enabled: !!rootSlug,
  });
};
