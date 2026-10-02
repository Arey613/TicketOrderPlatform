import { expect, type Page } from '@playwright/test';

type EventRouteOptions = {
  bookedAfterCreate?: boolean;
};

type MyOrdersRouteOptions = {
  fail?: boolean;
};

type OwnedOrder = ReturnType<typeof ownedOrderResponse>;

type MyOrdersPageResponse = {
  items: OwnedOrder[];
  page: {
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
  };
};

type MyOrdersCancellationRouteOptions = {
  fail?: boolean;
  pages?: Record<number, MyOrdersPageResponse>;
};

export type MyOrdersCancellationRouteState = {
  deleteRequests: Array<{
    body: unknown;
    url: string;
  }>;
  deletedOrderIds: string[];
  getPageNumbers: number[];
};

const eventId = '00000000-0000-0000-0000-000000000603';
const draftEventId = '00000000-0000-0000-0000-000000000609';
export const ownedOrderId = '00000000-0000-0000-0000-000000000701';

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
  options: MyOrdersRouteOptions = {},
): Promise<void> {
  await page.route('**/orders/mine**', async (route) => {
    const url = new URL(route.request().url());
    if (route.request().method() !== 'GET' || url.pathname !== '/orders/mine') {
      await route.fallback();
      return;
    }
    if (route.request().headers().accept?.includes('text/html')) {
      await route.fallback();
      return;
    }

    await expect(url.searchParams.get('sort')).toBe('eventDate,asc');

    if (options.fail) {
      await route.fulfill({ status: 500, contentType: 'application/json', body: '{}' });
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

export async function mockMyOrdersCancellation(
  page: Page,
  options: MyOrdersCancellationRouteOptions = {},
): Promise<MyOrdersCancellationRouteState> {
  const routeState: MyOrdersCancellationRouteState = {
    deleteRequests: [],
    deletedOrderIds: [],
    getPageNumbers: [],
  };
  const pages = options.pages ?? {
    0: myOrdersPageResponse([ownedOrderResponse()], 0, 20, 1),
  };

  await page.route('**/orders/mine**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    if (url.pathname !== '/orders/mine') {
      await route.fallback();
      return;
    }
    if (request.headers().accept?.includes('text/html')) {
      await route.fallback();
      return;
    }

    if (request.method() === 'GET') {
      const pageNumber = Number(url.searchParams.get('page') ?? '0');
      routeState.getPageNumbers.push(pageNumber);
      await expect(url.searchParams.get('sort')).toBe('eventDate,asc');
      const response = pages[pageNumber] ?? myOrdersPageResponse([], pageNumber, 20, 0);
      const items = response.items.filter(
        (order) => !routeState.deletedOrderIds.includes(order.eventOrderId),
      );

      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify({
          ...response,
          items,
          page: {
            ...response.page,
            totalElements: response.page.totalElements - (response.items.length - items.length),
            totalPages:
              response.page.totalElements - (response.items.length - items.length) === 0
                ? 0
                : response.page.totalPages,
          },
        }),
      });
      return;
    }

    if (request.method() === 'DELETE') {
      const body = request.postDataJSON();
      routeState.deleteRequests.push({ body, url: request.url() });

      if (options.fail) {
        await route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({ message: 'Unable to cancel order.' }),
        });
        return;
      }

      const ids = isEventOrderIdsPayload(body) ? body.eventOrderIds : [];
      routeState.deletedOrderIds.push(...ids);

      await route.fulfill({ status: 204 });
      return;
    }

    await route.fallback();
  });

  return routeState;
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

export async function mockMyEventsManagement(page: Page): Promise<void> {
  let draftDeleted = false;

  await page.route('**/events**', async (route) => {
    const url = new URL(route.request().url());
    if (route.request().method() !== 'GET' || url.pathname !== '/events') {
      await route.fallback();
      return;
    }

    await expect(url.searchParams.get('scope')?.toUpperCase()).toBe('MINE');

    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        items: [draftDeleted ? [] : [draftEventResponse()], eventResponse()].flat(),
        page: pageMetadata(10, draftDeleted ? 1 : 2),
      }),
    });
  });

  await page.route(`**/events/${draftEventId}`, async (route) => {
    if (route.request().method() !== 'DELETE') {
      await route.fallback();
      return;
    }

    await expect(route.request().headers()['x-xsrf-token']).toBe('e2e-token');
    draftDeleted = true;

    await route.fulfill({ status: 204 });
  });
}

function ownedOrderResponse() {
  return {
    eventOrderId: ownedOrderId,
    eventId,
    eventName: 'The Horizon Live',
    eventDate: '2026-09-12T19:30:00Z',
    eventPlace: 'Riverside Arena',
    row: 1,
    place: 2,
    placeType: 'STANDARD',
    reservationDate: '2026-08-24T10:00:00Z',
  };
}

export function ownedOrderResponseFor(overrides: Partial<OwnedOrder> = {}): OwnedOrder {
  return {
    ...ownedOrderResponse(),
    ...overrides,
  };
}

export function myOrdersPageResponse(
  items: OwnedOrder[],
  number: number,
  size: number,
  totalElements: number,
): MyOrdersPageResponse {
  const totalPages = totalElements === 0 ? 0 : Math.ceil(totalElements / size);

  return {
    items,
    page: {
      number,
      size,
      totalElements,
      totalPages,
      first: number === 0,
      last: totalPages === 0 || number + 1 >= totalPages,
    },
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

function draftEventResponse() {
  return {
    ...eventResponse(false),
    eventId: draftEventId,
    name: 'Draft acoustic night',
    status: 'DRAFT',
    ordersTaken: 0,
    availablePlaces: 4,
    takenPlaces: [],
  };
}

function isEventOrderIdsPayload(body: unknown): body is { eventOrderIds: string[] } {
  return (
    typeof body === 'object' &&
    body !== null &&
    'eventOrderIds' in body &&
    Array.isArray((body as { eventOrderIds: unknown }).eventOrderIds)
  );
}
