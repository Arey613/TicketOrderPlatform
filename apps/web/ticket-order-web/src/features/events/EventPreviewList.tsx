import clsx from 'clsx';
import { CalendarDays, Check, Lock, MapPin, RefreshCw, Ticket } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import type { AuthenticatedUser } from '../../api/authClient';
import {
  createEventOrders,
  getEvent,
  listEvents,
  listMyEventOrders,
  toEventUserMessage,
} from '../../api/eventsClient';
import type { EventResponse, MyEventOrderResponse } from '../../generated/api';

type EventPreviewListProps = {
  currentUser: AuthenticatedUser | null;
  onLogin: () => void;
};

type SelectedSeat = {
  row: number;
  place: number;
};

export function EventPreviewList({ currentUser, onLogin }: EventPreviewListProps) {
  const [events, setEvents] = useState<EventResponse[]>([]);
  const [selectedEventId, setSelectedEventId] = useState<string | null>(null);
  const [eventDetails, setEventDetails] = useState<EventResponse | null>(null);
  const [selectedSeat, setSelectedSeat] = useState<SelectedSeat | null>(null);
  const [myOrders, setMyOrders] = useState<MyEventOrderResponse[]>([]);
  const [isLoadingEvents, setIsLoadingEvents] = useState(true);
  const [isLoadingDetails, setIsLoadingDetails] = useState(false);
  const [isLoadingOrders, setIsLoadingOrders] = useState(false);
  const [isBooking, setIsBooking] = useState(false);
  const [message, setMessage] = useState('');

  const selectedEvent = eventDetails ?? events.find((event) => event.eventId === selectedEventId);
  const bookedSeats = useMemo(
    () =>
      new Map(
        (selectedEvent?.takenPlaces ?? []).map((place) => [`${place.row}:${place.place}`, place]),
      ),
    [selectedEvent],
  );
  const defaultPlaceType = selectedEvent?.details.placeTypes?.[0]?.name ?? 'STANDARD';

  useEffect(() => {
    void loadEvents();
  }, []);

  useEffect(() => {
    if (!currentUser) {
      setMyOrders([]);
      return;
    }

    void loadMyOrders();
  }, [currentUser]);

  async function loadEvents() {
    setIsLoadingEvents(true);
    setMessage('');

    try {
      const loadedEvents = await listEvents();
      setEvents(loadedEvents);
      setSelectedEventId((current) => current ?? loadedEvents[0]?.eventId ?? null);
    } catch (error) {
      setMessage(await toEventUserMessage(error));
    } finally {
      setIsLoadingEvents(false);
    }
  }

  async function loadEventDetails(eventId: string) {
    setSelectedEventId(eventId);
    setEventDetails(null);
    setSelectedSeat(null);
    setIsLoadingDetails(true);
    setMessage('');

    try {
      setEventDetails(await getEvent(eventId));
    } catch (error) {
      setMessage(await toEventUserMessage(error));
    } finally {
      setIsLoadingDetails(false);
    }
  }

  async function loadMyOrders() {
    setIsLoadingOrders(true);

    try {
      setMyOrders(await listMyEventOrders());
    } catch {
      setMyOrders([]);
    } finally {
      setIsLoadingOrders(false);
    }
  }

  async function submitBooking() {
    if (!selectedEvent || !selectedSeat) {
      return;
    }

    if (!currentUser) {
      onLogin();
      return;
    }

    setIsBooking(true);
    setMessage('');

    try {
      await createEventOrders({
        eventId: selectedEvent.eventId,
        row: selectedSeat.row,
        place: selectedSeat.place,
        placeType: defaultPlaceType,
      });
      setSelectedSeat(null);
      await loadEventDetails(selectedEvent.eventId);
      await loadMyOrders();
      setMessage('Place booked.');
    } catch (error) {
      setMessage(await toEventUserMessage(error));
    } finally {
      setIsBooking(false);
    }
  }

  const visibleSeats = selectedEvent
    ? Array.from({ length: selectedEvent.details.numberOfRows }, (_, rowIndex) =>
        Array.from({ length: selectedEvent.details.seatsPerRow }, (_, placeIndex) => ({
          row: rowIndex + 1,
          place: placeIndex + 1,
        })),
      )
    : [];

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
          className="flex w-fit items-center gap-2 rounded-md border border-teal-700 px-4 py-2 text-sm font-semibold text-teal-800 transition hover:bg-teal-50 focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2"
          onClick={loadEvents}
          type="button"
        >
          <RefreshCw className="h-4 w-4" aria-hidden="true" />
          Refresh
        </button>
      </div>

      {message && (
        <p className="mb-4 rounded-md border border-teal-200 bg-teal-50 px-4 py-3 text-sm font-semibold text-teal-900">
          {message}
        </p>
      )}

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_420px]">
        <div className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
          {isLoadingEvents && (
            <p className="p-5 text-sm font-semibold text-slate-600">Loading events...</p>
          )}

          {!isLoadingEvents && events.length === 0 && (
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
                  <CalendarDays
                    className="mt-0.5 h-4 w-4 shrink-0 text-slate-500"
                    aria-hidden="true"
                  />
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
                  onClick={() => loadEventDetails(event.eventId)}
                  type="button"
                >
                  Select
                </button>
              </div>
            </article>
          ))}
        </div>

        <aside className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
          {selectedEvent ? (
            <>
              <div className="flex items-start justify-between gap-4">
                <div>
                  <h3 className="text-xl font-black text-slate-950">{selectedEvent.name}</h3>
                  <p className="mt-1 text-sm leading-6 text-slate-600">
                    {selectedEvent.details.description}
                  </p>
                </div>
                <span className="shrink-0 rounded-md bg-emerald-50 px-3 py-2 text-sm font-bold text-emerald-800">
                  {selectedEvent.availablePlaces} left
                </span>
              </div>

              {isLoadingDetails ? (
                <p className="mt-5 text-sm font-semibold text-slate-600">Loading places...</p>
              ) : (
                <div className="mt-5 overflow-x-auto">
                  <div className="grid min-w-fit gap-2" aria-label="Place grid">
                    {visibleSeats.map((row) => (
                      <div className="flex gap-2" key={row[0].row}>
                        {row.map((seat) => {
                          const bookedSeat = bookedSeats.get(`${seat.row}:${seat.place}`);
                          const isSelected =
                            selectedSeat?.row === seat.row && selectedSeat.place === seat.place;
                          const isBooked = Boolean(bookedSeat);

                          return (
                            <button
                              aria-label={`Row ${seat.row}, place ${seat.place}`}
                              className={clsx(
                                'flex h-9 w-9 items-center justify-center rounded-md border text-xs font-black transition focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2',
                                isSelected &&
                                  'border-teal-800 bg-teal-700 text-white hover:bg-teal-800',
                                !isSelected &&
                                  !isBooked &&
                                  'border-slate-300 bg-white text-slate-800 hover:border-teal-700 hover:text-teal-800',
                                bookedSeat?.isMine === true &&
                                  'border-emerald-700 bg-emerald-100 text-emerald-900',
                                isBooked &&
                                  bookedSeat?.isMine !== true &&
                                  'border-slate-300 bg-slate-200 text-slate-500',
                              )}
                              disabled={isBooked}
                              key={`${seat.row}:${seat.place}`}
                              onClick={() => setSelectedSeat(seat)}
                              type="button"
                            >
                              {seat.place}
                            </button>
                          );
                        })}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              <div className="mt-5 flex flex-wrap gap-3 text-xs font-semibold text-slate-600">
                <Legend color="bg-white border-slate-300" text="Available" />
                <Legend color="bg-slate-200 border-slate-300" text="Booked" />
                <Legend color="bg-emerald-100 border-emerald-700" text="Mine" />
              </div>

              <button
                className="mt-5 flex w-full items-center justify-center gap-2 rounded-md bg-teal-700 px-5 py-3 text-sm font-bold text-white transition hover:bg-teal-800 disabled:cursor-not-allowed disabled:bg-slate-300 disabled:text-slate-600 focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2"
                disabled={isBooking || !selectedSeat}
                onClick={submitBooking}
                type="button"
              >
                {currentUser ? (
                  <Check className="h-4 w-4" aria-hidden="true" />
                ) : (
                  <Lock className="h-4 w-4" aria-hidden="true" />
                )}
                {currentUser ? 'Book selected place' : 'Login to book'}
              </button>
            </>
          ) : (
            <p className="text-sm font-semibold text-slate-600">
              Select an event to inspect places.
            </p>
          )}
        </aside>
      </div>

      {currentUser && (
        <section className="mt-8 rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
          <div className="flex items-center justify-between gap-4">
            <h3 className="text-lg font-black text-slate-950">My orders</h3>
            <button
              className="rounded-md border border-slate-300 px-3 py-2 text-sm font-bold text-slate-800 transition hover:border-teal-700 hover:text-teal-800 focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2"
              onClick={loadMyOrders}
              type="button"
            >
              Refresh
            </button>
          </div>

          {isLoadingOrders ? (
            <p className="mt-4 text-sm font-semibold text-slate-600">Loading orders...</p>
          ) : myOrders.length === 0 ? (
            <p className="mt-4 text-sm font-semibold text-slate-600">No orders yet.</p>
          ) : (
            <div className="mt-4 grid gap-3 md:grid-cols-2">
              {myOrders.map((order) => (
                <article
                  className="rounded-md border border-slate-200 p-4"
                  key={order.eventOrderId}
                >
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
      )}
    </section>
  );
}

function Legend({ color, text }: { color: string; text: string }) {
  return (
    <span className="flex items-center gap-2">
      <span className={clsx('h-3 w-3 rounded-sm border', color)} aria-hidden="true" />
      {text}
    </span>
  );
}

function formatDateTime(value: Date) {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(value);
}

function formatPrice(price?: string, currency?: string) {
  if (!price) {
    return 'TBA';
  }

  return currency ? `${price} ${currency}` : price;
}
