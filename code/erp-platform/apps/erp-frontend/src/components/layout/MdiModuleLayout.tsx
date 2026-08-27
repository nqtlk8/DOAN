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
    <div className="flex h-full bg-slate-100">
      {/* Left Vertical Tabs (Sidebar) */}
      <div className="w-[26px] bg-[#e2e8f0] border-r border-[#999] flex flex-col items-center shrink-0">
        <button
          onClick={() => onSubViewChange('FORM')}
          className={`w-full py-8 flex items-center justify-center border-b border-[#cbd5e1] hover:bg-[#cbd5e1] ${
            activeSubView === 'FORM' ? 'bg-white font-bold text-[#0f172a]' : 'text-slate-700'
          }`}
        >
          <span
            style={{ writingMode: 'vertical-rl', transform: 'rotate(180deg)' }}
            className="text-[12px] whitespace-nowrap"
          >
            Nội dung
          </span>
        </button>
        <button
          onClick={() => onSubViewChange('LIST')}
          className={`w-full py-8 flex items-center justify-center border-b border-[#cbd5e1] hover:bg-[#cbd5e1] ${
            activeSubView === 'LIST' ? 'bg-white font-bold text-[#0f172a]' : 'text-slate-700'
          }`}
        >
          <span
            style={{ writingMode: 'vertical-rl', transform: 'rotate(180deg)' }}
            className="text-[12px] whitespace-nowrap"
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
          <div className="h-[48px] bg-[#e2e8f0] text-[#0f172a] border-t border-[#999] px-[12px] flex justify-between items-center shrink-0">
            {/* Left side actions */}
            <div className="flex items-center gap-[8px]">
              {isView && (
                <>
                  <button
                    onClick={onAdd}
                    className="h-[32px] inline-flex items-center justify-center gap-[6px] px-3 bg-white border border-[#999] hover:bg-[#e2e8f0] rounded-[2px] text-[13px] font-medium transition-colors"
                  >
                    <FilePlus size={16} /> Thêm (F2)
                  </button>
                  <button
                    onClick={onEdit}
                    className="h-[32px] inline-flex items-center justify-center gap-[6px] px-3 bg-white border border-[#999] hover:bg-[#e2e8f0] rounded-[2px] text-[13px] font-medium transition-colors"
                  >
                    <Edit2 size={16} /> Sửa (F3)
                  </button>
                  <button
                    onClick={onDelete}
                    className="h-[32px] inline-flex items-center justify-center gap-[6px] px-3 bg-white border border-[#999] hover:bg-red-50 hover:text-red-600 rounded-[2px] text-[13px] font-medium transition-colors"
                  >
                    <Trash2 size={16} /> Xóa (F8)
                  </button>
                </>
              )}
              {!isView && (
                <>
                  <button
                    onClick={onSave}
                    disabled={isLoading}
                    className="h-[32px] inline-flex items-center justify-center gap-[6px] px-4 bg-teal-600 border border-teal-700 text-white hover:bg-teal-700 rounded-[2px] text-[13px] font-medium transition-colors disabled:opacity-50"
                  >
                    <Save size={16} /> Lưu (F4)
                  </button>
                  <button
                    onClick={onCancel}
                    disabled={isLoading}
                    className="h-[32px] inline-flex items-center justify-center gap-[6px] px-3 bg-white border border-[#999] hover:bg-[#e2e8f0] rounded-[2px] text-[13px] font-medium transition-colors disabled:opacity-50"
                  >
                    <X size={16} /> Hủy (Esc)
                  </button>
                </>
              )}
            </div>

            {/* Right side actions */}
            <div className="flex items-center gap-[8px]">
              <button
                onClick={onPrint}
                disabled={!isView}
                className="h-[32px] inline-flex items-center justify-center gap-[6px] px-3 bg-white border border-[#999] hover:bg-[#e2e8f0] rounded-[2px] text-[13px] font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <Printer size={16} /> In (F7)
              </button>
              <button
                onClick={onExit}
                className="h-[32px] inline-flex items-center justify-center gap-[6px] px-3 bg-white border border-[#999] hover:bg-red-50 hover:text-red-600 rounded-[2px] text-[13px] font-medium transition-colors"
              >
                <LogOut size={16} /> Thoát (F12)
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
