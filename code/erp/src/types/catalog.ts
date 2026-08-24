export interface Customer {
  id: string;
  code?: string;
  name: string;
  phone?: string;
  email?: string;
  address?: string;
  taxCode?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Product {
  id: string;
  sku?: string;
  name: string;
  description?: string;
  price: number;
  cost?: number;
  unit?: string;
  category?: string;
  createdAt?: string;
  updatedAt?: string;
  // keep old fields if needed for UI compatibility, or refactor UI components
  basePrice?: number;
  stockQuantity?: number;
  code?: string;
}

export interface Distributor {
  id: string;
  code?: string;
  name: string;
  phone?: string;
  email?: string;
  address?: string;
  region?: string;
  taxCode?: string;
  createdAt?: string;
  updatedAt?: string;
}
