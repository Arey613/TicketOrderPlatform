import { expect, test } from '@playwright/test';
import { mockSuccessfulLogin, setCsrfCookie } from './support/authRoutes';
import { mockEventBookingFlow, mockMyOrders, mockPublishedEvents } from './support/eventRoutes';

test('shows published events and public booked places', async ({ page }) => {
  await mockPublishedEvents(page);

  await page.goto('/');

  await expect(page.getByRole('heading', { name: 'Order tickets without queues' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Upcoming events' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'The Horizon Live' }).first()).toBeVisible();
  await expect(page.getByText('Live concert with reserved places.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Row 1, place 1' })).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Row 1, place 2' })).toBeEnabled();
  await expect(page.getByRole('button', { name: 'Login to book' })).toBeDisabled();

  await page.getByRole('button', { name: 'Row 1, place 2' }).click();
  await page.getByRole('button', { name: 'Login to book' }).click();

  await expect(page.getByRole('dialog', { name: 'Login' })).toBeVisible();
});

test('opens and closes the separate login panel', async ({ page }) => {
  await mockPublishedEvents(page);

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
  await mockPublishedEvents(page);
  await mockMyOrders(page);

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

test('books an available place after login and refreshes owned orders', async ({ page }) => {
  await mockSuccessfulLogin(page);
  await mockEventBookingFlow(page);

  await page.goto('/');
  await setCsrfCookie(page);
  await page.getByRole('button', { name: 'Login' }).first().click();
  const loginDialog = page.getByRole('dialog', { name: 'Login' });
  await page.getByLabel('Email').fill('buyer@example.com');
  await page.getByLabel('Password').fill('correct-password');
  await loginDialog.getByRole('button', { name: 'Login' }).click();

  await page.getByRole('button', { name: 'Row 1, place 2' }).click();
  await page.getByRole('button', { name: 'Book selected place' }).click();

  await expect(page.getByText('Place booked.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Row 1, place 2' })).toBeDisabled();
  await expect(page.getByText('Row 1, place 2')).toBeVisible();
});
