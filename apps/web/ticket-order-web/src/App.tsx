import { lazy, Suspense, useState } from 'react';
import { LogOut, Ticket } from 'lucide-react';
import { logout } from './api/authClient';
import type { AuthenticatedUser } from './api/authClient';
import { clearStoredUser, loadStoredUser, storeUser } from './api/authStorage';
import { HomePage } from './features/home/HomePage';

const AuthPanel = lazy(() =>
  import('./features/auth/AuthPanel').then((module) => ({
    default: module.AuthPanel,
  })),
);

type AuthMode = 'login' | 'register';

function App() {
  const [authPanelMode, setAuthPanelMode] = useState<AuthMode | null>(null);
  const [currentUser, setCurrentUser] = useState<AuthenticatedUser | null>(() => loadStoredUser());
  const [logoutError, setLogoutError] = useState('');
  const isAuthPanelOpen = authPanelMode !== null;

  const openAuthPanel = (mode: AuthMode) => {
    setLogoutError('');
    setAuthPanelMode(mode);
  };

  const completeAuthentication = (user: AuthenticatedUser) => {
    storeUser(user);
    setCurrentUser(user);
  };

  const submitLogout = async () => {
    setLogoutError('');

    try {
      await logout();
      clearStoredUser();
      setCurrentUser(null);
    } catch {
      setLogoutError('Logout failed. Try again in a moment.');
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-950">
      <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 backdrop-blur">
        <div className="mx-auto flex h-20 w-full max-w-7xl items-center justify-between gap-4 px-4 sm:px-6 lg:px-8">
          <a
            aria-label="TicketOrderPlatform home"
            className="flex min-w-0 items-center gap-3 text-lg font-black text-slate-950"
            href="/"
          >
            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-md bg-teal-700 text-white">
              <Ticket className="h-5 w-5" aria-hidden="true" />
            </span>
            <span className="truncate">TicketOrderPlatform</span>
          </a>

          <nav className="hidden items-center gap-8 text-sm font-semibold text-slate-700 md:flex">
            <a className="transition hover:text-teal-800" href="#events">
              Events
            </a>
            <a className="transition hover:text-teal-800" href="#prices">
              Prices
            </a>
            <a className="transition hover:text-teal-800" href="#help">
              Help
            </a>
          </nav>

          <div className="flex shrink-0 items-center gap-3">
            {currentUser ? (
              <>
                <div className="hidden text-right sm:block">
                  <p className="text-sm font-bold text-slate-950">{currentUser.email}</p>
                  <p className="text-xs font-semibold uppercase tracking-normal text-slate-500">
                    {currentUser.role}
                  </p>
                </div>
                <button
                  aria-label="Logout"
                  className="rounded-md border border-slate-300 bg-white p-3 text-slate-700 transition hover:border-red-700 hover:text-red-700 focus:outline-none focus:ring-2 focus:ring-red-700 focus:ring-offset-2"
                  onClick={submitLogout}
                  type="button"
                >
                  <LogOut className="h-5 w-5" aria-hidden="true" />
                </button>
              </>
            ) : (
              <button
                className="rounded-md border border-teal-700 px-5 py-3 text-sm font-bold text-teal-800 transition hover:bg-teal-50 focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2"
                onClick={() => openAuthPanel('login')}
                type="button"
              >
                Login
              </button>
            )}
          </div>
        </div>
      </header>

      {logoutError && (
        <div className="mx-auto mt-4 w-full max-w-7xl px-4 sm:px-6 lg:px-8">
          <p className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-800">
            {logoutError}
          </p>
        </div>
      )}

      <HomePage
        onLogin={() => openAuthPanel('login')}
        onRegister={() => openAuthPanel('register')}
      />

      <footer className="border-t border-slate-200 bg-white">
        <div className="mx-auto flex w-full max-w-7xl flex-col gap-3 px-4 py-6 text-sm text-slate-600 sm:flex-row sm:items-center sm:justify-between sm:px-6 lg:px-8">
          <span>TicketOrderPlatform</span>
          <span>Static event previews are temporary until event APIs are wired.</span>
        </div>
      </footer>

      {isAuthPanelOpen && (
        <Suspense fallback={null}>
          <AuthPanel
            initialMode={authPanelMode}
            onAuthenticated={completeAuthentication}
            onClose={() => setAuthPanelMode(null)}
          />
        </Suspense>
      )}
    </div>
  );
}

export default App;
