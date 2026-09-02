import { z } from 'zod';

export const eventPlaceTypeSchema = z.object({
  name: z.string().min(1, 'Enter a place type name.'),
  price: z.string().min(1, 'Enter a price.'),
  currency: z.string().min(1, 'Enter a currency.'),
});

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
  imageUrl: z.string().optional(),
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
