import { useQuery } from '@tanstack/react-query';
import { getSidebarCategories } from '../api/categoryApi';

export const useCategorySidebar = (rootSlug: string) => {
  return useQuery({
    queryKey: ['sidebarCategories', rootSlug],
    queryFn: () => getSidebarCategories(rootSlug),
    enabled: !!rootSlug,
  });
};
