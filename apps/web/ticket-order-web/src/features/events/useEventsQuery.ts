import { useQuery } from '@tanstack/react-query';
import type { PageQuery } from '../../api/eventsClient';
import { listPublishedEvents } from '../../api/eventsClient';

export function useEventsQuery(query: PageQuery) {
  return useQuery({
    queryKey: ['events', 'published', query.page, query.size],
    queryFn: () => listPublishedEvents(query),
    placeholderData: (previousData) => previousData,
  });
}
