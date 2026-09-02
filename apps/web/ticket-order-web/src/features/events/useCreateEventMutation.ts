import { useMutation } from '@tanstack/react-query';
import { createEvent } from '../../api/eventsClient';
import type { CreateEventFormValues } from './createEventSchema';

export function useCreateEventMutation() {
  return useMutation({
    mutationFn: (values: CreateEventFormValues) => createEvent(values),
  });
}
