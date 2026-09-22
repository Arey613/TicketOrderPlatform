import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { listMyOrders } from '../../../../src/api/ordersClient';
import { MyOrdersPage } from '../../../../src/features/orders/MyOrdersPage';
import type { MyOrdersResponse, PageMetadata } from '../../../../src/generated/api';
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

  it('requests the next and previous pages from pagination controls', async () => {
    mockedListMyOrders.mockImplementation(({ page, size }) =>
      Promise.resolve({
        items: [myEventOrder],
        page: pageDetails({ number: page, size, totalElements: 2, totalPages: 2 }),
      }),
    );
    const user = userEvent.setup();

    renderWithQueryClient(<MyOrdersPage />);

    await screen.findByText('Page 1 of 2');
    await user.click(screen.getByRole('button', { name: 'Next' }));

    await screen.findByText('Page 2 of 2');
    await waitFor(() => {
      expect(mockedListMyOrders).toHaveBeenCalledWith({
        page: 1,
        size: 20,
        sort: 'eventDate,asc',
      });
    });

    await user.click(screen.getByRole('button', { name: 'Previous' }));

    await screen.findByText('Page 1 of 2');
    await waitFor(() => {
      expect(mockedListMyOrders).toHaveBeenCalledWith({
        page: 0,
        size: 20,
        sort: 'eventDate,asc',
      });
    });
  });

  it('resets to the first page when page size changes', async () => {
    mockedListMyOrders.mockImplementation(({ page, size }) =>
      Promise.resolve({
        items: [myEventOrder],
        page: pageDetails({ number: page, size, totalElements: 3, totalPages: 3 }),
      }),
    );
    const user = userEvent.setup();

    renderWithQueryClient(<MyOrdersPage />);

    await screen.findByText('Page 1 of 3');
    await user.click(screen.getByRole('button', { name: 'Next' }));
    await screen.findByText('Page 2 of 3');
    await user.selectOptions(screen.getByLabelText('Orders per page'), '50');

    await waitFor(() => {
      expect(mockedListMyOrders).toHaveBeenCalledWith({
        page: 0,
        size: 50,
        sort: 'eventDate,asc',
      });
    });
  });

  it('disables pagination buttons for first and last page states', async () => {
    mockedListMyOrders.mockResolvedValueOnce(pageResponse({ number: 0, last: false }));
    const user = userEvent.setup();

    renderWithQueryClient(<MyOrdersPage />);

    expect(await screen.findByRole('button', { name: 'Previous' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Next' })).toBeEnabled();

    mockedListMyOrders.mockResolvedValueOnce(pageResponse({ number: 1, first: false }));
    await user.click(screen.getByRole('button', { name: 'Next' }));

    expect(await screen.findByRole('button', { name: 'Next' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Previous' })).toBeEnabled();
  });
});

function pageResponse(overrides: Partial<PageMetadata>): MyOrdersResponse {
  return {
    items: [myEventOrder],
    page: pageDetails(overrides),
  };
}

function pageDetails(overrides: Partial<PageMetadata>): PageMetadata {
  const number = overrides.number ?? 0;
  const totalPages = overrides.totalPages ?? 2;

  return {
    number,
    size: overrides.size ?? 20,
    totalElements: overrides.totalElements ?? totalPages,
    totalPages,
    first: overrides.first ?? number === 0,
    last: overrides.last ?? number >= totalPages - 1,
  };
}
