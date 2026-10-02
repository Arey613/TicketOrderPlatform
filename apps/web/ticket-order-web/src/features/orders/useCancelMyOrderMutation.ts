import { useMutation, useQueryClient } from '@tanstack/react-query';
import { cancelMyOrder } from '../../api/ordersClient';

export function useCancelMyOrderMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: cancelMyOrder,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['orders', 'mine'] }),
  });
}
