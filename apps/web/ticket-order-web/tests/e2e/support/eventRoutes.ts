import { expect, type Page } from '@playwright/test';

type EventRouteOptions = {
  bookedAfterCreate?: boolean;
};

const eventId = '00000000-0000-0000-0000-000000000603';

export type EventBookingRouteState = {
  bookedAfterCreate: boolean;
};

export async function mockPublishedEvents(
  page: Page,
  options: EventRouteOptions = {},
): Promise<void> {
  await page.route('**/public/events**', async (route) => {
    const url = new URL(route.request().url());
    if (route.request().method() !== 'GET' || url.pathname !== '/public/events') {
      await route.fallback();
      return;
    }

    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        items: [eventResponse(options.bookedAfterCreate)],
        page: pageMetadata(10, 1),
      }),
    });
  });

  await page.route(`**/public/events/${eventId}`, async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(eventResponse(options.bookedAfterCreate)),
    });
  });

  await page.route(`**/events/${eventId}`, async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(eventResponse(options.bookedAfterCreate)),
    });
  });
}

export async function mockMyOrders(
  page: Page,
  state: EventBookingRouteState = { bookedAfterCreate: true },
): Promise<void> {
  await page.route('**/events/orders/mine**', async (route) => {
    const url = new URL(route.request().url());
    if (route.request().method() !== 'GET' || url.pathname !== '/events/orders/mine') {
      await route.fallback();
      return;
    }

    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        items: state.bookedAfterCreate ? [ownedOrderResponse()] : [],
        page: pageMetadata(20, state.bookedAfterCreate ? 1 : 0),
      }),
    });
  });
}

export async function mockCreateOrder(
  page: Page,
  state: EventBookingRouteState = { bookedAfterCreate: false },
): Promise<void> {
  await page.route('**/events/orders', async (route) => {
    if (route.request().method() !== 'POST') {
      await route.fallback();
      return;
    }

    await expect(route.request().postDataJSON()).toEqual({
      orders: [
        {
          eventId,
          row: 1,
          place: 2,
          placeType: 'STANDARD',
        },
      ],
    });
    await expect(route.request().headers()['x-xsrf-token']).toBe('e2e-token');

    state.bookedAfterCreate = true;

    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        orders: [
          {
            eventOrderId: '00000000-0000-0000-0000-000000000702',
            eventId,
            row: 1,
            place: 2,
            placeType: 'STANDARD',
            reservationDate: '2026-08-24T10:01:00Z',
          },
        ],
      }),
    });
  });
}

export async function mockEventBookingFlow(page: Page): Promise<EventBookingRouteState> {
  const state: EventBookingRouteState = { bookedAfterCreate: false };

  await mockPublishedEvents(page, state);
  await mockCreateOrder(page, state);
  await mockMyOrders(page, state);

  return state;
}

function ownedOrderResponse() {
  return {
    eventOrderId: '00000000-0000-0000-0000-000000000701',
    eventId,
    eventName: 'The Horizon Live',
    eventDate: '2026-09-12T19:30:00Z',
    row: 1,
    place: 2,
    placeType: 'STANDARD',
    reservationDate: '2026-08-24T10:00:00Z',
  };
}

function pageMetadata(size: number, totalElements: number) {
  return {
    number: 0,
    size,
    totalElements,
    totalPages: totalElements === 0 ? 0 : 1,
    first: true,
    last: true,
  };
}

function eventResponse(bookedAfterCreate = false) {
  return {
    eventId,
    ownerId: '00000000-0000-0000-0000-000000000601',
    name: 'The Horizon Live',
    date: '2026-09-12T19:30:00Z',
    place: 'Riverside Arena',
    city: 'Chisinau',
    type: 'Rock concert',
    status: 'PUBLISHED',
    price: '59.00',
    currency: 'USD',
    details: {
      description: 'Live concert with reserved places.',
      numberOfPlaces: 4,
      numberOfRows: 2,
      seatsPerRow: 2,
      availablePlaces: bookedAfterCreate ? 2 : 3,
      placeTypes: [{ name: 'STANDARD', price: '59.00', currency: 'USD' }],
    },
    ordersTaken: bookedAfterCreate ? 2 : 1,
    availablePlaces: bookedAfterCreate ? 2 : 3,
    takenPlaces: [
      { row: 1, place: 1 },
      ...(bookedAfterCreate ? [{ row: 1, place: 2, isMine: true }] : []),
    ],
  };
}
