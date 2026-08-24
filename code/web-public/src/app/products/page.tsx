'use client';
import { useSearchParams } from 'next/navigation';
import BreadcrumbHeader from '../../components/ui/listing/BreadcrumbHeader';
import ResultsToolbar from '../../components/ui/listing/ResultsToolbar';
import CategorySidebar from './components/CategorySidebar';
import ProductGrid from './components/ProductGrid';
import { useCategorySidebar } from '../../features/categories/hooks/useCategorySidebar';
import { useProductListing } from '../../features/products/hooks/useProductListing';

export default function ProductListingPage() {
  const searchParams = useSearchParams();
  const rootParams = searchParams?.get('root') || 'vlxd';
  const subParams = searchParams?.get('sub');

  const { data: categories = [] } = useCategorySidebar(rootParams);
  const { data: listingData, isFetching } = useProductListing(rootParams, subParams);

  // Logic title theo subParams
  let pageTitle = 'Vật Liệu Xây Dựng';
  if (rootParams.toLowerCase() === 'ttnt') pageTitle = 'Trang Trí Nội Thất';
  if (subParams) {
    const activeCat = categories.find(c => c.slug === subParams);
    if (activeCat) pageTitle = activeCat.name;
  }

  const products = listingData?.data || [];

  return (
    <div className="container mx-auto px-4 py-8 max-w-[1300px]">
      <BreadcrumbHeader title={pageTitle} />
      
      <div className="flex flex-col md:flex-row items-start gap-10">
        <CategorySidebar 
          categories={categories} 
          activeSlug={subParams} 
          rootParams={rootParams} 
        />
        
        <div className="flex-1">
          <ResultsToolbar 
            totalItems={listingData?.totalItems || 0}
            currentPage={listingData?.currentPage || 1}
            limit={60}
          />
          <div className={`transition-opacity duration-300 ${isFetching ? 'opacity-50 pointer-events-none' : 'opacity-100'}`}>
            <ProductGrid products={products} />
          </div>
        </div>
      </div>
    </div>
  );
}
