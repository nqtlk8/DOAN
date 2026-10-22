/** Lấy thông điệp lỗi hiển thị cho người dùng từ một giá trị bị throw bất kỳ. */
export const errorMessage = (e: unknown, fallback: string): string =>
  e instanceof Error && e.message ? e.message : typeof e === 'string' && e ? e : fallback;
