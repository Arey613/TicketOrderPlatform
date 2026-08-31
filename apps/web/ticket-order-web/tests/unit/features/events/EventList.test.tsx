import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { listPublishedEvents } from '../../../../src/api/eventsClient';
import { EventList } from '../../../../src/features/events/EventList';
import { ResponseError } from '../../../../src/generated/api';
import { pageMetadata, publishedEvent } from '../../../support/eventTestData';
import { renderWithQueryClient } from '../../../support/renderWithQueryClient';

vi.mock('../../../../src/api/eventsClient', async () => {
  const actual = await vi.importActual<typeof import('../../../../src/api/eventsClient')>(
    '../../../../src/api/eventsClient',
  );

  return {
    ...actual,
    listPublishedEvents: vi.fn(),
  };
});

const mockedListPublishedEvents = vi.mocked(listPublishedEvents);

describe('EventList', () => {
  beforeEach(() => {
    mockedListPublishedEvents.mockReset();
  });

  it('shows a loading state before events resolve', () => {
    mockedListPublishedEvents.mockReturnValue(new Promise(() => {}));

    renderWithQueryClient(<EventList selectedEventId={null} onSelectEvent={vi.fn()} />);

    expect(screen.getByText('Loading events...')).toBeVisible();
  });

  it('shows an empty state when there are no published events', async () => {
    mockedListPublishedEvents.mockResolvedValue({ items: [], page: pageMetadata(10, 0) });

    renderWithQueryClient(<EventList selectedEventId={null} onSelectEvent={vi.fn()} />);

    expect(await screen.findByText('No published events yet.')).toBeVisible();
  });

  it('shows a mapped error message when the request fails', async () => {
    mockedListPublishedEvents.mockRejectedValue(
      new ResponseError(new Response(null, { status: 500 }), 'Server error'),
    );

    renderWithQueryClient(<EventList selectedEventId={null} onSelectEvent={vi.fn()} />);

    expect(await screen.findByText('Events are unavailable. Try again in a moment.')).toBeVisible();
  });

  it('renders published events and auto-selects the first one', async () => {
    mockedListPublishedEvents.mockResolvedValue({
      items: [publishedEvent],
      page: pageMetadata(10, 1),
    });
    const onSelectEvent = vi.fn();

    renderWithQueryClient(<EventList selectedEventId={null} onSelectEvent={onSelectEvent} />);

    expect(await screen.findByRole('heading', { name: 'The Horizon Live' })).toBeVisible();
    await waitFor(() => {
      expect(onSelectEvent).toHaveBeenCalledWith('event-1');
    });
  });

  it('does not auto-select when an event is already selected', async () => {
    mockedListPublishedEvents.mockResolvedValue({
      items: [publishedEvent],
      page: pageMetadata(10, 1),
    });
    const onSelectEvent = vi.fn();

    renderWithQueryClient(<EventList selectedEventId="event-1" onSelectEvent={onSelectEvent} />);

    await screen.findByRole('heading', { name: 'The Horizon Live' });
    expect(onSelectEvent).not.toHaveBeenCalled();
  });

  it('calls onSelectEvent when a Select button is clicked', async () => {
    mockedListPublishedEvents.mockResolvedValue({
      items: [publishedEvent],
      page: pageMetadata(10, 1),
    });
    const onSelectEvent = vi.fn();
    const user = userEvent.setup();

    renderWithQueryClient(<EventList selectedEventId="event-1" onSelectEvent={onSelectEvent} />);

    await user.click(await screen.findByRole('button', { name: 'Select' }));
    expect(onSelectEvent).toHaveBeenCalledWith('event-1');
  });

  it('refetches events when Refresh is clicked', async () => {
    mockedListPublishedEvents.mockResolvedValue({
      items: [publishedEvent],
      page: pageMetadata(10, 1),
    });
    const user = userEvent.setup();

    renderWithQueryClient(<EventList selectedEventId="event-1" onSelectEvent={vi.fn()} />);

    await screen.findByRole('heading', { name: 'The Horizon Live' });
    await user.click(screen.getByRole('button', { name: 'Refresh' }));

    await waitFor(() => {
      expect(mockedListPublishedEvents).toHaveBeenCalledTimes(2);
    });
  });
});
