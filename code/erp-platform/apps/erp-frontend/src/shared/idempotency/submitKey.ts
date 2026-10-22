import { useCallback, useState } from 'react';
import { ApiError } from '../errors/ApiError';
import { errorMessage } from '../errors/errorMessage';

/**
 * Chống tạo trùng chứng từ (hóa đơn, phiếu trả, phiếu nhập, phiếu thu).
 *
 * Mỗi lần lập phiếu có MỘT Idempotency-Key, tạo khi mở form "Thêm mới" và giữ nguyên qua mọi lần bấm Lưu,
 * kể cả gửi lại sau khi mạng lỗi hoặc hết thời gian chờ. Backend (IdempotencyAspect + cột request_key) chỉ
 * xử lý key đó một lần và trả lại đúng chứng từ đã tạo cho các lần gửi sau.
 * Bước xác nhận / hủy dùng key cố định theo id chứng từ (confirmKey / cancelKey).
 */

export const IDEMPOTENCY_HEADER = 'Idempotency-Key';
/** Backend: request đầu tiên với key này vẫn đang chạy. */
export const IDEMPOTENCY_IN_PROGRESS = 'IDEMPOTENCY_IN_PROGRESS';
/** Backend: key đã được dùng cho nội dung khác (lần gửi trước đã lưu với nội dung cũ). */
export const IDEMPOTENCY_KEY_REUSED = 'IDEMPOTENCY_KEY_REUSED';
/** Thời gian chờ cho request ghi chứng từ: dài hơn mặc định (10 s) để ít gặp tình huống "chưa rõ kết quả". */
export const DOCUMENT_REQUEST_TIMEOUT_MS = 30_000;

export const newIdempotencyKey = (): string =>
  typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
    ? crypto.randomUUID()
    : `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}-${Math.random().toString(36).slice(2)}`;

export type DocumentKind = 'sales-invoice' | 'goods-return' | 'inbound-receipt' | 'cash-receipt';

/** Key xác nhận cố định theo chứng từ: bấm Xác nhận nhiều lần chỉ tác động kho / công nợ một lần. */
export const confirmKey = (kind: DocumentKind, id: string): string => `${kind}-confirm:${id}`;
/** Key hủy cố định theo chứng từ. */
export const cancelKey = (kind: DocumentKind, id: string): string => `${kind}-cancel:${id}`;
/**
 * Key sửa phiếu nháp: một key cho một lần sửa (bấm "Sửa" -> renew). Bấm Lưu lại sau lỗi mạng gửi cùng key,
 * nên backend chỉ ghi đè một lần.
 */
export const updateKey = (kind: DocumentKind, id: string, editSession: string): string =>
  `${kind}-update:${id}:${editSession}`;

/** Backend: chứng từ không còn ở trạng thái cho phép (vd đã xác nhận, không sửa được nữa). */
export const INVALID_STATUS = 'INVALID_STATUS';

/**
 * Key của lần lập phiếu đang mở. Gọi `renew()` khi bắt đầu phiếu mới ("Thêm mới"); KHÔNG gọi lại khi lưu lỗi,
 * để lần bấm Lưu tiếp theo vẫn gửi cùng key.
 */
export function useSubmitKey(): { key: string; renew: () => void } {
  const [key, setKey] = useState<string>(newIdempotencyKey);
  const renew = useCallback(() => setKey(newIdempotencyKey()), []);
  return { key, renew };
}

/** Mã lỗi backend trong `ApiResponse.errors` (ApiError.details). */
export function errorCodes(err: unknown): string[] {
  if (!(err instanceof ApiError)) return [];
  return Array.isArray(err.details) ? err.details.filter((c): c is string => typeof c === 'string') : [];
}

export const hasErrorCode = (err: unknown, code: string): boolean => errorCodes(err).includes(code);

/** Không nhận được phản hồi (hết thời gian chờ, mất mạng): chưa biết server đã lưu hay chưa. */
export const isNoResponse = (err: unknown): boolean => err instanceof ApiError && err.status === undefined;

/** Lỗi khi gửi chứng từ mà không biết kết quả. Gửi lại với cùng key là an toàn. */
export class OutcomeUnknownError extends Error {
  readonly original: unknown;

  constructor(original: unknown) {
    super(
      'Mất kết nối trong lúc lưu nên chưa rõ phiếu đã được lưu hay chưa. Bấm Lưu lại: hệ thống sẽ không tạo phiếu trùng.',
    );
    this.name = 'OutcomeUnknownError';
    this.original = original;
  }
}

export interface SubmitOptions {
  /** Số lần gửi lại khi backend báo request trước còn đang chạy. */
  retries?: number;
  delayMs?: number;
  sleep?: (ms: number) => Promise<void>;
}

const defaultSleep = (ms: number) => new Promise<void>((resolve) => setTimeout(resolve, ms));

/**
 * Gửi một request ghi chứng từ (đã gắn key). Nếu backend báo request trước với cùng key đang chạy thì chờ rồi
 * gửi lại cùng key: khi request trước xong, backend trả đúng kết quả của nó. Không có phản hồi thì ném
 * {@link OutcomeUnknownError} để form báo người dùng bấm Lưu lại.
 */
export async function submitWithKey<T>(send: () => Promise<T>, options: SubmitOptions = {}): Promise<T> {
  const { retries = 5, delayMs = 1500, sleep = defaultSleep } = options;
  for (let attempt = 0; ; attempt++) {
    try {
      return await send();
    } catch (err) {
      if (hasErrorCode(err, IDEMPOTENCY_IN_PROGRESS) && attempt < retries) {
        await sleep(delayMs);
        continue;
      }
      if (isNoResponse(err)) throw new OutcomeUnknownError(err);
      throw err;
    }
  }
}

/** Thao tác ghi chứng từ, để thông báo nói đúng việc người dùng cần làm lại. */
export type SubmitAction = 'save' | 'confirm' | 'cancel';

const OUTCOME_UNKNOWN_MESSAGE: Record<SubmitAction, string> = {
  save: 'Mất kết nối trong lúc lưu nên chưa rõ phiếu đã được lưu hay chưa. Bấm Lưu lại: hệ thống sẽ không tạo phiếu trùng.',
  confirm:
    'Mất kết nối trong lúc xác nhận nên chưa rõ phiếu đã được xác nhận hay chưa. Bấm Xác nhận lại: kho và công nợ không bị ghi hai lần.',
  cancel:
    'Mất kết nối trong lúc hủy nên chưa rõ phiếu đã được hủy hay chưa. Bấm Hủy phiếu lại: công nợ không bị cộng hai lần.',
};

/** Thông điệp hiển thị cho lỗi khi lưu / xác nhận / hủy chứng từ. */
export function submitErrorMessage(err: unknown, fallback: string, action: SubmitAction = 'save'): string {
  if (err instanceof OutcomeUnknownError) return OUTCOME_UNKNOWN_MESSAGE[action];
  if (action === 'save' && hasErrorCode(err, INVALID_STATUS)) {
    return 'Phiếu không còn ở trạng thái nháp (đã được xác nhận hoặc hủy) nên không sửa được nữa.';
  }
  if (hasErrorCode(err, IDEMPOTENCY_KEY_REUSED)) {
    return action === 'save'
      ? 'Phiếu này đã được lưu ở lần gửi trước (với nội dung trước khi sửa). Mở "Danh sách phiếu" để kiểm tra, hoặc bấm "Thêm mới" để lập phiếu khác.'
      : 'Thao tác này đã được gửi trước đó với nội dung khác. Mở lại phiếu để xem trạng thái mới nhất.';
  }
  return errorMessage(err, fallback);
}

/** Cấu hình axios cho request ghi chứng từ: gắn key, thời gian chờ dài, form tự báo lỗi (không toast chung). */
export const documentWriteConfig = (key: string) => ({
  headers: { [IDEMPOTENCY_HEADER]: key },
  timeout: DOCUMENT_REQUEST_TIMEOUT_MS,
  skipGlobalErrorToast: true,
});
