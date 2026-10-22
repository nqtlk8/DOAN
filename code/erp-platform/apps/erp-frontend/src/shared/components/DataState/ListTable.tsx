import React from 'react';
import { SearchX } from 'lucide-react';
import { ErrorState } from './ErrorState';
import { EmptyState } from './EmptyState';
import type { ApiError } from '../../errors/ApiError';

interface ListTableProps {
  /** Nội dung <tr> của thead. */
  header: React.ReactNode;
  colCount: number;
  isLoading: boolean;
  isError: boolean;
  error?: Error | ApiError | null;
  onRetry?: () => void;
  /** Tổng số bản ghi (trước khi lọc) và số bản ghi sau khi lọc. */
  totalCount: number;
  filteredCount: number;
  searchTerm?: string;
  emptyTitle?: string;
  emptyMessage?: string;
  emptyAction?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
  'data-testid'?: string;
}

/**
 * Bảng danh mục chuẩn: card > table.erp-table, header luôn hiển thị,
 * trạng thái tải / lỗi / trống / không tìm thấy nằm trong thân bảng (không lồng DataState).
 */
export const ListTable: React.FC<ListTableProps> = ({
  header,
  colCount,
  isLoading,
  isError,
  error,
  onRetry,
  totalCount,
  filteredCount,
  searchTerm,
  emptyTitle,
  emptyMessage,
  emptyAction,
  children,
  className = '',
  'data-testid': testId,
}) => {
  const stateRow = (content: React.ReactNode) => (
    <tr>
      <td colSpan={colCount} className="p-0 hover:bg-transparent">
        {content}
      </td>
    </tr>
  );

  let body: React.ReactNode = children;
  if (isError) body = stateRow(<ErrorState error={error || null} onRetry={onRetry} />);
  else if (isLoading)
    body = Array.from({ length: 5 }).map((_, i) => (
      <tr key={i} aria-hidden="true">
        <td colSpan={colCount}>
          <div className="h-3 rounded bg-slate-100 animate-pulse" />
        </td>
      </tr>
    ));
  else if (totalCount === 0) body = stateRow(<EmptyState title={emptyTitle} message={emptyMessage} action={emptyAction} />);
  else if (filteredCount === 0)
    body = stateRow(
      <div className="flex flex-col items-center gap-2 py-8 text-ink-subtle text-[13px]" data-testid="list-no-match">
        <SearchX size={24} />
        Không tìm thấy kết quả phù hợp với “{searchTerm}”
      </div>,
    );

  return (
    <div className={`card overflow-hidden ${className}`} data-testid={testId}>
      <div className="overflow-auto">
        <table className="erp-table">
          <thead className="sticky top-0 z-10">{header}</thead>
          <tbody>{body}</tbody>
        </table>
      </div>
    </div>
  );
};
