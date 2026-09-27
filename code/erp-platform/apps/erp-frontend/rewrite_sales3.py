import re

with open('src/components/sales/SalesOrderForm.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("import { GenericDocumentForm, type OrderItem } from '../common/document/GenericDocumentForm';", "import { GenericDocumentForm, type OrderItem } from '../common/document/GenericDocumentForm';\nimport { QuickCreateCustomer } from '../common/quick-create/QuickCreateCustomer';\nimport { QuickCreateProduct } from '../common/quick-create/QuickCreateProduct';\nimport { formatCurrency } from '../../shared/utils/format';")

start_idx = content.find('<GenericDocumentForm')
end_idx = content.find('{/* Print Preview Modal */}', start_idx)

new_form = """<GenericDocumentForm
          mode={mode}
          docTitle="Phiếu bán hàng"
          docCode={orderCode || 'AUTO-GENERATE'}
          status={initialData?.status}
          error={error}
          errors={fieldErrors}
          info={[
            { key: 'branch', label: 'Kho xuất', value: user?.branchName || 'CN Trung tâm' },
            { key: 'priceList', label: 'Bảng giá', value: 'Bán hàng theo khách' },
            { key: 'creator', label: 'Nhân viên', value: user?.username || '' }
          ]}
          createdDate={createdDate ?? ''}
          onCreatedDateChange={setCreatedDate}
          partner={{
            label: 'Khách hàng',
            required: true,
            displayName: `${customerName} ${customerCode ? `(${customerCode})` : ''}`,
            onAdvancedSearch: () => setShowCustomerSearch(true),
            renderCombobox: (hasError) => (
              <SearchableCombobox
                data-testid="sales-customer-combo"
                value={customerName}
                placeholder="Nhập mã, tên hoặc SĐT khách hàng..."
                error={hasError}
                fetchData={ApiService.Catalog.searchCustomers}
                columns={[
                  { header: 'MÃ KH', field: 'customerCode', width: '90px' },
                  { header: 'TÊN KH', field: 'name', width: '1fr' },
                  { header: 'ĐIỆN THOẠI', field: 'phone', width: '110px' },
                  { header: 'ĐỊA CHỈ', field: 'address', width: '30%' }
                ]}
                onSelect={async (customer) => {
                  setCustomerCode(customer.customerCode || customer.customerId || '');
                  setCustomerId(customer.id);
                  setCustomerName(customer.name);
                  setAddress(customer.address || '');
                  setPhone(customer.phone || '');
                  setContactPerson(customer.contactPerson || '');
                  try {
                    const debt = await ApiService.Debt.getBalance(customer.id);
                    setOldDebt(debt || 0);
                  } catch (e) {
                    console.error('Failed to fetch debt', e);
                    setOldDebt(0);
                  }
                }}
                onCreateNew={user?.role === 'admin' ? () => setShowCustomerSearch(true) : undefined}
                renderCreateNew={(closeModal) => null /* TODO */}
              />
            ),
            fields: [
              { key: 'contact', label: 'Người liên hệ', value: contactPerson, onChange: setContactPerson },
              { key: 'phone', label: 'Điện thoại', value: phone, onChange: setPhone },
              { key: 'address', label: 'Địa chỉ', value: address, onChange: setAddress },
              { key: 'note', label: 'Ghi chú', value: note, onChange: setNote }
            ]
          }}
          summary={[
            { key: 'oldDebt', label: 'Nợ trước', value: oldDebt, onChange: setOldDebt, testId: 'sum-old-debt' },
            { key: 'total', label: 'Tiền hàng', value: totalAmount, testId: 'sum-total' },
            { key: 'discount', label: 'Chiết khấu', value: discount, onChange: setDiscount, testId: 'sum-discount' },
            { key: 'tax', label: 'VAT', value: tax, onChange: setTax, testId: 'sum-tax' },
            { key: 'advance', label: 'Trả trước', value: advancePayment, onChange: setAdvancePayment, testId: 'sum-advance' },
            { key: 'invoiceRemaining', label: 'Cần của đơn', value: invoiceRemaining, tone: 'primary', strong: true, testId: 'sum-invoice-remaining' },
            { key: 'newDebt', label: 'Nợ tổng mới', value: remainingBalance, tone: 'danger', strong: true, testId: 'sum-new-debt' },
          ]}
          lines={{
            testIdPrefix: 'sales',
            items: items,
            priceLabel: 'Đơn giá',
            onAdd: () => setItems([...items, { id: Date.now().toString() + Math.random(), productId: '', productName: '', quantity: 1, unitPrice: 0 }]),
            onRemove: (id) => setItems(items.filter(i => i.id !== id)),
            onUpdate: (id, field, value) => updateItem(id, field as any, value),
            renderProductCombobox: (line, index, hasError) => (
              <SearchableCombobox
                data-testid={`sales-product-combo-${line.id}`}
                value={line.productName}
                placeholder="Nhấn để chọn..."
                error={hasError}
                fetchData={ApiService.Catalog.searchProducts}
                columns={[
                  { header: 'MÃ', field: 'code', width: '90px' },
                  { header: 'TÊN', field: 'name', width: '1fr' },
                  { header: 'ĐVT', field: 'baseUnit', width: '70px' },
                  { header: 'GIÁ', field: 'price', width: '120px', format: (val) => formatCurrency(val || 0) }
                ]}
                onSelect={(product) => {
                  updateItem(line.id, 'productId', String(product.id));
                  updateItem(line.id, 'productCode', product.code || '');
                  updateItem(line.id, 'productName', product.name);
                  updateItem(line.id, 'unitPrice', Number(product.price ?? 0));
                  updateItem(line.id, 'unitOfMeasure', product.baseUnit || 'CAI');
                  
                  setTimeout(() => {
                    try {
                      const inputs = document.querySelectorAll(`[data-testid="sales-line-quantity"]`);
                      const input = inputs[index] as HTMLInputElement;
                      if (input) input.focus();
                    } catch (e) {}
                  }, 50);
                }}
                onCreateNew={user?.role === 'admin' ? () => setShowProductSearch(true) : undefined}
                renderCreateNew={() => null}
              />
            )
          }}
          onAdd={handleAdd}
          onEdit={handleEdit}
          onDelete={handleDelete}
          onConfirm={handleConfirm}
          onPrint={handlePrint}
          onSave={handleSubmit}
          onCancel={handleCancel}
          onExit={handleExit}
          hideConfirm={initialData?.status === 'CONFIRMED'}
          isLoading={createMutation.isPending || confirmMutation.isPending}
        />

        """

if start_idx != -1 and end_idx != -1:
    content = content[:start_idx] + new_form + content[end_idx:]

with open('src/components/sales/SalesOrderForm.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
