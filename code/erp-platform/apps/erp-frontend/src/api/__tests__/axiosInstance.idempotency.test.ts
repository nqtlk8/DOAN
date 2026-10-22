import type { AxiosAdapter, InternalAxiosRequestConfig } from 'axios';
import { afterEach, describe, expect, it } from 'vitest';
import axiosInstance from '../axiosInstance';

/** Adapter giả: ghi lại header Idempotency-Key của mỗi request, luôn trả 200. */
const seen: (string | undefined)[] = [];
const recordingAdapter: AxiosAdapter = async (config: InternalAxiosRequestConfig) => {
  const key = config.headers.get('Idempotency-Key');
  seen.push(typeof key === 'string' ? key : undefined);
  return { data: { data: null }, status: 200, statusText: 'OK', headers: {}, config };
};

describe('axiosInstance – Idempotency-Key', () => {
  afterEach(() => {
    seen.length = 0;
  });

  it('giữ nguyên key do form chứng từ gắn (không ghi đè bằng key dự phòng 5 giây)', async () => {
    const config = { adapter: recordingAdapter, headers: { 'Idempotency-Key': 'K-FORM' } };
    await axiosInstance.post('/api/v1/sales-invoices', { a: 1 }, config);
    await axiosInstance.post('/api/v1/sales-invoices', { a: 1 }, config);
    expect(seen).toEqual(['K-FORM', 'K-FORM']);
  });

  it('request không tự gắn key: dùng key dự phòng theo nội dung (bấm đúp trong 5 giây cùng key)', async () => {
    await axiosInstance.post('/api/v1/customers', { name: 'A' }, { adapter: recordingAdapter });
    await axiosInstance.post('/api/v1/customers', { name: 'A' }, { adapter: recordingAdapter });
    await axiosInstance.post('/api/v1/customers', { name: 'B' }, { adapter: recordingAdapter });
    expect(seen[0]).toBeTruthy();
    expect(seen[1]).toBe(seen[0]);
    expect(seen[2]).not.toBe(seen[0]);
  });

  it('GET không gắn key', async () => {
    await axiosInstance.get('/api/v1/customers', { adapter: recordingAdapter });
    expect(seen).toEqual([undefined]);
  });
});
