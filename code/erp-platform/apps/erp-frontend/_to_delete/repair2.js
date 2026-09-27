const fs = require('fs');

const f1Path = 'D:/Docs/CodeProject/DOAN/code/erp/src/components/layout/MdiModuleLayout.tsx';
const f2Path = 'src/layout/MdiModuleLayout.tsx';

if (fs.existsSync(f1Path) && fs.existsSync(f2Path)) {
  const f1 = fs.readFileSync(f1Path, 'utf8');
  let f2 = fs.readFileSync(f2Path, 'utf8');
  
  const classRegex = /className=(?:"[^"]*"|\{[^}]*\})/g;
  const matches1 = f1.match(classRegex) || [];
  const matches2 = f2.match(classRegex) || [];
  
  if (matches1.length === matches2.length && matches1.length > 0) {
    let matchIdx = 0;
    f2 = f2.replace(classRegex, () => {
      let origClass = matches1[matchIdx++];
      return origClass
          .replace(/\bblue\b/g, 'teal')
          .replace(/\bindigo\b/g, 'amber');
    });
    fs.writeFileSync(f2Path, f2, 'utf8');
    console.log('Repaired MdiModuleLayout');
  } else {
    console.log('Mismatch:', matches1.length, matches2.length);
  }
}
