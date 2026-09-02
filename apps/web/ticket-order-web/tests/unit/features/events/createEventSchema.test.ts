import { describe, expect, it } from 'vitest';
import { createEventSchema } from '../../../../src/features/events/createEventSchema';

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
  it('accepts a valid minimal payload', () => {
    const result = createEventSchema.safeParse(validPayload);

    expect(result.success).toBe(true);
  });

  it('accepts optional fields and place types when provided', () => {
    const result = createEventSchema.safeParse({
      ...validPayload,
      city: 'Springfield',
      summary: 'A great night',
      imageUrl: 'https://example.com/image.png',
      price: '25.00',
      currency: 'USD',
      details: {
        ...validPayload.details,
        placeTypes: [{ name: 'VIP', price: '45.00', currency: 'USD' }],
      },
    });

    expect(result.success).toBe(true);
  });

  it.each(['name', 'date', 'place', 'type'])('rejects a missing required field: %s', (field) => {
    const payload = { ...validPayload };
    delete (payload as Record<string, unknown>)[field];

    const result = createEventSchema.safeParse(payload);

    expect(result.success).toBe(false);
  });

  it('rejects a missing details.description', () => {
    const result = createEventSchema.safeParse({
      ...validPayload,
      details: { ...validPayload.details, description: '' },
    });

    expect(result.success).toBe(false);
  });

  it.each(['numberOfPlaces', 'numberOfRows', 'seatsPerRow'])(
    'rejects a non-positive %s',
    (field) => {
      const result = createEventSchema.safeParse({
        ...validPayload,
        details: { ...validPayload.details, [field]: 0 },
      });

      expect(result.success).toBe(false);
    },
  );

  it('rejects an invalid date string', () => {
    const result = createEventSchema.safeParse({ ...validPayload, date: 'not-a-date' });

    expect(result.success).toBe(false);
  });
});
