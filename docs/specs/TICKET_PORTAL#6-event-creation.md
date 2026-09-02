# TICKET_PORTAL#6 - Event Creation

## Goal

Give authenticated `MANAGER` and `ADMIN` users a working "Create event" flow in `ticket-order-web`, wired to the existing `POST /events` contract. This ticket is web-UI-only.

## Context

The Ticket Service API already implements event creation end to end (`TICKET_SERVICE#6-event-management.md`): `POST /events` persists the event as `DRAFT`, and accepts `name`, `date`, `place`, optional `city`, `type`, optional `summary`, optional `imageUrl`, optional `price`/`currency`, and a required `details` object (`description`, `numberOfPlaces`, `numberOfRows`, `seatsPerRow`, optional `placeTypes[]`). The generated client (`EventsApi.createEvent`, `CreateEventRequest`, `EventDetailsRequest`, `EventPlaceTypeRequest`) already exists under `src/generated/api`.

Authorization for `POST /events` is `@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")` (`EventController.java:39`), and `EventService.createEvent` does no further role check — it simply persists the event owned by the authenticated user's id. So `ADMIN` is already permitted server-side today, not just `MANAGER`; no backend or contract change is needed to expose this to both roles.

The web app has no path to this endpoint. `App.tsx` lists `'Create event'` as a nav label for `MANAGER` users only (`ADMIN`'s nav list is `['Users', 'Operations', 'Events']`, with no create-event entry), and it renders as a plain anchor with no handler (`href="#events"`). `MANAGER`'s nav array also lists `'My events'` and `'Event orders'`, equally non-functional. `TICKET_PORTAL#5-web-hardening.md` explicitly removed the equivalent non-functional action button from `HomePage.tsx` rather than ship it dead, and deferred building the real flow.

`EventService.createEvent` sets `ownerId` to the authenticated caller, not to any fixed "manager" concept. So an event created by an `ADMIN` is owned by that admin account, not by a manager. Whether a manager can later manage an admin-owned event (or vice versa) depends on ownership checks in the update/publish endpoints that this ticket does not touch. This is a pre-existing backend characteristic, not something this ticket changes, but it's worth knowing before granting `ADMIN` create access: draft events created by admins will need an admin (not necessarily any manager) to publish or edit them later, until a future ticket revisits ownership/manager-assignment.

The app currently has no routing at all — it is a single page (`App.tsx` rendering `HomePage`), with `AuthPanel` as an overlay for login/register. Event creation is deliberately a separate page rather than an overlay, so this ticket introduces the app's first real route using `react-router`. This is a bigger step than an overlay panel: it adds a new dependency and a route boundary, but matches `AGENTS.md`'s guidance to add `react-router` "once the product has multiple real views" — this is that point.

No form/validation library is installed yet (`react-hook-form`, `zod` are absent from `package.json`). The login/register form uses plain `useState`. The create-event form has 10+ fields including a required nested object and an optional repeatable array, which crosses the complexity threshold in `AGENTS.md` for adopting `react-hook-form` + `zod`. Adding these two dependencies has been approved for this ticket.

## Scope

This ticket includes:

- Adding `react-router` and a minimal route structure: `/` (existing `HomePage`) and `/events/create` (new `CreateEventPage`).
- Adding a `'Create event'` nav item, as a real link to `/events/create`, for both `MANAGER` and `ADMIN` users (today only `MANAGER`'s nav list has this label, and `ADMIN`'s does not).
- Removing the still-non-functional `'My events'` and `'Event orders'` labels from the `MANAGER` nav list (and not adding them to `ADMIN`'s), so `'Create event'` does not ship as a working link next to two dead ones. This follows `TICKET_PORTAL#5`'s precedent of removing non-functional nav/action items rather than leaving them as placeholders; they can be re-added once their own tickets build real destinations.
- A `CreateEventPage` component in `src/features/events` that renders a form matching `CreateEventRequest`/`EventDetailsRequest`, laid out as its own page (header/nav stay, page body is replaced).
- Client-side validation with `react-hook-form` + `zod`, mirroring the required/optional fields of the OpenAPI contract exactly (no new client-only constraints beyond what the contract already requires).
- A basic repeatable `placeTypes` sub-list (name, price, currency rows) since it is part of the existing `EventDetailsRequest` contract.
- A TanStack Query mutation wrapping `EventsApi.createEvent`, added to `src/api/eventsClient.ts` and a matching `useCreateEventMutation` hook in `src/features/events`.
- Success/error handling reusing the existing `toEventUserMessage` pattern.
- Route-level access control: an authenticated user who is neither `MANAGER` nor `ADMIN` (or a logged-out user) visiting `/events/create` directly is redirected to `/` instead of seeing the form.
- Page-level accessible behavior: a focus-managed heading on navigation, `aria-live` status/error region, cancel action returning to `/`.
- Adding `react-router`, `react-hook-form`, and `zod` to `package.json`.
- Unit tests for the new component/hook and Playwright coverage for the manager create-event flow.

This ticket does not include:

- Editing, publishing, or unpublishing events.
- A "My events" list or any other manager event-management screen.
- A visual seat-map/layout builder; `numberOfRows`/`seatsPerRow`/`numberOfPlaces` remain simple numeric inputs, matching what the backend already accepts (including the `9999` placeholder convention).
- Image upload; `imageUrl` is a plain URL text field.
- A live event-preview sidebar or any preview rendering beyond the form itself.
- Any backend, contract, or database change.
- Role assignment or a way to become a `MANAGER`/`ADMIN`; the flow assumes the authenticated user already has one of those roles.
- Migrating any other existing view (login/register stays an `AuthPanel` overlay; `HomePage`/events browsing stays where it is) onto the router beyond what's needed to add the `/events/create` route.

## Routing

Composition pattern (deliberately minimal, given there are only two routes):

- `currentUser` stays exactly where it is today — `App`'s own `useState` ([App.tsx:25](apps/web/ticket-order-web/src/App.tsx:25)). Do not introduce a Context/auth-provider for this ticket; with two routes both defined as direct JSX children of `App`, a route element can close over `currentUser` for free. Revisit only if a future route needs `currentUser` from somewhere that isn't a descendant of `App`.
- Use `<BrowserRouter>` with `<Routes>`/`<Route>` declared inline inside `App`'s render (not a separate router-config module), keeping `App.tsx` the single place that owns both auth state and route structure.
- Model the persistent header/footer as a layout route: a `Layout` element (the current header/nav/footer JSX, extracted from `App`) renders `<Outlet/>` for the routed page body. `/` and `/events/create` are both children of that layout route, so the header/nav/footer render once and stay constant across navigation instead of being duplicated per page.
- The `/events/create` route element applies the guard inline at the route-definition call site: render `<CreateEventPage />` when `currentUser` exists and its role is `MANAGER` or `ADMIN`, otherwise render `<Navigate to="/" replace />`. No separate guard component or hook is needed for two routes.
- The `'Create event'` nav item becomes a `react-router` `Link` to `/events/create` for both `MANAGER` and `ADMIN` users, instead of a plain anchor shown only to `MANAGER`.
- The server-side `POST /events` authorization check (`hasAnyRole('MANAGER', 'ADMIN')`) remains the actual source of truth; the client guard is a UX convenience, not a security boundary.
- `CreateEventPage` is lazy-loaded via `React.lazy`/route-level code splitting, so the form/validation bundle is not part of the main chunk.

## UI / Component Design

- `CreateEventPage` renders as a normal page: page title ("Create event"), short helper copy, the form, and a cancel action that navigates back to `/`.
- On successful submission, navigate back to `/` and show a brief success confirmation (event created as `DRAFT`); this ticket does not add a place to list or view the created event beyond that confirmation.
- Fields, grouped to match the contract:
  - Event: `name`, `date`, `place`, `city` (optional), `type`, `summary` (optional), `imageUrl` (optional), `price`/`currency` (optional pair).
  - Details: `description`, `numberOfPlaces`, `numberOfRows`, `seatsPerRow`.
  - Place types (optional, repeatable): `name`, `price`, `currency` rows with add/remove controls.

## Form & Validation

- Add `react-hook-form` for form state and `zod` for schema validation, following the pattern already approved in `AGENTS.md` once a non-trivial form exists.
- Define a `zod` schema colocated with `CreateEventPage` (e.g. `createEventSchema.ts` in `src/features/events`) that mirrors `CreateEventRequest`/`EventDetailsRequest`/`EventPlaceTypeRequest` required/optional fields and basic type/format constraints (e.g. `numberOfPlaces`/`numberOfRows`/`seatsPerRow` are positive integers, `date` parses to a valid date).
- Do not duplicate business rules already enforced server-side (e.g. publish-time requirements); this form only validates what `POST /events` itself requires for a `201`.
- Map `zod`/`react-hook-form` validation errors to visible, associated field-level messages (not just a single top-level error).

## API Integration

- Add `createEvent(command: CreateEventFormValues): Promise<EventResponse>` to `src/api/eventsClient.ts`, calling `eventsApi.createEvent` with a CSRF token via the existing `prepareCsrfToken`/`withCsrfHeader` pattern used by `createEventOrders`. `CreateEventFormValues` is the type inferred from the `zod` schema (`z.infer<typeof createEventSchema>`); this function is the mapping boundary between form values and the generated `CreateEventRequest` DTO — form values stay generated-DTO-shaped where possible, except:
  - the form's date input (a string) is converted to a `Date` before being placed on `CreateEventRequest.date`, matching the generated model's type;
  - unfilled optional fields (`city`, `summary`, `imageUrl`, `price`, `currency`) are sent as `undefined`, not empty strings;
  - an empty place-type list is sent as `undefined` for `details.placeTypes`, not `[]`.
- Add `useCreateEventMutation` in `src/features/events`, following `useCreateEventOrderMutation`'s shape: `useMutation` wrapping the client call. No query invalidation is needed for this ticket — a newly created event is always `DRAFT`, so it cannot appear in the published events list (`['events', 'published', ...]`) or any other query this ticket's scope touches. Add invalidation once a "My events" or draft-events query exists in a later ticket.
- Reuse `toEventUserMessage` for mapping `ResponseError` status codes to user-facing messages; extend it only if a new status code needs distinct copy (e.g. confirm `403` messaging covers "not a manager/admin" cases already handled generically).

## Accessibility

- `CreateEventPage` moves focus to its page `<h1>` on route entry, matching common SPA route-change focus-management practice (screen reader users are told they've navigated).
- Submission status and validation summary use an `aria-live="polite"` region, consistent with `TICKET_PORTAL#5`'s `aria-live` hardening.
- All fields have visible, programmatically associated labels; the repeatable place-type rows have accessible add/remove controls with clear names (e.g. "Remove place type 2").
- The `'Create event'` nav link is a real `<a>`-rendering `Link`, keyboard-reachable and usable with "open in new tab".

## Tests

### Unit Tests

Validate:

1. The `'Create event'` nav item renders as a link to `/events/create` for `MANAGER` and `ADMIN` users; `CUSTOMER` and logged-out users do not see a functional create-event trigger.
2. Visiting `/events/create` as a `CUSTOMER` (or logged-out) user redirects to `/`; visiting as `MANAGER` or `ADMIN` renders the form.
3. Required-field validation blocks submission and surfaces field-level errors (missing `name`, `date`, `place`, `type`, `details.description`, `details.numberOfPlaces`, `details.numberOfRows`, `details.seatsPerRow`).
4. Non-positive `numberOfPlaces`/`numberOfRows`/`seatsPerRow` are rejected client-side.
5. A valid submission calls the create-event client wrapper with a correctly shaped `CreateEventRequest`, including omitted optional fields left `undefined` rather than empty strings.
6. Adding/removing place-type rows updates the submitted `details.placeTypes` array correctly, including the case of zero rows (field omitted).
7. On success, navigation returns to `/` and a success confirmation is shown.
8. `403`/`401`/`400` responses surface the mapped message from `toEventUserMessage` without navigating away from the form.
9. Focus moves to the page heading when `/events/create` is entered.

### Browser Tests

Validate:

1. A logged-in manager can navigate to `/events/create` from the nav, fill the form, submit, and land back on `/` with a success confirmation.
2. A logged-in admin can do the same.
3. A `CUSTOMER` (or logged-out user) is redirected away from `/events/create`, including when navigating to the URL directly.
4. Keyboard-only flow: activate the nav link, tab through fields, submit, cancel returns to `/`.

## Acceptance Criteria

1. `MANAGER` and `ADMIN` users can create an event from the web app without any backend or contract change.
2. `/events/create` is a real, routed page reachable from the `'Create event'` nav link for `MANAGER` and `ADMIN` users; it is not rendered as a dead link.
3. `CUSTOMER`/logged-out users are redirected away from `/events/create`.
4. The submitted request matches the existing `CreateEventRequest`/`EventDetailsRequest` contract exactly.
5. `react-router`, `react-hook-form`, and `zod` are added as dependencies and used for this ticket.
6. The page meets the accessibility behavior described above.
7. New and existing unit and browser tests pass via `make test-web`.

## Implementation Plan

Implement this ticket in this order:

1. Add `react-router`, `react-hook-form`, and `zod` to `package.json` (after dependency-installation approval is confirmed at execution time).
2. Extract the current header/nav/footer JSX in `App.tsx` into a `Layout` component rendering `<Outlet/>`; wrap it in `<BrowserRouter>`/`<Routes>` declared inline in `App`'s render, with `/` rendering the existing `HomePage` and a placeholder `/events/create` route, both as children of the layout route. `currentUser` stays in `App`'s `useState`.
3. Remove the non-functional `'My events'` and `'Event orders'` nav labels; add `'Create event'` as a real `Link` to `/events/create` for `MANAGER` and `ADMIN`.
4. Add the `zod` schema for the create-event form, mirroring the OpenAPI contract's required/optional fields.
5. Add `createEvent` to `src/api/eventsClient.ts` (including the form-values-to-`CreateEventRequest` mapping) and `useCreateEventMutation` in `src/features/events`.
6. Build `CreateEventPage` (form, place-type repeatable rows, submission wiring, focus/accessibility behavior), lazy-loaded at the route level.
7. Wire the inline `MANAGER`/`ADMIN` guard at the `/events/create` route definition (`<Navigate to="/" replace />` otherwise).
8. Add unit tests for the schema, hook, route guard, and page behavior.
9. Add/extend Playwright coverage for the manager and admin create-event flows, including direct-URL access by a `CUSTOMER`.
10. Update `apps/web/ticket-order-web/CHANGELOG.md` and the root `CHANGELOG.md` under the `ticket-order-web` module section.
11. Run the smallest relevant validation target, then the full web target:

```text
make test-web
```
