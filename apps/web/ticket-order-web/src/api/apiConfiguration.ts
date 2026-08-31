import type { Middleware, ResponseContext } from '../generated/api';
import { notifySessionExpired } from './sessionEvents';

export function resolveApiBaseUrl(): string {
  const configuredUrl = import.meta.env.VITE_TICKET_API_BASE_URL;

  if (configuredUrl) {
    return configuredUrl;
  }

  if (import.meta.env.PROD) {
    throw new Error(
      'VITE_TICKET_API_BASE_URL must be set for production builds; refusing to default to localhost.',
    );
  }

  return 'http://localhost:8080';
}

export const sessionAwareMiddleware: Middleware = {
  post: async (context: ResponseContext) => {
    if (context.response.status === 401) {
      notifySessionExpired();
    }
  },
};
