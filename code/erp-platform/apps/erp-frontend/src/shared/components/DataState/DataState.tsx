import React from 'react';
import { LoadingState, TableSkeleton } from './LoadingState';
import { ErrorState } from './ErrorState';
import { EmptyState } from './EmptyState';
import { ApiError } from '../../errors/ApiError';

interface DataStateProps {
  isLoading: boolean;
  isError: boolean;
  error?: Error | ApiError | null;
  isEmpty: boolean;
  onRetry?: () => void;
  loadingType?: 'spinner' | 'table' | 'card';
  emptyTitle?: string;
  emptyMessage?: string;
  emptyAction?: React.ReactNode;
  children: React.ReactNode;
}

export const DataState: React.FC<DataStateProps> = ({
  isLoading,
  isError,
  error,
  isEmpty,
  onRetry,
  loadingType = 'spinner',
  emptyTitle,
  emptyMessage,
  emptyAction,
  children
}) => {
  if (isError) {
    return <ErrorState error={error || null} onRetry={onRetry} />;
  }

  if (isLoading) {
    if (loadingType === 'table') {
      return <TableSkeleton />;
    }
    // TODO: Thêm các skeleton khác như card nếu cần
    return <LoadingState />;
  }

  if (isEmpty) {
    return (
      <EmptyState 
        title={emptyTitle} 
        message={emptyMessage} 
        action={emptyAction} 
      />
    );
  }

  return <>{children}</>;
};
