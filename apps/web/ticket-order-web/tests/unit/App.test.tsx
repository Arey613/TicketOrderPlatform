import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import App from '../../src/App';
import { login, logout, register, toUserMessage } from '../../src/api/authClient';
import {
  createEventOrders,
  getEvent,
  listEvents,
  listMyEventOrders,
} from '../../src/api/eventsClient';
import { submitLoginForm, submitRegistrationForm } from '../support/appTestActions';
import {
  adminUser,
  buyerUser,
  managerUser,
  newBuyerUser,
  storedUserKey,
} from '../support/authTestData';

vi.mock('../../src/api/authClient', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  register: vi.fn(),
  toUserMessage: vi.fn(),
}));

vi.mock('../../src/api/eventsClient', () => ({
  createEventOrders: vi.fn(),
  getEvent: vi.fn(),
  listEvents: vi.fn(),
  listMyEventOrders: vi.fn(),
  toEventUserMessage: vi.fn(),
}));

const mockedLogin = vi.mocked(login);
const mockedLogout = vi.mocked(logout);
const mockedRegister = vi.mocked(register);
const mockedToUserMessage = vi.mocked(toUserMessage);
const mockedCreateEventOrders = vi.mocked(createEventOrders);
const mockedGetEvent = vi.mocked(getEvent);
const mockedListEvents = vi.mocked(listEvents);
const mockedListMyEventOrders = vi.mocked(listMyEventOrders);

const publishedEvent = {
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
} as const;

const bookedEvent = {
  ...publishedEvent,
  availablePlaces: 2,
  takenPlaces: [...publishedEvent.takenPlaces, { row: 1, place: 2, isMine: true }],
} as const;

const myEventOrder = {
  eventOrderId: 'order-1',
  eventId: publishedEvent.eventId,
  eventName: publishedEvent.name,
  eventDate: publishedEvent.date,
  row: 1,
  place: 2,
  placeType: 'STANDARD',
  reservationDate: new Date('2026-08-24T10:00:00.000Z'),
} as const;

describe('App', () => {
  beforeEach(() => {
    localStorage.clear();
    mockedLogin.mockReset();
    mockedLogout.mockReset();
    mockedRegister.mockReset();
    mockedToUserMessage.mockReset();
    mockedToUserMessage.mockResolvedValue('The email or password is not valid.');
    mockedCreateEventOrders.mockReset();
    mockedCreateEventOrders.mockResolvedValue();
    mockedGetEvent.mockReset();
    mockedGetEvent.mockResolvedValue(publishedEvent);
    mockedListEvents.mockReset();
    mockedListEvents.mockResolvedValue([publishedEvent]);
    mockedListMyEventOrders.mockReset();
    mockedListMyEventOrders.mockResolvedValue([]);
  });

  it('renders the public ticketing page with published events', async () => {
    render(<App />);

    expect(
      screen.getByRole('heading', {
        name: 'Order tickets without queues',
      }),
    ).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Upcoming events' })).toBeVisible();
    expect(await screen.findAllByRole('heading', { name: 'The Horizon Live' })).toHaveLength(2);
    expect(screen.getByText('3 left')).toBeVisible();
    expect(screen.getByText('Live concert with reserved places.')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Row 1, place 1' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Row 1, place 2' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Create account' })).toBeVisible();
    expect(screen.getAllByRole('button', { name: 'Login' })[0]).toBeVisible();
  });

  it('opens login instead of booking for public users', async () => {
    const user = userEvent.setup();
    render(<App />);

    await screen.findAllByRole('heading', { name: 'The Horizon Live' });
    await user.click(screen.getByRole('button', { name: 'Row 1, place 2' }));
    await user.click(screen.getByRole('button', { name: 'Login to book' }));

    expect(await screen.findByRole('dialog', { name: 'Login' })).toBeVisible();
    expect(mockedCreateEventOrders).not.toHaveBeenCalled();
  });

  it('books a selected place for an authenticated customer and refreshes owned orders', async () => {
    const user = userEvent.setup();
    localStorage.setItem(storedUserKey, JSON.stringify(buyerUser));
    mockedGetEvent.mockResolvedValue(bookedEvent);
    mockedListMyEventOrders.mockResolvedValue([myEventOrder]);

    render(<App />);

    await screen.findByText('buyer@example.com');
    await user.click(screen.getByRole('button', { name: 'Row 1, place 2' }));
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
    expect(screen.getByText('Row 1, place 2')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Row 1, place 2' })).toBeDisabled();
  });

  it('opens and closes the lazy login panel', async () => {
    const user = userEvent.setup();
    render(<App />);

    const loginButton = screen.getAllByRole('button', { name: 'Login' })[0];
    await user.click(loginButton);

    expect(await screen.findByRole('dialog', { name: 'Login' })).toBeVisible();

    await user.keyboard('{Escape}');

    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: 'Login' })).not.toBeInTheDocument();
    });
    expect(loginButton).toHaveFocus();
  });

  it('keeps keyboard focus inside the login panel', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getAllByRole('button', { name: 'Login' })[0]);
    const dialog = await screen.findByRole('dialog', { name: 'Login' });
    const closeButton = screen.getByRole('button', { name: 'Close account form' });
    const modeSwitchButton = within(dialog).getByRole('button', { name: 'Create account' });

    expect(closeButton).toHaveFocus();

    await user.keyboard('{Shift>}{Tab}{/Shift}');

    expect(modeSwitchButton).toHaveFocus();

    await user.tab();

    expect(closeButton).toHaveFocus();
    expect(dialog).toBeVisible();
  });

  it('logs in through the auth panel and shows the current user', async () => {
    const user = userEvent.setup();
    mockedLogin.mockResolvedValue(buyerUser);

    render(<App />);

    await submitLoginForm(user, 'buyer@example.com', 'correct-password');

    await waitFor(() => {
      expect(mockedLogin).toHaveBeenCalledWith({
        email: 'buyer@example.com',
        password: 'correct-password',
      });
    });

    expect(await screen.findByText('buyer@example.com')).toBeVisible();
    expect(screen.getByText('CUSTOMER')).toBeVisible();
    expect(localStorage.getItem(storedUserKey)).toBe(JSON.stringify(buyerUser));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Create account' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Login' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Browse events' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'My orders' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'My tickets' })).toBeVisible();
  });

  it('switches to registration and creates an account', async () => {
    const user = userEvent.setup();
    mockedRegister.mockResolvedValue(newBuyerUser);

    render(<App />);

    await submitRegistrationForm(user, 'new-buyer@example.com', 'new-password');

    await waitFor(() => {
      expect(mockedRegister).toHaveBeenCalledWith({
        email: 'new-buyer@example.com',
        password: 'new-password',
      });
    });

    expect(await screen.findByText('new-buyer@example.com')).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Create account' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Login' })).not.toBeInTheDocument();
  });

  it('shows an auth error without closing the panel', async () => {
    const user = userEvent.setup();
    const error = new Error('Unauthorized');
    mockedLogin.mockRejectedValue(error);

    render(<App />);

    await submitLoginForm(user, 'buyer@example.com', 'wrong-password');

    expect(await screen.findByText('The email or password is not valid.')).toBeVisible();
    expect(screen.getByRole('dialog', { name: 'Login' })).toBeVisible();
    expect(mockedToUserMessage).toHaveBeenCalledWith(error);
  });

  it('logs out an authenticated user', async () => {
    const user = userEvent.setup();
    mockedLogin.mockResolvedValue(buyerUser);
    mockedLogout.mockResolvedValue();

    render(<App />);

    await submitLoginForm(user, 'buyer@example.com', 'correct-password');
    expect(await screen.findByText('buyer@example.com')).toBeVisible();

    await user.click(screen.getByRole('button', { name: 'Logout' }));

    await waitFor(() => {
      expect(mockedLogout).toHaveBeenCalled();
    });

    expect(screen.queryByText('buyer@example.com')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Create account' })).toBeVisible();
    expect(screen.getAllByRole('button', { name: 'Login' })[0]).toBeVisible();
    expect(localStorage.getItem(storedUserKey)).toBeNull();
  });

  it('restores an authenticated user from local storage', () => {
    localStorage.setItem(storedUserKey, JSON.stringify(buyerUser));

    render(<App />);

    expect(screen.getByText('buyer@example.com')).toBeVisible();
    expect(screen.getByText('CUSTOMER')).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Login' })).not.toBeInTheDocument();
  });

  it('shows manager-specific actions for manager users', () => {
    localStorage.setItem(storedUserKey, JSON.stringify(managerUser));

    render(<App />);

    expect(screen.getByText('manager@example.com')).toBeVisible();
    expect(screen.getByRole('button', { name: 'My events' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Create event' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Event orders' })).toBeVisible();
    expect(screen.queryByRole('button', { name: 'User administration' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'My tickets' })).not.toBeInTheDocument();
  });

  it('shows admin-specific actions for admin users', () => {
    localStorage.setItem(storedUserKey, JSON.stringify(adminUser));

    render(<App />);

    expect(screen.getByText('admin@example.com')).toBeVisible();
    expect(screen.getByRole('button', { name: 'User administration' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Platform operations' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Event oversight' })).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Create event' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'My tickets' })).not.toBeInTheDocument();
  });
});
