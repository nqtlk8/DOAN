import type { Category } from '../../types/catalog';

/** Nhóm danh mục để hiển thị: mỗi danh mục gốc là một nhóm, danh mục con là lựa chọn. */
export interface CategoryGroup {
  root: Category;
  children: Category[];
}

/**
 * Chia danh sách phẳng thành nhóm theo danh mục gốc, giữ thứ tự API trả về.
 * Danh mục gốc KHÔNG có con được coi là một lựa chọn độc lập (nhóm không có children).
 */
export const groupCategories = (categories: Category[]): CategoryGroup[] => {
  const roots = categories.filter((c) => c.parentId == null);
  return roots.map((root) => ({
    root,
    children: categories.filter((c) => c.parentId === root.id),
  }));
};
