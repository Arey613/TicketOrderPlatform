import { beforeEach, describe, expect, it, vi } from 'vitest';
import { login, logout, register, toUserMessage } from '../../../src/api/authClient';
import { ResponseError } from '../../../src/generated/api';
import {
  buyerLoginResponse,
  buyerUser,
  newBuyerLoginResponse,
  newBuyerUser,
} from '../../support/authTestData';

describe('authClient', () => {
  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=';
    vi.restoreAllMocks();
  });

  it('logs in with credentials, session cookies, and csrf header', async () => {
    document.cookie = 'XSRF-TOKEN=test-csrf-token';
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = input.toString();

      if (url.endsWith('/auth/csrf')) {
        return new Response(null, { status: 204 });
      }

      return Response.json(buyerLoginResponse);
    });
    vi.stubGlobal('fetch', fetchMock);

    const user = await login({
      email: 'buyer@example.com',
      password: 'correct-password',
    });

    expect(user).toEqual(buyerUser);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(fetchMock).toHaveBeenLastCalledWith(
      'http://localhost:8080/auth/login',
      expect.objectContaining({
        body: JSON.stringify({
          login: 'buyer@example.com',
          password: 'correct-password',
        }),
        credentials: 'include',
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
          'X-XSRF-TOKEN': 'test-csrf-token',
        }),
        method: 'POST',
      }),
    );
  });

  it('registers a user through the generated client boundary', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = input.toString();

      if (url.endsWith('/auth/csrf')) {
        return new Response(null, { status: 204 });
      }

      return Response.json(newBuyerLoginResponse);
    });
    vi.stubGlobal('fetch', fetchMock);

    const user = await register({
      email: 'new-buyer@example.com',
      password: 'new-password',
    });

    expect(user).toEqual(newBuyerUser);
    expect(fetchMock).toHaveBeenLastCalledWith(
      'http://localhost:8080/auth/register',
      expect.objectContaining({
        body: JSON.stringify({
          email: 'new-buyer@example.com',
          password: 'new-password',
        }),
        credentials: 'include',
        method: 'POST',
      }),
    );
  });

  it('logs out with session credentials', async () => {
    const fetchMock = vi.fn(async () => new Response(null, { status: 204 }));
    vi.stubGlobal('fetch', fetchMock);

    await logout();

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(fetchMock).toHaveBeenLastCalledWith(
      'http://localhost:8080/auth/logout',
      expect.objectContaining({
        credentials: 'include',
        method: 'POST',
      }),
    );
  });

  it('maps generated response errors to user-facing messages', () => {
    expect(
      toUserMessage(new ResponseError(new Response(null, { status: 401 }), 'Unauthorized')),
    ).toBe('The email or password is not valid.');

    expect(toUserMessage(new ResponseError(new Response(null, { status: 409 }), 'Conflict'))).toBe(
      'An account with this email already exists.',
    );

    expect(toUserMessage(new Error('Network failed'))).toBe(
      'The server is unavailable. Try again in a moment.',
    );
  });
});
