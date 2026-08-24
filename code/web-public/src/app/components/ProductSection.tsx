'use client';
import SectionTitle from '../../components/ui/product/SectionTitle';
import ViewAllButton from '../../components/ui/product/ViewAllButton';
import ProductCard from '../../components/ui/product/ProductCard';
import { useFeaturedProducts } from '../../features/products/hooks/useFeaturedProducts';

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
             {products.slice(0, 8).map((product) => (
               <ProductCard key={product.id} product={product} />
             ))}
           </div>
        )}
        <ViewAllButton linkTo={linkTo} />
      </div>
    </section>
  );
}
