import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  createEventOrders,
  getAuthenticatedEvent,
  getPublishedEvent,
} from '../../../../src/api/eventsClient';
import { EventDetailsPanel } from '../../../../src/features/events/EventDetailsPanel';
import { ResponseError } from '../../../../src/generated/api';
import { buyerUser } from '../../../support/authTestData';
import { bookedEvent, publishedEvent } from '../../../support/eventTestData';
import { renderWithQueryClient } from '../../../support/renderWithQueryClient';

vi.mock('../../../../src/api/eventsClient', async () => {
  const actual = await vi.importActual<typeof import('../../../../src/api/eventsClient')>(
    '../../../../src/api/eventsClient',
  );

  return {
    ...actual,
    getPublishedEvent: vi.fn(),
    getAuthenticatedEvent: vi.fn(),
    createEventOrders: vi.fn(),
  };
});

const mockedGetPublishedEvent = vi.mocked(getPublishedEvent);
const mockedGetAuthenticatedEvent = vi.mocked(getAuthenticatedEvent);
const mockedCreateEventOrders = vi.mocked(createEventOrders);

describe('EventDetailsPanel', () => {
  beforeEach(() => {
    mockedGetPublishedEvent.mockReset();
    mockedGetAuthenticatedEvent.mockReset();
    mockedCreateEventOrders.mockReset();
  });

  it('prompts to select an event when none is selected', () => {
    renderWithQueryClient(
      <EventDetailsPanel eventId={null} currentUser={null} onLogin={vi.fn()} />,
    );

    expect(screen.getByText('Select an event to inspect places.')).toBeVisible();
  });

  it('shows a loading state while details resolve', () => {
    mockedGetPublishedEvent.mockReturnValue(new Promise(() => {}));

    renderWithQueryClient(
      <EventDetailsPanel eventId="event-1" currentUser={null} onLogin={vi.fn()} />,
    );

    expect(screen.getByText('Loading places...')).toBeVisible();
  });

  it('shows a mapped error message when the detail request fails', async () => {
    mockedGetPublishedEvent.mockRejectedValue(
      new ResponseError(new Response(null, { status: 500 }), 'Server error'),
    );

    renderWithQueryClient(
      <EventDetailsPanel eventId="event-1" currentUser={null} onLogin={vi.fn()} />,
    );

    expect(await screen.findByText('Events are unavailable. Try again in a moment.')).toBeVisible();
  });

  it('renders the seat grid from the public endpoint for anonymous users', async () => {
    mockedGetPublishedEvent.mockResolvedValue(publishedEvent);

    renderWithQueryClient(
      <EventDetailsPanel eventId="event-1" currentUser={null} onLogin={vi.fn()} />,
    );

    expect(await screen.findByRole('heading', { name: 'The Horizon Live' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Row 1, place 1' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Row 1, place 2' })).toBeEnabled();
    expect(mockedGetAuthenticatedEvent).not.toHaveBeenCalled();
  });

  it('opens login instead of booking for anonymous users', async () => {
    mockedGetPublishedEvent.mockResolvedValue(publishedEvent);
    const onLogin = vi.fn();
    const user = userEvent.setup();

    renderWithQueryClient(
      <EventDetailsPanel eventId="event-1" currentUser={null} onLogin={onLogin} />,
    );

    await user.click(await screen.findByRole('button', { name: 'Row 1, place 2' }));
    await user.click(screen.getByRole('button', { name: 'Login to book' }));

    expect(onLogin).toHaveBeenCalledTimes(1);
    expect(mockedCreateEventOrders).not.toHaveBeenCalled();
  });

  it('books a selected place for an authenticated user and shows a confirmation', async () => {
    mockedGetAuthenticatedEvent
      .mockResolvedValueOnce(publishedEvent)
      .mockResolvedValue(bookedEvent);
    mockedCreateEventOrders.mockResolvedValue();
    const user = userEvent.setup();

    renderWithQueryClient(
      <EventDetailsPanel eventId="event-1" currentUser={buyerUser} onLogin={vi.fn()} />,
    );

    await user.click(await screen.findByRole('button', { name: 'Row 1, place 2' }));
    await user.click(screen.getByRole('button', { name: 'Book selected place' }));

    await waitFor(() => {
      expect(mockedCreateEventOrders).toHaveBeenCalledWith({
        eventId: 'event-1',
        row: 1,
        place: 2,
        placeType: 'STANDARD',
      });
    });
    expect(await screen.findByText('Place booked.')).toBeVisible();
    expect(await screen.findByRole('button', { name: 'Row 1, place 2' })).toBeDisabled();
  });

  it('shows a mapped error message when booking fails', async () => {
    mockedGetAuthenticatedEvent.mockResolvedValue(publishedEvent);
    mockedCreateEventOrders.mockRejectedValue(
      new ResponseError(new Response(null, { status: 409 }), 'Conflict'),
    );
    const user = userEvent.setup();

    renderWithQueryClient(
      <EventDetailsPanel eventId="event-1" currentUser={buyerUser} onLogin={vi.fn()} />,
    );

    await user.click(await screen.findByRole('button', { name: 'Row 1, place 2' }));
    await user.click(screen.getByRole('button', { name: 'Book selected place' }));

    expect(await screen.findByText('This place is no longer available.')).toBeVisible();
  });
});
