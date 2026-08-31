# TICKET_SERVICE#9 - Pagination

## Goal

Define common pagination behavior for Ticket Service API list endpoints and the Ticket Portal UI screens that consume them.

The API must support predictable page-based navigation for list responses. The UI must consume paginated responses without assuming that all events or orders are loaded at once.

Endpoint-specific filtering, authorization, and visibility rules remain unchanged.

## Context

The current event and event-order APIs expose list endpoints:

```http
GET /events
GET /public/events
GET /events/orders/mine
```

These endpoints can grow without bound as the platform adds more events and customer orders. Returning full lists is acceptable for early development, but it creates avoidable response-size, UI, and database-query problems as data volume grows.

This ticket defines the shared API and UI pagination behavior before changing individual contracts, persistence queries, or screens.

## Scope

This feature includes:

- Adding page-based pagination query parameters to list endpoints.
- Defining a reusable paginated response shape.
- Preserving endpoint-specific filters, authorization, and visibility rules.
- Applying stable default sorting for deterministic results.
- Returning total counts for first-page UI and page-count calculation.
- Validating invalid pagination parameters consistently.
- Defining contract-level default page sizes per endpoint type.
- Updating OpenAPI, backend mapping, and web client behavior when implemented.
- Handling pagination smoothly in the backend without loading full result sets.
- Adding UI pagination controls for event browsing and owned-order browsing.
- Allowing users to configure page size in the UI within API-supported limits.
- Keeping UI state consistent when users move between pages, book a place, or delete an owned order.

This feature does not include:

- Cursor pagination.
- Infinite scroll.
- Search.
- Full-text filtering.
- Database index redesign beyond the indexes needed by the paginated queries.
- Changing who can see an event or order.
- Changing event-order creation or deletion semantics.
- Changing authentication, session, or CSRF behavior.
- Redesigning the event browsing or my-orders screen beyond the pagination behavior required here.

## Pagination Model

The API must use zero-based page pagination.

Shared query parameters:

```text
page
size
sort
```

Parameter rules:

- `page` is optional and defaults to `0`.
- `page` must be greater than or equal to `0`.
- `size` is optional and defaults per endpoint type.
- `size` must be between `1` and `100`, inclusive.
- `sort` is optional, uses a common contract parameter, and is validated against endpoint-specific acceptable values.
- Unknown sort values must return `400 Bad Request`.

Default page sizes:

```text
event lists: 10
order lists: 20
```

Event list defaults apply to:

```http
GET /events
GET /public/events
```

Order list defaults apply to:

```http
GET /events/orders/mine
```

The server must clamp neither `page` nor `size`. Invalid values must be rejected so clients notice integration mistakes.

## Response Shape

Paginated list responses must include the list content and pagination metadata.

This ticket is allowed to make a breaking list-response shape change for existing list endpoints because the API and UI clients are owned in the same repository and generated together.

Common shape:

```json
{
  "items": [],
  "page": {
    "number": 0,
    "size": 10,
    "totalElements": 135,
    "totalPages": 14,
    "first": true,
    "last": false
  }
}
```

Metadata rules:

- `number` is the zero-based page number requested by the client.
- `size` is the effective page size.
- `totalElements` is the number of records visible to the request after authorization and filters are applied.
- `totalPages` is `ceil(totalElements / size)`.
- `first` is true when `number` is `0`.
- `last` is true when there are no later pages.
- Empty result pages are valid when the requested page is beyond the available result range.

For empty result sets:

```json
{
  "items": [],
  "page": {
    "number": 0,
    "size": 10,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true
  }
}
```

Endpoint-specific list property names such as `events` or `orders` must be replaced by the common `items` field when the OpenAPI contract is changed.

## Sorting

Each paginated endpoint must define its allowed sort values explicitly. Sort strings use:

```text
field,direction
```

Supported directions:

```text
asc
desc
```

If `direction` is omitted, the endpoint default direction applies.

Every sort must add a stable tie-breaker using the entity identifier so repeated requests do not duplicate or skip records when multiple rows have the same primary sort value.

The API must not accept arbitrary entity field names as sort values.

Page pagination does not guarantee snapshot consistency across multiple page requests. If matching records are inserted, updated, or deleted between requests, later pages can shift. This is acceptable for this ticket because stable sorting plus identifier tie-breakers make each individual page deterministic, while the UI can refresh the first page after user actions that change list content.

## Endpoint Behavior

### List Authenticated Events

```http
GET /events?page=0&size=10&sort=date,asc
```

Existing behavior remains:

- Default `scope` is `published`.
- `scope=published` returns only published events visible to authenticated users.
- `scope=mine` requires `MANAGER` and returns only events owned by the authenticated manager.

Default sort:

```text
date,asc
```

Default page size:

```text
10
```

Allowed sort values:

```text
date,asc
date,desc
name,asc
name,desc
createdAt,desc
createdAt,asc
```

The total count must be calculated after applying `scope` and ownership rules.

### List Public Events

```http
GET /public/events?page=0&size=10&sort=date,asc
```

Existing behavior remains:

- Returns only published events.
- Does not require authentication.
- Does not expose draft manager data.

Default sort:

```text
date,asc
```

Default page size:

```text
10
```

Allowed sort values:

```text
date,asc
date,desc
name,asc
name,desc
```

The total count must include only published events visible through the public endpoint.

### List My Event Orders

```http
GET /events/orders/mine?page=0&size=20&sort=reservationDate,desc
```

Existing behavior remains:

- Requires authentication.
- Returns only event-order records owned by the authenticated user.
- Excludes unowned placeholder orders.
- Does not return orders owned by other users.

Default sort:

```text
reservationDate,desc
```

Default page size:

```text
20
```

Allowed sort values:

```text
reservationDate,desc
reservationDate,asc
eventDate,asc
eventDate,desc
```

The total count must be calculated after filtering by authenticated user ownership.

## Error Handling

Invalid pagination input must return `400 Bad Request`.

Examples:

- `page=-1`
- `size=0`
- `size=101`
- `sort=ownerId,asc`
- `sort=date,sideways`

The error response must use the existing API error response contract.

Authorization and visibility errors keep their existing status codes. Pagination must not convert `401`, `403`, or `404` behavior into a successful empty page.

## Persistence Requirements

Paginated queries must apply filters before pagination.

Query order:

1. Apply endpoint visibility and authorization filters.
2. Apply endpoint-specific filters such as `scope`.
3. Apply deterministic sorting.
4. Apply limit and offset.
5. Return the page content and the matching total count.

The content query and count query must use equivalent visibility filters.

When read-replica routing from `TICKET_SERVICE#8` is implemented, these list queries are eligible read-replica queries unless they are part of command validation.

Recommended supporting indexes:

- Published event browsing: status plus event date.
- Manager event browsing: owner plus event date.
- My event orders: customer reference plus reservation date.

Exact index names and migration versions should be decided during implementation after checking the current schema.

## OpenAPI Requirements

The contract should introduce reusable components:

```text
PageParameter
SortParameter
PageMetadata
```

Each paginated endpoint should reference the shared `page` and `sort` parameters. The `size` query parameter must use the same API parameter name on every endpoint and define the common validation rules inline where the endpoint-specific default is declared:

- Event list endpoints define `size` with OpenAPI `default: 10`.
- Order list endpoints define `size` with OpenAPI `default: 20`.
- Every `size` parameter must use the same API-facing name and the same validation rule of `1` to `100`.

The OpenAPI contract may use one common `SortParameter` name and schema across paginated endpoints. The common contract parameter defines the accepted syntax, not the endpoint-specific allowed fields.

The common OpenAPI `SortParameter` must not define an enum. Allowed sort values differ by endpoint, so acceptable sort values must be defined at the controller level as a list, set, or equivalent validator and used to validate whether the requested sort value is appropriate for the current endpoint.

Controllers must validate the common `sort` value against endpoint-specific acceptable values before invoking use cases:

- `GET /events` allows authenticated event sort values.
- `GET /public/events` allows public event sort values.
- `GET /events/orders/mine` allows my-order sort values.

This keeps the contract name reusable while preventing arbitrary entity fields from entering application or persistence code.

This ticket intentionally changes the generated list response shape. The API and web clients must be regenerated in the same implementation branch, and all current list consumers must migrate from endpoint-specific list fields such as `events` and `orders` to the common `items` field.

List response schemas should use:

```text
items
page
```

Endpoint-specific response names can remain distinct, for example:

```text
PaginatedEventListResponse
PaginatedMyEventOrdersResponse
```

Generated DTOs remain confined to the web adapter boundary and must be mapped into application POJOs before entering use cases.

## Backend Requirements

Application query ports should accept a pagination request object instead of passing raw web query parameters inward.

Suggested application shape:

```text
PageRequest
  page
  size
  sort

PageResult<T>
  items
  page metadata
```

Use cases must receive already-validated pagination and sort values. Web adapters are responsible for mapping OpenAPI query parameters into application pagination objects.

Repository ports should expose paginated query methods for list behavior instead of returning all records and slicing in memory.

Spring Data `Pageable` must not be exposed through application ports. If Spring `Pageable` is used, it belongs in persistence adapters after mapping from application pagination objects.

Backend pagination must be handled smoothly across the API, application, and persistence layers:

- Controllers must apply defaults when query parameters are omitted.
- Controllers must reject invalid pagination and sort parameters before invoking use cases.
- Controllers must validate the common `sort` contract parameter against the current endpoint's allowed sort values.
- Application services must pass pagination intent to repository ports without losing endpoint-specific visibility rules.
- Repository adapters must apply pagination in the database query through limit and offset.
- Repository adapters must run a matching count query using the same visibility and filter rules as the content query.
- Repository adapters must not load all matching rows and slice them in Java.
- Backend responses must preserve stable ordering between page requests by applying the requested sort plus an identifier tie-breaker.
- Empty pages beyond the available result range must return an empty `items` list with valid page metadata.
- Pagination failures caused by invalid client input must return `400 Bad Request`, not a partial or silently adjusted page.
- Backend tests must cover default sizes, custom sizes, invalid sizes, empty pages, and endpoint-specific visibility counts.

The backend must return `totalElements` and `totalPages`. Cursor pagination is out of scope because current list entities use UUID identifiers, which are not suitable as natural chronological cursors by themselves. Count-based page metadata is the required approach for this ticket.

## UI Requirements

The web app should request the first page by default.

The web app must expose a page-size control for paginated screens.

Page-size control rules:

- Available values must stay within the API-supported `1` to `100` range.
- Event browsing defaults to `10`.
- My orders defaults to `20`.
- Event browsing must expose `10`, `20`, and `50`.
- My orders must expose `20`, `50`, and `100`.
- Changing page size resets the current page to `0`.
- The selected page size should remain stable while the user stays on the screen.

The UI page-size options are UI-only defaults. Other API clients may send any valid `size` value from `1` to `100`.

For public event browsing:

- Load `page=0`.
- Use `size=10` unless the user selects a different page size.
- Use the default API sort unless the UI exposes explicit sorting.
- Display pagination controls only when more than one page exists.
- Preserve current event booking behavior after page refreshes.
- Preserve the selected event-detail interaction when the selected event is still present on the current page.
- Clear the selected event-detail interaction when the selected event is no longer present after changing page or refreshing data.

For my orders:

- Load `page=0`.
- Use `size=20` unless the user selects a different page size.
- Use newest reservations first by default.
- Refresh the first page after creating or deleting orders.
- If deleting the last item on a non-first page creates an empty page, move to the previous available page.

The UI must not assume all records are available client-side.

Pagination controls must support:

- Previous page.
- Next page.
- Current page display using one-based numbering for users.
- Disabled previous action on the first page.
- Disabled next action on the last page.
- Loading and error states that do not erase the last successful page until replacement data is available.

## Acceptance Criteria

1. OpenAPI defines reusable page and endpoint-specific size parameters.
2. OpenAPI defines reusable page metadata.
3. `GET /events` accepts `page`, `size`, and endpoint-specific `sort`, with contract default `size=10`.
4. `GET /public/events` accepts `page`, `size`, and endpoint-specific `sort`, with contract default `size=10`.
5. `GET /events/orders/mine` accepts `page`, `size`, and endpoint-specific `sort`, with contract default `size=20`.
6. List responses include `items` and `page`.
7. Invalid `page`, `size`, or `sort` values return `400 Bad Request`.
8. `scope=mine` pagination counts only the authenticated manager's events.
9. Public event pagination counts only published events.
10. My-order pagination counts only orders owned by the authenticated user.
11. Pagination does not change authentication, authorization, or event visibility rules.
12. Pagination is performed by repository queries, not by in-memory slicing after loading full lists.
13. Sorting is deterministic and uses an identifier tie-breaker.
14. Backend count queries use the same visibility and filter rules as backend content queries.
15. Backend returns valid empty page responses for pages beyond the available result range.
16. Backend tests cover default sizes, custom sizes, invalid sizes, empty pages, and visibility-specific counts.
17. Application ports do not expose Spring Data `Pageable`.
18. OpenAPI uses a common `sort` contract parameter name/schema, and controllers enforce endpoint-specific allowed sort values.
19. List response schemas use the common `items` field instead of endpoint-specific list fields.
20. UI event browsing does not assume all events are loaded at once.
21. UI my-order browsing does not assume all orders are loaded at once.
22. UI pagination controls expose previous and next navigation.
23. UI pagination controls disable invalid navigation at page boundaries.
24. UI refresh behavior after booking or deleting orders keeps the user on a valid page.
25. UI exposes configurable page size on paginated screens.
26. UI resets to the first page when the user changes page size.
27. UI page-size options are limited to screen-specific choices while the API accepts any valid `size` from `1` to `100`.

## Implementation Notes

Implementation should be contract-first:

1. Update OpenAPI source files.
2. Validate and bundle the contract.
3. Regenerate API and web contract clients.
4. Implement backend query validation, use-case input mapping, repository pagination, and tests.
5. Update web data loading and pagination controls.

Suggested validation commands:

```bash
make validate-contracts
make generate
make test
```

If the implementation touches only one module at first, run the smallest relevant target before the full cross-module validation.
