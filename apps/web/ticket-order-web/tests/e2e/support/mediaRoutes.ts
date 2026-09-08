import { expect, type Page } from '@playwright/test';

const uploadUrl =
  'https://storage.example.com/events/00000000-0000-0000-0000-000000000900/video/clip.mp4';

type ImageRouteOptions = {
  eventId?: string;
  fail?: boolean;
};

type VideoRouteOptions = {
  eventId?: string;
  failUpload?: boolean;
  failConfirm?: boolean;
};

function baseEventResponse(eventId: string) {
  return {
    eventId,
    ownerId: '00000000-0000-0000-0000-000000000601',
    name: 'Summer music night',
    date: '2026-09-10T19:00:00Z',
    place: 'Central Hall',
    type: 'CONCERT',
    status: 'DRAFT',
    details: {
      description: 'Outdoor concert with reserved seating',
      numberOfPlaces: 120,
      numberOfRows: 12,
      seatsPerRow: 10,
    },
    ordersTaken: 0,
    takenPlaces: [],
  };
}

export async function mockAttachEventImage(
  page: Page,
  options: ImageRouteOptions = {},
): Promise<void> {
  const eventId = options.eventId ?? '00000000-0000-0000-0000-000000000900';

  await page.route(`**/events/${eventId}/image`, async (route) => {
    if (route.request().method() !== 'POST') {
      await route.fallback();
      return;
    }

    await expect(route.request().headers()['x-xsrf-token']).toBe('e2e-token');

    if (options.fail) {
      await route.fulfill({ status: 500 });
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        ...baseEventResponse(eventId),
        imageUrl: `https://cdn.example.com/events/${eventId}/image/cover.png`,
      }),
    });
  });
}

export async function mockVideoUpload(page: Page, options: VideoRouteOptions = {}): Promise<void> {
  const eventId = options.eventId ?? '00000000-0000-0000-0000-000000000900';

  await page.route(`**/events/${eventId}/video-upload-url`, async (route) => {
    if (route.request().method() !== 'POST') {
      await route.fallback();
      return;
    }

    await expect(route.request().headers()['x-xsrf-token']).toBe('e2e-token');
    const body = route.request().postDataJSON();
    await expect(body).toEqual(
      expect.objectContaining({ fileName: expect.any(String), contentType: 'video/mp4' }),
    );

    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        event: baseEventResponse(eventId),
        videoUrl: `https://cdn.example.com/events/${eventId}/video/clip.mp4`,
        uploadUrl,
        requiredHeaders: { 'x-amz-signature': 'e2e-signature' },
        expiresAt: '2026-09-10T20:00:00Z',
      }),
    });
  });

  await page.route(uploadUrl, async (route) => {
    if (route.request().method() !== 'PUT') {
      await route.fallback();
      return;
    }

    if (options.failUpload) {
      await route.fulfill({ status: 500 });
      return;
    }

    await route.fulfill({ status: 200 });
  });

  await page.route(`**/events/${eventId}/video-upload-confirmation`, async (route) => {
    if (route.request().method() !== 'POST') {
      await route.fallback();
      return;
    }

    await expect(route.request().headers()['x-xsrf-token']).toBe('e2e-token');

    if (options.failConfirm) {
      await route.fulfill({ status: 500 });
      return;
    }

    const body = route.request().postDataJSON();
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ ...baseEventResponse(eventId), videoUrl: body.videoUrl }),
    });
  });
}

/**
 * Stubs `<video>`'s `src` setter in the real browser so setting `src` synchronously reports the
 * given duration through a `loadedmetadata` event, mirroring the Vitest unit test's approach -
 * avoids needing a real decodable video fixture file just to satisfy the client-side duration
 * probe in createEventSchema.
 */
export async function mockVideoDuration(page: Page, durationSeconds: number): Promise<void> {
  await page.addInitScript((duration) => {
    Object.defineProperty(window.HTMLMediaElement.prototype, 'src', {
      configurable: true,
      get() {
        return '';
      },
      set(this: HTMLMediaElement) {
        Object.defineProperty(this, 'duration', { configurable: true, value: duration });
        this.dispatchEvent(new Event('loadedmetadata'));
      },
    });
  }, durationSeconds);
}
