import { act, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { login, logout, register, toUserMessage } from '../../src/api/authClient';
import {
  createEventOrders,
  getAuthenticatedEvent,
  getPublishedEvent,
  listMyEventOrders,
  listPublishedEvents,
} from '../../src/api/eventsClient';
import { notifySessionExpired } from '../../src/api/sessionEvents';
import { submitLoginForm, submitRegistrationForm } from '../support/appTestActions';
import {
  adminUser,
  buyerUser,
  managerUser,
  newBuyerUser,
  storedUserKey,
} from '../support/authTestData';
import { bookedEvent, myEventOrder, pageMetadata, publishedEvent } from '../support/eventTestData';
import { renderApp } from '../support/renderApp';

vi.mock('../../src/api/authClient', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  register: vi.fn(),
  toUserMessage: vi.fn(),
}));

vi.mock('../../src/api/eventsClient', () => ({
  createEventOrders: vi.fn(),
  getAuthenticatedEvent: vi.fn(),
  getPublishedEvent: vi.fn(),
  listPublishedEvents: vi.fn(),
  listMyEventOrders: vi.fn(),
  toEventUserMessage: vi.fn(),
}));

const mockedLogin = vi.mocked(login);
const mockedLogout = vi.mocked(logout);
const mockedRegister = vi.mocked(register);
const mockedToUserMessage = vi.mocked(toUserMessage);
const mockedCreateEventOrders = vi.mocked(createEventOrders);
const mockedGetAuthenticatedEvent = vi.mocked(getAuthenticatedEvent);
const mockedGetPublishedEvent = vi.mocked(getPublishedEvent);
const mockedListPublishedEvents = vi.mocked(listPublishedEvents);
const mockedListMyEventOrders = vi.mocked(listMyEventOrders);

describe('App', () => {
  beforeEach(() => {
    localStorage.clear();
    mockedLogin.mockReset();
    mockedLogout.mockReset();
    mockedRegister.mockReset();
    mockedToUserMessage.mockReset();
    mockedToUserMessage.mockReturnValue('The email or password is not valid.');
    mockedCreateEventOrders.mockReset();
    mockedCreateEventOrders.mockResolvedValue();
    mockedGetAuthenticatedEvent.mockReset();
    mockedGetAuthenticatedEvent.mockResolvedValue(publishedEvent);
    mockedGetPublishedEvent.mockReset();
    mockedGetPublishedEvent.mockResolvedValue(publishedEvent);
    mockedListPublishedEvents.mockReset();
    mockedListPublishedEvents.mockResolvedValue({
      items: [publishedEvent],
      page: pageMetadata(10, 1),
    });
    mockedListMyEventOrders.mockReset();
    mockedListMyEventOrders.mockResolvedValue({
      items: [],
      page: pageMetadata(20, 0),
    });
  });

  it('renders the public ticketing page with published events', async () => {
    renderApp();

    expect(
      screen.getByRole('heading', {
        name: 'Order tickets without queues',
      }),
    ).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Upcoming events' })).toBeVisible();
    await waitFor(() => {
      expect(screen.getAllByRole('heading', { name: 'The Horizon Live' })).toHaveLength(2);
    });
    expect(screen.getByText('3 left')).toBeVisible();
    expect(screen.getByText('Live concert with reserved places.')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Row 1, place 1' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Row 1, place 2' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Create account' })).toBeVisible();
    expect(screen.getAllByRole('button', { name: 'Login' })[0]).toBeVisible();
    expect(mockedListPublishedEvents).toHaveBeenCalledWith({ page: 0, size: 10 });
    expect(mockedGetAuthenticatedEvent).not.toHaveBeenCalled();
    expect(mockedListMyEventOrders).not.toHaveBeenCalled();
  });

  it('loads selected event details through the public endpoint for public users', async () => {
    const user = userEvent.setup();
    renderApp();

    await screen.findAllByRole('heading', { name: 'The Horizon Live' });
    await user.click(screen.getByRole('button', { name: 'Select' }));

    await waitFor(() => {
      expect(mockedGetPublishedEvent).toHaveBeenCalledWith('event-1');
    });
    expect(mockedGetAuthenticatedEvent).not.toHaveBeenCalled();
  });

  it('opens login instead of booking for public users', async () => {
    const user = userEvent.setup();
    renderApp();

    await screen.findAllByRole('heading', { name: 'The Horizon Live' });
    await user.click(await screen.findByRole('button', { name: 'Row 1, place 2' }));
    await user.click(screen.getByRole('button', { name: 'Login to book' }));

    expect(await screen.findByRole('dialog', { name: 'Login' })).toBeVisible();
    expect(mockedCreateEventOrders).not.toHaveBeenCalled();
  });

  it('books a selected place for an authenticated customer and refreshes owned orders', async () => {
    const user = userEvent.setup();
    localStorage.setItem(storedUserKey, JSON.stringify(buyerUser));
    mockedGetAuthenticatedEvent.mockReset();
    mockedGetAuthenticatedEvent
      .mockResolvedValueOnce(publishedEvent)
      .mockResolvedValue(bookedEvent);
    mockedListMyEventOrders.mockResolvedValue({
      items: [myEventOrder],
      page: pageMetadata(20, 1),
    });

    renderApp();

    await screen.findByText('buyer@example.com');
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
    expect(screen.getByText('Row 1, place 2')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Row 1, place 2' })).toBeDisabled();
  });

  it('opens and closes the lazy login panel', async () => {
    const user = userEvent.setup();
    renderApp();

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
    renderApp();

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

    renderApp();

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
  });

  it('switches to registration and creates an account', async () => {
    const user = userEvent.setup();
    mockedRegister.mockResolvedValue(newBuyerUser);

    renderApp();

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

    renderApp();

    await submitLoginForm(user, 'buyer@example.com', 'wrong-password');

    expect(await screen.findByText('The email or password is not valid.')).toBeVisible();
    expect(screen.getByRole('dialog', { name: 'Login' })).toBeVisible();
    expect(mockedToUserMessage).toHaveBeenCalledWith(error);
  });

  it('logs out an authenticated user', async () => {
    const user = userEvent.setup();
    mockedLogin.mockResolvedValue(buyerUser);
    mockedLogout.mockResolvedValue();

    renderApp();

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

  it('clears the authenticated session and shows a message when the session expires', async () => {
    localStorage.setItem(storedUserKey, JSON.stringify(buyerUser));

    renderApp();

    expect(await screen.findByText('buyer@example.com')).toBeVisible();

    act(() => {
      notifySessionExpired();
    });

    await waitFor(() => {
      expect(screen.queryByText('buyer@example.com')).not.toBeInTheDocument();
    });
    expect(screen.getByText('Your session has expired. Please log in again.')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Create account' })).toBeVisible();
    expect(localStorage.getItem(storedUserKey)).toBeNull();
  });

  it('restores an authenticated user from local storage', () => {
    localStorage.setItem(storedUserKey, JSON.stringify(buyerUser));

    renderApp();

    expect(screen.getByText('buyer@example.com')).toBeVisible();
    expect(screen.getByText('CUSTOMER')).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Login' })).not.toBeInTheDocument();
  });

  it('does not render placeholder role-action buttons for any authenticated role', () => {
    localStorage.setItem(storedUserKey, JSON.stringify(managerUser));
    const { unmount } = renderApp();

    expect(screen.getByText('manager@example.com')).toBeVisible();
    expect(screen.queryByRole('button', { name: 'My events' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Create event' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Event orders' })).not.toBeInTheDocument();
    unmount();

    localStorage.setItem(storedUserKey, JSON.stringify(adminUser));
    renderApp();

    expect(screen.getByText('admin@example.com')).toBeVisible();
    expect(screen.queryByRole('button', { name: 'User administration' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Platform operations' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Event oversight' })).not.toBeInTheDocument();
  });
});
