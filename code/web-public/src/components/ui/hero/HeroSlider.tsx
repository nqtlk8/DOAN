'use client';

import { Swiper, SwiperSlide } from 'swiper/react';
import { Autoplay, EffectFade, Pagination } from 'swiper/modules';
import 'swiper/css';
import 'swiper/css/effect-fade';
import 'swiper/css/pagination';
import Link from 'next/link';
import { ArrowRight } from 'lucide-react';

const SLIDES = [
  {
    id: 1,
    image: 'https://images.unsplash.com/photo-1541888081622-19e487103a07?q=80&w=2070&auto=format&fit=crop',
    title: 'Vật Liệu Xây Dựng Chất Lượng Cao',
    subtitle: 'Nền móng vững chắc cho mọi công trình',
    cta: 'XEM BÁO GIÁ',
    link: '/products?root=vlxd'
  },
  {
    id: 2,
    image: 'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?q=80&w=2070&auto=format&fit=crop',
    title: 'Trang Trí Nội Thất Hiện Đại',
    subtitle: 'Không gian sống đẳng cấp và tinh tế',
    cta: 'KHÁM PHÁ NGAY',
    link: '/products?root=ttnt'
  }
];

export default function HeroSlider() {
  return (
    <div className="w-full relative h-[400px] md:h-[500px] lg:h-[600px]">
      <Swiper
        modules={[Autoplay, EffectFade, Pagination]}
        effect="fade"
        autoplay={{ delay: 5000, disableOnInteraction: false }}
        pagination={{ clickable: true }}
        loop={true}
        className="w-full h-full"
      >
        {SLIDES.map((slide) => (
          <SwiperSlide key={slide.id}>
            <div className="w-full h-full relative flex items-center justify-center">
              {/* Background Image */}
              <div 
                className="absolute inset-0 bg-cover bg-center"
                style={{ backgroundImage: `url(${slide.image})` }}
              />
              {/* Dark Overlay */}
              <div className="absolute inset-0 bg-black/50" />
              
              {/* Content */}
              <div className="relative z-10 text-center px-4 max-w-4xl flex flex-col items-center">
                <h2 className="text-3xl md:text-5xl lg:text-7xl font-black text-white uppercase tracking-tight mb-4 drop-shadow-lg">
                  {slide.title}
                </h2>
                <p className="text-lg md:text-2xl text-gray-200 font-medium mb-8 drop-shadow-md">
                  {slide.subtitle}
                </p>
                <Link 
                  href={slide.link}
                  className="bg-primary hover:bg-red-700 text-white px-8 py-4 rounded-full font-bold text-lg transition-all flex items-center gap-2 group"
                >
                  {slide.cta}
                  <ArrowRight className="group-hover:translate-x-1 transition-transform" />
                </Link>
              </div>
            </div>
          </SwiperSlide>
        ))}
      </Swiper>
    </div>
  );
}
