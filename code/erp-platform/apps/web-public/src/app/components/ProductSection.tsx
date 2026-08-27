'use client';
import SectionTitle from '../../components/ui/product/SectionTitle';
import ViewAllButton from '../../components/ui/product/ViewAllButton';
import ProductCard from '../../components/ui/product/ProductCard';
import { useFeaturedProducts } from '../../features/products/hooks/useFeaturedProducts';
import type { components } from '@erp/api-contract';

type ProductResponseDto = components['schemas']['ProductResponseDto'];

interface ProductSectionProps {
  title: string;
  linkTo: string;
  rootSlug: string;
}

export default function ProductSection({ title, linkTo, rootSlug }: ProductSectionProps) {
  const { data: products = [], isLoading } = useFeaturedProducts(rootSlug);

  return (
    <section className="w-full bg-white py-12">
      <div className="container mx-auto px-4 max-w-7xl">
        <SectionTitle title={title} />
        {isLoading ? (
           <div className="h-[350px] w-full flex items-center justify-center text-primary font-bold text-xl uppercase">Đang Tải Dữ Liệu...</div>
        ) : (
           <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 md:gap-6">
             {products.slice(0, 8).map((product: ProductResponseDto) => (
               <ProductCard key={product.id} product={{
                 id: String(product.id),
                 category: product.categoryName || 'Sản Phẩm',
                 name: product.name || '',
                 image: '/images/products/placeholder.jpg',
                 originalPrice: product.price || 0,
                 salePrice: product.price || 0,
                 discountBadge: ''
               }} />
             ))}
           </div>
        )}
        <ViewAllButton linkTo={linkTo} />
      </div>
    </section>
  );
}