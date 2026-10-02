import { useMutation } from '@tanstack/react-query';
import { cancelMyOrder } from '../../api/ordersClient';

export function useCancelMyOrderMutation() {
  return useMutation({
    mutationFn: cancelMyOrder,
  });
}
