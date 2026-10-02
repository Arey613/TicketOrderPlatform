import { expect, test } from '@playwright/test';
import { buyerLoginUser, setCsrfCookie } from './support/authRoutes';
import {
  mockMyOrdersCancellation,
  myOrdersPageResponse,
  ownedOrderId,
  ownedOrderResponseFor,
} from './support/eventRoutes';

test('opens and dismisses customer order cancellation without sending delete', async ({ page }) => {
  const ordersRoute = await mockMyOrdersCancellation(page);
  await seedCurrentUser(page);

  await page.goto('/orders/mine');
  await setCsrfCookie(page);

  await expect(page.getByRole('heading', { name: 'My orders' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Refresh' })).toBeHidden();
  await page.getByRole('button', { name: 'Cancel order for The Horizon Live' }).click();

  const dialog = page.getByRole('dialog', { name: 'Cancel order?' });
  await expect(dialog).toBeVisible();
  await expect(
    dialog.getByText('This removes your booking and makes the place available again.'),
  ).toBeVisible();
  await expect(dialog.getByText('The Horizon Live, row 1, place 2')).toBeVisible();
  await expect(dialog.getByRole('button', { name: 'Keep order' })).toBeFocused();

  await dialog.getByRole('button', { name: 'Keep order' }).click();

  await expect(dialog).toBeHidden();
  await expect(page.getByRole('heading', { name: 'The Horizon Live' })).toBeVisible();
  expect(ordersRoute.deleteRequests).toEqual([]);
});

test('keeps the dialog open when the event can no longer be cancelled', async ({ page }) => {
  await mockMyOrdersCancellation(page, { fail: true, failStatus: 409 });
  await seedCurrentUser(page);

  await page.goto('/orders/mine');
  await setCsrfCookie(page);
  await page.getByRole('button', { name: 'Cancel order for The Horizon Live' }).click();
  await page
    .getByRole('dialog', { name: 'Cancel order?' })
    .getByRole('button', { name: 'Cancel order' })
    .click();

  const dialog = page.getByRole('dialog', { name: 'Cancel order?' });
  await expect(dialog).toBeVisible();
  await expect(dialog).toContainText('This order can no longer be cancelled.');
});

test('cancels a customer order with a singleton request body and removes it from my orders', async ({
  page,
}) => {
  const ordersRoute = await mockMyOrdersCancellation(page, {
    pages: {
      0: myOrdersPageResponse([ownedOrderResponseFor()], 0, 20, 1),
    },
  });
  await seedCurrentUser(page);

  await page.goto('/orders/mine');
  await setCsrfCookie(page);
  await page.getByRole('button', { name: 'Cancel order for The Horizon Live' }).click();

  const deleteResponse = page.waitForResponse(
    (response) =>
      response.request().method() === 'DELETE' &&
      new URL(response.url()).pathname === '/orders/mine',
  );

  await page
    .getByRole('dialog', { name: 'Cancel order?' })
    .getByRole('button', { name: 'Cancel order' })
    .click();

  await expect((await deleteResponse).status()).toBe(204);
  await expect.poll(() => ordersRoute.deleteRequests).toHaveLength(1);
  expect(ordersRoute.deleteRequests[0].body).toEqual({ eventOrderIds: [ownedOrderId] });
  expect(new URL(ordersRoute.deleteRequests[0].url).pathname).toBe('/orders/mine');
  await expect(page.getByRole('heading', { name: 'The Horizon Live' })).toBeHidden();
  await expect(page.getByText('No upcoming orders yet.')).toBeVisible();
});

test('keeps a customer order visible and reports an accessible cancellation failure', async ({
  page,
}) => {
  await mockMyOrdersCancellation(page, { fail: true });
  await seedCurrentUser(page);

  await page.goto('/orders/mine');
  await setCsrfCookie(page);
  await page.getByRole('button', { name: 'Cancel order for The Horizon Live' }).click();
  await page
    .getByRole('dialog', { name: 'Cancel order?' })
    .getByRole('button', { name: 'Cancel order' })
    .click();

  await expect(page.getByRole('heading', { name: 'The Horizon Live' })).toBeVisible();
  await expect(page.getByRole('dialog', { name: 'Cancel order?' })).toContainText(
    'Order could not be cancelled',
  );
});

test('requests the previous page after cancelling the only order on the last page', async ({
  page,
}) => {
  const ordersRoute = await mockMyOrdersCancellation(page, {
    pages: {
      0: myOrdersPageResponse(earlierOrders(), 0, 20, 21),
      1: myOrdersPageResponse([ownedOrderResponseFor()], 1, 20, 21),
    },
  });
  await seedCurrentUser(page);

  await page.goto('/orders/mine');
  await setCsrfCookie(page);
  await page.getByRole('button', { name: 'Next' }).click();

  await expect(page.getByText('Page 2 of 2')).toBeVisible();
  await page.getByRole('button', { name: 'Cancel order for The Horizon Live' }).click();
  await page
    .getByRole('dialog', { name: 'Cancel order?' })
    .getByRole('button', { name: 'Cancel order' })
    .click();

  await expect.poll(() => ordersRoute.getPageNumbers).toContain(0);
  await expect(page.getByText('Page 1 of 2')).toBeVisible();
  await expect(
    page.getByRole('heading', { name: 'Earlier Arena Show 1', exact: true }),
  ).toBeVisible();
});

test('stays on page zero and shows the empty state after cancelling the only first-page order', async ({
  page,
}) => {
  const ordersRoute = await mockMyOrdersCancellation(page);
  await seedCurrentUser(page);

  await page.goto('/orders/mine');
  await setCsrfCookie(page);
  await page.getByRole('button', { name: 'Cancel order for The Horizon Live' }).click();
  await page
    .getByRole('dialog', { name: 'Cancel order?' })
    .getByRole('button', { name: 'Cancel order' })
    .click();

  await expect
    .poll(() => ordersRoute.getPageNumbers.every((pageNumber) => pageNumber === 0))
    .toBe(true);
  await expect(page.getByText('No upcoming orders yet.')).toBeVisible();
});

async function seedCurrentUser(page: Parameters<typeof setCsrfCookie>[0]): Promise<void> {
  await page.addInitScript(
    (currentUser) => {
      localStorage.setItem('ticketOrderPlatform.currentUser', JSON.stringify(currentUser));
    },
    {
      id: buyerLoginUser.id,
      email: buyerLoginUser.email,
      role: buyerLoginUser.role,
    },
  );
}

function earlierOrders() {
  return Array.from({ length: 20 }, (_, index) =>
    ownedOrderResponseFor({
      eventOrderId: `00000000-0000-0000-0000-0000000007${String(index + 11).padStart(2, '0')}`,
      eventName: `Earlier Arena Show ${index + 1}`,
    }),
  );
}
