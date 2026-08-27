import Link from 'next/link';

interface CategoryItemProps {
  name: string;
  count: number;
  slug: string;
  isActive: boolean;
  rootParams: string; // "VLXD" or "TTNT"
}

export default function CategoryItem({ name, count, slug, isActive, rootParams }: CategoryItemProps) {
  return (
    <Link 
      href={`/products?root=${rootParams}&sub=${slug}`}
      className={`flex justify-between items-center py-4 border-b-2 border-gray-200 transition-none ${
        isActive ? 'font-black text-accent border-accent' : 'text-foreground font-bold hover:bg-slate-100 hover:text-accent'
      }`}
    >
      <span className="text-lg uppercase">{name}</span>
      <span className="text-md text-primary font-bold">({count})</span>
    </Link>
  );
}
