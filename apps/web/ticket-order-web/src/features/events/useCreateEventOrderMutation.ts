import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { SeatSelection } from '../../api/eventsClient';
import { createEventOrders } from '../../api/eventsClient';

export function useCreateEventOrderMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (selection: SeatSelection) => createEventOrders(selection),
    onSuccess: (_data, selection) => {
      void queryClient.invalidateQueries({ queryKey: ['events', 'detail', selection.eventId] });
      void queryClient.invalidateQueries({ queryKey: ['orders', 'mine'] });
    },
  });
}
