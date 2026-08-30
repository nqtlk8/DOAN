import React, { useEffect } from 'react';
import { Trash2, Save, X, Printer, Edit2, FilePlus, LogOut } from 'lucide-react';

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
  onPrint?: () => void;
  onExit?: () => void;
  isLoading?: boolean;
}

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
  onPrint,
  onExit,
  isLoading = false,
}) => {
  const isView = mode === 'VIEW';

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (activeSubView !== 'FORM') return; // Only trigger hotkeys in FORM view
      // Note: If there are modals open (like Print Preview), you might need an external state to skip hotkeys.

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
  }, [activeSubView, isView, onAdd, onEdit, onSave, onCancel, onDelete, onPrint, onExit]);

  return (
    <div className="flex h-full bg-erp-bg-content min-h-0">
      {/* Left Vertical Tabs (Sidebar) */}
      <div className="w-[28px] bg-erp-bg-ribbon border-r border-erp-bg-ribbon-border flex flex-col items-center shrink-0 min-h-0 overflow-y-auto overflow-x-hidden scrollbar-hide">
        <button
          onClick={() => onSubViewChange('FORM')}
          data-testid="subview-form"
          className={`w-full py-6 flex items-center justify-center border-b border-erp-bg-ribbon-border transition-none shrink-0 ${
            activeSubView === 'FORM' ? 'bg-erp-bg-content font-bold text-erp-text-primary' : 'bg-transparent hover:bg-white/40 text-slate-700'
          }`}
        >
          <span
            style={{ writingMode: 'vertical-rl', textOrientation: 'mixed', transform: 'rotate(180deg)' }}
            className="text-erp-label whitespace-nowrap overflow-visible"
          >
            Nội dung
          </span>
        </button>
        <button
          onClick={() => onSubViewChange('LIST')}
          data-testid="subview-list"
          className={`w-full py-6 flex items-center justify-center border-b border-erp-bg-ribbon-border transition-none shrink-0 ${
            activeSubView === 'LIST' ? 'bg-erp-bg-content font-bold text-erp-text-primary' : 'bg-transparent hover:bg-white/40 text-slate-700'
          }`}
        >
          <span
            style={{ writingMode: 'vertical-rl', textOrientation: 'mixed', transform: 'rotate(180deg)' }}
            className="text-erp-label whitespace-nowrap overflow-visible"
          >
            Danh sách phiếu
          </span>
        </button>
      </div>

      {/* Main Content */}
      <div className="flex-1 flex flex-col relative overflow-hidden bg-white min-w-0">
        <div className="flex-1 overflow-auto">{children}</div>

        {/* Bottom Toolbar (Sticky at bottom of main content) */}
        {activeSubView === 'FORM' && (
          <div className="h-[32px] bg-erp-bg-content text-erp-text-primary border-t border-erp-btn-border px-2 flex justify-between items-center shrink-0 overflow-x-auto scrollbar-hide">
            {/* Left side actions */}
            <div className="flex items-center gap-1 flex-nowrap">
              {isView && (
                <>
                  <button
                    onClick={onAdd}
                    data-testid="btn-add"
                    className="h-[24px] inline-flex items-center gap-1 px-2 bg-erp-btn-bg border border-erp-btn-border hover:bg-erp-btn-hover-bg rounded-erp text-erp-base transition-none shrink-0"
                  >
                    <FilePlus size={14} className="text-blue-600" />
                    <span>Thêm</span>
                    <span className="text-slate-500 ml-1 text-erp-label">(F2)</span>
                  </button>
                  <button
                    onClick={onEdit}
                    data-testid="btn-edit"
                    className="h-[24px] inline-flex items-center gap-1 px-2 bg-erp-btn-bg border border-erp-btn-border hover:bg-erp-btn-hover-bg rounded-erp text-erp-base transition-none shrink-0"
                  >
                    <Edit2 size={14} className="text-blue-600" />
                    <span>Sửa</span>
                    <span className="text-slate-500 ml-1 text-erp-label">(F3)</span>
                  </button>
                  <button
                    onClick={onDelete}
                    data-testid="btn-delete"
                    className="h-[24px] inline-flex items-center gap-1 px-2 bg-erp-btn-bg border border-erp-btn-border hover:bg-erp-btn-hover-bg rounded-erp text-erp-base transition-none shrink-0"
                  >
                    <Trash2 size={14} className="text-erp-text-accent-red" />
                    <span>Xóa</span>
                    <span className="text-slate-500 ml-1 text-erp-label">(F8)</span>
                  </button>
                </>
              )}
              {!isView && (
                <>
                  <button
                    onClick={onSave}
                    data-testid="btn-save"
                    disabled={isLoading}
                    className="h-[24px] inline-flex items-center gap-1 px-2 bg-erp-btn-bg border border-erp-btn-border hover:bg-erp-btn-hover-bg rounded-erp text-erp-base transition-none shrink-0 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <Save size={14} className="text-blue-600" />
                    <span>Lưu</span>
                    <span className="text-slate-500 ml-1 text-erp-label">(F4)</span>
                  </button>
                  <button
                    onClick={onCancel}
                    data-testid="btn-cancel"
                    disabled={isLoading}
                    className="h-[24px] inline-flex items-center gap-1 px-2 bg-erp-btn-bg border border-erp-btn-border hover:bg-erp-btn-hover-bg rounded-erp text-erp-base transition-none shrink-0 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <X size={14} className="text-red-500" />
                    <span>Hủy</span>
                    <span className="text-slate-500 ml-1 text-erp-label">(Esc)</span>
                  </button>
                </>
              )}
            </div>

            {/* Right side actions */}
            <div className="flex items-center gap-1 flex-nowrap ml-4">
              <button
                onClick={onPrint}
                data-testid="btn-print"
                disabled={!isView}
                className="h-[24px] inline-flex items-center gap-1 px-2 bg-erp-btn-bg border border-erp-btn-border hover:bg-erp-btn-hover-bg rounded-erp text-erp-base transition-none shrink-0 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <Printer size={14} className="text-slate-700" />
                <span>In phiếu</span>
                <span className="text-slate-500 ml-1 text-erp-label">(F7)</span>
              </button>
              <button
                onClick={onExit}
                data-testid="btn-exit"
                className="h-[24px] inline-flex items-center gap-1 px-2 bg-erp-btn-bg border border-erp-btn-border hover:bg-erp-btn-hover-bg rounded-erp text-erp-base transition-none shrink-0"
              >
                <LogOut size={14} className="text-erp-text-accent-red" />
                <span>Thoát</span>
                <span className="text-slate-500 ml-1 text-erp-label">(F12)</span>
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
