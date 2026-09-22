import { useQuery } from '@tanstack/react-query';
import type { OrdersPageQuery } from '../../api/ordersClient';
import { listMyOrders } from '../../api/ordersClient';

export function useMyOrdersQuery(query: OrdersPageQuery, enabled: boolean) {
  return useQuery({
    queryKey: ['orders', 'mine', query.page, query.size, query.sort],
    queryFn: () => listMyOrders(query),
    enabled,
    placeholderData: (previousData) => previousData,
  });
}
