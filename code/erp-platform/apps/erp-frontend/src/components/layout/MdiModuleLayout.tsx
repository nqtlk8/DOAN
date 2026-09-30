import React, { useEffect } from 'react';
import { Trash2, Save, X, Printer, Edit2, FilePlus, LogOut, Check, Loader2 } from 'lucide-react';

export type SubViewType = 'FORM' | 'LIST';
export type FormMode = 'VIEW' | 'ADD' | 'EDIT';

export interface MdiModuleLayoutProps {
  children: React.ReactNode;
  activeSubView: SubViewType;
  onSubViewChange: (view: SubViewType) => void;
  mode?: FormMode;
  onAdd?: () => void;
  onEdit?: () => void;
  onSave?: () => void;
  onCancel?: () => void;
  onDelete?: () => void;
  onConfirm?: () => void;
  onPrint?: () => void;
  onExit?: () => void;
  isLoading?: boolean;
  hideConfirm?: boolean;
}

const Kbd = ({ children }: { children: React.ReactNode }) => (
  <kbd className="ml-0.5 font-sans text-[11px] font-normal opacity-70">{children}</kbd>
);

export const MdiModuleLayout: React.FC<MdiModuleLayoutProps> = ({
  children,
  activeSubView,
  onSubViewChange,
  mode = 'VIEW',
  onAdd,
  onEdit,
  onSave,
  onCancel,
  onDelete,
  onConfirm,
  onPrint,
  onExit,
  isLoading = false,
  hideConfirm = false,
}) => {
  const isView = mode === 'VIEW';

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.defaultPrevented) return;
      if (activeSubView !== 'FORM') return; // Chỉ bắt phím tắt khi đang ở tab "Nội dung"
      if (isLoading) return;
      // Đang mở hộp thoại (xác nhận, thêm nhanh, tìm kiếm nâng cao…) → để hộp thoại tự xử lý phím.
      if (document.querySelector('[role="dialog"]')) return;

      switch (e.key) {
        case 'F2':
          e.preventDefault();
          if (isView && onAdd) onAdd();
          break;
        case 'F3':
          e.preventDefault();
          if (isView && onEdit) onEdit();
          break;
        case 'F4':
          e.preventDefault();
          if (!isView && onSave) onSave();
          break;
        case 'Escape':
          e.preventDefault();
          if (!isView && onCancel) onCancel();
          break;
        case 'F8':
          e.preventDefault();
          if (isView && onDelete) onDelete();
          break;
        case 'F9':
          e.preventDefault();
          if (isView && onConfirm && !hideConfirm) onConfirm();
          break;
        case 'F7':
          e.preventDefault();
          if (isView && onPrint) onPrint();
          break;
        case 'F12':
          e.preventDefault();
          if (onExit) onExit();
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [activeSubView, isView, isLoading, hideConfirm, onAdd, onEdit, onSave, onCancel, onDelete, onConfirm, onPrint, onExit]);

  const tabClass = (active: boolean) =>
    `w-full py-5 flex items-center justify-center border-b border-line shrink-0 transition-colors ${
      active
        ? 'bg-surface text-primary font-semibold shadow-[inset_2px_0_0_var(--color-primary)]'
        : 'bg-transparent text-ink-muted hover:bg-surface/60'
    }`;

  return (
    <div className="flex h-full bg-surface min-h-0">
      {/* Thanh dọc bên trái: Nội dung / Danh sách phiếu */}
      <div className="w-[32px] bg-slate-50 border-r border-line flex flex-col items-center shrink-0 min-h-0 overflow-y-auto overflow-x-hidden scrollbar-hide">
        <button
          type="button"
          onClick={() => onSubViewChange('FORM')}
          data-testid="subview-form"
          aria-pressed={activeSubView === 'FORM'}
          className={tabClass(activeSubView === 'FORM')}
        >
          <span
            style={{ writingMode: 'vertical-rl', transform: 'rotate(180deg)' }}
            className="text-[12px] whitespace-nowrap"
          >
            Nội dung
          </span>
        </button>
        <button
          type="button"
          onClick={() => onSubViewChange('LIST')}
          data-testid="subview-list"
          aria-pressed={activeSubView === 'LIST'}
          className={tabClass(activeSubView === 'LIST')}
        >
          <span
            style={{ writingMode: 'vertical-rl', transform: 'rotate(180deg)' }}
            className="text-[12px] whitespace-nowrap"
          >
            Danh sách phiếu
          </span>
        </button>
      </div>

      {/* Nội dung chính */}
      <div className="flex-1 flex flex-col relative overflow-hidden bg-surface min-w-0">
        <div className="flex-1 min-h-0 overflow-auto">{children}</div>

        {/* Thanh nút dưới cùng — chỉ hiện nút có handler tương ứng */}
        {activeSubView === 'FORM' && (
          <div className="h-10 bg-surface border-t border-line px-3 flex justify-between items-center gap-3 shrink-0 overflow-x-auto scrollbar-hide">
            <div className="flex items-center gap-1.5 flex-nowrap">
              {isView && onAdd && (
                <button type="button" onClick={onAdd} data-testid="btn-add" className="btn btn-secondary shrink-0">
                  <FilePlus size={14} className="text-primary" /> Thêm <Kbd>F2</Kbd>
                </button>
              )}
              {isView && onEdit && (
                <button type="button" onClick={onEdit} data-testid="btn-edit" className="btn btn-secondary shrink-0">
                  <Edit2 size={14} className="text-primary" /> Sửa <Kbd>F3</Kbd>
                </button>
              )}
              {isView && onDelete && (
                <button type="button" onClick={onDelete} data-testid="btn-delete" className="btn btn-danger shrink-0">
                  <Trash2 size={14} /> Xóa <Kbd>F8</Kbd>
                </button>
              )}
              {isView && onConfirm && !hideConfirm && (
                <button
                  type="button"
                  onClick={onConfirm}
                  disabled={isLoading}
                  data-testid="btn-confirm"
                  className="btn btn-primary shrink-0"
                >
                  {isLoading ? <Loader2 size={14} className="animate-spin" /> : <Check size={14} />} Xác nhận <Kbd>F9</Kbd>
                </button>
              )}
              {!isView && onSave && (
                <button
                  type="button"
                  onClick={onSave}
                  data-testid="btn-save"
                  disabled={isLoading}
                  className="btn btn-primary shrink-0"
                >
                  {isLoading ? <Loader2 size={14} className="animate-spin" /> : <Save size={14} />} Lưu <Kbd>F4</Kbd>
                </button>
              )}
              {!isView && onCancel && (
                <button
                  type="button"
                  onClick={onCancel}
                  data-testid="btn-cancel"
                  disabled={isLoading}
                  className="btn btn-secondary shrink-0"
                >
                  <X size={14} className="text-danger" /> Hủy <Kbd>Esc</Kbd>
                </button>
              )}
            </div>

            <div className="flex items-center gap-1.5 flex-nowrap">
              {onPrint && (
                <button
                  type="button"
                  onClick={onPrint}
                  data-testid="btn-print"
                  disabled={!isView}
                  className="btn btn-secondary shrink-0"
                >
                  <Printer size={14} /> In phiếu <Kbd>F7</Kbd>
                </button>
              )}
              {onExit && (
                <button type="button" onClick={onExit} data-testid="btn-exit" className="btn btn-secondary shrink-0">
                  <LogOut size={14} className="text-danger" /> Thoát <Kbd>F12</Kbd>
                </button>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
