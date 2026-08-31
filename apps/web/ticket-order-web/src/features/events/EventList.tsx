import clsx from 'clsx';
import { CalendarDays, MapPin, RefreshCw, Ticket } from 'lucide-react';
import { useEffect } from 'react';
import { toEventUserMessage } from '../../api/eventsClient';
import { PaginationToolbar } from '../../components/PaginationToolbar';
import { usePagination } from '../../hooks/usePagination';
import { formatDateTime, formatPrice } from '../../utils/formatters';
import { useEventsQuery } from './useEventsQuery';

const EVENT_PAGE_SIZES = [5, 10, 20, 50];
const DEFAULT_EVENT_PAGE_SIZE = 10;

type EventListProps = {
  selectedEventId: string | null;
  onSelectEvent: (eventId: string) => void;
};

export function EventList({ selectedEventId, onSelectEvent }: EventListProps) {
  const pagination = usePagination(DEFAULT_EVENT_PAGE_SIZE);
  const query = useEventsQuery({ page: pagination.pageNumber, size: pagination.pageSize });
  const events = query.data?.items ?? [];
  const page = query.data?.page;

  useEffect(() => {
    if (selectedEventId !== null || events.length === 0) {
      return;
    }

    onSelectEvent(events[0].eventId);
  }, [selectedEventId, events, onSelectEvent]);

  return (
    <div className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
      <div className="flex flex-col gap-3 border-b border-slate-200 px-4 py-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-semibold uppercase tracking-normal text-teal-700">
            Now booking
          </p>
          <h2 className="mt-1 text-2xl font-bold text-slate-950 sm:text-3xl">Upcoming events</h2>
        </div>
        <button
          className="flex w-fit items-center gap-2 rounded-md border border-teal-700 px-4 py-2 text-sm font-semibold text-teal-800 transition hover:bg-teal-50 focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2"
          onClick={() => void query.refetch()}
          type="button"
        >
          <RefreshCw className="h-4 w-4" aria-hidden="true" />
          Refresh
        </button>
      </div>

      {page && (
        <PaginationToolbar
          label="Events per page"
          page={page}
          pageSize={pagination.pageSize}
          pageSizes={EVENT_PAGE_SIZES}
          onPageSizeChange={pagination.setPageSize}
          onPrevious={pagination.goToPrevious}
          onNext={pagination.goToNext}
        />
      )}

      <p
        aria-live="polite"
        className={
          query.isError
            ? 'border-b border-red-200 bg-red-50 p-4 text-sm font-semibold text-red-800'
            : 'sr-only'
        }
      >
        {query.isError ? toEventUserMessage(query.error) : ''}
      </p>

      {query.isLoading && (
        <p className="p-5 text-sm font-semibold text-slate-600">Loading events...</p>
      )}

      {!query.isLoading && !query.isError && events.length === 0 && (
        <p className="p-5 text-sm font-semibold text-slate-600">No published events yet.</p>
      )}

      {events.map((event) => (
        <article
          className={clsx(
            'grid gap-4 border-b border-slate-200 p-4 last:border-b-0 sm:grid-cols-[112px_1fr] xl:grid-cols-[132px_1fr_1fr_156px] xl:items-center',
            selectedEventId === event.eventId && 'bg-teal-50/70',
          )}
          key={event.eventId}
        >
          <div
            className="flex h-24 items-end rounded-md bg-gradient-to-br from-teal-600 to-zinc-900 p-3 text-white"
            aria-hidden="true"
          >
            <Ticket className="h-7 w-7" />
          </div>

          <div>
            <h3 className="text-lg font-bold text-slate-950">{event.name}</h3>
            <p className="mt-1 text-sm text-slate-600">{event.type}</p>
          </div>

          <dl className="grid gap-3 text-sm text-slate-700 sm:grid-cols-2 lg:grid-cols-1">
            <div className="flex items-start gap-2">
              <CalendarDays className="mt-0.5 h-4 w-4 shrink-0 text-slate-500" aria-hidden="true" />
              <div>
                <dt className="sr-only">Date</dt>
                <dd>{formatDateTime(event.date)}</dd>
              </div>
            </div>
            <div className="flex items-start gap-2 sm:col-span-2 lg:col-span-1">
              <MapPin className="mt-0.5 h-4 w-4 shrink-0 text-slate-500" aria-hidden="true" />
              <div>
                <dt className="sr-only">Venue</dt>
                <dd>
                  {event.place}
                  {event.city ? `, ${event.city}` : ''}
                </dd>
              </div>
            </div>
          </dl>

          <div className="flex items-center justify-between gap-4 lg:block lg:text-right">
            <div>
              <span className="block text-xs font-semibold uppercase tracking-normal text-slate-500">
                From
              </span>
              <span className="text-xl font-bold text-red-700">
                {formatPrice(event.price, event.currency)}
              </span>
            </div>
            <button
              className="rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-bold text-slate-900 transition hover:border-teal-700 hover:text-teal-800 focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2"
              onClick={() => onSelectEvent(event.eventId)}
              type="button"
            >
              Select
            </button>
          </div>
        </article>
      ))}
    </div>
  );
}
