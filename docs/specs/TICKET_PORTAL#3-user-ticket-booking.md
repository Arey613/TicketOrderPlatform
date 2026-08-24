# TICKET_PORTAL#3 - User Ticket Booking

## Goal

Add the regular user ticket-booking flow to the portal and align the Ticket Service API contract with authenticated customer ownership.

This iteration focuses only on the regular `CUSTOMER` user journey:

- Browse published events.
- View event details and booked places.
- Book available places after authentication.
- View owned event orders or tickets.

## Context

The platform already supports authentication, event management, published event browsing, and event-order persistence. The current event-order model exposes `customerReference`. That value should remain available in event details only when manager or admin checking is allowed, but it is not strong enough for "my tickets" behavior because ownership must come from the authenticated user, not from client-supplied data.

Public browsing remains allowed. A public viewer may inspect published events and see which places are already booked, but must not see booking ownership or customer data.

## Scope

This feature includes:

- Contract-first changes for customer-owned booking.
- Public published-event browsing and event detail viewing.
- Booked-place grid data for public and authenticated viewers.
- Optional `isMine` booked-place response hint for authenticated viewers.
- Backend booking authorization and validation.
- Frontend authentication gate for booking and owned-ticket views.
- Frontend booked-place display with a distinct state for the current user's booked places.
- Focused backend and frontend tests for the regular user flow.

This feature does not include:

- Manager event-management UI.
- Admin UI.
- Payment.
- Ticket issuance, QR codes, or scanning.
- Guest checkout.
- Seat-map storage as a separate aggregate.
- Standing/general-admission reservations.
- Multi-manager permission changes.
- Analytical read models or reporting views.
- Changing event creation or manager lifecycle behavior except where required by customer booking.

## User Flows

### Public Event Browsing

Rules:

1. Any viewer can list published events.
2. Any viewer can open published event details.
3. Draft, unpublished, or cancelled events are not available through public browsing.
4. Event list items show enough purchase context:
   - name
   - place
   - city when available
   - date
   - price and currency when available
   - available place count
5. Sold-out published events may remain visible, but the booking action must be unavailable.

### Public Event Details

Rules:

1. Public event details show the event description and place layout metadata.
2. Public event details include the list of booked places.
3. Each booked place exposes only row and place coordinates.
4. Public responses must not expose:
   - customer ID
   - order ID
   - customer email
   - customer name
   - session data
   - ownership flags

### Authenticated Event Details

Rules:

1. Authenticated customers see the same published event details as public viewers.
2. Booked places owned by the current user include an optional `isMine` flag with value `true`.
3. Booked places owned by another user may include `isMine` with value `false`.
4. Public responses omit `isMine`.
5. `isMine` is a viewer-specific read-model hint. It is not domain ownership.

Frontend display rule:

```text
isMine === true -> display as the current user's booked place
otherwise       -> display as unavailable/booked
```

The initial color choice can be simple. For example, the current user's booked places may be green while other booked places use the normal unavailable style.

### Authentication Gate

Rules:

1. Public users can browse events and inspect booked places.
2. Public users cannot book places.
3. Public users cannot view owned orders or tickets.
4. The frontend must block booking actions until login or registration succeeds.
5. The backend must still enforce authentication on booking and owned-order endpoints.
6. After login or registration, public authentication calls to action are hidden.

### Booking

Rules:

1. Only authenticated `CUSTOMER` users can book in this iteration.
2. The booking request must not include `customerId`.
3. The booking request must not include `customerReference`.
4. The backend derives customer ownership from the authenticated session.
5. A user can submit one or more places for the same published event.
6. The backend validates every requested place.
7. The backend creates all requested reservations atomically.
8. If one requested place is invalid or unavailable, no requested place is reserved.

Backend validation:

1. The user is authenticated.
2. The user has role `CUSTOMER`.
3. The event exists.
4. The event is `PUBLISHED`.
5. The event is not sold out.
6. Each row is greater than or equal to `1`.
7. Each row is less than or equal to `numberOfRows`.
8. Each place is greater than or equal to `1`.
9. Each place is less than or equal to `seatsPerRow`.
10. The request does not contain duplicate row/place pairs.
11. No requested row/place pair is already booked.
12. The database unique constraint remains the final concurrency protection for `(event_id, row_number, place_number)`.

Recommended error mapping:

- `400 Bad Request` for malformed payload, empty place list, duplicate requested places, or capacity-bound violations.
- `401 Unauthorized` for missing or invalid authentication.
- `403 Forbidden` for authenticated users that are not allowed to book.
- `404 Not Found` when the published event cannot be found through the customer booking boundary.
- `409 Conflict` when the event exists but is not bookable, is sold out, or one selected place is already booked.

### My Orders Or Tickets

Rules:

1. Only authenticated users can view their own orders or tickets.
2. The backend derives the current customer from the session.
3. The request must not accept a customer ID parameter.
4. The response includes only orders belonging to the current user.
5. Each item shows:
   - event order ID
   - event ID
   - event name
   - event date
   - row
   - place
   - place type when still used by the domain
   - reservation date
6. Empty state is valid when the user has no orders.

## Domain Model

### Aggregates

The relevant aggregates are:

```text
User
Event
EventOrder
```

Rules:

1. `User` represents the authenticated account.
2. `Event` owns event definition, publication status, and place layout metadata.
3. `EventOrder` owns the reservation data exposed to the domain.
4. Authenticated ownership is attached by the application/persistence boundary.
5. `EventOrder` must not embed the full `User` aggregate.
6. `EventOrder` must not use client-supplied identity for ownership.

### EventOrder Ownership

Keep manager-facing reference metadata:

```text
customerReference
```

Add real authenticated ownership in persistence/application operations:

```text
customerId
```

Rules:

1. `customer_id` references the authenticated `User` in the transactional database.
2. `customer_id` is assigned from the authenticated session by the backend.
3. `customerId` is not accepted in the create-order request.
4. `customerId` is not exposed in public event details.
5. `customerId` may exist in persistence/internal read models, but is not required on the domain `EventOrder`.
6. `customerReference` may be exposed in event details for manager or admin checking workflows.
7. `customerReference` must not be used for authorization, ownership filtering, or "my tickets" access.

## Database

The transactional event-order table should add user ownership while preserving manager-facing customer reference metadata.

Target column:

```text
t_event_order.customer_id UUID NOT NULL
```

Rules:

1. `customer_id` references `ticket_transactional.t_users(id)`.
2. `customer_reference` remains in place.
3. `customer_reference` must not define authenticated ownership.
4. `password_hash` remains only in `ticket_transactional.t_users`.
5. Analytical schema changes are out of scope for this ticket.

Required constraints:

```text
t_event_order(event_id, row_number, place_number) UNIQUE
t_event_order.customer_id REFERENCES ticket_transactional.t_users(id)
```

## API Contract Direction

Contract changes must start in:

```text
contracts/openapi/ticket-order-api/openapi.yml
```

After contract edits, regenerate Java and web consumers from the root:

```text
make validate-contracts
make generate
```

### Event Details

Event details should use the same endpoint for public and authenticated viewers. The endpoint returns the same event shape, with only one viewer-specific difference: authenticated responses may include the optional booked-place ownership hint.

Rules:

1. Do not create separate public and authenticated event-detail endpoints for this ticket.
2. The endpoint may accept an anonymous viewer.
3. The endpoint may accept an authenticated viewer.
4. The application service loads the same event detail read model for both viewer types.
5. The response mapper receives optional viewer context.
6. Anonymous viewer context omits ownership hints.
7. Authenticated viewer context may project ownership through `isMine`.

The booked-place schema should include an optional ownership hint.

Recommended schema direction:

```yaml
BookedPlaceResponse:
  type: object
  required:
    - row
    - place
  properties:
    row:
      type: integer
      minimum: 1
    place:
      type: integer
      minimum: 1
    isMine:
      type: boolean
      description: Present only when ownership is projected for an authenticated viewer.
    customerReference:
      type: string
      format: uuid
      description: Present only for manager or admin event-detail views when customer reference checking is allowed.
```

`isMine` is optional because it is not listed under `required`.

Rules:

1. Anonymous responses omit `isMine`.
2. Authenticated responses may include `isMine`.
3. `isMine=true` means the place belongs to the current authenticated user.
4. `isMine=false` means the authenticated viewer has ownership context, but the place belongs to another user.
5. `customerReference` is omitted for public and regular customer event-detail responses.
6. `customerReference` may be present for manager or admin event-detail responses when checking booked places.
7. The response must not expose `customerId`.

### Booking Request

The create-order request should remove client-supplied customer identity.

Recommended direction:

```yaml
CreateEventOrderItem:
  type: object
  required:
    - eventId
    - row
    - place
    - placeType
  properties:
    eventId:
      type: string
      format: uuid
    row:
      type: integer
      minimum: 1
    place:
      type: integer
      minimum: 1
    placeType:
      type: string
      minLength: 1
      maxLength: 100
```

Rules:

1. Remove `customerReference` from the request.
2. Do not add `customerId` to the request.
3. Keep list payload semantics.
4. Preserve atomic all-or-none behavior.

### Owned Orders

The existing owned-order endpoint can remain:

```http
GET /events/orders/mine
```

Rules:

1. Requires session authentication.
2. Uses the authenticated session user.
3. Does not accept a user ID.
4. Returns only orders owned by the current user.

## Backend Design

### Application Boundary

Booking should be modeled as an authenticated use-case call:

```text
createEventOrders(customerId, orders)
```

Rules:

1. The web adapter reads the authenticated principal.
2. The web adapter passes the authenticated `customerId` separately from the order payload.
3. Generated OpenAPI request models stay at the web boundary.
4. Application services receive application commands, not generated DTOs.
5. Passwords and password hashes remain outside booking commands and responses.

### Read Model

Event details may load booked places with owner identity internally when a dedicated read model is introduced:

```text
BookedPlaceReadModel
- row
- place
- customerId
```

Rules:

1. This read model is internal.
2. One event-detail application query should serve both anonymous and authenticated viewers.
3. The query receives optional viewer context.
4. The API response mapper receives optional viewer context.
5. The mapper computes optional `isMine`.
6. Public viewer context omits `isMine`.
7. Authenticated viewer context compares internal ownership data with the current user ID.
8. `customerId` is never serialized to public or authenticated event-detail responses.

Recommended SQL shape for booked places:

```sql
SELECT
    event_order.row_number,
    event_order.place_number,
    event_order.customer_id
FROM ticket_transactional.t_event_order event_order
WHERE event_order.event_id = :event_id
ORDER BY
    event_order.row_number,
    event_order.place_number;
```

This avoids loading full aggregates for a display query while keeping response shaping in the mapper.

## Frontend Design

Rules:

1. Use generated OpenAPI client calls.
2. Public users can browse and inspect event details.
3. Booking controls are disabled or gated until login or registration succeeds.
4. Authenticated `CUSTOMER` users can select available places.
5. Already booked places cannot be selected.
6. The current user's booked places use a distinct visual state.
7. Public auth calls to action disappear after authentication.
8. Owned-ticket/order view requires authentication.
9. Empty, loading, validation error, conflict, and session-expired states are visible and actionable.

## Tests

### Contract Tests

Validate:

1. `BookedPlaceResponse.isMine` is optional.
2. `BookedPlaceResponse.customerReference` is optional and only for manager/admin event-detail views.
3. Create-order request does not contain `customerId`.
4. Create-order request does not contain `customerReference`.
5. Owned-order endpoint requires session authentication.

### Backend Tests

Validate:

1. Anonymous viewer can read published event details.
2. Anonymous booked-place response omits `isMine`.
3. Authenticated customer sees `isMine=true` for owned booked places.
4. Authenticated customer sees no customer IDs in event details.
5. Booking requires authentication.
6. Booking rejects non-customer roles for this iteration.
7. Booking derives `customerId` from the session.
8. Booking does not use `customerReference` for ownership.
9. Booking rejects unpublished events.
10. Booking rejects duplicate requested places.
11. Booking rejects places outside capacity bounds.
12. Booking rejects already-booked places.
13. Booking is atomic when one requested place fails.
14. `GET /events/orders/mine` returns only the current user's orders.

### Frontend Tests

Validate:

1. Public user can see published events.
2. Public user can see booked places on event details.
3. Public user cannot submit booking.
4. Login/register gate appears when public user tries to book.
5. Authenticated customer can select available places.
6. Authenticated customer cannot select already-booked places.
7. Current user's booked places render with the owned-place visual state.
8. My tickets/orders view is blocked until authentication.
9. My tickets/orders view shows only the current user's returned orders.
10. Conflict and session-expired errors are shown clearly.

## Acceptance Criteria

1. Public users can view published event details and booked place coordinates.
2. Public users do not receive ownership hints or customer data.
3. Authenticated customers can see an optional `isMine` hint for booked places.
4. Booking is unavailable to public users.
5. Backend enforces authentication and customer role for booking.
6. Booking derives customer ownership from the authenticated session.
7. Create-order requests do not contain client-supplied ownership identity.
8. Owned orders are returned only for the current user.
9. Event-order ownership uses `customer_id` in the database and backend ownership filters.
10. `customerReference` remains available only as optional manager/admin event-detail metadata.
11. Contract, backend, and frontend tests cover the user flow.

## Implementation Plan

Implement this ticket in this order:

1. Contract.
2. Database.
3. Backend application and adapters.
4. Frontend.
5. Validation.

### 1. Contract

Update the OpenAPI contract first:

1. Rename `TakenPlaceResponse` to `BookedPlaceResponse`.
2. Add optional `isMine` to booked places by leaving it out of the schema `required` list.
3. Add optional `customerReference` to booked-place responses for manager/admin event-detail views.
4. Remove `customerReference` from create-order requests.
5. Do not add `customerId` to create-order requests.
6. Keep `eventId` on each create-order item.
7. Keep `placeType` unchanged for this ticket.
8. Keep `GET /events/orders/mine` as the owned-order endpoint.
7. Validate and regenerate consumers:

```text
make validate-contracts
make generate
```

### 2. Database

Update transactional ownership storage:

1. Add `t_event_order.customer_id`.
2. Reference `ticket_transactional.t_users(id)`.
3. Preserve `customer_reference`.
4. Replace authenticated ownership usage with `customer_id`.
5. Decide the exact migration strategy before making `customer_id NOT NULL`.
6. Keep the existing unique row/place reservation constraint.
7. Do not add analytical schema work in this ticket.

### 3. Backend

Update backend behavior after contract and database are ready:

1. Derive booking ownership from the authenticated session.
2. Reject create-order requests from anonymous users.
3. Reject create-order requests from non-`CUSTOMER` users.
4. Validate event status, capacity bounds, duplicate requested places, and already-booked places.
5. Preserve atomic all-or-none order creation.
6. Use one event-detail query path for anonymous and authenticated viewers.
7. Map optional `isMine` from viewer context.
8. Ensure `GET /events/orders/mine` filters by authenticated customer ID.

### 4. Frontend

Update the regular user portal flow:

1. Use regenerated OpenAPI client types.
2. Show published event browsing to public users.
3. Show booked places on the event grid.
4. Block booking until login or registration.
5. Render `isMine=true` places with the owned-place visual state.
6. Prevent selecting already-booked places.
7. Add owned orders or tickets view behind authentication.

### 5. Validation

Run focused checks first, then full validation:

1. Contract validation.
2. Database migration/package validation.
3. Backend tests.
4. Web tests.
5. Full `make test`.

## Architecture Validation

Architecture pass result: acceptable with the constraints below.

Strengths:

1. The design keeps identity at the backend boundary and avoids trusting frontend-supplied customer IDs.
2. `EventOrder` owns reservation behavior while referencing `User` by identity only.
3. Public event details expose availability without leaking ownership.
4. The optional `isMine` flag is a read-model projection, not a domain rule.
5. Contract-first changes fit the repository workflow.

Constraints to preserve during implementation:

1. Generated OpenAPI types must remain in web adapters.
2. Application services must receive commands and read queries, not generated DTOs.
3. `customerId` may exist in internal persistence/read models, but not in event-detail API responses.
4. Booking writes must stay on the primary transactional datasource.

## Critic Validation

Critic pass result: proceed with the decisions below.

Decisions:

1. Rename `TakenPlaceResponse` to `BookedPlaceResponse` during the contract change.
2. Keep `eventId` on each `CreateEventOrderItem` because this item shape is expected to be reused by another endpoint later.
3. Do not revisit `placeType` in this ticket. Keep the current contract/domain requirement for now.
4. Event details should be served by one endpoint. Anonymous and authenticated viewers use the same endpoint and same base read model; the only response difference is optional `isMine` projection when viewer context exists.
5. Booking role policy is explicit: only `CUSTOMER` can book in this iteration.
6. `customer_reference` remains for manager/admin event-detail checking, while `customer_id` is added for authenticated ownership.

Accepted event-detail design:

1. Route public and authenticated event-detail requests to the same controller/application query when feasible.
2. Resolve viewer context at the web boundary:
   - anonymous viewer -> empty viewer context
   - authenticated viewer -> current user ID and role
3. Load the same event details read model.
4. Map the response with optional viewer context.
5. Omit `isMine` for anonymous viewers.
6. Include `isMine` for authenticated viewers when ownership projection is available.
