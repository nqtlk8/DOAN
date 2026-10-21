import { formatNumber } from '../../shared/utils/format';

/** Làm tròn 4 chữ số thập phân (khớp NUMERIC(19,4) ở backend) để so sánh tiền không lệch do số thực. */
const toUnits = (v: number) => Math.round((Number(v) || 0) * 10000);

/** Số tiền còn nợ sau khi thu = nợ hiện tại − số tiền thu. */
export const remainingAfterReceipt = (debt: number, amount: number): number =>
  (toUnits(debt) - toUnits(amount)) / 10000;

export interface ReceiptValidationInput {
  customerId: string;
  debt: number;
  amount: number;
  receiptDate: string; // yyyy-MM-dd
  today: string; // yyyy-MM-dd
}

/**
 * Kiểm tra phiếu thu trước khi gửi. Key lỗi: 'partner' (khách hàng), 'amount', 'date'.
 * Backend vẫn kiểm tra lại (khóa dòng công nợ, cấm nợ âm) — đây chỉ để báo lỗi sớm.
 */
export const validateReceipt = (input: ReceiptValidationInput): Record<string, string> => {
  const errors: Record<string, string> = {};
  if (!input.customerId) {
    errors.partner = 'Vui lòng chọn khách hàng';
  } else if (toUnits(input.debt) <= 0) {
    errors.amount = 'Khách hàng không còn nợ tại chi nhánh này';
  }
  if (!errors.amount) {
    const amount = Number(input.amount) || 0;
    if (toUnits(amount) <= 0) {
      errors.amount = 'Số tiền thu phải lớn hơn 0';
    } else if (input.customerId && toUnits(amount) > toUnits(input.debt)) {
      errors.amount = `Số tiền thu vượt số nợ hiện tại (${formatNumber(input.debt, 4)})`;
    }
  }
  if (!input.receiptDate) {
    errors.date = 'Vui lòng chọn ngày thu';
  } else if (input.receiptDate > input.today) {
    errors.date = 'Ngày thu không được sau hôm nay';
  }
  return errors;
};
