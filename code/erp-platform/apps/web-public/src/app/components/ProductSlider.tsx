'use client';
import { Swiper, SwiperSlide } from 'swiper/react';
import 'swiper/css';
import ProductCard, { Product } from '../../components/ui/product/ProductCard';

interface ProductSliderProps {
  products: Product[];
}

export default function ProductSlider({ products }: ProductSliderProps) {
  return (
    <div className="w-full relative">
      <Swiper
        spaceBetween={20}
        slidesPerView={1}
        breakpoints={{
          640: { slidesPerView: 2 },
          768: { slidesPerView: 3 },
          1024: { slidesPerView: 4 },
        }}
        className="pb-4"
      >
        {products.map((product) => (
          <SwiperSlide key={product.id} className="h-auto">
            <ProductCard product={product} />
          </SwiperSlide>
        ))}
      </Swiper>
    </div>
  );
}
