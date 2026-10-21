import { act, renderHook } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ApiError } from '../../errors/ApiError';
import {
  IDEMPOTENCY_IN_PROGRESS,
  IDEMPOTENCY_KEY_REUSED,
  OutcomeUnknownError,
  cancelKey,
  confirmKey,
  documentWriteConfig,
  hasErrorCode,
  submitErrorMessage,
  submitWithKey,
  useSubmitKey,
} from '../submitKey';

const conflict = (code: string) => new ApiError({ status: 409, message: 'conflict', details: [code] });
const noResponse = () => new ApiError({ message: 'Kết nối mạng quá hạn.' });
const noSleep = () => Promise.resolve();

describe('useSubmitKey', () => {
  it('giữ nguyên key qua các lần render; renew() mới đổi key', () => {
    const { result, rerender } = renderHook(() => useSubmitKey());
    const first = result.current.key;
    rerender();
    expect(result.current.key).toBe(first);

    act(() => result.current.renew());
    expect(result.current.key).not.toBe(first);
    expect(result.current.key).toMatch(/\S{16,}/);
  });
});

describe('submitWithKey', () => {
  it('thành công ngay: gọi đúng một lần', async () => {
    const send = vi.fn().mockResolvedValue('ok');
    await expect(submitWithKey(send, { sleep: noSleep })).resolves.toBe('ok');
    expect(send).toHaveBeenCalledTimes(1);
  });

  it('IDEMPOTENCY_IN_PROGRESS: chờ rồi gửi lại cùng request cho tới khi có kết quả', async () => {
    const send = vi
      .fn()
      .mockRejectedValueOnce(conflict(IDEMPOTENCY_IN_PROGRESS))
      .mockRejectedValueOnce(conflict(IDEMPOTENCY_IN_PROGRESS))
      .mockResolvedValueOnce('kết quả của lần đầu');
    const sleep = vi.fn(noSleep);

    await expect(submitWithKey(send, { sleep, delayMs: 10 })).resolves.toBe('kết quả của lần đầu');
    expect(send).toHaveBeenCalledTimes(3);
    expect(sleep).toHaveBeenCalledWith(10);
  });

  it('IDEMPOTENCY_IN_PROGRESS quá số lần thử: ném lỗi gốc', async () => {
    const err = conflict(IDEMPOTENCY_IN_PROGRESS);
    const send = vi.fn().mockRejectedValue(err);
    await expect(submitWithKey(send, { sleep: noSleep, retries: 2 })).rejects.toBe(err);
    expect(send).toHaveBeenCalledTimes(3);
  });

  it('không có phản hồi (timeout / mất mạng): ném OutcomeUnknownError, không tự gửi lại', async () => {
    const send = vi.fn().mockRejectedValue(noResponse());
    await expect(submitWithKey(send, { sleep: noSleep })).rejects.toBeInstanceOf(OutcomeUnknownError);
    expect(send).toHaveBeenCalledTimes(1);
  });

  it('lỗi nghiệp vụ khác: ném nguyên lỗi, không gửi lại', async () => {
    const err = new ApiError({ status: 409, message: 'Không đủ hàng', details: ['INSUFFICIENT_STOCK'] });
    const send = vi.fn().mockRejectedValue(err);
    await expect(submitWithKey(send, { sleep: noSleep })).rejects.toBe(err);
    expect(send).toHaveBeenCalledTimes(1);
  });
});

describe('thông điệp và key cố định', () => {
  it('submitErrorMessage: phân biệt chưa rõ kết quả, key đã dùng, lỗi thường', () => {
    expect(submitErrorMessage(new OutcomeUnknownError(null), 'x')).toContain('không tạo phiếu trùng');
    expect(submitErrorMessage(new OutcomeUnknownError(null), 'x', 'confirm')).toContain('Bấm Xác nhận lại');
    expect(submitErrorMessage(new OutcomeUnknownError(null), 'x', 'cancel')).toContain('Bấm Hủy phiếu lại');
    expect(submitErrorMessage(conflict(IDEMPOTENCY_KEY_REUSED), 'x', 'cancel')).toContain('Mở lại phiếu');
    expect(submitErrorMessage(conflict(IDEMPOTENCY_KEY_REUSED), 'x')).toContain('đã được lưu ở lần gửi trước');
    expect(submitErrorMessage(new Error('Không đủ hàng'), 'x')).toBe('Không đủ hàng');
    expect(submitErrorMessage('', 'mặc định')).toBe('mặc định');
  });

  it('hasErrorCode chỉ đọc mã trong ApiError.details', () => {
    expect(hasErrorCode(conflict(IDEMPOTENCY_KEY_REUSED), IDEMPOTENCY_KEY_REUSED)).toBe(true);
    expect(hasErrorCode(new Error(IDEMPOTENCY_KEY_REUSED), IDEMPOTENCY_KEY_REUSED)).toBe(false);
  });

  it('key xác nhận / hủy cố định theo chứng từ', () => {
    expect(confirmKey('sales-invoice', 'abc')).toBe('sales-invoice-confirm:abc');
    expect(confirmKey('sales-invoice', 'abc')).toBe(confirmKey('sales-invoice', 'abc'));
    expect(confirmKey('goods-return', 'abc')).not.toBe(confirmKey('sales-invoice', 'abc'));
    expect(cancelKey('cash-receipt', 'r1')).toBe('cash-receipt-cancel:r1');
  });

  it('documentWriteConfig gắn header, thời gian chờ dài và tắt toast chung', () => {
    expect(documentWriteConfig('K')).toEqual({
      headers: { 'Idempotency-Key': 'K' },
      timeout: 30_000,
      skipGlobalErrorToast: true,
    });
  });
});
