import React, { useCallback, useRef, useState } from 'react';
import toast from 'react-hot-toast';
import { MdiModuleLayout, type SubViewType } from '../layout/MdiModuleLayout';
import { ApiService } from '../../api/ApiService';
import { errorMessage } from '../../shared/errors/errorMessage';
import type { FormMode } from '../../types/documents';
import type { CashReceiptDto, ReceiptCustomer } from '../../types/cashReceipt';
import { CashReceiptForm, type CashReceiptFormRef } from './CashReceiptForm';
import { CashReceiptList } from './CashReceiptList';

interface CashReceiptModuleProps {
  /** Mở từ màn Công nợ ("Thu tiền"): chọn sẵn khách hàng. */
  initialCustomer?: ReceiptCustomer;
}

/** Phiếu thu: tab "Nội dung" (lập / xem / hủy phiếu) và "Danh sách phiếu". Không có Sửa, Xóa. */
export const CashReceiptModule: React.FC<CashReceiptModuleProps> = ({ initialCustomer }) => {
  const [activeSubView, setActiveSubView] = useState<SubViewType>('FORM');
  const [currentMode, setCurrentMode] = useState<FormMode>('ADD');
  const [isLoading, setIsLoading] = useState(false);
  const [canCancelReceipt, setCanCancelReceipt] = useState(false);
  const [formData, setFormData] = useState<CashReceiptDto | null>(null);
  const [formKey, setFormKey] = useState(0);
  const formRef = useRef<CashReceiptFormRef>(null);

  const handleStateChange = useCallback((mode: FormMode, loading: boolean, canCancel: boolean) => {
    setCurrentMode(mode);
    setIsLoading(loading);
    setCanCancelReceipt(canCancel);
  }, []);

  const handleRowDoubleClick = async (id: string) => {
    try {
      setIsLoading(true);
      const receipt = await ApiService.CashReceipt.getById(id);
      setFormData(receipt);
      setFormKey((k) => k + 1);
      setActiveSubView('FORM');
    } catch (err) {
      console.error(err);
      toast.error('Không thể tải phiếu thu: ' + errorMessage(err, ''));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <MdiModuleLayout
      activeSubView={activeSubView}
      onSubViewChange={setActiveSubView}
      mode={currentMode}
      isLoading={isLoading}
      onAdd={() => formRef.current?.handleAdd()}
      onSave={() => formRef.current?.handleSubmit()}
      onCancel={() => formRef.current?.handleCancel()}
      onExit={() => formRef.current?.handleExit()}
      onDelete={canCancelReceipt ? () => formRef.current?.handleCancelReceipt() : undefined}
      deleteLabel="Hủy phiếu thu"
    >
      {/* Giữ form luôn mounted: sang "Danh sách phiếu" rồi quay lại không mất dữ liệu đang nhập */}
      <div className={activeSubView === 'FORM' ? 'h-full' : 'hidden'}>
        <CashReceiptForm
          key={`${formData?.id ?? 'new'}-${formKey}`}
          ref={formRef}
          initialData={formData}
          initialCustomer={formData ? undefined : initialCustomer}
          onStateChange={handleStateChange}
        />
      </div>
      {activeSubView === 'LIST' && (
        <div className="p-4 h-full">
          <CashReceiptList onRowDoubleClick={handleRowDoubleClick} />
        </div>
      )}
    </MdiModuleLayout>
  );
};
