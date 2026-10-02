import type { ReactNode } from 'react';
import { useEffect, useId, useRef } from 'react';

type ConfirmDialogProps = {
  title: string;
  children: ReactNode;
  confirmLabel: string;
  pendingConfirmLabel?: string;
  cancelLabel: string;
  isPending?: boolean;
  errorMessage?: string | null;
  onCancel: () => void;
  onConfirm: () => void;
};

const FOCUSABLE_SELECTOR =
  'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])';

const cancelButtonClassName =
  'rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-bold text-slate-800 transition hover:border-teal-700 hover:text-teal-800 focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60';

const dangerButtonClassName =
  'rounded-md border border-red-700 bg-red-700 px-4 py-2 text-sm font-bold text-white transition hover:bg-red-800 focus:outline-none focus:ring-2 focus:ring-red-700 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60';

export function ConfirmDialog({
  title,
  children,
  confirmLabel,
  pendingConfirmLabel,
  cancelLabel,
  isPending = false,
  errorMessage,
  onCancel,
  onConfirm,
}: ConfirmDialogProps) {
  const titleId = useId();
  const descriptionId = useId();
  const dialogRef = useRef<HTMLDivElement>(null);
  const cancelButtonRef = useRef<HTMLButtonElement>(null);
  const previouslyFocusedRef = useRef<HTMLElement | null>(null);
  const isPendingRef = useRef(isPending);
  const onCancelRef = useRef(onCancel);

  useEffect(() => {
    isPendingRef.current = isPending;
  }, [isPending]);

  useEffect(() => {
    onCancelRef.current = onCancel;
  }, [onCancel]);

  useEffect(() => {
    previouslyFocusedRef.current =
      document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const previousBodyOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    cancelButtonRef.current?.focus();

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        if (!isPendingRef.current) {
          onCancelRef.current();
        }
        return;
      }

      if (event.key !== 'Tab') {
        return;
      }

      const focusableElements = Array.from(
        dialogRef.current?.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR) ?? [],
      ).filter((element) => element.offsetParent !== null);

      if (focusableElements.length === 0) {
        event.preventDefault();
        return;
      }

      const firstElement = focusableElements[0];
      const lastElement = focusableElements[focusableElements.length - 1];

      if (event.shiftKey && document.activeElement === firstElement) {
        event.preventDefault();
        lastElement.focus();
      } else if (!event.shiftKey && document.activeElement === lastElement) {
        event.preventDefault();
        firstElement.focus();
      }
    }

    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = previousBodyOverflow;
      previouslyFocusedRef.current?.focus();
    };
  }, []);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center px-4">
      <div
        aria-hidden="true"
        className="absolute inset-0 bg-slate-950/40"
        onMouseDown={() => {
          if (!isPending) {
            onCancel();
          }
        }}
        role="presentation"
      />
      <div
        aria-describedby={descriptionId}
        aria-labelledby={titleId}
        aria-modal="true"
        className="relative w-full max-w-md rounded-md bg-white p-6 shadow-xl"
        ref={dialogRef}
        role="dialog"
      >
        <h2 className="text-xl font-black text-slate-950" id={titleId}>
          {title}
        </h2>
        <div className="mt-3 text-sm font-semibold text-slate-700" id={descriptionId}>
          {children}
        </div>
        {errorMessage && <p className="mt-4 text-sm font-semibold text-red-800">{errorMessage}</p>}
        <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            className={cancelButtonClassName}
            disabled={isPending}
            onClick={onCancel}
            ref={cancelButtonRef}
            type="button"
          >
            {cancelLabel}
          </button>
          <button
            className={dangerButtonClassName}
            disabled={isPending}
            onClick={onConfirm}
            type="button"
          >
            {isPending ? (pendingConfirmLabel ?? confirmLabel) : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
