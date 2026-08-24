import { mockApi } from '../../../test/mockApi';

export const getNavCategories = () => {
  return mockApi.fetchNavCategories();
};

export const getSidebarCategories = (rootSlug: string) => {
  return mockApi.fetchSidebarCategories(rootSlug);
};
