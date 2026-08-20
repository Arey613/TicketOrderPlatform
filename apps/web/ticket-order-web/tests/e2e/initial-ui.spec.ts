import { expect, test } from '@playwright/test';
import { mockSuccessfulLogin, setCsrfCookie } from './support/authRoutes';

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
  await mockSuccessfulLogin(page);

  await page.goto('/');
  await setCsrfCookie(page);
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
