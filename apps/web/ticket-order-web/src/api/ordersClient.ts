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

export async function listMyOrders(query: OrdersPageQuery): Promise<MyOrdersResponse> {
  return ordersApi.listMyOrders(query);
}
