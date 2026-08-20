import { CalendarCheck, ShieldCheck, Smartphone } from 'lucide-react';
import type { AuthenticatedUser } from '../../api/authClient';
import { EventPreviewList } from '../events/EventPreviewList';

type HomePageProps = {
  currentUser: AuthenticatedUser | null;
  onLogin: () => void;
  onRegister: () => void;
};

const actionsByRole = {
  ADMIN: ['User administration', 'Platform operations', 'Event oversight'],
  MANAGER: ['My events', 'Create event', 'Event orders'],
  CUSTOMER: ['Browse events', 'My orders', 'My tickets'],
} as const;

const benefits = [
  {
    title: 'Easy booking',
    text: 'Choose a published event and prepare your order without waiting in a queue.',
    icon: CalendarCheck,
  },
  {
    title: 'Protected account',
    text: 'Your orders are linked to your authenticated account and session.',
    icon: ShieldCheck,
  },
  {
    title: 'Mobile ready',
    text: 'Owned tickets will be available from the web dashboard as the product grows.',
    icon: Smartphone,
  },
];

export function HomePage({ currentUser, onLogin, onRegister }: HomePageProps) {
  const roleActions = currentUser ? actionsByRole[currentUser.role] : [];

  return (
    <main>
      <section className="mx-auto grid w-full max-w-7xl gap-8 px-4 py-12 sm:px-6 lg:grid-cols-[1fr_360px] lg:px-8 lg:py-16">
        <div className="flex min-h-[390px] flex-col justify-center">
          <p className="text-sm font-semibold uppercase tracking-normal text-teal-700">
            Event tickets
          </p>
          <h1 className="mt-4 max-w-3xl text-4xl font-black leading-tight text-slate-950 sm:text-5xl lg:text-6xl">
            Order tickets without queues
          </h1>
          <p className="mt-5 max-w-2xl text-lg leading-8 text-slate-600">
            Browse upcoming events, check availability, and keep your booking history connected to a
            single account.
          </p>

          {currentUser ? (
            <div className="mt-8">
              <div className="flex flex-wrap gap-3" aria-label={`${currentUser.role} actions`}>
                {roleActions.map((action) => (
                  <button
                    className="rounded-md border border-slate-300 bg-white px-5 py-3 text-sm font-bold text-slate-900 transition hover:border-teal-700 hover:text-teal-800 focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2"
                    key={action}
                    type="button"
                  >
                    {action}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            <div className="mt-8 flex flex-col gap-3 sm:flex-row">
              <button
                className="rounded-md bg-teal-700 px-5 py-3 text-sm font-bold text-white transition hover:bg-teal-800 focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2"
                onClick={onRegister}
                type="button"
              >
                Create account
              </button>
              <button
                className="rounded-md border border-slate-300 bg-white px-5 py-3 text-sm font-bold text-slate-900 transition hover:border-teal-700 hover:text-teal-800 focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2"
                onClick={onLogin}
                type="button"
              >
                Login
              </button>
            </div>
          )}
        </div>

        <div className="grid content-center gap-3 rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
          <div className="rounded-md bg-slate-950 p-5 text-white">
            <p className="text-sm font-semibold text-teal-200">Booking snapshot</p>
            <p className="mt-3 text-4xl font-black">405</p>
            <p className="mt-1 text-sm text-slate-300">
              sample seats available across preview events
            </p>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="rounded-md border border-slate-200 p-4">
              <p className="text-2xl font-black text-slate-950">3</p>
              <p className="mt-1 text-sm text-slate-600">categories</p>
            </div>
            <div className="rounded-md border border-slate-200 p-4">
              <p className="text-2xl font-black text-slate-950">1</p>
              <p className="mt-1 text-sm text-slate-600">account</p>
            </div>
          </div>
        </div>
      </section>

      <section className="border-y border-slate-200 bg-white">
        <div className="mx-auto grid w-full max-w-7xl gap-6 px-4 py-8 sm:px-6 lg:grid-cols-3 lg:px-8">
          {benefits.map((benefit) => {
            const Icon = benefit.icon;

            return (
              <article className="flex gap-4" key={benefit.title}>
                <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-md bg-teal-50 text-teal-800">
                  <Icon className="h-5 w-5" aria-hidden="true" />
                </span>
                <div>
                  <h2 className="text-base font-bold text-slate-950">{benefit.title}</h2>
                  <p className="mt-1 text-sm leading-6 text-slate-600">{benefit.text}</p>
                </div>
              </article>
            );
          })}
        </div>
      </section>

      <EventPreviewList />
    </main>
  );
}
