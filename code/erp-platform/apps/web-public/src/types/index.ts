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
