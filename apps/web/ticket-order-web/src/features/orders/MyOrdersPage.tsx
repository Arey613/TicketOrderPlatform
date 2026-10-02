import { useEffect, useRef, useState } from 'react';
import { PaginationToolbar } from '../../components/PaginationToolbar';
import { usePagination } from '../../hooks/usePagination';
import { formatDateTime } from '../../utils/formatters';
import { useCancelMyOrderMutation } from './useCancelMyOrderMutation';
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
  const cancelOrder = useCancelMyOrderMutation();
  const [orderIdToCancel, setOrderIdToCancel] = useState<string | null>(null);
  const [cancelErrorOrderId, setCancelErrorOrderId] = useState<string | null>(null);
  const orders = query.data?.items ?? [];
  const page = query.data?.page;
  const cancellingOrderId = cancelOrder.isPending ? cancelOrder.variables : null;

  useEffect(() => {
    headingRef.current?.focus();
  }, []);

  function handleCancelOrder() {
    if (!orderIdToCancel) {
      return;
    }

    const shouldMoveToPreviousPage = orders.length === 1 && pagination.pageNumber > 0;
    const selectedOrderId = orderIdToCancel;
    setCancelErrorOrderId(null);

    cancelOrder.mutate(selectedOrderId, {
      onSuccess: () => {
        setOrderIdToCancel(null);

        if (shouldMoveToPreviousPage) {
          pagination.goToPrevious();
        } else {
          void query.refetch();
        }
      },
      onError: () => {
        setCancelErrorOrderId(selectedOrderId);
      },
    });
  }

  return (
    <main className="mx-auto w-full max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p className="text-sm font-bold uppercase tracking-normal text-teal-800">Orders</p>
          <h1 className="mt-2 text-3xl font-black text-slate-950" ref={headingRef} tabIndex={-1}>
            My orders
          </h1>
        </div>
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
                  <div className="col-span-2">
                    <button
                      className="rounded-md border border-red-200 bg-white px-3 py-2 text-sm font-bold text-red-800 transition hover:border-red-700 hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-red-700 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60"
                      disabled={cancelOrder.isPending}
                      onClick={() => {
                        setCancelErrorOrderId(null);
                        setOrderIdToCancel(order.eventOrderId);
                      }}
                      type="button"
                    >
                      {cancellingOrderId === order.eventOrderId
                        ? 'Cancelling order'
                        : 'Cancel order'}
                    </button>
                    <p
                      aria-live="polite"
                      className={
                        cancelErrorOrderId === order.eventOrderId
                          ? 'mt-2 text-sm font-semibold text-red-800'
                          : 'sr-only'
                      }
                      role={cancelErrorOrderId === order.eventOrderId ? 'alert' : undefined}
                    >
                      {cancelErrorOrderId === order.eventOrderId
                        ? 'Order could not be cancelled. Try again in a moment.'
                        : ''}
                    </p>
                  </div>
                </dl>
              </article>
            ))}
          </div>
        )}
      </section>

      {orderIdToCancel && (
        <div
          aria-labelledby="cancel-order-title"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 px-4"
          role="dialog"
        >
          <div className="w-full max-w-md rounded-md bg-white p-6 shadow-xl">
            <h2 className="text-xl font-black text-slate-950" id="cancel-order-title">
              Cancel order?
            </h2>
            <p className="mt-3 text-sm font-semibold text-slate-700">
              This removes your booking and makes the place available again.
            </p>
            <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
              <button
                className="rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-bold text-slate-800 transition hover:border-teal-700 hover:text-teal-800 focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60"
                disabled={cancelOrder.isPending}
                onClick={() => setOrderIdToCancel(null)}
                type="button"
              >
                Keep order
              </button>
              <button
                className="rounded-md border border-red-700 bg-red-700 px-4 py-2 text-sm font-bold text-white transition hover:bg-red-800 focus:outline-none focus:ring-2 focus:ring-red-700 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60"
                disabled={cancelOrder.isPending}
                onClick={handleCancelOrder}
                type="button"
              >
                {cancelOrder.isPending ? 'Cancelling order' : 'Cancel order'}
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}
