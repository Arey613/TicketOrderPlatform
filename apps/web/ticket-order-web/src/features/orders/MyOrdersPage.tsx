import { useEffect, useRef, useState } from 'react';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { PaginationToolbar } from '../../components/PaginationToolbar';
import type { MyOrderResponse } from '../../generated/api';
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
  const [orderToCancel, setOrderToCancel] = useState<MyOrderResponse | null>(null);
  const [cancelErrorMessage, setCancelErrorMessage] = useState<string | null>(null);
  const [statusMessage, setStatusMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const orders = query.data?.items ?? [];
  const page = query.data?.page;
  const cancellingOrderId = cancelOrder.isPending ? cancelOrder.variables : null;

  useEffect(() => {
    headingRef.current?.focus();
  }, []);

  function handleCancelOrder() {
    if (!orderToCancel) {
      return;
    }

    const shouldMoveToPreviousPage = orders.length === 1 && pagination.pageNumber > 0;
    const selectedOrderId = orderToCancel.eventOrderId;
    setCancelErrorMessage(null);
    setStatusMessage('');

    cancelOrder.mutate(selectedOrderId, {
      onSuccess: () => {
        setOrderToCancel(null);
        setSuccessMessage('Order cancelled.');
        setStatusMessage('Order cancelled.');

        if (shouldMoveToPreviousPage) {
          pagination.goToPrevious();
        }
      },
      onError: (error) => {
        const message = getCancelOrderErrorMessage(error);
        setCancelErrorMessage(message);
        setStatusMessage(message);
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

      <p aria-live="polite" className="sr-only">
        {statusMessage}
      </p>

      {successMessage && (
        <div className="fixed right-4 top-4 z-40 max-w-sm rounded-md border border-teal-200 bg-white p-4 shadow-lg">
          <p className="text-sm font-bold text-teal-900">{successMessage}</p>
          <button
            aria-label="Dismiss notification"
            className="mt-2 text-sm font-bold text-teal-800 underline-offset-2 hover:underline focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2"
            onClick={() => setSuccessMessage(null)}
            type="button"
          >
            Dismiss
          </button>
        </div>
      )}

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

        {query.isError && (
          <p className="px-5 py-4 text-sm font-semibold text-red-800">
            Orders are unavailable. Try again in a moment.
          </p>
        )}

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
                <div className="flex justify-start md:justify-end">
                  <div className="col-span-2">
                    <button
                      aria-label={`Cancel order for ${order.eventName}`}
                      className="rounded-md border border-red-200 bg-white px-3 py-2 text-sm font-bold text-red-800 transition hover:border-red-700 hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-red-700 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60"
                      disabled={cancelOrder.isPending}
                      onClick={() => {
                        setSuccessMessage(null);
                        setCancelErrorMessage(null);
                        setOrderToCancel(order);
                      }}
                      type="button"
                    >
                      {cancellingOrderId === order.eventOrderId
                        ? 'Cancelling order'
                        : 'Cancel order'}
                    </button>
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      {orderToCancel && (
        <ConfirmDialog
          cancelLabel="Keep order"
          confirmLabel="Cancel order"
          errorMessage={cancelErrorMessage}
          isPending={cancelOrder.isPending}
          onCancel={() => {
            setCancelErrorMessage(null);
            setOrderToCancel(null);
          }}
          onConfirm={handleCancelOrder}
          pendingConfirmLabel="Cancelling order"
          title="Cancel order?"
        >
          <p>This removes your booking and makes the place available again.</p>
          <p className="mt-3 text-slate-900">
            {orderToCancel.eventName}, row {orderToCancel.row}, place {orderToCancel.place}
          </p>
        </ConfirmDialog>
      )}
    </main>
  );
}

function getCancelOrderErrorMessage(error: unknown): string {
  const responseStatus =
    typeof error === 'object' &&
    error !== null &&
    'response' in error &&
    typeof (error as { response?: { status?: unknown } }).response?.status === 'number'
      ? (error as { response: { status: number } }).response.status
      : undefined;

  if (responseStatus === 409) {
    return 'This order can no longer be cancelled.';
  }

  return 'Order could not be cancelled. Try again in a moment.';
}
