const fs = require('fs');
let code = fs.readFileSync('src/components/common/document/GenericDocumentForm.tsx', 'utf8');

const oldLabel = `const labelClass = "text-[13px] text-[#000] whitespace-nowrap px-[10px] text-right font-medium flex items-center justify-end";`;
const newLabels = `const labelClass = "text-[13px] text-[#000] whitespace-nowrap px-[10px] text-right font-medium flex items-center justify-end";
  const leftLabelClass = "text-[13px] text-[#333] whitespace-nowrap px-[10px] text-left font-medium flex items-center justify-start";`;

code = code.replace(oldLabel, newLabels);

code = code.replace(/<div className=\{labelClass\}>Ngày<\/div>/g, '<div className={labelClass}>Ngày</div>'); // Keep right for first row? Wait, Row 1 of left block is right-aligned in my previous script? Yes. Let's make it right-aligned.
code = code.replace(/<div className=\{labelClass\}>Lấy giá<\/div>/g, '<div className={leftLabelClass}>Lấy giá</div>');
code = code.replace(/<div className=\{labelClass\}>Kho xuất<\/div>/g, '<div className={leftLabelClass}>Kho xuất</div>');
code = code.replace(/<div className=\{labelClass\}>Nhân viên<\/div>/g, '<div className={leftLabelClass}>Nhân viên</div>');
code = code.replace(/<div className=\{labelClass\}>Người lập<\/div>/g, '<div className={leftLabelClass}>Người lập</div>');

// Format the file
fs.writeFileSync('src/components/common/document/GenericDocumentForm.tsx', code, 'utf8');
console.log('Fixed left label alignment!');
