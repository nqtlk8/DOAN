export interface Customer {
  id: string;
  customerCode?: string;
  code?: string; // Tạm thời giữ để UI khỏi lỗi
  name: string;
  phone?: string;
  email?: string;
  address?: string;
  taxCode?: string;
  isActive?: boolean;
  isDeleted?: boolean;
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

export interface Supplier {
  id: string;
  branchId?: number;
  code: string;
  name: string;
  phone?: string;
  email?: string;
  address?: string;
  taxCode?: string;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}
