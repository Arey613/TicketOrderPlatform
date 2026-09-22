import { useQuery } from '@tanstack/react-query';
import type { PageQuery } from '../../api/eventsClient';
import { listMyEvents } from '../../api/eventsClient';

export function useMyEventsQuery(query: PageQuery) {
  return useQuery({
    queryKey: ['events', 'mine', query.page, query.size],
    queryFn: () => listMyEvents(query),
    placeholderData: (previousData) => previousData,
  });
}
