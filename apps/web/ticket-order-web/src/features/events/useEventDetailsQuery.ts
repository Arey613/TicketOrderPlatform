import { useQuery } from '@tanstack/react-query';
import { getAuthenticatedEvent, getPublishedEvent } from '../../api/eventsClient';

export function useEventDetailsQuery(eventId: string | null, isAuthenticated: boolean) {
  return useQuery({
    queryKey: ['events', 'detail', eventId, isAuthenticated],
    queryFn: () =>
      isAuthenticated
        ? getAuthenticatedEvent(eventId as string)
        : getPublishedEvent(eventId as string),
    enabled: eventId !== null,
  });
}
