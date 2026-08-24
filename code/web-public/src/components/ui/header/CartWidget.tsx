import { ShoppingCart } from 'lucide-react';
import Link from 'next/link';

export default function CartWidget() {
  return (
    <Link href="/cart" className="flex items-center justify-center w-12 h-12 bg-white border border-gray-200 rounded-full cursor-pointer hover:bg-gray-50 hover:border-primary hover:text-primary transition-all relative shadow-sm text-gray-700">
      <ShoppingCart size={22} strokeWidth={2} />
      <span className="absolute -top-1 -right-1 bg-primary text-white text-xs font-bold w-5 h-5 flex items-center justify-center rounded-full border-2 border-white shadow-sm">
        0
      </span>
    </Link>
  );
}
