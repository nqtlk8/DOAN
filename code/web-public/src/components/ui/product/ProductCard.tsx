export interface Product {
  id: string;
  category: string;
  name: string;
  image: string;
  originalPrice: number;
  salePrice: number;
  discountBadge: string;
}

export default function ProductCard({ product }: { product: Product }) {
  // Hàm format tiền tệ
  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN').format(price) + 'đ';
  };

  return (
    <div className="flex flex-col bg-white border border-gray-200 rounded-xl hover:border-primary hover:shadow-lg transition-all duration-300 h-full relative p-4 group">
      {/* Product Image Area */}
      <div className="relative aspect-square w-full bg-gray-50 mb-4 rounded-lg overflow-hidden">
        <img 
          src={product.image} 
          alt={product.name} 
          loading="lazy"
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
        />
        {product.discountBadge && (
          <div className="absolute top-2 left-2 bg-red-600/90 backdrop-blur-sm text-white text-xs font-bold px-2.5 py-1 rounded-md uppercase z-10 border border-red-500 shadow-sm">
            {product.discountBadge}
          </div>
        )}
      </div>

      {/* Product Info Area */}
      <div className="flex flex-col flex-grow">
        <div className="text-xs text-primary font-bold uppercase tracking-wider mb-2">
          {product.category}
        </div>
        <div className="text-base font-bold text-gray-800 leading-snug mb-4 line-clamp-2 min-h-[40px] group-hover:text-primary transition-colors">
          {product.name}
        </div>
        
        <div className="flex flex-col mb-4 mt-auto">
          {product.originalPrice > product.salePrice && (
            <span className="text-xs text-gray-400 line-through font-semibold">
              {formatPrice(product.originalPrice)}
            </span>
          )}
          <span className="text-xl font-black text-red-600">
            {formatPrice(product.salePrice)}
          </span>
        </div>

        <button className="w-full bg-white text-primary py-2.5 border-2 border-primary rounded-lg text-sm font-bold uppercase hover:bg-primary hover:text-white transition-colors mt-auto">
          MUA NGAY
        </button>
      </div>
    </div>
  );
}
