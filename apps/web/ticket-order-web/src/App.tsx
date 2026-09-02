import { LogOut, Ticket } from 'lucide-react';
import { lazy, Suspense, useEffect, useState } from 'react';
import {
  BrowserRouter,
  Link,
  Navigate,
  Outlet,
  Route,
  Routes,
  useLocation,
  useNavigate,
} from 'react-router';
import type { AuthenticatedUser } from './api/authClient';
import { logout } from './api/authClient';
import { clearStoredUser, loadStoredUser, storeUser } from './api/authStorage';
import { onSessionExpired } from './api/sessionEvents';
import { HomePage } from './features/home/HomePage';

const AuthPanel = lazy(() =>
  import('./features/auth/AuthPanel').then((module) => ({
    default: module.AuthPanel,
  })),
);

const CreateEventPage = lazy(() =>
  import('./features/events/CreateEventPage').then((module) => ({
    default: module.CreateEventPage,
  })),
);

type AuthMode = 'login' | 'register';

const navItemsByRole = {
  ADMIN: ['Users', 'Operations', 'Events', 'Create event'],
  MANAGER: ['Create event'],
  CUSTOMER: ['Events', 'My orders', 'My tickets'],
} as const;

type LayoutProps = {
  currentUser: AuthenticatedUser | null;
  statusMessage: string;
  setStatusMessage: (message: string) => void;
  onLogin: () => void;
  onLogout: () => void;
};

function Layout({ currentUser, statusMessage, setStatusMessage, onLogin, onLogout }: LayoutProps) {
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    const state = location.state as { eventCreated?: boolean } | null;

    if (state?.eventCreated) {
      setStatusMessage('Event created as a draft.');
      navigate(location.pathname, { replace: true, state: null });
    }
  }, [location, navigate, setStatusMessage]);

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
            {(currentUser ? navItemsByRole[currentUser.role] : ['Events', 'Prices', 'Help']).map(
              (item) =>
                item === 'Create event' ? (
                  <Link className="transition hover:text-teal-800" key={item} to="/events/create">
                    {item}
                  </Link>
                ) : (
                  <a className="transition hover:text-teal-800" href="#events" key={item}>
                    {item}
                  </a>
                ),
            )}
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
                  onClick={onLogout}
                  type="button"
                >
                  <LogOut className="h-5 w-5" aria-hidden="true" />
                </button>
              </>
            ) : (
              <button
                className="rounded-md border border-teal-700 px-5 py-3 text-sm font-bold text-teal-800 transition hover:bg-teal-50 focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2"
                onClick={onLogin}
                type="button"
              >
                Login
              </button>
            )}
          </div>
        </div>
      </header>

      <div className="mx-auto w-full max-w-7xl px-4 sm:px-6 lg:px-8">
        <p
          aria-live="polite"
          className={
            statusMessage
              ? 'mt-4 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-800'
              : 'sr-only'
          }
        >
          {statusMessage}
        </p>
      </div>

      <Outlet />

      <footer className="border-t border-slate-200 bg-white">
        <div className="mx-auto flex w-full max-w-7xl flex-col gap-3 px-4 py-6 text-sm text-slate-600 sm:flex-row sm:items-center sm:justify-between sm:px-6 lg:px-8">
          <span>TicketOrderPlatform</span>
          <span>Browse published events and keep orders connected to your account.</span>
        </div>
      </footer>
    </div>
  );
}

function App() {
  const [authPanelMode, setAuthPanelMode] = useState<AuthMode | null>(null);
  const [currentUser, setCurrentUser] = useState<AuthenticatedUser | null>(() => loadStoredUser());
  const [statusMessage, setStatusMessage] = useState('');
  const isAuthPanelOpen = authPanelMode !== null;

  const openAuthPanel = (mode: AuthMode) => {
    setStatusMessage('');
    setAuthPanelMode(mode);
  };

  const completeAuthentication = (user: AuthenticatedUser) => {
    storeUser(user);
    setCurrentUser(user);
  };

  const submitLogout = async () => {
    setStatusMessage('');

    try {
      await logout();
      clearStoredUser();
      setCurrentUser(null);
    } catch {
      setStatusMessage('Logout failed. Try again in a moment.');
    }
  };

  useEffect(() => {
    return onSessionExpired(() => {
      clearStoredUser();
      setCurrentUser(null);
      setStatusMessage('Your session has expired. Please log in again.');
    });
  }, []);

  return (
    <BrowserRouter>
      <Routes>
        <Route
          element={
            <Layout
              currentUser={currentUser}
              onLogin={() => openAuthPanel('login')}
              onLogout={submitLogout}
              setStatusMessage={setStatusMessage}
              statusMessage={statusMessage}
            />
          }
        >
          <Route
            index
            element={
              <HomePage
                currentUser={currentUser}
                onLogin={() => openAuthPanel('login')}
                onRegister={() => openAuthPanel('register')}
              />
            }
          />
          <Route
            path="events/create"
            element={
              currentUser && (currentUser.role === 'MANAGER' || currentUser.role === 'ADMIN') ? (
                <Suspense fallback={null}>
                  <CreateEventPage />
                </Suspense>
              ) : (
                <Navigate replace to="/" />
              )
            }
          />
        </Route>
      </Routes>

      {isAuthPanelOpen && (
        <Suspense fallback={null}>
          <AuthPanel
            initialMode={authPanelMode}
            onAuthenticated={completeAuthentication}
            onClose={() => setAuthPanelMode(null)}
          />
        </Suspense>
      )}
    </BrowserRouter>
  );
}

export default App;
