const fs = require('fs');
const files = [
  'tests/sales-list.spec.ts',
  'tests/sales-create.spec.ts',
  'tests/login.spec.ts',
  'tests/customer.spec.ts',
  'tests/dashboard.spec.ts',
  'tests/products.spec.ts'
];
files.forEach(f => {
  if (fs.existsSync(f)) {
    let content = fs.readFileSync(f, 'utf8');
    content = content.replace(/role: "admin"/g, 'role: "ADMIN"');
    content = content.replace(/role: 'admin'/g, "role: 'ADMIN'");
    content = content.replace(/role: "sales"/g, 'role: "STAFF"');
    content = content.replace(/role: 'sales'/g, "role: 'STAFF'");
    fs.writeFileSync(f, content);
  }
});
