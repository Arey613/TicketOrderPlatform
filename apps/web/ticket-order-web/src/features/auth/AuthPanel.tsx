import { Lock, Mail, X } from 'lucide-react';
import type { FormEvent } from 'react';
import { useCallback, useEffect, useRef, useState } from 'react';
import type { AuthenticatedUser } from '../../api/authClient';
import { login, register, toUserMessage } from '../../api/authClient';

type AuthMode = 'login' | 'register';

type AuthPanelProps = {
  initialMode: AuthMode;
  onClose: () => void;
  onAuthenticated: (user: AuthenticatedUser) => void;
};

export function AuthPanel({ initialMode, onClose, onAuthenticated }: AuthPanelProps) {
  const [mode, setMode] = useState<AuthMode>(initialMode);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const panelRef = useRef<HTMLElement>(null);
  const closeButtonRef = useRef<HTMLButtonElement>(null);
  const previousActiveElementRef = useRef<HTMLElement | null>(null);

  const title = mode === 'login' ? 'Login' : 'Create account';

  useEffect(() => {
    previousActiveElementRef.current =
      document.activeElement instanceof HTMLElement ? document.activeElement : null;
    closeButtonRef.current?.focus();

    return () => {
      previousActiveElementRef.current?.focus();
    };
  }, []);

  const trapFocus = useCallback((event: KeyboardEvent) => {
    const panel = panelRef.current;

    if (!panel) {
      return;
    }

    const focusableElements = Array.from(
      panel.querySelectorAll<HTMLElement>(
        'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
      ),
    ).filter((element) => !element.hasAttribute('aria-hidden'));

    const firstElement = focusableElements[0];
    const lastElement = focusableElements.at(-1);

    if (!firstElement || !lastElement) {
      event.preventDefault();
      return;
    }

    if (event.shiftKey && document.activeElement === firstElement) {
      event.preventDefault();
      lastElement.focus();
      return;
    }

    if (!event.shiftKey && document.activeElement === lastElement) {
      event.preventDefault();
      firstElement.focus();
    }
  }, []);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
        return;
      }

      if (event.key === 'Tab') {
        trapFocus(event);
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [onClose, trapFocus]);

  const submitAuth = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage('');
    setIsSubmitting(true);

    try {
      const user =
        mode === 'login'
          ? await login({ email, password })
          : await register({
              email,
              password,
            });

      onAuthenticated(user);
      onClose();
    } catch (error) {
      setErrorMessage(toUserMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    // biome-ignore lint/a11y/noStaticElementInteractions: decorative backdrop; Escape key and the close button already provide keyboard-accessible dismissal
    <div
      className="fixed inset-0 z-50 bg-slate-950/45 backdrop-blur-sm"
      onMouseDown={onClose}
      role="presentation"
    >
      <aside
        aria-labelledby="auth-panel-title"
        aria-modal="true"
        className="ml-auto flex min-h-full w-full max-w-md flex-col bg-white px-6 py-5 shadow-2xl sm:px-10 sm:py-8"
        onMouseDown={(event) => event.stopPropagation()}
        ref={panelRef}
        role="dialog"
      >
        <button
          aria-label="Close account form"
          className="ml-auto rounded-md p-2 text-slate-500 transition hover:bg-slate-100 hover:text-slate-950 focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2"
          onClick={onClose}
          ref={closeButtonRef}
          type="button"
        >
          <X className="h-5 w-5" aria-hidden="true" />
        </button>

        <div className="mt-12">
          <p className="text-sm font-semibold uppercase tracking-normal text-teal-700">
            Account access
          </p>
          <h2 className="mt-2 text-3xl font-bold text-slate-950" id="auth-panel-title">
            {title}
          </h2>
          <p className="mt-3 text-sm leading-6 text-slate-600">
            Use your account to keep orders and owned tickets connected to your session.
          </p>
        </div>

        <form className="mt-8 space-y-5" onSubmit={submitAuth}>
          <label className="block text-sm font-semibold text-slate-900">
            Email
            <span className="mt-2 flex items-center gap-3 rounded-md border border-slate-300 bg-white px-3 py-3 focus-within:border-teal-700 focus-within:ring-2 focus-within:ring-teal-700/20">
              <Mail className="h-5 w-5 text-slate-400" aria-hidden="true" />
              <input
                autoComplete="email"
                className="min-w-0 flex-1 border-0 bg-transparent text-slate-950 outline-none placeholder:text-slate-400"
                name="email"
                onChange={(event) => setEmail(event.target.value)}
                placeholder="you@example.com"
                required
                type="email"
                value={email}
              />
            </span>
          </label>

          <label className="block text-sm font-semibold text-slate-900">
            Password
            <span className="mt-2 flex items-center gap-3 rounded-md border border-slate-300 bg-white px-3 py-3 focus-within:border-teal-700 focus-within:ring-2 focus-within:ring-teal-700/20">
              <Lock className="h-5 w-5 text-slate-400" aria-hidden="true" />
              <input
                autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                className="min-w-0 flex-1 border-0 bg-transparent text-slate-950 outline-none placeholder:text-slate-400"
                minLength={mode === 'register' ? 8 : 1}
                name="password"
                onChange={(event) => setPassword(event.target.value)}
                placeholder="Password"
                required
                type="password"
                value={password}
              />
            </span>
          </label>

          <p
            aria-live="polite"
            className={
              errorMessage
                ? 'rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm font-medium text-red-800'
                : 'sr-only'
            }
          >
            {errorMessage}
          </p>

          <button
            className="w-full rounded-md bg-teal-700 px-5 py-3 text-sm font-bold text-white transition hover:bg-teal-800 focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2 disabled:cursor-not-allowed disabled:bg-slate-400"
            disabled={isSubmitting}
            type="submit"
          >
            {isSubmitting ? 'Please wait...' : title}
          </button>
        </form>

        <div className="mt-8 flex items-center justify-center gap-2 border-t border-slate-200 pt-6 text-sm text-slate-600">
          <span>{mode === 'login' ? 'New here?' : 'Already registered?'}</span>
          <button
            className="font-bold text-teal-800 underline-offset-4 hover:underline focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2"
            onClick={() => {
              setErrorMessage('');
              setMode(mode === 'login' ? 'register' : 'login');
            }}
            type="button"
          >
            {mode === 'login' ? 'Create account' : 'Login'}
          </button>
        </div>
      </aside>
    </div>
  );
}
