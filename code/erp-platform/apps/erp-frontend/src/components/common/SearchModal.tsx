import React, { useState, useEffect, useRef } from 'react';
import { Search, X, Loader2 } from 'lucide-react';

export interface SearchModalProps<T> {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  placeholder?: string;
  fetchData: (query: string) => Promise<T[]>;
  renderItem: (item: T) => React.ReactNode;
  onSelect: (item: T) => void;
}

export function SearchModal<T>({
  isOpen,
  onClose,
  title,
  placeholder = 'Tìm kiếm...',
  fetchData,
  renderItem,
  onSelect,
}: SearchModalProps<T>) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<T[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (isOpen) {
      setQuery('');
      setResults([]);
      // Autofocus
      setTimeout(() => inputRef.current?.focus(), 100);
      handleSearch(''); // Fetch initial
    }
  }, [isOpen]);

  const handleSearch = async (searchQuery: string) => {
    setIsLoading(true);
    try {
      const data = await fetchData(searchQuery);
      setResults(data);
    } catch (error) {
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  };

  const onSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setQuery(val);
    // basic debounce
    setTimeout(() => {
      handleSearch(val);
    }, 300);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/50 z-[200] flex items-center justify-center p-4">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg flex flex-col max-h-[80vh]">
        <div className="p-4 border-b flex justify-between items-center bg-slate-50 rounded-t-xl">
          <h2 className="text-lg font-semibold text-slate-800">{title}</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-600">
            <X size={20} />
          </button>
        </div>

        <div className="p-4 border-b">
          <div className="relative">
            <input
              ref={inputRef}
              type="text"
              placeholder={placeholder}
              value={query}
              onChange={onSearchChange}
              className="w-full pl-10 pr-4 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 text-slate-900"
            />
            <Search className="absolute left-3 top-2.5 text-slate-400" size={20} />
            {isLoading && <Loader2 className="absolute right-3 top-2.5 text-teal-500 animate-spin" size={20} />}
          </div>
        </div>

        <div className="overflow-y-auto flex-1 p-2">
          {results.length > 0 ? (
            <ul className="space-y-1">
              {results.map((item, index) => (
                <li
                  key={index}
                  onClick={() => {
                    onSelect(item);
                    onClose();
                  }}
                  className="p-3 hover:bg-teal-50 rounded-lg cursor-pointer transition-colors border border-transparent hover:border-teal-100"
                >
                  {renderItem(item)}
                </li>
              ))}
            </ul>
          ) : (
            !isLoading && <div className="text-center text-slate-500 py-8">Không tìm thấy kết quả phù hợp.</div>
          )}
        </div>
      </div>
    </div>
  );
}
