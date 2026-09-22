import { beforeEach, describe, expect, it, vi } from 'vitest';
import { listMyOrders } from '../../../src/api/ordersClient';

describe('ordersClient', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('uses my-orders defaults when pagination values are omitted', async () => {
    const fetchMock = vi.fn(async () =>
      Response.json({
        items: [],
        page: {
          number: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
          first: true,
          last: true,
        },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    await listMyOrders();

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/orders/mine?page=0&size=20&sort=eventDate%2Casc',
      expect.objectContaining({
        credentials: 'include',
        method: 'GET',
      }),
    );
  });

  it('allows callers to override my-orders pagination values', async () => {
    const fetchMock = vi.fn(async () =>
      Response.json({
        items: [],
        page: {
          number: 1,
          size: 50,
          totalElements: 0,
          totalPages: 0,
          first: false,
          last: true,
        },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    await listMyOrders({ page: 1, size: 50, sort: 'eventName,desc' });

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/orders/mine?page=1&size=50&sort=eventName%2Cdesc',
      expect.objectContaining({
        credentials: 'include',
        method: 'GET',
      }),
    );
  });
});
