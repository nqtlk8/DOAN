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
  error?: boolean;
  'data-testid'?: string;
}

export function SearchableCombobox<T extends Record<string, any>>({
  value,
  placeholder = 'Tìm kiếm...',
  disabled = false,
  fetchData,
  columns,
  onSelect,
  renderEmpty,
  autoFocus = false,
  error = false,
  'data-testid': testId
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

    try {
      setLoading(true);
      const data = await fetchData(searchQuery);
      setResults(data || []);
      setSelectedIndex(-1);
    } catch (err: any) {
      if (err.name !== 'AbortError') {
        console.error('Combobox fetch error:', err);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!isOpen) {
      setQuery('');
      setResults([]);
    } else {
      executeSearch('');
    }
  }, [isOpen]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setQuery(val);
    if (!isOpen) setIsOpen(true);
    executeSearch(val);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (!isOpen) {
      if (e.key === 'Enter' || e.key === 'ArrowDown') {
        setIsOpen(true);
      }
      return;
    }

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedIndex(prev => (prev < results.length - 1 ? prev + 1 : prev));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedIndex(prev => (prev > 0 ? prev - 1 : prev));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (selectedIndex >= 0 && selectedIndex < results.length) {
        onSelect(results[selectedIndex]);
        setIsOpen(false);
      }
    } else if (e.key === 'Escape') {
      setIsOpen(false);
    }
  };

  return (
    <div className="relative w-full" ref={wrapperRef}>
      <div className="relative">
        <input
          data-testid={testId}
          ref={inputRef}
          type="text"
          value={isOpen ? query : value}
          onChange={handleInputChange}
          onKeyDown={handleKeyDown}
          disabled={disabled}
          placeholder={placeholder}
          className={`w-full pl-3 pr-8 py-1 text-sm border rounded focus:outline-none focus:ring-1 ${
            error 
              ? 'border-red-500 ring-red-500 focus:ring-red-500' 
              : 'border-slate-300 focus:ring-teal-500'
          } disabled:bg-slate-100 disabled:cursor-not-allowed`}
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
