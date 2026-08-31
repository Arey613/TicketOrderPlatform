import type { EventResponse, MyEventOrderResponse, PageMetadata } from '../../src/generated/api';

export const publishedEvent = {
  eventId: 'event-1',
  ownerId: 'manager-1',
  name: 'The Horizon Live',
  date: new Date('2026-09-12T19:30:00.000Z'),
  place: 'Riverside Arena',
  city: 'Chisinau',
  type: 'Rock concert',
  status: 'PUBLISHED',
  details: {
    description: 'Live concert with reserved places.',
    numberOfPlaces: 4,
    numberOfRows: 2,
    seatsPerRow: 2,
    availablePlaces: 3,
    placeTypes: [{ name: 'STANDARD', price: '59.00', currency: 'USD' }],
  },
  ordersTaken: 1,
  availablePlaces: 3,
  takenPlaces: [{ row: 1, place: 1 }],
} as const satisfies EventResponse;

export const bookedEvent = {
  ...publishedEvent,
  availablePlaces: 2,
  takenPlaces: [...publishedEvent.takenPlaces, { row: 1, place: 2, isMine: true }],
} as const satisfies EventResponse;

export const myEventOrder = {
  eventOrderId: 'order-1',
  eventId: publishedEvent.eventId,
  eventName: publishedEvent.name,
  eventDate: publishedEvent.date,
  row: 1,
  place: 2,
  placeType: 'STANDARD',
  reservationDate: new Date('2026-08-24T10:00:00.000Z'),
} as const satisfies MyEventOrderResponse;

export function pageMetadata(size: number, totalElements: number): PageMetadata {
  return {
    number: 0,
    size,
    totalElements,
    totalPages: totalElements === 0 ? 0 : 1,
    first: true,
    last: true,
  };
}
