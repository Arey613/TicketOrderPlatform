import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { createEventSchema } from '../../../../src/features/events/createEventSchema';

async function toFileList(files: File[]): Promise<FileList> {
  const input = document.createElement('input');
  input.type = 'file';
  input.multiple = files.length > 1;
  const user = userEvent.setup();
  await user.upload(input, files);
  return input.files as FileList;
}

function createFile(name: string, type: string, sizeBytes: number): File {
  return new File([new Uint8Array(sizeBytes)], name, { type });
}

/**
 * Stubs `URL.createObjectURL`/`revokeObjectURL` (unimplemented in jsdom) and the
 * `<video>` element's `src` setter so setting `src` synchronously reports the given
 * duration through a `loadedmetadata` event, mirroring what `getVideoDurationSeconds`
 * listens for. Returns a restore function.
 */
function mockVideoDuration(durationSeconds: number): () => void {
  const originalCreateObjectURL = URL.createObjectURL;
  const originalRevokeObjectURL = URL.revokeObjectURL;
  URL.createObjectURL = vi.fn(() => 'blob:mock-video');
  URL.revokeObjectURL = vi.fn();

  const originalSrcDescriptor = Object.getOwnPropertyDescriptor(
    window.HTMLMediaElement.prototype,
    'src',
  );
  Object.defineProperty(window.HTMLMediaElement.prototype, 'src', {
    configurable: true,
    get() {
      return '';
    },
    set(this: HTMLMediaElement) {
      Object.defineProperty(this, 'duration', { configurable: true, value: durationSeconds });
      this.dispatchEvent(new Event('loadedmetadata'));
    },
  });

  return () => {
    URL.createObjectURL = originalCreateObjectURL;
    URL.revokeObjectURL = originalRevokeObjectURL;
    if (originalSrcDescriptor) {
      Object.defineProperty(window.HTMLMediaElement.prototype, 'src', originalSrcDescriptor);
    }
  };
}

/** Stubs the `<video>` element to fire an `error` event instead of `loadedmetadata`. */
function mockVideoMetadataError(): () => void {
  const originalCreateObjectURL = URL.createObjectURL;
  const originalRevokeObjectURL = URL.revokeObjectURL;
  URL.createObjectURL = vi.fn(() => 'blob:mock-video');
  URL.revokeObjectURL = vi.fn();

  const originalSrcDescriptor = Object.getOwnPropertyDescriptor(
    window.HTMLMediaElement.prototype,
    'src',
  );
  Object.defineProperty(window.HTMLMediaElement.prototype, 'src', {
    configurable: true,
    get() {
      return '';
    },
    set(this: HTMLMediaElement) {
      this.dispatchEvent(new Event('error'));
    },
  });

  return () => {
    URL.createObjectURL = originalCreateObjectURL;
    URL.revokeObjectURL = originalRevokeObjectURL;
    if (originalSrcDescriptor) {
      Object.defineProperty(window.HTMLMediaElement.prototype, 'src', originalSrcDescriptor);
    }
  };
}

const validPayload = {
  name: 'Summer music night',
  date: '2026-09-10T19:00',
  place: 'Central Hall',
  type: 'CONCERT',
  details: {
    description: 'Outdoor concert with reserved seating',
    numberOfPlaces: 120,
    numberOfRows: 12,
    seatsPerRow: 10,
  },
};

describe('createEventSchema', () => {
  it('accepts a valid minimal payload', async () => {
    const result = await createEventSchema.safeParseAsync(validPayload);

    expect(result.success).toBe(true);
  });

  it('accepts optional fields and place types when provided', async () => {
    const result = await createEventSchema.safeParseAsync({
      ...validPayload,
      city: 'Springfield',
      summary: 'A great night',
      price: '25.00',
      currency: 'USD',
      details: {
        ...validPayload.details,
        placeTypes: [{ name: 'VIP', price: '45.00', currency: 'USD' }],
      },
    });

    expect(result.success).toBe(true);
  });

  it.each(['name', 'date', 'place', 'type'])(
    'rejects a missing required field: %s',
    async (field) => {
      const payload = { ...validPayload };
      delete (payload as Record<string, unknown>)[field];

      const result = await createEventSchema.safeParseAsync(payload);

      expect(result.success).toBe(false);
    },
  );

  it('rejects a missing details.description', async () => {
    const result = await createEventSchema.safeParseAsync({
      ...validPayload,
      details: { ...validPayload.details, description: '' },
    });

    expect(result.success).toBe(false);
  });

  it.each(['numberOfPlaces', 'numberOfRows', 'seatsPerRow'])(
    'rejects a non-positive %s',
    async (field) => {
      const result = await createEventSchema.safeParseAsync({
        ...validPayload,
        details: { ...validPayload.details, [field]: 0 },
      });

      expect(result.success).toBe(false);
    },
  );

  it('rejects an invalid date string', async () => {
    const result = await createEventSchema.safeParseAsync({ ...validPayload, date: 'not-a-date' });

    expect(result.success).toBe(false);
  });

  describe('image', () => {
    it('accepts a valid JPEG within the size limit', async () => {
      const image = await toFileList([createFile('cover.jpg', 'image/jpeg', 1024)]);

      const result = await createEventSchema.safeParseAsync({ ...validPayload, image });

      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.image?.name).toBe('cover.jpg');
      }
    });

    it('rejects an unsupported image type', async () => {
      const image = await toFileList([createFile('cover.gif', 'image/gif', 1024)]);

      const result = await createEventSchema.safeParseAsync({ ...validPayload, image });

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0]?.message).toBe('Use a JPEG, PNG, or WebP image.');
      }
    });

    it('rejects an image over 5MB', async () => {
      const image = await toFileList([createFile('cover.png', 'image/png', 5 * 1024 * 1024 + 1)]);

      const result = await createEventSchema.safeParseAsync({ ...validPayload, image });

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0]?.message).toBe('Image must be 5MB or smaller.');
      }
    });
  });

  describe('video', () => {
    afterEach(() => {
      vi.restoreAllMocks();
    });

    it('rejects an unsupported video type', async () => {
      const video = await toFileList([createFile('trailer.mov', 'video/avi', 1024)]);

      const result = await createEventSchema.safeParseAsync({ ...validPayload, video });

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0]?.message).toBe('Use an MP4, WebM, or QuickTime video.');
      }
    });

    it('accepts a video within the 3-minute limit', async () => {
      const restore = mockVideoDuration(150);
      try {
        const video = await toFileList([createFile('trailer.mp4', 'video/mp4', 1024)]);

        const result = await createEventSchema.safeParseAsync({ ...validPayload, video });

        expect(result.success).toBe(true);
      } finally {
        restore();
      }
    });

    it('rejects a video longer than 3 minutes', async () => {
      const restore = mockVideoDuration(200);
      try {
        const video = await toFileList([createFile('trailer.mp4', 'video/mp4', 1024)]);

        const result = await createEventSchema.safeParseAsync({ ...validPayload, video });

        expect(result.success).toBe(false);
        if (!result.success) {
          expect(result.error.issues[0]?.message).toBe('Video must be 3 minutes or shorter.');
        }
      } finally {
        restore();
      }
    });

    it('rejects a video whose duration cannot be read', async () => {
      const restore = mockVideoMetadataError();
      try {
        const video = await toFileList([createFile('trailer.mp4', 'video/mp4', 1024)]);

        const result = await createEventSchema.safeParseAsync({ ...validPayload, video });

        expect(result.success).toBe(false);
      } finally {
        restore();
      }
    });
  });
});
