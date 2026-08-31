import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { listMyEventOrders } from '../../../../src/api/eventsClient';
import { MyOrdersPanel } from '../../../../src/features/orders/MyOrdersPanel';
import { myEventOrder, pageMetadata } from '../../../support/eventTestData';
import { renderWithQueryClient } from '../../../support/renderWithQueryClient';

vi.mock('../../../../src/api/eventsClient', async () => {
  const actual = await vi.importActual<typeof import('../../../../src/api/eventsClient')>(
    '../../../../src/api/eventsClient',
  );

  return {
    ...actual,
    listMyEventOrders: vi.fn(),
  };
});

const mockedListMyEventOrders = vi.mocked(listMyEventOrders);

describe('MyOrdersPanel', () => {
  beforeEach(() => {
    mockedListMyEventOrders.mockReset();
  });

  it('shows a loading state before orders resolve', () => {
    mockedListMyEventOrders.mockReturnValue(new Promise(() => {}));

    renderWithQueryClient(<MyOrdersPanel />);

    expect(screen.getByText('Loading orders...')).toBeVisible();
  });

  it('shows an empty state when there are no orders', async () => {
    mockedListMyEventOrders.mockResolvedValue({ items: [], page: pageMetadata(20, 0) });

    renderWithQueryClient(<MyOrdersPanel />);

    expect(await screen.findByText('No orders yet.')).toBeVisible();
  });

  it('shows an error message when the request fails', async () => {
    mockedListMyEventOrders.mockRejectedValue(new Error('network down'));

    renderWithQueryClient(<MyOrdersPanel />);

    expect(await screen.findByText('Orders are unavailable. Try again in a moment.')).toBeVisible();
  });

  it('renders owned orders', async () => {
    mockedListMyEventOrders.mockResolvedValue({
      items: [myEventOrder],
      page: pageMetadata(20, 1),
    });

    renderWithQueryClient(<MyOrdersPanel />);

    expect(await screen.findByText('The Horizon Live')).toBeVisible();
    expect(screen.getByText('Row 1, place 2')).toBeVisible();
  });

  it('refetches orders when Refresh is clicked', async () => {
    mockedListMyEventOrders.mockResolvedValue({
      items: [myEventOrder],
      page: pageMetadata(20, 1),
    });
    const user = userEvent.setup();

    renderWithQueryClient(<MyOrdersPanel />);

    await screen.findByText('The Horizon Live');
    await user.click(screen.getByRole('button', { name: 'Refresh' }));

    await waitFor(() => {
      expect(mockedListMyEventOrders).toHaveBeenCalledTimes(2);
    });
  });
});
