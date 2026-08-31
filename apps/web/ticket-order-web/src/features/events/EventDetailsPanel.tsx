import clsx from 'clsx';
import { Check, Lock } from 'lucide-react';
import { useMemo, useState } from 'react';
import type { AuthenticatedUser } from '../../api/authClient';
import { toEventUserMessage } from '../../api/eventsClient';
import { useCreateEventOrderMutation } from './useCreateEventOrderMutation';
import { useEventDetailsQuery } from './useEventDetailsQuery';

type SelectedSeat = {
  row: number;
  place: number;
};

type EventDetailsPanelProps = {
  eventId: string | null;
  currentUser: AuthenticatedUser | null;
  onLogin: () => void;
};

export function EventDetailsPanel({ eventId, currentUser, onLogin }: EventDetailsPanelProps) {
  const [selectedSeat, setSelectedSeat] = useState<SelectedSeat | null>(null);
  const query = useEventDetailsQuery(eventId, Boolean(currentUser));
  const mutation = useCreateEventOrderMutation();
  const event = query.data;

  const bookedSeats = useMemo(
    () =>
      new Map((event?.takenPlaces ?? []).map((place) => [`${place.row}:${place.place}`, place])),
    [event],
  );
  const defaultPlaceType = event?.details.placeTypes?.[0]?.name ?? 'STANDARD';
  const visibleSeats = event
    ? Array.from({ length: event.details.numberOfRows }, (_, rowIndex) =>
        Array.from({ length: event.details.seatsPerRow }, (_, placeIndex) => ({
          row: rowIndex + 1,
          place: placeIndex + 1,
        })),
      )
    : [];

  const isStatusError = mutation.isError || query.isError;
  const statusMessage = mutation.isSuccess
    ? 'Place booked.'
    : mutation.isError
      ? toEventUserMessage(mutation.error)
      : query.isError
        ? toEventUserMessage(query.error)
        : '';

  function submitBooking() {
    if (!event || !selectedSeat) {
      return;
    }

    if (!currentUser) {
      onLogin();
      return;
    }

    mutation.mutate(
      {
        eventId: event.eventId,
        row: selectedSeat.row,
        place: selectedSeat.place,
        placeType: defaultPlaceType,
      },
      {
        onSuccess: () => setSelectedSeat(null),
      },
    );
  }

  return (
    <aside className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
      {!eventId && (
        <p className="text-sm font-semibold text-slate-600">Select an event to inspect places.</p>
      )}

      {eventId && query.isLoading && (
        <p className="text-sm font-semibold text-slate-600">Loading places...</p>
      )}

      {event && (
        <>
          <div className="flex items-start justify-between gap-4">
            <div>
              <h3 className="text-xl font-black text-slate-950">{event.name}</h3>
              <p className="mt-1 text-sm leading-6 text-slate-600">{event.details.description}</p>
            </div>
            <span className="shrink-0 rounded-md bg-emerald-50 px-3 py-2 text-sm font-bold text-emerald-800">
              {event.availablePlaces} left
            </span>
          </div>

          <div className="mt-5 overflow-x-auto">
            <fieldset className="m-0 grid min-w-fit gap-2 border-0 p-0" aria-label="Place grid">
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
                          isSelected && 'border-teal-800 bg-teal-700 text-white hover:bg-teal-800',
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
            </fieldset>
          </div>

          <div className="mt-5 flex flex-wrap gap-3 text-xs font-semibold text-slate-600">
            <Legend color="bg-white border-slate-300" text="Available" />
            <Legend color="bg-slate-200 border-slate-300" text="Booked" />
            <Legend color="bg-emerald-100 border-emerald-700" text="Mine" />
          </div>

          <button
            className="mt-5 flex w-full items-center justify-center gap-2 rounded-md bg-teal-700 px-5 py-3 text-sm font-bold text-white transition hover:bg-teal-800 disabled:cursor-not-allowed disabled:bg-slate-300 disabled:text-slate-600 focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2"
            disabled={mutation.isPending || !selectedSeat}
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
      )}

      <p
        aria-live="polite"
        className={
          statusMessage
            ? clsx(
                'mt-4 rounded-md border px-3 py-2 text-sm font-semibold',
                isStatusError
                  ? 'border-red-200 bg-red-50 text-red-800'
                  : 'border-teal-200 bg-teal-50 text-teal-900',
              )
            : 'sr-only'
        }
      >
        {statusMessage}
      </p>
    </aside>
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
