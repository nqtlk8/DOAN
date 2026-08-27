const fs = require('fs');
let code = fs.readFileSync('src/components/layout/MdiModuleLayout.tsx', 'utf8');

// Replace the action bar
const newActionBar = `<div className="h-[48px] bg-[#E3E9F0] text-slate-800 border-t border-[#999] px-2 flex justify-between items-center shrink-0">
            {/* Left side actions */}
            <div className="flex items-center gap-[8px]">
              {isView && (
                <>
                  <button onClick={onAdd} className="h-[32px] inline-flex items-center justify-center gap-[6px] px-3 bg-white border border-[#999] hover:bg-[#e0e0e0] rounded-sm text-[12px] font-medium transition-colors">
                    <Plus size={14} /> Thêm (F2)
                  </button>
                  <button onClick={onEdit} className="h-[32px] inline-flex items-center justify-center gap-[6px] px-3 bg-white border border-[#999] hover:bg-[#e0e0e0] rounded-sm text-[12px] font-medium transition-colors">
                    <Edit size={14} /> Sửa (F3)
                  </button>
                  <button onClick={onDelete} className="h-[32px] inline-flex items-center justify-center gap-[6px] px-3 bg-white border border-[#999] hover:bg-red-50 hover:text-red-600 rounded-sm text-[12px] font-medium transition-colors">
                    <Trash2 size={14} /> Xóa (F8)
                  </button>
                </>
              )}
              {!isView && (
                <>
                  <button onClick={onSave} disabled={isLoading} className="h-[32px] inline-flex items-center justify-center gap-[6px] px-4 bg-blue-600 border border-blue-700 text-white hover:bg-blue-700 rounded-sm text-[12px] font-medium transition-colors disabled:opacity-50">
                    <Save size={14} /> Lưu (F4)
                  </button>
                  <button onClick={onCancel} disabled={isLoading} className="h-[32px] inline-flex items-center justify-center gap-[6px] px-3 bg-white border border-[#999] hover:bg-[#e0e0e0] rounded-sm text-[12px] font-medium transition-colors disabled:opacity-50">
                    <X size={14} /> Hủy (Esc)
                  </button>
                </>
              )}
            </div>

            {/* Right side actions */}
            <div className="flex items-center gap-[8px] pr-[12px]">
              <button onClick={onPrint} disabled={!isView} className="h-[32px] inline-flex items-center justify-center gap-[6px] px-3 bg-white border border-[#999] hover:bg-[#e0e0e0] rounded-sm text-[12px] font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
                <Printer size={14} /> In (F7)
              </button>
              <button onClick={onExit} className="h-[32px] inline-flex items-center justify-center gap-[6px] px-3 bg-white border border-[#999] hover:bg-red-50 hover:text-red-600 rounded-sm text-[12px] font-medium transition-colors">
                <LogOut size={14} /> Thoát (F12)
              </button>
            </div>
          </div>`;

code = code.replace(/<div className="h-12 bg-slate-800 text-slate-200 px-4 flex justify-between items-center shrink-0">[\s\S]*?<\/div>\s*<\/div>\s*<\/div>\s*<\/div>/, newActionBar + '\n        </div>\n      </div>\n    </div>');
fs.writeFileSync('src/components/layout/MdiModuleLayout.tsx', code, 'utf8');
console.log('Applied footer changes!');
