import { Category, Product } from '../types';

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

export const MOCK_PRODUCTS: Product[] = [
  // VLXD
  { id: 'p1', category: 'Bàn Cầu - Bồn Tiểu', name: 'BỒN CẦU 1 KHỐI TOTO CW166RB_TC384CVK', image: 'https://images.unsplash.com/photo-1584622650111-993a426fbf0a?auto=format&fit=crop&w=500&q=80', originalPrice: 33923000, salePrice: 27941200, discountBadge: 'Giảm 18%' },
  { id: 'p2', category: 'Bồn cầu 1 khối', name: 'BỒN CẦU 1 KHỐI TOTO CW166RB_TCF33320GAA', image: 'https://images.unsplash.com/photo-1584622650111-993a426fbf0a?auto=format&fit=crop&w=500&q=80', originalPrice: 54265000, salePrice: 44787350, discountBadge: 'Giảm 17%' },
  { id: 'p3', category: 'Bồn cầu 1 khối', name: 'BỒN CẦU 1 KHỐI TOTO CW166RB_TCF34320GAA', image: 'https://images.unsplash.com/photo-1584622650111-993a426fbf0a?auto=format&fit=crop&w=500&q=80', originalPrice: 63075000, salePrice: 52063350, discountBadge: 'Giảm 17%' },
  { id: 'p4', category: 'Chậu Rửa - Lavabo', name: 'CHẬU RỬA ĐẶT BÀN TOTO LT4706', image: 'https://images.unsplash.com/photo-1550581190-9c1c48d21d6c?auto=format&fit=crop&w=500&q=80', originalPrice: 4500000, salePrice: 4000000, discountBadge: 'Giảm 11%' },
  { id: 'p5', category: 'Chậu Rửa - Lavabo', name: 'CHẬU RỬA ÂM BÀN TOTO LT548', image: 'https://images.unsplash.com/photo-1550581190-9c1c48d21d6c?auto=format&fit=crop&w=500&q=80', originalPrice: 2000000, salePrice: 1800000, discountBadge: 'Giảm 10%' },
  
  // TTNT
  { id: 't1', category: 'Đèn Trang Trí', name: 'ĐÈN CHÙM PHA LÊ CAO CẤP LUXURY L500', image: 'https://images.unsplash.com/photo-1513506003901-1e6a229e2d15?auto=format&fit=crop&w=800&q=80', originalPrice: 5500000, salePrice: 4800000, discountBadge: 'Giảm 12%' },
  { id: 't2', category: 'Rèm Cửa', name: 'RÈM VẢI CHỐNG NẮNG CHÂU ÂU CAO CẤP', image: 'https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=800&q=80', originalPrice: 1200000, salePrice: 950000, discountBadge: 'Giảm 20%' },
  { id: 't3', category: 'Sofa', name: 'SOFA DA THẬT ITALY HIỆN ĐẠI', image: 'https://images.unsplash.com/photo-1493663284031-b7e3aefcae8e?auto=format&fit=crop&w=800&q=80', originalPrice: 35000000, salePrice: 35000000, discountBadge: '' },
  { id: 't4', category: 'Bàn Trà', name: 'BÀN TRÀ MẶT ĐÁ CẨM THẠCH', image: 'https://images.unsplash.com/photo-1532323544230-7191fd51bc1b?auto=format&fit=crop&w=800&q=80', originalPrice: 4500000, salePrice: 3800000, discountBadge: 'Giảm 15%' },
  { id: 't5', category: 'Đồ Trang Trí', name: 'BÌNH HOA PHA LÊ CHÂU ÂU NGHỆ THUẬT', image: 'https://images.unsplash.com/photo-1581783898377-1c85bf937427?auto=format&fit=crop&w=800&q=80', originalPrice: 850000, salePrice: 750000, discountBadge: 'Giảm 11%' }
];
