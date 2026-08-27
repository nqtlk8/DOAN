const fs = require('fs');
const path = require('path');

function getFiles(dir, files = []) {
  if (!fs.existsSync(dir)) return files;
  const list = fs.readdirSync(dir);
  for (const file of list) {
    const fullPath = path.join(dir, file);
    if (fs.statSync(fullPath).isDirectory()) {
      getFiles(fullPath, files);
    } else if (fullPath.endsWith('.tsx') || fullPath.endsWith('.ts')) {
      files.push(fullPath);
    }
  }
  return files;
}

const erpDir = 'D:/Docs/CodeProject/DOAN/code/erp/src/components';
const frontendDir = 'src/components';

const files = getFiles(frontendDir);
let changedFiles = 0;

for (const file of files) {
  const relPath = path.relative(frontendDir, file);
  const origPath = path.join(erpDir, relPath);
  
  if (fs.existsSync(origPath)) {
    const f1 = fs.readFileSync(origPath, 'utf8');
    let f2 = fs.readFileSync(file, 'utf8');
    
    const classRegex = /className=(?:"[^"]*"|\{[^}]*\})/g;
    const matches1 = f1.match(classRegex) || [];
    const matches2 = f2.match(classRegex) || [];
    
    if (matches1.length === matches2.length && matches1.length > 0) {
      // Replace all classNames in f2 with the transformed ones from f1
      let matchIdx = 0;
      f2 = f2.replace(classRegex, () => {
        let origClass = matches1[matchIdx++];
        // Transform blue -> teal and indigo -> amber
        // Only replace word boundaries to avoid breaking other words
        let newClass = origClass
            .replace(/\bblue\b/g, 'teal')
            .replace(/\bindigo\b/g, 'amber');
        return newClass;
      });
      
      fs.writeFileSync(file, f2, 'utf8');
      changedFiles++;
    }
  }
}
console.log('Successfully repaired and themed classes in ' + changedFiles + ' files.');
