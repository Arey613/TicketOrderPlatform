import { useState } from 'react';
import type { AuthenticatedUser } from '../../api/authClient';
import { MyOrdersPanel } from '../orders/MyOrdersPanel';
import { EventDetailsPanel } from './EventDetailsPanel';
import { EventList } from './EventList';

type EventsSectionProps = {
  currentUser: AuthenticatedUser | null;
  onLogin: () => void;
};

export function EventsSection({ currentUser, onLogin }: EventsSectionProps) {
  const [selectedEventId, setSelectedEventId] = useState<string | null>(null);

  return (
    <section className="mx-auto w-full max-w-7xl px-4 py-10 sm:px-6 lg:px-8" id="events">
      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_420px]">
        <EventList selectedEventId={selectedEventId} onSelectEvent={setSelectedEventId} />
        <EventDetailsPanel
          // remounts the panel on event change, resetting seat selection and booking status locally
          key={selectedEventId ?? 'none'}
          eventId={selectedEventId}
          currentUser={currentUser}
          onLogin={onLogin}
        />
      </div>

      {currentUser && <MyOrdersPanel />}
    </section>
  );
}
