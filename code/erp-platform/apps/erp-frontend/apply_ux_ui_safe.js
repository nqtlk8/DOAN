const fs = require('fs');
let code = fs.readFileSync('src/components/common/document/GenericDocumentForm.tsx', 'utf8');

// 1. Spacing
code = code.replace(/<div>\s*<label className="block text-xs font-medium text-slate-500 mb-1">/g, '<div className="flex flex-col gap-1.5">\n              <label className="text-xs font-semibold text-slate-600">');
code = code.replace(/<div className="flex gap-2">\s*<div className="flex-1">\s*<label className="block text-xs font-medium text-slate-500 mb-1">/g, '<div className="grid grid-cols-2 gap-4">\n              <div className="flex flex-col gap-1.5">\n                <label className="text-xs font-semibold text-slate-600">');
code = code.replace(/<\/div>\s*<div className="flex-1">\s*<label className="block text-xs font-medium text-slate-500 mb-1">/g, '</div>\n              <div className="flex flex-col gap-1.5">\n                <label className="text-xs font-semibold text-slate-600">');

// 2 & 3. Input Heights & Focus States
code = code.replace(/className="w-full px-3 py-2 bg-slate-100 border border-slate-200 rounded-lg text-sm text-slate-900"/g, 'className="w-full h-[42px] px-3.5 py-2.5 bg-slate-100 border border-slate-300 rounded-lg text-sm text-slate-900 disabled:bg-slate-50 disabled:text-slate-500 disabled:border-slate-200 cursor-not-allowed"');
code = code.replace(/className="w-full px-3 py-2 bg-slate-100 border border-slate-200 rounded-lg text-sm text-slate-900 font-semibold"/g, 'className="w-full h-[42px] px-3.5 py-2.5 bg-slate-100 border border-slate-300 rounded-lg text-sm text-slate-900 font-semibold disabled:bg-slate-50 disabled:text-slate-500 disabled:border-slate-200 cursor-not-allowed"');
code = code.replace(/className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-900 focus:ring-2 focus:ring-blue-500 disabled:bg-slate-100 disabled:text-slate-500"/g, 'className="w-full h-[42px] px-3.5 py-2.5 bg-white border border-slate-300 rounded-lg text-sm text-slate-900 focus:border-teal-500 focus:ring-4 focus:ring-teal-500/10 transition-all duration-200 disabled:bg-slate-50 disabled:text-slate-500 disabled:border-slate-200 cursor-not-allowed"');
code = code.replace(/className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-900 focus:ring-2 focus:ring-blue-500 disabled:bg-slate-100 disabled:text-slate-500 cursor-pointer"/g, 'className="w-full h-[42px] px-3.5 py-2.5 bg-white border border-slate-300 rounded-lg text-sm text-slate-900 focus:border-teal-500 focus:ring-4 focus:ring-teal-500/10 transition-all duration-200 disabled:bg-slate-50 disabled:text-slate-500 disabled:border-slate-200 disabled:cursor-not-allowed cursor-pointer"');

// Fix textarea height specifically
code = code.replace(/rows=\{2\}\n\s+className="w-full h-\[42px\]/g, 'className="w-full min-h-[42px] resize-y');

// 5. Section Headers
code = code.replace(/<h3 className="text-sm text-slate-900 font-semibold uppercase tracking-wider border-b border-slate-200 pb-2 mb-4">/g, '<h3 className="flex items-center text-sm font-bold text-slate-800 uppercase tracking-wider bg-slate-50 px-4 py-3 rounded-lg border-l-4 border-teal-500 mb-5">');

// 6. Financial Section alignment
code = code.replace(/<div className="flex justify-between items-center">\s*<label className="text-xs font-medium text-slate-500">([^<]+)<\/label>\s*<input\n\s+type="number"\n\s+disabled=\{isView\}\n\s+value=\{([^}]+)\}\n\s+onChange=\{([^}]+)\}\n\s+className="w-1\/2 px-2 py-1 bg-white border border-slate-200 rounded text-sm text-slate-900 text-right focus:ring-1 focus:ring-blue-500 disabled:bg-slate-100 disabled:text-slate-700"\n\s+\/>\s*<\/div>/g, 
  '<div className="grid grid-cols-[1fr_120px] items-center gap-4">\n              <label className="text-right text-sm font-medium text-slate-600">$1</label>\n              <input\n                type="number"\n                disabled={isView}\n                value={$2}\n                onChange={$3}\n                className="w-full h-[36px] px-3 py-1.5 bg-white border border-slate-300 rounded text-sm text-slate-900 text-right focus:border-teal-500 focus:ring-4 focus:ring-teal-500/10 transition-all duration-200 disabled:bg-slate-50 disabled:text-slate-500 disabled:border-slate-200 cursor-not-allowed"\n              />\n            </div>');

code = code.replace(/<div className="flex gap-2">\s*<div className="flex-1 flex justify-between items-center">\s*<label className="text-xs font-medium text-slate-500">([^<]+)<\/label>\s*<input\n\s+type="number"\n\s+disabled=\{isView\}\n\s+value=\{([^}]+)\}\n\s+onChange=\{([^}]+)\}\n\s+className="w-1\/2 px-2 py-1 bg-white border border-slate-200 rounded text-sm text-slate-900 text-right focus:ring-1 focus:ring-blue-500 disabled:bg-slate-100 disabled:text-slate-700"\n\s+\/>\s*<\/div>\s*<div className="flex-1 flex justify-between items-center">\s*<label className="text-xs font-medium text-slate-500">([^<]+)<\/label>\s*<input\n\s+type="number"\n\s+disabled=\{isView\}\n\s+value=\{([^}]+)\}\n\s+onChange=\{([^}]+)\}\n\s+className="w-1\/2 px-2 py-1 bg-white border border-slate-200 rounded text-sm text-slate-900 text-right focus:ring-1 focus:ring-blue-500 disabled:bg-slate-100 disabled:text-slate-700"\n\s+\/>\s*<\/div>\s*<\/div>/g,
  '<div className="grid grid-cols-2 gap-4">\n              <div className="grid grid-cols-[1fr_120px] items-center gap-4">\n                <label className="text-right text-sm font-medium text-slate-600">$1</label>\n                <input type="number" disabled={isView} value={$2} onChange={$3} className="w-full h-[36px] px-3 py-1.5 bg-white border border-slate-300 rounded text-sm text-slate-900 text-right focus:border-teal-500 focus:ring-4 focus:ring-teal-500/10 transition-all duration-200 disabled:bg-slate-50 disabled:text-slate-500 disabled:border-slate-200 cursor-not-allowed" />\n              </div>\n              <div className="grid grid-cols-[1fr_120px] items-center gap-4">\n                <label className="text-right text-sm font-medium text-slate-600">$4</label>\n                <input type="number" disabled={isView} value={$5} onChange={$6} className="w-full h-[36px] px-3 py-1.5 bg-white border border-slate-300 rounded text-sm text-slate-900 text-right focus:border-teal-500 focus:ring-4 focus:ring-teal-500/10 transition-all duration-200 disabled:bg-slate-50 disabled:text-slate-500 disabled:border-slate-200 cursor-not-allowed" />\n              </div>\n            </div>');

code = code.replace(/<div className="flex justify-between items-center pt-2 border-t border-slate-200">\s*<label className="text-xs font-medium text-slate-500">([^<]+)<\/label>\s*<div className="w-1\/2 px-2 py-1 bg-slate-100 border border-slate-200 rounded text-sm text-right font-bold text-red-600">\s*\{([^}]+)\}\s*<\/div>\s*<\/div>/g,
  '<div className="grid grid-cols-[1fr_120px] items-center gap-4 pt-4 border-t border-slate-200 mt-2">\n              <label className="text-right text-sm font-bold text-slate-800">$1</label>\n              <div className="w-full h-[36px] px-3 py-1.5 bg-red-50 border border-red-200 rounded flex items-center justify-end text-sm font-bold text-red-600">\n                {$2}\n              </div>\n            </div>');

// 7. Table Header
code = code.replace(/<tr className="bg-slate-100 border-b border-slate-200">/g, '<tr className="bg-slate-100/80 border-b border-slate-200">');
code = code.replace(/<th className="px-4 py-2 text-xs font-semibold text-slate-600 uppercase([^"]*)">/g, '<th className="px-4 py-3 text-xs font-bold text-slate-700 uppercase$1">');

// Global Teal replacement for remaining blue-500, blue-50, etc that might have been missed
code = code.replace(/bg-blue-50/g, 'bg-teal-50');
code = code.replace(/text-blue-/g, 'text-teal-');
code = code.replace(/border-blue-/g, 'border-teal-');
code = code.replace(/focus:ring-blue-/g, 'focus:ring-teal-');

fs.writeFileSync('src/components/common/document/GenericDocumentForm.tsx', code, 'utf8');
console.log('Applied robust regex transformations!');
