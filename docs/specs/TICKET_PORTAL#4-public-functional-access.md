# TICKET_PORTAL#4 - Public Functional Access

## Goal

Enable non-authenticated users to use the public event discovery functionality in the Ticket Portal.

Public users must be able to browse published events and inspect event details, available places, and booked places without creating an account first. Authentication remains required for booking, owned orders, and all user-specific actions.

## Context

The Ticket Service API separates public event browsing from authenticated event and order operations:

```http
GET /public/events
GET /public/events/{eventId}
POST /events/orders
GET /events/orders/mine
```

The portal must respect that boundary. The public screen should use the public endpoints for read-only discovery, then gate booking behind login or registration.

This ticket is a portal behavior and integration specification with one required backend alignment: Spring Security must allow anonymous access to the public event endpoints. It should not change event ownership, order ownership, payment, ticket issuance, or manager event-management rules.

## Visual Direction

Use the public functional access mockup as the implementation reference:

```text
docs/ui/mockups/ticket-portal-4-public-functional-access.png
```

The mockup shows the intended anonymous state: public event list, selected event details, seat grid, generic booked seats, selected seats, and a `Login to book` gate. It intentionally does not show user email, role badge, logout, owned orders, or owned-seat coloring.

## Scope

This feature includes:

- Loading published events for anonymous visitors.
- Loading published event details for anonymous visitors.
- Showing public booked-place coordinates in the event detail seat grid.
- Allowing anonymous visitors to select an available place locally before authentication.
- Opening login or registration when an anonymous visitor tries to book.
- Preserving the selected event and selected seat after successful authentication when the selected seat is still available.
- Keeping owned orders hidden until authentication succeeds.
- Keeping booking and owned-order API calls authenticated.
- Allowing authenticated event details to use the optional `isMine` seat flag as a UI color hint.
- Allowing anonymous event details to ignore missing `isMine` values.
- Adding focused portal tests for the public read and authenticated write boundary.

This feature does not include:

- Guest checkout.
- Public order creation.
- Public owned-order or ticket views.
- Public exposure of customer identity, customer ID, order ID, or ownership hints.
- Manager event-management UI.
- Admin UI.
- Payment, QR codes, scanning, or ticket issuance.
- New backend endpoints unless implementation proves an existing endpoint is missing.
- Contract changes when the existing public endpoints already satisfy the flow.

## User Flows

### Public Event Browsing

Rules:

1. A visitor can open the portal without a session.
2. The portal loads published events from the public event list endpoint.
3. Draft, unpublished, or cancelled events are not shown to public visitors.
4. Event list items show purchase context:
   - event name
   - type
   - date
   - place
   - city when available
   - starting price and currency when available
   - available place count
5. Empty public event lists show a clear empty state.
6. Loading and unavailable states do not require login.

### Public Event Details

Rules:

1. A visitor can select a published event from the list.
2. The portal loads event details from the public event detail endpoint.
3. Event details show:
   - description
   - place layout metadata
   - available place count
   - booked place coordinates
4. Public booked places are displayed as unavailable.
5. `isMine` is an authenticated UI hint for coloring seats owned by the current user.
6. Anonymous responses may omit `isMine`.
7. Anonymous UI behavior must ignore missing `isMine` values and render the place as generically booked.
8. Public details must not expose:
   - customer ID
   - customer email
   - customer name
   - order ID
   - session data

### Public Booking Gate

Rules:

1. A visitor can select an available place in the UI.
2. Selecting a place does not create an order.
3. Clicking the booking action without authentication opens the login flow.
4. The portal must not call the booking endpoint for anonymous users.
5. The booking call remains unavailable until login or registration succeeds.
6. The UI copy should make the authentication requirement clear without hiding the public discovery functionality.

### Authenticated Continuation

Rules:

1. After login or registration, public authentication calls to action are hidden.
2. The same event discovery screen remains usable.
3. Authenticated customers can book a selected available place.
4. After booking succeeds, the portal refreshes event details so the booked place becomes unavailable.
5. Owned orders are loaded only after authentication.
6. After successful login or registration, the portal keeps the selected event ID and selected seat in local UI state, then reloads the selected event with authenticated viewer context.
7. If the selected seat is no longer available after the authenticated refresh, the portal clears only the selected seat and shows the updated booked state.
8. Logging out returns the portal to the public discovery state and clears owned-order state from the UI.

## API Boundary

### Public Reads

The portal should use public endpoints for unauthenticated event discovery:

```http
GET /public/events
GET /public/events/{eventId}
```

Rules:

1. These requests must not depend on a stored user.
2. These requests may include credentials if the shared generated client is configured for session support, but they must succeed without a session.
3. Public responses drive only public display state.
4. Public display state must not assume ownership hints are present.
5. Backend security must explicitly allow anonymous `GET` access to `/public/events` and `/public/events/{eventId}`.
6. Backend security must not make authenticated event or order operations public.

### Authenticated Actions

The portal should keep these operations authenticated:

```http
POST /events/orders
GET /events/orders/mine
```

Rules:

1. `POST /events/orders` is called only after authentication.
2. `GET /events/orders/mine` is called only after authentication.
3. Anonymous users must not trigger owned-order loading.
4. Backend `401` and `403` responses remain authoritative even when the frontend gates actions.
5. Session expiration during booking should return the user to the login flow or show a clear login-required message.
6. Authenticated event detail loading may use the authenticated event detail endpoint when the UI needs viewer-specific `isMine` values.

## Frontend Design

Rules:

1. The initial screen remains a functional event browsing page, not a marketing-only landing page.
2. Public login and create-account controls are visible before authentication.
3. Booking controls can be visible to public users, but the action must be gated.
4. The event list and detail panel must work on mobile and desktop.
5. Seat buttons must keep stable dimensions and must not shift layout when selected, booked, or disabled.
6. Public booked places use the generic booked visual state.
7. Authenticated owned places may use the existing owned visual state when the API supplies `isMine=true`.
8. The `My orders` section is rendered only for authenticated users.
9. Public users should not see empty authenticated sections.

## Error Handling

Rules:

1. Public event list failure shows a public events-unavailable message.
2. Public event detail failure keeps the event list usable.
3. Booking attempted without authentication opens login instead of showing a generic failure.
4. `401 Unauthorized` from an authenticated action is shown as login required.
5. `403 Forbidden` from an authenticated action is shown as the account not being allowed to perform the action.
6. `409 Conflict` from booking is shown as the selected place no longer being available.
7. Error messages must not expose internal endpoint names or implementation details.

## Tests

### Unit Tests

Validate:

1. Public initial render loads published events through the public event list client wrapper.
2. Public event selection loads details through the public event detail client wrapper.
3. Anonymous booking opens login.
4. Anonymous booking does not call the create-order client wrapper.
5. Anonymous rendering does not load owned orders.
6. Authenticated rendering loads owned orders.
7. Authenticated booking calls the create-order client wrapper with the selected place.
8. Authenticated detail rendering can color owned seats when `isMine=true`.
9. Successful login keeps the selected event and selected seat until an authenticated refresh proves the seat is unavailable.
10. Logout hides owned orders and returns the screen to public controls.

### Browser Tests

Validate:

1. A public visitor sees published events.
2. A public visitor can inspect the seat grid.
3. A booked public place is disabled.
4. A public visitor can select an available place.
5. Clicking the booking action opens login.
6. Booking is completed only after login.
7. Owned orders appear only after login.
8. A seat with `isMine=true` uses the authenticated owned-seat visual state.

## Acceptance Criteria

1. The portal is usable without authentication for published event discovery.
2. Public event browsing uses the public event endpoints.
3. Public event details use the public event detail endpoint.
4. Anonymous users can inspect booked and available places.
5. Anonymous users cannot create orders.
6. Anonymous users do not trigger owned-order API calls.
7. Booking and owned-order flows remain authenticated.
8. Public responses do not expose customer or order ownership data.
9. The UI keeps the selected event and selected seat after authentication, unless the authenticated refresh shows the selected seat is no longer available.
10. `isMine=true` is treated only as an authenticated UI color hint.
11. Focused portal tests prove the public-read and authenticated-write boundary.

## Implementation Plan

Implement this ticket in this order:

1. Inspect the generated OpenAPI TypeScript client methods for the existing public event operations.
2. Update backend security so anonymous `GET` requests can reach the public event endpoints.
3. Update the handwritten portal events client wrapper to expose public event list and detail calls.
4. Update the event browsing component to use public wrappers for anonymous list and detail loading.
5. Use authenticated detail loading when authenticated viewer-specific `isMine` seat coloring is required.
6. Keep booking and owned-order wrappers unchanged and authenticated.
7. Update unit test mocks and assertions around the public/authenticated boundary.
8. Update browser route mocks so unauthenticated browsing exercises `/public/events`.
9. Run the smallest relevant validation target.

Recommended validation:

```text
make test-api
make test-web
```

If generated client files are missing locally, regenerate the web contract client first:

```text
make generate-web-contracts
```
