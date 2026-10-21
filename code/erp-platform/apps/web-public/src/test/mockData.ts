import { Category } from '../types';

export const MOCK_CATEGORIES: Category[] = [
  {
    id: 'c1',
    name: 'VẬT LIỆU XÂY DỰNG',
    slug: 'vlxd',
    subCategories: [
      { id: '1', name: 'Bàn Cầu - Bồn Tiểu', slug: 'ban-cau', count: 283 },
      { id: '2', name: 'Bồn cầu 1 khối', slug: 'bon-cau-1-khoi', count: 78 },
      { id: '3', name: 'Bồn cầu 2 khối', slug: 'bon-cau-2-khoi', count: 59 },
      { id: '4', name: 'Bồn cầu thông minh', slug: 'bon-cau-thong-minh', count: 186 },
      { id: '5', name: 'Chậu Rửa - Lavabo', slug: 'chau-rua', count: 273 },
    ]
  },
  {
    id: 'c2',
    name: 'TRANG TRÍ NỘI THẤT',
    slug: 'ttnt',
    subCategories: [
      { id: 't1', name: 'Đèn Trang Trí', slug: 'den-trang-tri', count: 120 },
      { id: 't2', name: 'Rèm Cửa', slug: 'rem-cua', count: 85 },
      { id: 't3', name: 'Sofa', slug: 'sofa', count: 45 },
      { id: 't4', name: 'Bàn Trà', slug: 'ban-tra', count: 90 },
      { id: 't5', name: 'Đồ Trang Trí', slug: 'do-trang-tri', count: 320 },
    ]
  }
];
