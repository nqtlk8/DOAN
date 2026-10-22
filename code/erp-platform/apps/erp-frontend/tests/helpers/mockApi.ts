import type { Page, Route } from '@playwright/test';

export const ok = (data: unknown) => ({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data }) });

/** Đăng ký fallback TRƯỚC (Playwright ưu tiên route đăng ký sau). */
export async function mockApi(page: Page, routes: Record<string, (route: Route) => unknown>) {
  await page.route('**/api/v1/**', (route) => route.fulfill(ok([])));
  for (const [pattern, handler] of Object.entries(routes)) {
    await page.route(pattern, handler as (route: Route) => Promise<void>);
  }
}
