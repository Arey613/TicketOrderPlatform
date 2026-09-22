import { IssueVideoUploadUrlRequestContentTypeEnum } from '../../generated/api';

export const MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;
export const MAX_VIDEO_SIZE_BYTES = 100 * 1024 * 1024;
export const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

export const ALLOWED_VIDEO_TYPES: readonly string[] = Object.values(
  IssueVideoUploadUrlRequestContentTypeEnum,
);
