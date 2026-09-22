# TICKET_PLATFORM#13 - My Orders

## Goal

Add a first-class "My orders" customer flow across the TicketOrderPlatform contract, Java API, and web app.

Authenticated customers must be able to open a dedicated page and see the list of event orders that belong to their own account.

The customer-facing endpoint for this feature is:

```http
GET /orders/mine
```

This endpoint replaces the older event-scoped endpoint:

```http
GET /events/orders/mine
```

## Context

The platform already supports customer booking through event orders. The web app also has an embedded `MyOrdersPanel` inside the event browsing section, and the generated client can call the existing `GET /events/orders/mine` endpoint.

That behavior is only a partial "My orders" experience:

- "My orders" navigation does not open a dedicated route.
- The orders list is embedded below event browsing.
- The API path still presents orders as an event subresource.
- The UI does not yet provide a full page for customer order history.

This ticket promotes owned orders to their own customer-facing resource and route while keeping the current domain model name `EventOrder` where that remains useful internally.

## Scope

This feature includes:

- Replacing `GET /events/orders/mine` with `GET /orders/mine` in the OpenAPI contract.
- Replacing event-scoped owned-order response schemas with dedicated order-resource response schemas.
- Generating updated Java API and TypeScript client code from the contract.
- Updating the Java API controller mapping to expose `GET /orders/mine`.
- Returning only orders owned by the authenticated customer.
- Deriving customer ownership from the authenticated principal, never from request parameters or body fields.
- Preserving page-based pagination behavior from `TICKET_SERVICE#9`.
- Adding stable order sorting for the owned-order list.
- Updating the web client to call the new endpoint.
- Creating a routed "My orders" web page.
- Updating customer navigation so "My orders" opens the new page.
- Moving the embedded orders experience into the new page, or extracting shared components only where reuse remains useful.
- Handling loading, error, empty, refresh, and paginated states on the new page.
- Refreshing owned-order query data after successful booking.
- Updating unit, integration, and browser tests across affected modules.
- Updating root and module changelogs when implemented.

This feature does not include:

- Payment.
- Ticket issuance.
- QR codes.
- Ticket scanning.
- Order cancellation.
- Order refunding.
- Order transfer.
- Guest checkout.
- Admin lookup of another user's orders.
- Manager-wide order management.
- A separate order details page.
- Renaming every internal Java domain type away from `EventOrder`.
- Database schema changes unless a missing index is required for the new query path.

## Product Behavior

Rules:

1. A customer can open a dedicated "My orders" page.
2. The page lists only orders owned by the signed-in customer.
3. The user cannot choose or override the customer whose orders are returned.
4. A customer with no orders sees a valid empty state.
5. The list is paginated.
6. The customer-facing list shows orders for upcoming events only in this ticket.
7. Past-event orders must not be shown in the "My orders" page or returned by `GET /orders/mine`.
8. The UI requests upcoming orders by event date ascending so the nearest upcoming event appears first.
9. Booking a new place refreshes or invalidates the owned-order list so the new order can appear without a full page reload.
10. The feature is customer-focused. `MANAGER` and `ADMIN` users must not see this page or any "My orders" navigation entry in the UI for this ticket.

## Routing

Add a customer route:

```text
/orders/mine
```

Routing rules:

1. Add `/orders/mine` as a child route under the existing app layout.
2. Lazy-load the route page as `MyOrdersPage`.
3. Keep authentication state owned by `App`.
4. Render the page only when `currentUser.role` is `CUSTOMER`.
5. Redirect anonymous users to `/` and let existing login entry points handle authentication.
6. Redirect `MANAGER` and `ADMIN` users to `/`.
7. Add "My orders" to customer navigation as a `react-router` `Link` to `/orders/mine`.
8. The "My orders" navigation item must not link to `#events`.
9. The event browsing page must not be the primary destination for viewing owned orders after this ticket.

## API Contract

### List My Orders

```http
GET /orders/mine?page=0&size=20&sort=eventDate,asc
```

Authentication:

- Required.

Authorization:

- `CUSTOMER` only for this ticket.
- Missing or invalid authentication returns `401 Unauthorized`.
- Authenticated users without customer access return `403 Forbidden`.

Ownership:

- The backend derives the customer id from the authenticated principal.
- The request must not accept `customerId`.
- The request must not accept a user id, customer id, email, or owner filter in any path, query, or body field.
- The response includes only orders where `customer_id` belongs to the authenticated customer.

Replacement behavior:

- `GET /orders/mine` is the canonical endpoint.
- `GET /events/orders/mine` must be removed from the OpenAPI contract.
- `GET /events/orders/mine` must be removed from generated web usage.
- The backend must remove the old `GET /events/orders/mine` controller mapping in this ticket.
- A temporary compatibility route is out of scope unless a future ticket explicitly asks for one.

## Contract Changes

Update the OpenAPI source of truth in:

```text
contracts/openapi/ticket-order-api/openapi.yml
contracts/openapi/ticket-order-api/src/openapi.yml
contracts/openapi/ticket-order-api/src/paths/orders.yml
contracts/openapi/ticket-order-api/src/schemas/order.yml
```

Required contract changes:

1. Remove `GET /events/orders/mine` from the source and bundled OpenAPI contract.
2. Add `GET /orders/mine` with stable `operationId: listMyOrders`.
3. Add a dedicated path file `contracts/openapi/ticket-order-api/src/paths/orders.yml` for `/orders/*` customer order endpoints.
4. Register `/orders/mine` in `contracts/openapi/ticket-order-api/src/openapi.yml` by referencing `./paths/orders.yml#/~1orders~1mine`.
5. Keep event-order creation and deletion endpoints under their existing paths unless a separate ticket changes those write APIs.
6. Add a dedicated schema file `contracts/openapi/ticket-order-api/src/schemas/order.yml` for customer order response schemas.
7. Define `MyOrderResponse` in `schemas/order.yml` for items returned by `GET /orders/mine`.
8. Define `MyOrdersResponse` in `schemas/order.yml` for the paginated response returned by `GET /orders/mine`.
9. Register `MyOrderResponse` and `MyOrdersResponse` in `components.schemas` from `src/openapi.yml`.
10. `MyOrderResponse` and `MyOrdersResponse` should reuse the same payload shape as the old owned-order schemas, but under the new order-resource schema names for `/orders/mine`.
11. Do not reuse `MyEventOrderResponse` or `MyEventOrdersResponse` for the new customer-facing order resource.
12. Remove the old event-named schema definitions and references after moving their payload shape to `MyOrderResponse` and `MyOrdersResponse`, because the old schemas existed for the removed `GET /events/orders/mine` endpoint.
13. `MyOrdersResponse.items` must reference `MyOrderResponse`.
14. `MyOrdersResponse.page` must reference the shared `PageMetadata` schema.
15. Generate Java API interfaces and TypeScript client code after the contract source changes.
16. Generated clients should expose `listMyOrders`, not `listMyEventOrders`.

## Pagination And Sorting

Use the shared pagination model from `TICKET_SERVICE#9`.

Query parameters:

```text
page
size
sort
```

Rules:

1. `page` defaults to `0`.
2. `page` must be greater than or equal to `0`.
3. `size` defaults to `20`.
4. `size` must be between `1` and `100`, inclusive.
5. `sort` defaults to `eventDate,asc`.
6. Unknown sort values return `400 Bad Request`.
7. Invalid pagination values return `400 Bad Request`.
8. The server must not silently clamp invalid values.
9. Every sort must include a stable `eventOrderId` tie-breaker.

The web page must request `eventDate,asc` for the default customer view in this ticket. The backend must use the same `eventDate,asc` default when clients do not send `sort`.

Allowed sort values:

```text
reservationDate,desc
reservationDate,asc
eventDate,asc
eventDate,desc
eventName,asc
eventName,desc
```

Default list response shape:

```json
{
  "items": [],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true
  }
}
```

## Response Model

Each order item should include enough event context for the page to render without fetching one event per order.

Define a dedicated contract item schema:

```text
MyOrderResponse
```

Required fields:

```text
eventOrderId
eventId
eventName
eventDate
eventPlace
row
place
placeType
reservationDate
```

Field rules:

1. `eventOrderId` maps to the current event-order identifier and remains the identifier field name in the response.
2. `eventId` identifies the event that was booked.
3. `eventName` is the event name at read time.
4. `eventDate` is the event date at read time.
5. `eventPlace` is the event place at read time.
6. `row` and `place` expose the reserved seat coordinate.
7. `placeType` exposes the reserved place type when still used by the domain.
8. `reservationDate` is the time the reservation/order was created.
9. The response must not expose any city field.
10. The response must not expose order status; this ticket does not introduce an order lifecycle status.
11. The response must not expose `customerId`, customer email, password fields, session data, or another user's ownership data.

Define a dedicated contract page schema:

```text
MyOrdersResponse
```

Rules:

1. `MyOrdersResponse.items` contains `MyOrderResponse` items.
2. `MyOrdersResponse.page` uses shared `PageMetadata`.
3. The new order-resource schemas must live in `contracts/openapi/ticket-order-api/src/schemas/order.yml`, not as event-detail response models.

Example:

```json
{
  "items": [
    {
      "eventOrderId": "4a74be10-e319-46ed-b2eb-47bbbd165d94",
      "eventId": "fe301199-651f-4658-9977-3a7eced1be1f",
      "eventName": "Autumn Jazz Night",
      "eventDate": "2026-10-15T18:30:00Z",
      "eventPlace": "Central Hall",
      "row": 4,
      "place": 8,
      "placeType": "STANDARD",
      "reservationDate": "2026-09-21T12:00:00Z"
    }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

## Java API

Implementation rules:

1. Add or update the generated API interface from the OpenAPI contract.
2. Expose a controller method for `GET /orders/mine`.
3. Use the shared authenticated-user web helper to resolve the current user.
4. Do not read `SecurityContextHolder` directly in the controller.
5. Use `@PreAuthorize` or equivalent HTTP adapter role checks for `CUSTOMER`.
6. Keep ownership filtering inside the application service and persistence query.
7. Keep controllers thin: controller maps request/authentication data and calls an application port.
8. Preserve hexagonal boundaries with an inbound query/use-case port for listing current customer orders.
9. Use the read/query persistence path for the list operation.
10. When CQRS datasource routing is active, read `GET /orders/mine` from the read-replica datasource.
11. Keep read-replica fallback behavior owned by infrastructure and limited to connection or datasource resource failures.
12. Do not fall back to the primary datasource for SQL, mapping, authorization, constraint, or business data errors.
13. Do not load all orders and filter in memory.
14. Return the shared paginated response shape.
15. Map API errors consistently with existing event list and booking behavior.

Suggested application API shape:

```text
listMyOrders(customerId, pageRequest)
```

The exact Java names may follow existing `EventQueryUseCase` and pagination conventions.

## Database And Persistence

Expected existing ownership column:

```text
t_event_order.customer_id
```

Rules:

1. Query `t_event_order` by `customer_id`.
2. Join event data needed for the response without exposing manager-only data.
3. Filter out orders for events whose event date is in the past, using the backend current time; an order is upcoming when `event.date >= now`.
4. Apply pagination and sorting in the database query.
5. Count only upcoming rows visible to the authenticated customer.
6. Keep the database unique constraint for reserved positions unchanged.
7. Add an index only if the current schema does not support efficient owned-order pagination.
8. Execute the owned-order query through the read-replica datasource when CQRS routing is active.
9. Keep writes, booking creation, and authorization-sensitive identity reads on their existing primary datasource paths.

Candidate indexes, if missing:

```text
t_event_order(customer_id, event_id, event_order_id)
t_event(date, event_id)
t_event(name, event_id)
```

These candidate indexes support customer ownership filtering, the event join, upcoming-event filtering, stable tie-breaking by `eventOrderId`, and allowed event-based sorting by `eventDate` and `eventName`. Add or adjust indexes only after checking the current migration state and query plan.

Any database change must go through the Java Flyway migration module and should be included only if verified necessary during implementation.

## Web App

Add a page:

```text
MyOrdersPage
```

Recommended location:

```text
apps/web/ticket-order-web/src/features/orders/MyOrdersPage.tsx
```

Client integration:

1. Replace `listMyEventOrders` with a customer-facing wrapper named `listMyOrders`.
2. Create `ordersClient.ts` for `/orders/*` APIs.
3. Keep `eventsClient.ts` focused on event APIs and event booking mutations.
4. Update query keys to remain stable, for example `['orders', 'mine', page, size, sort]`.
5. Update booking success invalidation to refresh `['orders', 'mine']`.
6. Update test route mocks from `**/events/orders/mine**` to `**/orders/mine**`.

Page behavior:

1. Title: "My orders".
2. Move focus to the page heading on route entry.
3. Fetch the first page on page load.
4. Show loading state while orders load.
5. Show an error state with a retry action when loading fails.
6. Show an empty state when the customer has no orders.
7. Show a refresh action.
8. Show pagination controls using the existing pagination component.
9. Let customers change page size within API-supported limits.
10. Keep the layout readable on mobile and desktop.
11. Use a dense list or table-like layout rather than a marketing-style page.
12. Do not show placeholder actions for cancellation, payment, ticket download, QR code, or transfer.

Each row/card should show:

- event name;
- event date;
- event place;
- row and place;
- place type;
- reservation date.

## Navigation

Rules:

1. Customer navigation includes "My orders".
2. "My orders" renders as a `Link` to `/orders/mine`.
3. The link must not point to `#events`.
4. Anonymous navigation does not include "My orders".
5. Manager/admin navigation does not include "My orders" for this ticket.
6. Existing event browsing remains available separately through the events entry point.

## Accessibility

Rules:

1. The page has one `<h1>` with the text "My orders".
2. Focus moves to the heading on route entry.
3. Loading and error state changes are announced with `aria-live="polite"` where appropriate.
4. Refresh and pagination controls have accessible names.
5. Order cards or rows have semantic grouping and readable labels.
6. Dates are displayed in the existing UI date format.
7. Text must not overflow or overlap on mobile.

## Tests

### Contract Tests

Validate:

1. `GET /orders/mine` exists in the OpenAPI contract.
2. `GET /events/orders/mine` is removed from the OpenAPI contract.
3. The operation has a stable operation id such as `listMyOrders`.
4. The endpoint accepts `page`, `size`, and `sort`.
5. The endpoint does not accept `customerId`.
6. The response uses the shared paginated `items` plus `page` shape.
7. The order item schema does not expose customer identity.
8. `GET /orders/mine` returns `MyOrdersResponse`.
9. `MyOrdersResponse.items` references `MyOrderResponse`.
10. `MyEventOrderResponse` and `MyEventOrdersResponse` are removed as part of replacing `GET /events/orders/mine`.

### Java API Tests

Validate:

1. Anonymous requests to `GET /orders/mine` return `401`.
2. Authenticated non-customer users return `403`.
3. A customer receives only their own orders.
4. Another customer's orders are not returned.
5. Empty owned-order results return an empty `items` list and valid page metadata.
6. Default pagination uses page `0` and size `20`.
7. Invalid pagination values return `400`.
8. Unknown sort values return `400`.
9. Default sort is `eventDate,asc` with a stable id tie-breaker.
10. The persistence adapter applies ownership filtering in the query.
11. Manipulated pagination and sort parameters never expose another customer's orders.
12. Orders for past events are not returned.
13. CQRS-enabled tests prove the list operation uses the read/query port or read-replica transaction manager.
14. Read-replica fallback tests prove fallback happens only for connection or datasource resource failures.
15. Tests prove SQL, mapping, authorization, constraint, and business data errors do not fall back to the primary datasource.

### Web Unit Tests

Validate:

1. `CUSTOMER` users see the "My orders" nav link.
2. The "My orders" nav link points to `/orders/mine`.
3. `MANAGER`, `ADMIN`, and logged-out users do not see the "My orders" nav link.
4. Direct navigation to `/orders/mine` renders the page for `CUSTOMER`.
5. Direct navigation to `/orders/mine` redirects logged-out users to `/`.
6. Direct navigation to `/orders/mine` redirects `MANAGER` and `ADMIN` users to `/`.
7. The page calls the new `listMyOrders` wrapper with the expected pagination query.
8. Loading, empty, error, refresh, and populated states render correctly.
9. Pagination controls request the correct next and previous pages.
10. Changing page size refetches orders with the new size.
11. The page requests `eventDate,asc` by default.
12. Successful booking invalidates owned-order queries.

### Browser Tests

Validate:

1. A logged-in customer can open "My orders" from navigation.
2. The browser URL becomes `/orders/mine`.
3. The page renders the customer's orders from `GET /orders/mine`.
4. A customer with no orders sees the empty state.
5. A failed orders request shows a retry path.
6. A logged-out user cannot access `/orders/mine` directly.
7. A manager/admin cannot access `/orders/mine` directly.
8. Booking an available place and then opening "My orders" shows the new order after refresh or query invalidation.
9. Past-event orders are not shown in the list.

## Acceptance Criteria

1. `GET /orders/mine` is the canonical contract endpoint for current-customer orders.
2. Web code no longer calls `GET /events/orders/mine`.
3. The backend route is renamed from `GET /events/orders/mine` to `GET /orders/mine`; the old controller mapping is removed.
4. The backend returns only orders owned by the authenticated customer.
5. The request does not accept customer identity input.
6. Manipulating pagination or sort parameters never exposes another customer's orders.
7. The endpoint returns only orders for upcoming events.
8. The endpoint uses shared pagination and supports the allowed sort values.
9. The owned-order query uses the read-replica datasource when CQRS routing is active, with fallback limited to infrastructure resource failures.
10. The response includes order and event display data needed by the web page.
11. The response does not expose order status.
12. A routed `/orders/mine` page exists in the web app.
13. Customer navigation opens `/orders/mine`.
14. The page handles loading, error, empty, refresh, populated, and paginated states.
15. Anonymous, manager, and admin users cannot access the customer "My orders" page in this ticket.
16. Booking success refreshes or invalidates the owned-order list.
17. Contract, Java API, persistence, web unit, and browser tests cover the new endpoint and page behavior.
