import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App';
import { login, logout, register, toUserMessage } from './api/authClient';

vi.mock('./api/authClient', () => ({
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
    mockedLogin.mockResolvedValue({
      id: '4b804b8d-6a6f-44d3-9b98-a2ab33f8b8c2',
      email: 'buyer@example.com',
      role: 'CUSTOMER',
    });

    render(<App />);

    await user.click(screen.getAllByRole('button', { name: 'Login' })[0]);
    const dialog = await screen.findByRole('dialog', { name: 'Login' });
    await user.type(await screen.findByLabelText('Email'), 'buyer@example.com');
    await user.type(screen.getByLabelText('Password'), 'correct-password');
    await user.click(within(dialog).getByRole('button', { name: 'Login' }));

    await waitFor(() => {
      expect(mockedLogin).toHaveBeenCalledWith({
        email: 'buyer@example.com',
        password: 'correct-password',
      });
    });

    expect(await screen.findByText('buyer@example.com')).toBeVisible();
    expect(screen.getByText('CUSTOMER')).toBeVisible();
    expect(localStorage.getItem('ticketOrderPlatform.currentUser')).toBe(
      JSON.stringify({
        id: '4b804b8d-6a6f-44d3-9b98-a2ab33f8b8c2',
        email: 'buyer@example.com',
        role: 'CUSTOMER',
      }),
    );
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('switches to registration and creates an account', async () => {
    const user = userEvent.setup();
    mockedRegister.mockResolvedValue({
      id: '4f89e87e-9192-4a1a-9baa-7ad90a2ac5fd',
      email: 'new-buyer@example.com',
      role: 'CUSTOMER',
    });

    render(<App />);

    await user.click(screen.getAllByRole('button', { name: 'Create account' })[0]);
    const dialog = await screen.findByRole('dialog', { name: 'Create account' });
    await user.type(await within(dialog).findByLabelText('Email'), 'new-buyer@example.com');
    await user.type(screen.getByLabelText('Password'), 'new-password');
    await user.click(within(dialog).getByRole('button', { name: 'Create account' }));

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

    await user.click(screen.getAllByRole('button', { name: 'Login' })[0]);
    const dialog = await screen.findByRole('dialog', { name: 'Login' });
    await user.type(await screen.findByLabelText('Email'), 'buyer@example.com');
    await user.type(screen.getByLabelText('Password'), 'wrong-password');
    await user.click(within(dialog).getByRole('button', { name: 'Login' }));

    expect(await screen.findByText('The email or password is not valid.')).toBeVisible();
    expect(screen.getByRole('dialog', { name: 'Login' })).toBeVisible();
    expect(mockedToUserMessage).toHaveBeenCalledWith(error);
  });

  it('logs out an authenticated user', async () => {
    const user = userEvent.setup();
    mockedLogin.mockResolvedValue({
      id: '4b804b8d-6a6f-44d3-9b98-a2ab33f8b8c2',
      email: 'buyer@example.com',
      role: 'CUSTOMER',
    });
    mockedLogout.mockResolvedValue();

    render(<App />);

    await user.click(screen.getAllByRole('button', { name: 'Login' })[0]);
    const dialog = await screen.findByRole('dialog', { name: 'Login' });
    await user.type(await screen.findByLabelText('Email'), 'buyer@example.com');
    await user.type(screen.getByLabelText('Password'), 'correct-password');
    await user.click(within(dialog).getByRole('button', { name: 'Login' }));
    expect(await screen.findByText('buyer@example.com')).toBeVisible();

    await user.click(screen.getByRole('button', { name: 'Logout' }));

    await waitFor(() => {
      expect(mockedLogout).toHaveBeenCalled();
    });

    expect(screen.queryByText('buyer@example.com')).not.toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'Login' })[0]).toBeVisible();
    expect(localStorage.getItem('ticketOrderPlatform.currentUser')).toBeNull();
  });

  it('restores an authenticated user from local storage', () => {
    localStorage.setItem(
      'ticketOrderPlatform.currentUser',
      JSON.stringify({
        id: '4b804b8d-6a6f-44d3-9b98-a2ab33f8b8c2',
        email: 'buyer@example.com',
        role: 'CUSTOMER',
      }),
    );

    render(<App />);

    expect(screen.getByText('buyer@example.com')).toBeVisible();
    expect(screen.getByText('CUSTOMER')).toBeVisible();
  });
});
