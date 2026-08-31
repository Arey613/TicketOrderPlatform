import { beforeEach, describe, expect, it, vi } from 'vitest';
import { sessionAwareMiddleware } from '../../../src/api/apiConfiguration';
import { notifySessionExpired } from '../../../src/api/sessionEvents';

vi.mock('../../../src/api/sessionEvents', () => ({
  notifySessionExpired: vi.fn(),
}));

const mockedNotifySessionExpired = vi.mocked(notifySessionExpired);

function responseContext(status: number) {
  return {
    fetch,
    url: 'http://localhost:8080/events/orders/mine',
    init: {},
    response: new Response(null, { status }),
  };
}

describe('sessionAwareMiddleware', () => {
  beforeEach(() => {
    mockedNotifySessionExpired.mockReset();
  });

  it('notifies session expiry on a 401 response', async () => {
    await sessionAwareMiddleware.post?.(responseContext(401));

    expect(mockedNotifySessionExpired).toHaveBeenCalledTimes(1);
  });

  it('does not notify on a successful response', async () => {
    await sessionAwareMiddleware.post?.(responseContext(200));

    expect(mockedNotifySessionExpired).not.toHaveBeenCalled();
  });

  it('does not notify on an unrelated error response', async () => {
    await sessionAwareMiddleware.post?.(responseContext(500));

    expect(mockedNotifySessionExpired).not.toHaveBeenCalled();
  });
});
