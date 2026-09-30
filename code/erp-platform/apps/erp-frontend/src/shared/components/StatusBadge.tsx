import React from 'react';

const MAP: Record<string, { label: string; className: string }> = {
  DRAFT: { label: 'Nháp', className: 'badge-warning' },
  CONFIRMED: { label: 'Đã xác nhận', className: 'badge-success' },
  CANCELLED: { label: 'Đã hủy', className: 'badge-danger' },
};

/** Chip trạng thái chứng từ dùng chung cho form và danh sách phiếu. */
export const StatusBadge: React.FC<{ status?: string | null }> = ({ status }) => {
  if (!status) return null;
  const item = MAP[status] ?? { label: status, className: 'badge-neutral' };
  return <span className={item.className}>{item.label}</span>;
};
