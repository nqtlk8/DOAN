export interface Customer {
  id: string;
  customerCode?: string;
  code?: string; // Tạm thời giữ để UI khỏi lỗi
  name: string;
  phone?: string;
  email?: string;
  address?: string;
  taxCode?: string;
  isDeleted?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface Product {
  id: number;
  code: string;
  name: string;
  categoryId?: number;
  categoryName?: string;
  baseUnit?: string;
  isActive?: boolean;
  attributes?: Record<string, unknown>;
  price?: number;
  createdAt?: string;
  updatedAt?: string;
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
