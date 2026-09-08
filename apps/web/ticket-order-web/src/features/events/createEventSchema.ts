import { z } from 'zod';
import { ALLOWED_IMAGE_TYPES, ALLOWED_VIDEO_TYPES, MAX_IMAGE_SIZE_BYTES } from './mediaLimits';

const MAX_VIDEO_DURATION_SECONDS = 180;
const VIDEO_METADATA_TIMEOUT_MS = 10_000;

function extractFile(fileList: FileList | undefined): File | undefined {
  return fileList && fileList.length > 0 ? fileList[0] : undefined;
}

/**
 * Probes a video file's duration by loading it into a detached (never appended to the DOM)
 * `<video>` element and reading `.duration` once metadata is available. This is UI-only
 * validation - the backend does not check video duration.
 */
export function getVideoDurationSeconds(file: File): Promise<number> {
  return new Promise((resolve, reject) => {
    const objectUrl = URL.createObjectURL(file);
    const videoElement = document.createElement('video');
    videoElement.preload = 'metadata';

    const cleanup = () => {
      clearTimeout(timeoutId);
      URL.revokeObjectURL(objectUrl);
    };

    const timeoutId = setTimeout(() => {
      cleanup();
      reject(new Error('Timed out reading video metadata.'));
    }, VIDEO_METADATA_TIMEOUT_MS);

    videoElement.addEventListener('loadedmetadata', () => {
      const duration = videoElement.duration;
      cleanup();
      resolve(duration);
    });

    videoElement.addEventListener('error', () => {
      cleanup();
      reject(new Error('Unable to read video metadata.'));
    });

    videoElement.src = objectUrl;
  });
}

export const eventPlaceTypeSchema = z.object({
  name: z.string().min(1, 'Enter a place type name.'),
  price: z.string().min(1, 'Enter a price.'),
  currency: z.string().min(1, 'Enter a currency.'),
});

const eventImageSchema = z
  .instanceof(FileList)
  .optional()
  .transform(extractFile)
  .refine((file) => !file || ALLOWED_IMAGE_TYPES.includes(file.type), {
    message: 'Use a JPEG, PNG, or WebP image.',
  })
  .refine((file) => !file || file.size <= MAX_IMAGE_SIZE_BYTES, {
    message: 'Image must be 5MB or smaller.',
  });

const eventVideoSchema = z
  .instanceof(FileList)
  .optional()
  .transform(extractFile)
  .refine((file) => !file || ALLOWED_VIDEO_TYPES.includes(file.type), {
    message: 'Use an MP4, WebM, or QuickTime video.',
  })
  .refine(
    async (file) => {
      if (!file || !ALLOWED_VIDEO_TYPES.includes(file.type)) {
        return true;
      }

      try {
        const durationSeconds = await getVideoDurationSeconds(file);
        return durationSeconds <= MAX_VIDEO_DURATION_SECONDS;
      } catch {
        return false;
      }
    },
    { message: 'Video must be 3 minutes or shorter.' },
  );

export const createEventSchema = z.object({
  name: z.string().min(1, 'Enter an event name.'),
  date: z
    .string()
    .min(1, 'Enter a date.')
    .refine((value) => !Number.isNaN(Date.parse(value)), 'Enter a valid date.'),
  place: z.string().min(1, 'Enter a place.'),
  city: z.string().optional(),
  type: z.string().min(1, 'Enter an event type.'),
  summary: z.string().optional(),
  image: eventImageSchema,
  video: eventVideoSchema,
  price: z.string().optional(),
  currency: z.string().optional(),
  details: z.object({
    description: z.string().min(1, 'Enter a description.'),
    numberOfPlaces: z.coerce.number().int().positive('Enter a positive number of places.'),
    numberOfRows: z.coerce.number().int().positive('Enter a positive number of rows.'),
    seatsPerRow: z.coerce.number().int().positive('Enter a positive number of seats per row.'),
    placeTypes: z.array(eventPlaceTypeSchema).optional(),
  }),
});

export type CreateEventFormValues = z.infer<typeof createEventSchema>;
