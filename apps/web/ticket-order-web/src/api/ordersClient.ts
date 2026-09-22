import type { MyOrdersResponse } from '../generated/api';
import { Configuration, OrdersApi } from '../generated/api';
import { resolveApiBaseUrl, sessionAwareMiddleware } from './apiConfiguration';

const ordersApi = new OrdersApi(
  new Configuration({
    basePath: resolveApiBaseUrl(),
    credentials: 'include',
    middleware: [sessionAwareMiddleware],
  }),
);

export type OrdersPageQuery = {
  page: number;
  size: number;
  sort?: string;
};

const DEFAULT_ORDERS_PAGE = 0;
const DEFAULT_ORDERS_PAGE_SIZE = 20;
const DEFAULT_ORDERS_SORT = 'eventDate,asc';

export async function listMyOrders(
  query: Partial<OrdersPageQuery> = {},
): Promise<MyOrdersResponse> {
  return ordersApi.listMyOrders({
    page: query.page ?? DEFAULT_ORDERS_PAGE,
    size: query.size ?? DEFAULT_ORDERS_PAGE_SIZE,
    sort: query.sort ?? DEFAULT_ORDERS_SORT,
  });
}
