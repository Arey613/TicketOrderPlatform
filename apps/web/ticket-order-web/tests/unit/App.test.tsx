import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import App from '../../src/App';
import { login, logout, register, toUserMessage } from '../../src/api/authClient';
import { submitLoginForm, submitRegistrationForm } from '../support/appTestActions';
import { buyerUser, newBuyerUser, storedUserKey } from '../support/authTestData';

vi.mock('../../src/api/authClient', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  register: vi.fn(),
  toUserMessage: vi.fn(),
}));

const mockedLogin = vi.mocked(login);
const mockedLogout = vi.mocked(logout);
const mockedRegister = vi.mocked(register);
const mockedToUserMessage = vi.mocked(toUserMessage);

describe('App', () => {
  beforeEach(() => {
    localStorage.clear();
    mockedLogin.mockReset();
    mockedLogout.mockReset();
    mockedRegister.mockReset();
    mockedToUserMessage.mockReset();
    mockedToUserMessage.mockResolvedValue('The email or password is not valid.');
  });

  it('renders the public ticketing page with static event previews', () => {
    render(<App />);

    expect(
      screen.getByRole('heading', {
        name: 'Order tickets without queues',
      }),
    ).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Upcoming events' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'The Horizon Live' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'City Hoops Finals' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Laugh Out Loud' })).toBeVisible();
    expect(
      screen.getByText('Static event previews are temporary until event APIs are wired.'),
    ).toBeVisible();
  });

  it('opens and closes the lazy login panel', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getAllByRole('button', { name: 'Login' })[0]);

    expect(await screen.findByRole('dialog', { name: 'Login' })).toBeVisible();

    await user.keyboard('{Escape}');

    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: 'Login' })).not.toBeInTheDocument();
    });
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
    expect(screen.getAllByRole('button', { name: 'Login' })[0]).toBeVisible();
    expect(localStorage.getItem(storedUserKey)).toBeNull();
  });

  it('restores an authenticated user from local storage', () => {
    localStorage.setItem(storedUserKey, JSON.stringify(buyerUser));

    render(<App />);

    expect(screen.getByText('buyer@example.com')).toBeVisible();
    expect(screen.getByText('CUSTOMER')).toBeVisible();
  });
});
