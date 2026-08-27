import { MOCK_CATEGORIES, MOCK_PRODUCTS } from './mockData';
import { Category, Product, SubCategory, PaginatedResponse } from '../types';

const DELAY = 600; // Mô phỏng trễ mạng 600ms

export const mockApi = {
  // Lấy Menu (Tất cả Root Categories)
  fetchNavCategories: (): Promise<Category[]> => {
    return new Promise(resolve => setTimeout(() => resolve(MOCK_CATEGORIES), DELAY));
  },

  // Lấy Sidebar theo Root Slug (VD: "vlxd")
  fetchSidebarCategories: (rootSlug: string): Promise<SubCategory[]> => {
    return new Promise(resolve => {
      setTimeout(() => {
        const cat = MOCK_CATEGORIES.find(c => c.slug === rootSlug.toLowerCase());
        if (!cat) return resolve([]);
        
        // Cập nhật số lượng sản phẩm thực tế
        const updatedSubs = cat.subCategories.map(sub => {
          const actualCount = MOCK_PRODUCTS.filter(p => p.category.toLowerCase() === sub.name.toLowerCase()).length;
          return { ...sub, count: actualCount };
        });
        
        resolve(updatedSubs);
      }, DELAY);
    });
  },

  // Lấy Sản phẩm Slider ở Trang chủ
  fetchFeaturedProducts: (rootSlug: string, limit: number = 12): Promise<Product[]> => {
    return new Promise(resolve => {
      setTimeout(() => {
        // Trong thực tế sẽ filter theo root category id
        // Dựa vào mock data hiện tại, ta lấy đại diện
        let filtered = MOCK_PRODUCTS;
        if (rootSlug.toLowerCase() === 'ttnt') {
          filtered = MOCK_PRODUCTS.filter(p => p.id.startsWith('t'));
        } else if (rootSlug.toLowerCase() === 'vlxd') {
          filtered = MOCK_PRODUCTS.filter(p => p.id.startsWith('p'));
        }
        resolve(filtered.slice(0, limit));
      }, DELAY);
    });
  },

  // Lấy Sản phẩm phân trang ở Trang Listing
  fetchProductListing: (
    rootSlug: string, 
    subSlug: string | null, 
    page: number = 1, 
    limit: number = 60
  ): Promise<PaginatedResponse<Product>> => {
    return new Promise(resolve => {
      setTimeout(() => {
        let filtered = MOCK_PRODUCTS;
        
        // Filter by Root (thô sơ)
        if (rootSlug.toLowerCase() === 'ttnt') {
          filtered = filtered.filter(p => p.id.startsWith('t'));
        } else if (rootSlug.toLowerCase() === 'vlxd') {
          filtered = filtered.filter(p => p.id.startsWith('p'));
        }

        // Filter by Sub
        if (subSlug) {
          const rootCat = MOCK_CATEGORIES.find(c => c.slug === rootSlug.toLowerCase());
          if (rootCat) {
            const sub = rootCat.subCategories.find(s => s.slug === subSlug);
            if (sub) {
              filtered = filtered.filter(p => p.category.toLowerCase() === sub.name.toLowerCase());
            }
          }
        }

        resolve({
          data: filtered,
          totalItems: filtered.length,
          totalPages: Math.ceil(filtered.length / limit),
          currentPage: page
        });
      }, DELAY);
    });
  }
};
