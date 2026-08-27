import { ChangeEvent } from 'react';
import { Search } from 'lucide-react';

interface SearchBoxProps {
  keyword: string;
  onKeywordChange: (e: ChangeEvent<HTMLInputElement>) => void;
  onSearch: () => void;
}

export default function SearchBox({ keyword, onKeywordChange, onSearch }: SearchBoxProps) {
  return (
    <div className="flex w-full border-2 border-primary rounded-lg overflow-hidden bg-white h-[50px] shadow-sm hover:shadow-md transition-shadow">
      <input 
        type="text" 
        className="flex-grow px-5 py-2 outline-none text-foreground font-medium placeholder-gray-400 text-sm md:text-base"
        placeholder="Tìm kiếm vật liệu, thương hiệu..."
        value={keyword}
        onChange={onKeywordChange}
      />
      <button 
        className="bg-primary text-white px-8 h-full flex items-center justify-center font-bold text-sm md:text-base hover:bg-red-700 transition-colors gap-2"
        onClick={onSearch}
      >
        <Search strokeWidth={2.5} size={20} />
        <span className="hidden md:inline">Tìm Kiếm</span>
      </button>
    </div>
  );
}
