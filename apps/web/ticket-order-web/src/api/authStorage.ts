import { UserRole } from '../generated/api';
import type { AuthenticatedUser } from './authClient';

const storageKey = 'ticketOrderPlatform.currentUser';

export function loadStoredUser(): AuthenticatedUser | null {
  const value = localStorage.getItem(storageKey);

  if (!value) {
    return null;
  }

  try {
    const user = parseStoredUser(JSON.parse(value));

    if (!user) {
      localStorage.removeItem(storageKey);
    }

    return user;
  } catch {
    localStorage.removeItem(storageKey);
    return null;
  }
}

export function storeUser(user: AuthenticatedUser): void {
  localStorage.setItem(storageKey, JSON.stringify(user));
}

export function clearStoredUser(): void {
  localStorage.removeItem(storageKey);
}

function parseStoredUser(value: unknown): AuthenticatedUser | null {
  if (!value || typeof value !== 'object') {
    return null;
  }

  const candidate = value as Partial<AuthenticatedUser>;
  const role = Object.values(UserRole).find((knownRole) => knownRole === candidate.role);

  if (typeof candidate.id !== 'string' || typeof candidate.email !== 'string' || !role) {
    return null;
  }

  return {
    id: candidate.id,
    email: candidate.email,
    role,
  };
}
