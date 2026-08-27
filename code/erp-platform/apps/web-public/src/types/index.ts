// Category Types
export interface SubCategory {
  id: string;
  name: string;
  slug: string;
  count: number;
}

export interface Category {
  id: string;
  name: string;
  slug: string;
  subCategories: SubCategory[];
}

// Product Types
export interface Product {
  id: string;
  category: string;
  name: string;
  image: string;
  originalPrice: number;
  salePrice: number;
  discountBadge?: string;
}

// API Response Types
export interface PaginatedResponse<T> {
  data: T[];
  totalItems: number;
  totalPages: number;
  currentPage: number;
}
