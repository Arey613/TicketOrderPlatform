# TICKET_PORTAL#5 - Web Hardening

## Goal

Harden `ticket-order-web` across architecture, tooling, security, accessibility, and test coverage, without changing existing user-facing booking or authentication behavior.

This ticket is a follow-up code-quality and security review of the portal, mirroring the intent of `TICKET_SERVICE#10` (backend security hardening) on the web tier.

## Context

A senior-level review of the portal surfaced the following weak points:

- `EventPreviewList` is a single 550+ line component that owns event list fetching/pagination, event detail fetching, seat-grid rendering, booking submission, and owned-order fetching/pagination. This violates the module's own `AGENTS.md` guidance to keep components small and separated by functional area.
- All data fetching uses hand-rolled `useState`/`try/catch/finally` instead of TanStack Query, despite `AGENTS.md` directing that TanStack Query be used "once the UI has real backend workflows" — which is now the case (login, register, events, orders, booking).
- A `useEffect` in `EventPreviewList` reads `selectedEventId` and `selectedSeat` but only lists `currentUser` as a dependency, a stale-closure bug.
- There is no ESLint configuration or dependency in the project at all, and `noUnusedLocals`/`noUnusedParameters` are disabled in `tsconfig.json`, so dead code and hook-dependency bugs go undetected by tooling.
- `eventMocks.ts` (`eventPreviews`) is dead code, never imported.
- Role-scoped action buttons in `HomePage.tsx` (`Create event`, `User administration`, etc.) render with no `onClick` handler.
- `navItemsByRole` (`App.tsx`) and `actionsByRole` (`HomePage.tsx`) are two separate hardcoded role→label maps that can drift.
- `nginx.conf` serves the built SPA with no security headers (`Content-Security-Policy`, `X-Content-Type-Options`, `X-Frame-Options`/`frame-ancestors`, `Referrer-Policy`, `Permissions-Policy`).
- `currentUser` is trusted from `localStorage` after initial load and never cleared when the server rejects a request with `401`; the UI can keep showing a logged-in state after the session has actually expired.
- `VITE_TICKET_API_BASE_URL` silently falls back to `http://localhost:8080` if unset, including in production builds.
- Status/error messages (`message` in `EventPreviewList`, `logoutError` in `App`) are plain text with no `aria-live` region.
- `EventPreviewList` has no dedicated unit tests; its behavior is covered only incidentally through `App.test.tsx`, and no test exercises session-expiry (`401`) behavior.

## Scope

This ticket includes:

- Splitting `EventPreviewList` into smaller, focused components.
- Migrating event/order/booking data fetching to TanStack Query.
- Adding ESLint with React hooks and accessibility rules.
- Enabling stricter unused-code checks in `tsconfig.json`.
- Removing dead code and resolving the non-functional role-action buttons.
- Consolidating the duplicated role→label maps.
- Adding SPA security headers via `nginx.conf`.
- Adding a global handler that clears stored auth state on `401`.
- Failing fast on a missing API base URL in production builds.
- Adding `aria-live` regions to status/error messaging.
- Adding unit tests for the new components and for session-expiry behavior.

This ticket does not include:

- Introducing `react-router` or new routes.
- Adopting Radix UI for `AuthPanel` (noted as a future candidate given its hand-rolled focus trap, not required here).
- Any backend or OpenAPI contract changes.
- Any change to booking, auth, or ownership business rules.

## Architecture

Rules:

1. `EventPreviewList` is split into:
   - `EventList` — published events list, pagination, refresh, and selection.
   - `EventDetailsPanel` — selected event details, seat grid, and booking submission.
   - `MyOrdersPanel` — owned orders list and pagination.
2. A shared `usePagination` hook (or equivalent) replaces the duplicated page/size/number state pairs used by events and orders.
3. Each split component owns only the state and effects for its own concern; no component reaches into another's state.
4. Data fetching for events, event details, owned orders, and the booking mutation is migrated to TanStack Query (`useQuery`/`useMutation`), replacing manual `isLoading`/`try/catch/finally` state.
5. The stale-closure `useEffect` that re-fetches authenticated event details on login is replaced by TanStack Query's dependency-driven refetching (query key includes `currentUser` and `selectedEventId`).
6. `navItemsByRole` and `actionsByRole` are consolidated into one shared role→label source used by both `App.tsx` and `HomePage.tsx`.
7. `eventMocks.ts` is deleted.
8. Role-scoped action buttons in `HomePage.tsx` that have no current destination are removed from the rendered list rather than left as non-functional placeholders; only actions with a real handler remain.

## Tooling

Rules:

1. ESLint is added with `typescript-eslint`, `eslint-plugin-react-hooks`, and `eslint-plugin-jsx-a11y`.
2. A lint script is added to `package.json` and wired into `npm run build` (and therefore `npm run test`, since `test` runs `build`).
3. `noUnusedLocals` and `noUnusedParameters` are enabled in `tsconfig.json`.
4. Existing source is cleaned up as needed so the new lint and compiler settings pass without suppressions.

## Security

Rules:

1. `nginx.conf` sets response headers on all responses: `Content-Security-Policy`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` (or equivalent `frame-ancestors 'none'` in CSP), `Referrer-Policy`, and `Permissions-Policy`.
2. The CSP is scoped to what the SPA actually needs (self-hosted script/style, the configured API origin for `connect-src`) and does not use `unsafe-inline`/`unsafe-eval` unless proven necessary.
3. A shared API response handler clears `currentUser` from React state and `localStorage` whenever a request fails with `401`, so the UI cannot keep displaying a logged-in state after the session is no longer valid.
4. The API client configuration fails fast (throws or fails the production build) when `VITE_TICKET_API_BASE_URL` is unset in a production build, instead of silently defaulting to `http://localhost:8080`.
5. The `http://localhost:8080` default remains for local development only.

## Accessibility

Rules:

1. The booking/event status message region in `EventPreviewList` and the logout error message in `App` use `aria-live="polite"` (or an equivalent live region) so screen readers announce state changes.
2. `eslint-plugin-jsx-a11y` is enabled to catch future regressions.

## Tests

### Unit Tests

Validate:

1. `EventList`, `EventDetailsPanel`, and `MyOrdersPanel` each have focused unit tests for their own loading, empty, error, and success states.
2. Pagination behavior (`usePagination` or equivalent) is tested independently of any single feature component.
3. Booking submission through `EventDetailsPanel` calls the create-order client wrapper with the selected place, matching current behavior.
4. A `401` response from any authenticated call clears `currentUser` and returns the UI to the public/logged-out state.
5. Existing `App.test.tsx` scenarios continue to pass, updated only where component boundaries changed.

### Browser Tests

Validate:

1. Existing Playwright scenarios in `tests/e2e/initial-ui.spec.ts` continue to pass unmodified in behavior (route mocks may need updating for TanStack Query timing).
2. A session-expiry scenario: an authenticated user whose next request receives `401` sees the UI return to the logged-out state.

## Acceptance Criteria

1. `EventPreviewList` no longer exists as a single combined component; its responsibilities are split as described above.
2. Event, event-detail, owned-order, and booking data flow through TanStack Query.
3. `npm run build` runs ESLint and fails on violations.
4. `eventMocks.ts` and non-functional role-action buttons are removed.
5. `navItemsByRole`/`actionsByRole` duplication is resolved.
6. `nginx.conf` sends the security headers listed above on served responses.
7. A `401` from any authenticated request clears stored auth state and updates the UI.
8. Missing `VITE_TICKET_API_BASE_URL` fails a production build instead of defaulting silently.
9. Status and error messages are announced via `aria-live`.
10. New and existing unit and browser tests pass via `make test-web`.

## Implementation Plan

Implement this ticket in this order:

1. Add ESLint (`typescript-eslint`, `eslint-plugin-react-hooks`, `eslint-plugin-jsx-a11y`) and a `lint` script; wire it into `npm run build`. Fix any violations surfaced in existing code.
2. Enable `noUnusedLocals`/`noUnusedParameters` in `tsconfig.json`; fix any resulting compiler errors.
3. Remove `eventMocks.ts` and the non-functional role-action buttons; consolidate `navItemsByRole`/`actionsByRole`.
4. Add TanStack Query's provider/client setup at the app root.
5. Split `EventPreviewList` into `EventList`, `EventDetailsPanel`, and `MyOrdersPanel`, extracting a shared pagination hook, and migrate each to TanStack Query for its data fetching and mutations.
6. Add the global `401` handler that clears stored auth state; wire it into the shared API error handling used by `authClient`/`eventsClient`.
7. Add the production-mode fail-fast check for `VITE_TICKET_API_BASE_URL`.
8. Add `aria-live` regions to the status/error message elements.
9. Update `nginx.conf` with the security headers.
10. Update/add unit tests for the new components, pagination hook, and 401 handling; update Playwright route mocks as needed.
11. Run the smallest relevant validation target, then the full web target:

```text
make test-web
```
