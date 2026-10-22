import { describe, it, expect } from 'vitest';
import { remainingAfterReceipt, validateReceipt } from '../cashReceipt';

const base = { customerId: 'c1', debt: 4_000_000, amount: 1_000_000, receiptDate: '2026-10-18', today: '2026-10-18' };

describe('cashReceipt — tính toán và kiểm tra phiếu thu', () => {
  it('U-PT-F01: còn lại = nợ hiện tại − số tiền thu (không lệch số thực)', () => {
    expect(remainingAfterReceipt(4_000_000, 1_000_000)).toBe(3_000_000);
    expect(remainingAfterReceipt(4_000_000, 4_000_000)).toBe(0);
    expect(remainingAfterReceipt(0.3, 0.1)).toBe(0.2);
  });

  it('U-PT-F02: phiếu hợp lệ không có lỗi (thu một phần và thu hết)', () => {
    expect(validateReceipt(base)).toEqual({});
    expect(validateReceipt({ ...base, amount: 4_000_000 })).toEqual({});
  });

  it('U-PT-F03: chưa chọn khách hàng', () => {
    expect(validateReceipt({ ...base, customerId: '' }).partner).toBe('Vui lòng chọn khách hàng');
  });

  it('U-PT-F04: số tiền thu ≤ 0', () => {
    expect(validateReceipt({ ...base, amount: 0 }).amount).toBe('Số tiền thu phải lớn hơn 0');
    expect(validateReceipt({ ...base, amount: -1 }).amount).toBe('Số tiền thu phải lớn hơn 0');
  });

  it('U-PT-F05: số tiền thu vượt số nợ hiện tại', () => {
    expect(validateReceipt({ ...base, amount: 4_000_001 }).amount).toMatch(/vượt số nợ hiện tại/);
  });

  it('U-PT-F06: khách không còn nợ', () => {
    expect(validateReceipt({ ...base, debt: 0 }).amount).toBe('Khách hàng không còn nợ tại chi nhánh này');
  });

  it('U-PT-F07: ngày thu bỏ trống hoặc sau hôm nay; ngày quá khứ hợp lệ', () => {
    expect(validateReceipt({ ...base, receiptDate: '' }).date).toBe('Vui lòng chọn ngày thu');
    expect(validateReceipt({ ...base, receiptDate: '2026-10-19' }).date).toBe('Ngày thu không được sau hôm nay');
    expect(validateReceipt({ ...base, receiptDate: '2026-10-01' }).date).toBeUndefined();
  });
});
