import { Configuration, EventsApi, ResponseError } from '../generated/api';
import type { CreateEventOrderItem, EventResponse, MyEventOrderResponse } from '../generated/api';
import { prepareCsrfToken, withCsrfHeader } from './authClient';

const apiBaseUrl = import.meta.env.VITE_TICKET_API_BASE_URL ?? 'http://localhost:8080';

const eventsApi = new EventsApi(
  new Configuration({
    basePath: apiBaseUrl,
    credentials: 'include',
  }),
);

export type SeatSelection = {
  eventId: string;
  row: number;
  place: number;
  placeType: string;
};

export async function listEvents(): Promise<EventResponse[]> {
  const response = await eventsApi.listEvents();
  return response.events;
}

export async function getEvent(eventId: string): Promise<EventResponse> {
  return eventsApi.getEvent({ eventId });
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

export async function listMyEventOrders(): Promise<MyEventOrderResponse[]> {
  const response = await eventsApi.listMyEventOrders();
  return response.orders;
}

export async function toEventUserMessage(error: unknown): Promise<string> {
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
