import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { listMyOrders } from '../../../../src/api/ordersClient';
import { MyOrdersPage } from '../../../../src/features/orders/MyOrdersPage';
import { myEventOrder, pageMetadata } from '../../../support/eventTestData';
import { renderWithQueryClient } from '../../../support/renderWithQueryClient';

vi.mock('../../../../src/api/ordersClient', async () => {
  const actual = await vi.importActual<typeof import('../../../../src/api/ordersClient')>(
    '../../../../src/api/ordersClient',
  );

  return {
    ...actual,
    listMyOrders: vi.fn(),
  };
});

const mockedListMyOrders = vi.mocked(listMyOrders);

describe('MyOrdersPage', () => {
  beforeEach(() => {
    mockedListMyOrders.mockReset();
  });

  it('shows a loading state before orders resolve', () => {
    mockedListMyOrders.mockReturnValue(new Promise(() => {}));

    renderWithQueryClient(<MyOrdersPage />);

    expect(screen.getByRole('heading', { name: 'My orders' })).toHaveFocus();
    expect(screen.getByText('Loading orders...')).toBeVisible();
    expect(mockedListMyOrders).toHaveBeenCalledWith({
      page: 0,
      size: 20,
      sort: 'eventDate,asc',
    });
  });

  it('shows an empty state when there are no upcoming orders', async () => {
    mockedListMyOrders.mockResolvedValue({ items: [], page: pageMetadata(20, 0) });

    renderWithQueryClient(<MyOrdersPage />);

    expect(await screen.findByText('No upcoming orders yet.')).toBeVisible();
  });

  it('shows an error message when the request fails', async () => {
    mockedListMyOrders.mockRejectedValue(new Error('network down'));

    renderWithQueryClient(<MyOrdersPage />);

    expect(await screen.findByText('Orders are unavailable. Try again in a moment.')).toBeVisible();
  });

  it('renders owned orders', async () => {
    mockedListMyOrders.mockResolvedValue({
      items: [myEventOrder],
      page: pageMetadata(20, 1),
    });

    renderWithQueryClient(<MyOrdersPage />);

    expect(await screen.findByText('The Horizon Live')).toBeVisible();
    expect(screen.getByText('Riverside Arena')).toBeVisible();
    expect(screen.getByText('Row 1, place 2')).toBeVisible();
    expect(screen.getByText('STANDARD')).toBeVisible();
  });

  it('refetches orders when Refresh is clicked', async () => {
    mockedListMyOrders.mockResolvedValue({
      items: [myEventOrder],
      page: pageMetadata(20, 1),
    });
    const user = userEvent.setup();

    renderWithQueryClient(<MyOrdersPage />);

    await screen.findByText('The Horizon Live');
    await user.click(screen.getByRole('button', { name: 'Refresh' }));

    await waitFor(() => {
      expect(mockedListMyOrders).toHaveBeenCalledTimes(2);
    });
  });
});
