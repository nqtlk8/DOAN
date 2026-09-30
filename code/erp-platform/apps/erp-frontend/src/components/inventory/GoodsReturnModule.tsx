import React, { useCallback, useRef, useState } from 'react';
import toast from 'react-hot-toast';
import { errorMessage } from '../../shared/errors/errorMessage';
import { MdiModuleLayout, type SubViewType } from '../layout/MdiModuleLayout';
import { GoodsReturnForm, type GoodsReturnFormRef } from './GoodsReturnForm';
import { GoodsReturnList } from './GoodsReturnList';
import { ApiService } from '../../api/ApiService';
import type { DocStatus, FormMode, GoodsReturnResponse } from '../../types/documents';

interface GoodsReturnModuleProps {
  initialSubView?: SubViewType;
  mode?: FormMode;
  initialData?: GoodsReturnResponse | null;
}

export const GoodsReturnModule: React.FC<GoodsReturnModuleProps> = ({
  initialSubView = 'FORM',
  mode: initialMode = 'ADD',
  initialData,
}) => {
  const [activeSubView, setActiveSubView] = useState<SubViewType>(initialSubView);
  const [currentMode, setCurrentMode] = useState<FormMode>(initialMode);
  const [status, setStatus] = useState<DocStatus | undefined>(initialData?.status);
  const [isLoading, setIsLoading] = useState(false);
  const [formData, setFormData] = useState<GoodsReturnResponse | null | undefined>(initialData);
  const [formKey, setFormKey] = useState(0);
  const formRef = useRef<GoodsReturnFormRef>(null);

  const handleStateChange = useCallback((mode: FormMode, loading: boolean, st?: DocStatus) => {
    setCurrentMode(mode);
    setIsLoading(loading);
    setStatus(st);
  }, []);

  const handleRowDoubleClick = async (returnId: string) => {
    try {
      setIsLoading(true);
      const res = await ApiService.GoodsReturn.getById(returnId);
      setFormData(res ?? null);
      setCurrentMode('VIEW');
      setFormKey((k) => k + 1);
      setActiveSubView('FORM');
    } catch (err) {
      toast.error('Không tải được chi tiết phiếu trả hàng: ' + errorMessage(err, ''));
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
      hideConfirm={status !== 'DRAFT'}
      onAdd={() => formRef.current?.handleAdd()}
      onSave={() => formRef.current?.handleSubmit()}
      onCancel={() => formRef.current?.handleCancel()}
      onConfirm={() => formRef.current?.handleConfirm()}
      onExit={() => formRef.current?.handleExit()}
    >
      {/* Giữ form luôn mounted để chuyển qua "Danh sách phiếu" rồi quay lại không mất dữ liệu đang nhập */}
      <div className={activeSubView === 'FORM' ? 'h-full' : 'hidden'}>
        <GoodsReturnForm
          key={`${formData?.id ?? 'new'}-${formKey}`}
          ref={formRef}
          mode={formData?.id ? 'VIEW' : initialMode}
          initialData={formData}
          onStateChange={handleStateChange}
        />
      </div>
      {activeSubView === 'LIST' && (
        <div className="p-4 h-full">
          <GoodsReturnList onRowDoubleClick={handleRowDoubleClick} />
        </div>
      )}
    </MdiModuleLayout>
  );
};
