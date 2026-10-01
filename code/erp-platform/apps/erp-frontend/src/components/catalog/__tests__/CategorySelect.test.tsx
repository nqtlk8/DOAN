import { render, screen, fireEvent, within } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { CategorySelect } from '../CategorySelect';
import { groupCategories } from '../categoryTree';
import { useCategories } from '../../../hooks/useCategories';
import type { Category } from '../../../types/catalog';

vi.mock('../../../hooks/useCategories', () => ({
  useCategories: vi.fn(),
}));

// Cùng dạng dữ liệu API trả về (gốc trước, rồi con theo id) — trích từ cây danh mục của web-public.
const CATEGORIES: Category[] = [
  { id: 1, code: 'vlxd', name: 'VẬT LIỆU XÂY DỰNG', parentId: null },
  { id: 2, code: 'ttnt', name: 'TRANG TRÍ NỘI THẤT', parentId: null },
  { id: 3, code: 'ban-cau', name: 'Bàn Cầu - Bồn Tiểu', parentId: 1, parentName: 'VẬT LIỆU XÂY DỰNG' },
  { id: 7, code: 'chau-rua', name: 'Chậu Rửa - Lavabo', parentId: 1, parentName: 'VẬT LIỆU XÂY DỰNG' },
  { id: 8, code: 'den-trang-tri', name: 'Đèn Trang Trí', parentId: 2, parentName: 'TRANG TRÍ NỘI THẤT' },
];

/** Giả lập kết quả useCategories (chỉ các field component dùng). */
const mockCategories = (state: { data: Category[] | undefined; isLoading: boolean; isError: boolean }) =>
  vi.mocked(useCategories).mockReturnValue(state as unknown as ReturnType<typeof useCategories>);

describe('groupCategories', () => {
  it('nhóm danh mục con theo danh mục gốc, giữ thứ tự', () => {
    const groups = groupCategories(CATEGORIES);
    expect(groups.map((g) => g.root.code)).toEqual(['vlxd', 'ttnt']);
    expect(groups[0].children.map((c) => c.code)).toEqual(['ban-cau', 'chau-rua']);
    expect(groups[1].children.map((c) => c.code)).toEqual(['den-trang-tri']);
  });
});

describe('CategorySelect', () => {
  beforeEach(() => {
    mockCategories({ data: CATEGORIES, isLoading: false, isError: false });
  });

  it('hiển thị danh mục gốc thành nhóm, chỉ cho chọn danh mục con', () => {
    render(<CategorySelect value={undefined} onChange={vi.fn()} />);
    const select = screen.getByTestId('category-select');

    const groups = select.querySelectorAll('optgroup');
    expect(Array.from(groups).map((g) => g.getAttribute('label'))).toEqual(['VẬT LIỆU XÂY DỰNG', 'TRANG TRÍ NỘI THẤT']);
    expect(within(groups[0] as HTMLElement).getAllByRole('option').map((o) => o.textContent)).toEqual([
      'Bàn Cầu - Bồn Tiểu',
      'Chậu Rửa - Lavabo',
    ]);
    // Danh mục gốc không phải là một <option> chọn được
    expect(screen.queryByRole('option', { name: 'VẬT LIỆU XÂY DỰNG' })).toBeNull();
  });

  it('trả về id số khi chọn, undefined khi bỏ chọn', () => {
    const onChange = vi.fn();
    render(<CategorySelect value={undefined} onChange={onChange} />);
    const select = screen.getByTestId('category-select');

    fireEvent.change(select, { target: { value: '7' } });
    expect(onChange).toHaveBeenLastCalledWith(7);

    fireEvent.change(select, { target: { value: '' } });
    expect(onChange).toHaveBeenLastCalledWith(undefined);
  });

  it('khoá dropdown khi đang tải', () => {
    mockCategories({ data: undefined, isLoading: true, isError: false });
    render(<CategorySelect value={undefined} onChange={vi.fn()} />);
    expect(screen.getByTestId('category-select')).toBeDisabled();
    expect(screen.getByRole('option', { name: 'Đang tải danh mục...' })).toBeInTheDocument();
  });
});
