import { useEffect, useRef } from 'react';
import { PaginationToolbar } from '../../components/PaginationToolbar';
import { usePagination } from '../../hooks/usePagination';
import { formatDateTime } from '../../utils/formatters';
import { useMyOrdersQuery } from './useMyOrdersQuery';

const ORDER_PAGE_SIZES = [10, 20, 50, 100];
const DEFAULT_ORDER_PAGE_SIZE = 20;
const DEFAULT_ORDER_SORT = 'eventDate,asc';

export function MyOrdersPage() {
  const headingRef = useRef<HTMLHeadingElement>(null);
  const pagination = usePagination(DEFAULT_ORDER_PAGE_SIZE);
  const query = useMyOrdersQuery(
    { page: pagination.pageNumber, size: pagination.pageSize, sort: DEFAULT_ORDER_SORT },
    true,
  );
  const orders = query.data?.items ?? [];
  const page = query.data?.page;

  useEffect(() => {
    headingRef.current?.focus();
  }, []);

  return (
    <main className="mx-auto w-full max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p className="text-sm font-bold uppercase tracking-normal text-teal-800">Orders</p>
          <h1 className="mt-2 text-3xl font-black text-slate-950" ref={headingRef} tabIndex={-1}>
            My orders
          </h1>
        </div>
        <button
          className="rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-bold text-slate-800 transition hover:border-teal-700 hover:text-teal-800 focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2"
          onClick={() => void query.refetch()}
          type="button"
        >
          Refresh
        </button>
      </div>

      <section className="mt-6 overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm">
        {page && (
          <PaginationToolbar
            label="Orders per page"
            page={page}
            pageSize={pagination.pageSize}
            pageSizes={ORDER_PAGE_SIZES}
            onPageSizeChange={pagination.setPageSize}
            onPrevious={pagination.goToPrevious}
            onNext={pagination.goToNext}
          />
        )}

        <p
          aria-live="polite"
          className={query.isError ? 'px-5 py-4 text-sm font-semibold text-red-800' : 'sr-only'}
        >
          {query.isError ? 'Orders are unavailable. Try again in a moment.' : ''}
        </p>

        {query.isLoading ? (
          <p className="px-5 py-4 text-sm font-semibold text-slate-600">Loading orders...</p>
        ) : !query.isError && orders.length === 0 ? (
          <p className="px-5 py-4 text-sm font-semibold text-slate-600">No upcoming orders yet.</p>
        ) : (
          <div className="divide-y divide-slate-200">
            {orders.map((order) => (
              <article
                className="grid gap-3 p-5 md:grid-cols-[minmax(0,1fr)_auto]"
                key={order.eventOrderId}
              >
                <div className="min-w-0">
                  <h2 className="text-lg font-black text-slate-950">{order.eventName}</h2>
                  <p className="mt-1 text-sm font-semibold text-slate-700">
                    {formatDateTime(order.eventDate)}
                  </p>
                  <p className="mt-1 text-sm text-slate-600">{order.eventPlace}</p>
                </div>
                <dl className="grid grid-cols-2 gap-x-6 gap-y-2 text-sm md:min-w-80">
                  <div>
                    <dt className="font-semibold text-slate-500">Seat</dt>
                    <dd className="font-bold text-slate-900">
                      Row {order.row}, place {order.place}
                    </dd>
                  </div>
                  <div>
                    <dt className="font-semibold text-slate-500">Type</dt>
                    <dd className="font-bold text-slate-900">{order.placeType}</dd>
                  </div>
                  <div className="col-span-2">
                    <dt className="font-semibold text-slate-500">Reserved</dt>
                    <dd className="font-bold text-slate-900">
                      {formatDateTime(order.reservationDate)}
                    </dd>
                  </div>
                </dl>
              </article>
            ))}
          </div>
        )}
      </section>
    </main>
  );
}
