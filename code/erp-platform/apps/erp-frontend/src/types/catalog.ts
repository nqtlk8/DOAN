export interface Customer {
  id: number | string;
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

/**
 * Danh mục sản phẩm (GET /api/v1/catalog/categories). Cây 2 cấp: danh mục gốc (parentId rỗng)
 * dùng để nhóm, sản phẩm gắn vào danh mục con.
 */
export interface Category {
  id: number;
  code: string;
  name: string;
  parentId?: number | null;
  parentName?: string | null;
  isActive?: boolean;
}

export interface Product {
  id: number | string;
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
  id: number | string;
  code: string;
  name: string;
  phone?: string;
  email?: string;
  address?: string;
  taxCode?: string;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
}
