import { useMutation } from '@tanstack/react-query';
import {
  attachEventImage,
  issueEventVideoUploadUrl,
  uploadEventVideo,
} from '../../api/eventMediaClient';
import { createEvent } from '../../api/eventsClient';
import type { EventResponse } from '../../generated/api';
import type { CreateEventFormValues } from './createEventSchema';

export type CreateEventResult = {
  event: EventResponse;
  /**
   * Set only when the event itself was created successfully but attaching the image and/or
   * video afterward failed. Distinct from a mutation error, which means the event was not
   * created at all.
   */
  mediaWarning?: string;
};

export function useCreateEventMutation() {
  return useMutation({
    mutationFn: async (values: CreateEventFormValues): Promise<CreateEventResult> => {
      const event = await createEvent(values);

      const failedMedia: string[] = [];

      if (values.image) {
        try {
          await attachEventImage(event.eventId, values.image);
        } catch {
          failedMedia.push('image');
        }
      }

      if (values.video) {
        try {
          const { uploadUrl, requiredHeaders } = await issueEventVideoUploadUrl(
            event.eventId,
            values.video,
          );
          await uploadEventVideo(uploadUrl, values.video, requiredHeaders);
        } catch {
          failedMedia.push('video');
        }
      }

      if (failedMedia.length === 0) {
        return { event };
      }

      return {
        event,
        mediaWarning: `Event created, but the ${failedMedia.join(' and ')} could not be uploaded. Try attaching it again later.`,
      };
    },
  });
}
