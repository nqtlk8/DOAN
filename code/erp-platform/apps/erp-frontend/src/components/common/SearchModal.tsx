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
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const requestRef = useRef(0);

  useEffect(() => {
    if (isOpen) {
      setQuery('');
      setResults([]);
      // Autofocus
      setTimeout(() => inputRef.current?.focus(), 100);
      handleSearch(''); // Fetch initial
    } else {
      if (timerRef.current) clearTimeout(timerRef.current);
    }
  }, [isOpen]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        e.preventDefault();
        e.stopPropagation();
        onClose();
      }
    };
    if (isOpen) {
      window.addEventListener('keydown', handleKeyDown, true);
    }
    return () => window.removeEventListener('keydown', handleKeyDown, true);
  }, [isOpen, onClose]);

  const handleSearch = async (searchQuery: string) => {
    const req = ++requestRef.current; // chỉ nhận kết quả của lần tìm mới nhất
    setIsLoading(true);
    try {
      const data = await fetchData(searchQuery);
      if (req === requestRef.current) setResults(data ?? []);
    } catch (error) {
      console.error(error);
      if (req === requestRef.current) setResults([]);
    } finally {
      if (req === requestRef.current) setIsLoading(false);
    }
  };

  const onSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setQuery(val);
    
    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = setTimeout(() => {
      handleSearch(val);
    }, 250);
  };

  if (!isOpen) return null;

  return (
    <div role="dialog" aria-modal="true" aria-label={title} className="fixed inset-0 bg-black/50 z-[200] flex items-center justify-center p-4">
      <div className="card w-full max-w-lg flex flex-col max-h-[80vh] overflow-hidden" onClick={e => e.stopPropagation()}>
        <div className="h-[44px] px-4 border-b border-line flex justify-between items-center bg-slate-50">
          <h2 className="text-[15px] font-semibold text-ink">{title}</h2>
          <button type="button" onClick={onClose} aria-label="Đóng" className="text-ink-muted hover:text-ink">
            <X size={18} />
          </button>
        </div>

        <div className="p-4 border-b border-line bg-surface">
          <div className="relative">
            <input
              ref={inputRef}
              type="text"
              placeholder={placeholder}
              value={query}
              onChange={onSearchChange}
              className="erp-input h-8 pl-8 pr-8"
              data-testid="search-modal-input"
            />
            <Search className="absolute left-2.5 top-2 text-ink-subtle pointer-events-none" size={16} />
            {isLoading && <Loader2 className="absolute right-2.5 top-2 text-primary animate-spin pointer-events-none" size={16} />}
          </div>
        </div>

        <div className="overflow-y-auto flex-1 p-2 bg-surface">
          {results.length > 0 ? (
            <ul className="space-y-1">
              {results.map((item, index) => (
                <li
                  key={index}
                  data-testid="search-modal-row"
                  onClick={() => {
                    onSelect(item);
                    onClose();
                  }}
                  className="p-2.5 hover:bg-primary-soft rounded cursor-pointer transition-colors border border-transparent"
                >
                  {renderItem(item)}
                </li>
              ))}
            </ul>
          ) : (
            !isLoading && <div className="text-center text-ink-muted py-8 text-[13px]">Không tìm thấy kết quả phù hợp.</div>
          )}
        </div>
      </div>
    </div>
  );
}
