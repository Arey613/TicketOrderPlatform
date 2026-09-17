import { CalendarDays, Edit3, EyeOff, Plus, RefreshCw, Send, Ticket } from 'lucide-react';
import { Link } from 'react-router';
import { toEventUserMessage } from '../../api/eventsClient';
import { PaginationToolbar } from '../../components/PaginationToolbar';
import { type EventResponse, EventStatus } from '../../generated/api';
import { usePagination } from '../../hooks/usePagination';
import { formatDateTime } from '../../utils/formatters';
import { usePublishEventMutation, useUnpublishEventMutation } from './useEventManagementMutations';
import { useMyEventsQuery } from './useMyEventsQuery';

const EVENT_PAGE_SIZES = [5, 10, 20, 50];
const DEFAULT_EVENT_PAGE_SIZE = 10;

export function MyEventsPage() {
  const pagination = usePagination(DEFAULT_EVENT_PAGE_SIZE);
  const query = useMyEventsQuery({ page: pagination.pageNumber, size: pagination.pageSize });
  const events = query.data?.items ?? [];
  const page = query.data?.page;

  return (
    <section className="mx-auto w-full max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-semibold uppercase tracking-normal text-teal-700">
            Management
          </p>
          <h1 className="mt-1 text-3xl font-black text-slate-950">My events</h1>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link
            className="flex w-fit items-center gap-2 rounded-md bg-teal-700 px-4 py-2 text-sm font-bold text-white transition hover:bg-teal-800 focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2"
            to="/events/create"
          >
            <Plus className="h-4 w-4" aria-hidden="true" />
            Create event
          </Link>
          <button
            className="flex w-fit items-center gap-2 rounded-md border border-teal-700 px-4 py-2 text-sm font-semibold text-teal-800 transition hover:bg-teal-50 focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2"
            onClick={() => void query.refetch()}
            type="button"
          >
            <RefreshCw className="h-4 w-4" aria-hidden="true" />
            Refresh
          </button>
        </div>
      </div>

      <div className="mt-8 overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
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
          <p className="p-5 text-sm font-semibold text-slate-600">Loading your events...</p>
        )}

        {!query.isLoading && !query.isError && events.length === 0 && (
          <div className="p-5">
            <p className="text-sm font-semibold text-slate-600">No owned events yet.</p>
            <Link
              className="mt-3 inline-flex items-center gap-2 rounded-md border border-teal-700 px-4 py-2 text-sm font-bold text-teal-800 transition hover:bg-teal-50"
              to="/events/create"
            >
              <Plus className="h-4 w-4" aria-hidden="true" />
              Create event
            </Link>
          </div>
        )}

        {events.map((event) => (
          <MyEventRow event={event} key={event.eventId} />
        ))}
      </div>
    </section>
  );
}

function MyEventRow({ event }: { event: EventResponse }) {
  const publish = usePublishEventMutation();
  const unpublish = useUnpublishEventMutation();
  const isDraft = event.status === EventStatus.Draft;
  const isPublished = event.status === EventStatus.Published;
  const isPublishPending = publish.isPending && publish.variables === event.eventId;
  const isUnpublishPending = unpublish.isPending && unpublish.variables === event.eventId;
  const rowError =
    (publish.isError && publish.variables === event.eventId && publish.error) ||
    (unpublish.isError && unpublish.variables === event.eventId && unpublish.error);

  return (
    <article className="grid gap-4 border-t border-slate-200 p-4 first:border-t-0 md:grid-cols-[96px_1fr_auto] md:items-center">
      <div
        className="flex h-20 items-end rounded-md bg-gradient-to-br from-teal-600 to-zinc-900 p-3 text-white"
        aria-hidden="true"
      >
        <Ticket className="h-6 w-6" />
      </div>

      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h2 className="truncate text-lg font-bold text-slate-950">{event.name}</h2>
          <span className="rounded bg-slate-100 px-2 py-1 text-xs font-bold text-slate-700">
            {event.status}
          </span>
        </div>
        <p className="mt-1 text-sm text-slate-600">{event.type}</p>
        <p className="mt-2 flex items-center gap-2 text-sm text-slate-700">
          <CalendarDays className="h-4 w-4 text-slate-500" aria-hidden="true" />
          {formatDateTime(event.date)}
        </p>
        {rowError && (
          <p className="mt-2 text-sm font-semibold text-red-700">{toEventUserMessage(rowError)}</p>
        )}
      </div>

      <div className="flex flex-wrap gap-2 md:justify-end">
        {isDraft && (
          <>
            <Link
              aria-label={`Edit ${event.name}`}
              className="flex items-center gap-2 rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-bold text-slate-900 transition hover:border-teal-700 hover:text-teal-800"
              to={`/events/mine/${event.eventId}/edit`}
            >
              <Edit3 className="h-4 w-4" aria-hidden="true" />
              Edit
            </Link>
            <button
              aria-label={`Publish ${event.name}`}
              className="flex items-center gap-2 rounded-md bg-teal-700 px-4 py-2 text-sm font-bold text-white transition hover:bg-teal-800 disabled:cursor-not-allowed disabled:bg-slate-400"
              disabled={isPublishPending}
              onClick={() => publish.mutate(event.eventId)}
              type="button"
            >
              <Send className="h-4 w-4" aria-hidden="true" />
              Publish
            </button>
          </>
        )}

        {isPublished && (
          <button
            aria-label={`Unpublish ${event.name}`}
            className="flex items-center gap-2 rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-bold text-slate-900 transition hover:border-red-700 hover:text-red-700 disabled:cursor-not-allowed disabled:bg-slate-100"
            disabled={isUnpublishPending}
            onClick={() => unpublish.mutate(event.eventId)}
            type="button"
          >
            <EyeOff className="h-4 w-4" aria-hidden="true" />
            Unpublish
          </button>
        )}
      </div>
    </article>
  );
}
