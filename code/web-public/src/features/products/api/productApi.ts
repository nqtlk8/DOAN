import { mockApi } from '../../../test/mockApi';

export const getFeaturedProducts = (rootSlug: string, limit?: number) => {
  return mockApi.fetchFeaturedProducts(rootSlug, limit);
};

export const getProductListing = (rootSlug: string, subSlug: string | null, page?: number, limit?: number) => {
  return mockApi.fetchProductListing(rootSlug, subSlug, page, limit);
};
