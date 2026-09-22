import type { CreateEventFormValues } from '../features/events/createEventSchema';
import type {
  CreateEventOrderItem,
  CreateEventRequest,
  EventListResponse,
  EventResponse,
  PatchEventRequest,
} from '../generated/api';
import { Configuration, EventListScope, EventsApi, ResponseError } from '../generated/api';
import { resolveApiBaseUrl, sessionAwareMiddleware } from './apiConfiguration';
import { prepareCsrfToken, withCsrfHeader } from './authClient';

const eventsApi = new EventsApi(
  new Configuration({
    basePath: resolveApiBaseUrl(),
    credentials: 'include',
    middleware: [sessionAwareMiddleware],
  }),
);

export type SeatSelection = {
  eventId: string;
  row: number;
  place: number;
  placeType: string;
};

export type PageQuery = {
  page: number;
  size: number;
};

export async function listPublishedEvents(query: PageQuery): Promise<EventListResponse> {
  return eventsApi.listPublishedEvents(query);
}

export async function listMyEvents(query: PageQuery): Promise<EventListResponse> {
  return eventsApi.listEvents({ ...query, scope: EventListScope.Mine });
}

export async function getAuthenticatedEvent(eventId: string): Promise<EventResponse> {
  return eventsApi.getEvent({ eventId });
}

export async function getPublishedEvent(eventId: string): Promise<EventResponse> {
  return eventsApi.getPublishedEvent({ eventId });
}

export async function createEventOrders(selection: SeatSelection): Promise<void> {
  await prepareCsrfToken();

  const order: CreateEventOrderItem = {
    eventId: selection.eventId,
    row: selection.row,
    place: selection.place,
    placeType: selection.placeType,
  };

  await eventsApi.createEventOrders(
    {
      createEventOrdersRequest: {
        orders: [order],
      },
    },
    withCsrfHeader,
  );
}

export async function createEvent(command: CreateEventFormValues): Promise<EventResponse> {
  await prepareCsrfToken();

  const createEventRequest: CreateEventRequest = {
    name: command.name,
    date: new Date(command.date),
    place: command.place,
    city: command.city || undefined,
    type: command.type,
    summary: command.summary || undefined,
    price: command.price || undefined,
    currency: command.currency || undefined,
    details: {
      description: command.details.description,
      numberOfPlaces: command.details.numberOfPlaces,
      numberOfRows: command.details.numberOfRows,
      seatsPerRow: command.details.seatsPerRow,
      placeTypes: command.details.placeTypes?.length ? command.details.placeTypes : undefined,
    },
  };

  return eventsApi.createEvent({ createEventRequest }, withCsrfHeader);
}

export async function patchEvent(
  eventId: string,
  command: CreateEventFormValues,
): Promise<EventResponse> {
  await prepareCsrfToken();

  const patchEventRequest: PatchEventRequest = {
    name: command.name,
    date: new Date(command.date),
    place: command.place,
    type: command.type,
    details: {
      description: command.details.description,
      numberOfPlaces: command.details.numberOfPlaces,
      numberOfRows: command.details.numberOfRows,
      seatsPerRow: command.details.seatsPerRow,
    },
  };

  return eventsApi.patchEvent({ eventId, patchEventRequest }, withCsrfHeader);
}

export async function publishEvent(eventId: string): Promise<EventResponse> {
  await prepareCsrfToken();

  return eventsApi.publishEvent({ eventId }, withCsrfHeader);
}

export async function unpublishEvent(eventId: string): Promise<EventResponse> {
  await prepareCsrfToken();

  return eventsApi.unpublishEvent({ eventId }, withCsrfHeader);
}

export function toEventUserMessage(error: unknown): string {
  if (error instanceof ResponseError) {
    if (error.response.status === 401) {
      return 'Login is required for this action.';
    }

    if (error.response.status === 403) {
      return 'This account cannot perform this action.';
    }

    if (error.response.status === 409) {
      return 'This place is no longer available.';
    }

    if (error.response.status === 400) {
      return 'Check the selected place and try again.';
    }
  }

  return 'Events are unavailable. Try again in a moment.';
}
