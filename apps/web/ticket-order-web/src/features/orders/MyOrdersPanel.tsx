import { PaginationToolbar } from '../../components/PaginationToolbar';
import { usePagination } from '../../hooks/usePagination';
import { formatDateTime } from '../../utils/formatters';
import { useMyOrdersQuery } from './useMyOrdersQuery';

const ORDER_PAGE_SIZES = [10, 20, 50, 100];
const DEFAULT_ORDER_PAGE_SIZE = 20;

export function MyOrdersPanel() {
  const pagination = usePagination(DEFAULT_ORDER_PAGE_SIZE);
  const query = useMyOrdersQuery({ page: pagination.pageNumber, size: pagination.pageSize }, true);
  const orders = query.data?.items ?? [];
  const page = query.data?.page;

  return (
    <section className="mt-8 rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-center justify-between gap-4">
        <h3 className="text-lg font-black text-slate-950">My orders</h3>
        <button
          className="rounded-md border border-slate-300 px-3 py-2 text-sm font-bold text-slate-800 transition hover:border-teal-700 hover:text-teal-800 focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2"
          onClick={() => void query.refetch()}
          type="button"
        >
          Refresh
        </button>
      </div>

      {page && (
        <div className="mt-4">
          <PaginationToolbar
            label="Orders per page"
            page={page}
            pageSize={pagination.pageSize}
            pageSizes={ORDER_PAGE_SIZES}
            onPageSizeChange={pagination.setPageSize}
            onPrevious={pagination.goToPrevious}
            onNext={pagination.goToNext}
          />
        </div>
      )}

      <p
        aria-live="polite"
        className={query.isError ? 'mt-4 text-sm font-semibold text-red-800' : 'sr-only'}
      >
        {query.isError ? 'Orders are unavailable. Try again in a moment.' : ''}
      </p>

      {query.isLoading ? (
        <p className="mt-4 text-sm font-semibold text-slate-600">Loading orders...</p>
      ) : !query.isError && orders.length === 0 ? (
        <p className="mt-4 text-sm font-semibold text-slate-600">No orders yet.</p>
      ) : (
        <div className="mt-4 grid gap-3 md:grid-cols-2">
          {orders.map((order) => (
            <article className="rounded-md border border-slate-200 p-4" key={order.eventOrderId}>
              <h4 className="font-bold text-slate-950">{order.eventName}</h4>
              <p className="mt-1 text-sm text-slate-600">{formatDateTime(order.eventDate)}</p>
              <p className="mt-3 text-sm font-semibold text-slate-800">
                Row {order.row}, place {order.place}
              </p>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
