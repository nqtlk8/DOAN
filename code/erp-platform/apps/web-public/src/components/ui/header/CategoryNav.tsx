import Link from 'next/link';
import { useCategoryNav } from '../../../features/categories/hooks/useCategoryNav';
import { ChevronDown, Box, Home, Zap } from 'lucide-react';

export default function CategoryNav() {
  const { data: rootCategories = [], isLoading } = useCategoryNav();

  if (isLoading) {
    return <div className="flex justify-start gap-4 h-12 items-center bg-red-600/50 w-1/3 animate-pulse rounded-md ml-4"></div>;
  }

  return (
    <nav className="flex items-center">
      {/* Home Tab */}
      <Link 
        href="/"
        className="px-6 py-4 text-white font-bold uppercase tracking-wider text-sm flex items-center gap-2 hover:bg-white/10 transition-colors"
      >
        <Home size={18} /> TRANG CHỦ
      </Link>

      {/* Mega Menu Dropdown */}
      <div className="group relative">
        <button className="px-6 py-4 text-white font-bold uppercase tracking-wider text-sm flex items-center gap-2 hover:bg-white/10 transition-colors bg-white/5 border-b-4 border-white">
          <Box size={18} /> DANH MỤC SẢN PHẨM <ChevronDown size={18} className="group-hover:rotate-180 transition-transform" />
        </button>
        
        {/* Dropdown Content */}
        <div className="absolute top-full left-0 w-[600px] bg-white border border-gray-200 shadow-2xl rounded-b-xl opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 z-50 p-6">
          <div className="grid grid-cols-2 gap-6">
            {rootCategories.map((root, i) => (
              <div key={i} className="flex flex-col">
                <h4 className="text-primary font-black uppercase mb-3 flex items-center gap-2 border-b border-gray-100 pb-2">
                  <Zap size={16} /> {root.name}
                </h4>
                <ul className="flex flex-col gap-2">
                  {root.subCategories.map((sub, j) => (
                    <li key={j}>
                      <Link 
                        href={`/products?root=${root.slug}&sub=${sub.slug}`}
                        className="text-gray-600 font-medium hover:text-primary transition-colors text-sm flex items-center before:content-[''] before:w-1.5 before:h-1.5 before:rounded-full before:bg-gray-300 before:mr-2 hover:before:bg-primary"
                      >
                        {sub.name}
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>
      </div>

      <Link 
        href="/about"
        className="px-6 py-4 text-white font-bold uppercase tracking-wider text-sm hover:bg-white/10 transition-colors"
      >
        VỀ CHÚNG TÔI
      </Link>
      <Link 
        href="/contact"
        className="px-6 py-4 text-white font-bold uppercase tracking-wider text-sm hover:bg-white/10 transition-colors"
      >
        LIÊN HỆ
      </Link>
    </nav>
  );
}
