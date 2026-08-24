import { useQuery } from '@tanstack/react-query';
import { getNavCategories } from '../api/categoryApi';

export const useCategoryNav = () => {
  return useQuery({
    queryKey: ['categoryNav'],
    queryFn: getNavCategories,
    staleTime: Infinity, // Cache vô hạn như yêu cầu
  });
};
