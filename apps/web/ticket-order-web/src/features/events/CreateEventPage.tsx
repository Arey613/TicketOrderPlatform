import { zodResolver } from '@hookform/resolvers/zod';
import { Plus, Trash2 } from 'lucide-react';
import { useEffect, useRef } from 'react';
import { useFieldArray, useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router';
import type { z } from 'zod';
import { toEventUserMessage } from '../../api/eventsClient';
import type { CreateEventFormValues } from './createEventSchema';
import { createEventSchema } from './createEventSchema';
import { useCreateEventMutation } from './useCreateEventMutation';

type CreateEventFormInput = z.input<typeof createEventSchema>;

const defaultValues: CreateEventFormInput = {
  name: '',
  date: '',
  place: '',
  city: '',
  type: '',
  summary: '',
  imageUrl: '',
  price: '',
  currency: '',
  details: {
    description: '',
    numberOfPlaces: '' as unknown as number,
    numberOfRows: '' as unknown as number,
    seatsPerRow: '' as unknown as number,
    placeTypes: [],
  },
};

const inputClassName =
  'mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 outline-none focus:border-teal-700 focus:ring-2 focus:ring-teal-700/20';
const labelClassName = 'block text-sm font-semibold text-slate-900';

export function CreateEventPage() {
  const navigate = useNavigate();
  const mutation = useCreateEventMutation();
  const headingRef = useRef<HTMLHeadingElement>(null);

  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<CreateEventFormInput, unknown, CreateEventFormValues>({
    resolver: zodResolver(createEventSchema),
    defaultValues,
  });

  const { fields, append, remove } = useFieldArray({ control, name: 'details.placeTypes' });

  useEffect(() => {
    headingRef.current?.focus();
  }, []);

  const onSubmit = handleSubmit((values) => {
    mutation.mutate(values, {
      onSuccess: () => {
        navigate('/', { state: { eventCreated: true } });
      },
    });
  });

  const statusMessage = mutation.isPending
    ? 'Creating event...'
    : mutation.isError
      ? toEventUserMessage(mutation.error)
      : '';

  return (
    <section className="mx-auto w-full max-w-3xl px-4 py-10 sm:px-6 lg:px-8">
      <h1
        className="text-3xl font-black text-slate-950 outline-none"
        ref={headingRef}
        tabIndex={-1}
      >
        Create event
      </h1>
      <p className="mt-2 text-sm leading-6 text-slate-600">
        New events start as a draft. Publish them once the layout details are confirmed.
      </p>

      <form className="mt-8 space-y-8" onSubmit={onSubmit}>
        <fieldset className="space-y-5 border-0 p-0">
          <legend className="text-xs font-bold uppercase tracking-normal text-slate-500">
            Event details
          </legend>

          <div>
            <label className={labelClassName} htmlFor="name">
              Name
            </label>
            <input
              aria-describedby={errors.name ? 'name-error' : undefined}
              aria-invalid={Boolean(errors.name)}
              className={inputClassName}
              id="name"
              placeholder="Summer music night"
              {...register('name')}
            />
            <FieldError id="name-error" message={errors.name?.message} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClassName} htmlFor="date">
                Date
              </label>
              <input
                aria-describedby={errors.date ? 'date-error' : undefined}
                aria-invalid={Boolean(errors.date)}
                className={inputClassName}
                id="date"
                type="datetime-local"
                {...register('date')}
              />
              <FieldError id="date-error" message={errors.date?.message} />
            </div>

            <div>
              <label className={labelClassName} htmlFor="type">
                Type
              </label>
              <input
                aria-describedby={errors.type ? 'type-error' : undefined}
                aria-invalid={Boolean(errors.type)}
                className={inputClassName}
                id="type"
                placeholder="Concert"
                {...register('type')}
              />
              <FieldError id="type-error" message={errors.type?.message} />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClassName} htmlFor="place">
                Place
              </label>
              <input
                aria-describedby={errors.place ? 'place-error' : undefined}
                aria-invalid={Boolean(errors.place)}
                className={inputClassName}
                id="place"
                placeholder="Central Hall"
                {...register('place')}
              />
              <FieldError id="place-error" message={errors.place?.message} />
            </div>

            <div>
              <label className={labelClassName} htmlFor="city">
                City (optional)
              </label>
              <input
                className={inputClassName}
                id="city"
                placeholder="Springfield"
                {...register('city')}
              />
            </div>
          </div>

          <div>
            <label className={labelClassName} htmlFor="summary">
              Summary (optional)
            </label>
            <input className={inputClassName} id="summary" {...register('summary')} />
          </div>

          <div>
            <label className={labelClassName} htmlFor="imageUrl">
              Image URL (optional)
            </label>
            <input className={inputClassName} id="imageUrl" {...register('imageUrl')} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClassName} htmlFor="price">
                Price (optional)
              </label>
              <input className={inputClassName} id="price" {...register('price')} />
            </div>

            <div>
              <label className={labelClassName} htmlFor="currency">
                Currency (optional)
              </label>
              <input className={inputClassName} id="currency" {...register('currency')} />
            </div>
          </div>

          <div>
            <label className={labelClassName} htmlFor="description">
              Description
            </label>
            <textarea
              aria-describedby={errors.details?.description ? 'description-error' : undefined}
              aria-invalid={Boolean(errors.details?.description)}
              className={inputClassName}
              id="description"
              rows={3}
              {...register('details.description')}
            />
            <FieldError id="description-error" message={errors.details?.description?.message} />
          </div>
        </fieldset>

        <fieldset className="space-y-5 border-0 p-0">
          <legend className="text-xs font-bold uppercase tracking-normal text-slate-500">
            Capacity and layout
          </legend>

          <div className="grid gap-4 sm:grid-cols-3">
            <div>
              <label className={labelClassName} htmlFor="numberOfPlaces">
                Places
              </label>
              <input
                aria-describedby={
                  errors.details?.numberOfPlaces ? 'numberOfPlaces-error' : undefined
                }
                aria-invalid={Boolean(errors.details?.numberOfPlaces)}
                className={inputClassName}
                id="numberOfPlaces"
                type="number"
                {...register('details.numberOfPlaces')}
              />
              <FieldError
                id="numberOfPlaces-error"
                message={errors.details?.numberOfPlaces?.message}
              />
            </div>

            <div>
              <label className={labelClassName} htmlFor="numberOfRows">
                Rows
              </label>
              <input
                aria-describedby={errors.details?.numberOfRows ? 'numberOfRows-error' : undefined}
                aria-invalid={Boolean(errors.details?.numberOfRows)}
                className={inputClassName}
                id="numberOfRows"
                type="number"
                {...register('details.numberOfRows')}
              />
              <FieldError id="numberOfRows-error" message={errors.details?.numberOfRows?.message} />
            </div>

            <div>
              <label className={labelClassName} htmlFor="seatsPerRow">
                Per row
              </label>
              <input
                aria-describedby={errors.details?.seatsPerRow ? 'seatsPerRow-error' : undefined}
                aria-invalid={Boolean(errors.details?.seatsPerRow)}
                className={inputClassName}
                id="seatsPerRow"
                type="number"
                {...register('details.seatsPerRow')}
              />
              <FieldError id="seatsPerRow-error" message={errors.details?.seatsPerRow?.message} />
            </div>
          </div>
        </fieldset>

        <fieldset className="space-y-4 border-0 p-0">
          <div className="flex items-center justify-between">
            <legend className="text-xs font-bold uppercase tracking-normal text-slate-500">
              Place types (optional)
            </legend>
            <button
              className="flex items-center gap-1 text-sm font-semibold text-teal-800 hover:text-teal-900"
              onClick={() => append({ name: '', price: '', currency: '' })}
              type="button"
            >
              <Plus className="h-4 w-4" aria-hidden="true" />
              Add place type
            </button>
          </div>

          {fields.map((field, index) => (
            <div className="flex items-center gap-3" key={field.id}>
              <input
                aria-label={`Place type ${index + 1} name`}
                className={`${inputClassName} mt-0 flex-[1.2]`}
                placeholder="VIP"
                {...register(`details.placeTypes.${index}.name` as const)}
              />
              <input
                aria-label={`Place type ${index + 1} price`}
                className={`${inputClassName} mt-0 flex-1`}
                placeholder="45.00"
                {...register(`details.placeTypes.${index}.price` as const)}
              />
              <input
                aria-label={`Place type ${index + 1} currency`}
                className={`${inputClassName} mt-0 flex-[0.8]`}
                placeholder="USD"
                {...register(`details.placeTypes.${index}.currency` as const)}
              />
              <button
                aria-label={`Remove place type ${index + 1}`}
                className="shrink-0 rounded-md border border-slate-300 p-2 text-slate-500 transition hover:border-red-700 hover:text-red-700"
                onClick={() => remove(index)}
                type="button"
              >
                <Trash2 className="h-4 w-4" aria-hidden="true" />
              </button>
            </div>
          ))}
        </fieldset>

        <p
          aria-live="polite"
          className={
            statusMessage
              ? mutation.isError
                ? 'rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm font-medium text-red-800'
                : 'rounded-md border border-teal-200 bg-teal-50 px-3 py-2 text-sm font-medium text-teal-900'
              : 'sr-only'
          }
        >
          {statusMessage}
        </p>

        <div className="flex gap-3">
          <button
            className="flex-1 rounded-md bg-teal-700 px-5 py-3 text-sm font-bold text-white transition hover:bg-teal-800 disabled:cursor-not-allowed disabled:bg-slate-400"
            disabled={mutation.isPending}
            type="submit"
          >
            Create event
          </button>
          <Link
            className="rounded-md border border-slate-300 bg-white px-5 py-3 text-sm font-bold text-slate-900 transition hover:border-teal-700 hover:text-teal-800"
            to="/"
          >
            Cancel
          </Link>
        </div>
      </form>
    </section>
  );
}

function FieldError({ id, message }: { id: string; message?: string }) {
  if (!message) {
    return null;
  }

  return (
    <p className="mt-1 text-xs font-medium text-red-700" id={id}>
      {message}
    </p>
  );
}
