import { beforeEach, describe, expect, it } from 'vitest';
import { clearStoredUser, loadStoredUser, storeUser } from './authStorage';
import { buyerUser, storedUserKey } from '../test/authTestData';

describe('authStorage', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('stores and loads the authenticated user', () => {
    storeUser(buyerUser);

    expect(loadStoredUser()).toEqual(buyerUser);
  });

  it('clears the authenticated user', () => {
    storeUser(buyerUser);

    clearStoredUser();

    expect(loadStoredUser()).toBeNull();
  });

  it('drops invalid stored values', () => {
    localStorage.setItem(
      storedUserKey,
      JSON.stringify({
        id: '4b804b8d-6a6f-44d3-9b98-a2ab33f8b8c2',
        email: 'buyer@example.com',
        role: 'UNKNOWN',
      }),
    );

    expect(loadStoredUser()).toBeNull();
    expect(localStorage.getItem(storedUserKey)).toBeNull();
  });
});
