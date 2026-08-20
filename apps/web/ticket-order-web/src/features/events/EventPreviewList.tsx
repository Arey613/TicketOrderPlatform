import { CalendarDays, Clock, MapPin, Ticket } from 'lucide-react';
import { eventPreviews } from './eventMocks';

export function EventPreviewList() {
  return (
    <section className="mx-auto w-full max-w-7xl px-4 py-10 sm:px-6 lg:px-8" id="events">
      <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-semibold uppercase tracking-normal text-teal-700">
            Now booking
          </p>
          <h2 className="mt-1 text-2xl font-bold text-slate-950 sm:text-3xl">Upcoming events</h2>
        </div>
        <button
          className="w-fit rounded-md border border-teal-700 px-4 py-2 text-sm font-semibold text-teal-800 transition hover:bg-teal-50 focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2"
          type="button"
        >
          View all events
        </button>
      </div>

      <div className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
        {eventPreviews.map((event) => (
          <article
            className="grid gap-4 border-b border-slate-200 p-4 last:border-b-0 sm:grid-cols-[112px_1fr] lg:grid-cols-[132px_1fr_1fr_156px] lg:items-center"
            key={event.id}
          >
            <div
              className={`flex h-24 items-end rounded-md bg-gradient-to-br ${event.accent} p-3 text-white`}
              aria-hidden="true"
            >
              <Ticket className="h-7 w-7" />
            </div>

            <div>
              <h3 className="text-lg font-bold text-slate-950">{event.title}</h3>
              <p className="mt-1 text-sm text-slate-600">{event.category}</p>
            </div>

            <dl className="grid gap-3 text-sm text-slate-700 sm:grid-cols-2 lg:grid-cols-1">
              <div className="flex items-start gap-2">
                <CalendarDays
                  className="mt-0.5 h-4 w-4 shrink-0 text-slate-500"
                  aria-hidden="true"
                />
                <div>
                  <dt className="sr-only">Date</dt>
                  <dd>{event.date}</dd>
                </div>
              </div>
              <div className="flex items-start gap-2">
                <Clock className="mt-0.5 h-4 w-4 shrink-0 text-slate-500" aria-hidden="true" />
                <div>
                  <dt className="sr-only">Time</dt>
                  <dd>{event.time}</dd>
                </div>
              </div>
              <div className="flex items-start gap-2 sm:col-span-2 lg:col-span-1">
                <MapPin className="mt-0.5 h-4 w-4 shrink-0 text-slate-500" aria-hidden="true" />
                <div>
                  <dt className="sr-only">Venue</dt>
                  <dd>
                    {event.venue}, {event.city}
                  </dd>
                </div>
              </div>
            </dl>

            <div className="flex items-center justify-between gap-4 lg:block lg:text-right">
              <div>
                <span className="block text-xs font-semibold uppercase tracking-normal text-slate-500">
                  From
                </span>
                <span className="text-xl font-bold text-red-700">{event.price}</span>
              </div>
              <span className="text-sm font-medium text-emerald-700">
                {event.availableSeats} tickets left
              </span>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
