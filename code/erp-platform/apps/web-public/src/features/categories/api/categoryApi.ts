import { mockApi } from '../../../test/mockApi';

export const getNavCategories = () => {
  return mockApi.fetchNavCategories();
};
