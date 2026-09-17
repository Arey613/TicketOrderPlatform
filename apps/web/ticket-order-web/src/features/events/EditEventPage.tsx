import { zodResolver } from '@hookform/resolvers/zod';
import { useQueryClient } from '@tanstack/react-query';
import { Save } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useNavigate, useParams } from 'react-router';
import type { z } from 'zod';
import {
  attachEventImage,
  confirmEventVideoUpload,
  issueEventVideoUploadUrl,
  uploadEventVideo,
} from '../../api/eventMediaClient';
import { toEventUserMessage } from '../../api/eventsClient';
import { type EventResponse, EventStatus } from '../../generated/api';
import type { CreateEventFormValues } from './createEventSchema';
import { createEventSchema } from './createEventSchema';
import { useEventDetailsQuery } from './useEventDetailsQuery';
import { usePatchEventMutation } from './useEventManagementMutations';

type EventFormInput = z.input<typeof createEventSchema>;

const inputClassName =
  'mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 outline-none focus:border-teal-700 focus:ring-2 focus:ring-teal-700/20';
const fileInputClassName =
  'mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 outline-none file:mr-3 file:rounded file:border-0 file:bg-slate-100 file:px-3 file:py-1.5 file:text-sm file:font-semibold file:text-slate-700 focus:border-teal-700 focus:ring-2 focus:ring-teal-700/20';
const labelClassName = 'block text-sm font-semibold text-slate-900';
const hintClassName = 'mt-1 text-xs text-slate-500';

export function EditEventPage() {
  const { eventId } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const eventQuery = useEventDetailsQuery(eventId ?? null, true);
  const mutation = usePatchEventMutation(eventId ?? '');
  const [mediaError, setMediaError] = useState('');
  const [isSavingMedia, setIsSavingMedia] = useState(false);

  const {
    register,
    reset,
    handleSubmit,
    formState: { errors },
  } = useForm<EventFormInput, unknown, CreateEventFormValues>({
    resolver: zodResolver(createEventSchema),
  });

  useEffect(() => {
    if (eventQuery.data) {
      reset(toFormValues(eventQuery.data));
    }
  }, [eventQuery.data, reset]);

  if (!eventId) {
    return <Navigate replace to="/events/mine" />;
  }

  if (eventQuery.isLoading) {
    return <PageShell title="Edit event">Loading event...</PageShell>;
  }

  if (eventQuery.isError) {
    return <PageShell title="Edit event">{toEventUserMessage(eventQuery.error)}</PageShell>;
  }

  const event = eventQuery.data;

  if (!event) {
    return <Navigate replace to="/events/mine" />;
  }

  if (event.status !== EventStatus.Draft) {
    return (
      <PageShell title="Edit event">
        Only draft events can be edited. Published events must be unpublished before changes.
      </PageShell>
    );
  }

  const refreshEventQueries = async (updatedEventId: string) => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['events', 'mine'] }),
      queryClient.invalidateQueries({ queryKey: ['events', 'published'] }),
      queryClient.invalidateQueries({ queryKey: ['events', 'detail', updatedEventId] }),
    ]);
  };

  const replaceMedia = async (updatedEvent: EventResponse, values: CreateEventFormValues) => {
    setIsSavingMedia(true);

    try {
      if (values.image) {
        await attachEventImage(updatedEvent.eventId, values.image);
      }

      if (values.video) {
        const upload = await issueEventVideoUploadUrl(updatedEvent.eventId, values.video);
        await uploadEventVideo(upload.uploadUrl, values.video, upload.requiredHeaders);
        await confirmEventVideoUpload(
          updatedEvent.eventId,
          upload.videoUrl,
          values.video,
          upload.sha256,
        );
      }

      await refreshEventQueries(updatedEvent.eventId);
      navigate('/events/mine');
    } catch {
      setMediaError('Event details were saved, but media replacement failed. Try again.');
    } finally {
      setIsSavingMedia(false);
    }
  };

  const onSubmit = handleSubmit((values) => {
    setMediaError('');
    mutation.mutate(values, {
      onSuccess: (updatedEvent) => {
        void replaceMedia(updatedEvent, values);
      },
    });
  });

  const statusMessage =
    mutation.isPending || isSavingMedia
      ? 'Saving event...'
      : mediaError
        ? mediaError
        : mutation.isError
          ? toEventUserMessage(mutation.error)
          : '';

  return (
    <section className="mx-auto w-full max-w-3xl px-4 py-10 sm:px-6 lg:px-8">
      <h1 className="text-3xl font-black text-slate-950">Edit event</h1>

      <form className="mt-8 space-y-8" onSubmit={onSubmit}>
        <fieldset className="space-y-5 border-0 p-0">
          <legend className="text-xs font-bold uppercase tracking-normal text-slate-500">
            Event details
          </legend>

          <div>
            <label className={labelClassName} htmlFor="name">
              Name
            </label>
            <input className={inputClassName} id="name" {...register('name')} />
            <FieldError id="name-error" message={errors.name?.message} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClassName} htmlFor="date">
                Date
              </label>
              <input
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
              <input className={inputClassName} id="type" {...register('type')} />
              <FieldError id="type-error" message={errors.type?.message} />
            </div>
          </div>

          <div>
            <label className={labelClassName} htmlFor="place">
              Place
            </label>
            <input className={inputClassName} id="place" {...register('place')} />
            <FieldError id="place-error" message={errors.place?.message} />
          </div>

          <div>
            <label className={labelClassName} htmlFor="description">
              Description
            </label>
            <textarea
              className={inputClassName}
              id="description"
              rows={3}
              {...register('details.description')}
            />
            <FieldError id="description-error" message={errors.details?.description?.message} />
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <NumberField
              error={errors.details?.numberOfPlaces?.message}
              label="Places"
              name="details.numberOfPlaces"
              register={register}
            />
            <NumberField
              error={errors.details?.numberOfRows?.message}
              label="Rows"
              name="details.numberOfRows"
              register={register}
            />
            <NumberField
              error={errors.details?.seatsPerRow?.message}
              label="Per row"
              name="details.seatsPerRow"
              register={register}
            />
          </div>

          <div>
            <label className={labelClassName} htmlFor="image">
              Replace image (optional)
            </label>
            <input
              accept="image/jpeg,image/png,image/webp"
              className={fileInputClassName}
              id="image"
              type="file"
              {...register('image')}
            />
            <p className={hintClassName}>JPEG, PNG, or WebP, up to 5MB.</p>
            <FieldError id="image-error" message={errors.image?.message} />
          </div>

          <div>
            <label className={labelClassName} htmlFor="video">
              Replace video (optional)
            </label>
            <input
              accept="video/mp4,video/webm,video/quicktime"
              className={fileInputClassName}
              id="video"
              type="file"
              {...register('video')}
            />
            <p className={hintClassName}>MP4, WebM, or QuickTime, up to 100MB and 3 minutes.</p>
            <FieldError id="video-error" message={errors.video?.message} />
          </div>
        </fieldset>

        <p
          aria-live="polite"
          className={
            statusMessage
              ? 'rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm font-medium text-red-800'
              : 'sr-only'
          }
        >
          {statusMessage}
        </p>

        <div className="flex gap-3">
          <button
            className="flex flex-1 items-center justify-center gap-2 rounded-md bg-teal-700 px-5 py-3 text-sm font-bold text-white transition hover:bg-teal-800 disabled:cursor-not-allowed disabled:bg-slate-400"
            disabled={mutation.isPending || isSavingMedia}
            type="submit"
          >
            <Save className="h-4 w-4" aria-hidden="true" />
            Save event
          </button>
          <Link
            className="rounded-md border border-slate-300 bg-white px-5 py-3 text-sm font-bold text-slate-900 transition hover:border-teal-700 hover:text-teal-800"
            to="/events/mine"
          >
            Cancel
          </Link>
        </div>
      </form>
    </section>
  );
}

function PageShell({ children, title }: { children: string; title: string }) {
  return (
    <section className="mx-auto w-full max-w-3xl px-4 py-10 sm:px-6 lg:px-8">
      <h1 className="text-3xl font-black text-slate-950">{title}</h1>
      <p className="mt-4 rounded-md border border-slate-200 bg-white px-4 py-3 text-sm font-semibold text-slate-700">
        {children}
      </p>
      <Link
        className="mt-4 inline-block rounded-md border border-slate-300 bg-white px-5 py-3 text-sm font-bold text-slate-900 transition hover:border-teal-700 hover:text-teal-800"
        to="/events/mine"
      >
        Back to my events
      </Link>
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

function NumberField({
  error,
  label,
  name,
  register,
}: {
  error?: string;
  label: string;
  name: 'details.numberOfPlaces' | 'details.numberOfRows' | 'details.seatsPerRow';
  register: ReturnType<typeof useForm<EventFormInput, unknown, CreateEventFormValues>>['register'];
}) {
  return (
    <div>
      <label className={labelClassName} htmlFor={name}>
        {label}
      </label>
      <input className={inputClassName} id={name} type="number" {...register(name)} />
      <FieldError id={`${name}-error`} message={error} />
    </div>
  );
}

function toFormValues(event: EventResponse): EventFormInput {
  return {
    name: event.name,
    date: toLocalDateTimeInputValue(event.date),
    place: event.place,
    city: event.city ?? '',
    type: event.type,
    summary: event.summary ?? '',
    price: event.price ?? '',
    currency: event.currency ?? '',
    details: {
      description: event.details.description,
      numberOfPlaces: event.details.numberOfPlaces,
      numberOfRows: event.details.numberOfRows,
      seatsPerRow: event.details.seatsPerRow,
      placeTypes: event.details.placeTypes ?? [],
    },
  };
}

function toLocalDateTimeInputValue(value: Date): string {
  const localDate = new Date(value.getTime() - value.getTimezoneOffset() * 60_000);

  return localDate.toISOString().slice(0, 16);
}
