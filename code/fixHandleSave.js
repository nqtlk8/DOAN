const fs = require('fs');

let f1 = 'erp-platform/apps/erp-frontend/src/components/catalog/CustomerList.tsx';
let c1 = fs.readFileSync(f1, 'utf8');
c1 = c1.replace(/const handleSave = \(\) => \{[\s\S]*?createMutation\.mutate\(formData\);[\s\S]*?\};/, 
`const handleSave = () => {
    createMutation.mutate({
      customerCode: formData.code,
      name: formData.name,
      phone: formData.phone,
      email: formData.email,
      address: formData.address,
      taxCode: formData.taxCode,
      customerType: (formData as any).type || 'RETAIL'
    });
  };`);
fs.writeFileSync(f1, c1, 'utf8');

let f2 = 'erp-platform/apps/erp-frontend/src/components/catalog/ProductList.tsx';
let c2 = fs.readFileSync(f2, 'utf8');
c2 = c2.replace(/const handleSave = \(\) => \{[\s\S]*?createMutation\.mutate\(formData\);[\s\S]*?\};/, 
`const handleSave = () => {
    createMutation.mutate({
      productCode: formData.sku || formData.code,
      name: formData.name,
      baseUnit: formData.unit || 'CAI',
      categoryId: 1,
      description: formData.description
    });
  };`);
fs.writeFileSync(f2, c2, 'utf8');

console.log('Fixed handleSave');
