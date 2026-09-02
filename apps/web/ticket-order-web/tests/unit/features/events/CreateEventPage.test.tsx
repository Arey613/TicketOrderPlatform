import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createEvent } from '../../../../src/api/eventsClient';
import { CreateEventPage } from '../../../../src/features/events/CreateEventPage';
import { ResponseError } from '../../../../src/generated/api';
import { renderWithQueryClient } from '../../../support/renderWithQueryClient';

vi.mock('../../../../src/api/eventsClient', async () => {
  const actual = await vi.importActual<typeof import('../../../../src/api/eventsClient')>(
    '../../../../src/api/eventsClient',
  );

  return {
    ...actual,
    createEvent: vi.fn(),
  };
});

const mockedCreateEvent = vi.mocked(createEvent);

function renderCreateEventPage() {
  return renderWithQueryClient(
    <MemoryRouter initialEntries={['/events/create']}>
      <Routes>
        <Route path="/" element={<div>Home page</div>} />
        <Route path="/events/create" element={<CreateEventPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

async function fillRequiredFields(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('Name'), 'Summer music night');
  await user.type(screen.getByLabelText('Date'), '2026-09-10T19:00');
  await user.type(screen.getByLabelText('Type'), 'CONCERT');
  await user.type(screen.getByLabelText('Place'), 'Central Hall');
  await user.type(screen.getByLabelText('Description'), 'Outdoor concert with reserved seating');
  await user.type(screen.getByLabelText('Places'), '120');
  await user.type(screen.getByLabelText('Rows'), '12');
  await user.type(screen.getByLabelText('Per row'), '10');
}

describe('CreateEventPage', () => {
  beforeEach(() => {
    mockedCreateEvent.mockReset();
  });

  it('focuses the page heading on mount', () => {
    renderCreateEventPage();

    expect(screen.getByRole('heading', { name: 'Create event' })).toHaveFocus();
  });

  it('blocks submission and shows field errors when required fields are missing', async () => {
    const user = userEvent.setup();
    renderCreateEventPage();

    await user.click(screen.getByRole('button', { name: 'Create event' }));

    expect(await screen.findByText('Enter an event name.')).toBeVisible();
    expect(screen.getByText('Enter a date.')).toBeVisible();
    expect(screen.getByText('Enter a place.')).toBeVisible();
    expect(screen.getByText('Enter an event type.')).toBeVisible();
    expect(screen.getByText('Enter a description.')).toBeVisible();
    expect(mockedCreateEvent).not.toHaveBeenCalled();
  });

  it('rejects non-positive capacity fields', async () => {
    const user = userEvent.setup();
    renderCreateEventPage();

    await fillRequiredFields(user);
    await user.clear(screen.getByLabelText('Places'));
    await user.type(screen.getByLabelText('Places'), '0');
    await user.click(screen.getByRole('button', { name: 'Create event' }));

    expect(await screen.findByText('Enter a positive number of places.')).toBeVisible();
    expect(mockedCreateEvent).not.toHaveBeenCalled();
  });

  it('submits the mapped form values and navigates home on success', async () => {
    mockedCreateEvent.mockResolvedValue({
      eventId: 'event-1',
      ownerId: 'owner-1',
      name: 'Summer music night',
      date: new Date('2026-09-10T19:00:00Z'),
      place: 'Central Hall',
      type: 'CONCERT',
      status: 'DRAFT',
      details: {
        description: 'Outdoor concert with reserved seating',
        numberOfPlaces: 120,
        numberOfRows: 12,
        seatsPerRow: 10,
      },
      ordersTaken: 0,
      takenPlaces: [],
    });
    const user = userEvent.setup();
    renderCreateEventPage();

    await fillRequiredFields(user);
    await user.click(screen.getByRole('button', { name: 'Create event' }));

    await waitFor(() => {
      expect(mockedCreateEvent).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'Summer music night',
          date: '2026-09-10T19:00',
          place: 'Central Hall',
          type: 'CONCERT',
          city: '',
          summary: '',
          imageUrl: '',
          price: '',
          currency: '',
          details: expect.objectContaining({
            description: 'Outdoor concert with reserved seating',
            numberOfPlaces: 120,
            numberOfRows: 12,
            seatsPerRow: 10,
            placeTypes: [],
          }),
        }),
      );
    });
    expect(await screen.findByText('Home page')).toBeVisible();
  });

  it('adds and removes place-type rows', async () => {
    mockedCreateEvent.mockResolvedValue({
      eventId: 'event-1',
      ownerId: 'owner-1',
      name: 'Summer music night',
      date: new Date('2026-09-10T19:00:00Z'),
      place: 'Central Hall',
      type: 'CONCERT',
      status: 'DRAFT',
      details: {
        description: 'Outdoor concert with reserved seating',
        numberOfPlaces: 120,
        numberOfRows: 12,
        seatsPerRow: 10,
      },
      ordersTaken: 0,
      takenPlaces: [],
    });
    const user = userEvent.setup();
    renderCreateEventPage();

    await fillRequiredFields(user);
    await user.click(screen.getByRole('button', { name: 'Add place type' }));
    await user.type(screen.getByLabelText('Place type 1 name'), 'VIP');
    await user.type(screen.getByLabelText('Place type 1 price'), '45.00');
    await user.type(screen.getByLabelText('Place type 1 currency'), 'USD');
    await user.click(screen.getByRole('button', { name: 'Add place type' }));
    await user.click(screen.getByRole('button', { name: 'Remove place type 2' }));

    await user.click(screen.getByRole('button', { name: 'Create event' }));

    await waitFor(() => {
      expect(mockedCreateEvent).toHaveBeenCalledWith(
        expect.objectContaining({
          details: expect.objectContaining({
            placeTypes: [{ name: 'VIP', price: '45.00', currency: 'USD' }],
          }),
        }),
      );
    });
  });

  it('shows a mapped error message without navigating away on failure', async () => {
    mockedCreateEvent.mockRejectedValue(
      new ResponseError(new Response(null, { status: 403 }), 'Forbidden'),
    );
    const user = userEvent.setup();
    renderCreateEventPage();

    await fillRequiredFields(user);
    await user.click(screen.getByRole('button', { name: 'Create event' }));

    expect(await screen.findByText('This account cannot perform this action.')).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Create event' })).toBeVisible();
    expect(screen.queryByText('Home page')).not.toBeInTheDocument();
  });
});
