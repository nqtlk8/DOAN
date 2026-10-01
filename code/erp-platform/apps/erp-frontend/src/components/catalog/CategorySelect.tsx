import React from 'react';
import { useCategories } from '../../hooks/useCategories';
import { groupCategories } from './categoryTree';

interface CategorySelectProps {
  value?: number | null;
  onChange: (categoryId: number | undefined) => void;
  disabled?: boolean;
  id?: string;
  className?: string;
}

/**
 * Dropdown chọn danh mục sản phẩm. Danh mục gốc hiển thị thành tiêu đề nhóm (optgroup, không chọn được),
 * sản phẩm chỉ gắn vào danh mục con — cùng quy tắc với backend (ProductWriter.requireLeafCategory).
 */
export const CategorySelect: React.FC<CategorySelectProps> = ({ value, onChange, disabled, id, className }) => {
  const { data, isLoading, isError } = useCategories();
  const groups = groupCategories(data ?? []);

  return (
    <div>
      <select
        id={id}
        data-testid="category-select"
        className={className ?? 'erp-input bg-surface'}
        value={value ?? ''}
        disabled={disabled || isLoading}
        onChange={(e) => onChange(e.target.value ? Number(e.target.value) : undefined)}
      >
        <option value="">{isLoading ? 'Đang tải danh mục...' : '-- Chọn danh mục --'}</option>
        {groups.map(({ root, children }) =>
          children.length > 0 ? (
            <optgroup key={root.id} label={root.name}>
              {children.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </optgroup>
          ) : (
            <option key={root.id} value={root.id}>
              {root.name}
            </option>
          ),
        )}
      </select>
      {isError && <div className="text-danger text-[13px] mt-1">Không tải được danh mục</div>}
    </div>
  );
};
