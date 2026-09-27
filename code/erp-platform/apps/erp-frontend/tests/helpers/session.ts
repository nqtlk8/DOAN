import type { Page } from '@playwright/test';

/** JWT giả: AuthContext yêu cầu payload.sub là UUID hợp lệ, nếu không sẽ xóa phiên. */
export function fakeJwt(sub = '11111111-1111-4111-8111-111111111111'): string {
  const b64 = (o: object) => Buffer.from(JSON.stringify(o)).toString('base64url');
  return `${b64({ alg: 'HS256', typ: 'JWT' })}.${b64({ sub })}.signature`;
}

/** Giả lập đã đăng nhập (gọi TRƯỚC page.goto). */
export async function loginAs(page: Page, role: 'ADMIN' | 'STAFF', username = role === 'ADMIN' ? 'admin' : 'staff_tp1') {
  await page.addInitScript(
    ([user, token]) => {
      localStorage.setItem('user', user);
      localStorage.setItem('access_token', token);
    },
    [JSON.stringify({ username, role }), fakeJwt()] as const,
  );
}
