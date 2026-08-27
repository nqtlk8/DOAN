import BreadcrumbHeader from '../../components/ui/listing/BreadcrumbHeader';
import ResultsToolbar from '../../components/ui/listing/ResultsToolbar';
import CategorySidebar from './components/CategorySidebar';
import ProductGrid from './components/ProductGrid';
import { getProductListing } from '../../features/products/api/productApi';

export default async function ProductListingPage({
  searchParams,
}: {
  searchParams: Promise<{ root?: string; sub?: string; page?: string }>;
}) {
  const resolvedParams = await searchParams;
  const rootParams = resolvedParams?.root || 'vlxd';
  const subParams = resolvedParams?.sub || '';
  const page = resolvedParams?.page ? parseInt(resolvedParams.page) : 0;

  // Lấy dữ liệu Server-Side (có ISR 60s config ở productApi)
  const listingData = await getProductListing(undefined, undefined, page, 60);

  // Mock Categories for Sidebar
  const categories = [
    { id: '1', slug: 'gach-op-lat', name: 'Gạch ốp lát', count: 120 },
    { id: '2', slug: 'thiet-bi-ve-sinh', name: 'Thiết bị vệ sinh', count: 85 },
  ];

  let pageTitle = 'Vật Liệu Xây Dựng';
  if (rootParams.toLowerCase() === 'ttnt') pageTitle = 'Trang Trí Nội Thất';
  if (subParams) {
    const activeCat = categories.find((c) => c.slug === subParams);
    if (activeCat) pageTitle = activeCat.name;
  }

  // Cấu trúc response từ backend: { success: true, data: [...] }
  // Map DTO to Product interface
  const products = (listingData.data || []).map((dto: any) => ({
    id: String(dto.id),
    category: dto.categoryName || 'Sản Phẩm',
    name: dto.name || '',
    image: '/images/products/placeholder.jpg',
    originalPrice: dto.price || 0,
    salePrice: dto.price || 0,
    discountBadge: ''
  }));

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
            totalItems={products.length} // Temporary, replace with totalElements if paginated
            currentPage={page + 1}
            limit={60}
          />
          <div>
            <ProductGrid products={products} />
          </div>
        </div>
      </div>
    </div>
  );
}
