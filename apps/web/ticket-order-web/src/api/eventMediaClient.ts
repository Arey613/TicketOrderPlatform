import { ALLOWED_VIDEO_TYPES } from '../features/events/mediaLimits';
import type { EventResponse, IssueVideoUploadUrlResponse } from '../generated/api';
import {
  Configuration,
  EventsApi,
  type IssueVideoUploadUrlRequestContentTypeEnum,
} from '../generated/api';
import { resolveApiBaseUrl, sessionAwareMiddleware } from './apiConfiguration';
import { prepareCsrfToken, withCsrfHeader } from './authClient';

const eventsApi = new EventsApi(
  new Configuration({
    basePath: resolveApiBaseUrl(),
    credentials: 'include',
    middleware: [sessionAwareMiddleware],
  }),
);

function toVideoContentType(type: string): IssueVideoUploadUrlRequestContentTypeEnum {
  if (ALLOWED_VIDEO_TYPES.includes(type)) {
    return type as IssueVideoUploadUrlRequestContentTypeEnum;
  }

  throw new Error(`Unsupported video content type: ${type}`);
}

export async function attachEventImage(eventId: string, image: File): Promise<EventResponse> {
  await prepareCsrfToken();

  return eventsApi.attachEventImage({ eventId, image }, withCsrfHeader);
}

export async function issueEventVideoUploadUrl(
  eventId: string,
  video: File,
): Promise<IssueVideoUploadUrlResponse> {
  await prepareCsrfToken();

  return eventsApi.issueEventVideoUploadUrl(
    {
      eventId,
      issueVideoUploadUrlRequest: {
        fileName: video.name,
        contentType: toVideoContentType(video.type),
        fileSizeBytes: video.size,
      },
    },
    withCsrfHeader,
  );
}

export async function confirmEventVideoUpload(
  eventId: string,
  videoUrl: string,
): Promise<EventResponse> {
  await prepareCsrfToken();

  return eventsApi.confirmEventVideoUpload(
    { eventId, confirmVideoUploadRequest: { videoUrl } },
    withCsrfHeader,
  );
}

/**
 * Uploads the video bytes directly to object storage using a presigned URL.
 *
 * This intentionally bypasses the generated API client and the CSRF pattern used for
 * session-based calls: the presigned URL points at a different origin (S3/LocalStack),
 * carries its own signature-based authorization in `requiredHeaders`, and must not receive
 * cookies or CSRF headers meant for the ticket-order API.
 */
export async function uploadEventVideo(
  uploadUrl: string,
  video: File,
  requiredHeaders: Record<string, string>,
): Promise<void> {
  const response = await fetch(uploadUrl, {
    method: 'PUT',
    body: video,
    headers: requiredHeaders,
  });

  if (!response.ok) {
    throw new Error(`Video upload failed with status ${response.status}.`);
  }
}
