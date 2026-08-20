import { expect, type Page } from '@playwright/test';

export async function mockSuccessfulLogin(page: Page): Promise<void> {
  await page.route('**/auth/csrf', async (route) => {
    await route.fulfill({
      status: 204,
    });
  });
  await page.route('**/auth/login', async (route) => {
    await expect(route.request().postDataJSON()).toEqual({
      login: 'buyer@example.com',
      password: 'correct-password',
    });
    await expect(route.request().headers()['x-xsrf-token']).toBe('e2e-token');

    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        id: '4b804b8d-6a6f-44d3-9b98-a2ab33f8b8c2',
        email: 'buyer@example.com',
        role: 'CUSTOMER',
        enabled: true,
      }),
    });
  });
  await page.route('**/auth/logout', async (route) => {
    await route.fulfill({ status: 204 });
  });
}

export async function setCsrfCookie(page: Page): Promise<void> {
  await page.evaluate(() => {
    document.cookie = 'XSRF-TOKEN=e2e-token; Path=/';
  });
}
