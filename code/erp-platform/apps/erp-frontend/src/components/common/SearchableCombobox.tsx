import React, { useState, useEffect, useRef } from 'react';
import { Search, X } from 'lucide-react';

export interface ColumnDef {
  header: string;
  field: string;
  width?: string;
  format?: (value: any) => string | React.ReactNode;
}

export interface SearchableComboboxProps<T> {
  value: string;
  placeholder?: string;
  disabled?: boolean;
  fetchData: (query: string) => Promise<T[]>;
  columns: ColumnDef[];
  onSelect: (item: T) => void;
  renderEmpty?: () => React.ReactNode;
  autoFocus?: boolean;
}

export function SearchableCombobox<T extends Record<string, any>>({
  value,
  placeholder = 'Tìm kiếm...',
  disabled = false,
  fetchData,
  columns,
  onSelect,
  renderEmpty,
  autoFocus = false
}: SearchableComboboxProps<T>) {
  const [isOpen, setIsOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<T[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(-1);
  const wrapperRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  useEffect(() => {
    if (autoFocus && inputRef.current && !disabled) {
      inputRef.current.focus();
    }
  }, [autoFocus, disabled]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const executeSearch = async (searchQuery: string) => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    abortControllerRef.current = new AbortController();

    setLoading(true);
    try {
      const data = await fetchData(searchQuery);
      setResults(data);
      setSelectedIndex(-1);
    } catch (err: any) {
      if (err.name !== 'CanceledError' && err.name !== 'AbortError') {
        console.error('Search error:', err);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleFocus = () => {
    if (!disabled) {
      setIsOpen(true);
      executeSearch(query);
    }
  };

  const searchTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setQuery(val);
    setIsOpen(true);
    
    if (searchTimeoutRef.current) {
      clearTimeout(searchTimeoutRef.current);
    }
    
    searchTimeoutRef.current = setTimeout(() => {
      executeSearch(val);
    }, 300);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!isOpen) {
      if (e.key === 'ArrowDown' || e.key === 'Enter') {
        setIsOpen(true);
        executeSearch(query);
      }
      return;
    }

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setSelectedIndex(prev => (prev < results.length - 1 ? prev + 1 : prev));
        break;
      case 'ArrowUp':
        e.preventDefault();
        setSelectedIndex(prev => (prev > 0 ? prev - 1 : -1));
        break;
      case 'Enter':
        e.preventDefault();
        if (selectedIndex >= 0 && selectedIndex < results.length) {
          onSelect(results[selectedIndex]);
          setIsOpen(false);
          setQuery('');
        }
        break;
      case 'Escape':
        e.preventDefault();
        setIsOpen(false);
        break;
    }
  };

  return (
    <div className="relative w-full" ref={wrapperRef}>
      <div className="relative">
        <input
          ref={inputRef}
          type="text"
          value={isOpen ? query : value}
          onChange={handleChange}
          onFocus={handleFocus}
          onKeyDown={handleKeyDown}
          disabled={disabled}
          placeholder={placeholder}
          className="w-full pl-3 pr-8 py-1 text-sm border border-slate-300 rounded focus:outline-none focus:ring-1 focus:ring-teal-500 disabled:bg-slate-100 disabled:cursor-not-allowed"
        />
        {isOpen && query && (
          <button
            type="button"
            onClick={() => { setQuery(''); inputRef.current?.focus(); }}
            className="absolute right-2 top-1.5 text-slate-400 hover:text-slate-600"
          >
            <X size={14} />
          </button>
        )}
        {!isOpen && (
          <Search size={14} className="absolute right-2 top-1.5 text-slate-400" />
        )}
      </div>

      {isOpen && (
        <div className="absolute z-50 w-full min-w-[400px] max-h-64 mt-1 bg-white border border-slate-200 rounded shadow-lg overflow-y-auto">
          {loading ? (
            <div className="p-4 text-center text-sm text-slate-500">Đang tìm kiếm...</div>
          ) : results.length > 0 ? (
            <table className="w-full text-left border-collapse text-sm">
              <thead className="sticky top-0 bg-slate-50 border-b border-slate-200 shadow-sm">
                <tr>
                  {columns.map((col, idx) => (
                    <th key={idx} className="px-3 py-2 font-medium text-slate-600" style={{ width: col.width }}>
                      {col.header}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {results.map((item, idx) => (
                  <tr
                    key={idx}
                    onClick={() => {
                      onSelect(item);
                      setIsOpen(false);
                      setQuery('');
                    }}
                    onMouseEnter={() => setSelectedIndex(idx)}
                    className={`cursor-pointer border-b border-slate-50 last:border-0 ${
                      selectedIndex === idx ? 'bg-teal-50' : 'hover:bg-slate-50'
                    }`}
                  >
                    {columns.map((col, cIdx) => (
                      <td key={cIdx} className="px-3 py-2 text-slate-700">
                        {col.format ? col.format(item[col.field]) : item[col.field]}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <div className="p-4 text-center text-sm text-slate-500">
              {renderEmpty ? renderEmpty() : 'Không tìm thấy kết quả.'}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
