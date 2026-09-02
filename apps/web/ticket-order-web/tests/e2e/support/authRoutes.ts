import { expect, type Page } from '@playwright/test';

export type LoginUser = {
  id: string;
  email: string;
  password: string;
  role: string;
};

export const buyerLoginUser: LoginUser = {
  id: '4b804b8d-6a6f-44d3-9b98-a2ab33f8b8c2',
  email: 'buyer@example.com',
  password: 'correct-password',
  role: 'CUSTOMER',
};

export const managerLoginUser: LoginUser = {
  id: '8bfbf6a4-1df2-4586-a874-3f88728f7695',
  email: 'manager@example.com',
  password: 'correct-password',
  role: 'MANAGER',
};

export const adminLoginUser: LoginUser = {
  id: 'c00cc87b-d0d0-4211-a576-a0492028f917',
  email: 'admin@example.com',
  password: 'correct-password',
  role: 'ADMIN',
};

export async function mockSuccessfulLogin(
  page: Page,
  user: LoginUser = buyerLoginUser,
): Promise<void> {
  await page.route('**/auth/csrf', async (route) => {
    await route.fulfill({
      status: 204,
    });
  });
  await page.route('**/auth/login', async (route) => {
    await expect(route.request().postDataJSON()).toEqual({
      login: user.email,
      password: user.password,
    });
    await expect(route.request().headers()['x-xsrf-token']).toBe('e2e-token');

    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        id: user.id,
        email: user.email,
        role: user.role,
        enabled: true,
      }),
    });
  });
  await page.route('**/auth/logout', async (route) => {
    await route.fulfill({ status: 204 });
  });
}

export async function loginAs(page: Page, user: LoginUser): Promise<void> {
  await page.getByRole('button', { name: 'Login' }).first().click();
  const loginDialog = page.getByRole('dialog', { name: 'Login' });
  await page.getByLabel('Email').fill(user.email);
  await page.getByLabel('Password').fill(user.password);
  await loginDialog.getByRole('button', { name: 'Login' }).click();
  await expect(page.getByText(user.email)).toBeVisible();
}

export async function setCsrfCookie(page: Page): Promise<void> {
  await page.evaluate(() => {
    document.cookie = 'XSRF-TOKEN=e2e-token; Path=/';
  });
}
