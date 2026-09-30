import React, { useState, useEffect, useLayoutEffect, useRef, useCallback, useId } from 'react';
import { createPortal } from 'react-dom';
import { Search, X, Loader2 } from 'lucide-react';
import { escapeRegExp } from '../../shared/utils/format';

export interface ColumnDef {
  header: string;
  field: string;
  width?: string;
  align?: 'left' | 'right';
  format?: (value: any, row: any) => React.ReactNode;
  highlight?: boolean;
}

export interface SearchableComboboxProps<T> {
  value: string;
  placeholder?: string;
  disabled?: boolean;
  fetchData: (query: string) => Promise<T[]>;
  columns: ColumnDef[];
  onSelect: (item: T) => void;
  renderEmpty?: (query: string) => React.ReactNode;
  onCreateNew?: (query: string) => void;
  createLabel?: string;
  autoFocus?: boolean;
  error?: boolean;
  variant?: 'field' | 'cell';
  inputRef?: React.Ref<HTMLInputElement>;
  minDropdownWidth?: number;
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
  onCreateNew,
  createLabel = 'Thêm mới',
  autoFocus = false,
  error = false,
  variant = 'field',
  inputRef,
  minDropdownWidth = 560,
  'data-testid': testId,
}: SearchableComboboxProps<T>) {
  const [isOpen, setIsOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<T[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [fetchError, setFetchError] = useState<string | null>(null);

  const wrapperRef = useRef<HTMLDivElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const localInputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  
  const requestRef = useRef<number>(0);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const listboxId = useId();

  // Giữ fetchData mới nhất trong ref: nơi gọi thường truyền arrow function inline,
  // nếu đưa vào dependency thì mỗi lần component cha render lại sẽ gọi API lại và mất kết quả đang lọc.
  const fetchDataRef = useRef(fetchData);
  useLayoutEffect(() => {
    fetchDataRef.current = fetchData;
  }, [fetchData]);

  const [dropdownStyle, setDropdownStyle] = useState<React.CSSProperties>({ position: 'fixed', visibility: 'hidden' });

  const actualInputRef = (inputRef as React.RefObject<HTMLInputElement>) || localInputRef;

  const loadData = useCallback(async (searchQuery: string) => {
    const currentReq = ++requestRef.current;
    setLoading(true);
    setFetchError(null);
    try {
      const data = (await fetchDataRef.current(searchQuery)) ?? [];
      if (currentReq === requestRef.current) {
        setResults(data);
        setSelectedIndex(data.length > 0 ? 0 : -1);
      }
    } catch {
      if (currentReq === requestRef.current) {
        setFetchError('Không tải được dữ liệu');
        setResults([]);
      }
    } finally {
      if (currentReq === requestRef.current) {
        setLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    if (isOpen) {
      loadData('');
    } else {
      if (timerRef.current) clearTimeout(timerRef.current);
      requestRef.current++; // bỏ qua kết quả của request đang chạy
      setQuery('');
      setResults([]);
      setSelectedIndex(-1);
      setFetchError(null);
      setLoading(false);
      setDropdownStyle({ position: 'fixed', visibility: 'hidden' });
    }
  }, [isOpen, loadData]);

  useEffect(() => () => {
    if (timerRef.current) clearTimeout(timerRef.current);
  }, []);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setQuery(val);
    if (!isOpen) setIsOpen(true);
    
    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = setTimeout(() => {
      loadData(val);
    }, 250);
  };

  const calculatePosition = useCallback(() => {
    if (!isOpen || !wrapperRef.current || !dropdownRef.current) return;
    const rect = wrapperRef.current.getBoundingClientRect();
    // Ô nhập đã bị cuộn ra khỏi màn hình → đóng dropdown thay vì để nó "lơ lửng".
    if (rect.bottom < 0 || rect.top > window.innerHeight) {
      setIsOpen(false);
      return;
    }
    const minWidth = Math.max(rect.width, minDropdownWidth);
    
    let left = rect.left;
    if (left + minWidth > window.innerWidth) {
      left = Math.max(8, window.innerWidth - minWidth - 8);
    }

    const spaceAbove = rect.top;
    const spaceBelow = window.innerHeight - rect.bottom;
    const maxDropdownHeight = 312; // header 32 + 10 * 28

    let top: number;
    let bottom: 'auto' | number = 'auto';
    let maxHeight = Math.min(maxDropdownHeight, Math.max(spaceAbove, spaceBelow) - 12);

    if (spaceBelow < maxDropdownHeight + 8 && spaceAbove > spaceBelow) {
      // open upward
      top = -1; // unused
      bottom = window.innerHeight - rect.top + 4;
      maxHeight = Math.min(maxDropdownHeight, spaceAbove - 12);
    } else {
      // open downward
      top = rect.bottom + 4;
      maxHeight = Math.min(maxDropdownHeight, spaceBelow - 12);
    }

    setDropdownStyle({
      position: 'fixed',
      left: `${left}px`,
      top: top !== -1 ? `${top}px` : 'auto',
      bottom: bottom !== 'auto' ? `${bottom}px` : 'auto',
      width: `${minWidth}px`,
      maxHeight: `${Math.max(maxHeight, 120)}px`,
      zIndex: 300,
    });
  }, [isOpen, minDropdownWidth]);

  useEffect(() => {
    calculatePosition();
    window.addEventListener('resize', calculatePosition);
    window.addEventListener('scroll', calculatePosition, true);
    
    return () => {
      window.removeEventListener('resize', calculatePosition);
      window.removeEventListener('scroll', calculatePosition, true);
    };
  }, [calculatePosition, results, loading, fetchError]);

  useEffect(() => {
    const handleOutsideClick = (e: MouseEvent) => {
      if (
        isOpen &&
        wrapperRef.current &&
        !wrapperRef.current.contains(e.target as Node) &&
        dropdownRef.current &&
        !dropdownRef.current.contains(e.target as Node)
      ) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleOutsideClick);
    return () => document.removeEventListener('mousedown', handleOutsideClick);
  }, [isOpen]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (!isOpen) {
      if (e.key === 'ArrowDown' || e.key === 'Enter') {
        setIsOpen(true);
      }
      return;
    }

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setSelectedIndex(prev => {
          const next = prev < results.length - 1 ? prev + 1 : prev;
          scrollToIndex(next);
          return next;
        });
        break;
      case 'ArrowUp':
        e.preventDefault();
        setSelectedIndex(prev => {
          const next = prev > 0 ? prev - 1 : 0;
          scrollToIndex(next);
          return next;
        });
        break;
      case 'Enter':
      case 'Tab':
        if (selectedIndex >= 0 && selectedIndex < results.length) {
          if (e.key === 'Enter') e.preventDefault(); // allow tab to move focus
          onSelect(results[selectedIndex]);
          setIsOpen(false);
        }
        break;
      case 'Escape':
        e.preventDefault();
        e.stopPropagation();
        setIsOpen(false);
        break;
    }
  };

  const scrollToIndex = (index: number) => {
    if (!listRef.current) return;
    const row = listRef.current.children[index + 1] as HTMLElement; // +1 because of header
    if (row) {
      row.scrollIntoView({ block: 'nearest' });
    }
  };

  const highlightText = (text: unknown, search: string) => {
    if (text === null || text === undefined || text === '') return '';
    const str = String(text);
    const term = search.trim();
    if (!term) return str;
    // Escape ký tự đặc biệt: gõ "(", "+", "[" không được làm sập app.
    const parts = str.split(new RegExp(`(${escapeRegExp(term)})`, 'gi'));
    return (
      <>
        {parts.map((part, i) =>
          part.toLowerCase() === term.toLowerCase() ? (
            <mark key={i} className="bg-transparent font-semibold text-primary">{part}</mark>
          ) : (
            part
          )
        )}
      </>
    );
  };

  // Cột có width cố định (px/%) giữ nguyên; cột không có width (hoặc '1fr') giãn hết phần còn lại.
  const columnStyle = (col: ColumnDef): React.CSSProperties =>
    col.width && col.width !== '1fr'
      ? { width: col.width, flex: '0 0 auto' }
      : { flex: '1 1 0', minWidth: 0 };

  const displayValue = isOpen ? query : value;

  const inputClass = variant === 'cell'
    ? `h-full w-full border-0 bg-transparent px-2 focus:bg-surface focus:ring-2 focus:ring-inset focus:ring-primary/30 outline-none ${error ? 'bg-danger-soft' : ''}`
    : `erp-input ${error ? 'erp-input-error' : ''}`;

  return (
    <div ref={wrapperRef} className="relative w-full h-full">
      <div className="relative w-full h-full flex items-center">
        <input
          ref={actualInputRef}
          type="text"
          value={displayValue}
          onChange={handleInputChange}
          onFocus={() => { if (!disabled) setIsOpen(true); }}
          onClick={() => { if (!disabled && !isOpen) setIsOpen(true); }}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          disabled={disabled}
          className={inputClass}
          data-testid={testId}
          aria-expanded={isOpen}
          aria-controls={isOpen ? listboxId : undefined}
          role="combobox"
          aria-autocomplete="list"
          autoFocus={autoFocus}
        />
        {variant === 'field' && !isOpen && (
          <Search size={14} className="absolute right-2 text-ink-subtle pointer-events-none" />
        )}
        {variant === 'field' && isOpen && query && (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              setQuery('');
              actualInputRef.current?.focus();
            }}
            className="absolute right-2 text-ink-subtle hover:text-ink"
          >
            <X size={14} />
          </button>
        )}
      </div>

      {isOpen && createPortal(
        <div
          ref={dropdownRef}
          id={listboxId}
          data-testid="combobox-dropdown"
          role="listbox"
          style={dropdownStyle}
          className="bg-surface border border-line rounded-[6px] shadow-[0_10px_30px_-8px_rgb(15_23_42/0.25)] flex flex-col overflow-hidden"
        >
          {loading && results.length === 0 ? (
            <div className="flex items-center justify-center p-4 text-ink-muted text-[13px] gap-2">
              <Loader2 size={16} className="animate-spin" /> Đang tìm kiếm...
            </div>
          ) : fetchError ? (
            <div className="p-4 text-danger text-[13px]">{fetchError}</div>
          ) : results.length === 0 && !loading ? (
            <div className="p-4 flex flex-col gap-2">
              <div className="text-ink-muted text-[13px]">
                {renderEmpty ? renderEmpty(query) : `Không tìm thấy "${query}"`}
              </div>
              {onCreateNew && (
                <button
                  type="button"
                  data-testid="combobox-create-new"
                  onClick={() => onCreateNew(query)}
                  className="btn btn-primary self-start mt-2"
                >
                  + {createLabel}
                </button>
              )}
            </div>
          ) : (
            <div ref={listRef} className="overflow-auto flex-1">
              <div className="sticky top-0 bg-slate-50 flex border-b border-line h-8 z-10">
                {columns.map((col, idx) => (
                  <div
                    key={idx}
                    className={`px-2 flex items-center text-[12px] font-semibold text-ink-muted ${col.align === 'right' ? 'justify-end' : 'justify-start'}`}
                    style={columnStyle(col)}
                  >
                    {col.header}
                  </div>
                ))}
              </div>
              {results.map((row, rowIdx) => (
                <div
                  key={rowIdx}
                  role="option"
                  aria-selected={selectedIndex === rowIdx}
                  onClick={() => {
                    onSelect(row);
                    setIsOpen(false);
                  }}
                  onMouseEnter={() => setSelectedIndex(rowIdx)}
                  className={`flex h-[28px] text-[13px] cursor-pointer border-b border-line last:border-b-0 ${
                    selectedIndex === rowIdx ? 'bg-primary-soft' : 'bg-surface'
                  }`}
                >
                  {columns.map((col, colIdx) => (
                    <div
                      key={colIdx}
                      className={`px-2 flex items-center overflow-hidden whitespace-nowrap text-ellipsis ${col.align === 'right' ? 'justify-end tabular-nums' : 'justify-start'}`}
                      style={columnStyle(col)}
                    >
                      {col.format
                        ? col.format(row[col.field], row)
                        : col.highlight !== false
                          ? highlightText(row[col.field], query)
                          : row[col.field]}
                    </div>
                  ))}
                </div>
              ))}
            </div>
          )}
        </div>,
        document.body
      )}
    </div>
  );
}
