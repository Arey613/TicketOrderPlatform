import { expect, type Page } from '@playwright/test';

export async function mockCreateEvent(page: Page): Promise<void> {
  await page.route('**/events', async (route) => {
    const url = new URL(route.request().url());

    if (route.request().method() !== 'POST' || url.pathname !== '/events') {
      await route.fallback();
      return;
    }

    await expect(route.request().postDataJSON()).toEqual(
      expect.objectContaining({
        name: 'Summer music night',
        place: 'Central Hall',
        type: 'CONCERT',
        details: expect.objectContaining({
          description: 'Outdoor concert with reserved seating',
          numberOfPlaces: 120,
          numberOfRows: 12,
          seatsPerRow: 10,
        }),
      }),
    );
    await expect(route.request().headers()['x-xsrf-token']).toBe('e2e-token');

    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        eventId: '00000000-0000-0000-0000-000000000900',
        ownerId: '00000000-0000-0000-0000-000000000601',
        name: 'Summer music night',
        date: '2026-09-10T19:00:00Z',
        place: 'Central Hall',
        type: 'CONCERT',
        status: 'DRAFT',
        details: {
          description: 'Outdoor concert with reserved seating',
          numberOfPlaces: 120,
          numberOfRows: 12,
          seatsPerRow: 10,
        },
        ordersTaken: 0,
        takenPlaces: [],
      }),
    });
  });
}
