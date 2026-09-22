import { useMutation, useQueryClient } from '@tanstack/react-query';
import { patchEvent, publishEvent, unpublishEvent } from '../../api/eventsClient';
import type { CreateEventFormValues } from './createEventSchema';

function useRefreshEventQueries() {
  const queryClient = useQueryClient();

  return (eventId: string) => {
    void queryClient.invalidateQueries({ queryKey: ['events', 'mine'] });
    void queryClient.invalidateQueries({ queryKey: ['events', 'published'] });
    void queryClient.invalidateQueries({ queryKey: ['events', 'detail', eventId] });
  };
}

export function usePatchEventMutation(eventId: string) {
  const refresh = useRefreshEventQueries();

  return useMutation({
    mutationFn: (values: CreateEventFormValues) => patchEvent(eventId, values),
    onSuccess: () => refresh(eventId),
  });
}

export function usePublishEventMutation() {
  const refresh = useRefreshEventQueries();

  return useMutation({
    mutationFn: publishEvent,
    onSuccess: (event) => refresh(event.eventId),
  });
}

export function useUnpublishEventMutation() {
  const refresh = useRefreshEventQueries();

  return useMutation({
    mutationFn: unpublishEvent,
    onSuccess: (event) => refresh(event.eventId),
  });
}
