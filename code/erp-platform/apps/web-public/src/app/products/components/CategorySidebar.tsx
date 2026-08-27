import CategoryItem from '../../../components/ui/listing/CategoryItem';

export interface SubCategory {
  id: string;
  name: string;
  slug: string;
  count: number;
}

interface CategorySidebarProps {
  categories: SubCategory[];
  activeSlug: string | null;
  rootParams: string;
}

export default function CategorySidebar({ categories, activeSlug, rootParams }: CategorySidebarProps) {
  return (
    <div className="w-full md:w-[300px] flex-shrink-0 bg-white border-[3px] border-border p-4">
      <h3 className="font-black font-rubik text-xl text-foreground mb-4 uppercase tracking-wide border-b-2 border-primary pb-2">Danh Mục</h3>
      <div className="flex flex-col">
        {categories.map(cat => (
          <CategoryItem 
            key={cat.id}
            name={cat.name}
            count={cat.count}
            slug={cat.slug}
            isActive={activeSlug === cat.slug}
            rootParams={rootParams}
          />
        ))}
      </div>
    </div>
  );
}
