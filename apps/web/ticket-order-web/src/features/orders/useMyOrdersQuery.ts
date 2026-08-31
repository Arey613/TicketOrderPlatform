import { useQuery } from '@tanstack/react-query';
import type { PageQuery } from '../../api/eventsClient';
import { listMyEventOrders } from '../../api/eventsClient';

export function useMyOrdersQuery(query: PageQuery, enabled: boolean) {
  return useQuery({
    queryKey: ['orders', 'mine', query.page, query.size],
    queryFn: () => listMyEventOrders(query),
    enabled,
    placeholderData: (previousData) => previousData,
  });
}
