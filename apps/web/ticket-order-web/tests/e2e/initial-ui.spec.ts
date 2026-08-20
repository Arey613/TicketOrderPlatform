import { expect, test } from '@playwright/test';

test('shows the public ticketing page and static event previews', async ({ page }) => {
  await page.goto('/');

  await expect(page.getByRole('heading', { name: 'Order tickets without queues' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Upcoming events' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'The Horizon Live' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'City Hoops Finals' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Laugh Out Loud' })).toBeVisible();
});

test('opens and closes the separate login panel', async ({ page }) => {
  await page.goto('/');

  await page.getByRole('button', { name: 'Login' }).first().click();
  await expect(page.getByRole('dialog', { name: 'Login' })).toBeVisible();
  await expect(page.getByLabel('Email')).toBeVisible();
  await expect(page.getByLabel('Password')).toBeVisible();

  await page.keyboard.press('Escape');
  await expect(page.getByRole('dialog', { name: 'Login' })).toBeHidden();
});

test('logs in and logs out through mocked backend calls', async ({ page }) => {
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

  await page.goto('/');
  await page.evaluate(() => {
    document.cookie = 'XSRF-TOKEN=e2e-token; Path=/';
  });
  await page.getByRole('button', { name: 'Login' }).first().click();
  const loginDialog = page.getByRole('dialog', { name: 'Login' });
  await page.getByLabel('Email').fill('buyer@example.com');
  await page.getByLabel('Password').fill('correct-password');
  await loginDialog.getByRole('button', { name: 'Login' }).click();

  await expect(page.getByText('buyer@example.com')).toBeVisible();
  await expect(page.getByText('CUSTOMER')).toBeVisible();
  await expect
    .poll(() => page.evaluate(() => localStorage.getItem('ticketOrderPlatform.currentUser')))
    .toContain('buyer@example.com');

  await page.getByRole('button', { name: 'Logout' }).click();

  await expect(page.getByText('buyer@example.com')).toBeHidden();
  await expect(page.getByRole('button', { name: 'Login' }).first()).toBeVisible();
  await expect
    .poll(() => page.evaluate(() => localStorage.getItem('ticketOrderPlatform.currentUser')))
    .toBeNull();
});
