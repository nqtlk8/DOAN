const fs = require('fs');

let c = fs.readFileSync('erp-platform/apps/erp-frontend/src/components/sales/SalesOrderForm.tsx', 'utf8');

const oldHandleSubmit = /const handleSubmit = async \(\) => \{[\s\S]*?finally \{\s*setIsLoading\(false\);\s*\}\s*\};/;

const newHandleSubmit = `const handleSubmit = async () => {
      setIsLoading(true);
      setError(null);
      setFieldErrors({});
      try {
        const newErrors: Record<string, string> = {};
        if (!customerCode) {
          newErrors.partner = 'Vui lòng chọn khách hàng';
        }
        if (items.length === 0) {
          throw new Error('Đơn hàng phải có ít nhất 1 sản phẩm.');
        }

        // check items
        items.forEach((item, index) => {
          if (!item.productId) newErrors[\`item_\${index}_product\`] = 'Chọn sản phẩm';
          if (item.quantity <= 0) newErrors[\`item_\${index}_quantity\`] = 'Số lượng > 0';
        });

        if (Object.keys(newErrors).length > 0) {
          setFieldErrors(newErrors);
          throw new Error('Vui lòng kiểm tra lại thông tin nhập.');
        }

        const payload = {
          customerId: customerCode,
          paymentMethod,
          lines: items.map((i) => ({
            productId: i.productId,
            productName: i.productName,
            quantity: i.quantity,
            unitPrice: i.unitPrice,
            unitOfMeasure: 'CAI',
          })),
        };

        if (mode === 'ADD') {
          const response = await ApiService.SalesInvoice.create(payload);
          toast.success(\`Order created successfully! ID: \${response}\`);
          setOrderCode(response);
        } else {
          toast.success('Order updated successfully!');
        }

        setMode('VIEW');
      } catch (err: any) {
        console.error(err);
        toast.error(err.message || 'Lỗi khi lưu đơn hàng');
      } finally {
        setIsLoading(false);
      }
    };`;

c = c.replace(oldHandleSubmit, newHandleSubmit);
fs.writeFileSync('erp-platform/apps/erp-frontend/src/components/sales/SalesOrderForm.tsx', c, 'utf8');
console.log("Done");
