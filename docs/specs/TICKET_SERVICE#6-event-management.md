# TICKET_SERVICE#6 - Event Management

## Goal

Add event management to the Ticket Service API. An event represents a sellable ticketed occurrence and is owned by one authenticated user with the management role.

For this specification, the management role is named:

```text
MANAGER
```

## Context

The service already has users, login authentication, and role loading through Spring Security authorities. Event management builds on that foundation and introduces a new event aggregate persisted in the transactional database.

The event owner is the user responsible for creating and managing the event. Customers may browse published events later, but creation and management are restricted to authenticated managers.

Event ordering is separate from event management. Managers create and manage events. Any authenticated user can create an event order for a published event, subject to seat availability and layout validation.

This feature prepares the platform for later ticket inventory, payment, and customer ticket workflows. It should not implement payment or ticket issuance.

## Scope

This feature includes:

- Adding a `MANAGER` user role.
- Adding an events table in the transactional database.
- Adding an event domain model owned by a manager user.
- Creating events as the authenticated manager.
- Reading event details.
- Listing events.
- Updating event details by the owning manager.
- Publishing and unpublishing events by the owning manager.
- Creating event orders for published events as any authenticated user.
- Preventing non-manager users from creating or managing events.
- Preventing one manager from modifying another manager's event.
- Preventing duplicate event orders for the same event row and place.
- OpenAPI contract updates for the event endpoints.
- Java API implementation behind application ports.
- Integration tests for authorization and event behavior.

This feature does not include:

- Ticket types.
- Ticket inventory.
- Payments.
- Full seat-map behavior.
- QR codes or ticket scanning.
- Public organizer profiles.
- Frontend event-management screens.
- Analytical/CQRS event views unless required by the current repository migration pattern.

## Domain Model

An event has:

- `eventId`
- `ownerId`
- `date`
- `name`
- `place`
- `type`
- `status`
- `createdAt`
- `updatedAt`

An event order has:

- `eventOrderId`
- `eventId`
- `customerReference`
- `row`
- `place`
- `placeType`
- `reservationDate`

Event details have:

- `eventDetailsId`
- `eventId`
- `description`
- `numberOfPlaces`
- `numberOfRows`
- `seatsPerRow`
- `createdAt`
- `updatedAt`

Event status values:

```text
DRAFT
PUBLISHED
CANCELLED
```

Rules:

1. Every event must have exactly one owner user.
2. The owner user must have role `MANAGER`.
3. `name` is required and must be human-readable.
4. `date` is required.
5. `place` is required and identifies the main place label shown for the event.
6. `type` is required and classifies the event.
7. One event can have many related event orders through `t_event_order`.
8. Each event order belongs to exactly one event.
9. Event orders can be created by any authenticated user.
10. Event orders can be created only for `PUBLISHED` events.
11. Each event has one details record through `t_event_details`.
12. Event details describe the event and define the available place layout.
13. `numberOfPlaces` is the total available capacity for the event.
14. `numberOfRows` and `seatsPerRow` are required database values.
15. `9999` can be used as a placeholder value for `numberOfRows` or `seatsPerRow` when the manager does not have the final layout model yet.
16. Event orders represent actual reservations.
17. Event orders do not require seat-map behavior in this ticket.
18. A row in `t_event_order` means the row/place is reserved.
19. `customerReference` is a placeholder for future authenticated viewer/customer ownership.
20. `placeType` is required and classifies the concrete place, for example `STANDARD`, `VIP`, or `ACCESSIBLE`.
21. An event cannot have two event orders with the same row and place number.
22. Event creation does not create event orders.
23. New events start in `DRAFT`.
24. Only `DRAFT` events can be edited without extra rules in this feature.
25. `PUBLISHED` events are visible to regular authenticated users through read endpoints.
26. `CANCELLED` is reserved for future cancellation behavior unless this feature explicitly implements cancellation.

## Database

Add a transactional table:

```text
t_event
```

Required columns:

```text
event_id UUID PRIMARY KEY
owner_id UUID NOT NULL
date TIMESTAMP WITH TIME ZONE NOT NULL
name VARCHAR(200) NOT NULL
place VARCHAR(200) NOT NULL
type VARCHAR(100) NOT NULL
status VARCHAR(32) NOT NULL
created_at TIMESTAMP WITH TIME ZONE NOT NULL
updated_at TIMESTAMP WITH TIME ZONE NOT NULL
```

Add a transactional event-order table:

```text
t_event_order
```

Required columns:

```text
event_order_id UUID PRIMARY KEY
event_id UUID NOT NULL
customer_reference UUID NULL
row_number INTEGER NOT NULL
place_number INTEGER NOT NULL
place_type VARCHAR(100) NOT NULL
reservation_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
```

Add a transactional details table:

```text
t_event_details
```

Required columns:

```text
event_details_id UUID PRIMARY KEY
event_id UUID NOT NULL UNIQUE
description TEXT NOT NULL
number_of_places INTEGER NOT NULL
number_of_rows INTEGER NOT NULL
seats_per_row INTEGER NOT NULL
created_at TIMESTAMP WITH TIME ZONE NOT NULL
updated_at TIMESTAMP WITH TIME ZONE NOT NULL
```

Constraints:

- `owner_id` references `t_users(id)`.
- `t_event_order.event_id` references `t_event(event_id)`.
- `t_event_details.event_id` references `t_event(event_id)`.
- `status` must be one of `DRAFT`, `PUBLISHED`, or `CANCELLED`.
- `number_of_places` must be greater than zero.
- `number_of_rows` must be greater than zero.
- `seats_per_row` must be greater than zero.
- `number_of_rows = 9999` or `seats_per_row = 9999` means the exact layout is intentionally not modeled in this ticket.
- `row_number` must be greater than zero.
- `place_number` must be greater than zero.
- `place_type` must not be blank.
- `t_event_order(event_id, row_number, place_number)` must be unique.

Indexes:

- Unique index `t_event_order(event_id, row_number, place_number)` for preventing duplicate event-order positions.
- Unique index `t_event_details.event_id` for loading the single details record for an event.

Relationship:

- `t_event` has a one-to-many relationship with `t_event_order`.
- `t_event_order` is the owning foreign-key side through `event_id`.
- `row_number` and `place_number` store the concrete seat coordinate. API models may expose them as `row` and `place`.
- `t_event` has a one-to-one relationship with `t_event_details`.
- `t_event_details` is the owning foreign-key side through `event_id`.
- `reservation_date` defaults to the insert date when the application does not provide a value.

The migration must live in the transactional Java migrations module.

## API Contract

The API has two functional areas:

- Event management endpoints under `/events`, restricted to event managers where the operation changes event definitions or status.
- Event ordering endpoint under `/events/orders`, available to any authenticated user and validated independently from event-management authorization.

## Event Management Contract

Add event endpoints under:

```http
/events
```

Create event:

```http
POST /events
```

Authorization:

- Requires authentication.
- Requires `MANAGER`.
- The authenticated user becomes `ownerId`.

Request body:

```json
{
  "name": "Summer Music Night",
  "date": "2026-09-10T19:00:00Z",
  "place": "Central Hall",
  "type": "CONCERT",
  "details": {
    "description": "Outdoor concert with reserved seating",
    "numberOfPlaces": 120,
    "numberOfRows": 12,
    "seatsPerRow": 10
  }
}
```

The create-event request must contain all data needed to create the event definition:

- base `t_event` data
- required `t_event_details` data

Event orders are not part of event creation. They represent actual reservations and are created separately through the event-order endpoint after the event is published, with `eventId` supplied in the event-order request payload.

Seat-map payloads and storage are not part of this ticket.

Successful response:

```text
201 Created
```

List events:

```http
GET /events
```

Behavior:

- Authenticated customers and managers can list published events.
- Managers can optionally request their own events, including drafts, through a query parameter.

Suggested query parameter:

```text
scope=published|mine
```

Rules:

- Default scope is `published`.
- `scope=published` returns only `PUBLISHED` events.
- `scope=mine` requires `MANAGER` and returns only events owned by the authenticated manager.

Get event:

```http
GET /events/{eventId}
```

Behavior:

- `PUBLISHED` events are readable by any authenticated user.
- `DRAFT` events are readable only by their owning manager.
- The response includes the event's `t_event_details` data.
- The response includes event-order availability data from `t_event_order`.
- The response does not expose event-order customer references, reservation dates, ids, or place types.

Response shape:

```json
{
  "eventId": "00000000-0000-0000-0000-000000000001",
  "ownerId": "00000000-0000-0000-0000-000000000002",
  "name": "Summer Music Night",
  "date": "2026-09-10T19:00:00Z",
  "place": "Central Hall",
  "type": "CONCERT",
  "status": "PUBLISHED",
  "details": {
    "description": "Outdoor concert with reserved seating",
    "numberOfPlaces": 120,
    "numberOfRows": 12,
    "seatsPerRow": 10
  },
  "ordersTaken": 2,
  "takenPlaces": [
    {
      "row": 1,
      "place": 1
    },
    {
      "row": 1,
      "place": 2
    }
  ]
}
```

Update event:

```http
PUT /events/{eventId}
```

Authorization:

- Requires authentication.
- Requires `MANAGER`.
- Requires the authenticated manager to own the event.

Behavior:

- Updates editable event fields.
- Does not allow changing `ownerId`.
- Does not allow changing `status`; status transitions use dedicated endpoints.

Publish event:

```http
POST /events/{eventId}/publish
```

Authorization:

- Requires authentication.
- Requires `MANAGER`.
- Requires the authenticated manager to own the event.

Behavior:

The service must publish the event only when all of these requirements are satisfied:

1. The event exists.
2. The event is currently `DRAFT`.
3. The authenticated manager owns the event.
4. The event has required base fields: `date`, `name`, `place`, and `type`.
5. The event has one `t_event_details` record.
6. Event details include a non-blank `description`.
7. `numberOfPlaces` is positive.
8. `numberOfRows` and `seatsPerRow` are positive.

When all requirements are satisfied, the service changes `DRAFT` to `PUBLISHED` and returns the updated event.

If the event is not in `DRAFT`, return `409 Conflict`.

If the event is missing required publish data or contains invalid layout/order data, return `400 Bad Request`.

Unpublish event:

```http
POST /events/{eventId}/unpublish
```

Authorization:

- Requires authentication.
- Requires `MANAGER`.
- Requires the authenticated manager to own the event.

Behavior:

- Changes the event status from `PUBLISHED` to `DRAFT`.
- Returns the updated event.

## Event Ordering Contract

Create event orders:

```http
POST /events/orders
```

Authorization:

- Requires authentication.
- Does not require `MANAGER`.
- Customers, managers, and admins can create event orders.

Request body:

```json
{
  "orders": [
    {
      "eventId": "00000000-0000-0000-0000-000000000001",
      "customerReference": "00000000-0000-0000-0000-000000000020",
      "row": 1,
      "place": 1,
      "placeType": "STANDARD"
    }
  ]
}
```

Behavior:

- Reads event-order items from the `orders` array.
- Requires `orders` to contain at least one item.
- A single event order must be sent as a singleton `orders` list.
- Supports ordering seats for one or more published events in the same request.
- Creates each event order only when its referenced event is `PUBLISHED`.
- Validates each requested row and place as positive values.
- Rejects the request when any item conflicts with an already reserved row and place.
- Rejects the request when the payload contains duplicate `(eventId, row, place)` values.
- Defaults `reservationDate` to the insert date for each created order when the request does not provide one.
- Treats `customerReference` as an optional placeholder until viewer/customer ownership is introduced.
- Persists the order list atomically: either all valid orders are created, or none are created.

Successful response:

```text
201 Created
```

List my event orders:

```http
GET /events/orders/mine
```

Authorization:

- Requires authentication.
- Does not require `MANAGER`.
- Returns only event-order records that belong to the authenticated user.

Behavior:

- Uses `customerReference` as the temporary ownership marker until viewer/customer ownership is introduced.
- Returns the authenticated user's event orders.
- Includes enough event summary data for the user to recognize the order.
- Does not return event orders owned by other users.

Response shape:

```json
{
  "orders": [
    {
      "eventOrderId": "00000000-0000-0000-0000-000000000100",
      "eventId": "00000000-0000-0000-0000-000000000001",
      "eventName": "Summer Music Night",
      "eventDate": "2026-09-10T19:00:00Z",
      "row": 1,
      "place": 1,
      "placeType": "STANDARD",
      "reservationDate": "2026-08-10T10:00:00Z"
    }
  ]
}
```

Delete event orders:

```http
DELETE /events/orders
```

Authorization:

- Requires authentication.
- Does not require `MANAGER`.
- The authenticated user can delete only their own event-order records.

Request body:

```json
{
  "eventOrderIds": [
    "00000000-0000-0000-0000-000000000100"
  ]
}
```

Behavior:

- Reads event-order ids from the `eventOrderIds` array.
- Requires `eventOrderIds` to contain at least one id.
- A single event-order deletion must be sent as a singleton `eventOrderIds` list.
- Loads each event order by id.
- Confirms every event order belongs to the authenticated user.
- Uses `customerReference` as the temporary ownership marker until viewer/customer ownership is introduced.
- Deletes the event-order records atomically.
- After deletion, the row/place is no longer reserved because reservation is represented by the presence of a row in `t_event_order`.

Successful response:

```text
204 No Content
```

## Response Rules

```text
200 OK
Returned when an event read, update, publish, or unpublish operation succeeds.

201 Created
Returned when event creation or event-order creation succeeds.

204 No Content
Returned when event-order deletion succeeds.

400 Bad Request
Returned when the request body is structurally invalid or date validation fails.

401 Unauthorized
Returned when authentication is missing or invalid.

403 Forbidden
Returned when the authenticated user lacks the manager role for event management or tries to manage another manager's event.

404 Not Found
Returned when the event does not exist, or when returning 404 is preferred to avoid exposing a non-public draft event.

409 Conflict
Returned when an invalid status transition is requested, or when an event order conflicts with an already reserved row and place.
```

## Architecture Requirements

The Java implementation must follow the existing hexagonal structure:

- Web controllers stay in inbound adapters.
- Event use cases live behind application input ports.
- Event persistence lives behind outbound repository ports.
- JPA entities and Spring Data repositories stay in persistence adapters.
- Domain behavior must not depend on Spring MVC, Spring Security, or JPA types.

The current authenticated user should be resolved at the adapter/application boundary and passed into event use cases as an authenticated user identity containing:

- user id
- role

The domain and application layers should enforce owner checks and status-transition rules. Spring Security should still protect coarse endpoint access for authentication and role checks.

## Service Design

Use application services behind input ports. Controllers should depend on input ports, not persistence adapters.

Event command use case:

```text
EventCommandUseCase
- createEvent(command, authenticatedUser)
- updateEvent(eventId, command, authenticatedUser)
- publishEvent(eventId, authenticatedUser)
- unpublishEvent(eventId, authenticatedUser)
```

Responsibilities:

- Require authenticated user role `MANAGER`.
- Use the authenticated user id as `ownerId` during event creation.
- Enforce owner checks for update, publish, and unpublish.
- Persist `t_event` and `t_event_details` together for event creation.
- Reject `eventOrders` and `seatMap` fields in create-event requests.
- Keep new events in `DRAFT`.
- Keep status changes inside publish/unpublish operations.

Event query use case:

```text
EventQueryUseCase
- listEvents(scope, authenticatedUser)
- getEvent(eventId, authenticatedUser)
```

Responsibilities:

- Return published events for the default list scope.
- Return manager-owned events for `scope=mine`.
- Allow draft reads only for the owning manager.
- Build event details responses from `t_event` and `t_event_details`.
- Build event order availability responses from aggregated `t_event_order` data.
- Expose `ordersTaken` and `takenPlaces` without customer references, reservation dates, order ids, or place types.

Event-order use case:

```text
EventOrderUseCase
- createEventOrders(command, authenticatedUser)
- listMyEventOrders(authenticatedUser)
- deleteEventOrders(command, authenticatedUser)
```

Responsibilities:

- Require authentication but not role `MANAGER`.
- Read order items from the command payload.
- Require at least one order item.
- Treat single-order creation as a singleton list.
- Load each referenced event.
- Reject orders for non-`PUBLISHED` events.
- Validate positive row and place values for each item.
- Validate non-blank `placeType` for each item.
- Check duplicate row/place reservations before insert.
- Reject duplicate `(eventId, row, place)` values inside the same request.
- Persist the event-order list atomically.
- Treat `customerReference` as optional until viewer/customer ownership is introduced.
- List only event orders owned by the authenticated user.
- Join or compose minimal event summary data for the user's order list.
- Read event-order ids from the delete command payload.
- Require at least one event-order id.
- Treat single-order deletion as a singleton list.
- Delete only event orders owned by the authenticated user.
- Delete the event-order list atomically.
- Use `customerReference` as the temporary ownership marker for deletion until viewer/customer ownership is introduced.

Outbound ports:

```text
EventRepositoryPort
- save(event)
- findById(eventId)
- findPublished()
- findByOwner(ownerId)

EventDetailsRepositoryPort
- save(details)
- findByEventId(eventId)

EventOrderRepositoryPort
- saveAll(orders)
- findById(eventOrderId)
- findAllByIds(eventOrderIds)
- findByCustomerReference(customerReference)
- deleteAll(orders)
- existsByEventIdAndRowAndPlace(eventId, row, place)
- countByEventId(eventId)
- findTakenPlacesByEventId(eventId)
```

Implementation notes:

- `EventCommandService` should implement `EventCommandUseCase`.
- `EventQueryService` should implement `EventQueryUseCase`.
- `EventOrderService` should implement `EventOrderUseCase`.
- Duplicate row/place prevention must be enforced in the service and backed by the database unique constraint.
- Persistence adapters should map JPA entities to domain/application models; application services should not depend on JPA entities.
- `GET /events/{eventId}` should compose its response from event, details, and order-summary data instead of exposing persistence entities directly.

## Test Requirement

Use a TDD approach for the implementation. Add the contract and integration tests before production behavior where practical.

The event-management test suite should verify:

1. `MANAGER` is a valid user role in the API contract and Java model.
2. A customer cannot create an event.
3. An unauthenticated request cannot create an event.
4. A manager can create an event.
5. Created events are owned by the authenticated manager.
6. New events are created as `DRAFT`.
7. Event creation rejects missing required fields.
8. Event creation requires `date`, `name`, `place`, and `type`.
9. Event creation requires event details.
10. Event creation requires positive `numberOfPlaces`.
11. Event creation rejects non-positive `numberOfRows` or `seatsPerRow`.
12. Event creation does not persist event-order rows.
13. Event creation rejects an `eventOrders` field in the create-event request.
14. Event creation rejects a `seatMap` field in the create-event request.
15. Published events are listed by default.
16. Draft events are not listed in the default published list.
17. A manager can list their own events with `scope=mine`.
18. A manager cannot update another manager's event.
19. A manager can update their own draft event.
20. Updating an event cannot change `ownerId`.
21. A manager can publish their own draft event.
22. Publishing rejects missing event details.
23. Publishing rejects blank event detail descriptions.
24. Publishing rejects invalid capacity details.
25. Publishing a non-draft event returns `409 Conflict`.
26. A manager can unpublish their own published event.
27. Reading a draft event is limited to the owning manager.
28. Reading a published event succeeds for authenticated users.
29. `GET /events/{eventId}` includes event details.
30. `GET /events/{eventId}` includes `ordersTaken` from existing event orders.
31. `GET /events/{eventId}` includes `takenPlaces` with only `row` and `place`.
32. `GET /events/{eventId}` does not expose event-order customer references, reservation dates, ids, or place types.

The event-ordering test suite should verify:

1. An unauthenticated request cannot create event orders.
2. Event-order creation requires a non-empty `orders` list.
3. A single event order can be created through a singleton `orders` list.
4. Bulk event-order creation can create orders for multiple published events.
5. Event-order creation requires `eventId` in each order item.
6. A customer can create event orders for published events.
7. A manager can create event orders for published events.
8. Event-order creation rejects draft events.
9. Event-order creation rejects row values less than `1`.
10. Event-order creation rejects place values less than `1`.
11. Event-order creation rejects missing or blank `placeType`.
12. Event-order creation rejects duplicate `(eventId, row, place)` values inside the same request.
13. Event-order creation rejects duplicate `(row, place)` values already reserved for the same event.
14. Event-order creation accepts omitted `customerReference`.
15. Event-order creation defaults `reservationDate` when the request omits it.
16. Event-order creation is atomic when one item in the list is invalid.
17. An unauthenticated request cannot list my event orders.
18. Listing my event orders returns only records owned by the authenticated user.
19. Listing my event orders includes event summary data.
20. An unauthenticated request cannot delete event orders.
21. Event-order deletion requires a non-empty `eventOrderIds` list.
22. A single event order can be deleted through a singleton `eventOrderIds` list.
23. Bulk event-order deletion can delete multiple owned event-order records.
24. A user cannot delete another user's event-order record.
25. Event-order deletion is atomic when one requested id is not owned by the user.
26. Deleting event orders frees their row/place reservations because the reservation rows no longer exist.

## Done Criteria

This feature is done when:

- The transactional database has an events table with owner, date, status, and audit fields.
- `MANAGER` role is available wherever user roles are modeled.
- OpenAPI exposes event request and response schemas.
- OpenAPI exposes create, list, get, update, publish, and unpublish event endpoints.
- OpenAPI exposes event-order creation, listing, and deletion for authenticated users.
- Java API event behavior follows the existing hexagonal architecture.
- Authorization prevents customers and unrelated managers from managing events.
- Authorization allows authenticated non-manager users to create event orders for published events.
- Event status transitions are explicit and tested.
- The smallest relevant contract, migration, and Java API validation commands pass.
