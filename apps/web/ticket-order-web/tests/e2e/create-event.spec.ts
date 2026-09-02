import { expect, type Page, test } from '@playwright/test';
import {
  adminLoginUser,
  loginAs,
  managerLoginUser,
  mockSuccessfulLogin,
} from './support/authRoutes';
import { mockCreateEvent } from './support/createEventRoutes';
import { mockPublishedEvents } from './support/eventRoutes';

async function fillRequiredFields(page: Page): Promise<void> {
  await page.getByLabel('Name', { exact: true }).fill('Summer music night');
  await page.getByLabel('Date', { exact: true }).fill('2026-09-10T19:00');
  await page.getByLabel('Type', { exact: true }).fill('CONCERT');
  await page.getByLabel('Place', { exact: true }).fill('Central Hall');
  await page
    .getByLabel('Description', { exact: true })
    .fill('Outdoor concert with reserved seating');
  await page.getByLabel('Places', { exact: true }).fill('120');
  await page.getByLabel('Rows', { exact: true }).fill('12');
  await page.getByLabel('Per row', { exact: true }).fill('10');
}

test('a manager can create an event from the nav link', async ({ page }) => {
  await mockSuccessfulLogin(page, managerLoginUser);
  await mockPublishedEvents(page);
  await mockCreateEvent(page);

  await page.goto('/');
  await page.evaluate(() => {
    document.cookie = 'XSRF-TOKEN=e2e-token; Path=/';
  });
  await loginAs(page, managerLoginUser);

  await page.getByRole('link', { name: 'Create event' }).click();
  await expect(page.getByRole('heading', { name: 'Create event' })).toBeVisible();

  await fillRequiredFields(page);
  await page.getByRole('button', { name: 'Create event' }).click();

  await expect(page.getByRole('heading', { name: 'Order tickets without queues' })).toBeVisible();
  await expect(page.getByText('Event created as a draft.')).toBeVisible();
});

test('an admin can create an event from the nav link', async ({ page }) => {
  await mockSuccessfulLogin(page, adminLoginUser);
  await mockPublishedEvents(page);
  await mockCreateEvent(page);

  await page.goto('/');
  await page.evaluate(() => {
    document.cookie = 'XSRF-TOKEN=e2e-token; Path=/';
  });
  await loginAs(page, adminLoginUser);

  await page.getByRole('link', { name: 'Create event' }).click();
  await expect(page.getByRole('heading', { name: 'Create event' })).toBeVisible();

  await fillRequiredFields(page);
  await page.getByRole('button', { name: 'Create event' }).click();

  await expect(page.getByRole('heading', { name: 'Order tickets without queues' })).toBeVisible();
  await expect(page.getByText('Event created as a draft.')).toBeVisible();
});

test('a customer visiting the create-event URL directly is redirected home', async ({ page }) => {
  await mockPublishedEvents(page);

  await page.goto('/events/create');

  await expect(page.getByRole('heading', { name: 'Order tickets without queues' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Create event' })).toHaveCount(0);
  await expect(page).toHaveURL('/');
});

test('supports a keyboard-only flow to open and cancel the create-event form', async ({ page }) => {
  await mockSuccessfulLogin(page, managerLoginUser);
  await mockPublishedEvents(page);

  await page.goto('/');
  await page.evaluate(() => {
    document.cookie = 'XSRF-TOKEN=e2e-token; Path=/';
  });
  await loginAs(page, managerLoginUser);

  const createEventLink = page.getByRole('link', { name: 'Create event' });
  await createEventLink.focus();
  await page.keyboard.press('Enter');

  await expect(page.getByRole('heading', { name: 'Create event' })).toBeFocused();

  await page.keyboard.press('Tab');
  await expect(page.getByLabel('Name', { exact: true })).toBeFocused();

  const cancelLink = page.getByRole('link', { name: 'Cancel' });
  await cancelLink.focus();
  await page.keyboard.press('Enter');

  await expect(page.getByRole('heading', { name: 'Order tickets without queues' })).toBeVisible();
});
