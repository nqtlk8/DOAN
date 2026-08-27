import ProductSection from './components/ProductSection';
import HeroSlider from '../components/ui/hero/HeroSlider';
import TrustCards from '../components/ui/hero/TrustCards';
import './page.css';

export default function HomePage() {
  return (
    <main className="w-full flex-grow flex flex-col items-center pb-12 bg-background">
      
      {/* Hero Section */}
      <HeroSlider />
      
      {/* Floating Trust Cards */}
      <TrustCards />

      {/* Main Content Area */}
      <div className="w-full max-w-7xl mx-auto px-4 flex flex-col gap-12">
        {/* Product Section: VẬT LIỆU XÂY DỰNG */}
        <ProductSection 
          title="VẬT LIỆU XÂY DỰNG MỚI" 
          linkTo="/products?root=VLXD"
          rootSlug="vlxd"
        />

        {/* Product Section: TRANG TRÍ NỘI THẤT */}
        <ProductSection 
          title="TRANG TRÍ NỘI THẤT CAO CẤP" 
          linkTo="/products?root=TTNT"
          rootSlug="ttnt"
        />
      </div>
      
    </main>
  );
}