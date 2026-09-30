# TICKET_PLATFORM#14 - Owned Event Deletion

## Goal

Allow authenticated managers and admins to delete their own draft events from the
TicketOrderPlatform API and web app.

This feature completes the event-owner management workflow for draft events while
preserving published event integrity, order safety, and the existing ownership
model.

## Approved Product Decisions

- Only `DRAFT` events can be deleted.
- `PUBLISHED` events cannot be deleted by this feature.
- Events with existing orders cannot be deleted, even though draft events should
  not have orders by design.
- `MANAGER` and `ADMIN` users can delete only events they personally own.
- Admin users do not get global delete permissions in this ticket.
- The feature includes contract, Java API, web UI, and tests.
- Tests should be written first before implementation.

## Current Context

The platform already supports owner-scoped event management:

- `MANAGER` and `ADMIN` users can create events.
- Created events start as `DRAFT`.
- Event ownership is stored as `ownerId`.
- Owners can edit draft events.
- Owners can publish and unpublish events.
- The web app exposes owned events through `/events/mine`.

The platform does not currently expose event deletion. The closest existing
delete behavior is customer event-order deletion through `DELETE /events/orders`,
which is a different workflow and must remain separate.

## Scope

This ticket includes:

- Adding `DELETE /events/{eventId}` to the OpenAPI contract.
- Generating updated Java API and web client code from the contract.
- Adding an application command use case for event deletion.
- Enforcing owner-only deletion in the application layer.
- Allowing deletion only when the event status is `DRAFT`.
- Defensively rejecting deletion when the event has existing orders.
- Adding a web delete action on the "My events" flow for draft events.
- Refreshing the owned-event list after successful deletion.
- Adding backend and frontend tests before implementation.

This ticket does not include:

- Deleting published events.
- Cancelling published events.
- Moving events to `CANCELLED`.
- Deleting event orders.
- Deleting customer tickets.
- Global admin event deletion.
- Multi-owner or delegated event management.
- Storage lifecycle cleanup for existing image or video objects.

## Domain Rules

1. An event can be deleted only by its `ownerId`.
2. The authenticated user id is the only source of ownership identity.
3. Request bodies, query parameters, or client-provided owner ids must not affect
   authorization.
4. Only `MANAGER` and `ADMIN` roles can call the event delete endpoint.
5. A `CUSTOMER` cannot delete events.
6. An unauthenticated user cannot delete events.
7. A `DRAFT` event owned by the authenticated manager/admin can be deleted.
8. A `PUBLISHED` event cannot be deleted.
9. A `CANCELLED` event cannot be deleted in this ticket.
10. An event with any existing event orders cannot be deleted.
11. Event deletion must be atomic.
12. Deleting an event must also remove its owned event-details row through the
    existing persistence relationship or explicit repository behavior.
13. Deleting an event must not delete users.
14. Deleting an event must not delete unrelated event orders.
15. Deleting an event must not expose whether another user's event exists beyond
    the existing authorization/error semantics used by event management.

## API Contract

Add this endpoint:

```text
DELETE /events/{eventId}
```

Operation:

```yaml
operationId: deleteEvent
tags:
  - Events
summary: Delete an owned draft event
security:
  - sessionAuth: []
```

Responses:

- `204` - The owned draft event was deleted.
- `401` - The user is not authenticated.
- `403` - The authenticated user is not allowed to delete the event.
- `404` - The event does not exist.
- `409` - The event exists but cannot be deleted because of status or dependent
  orders.

The endpoint must not accept an owner id, manager id, admin id, or user id in the
request.

## Java API Design

### Controller

Add a generated-controller implementation method in `EventController`:

```java
@Override
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public ResponseEntity<Void> deleteEvent(UUID eventId) {
  eventCommandUseCase.deleteEvent(eventId, currentUserProvider.currentUser().id());
  return ResponseEntity.noContent().build();
}
```

The controller should:

- Use the authenticated principal from `CurrentUserProvider`.
- Delegate business behavior to `EventCommandUseCase`.
- Not inspect repository state directly.
- Not read `SecurityContextHolder` directly.

### Application Port

Add to `EventCommandUseCase`:

```java
void deleteEvent(UUID eventId, UUID userId);
```

### Application Service

Implement deletion in `EventService`:

1. Load the event through `EventAccessGuard.requireOwnedEvent(eventId, userId)`.
2. Reject the event unless `status == DRAFT`.
3. Check whether the event has existing orders.
4. Reject deletion if any order exists.
5. Delete through `EventCommandRepositoryPort`.

The service owns business authorization and state validation. Repository methods
should support persistence behavior but should not replace the application-layer
owner and status rules.

### Repository Port

Extend `EventCommandRepositoryPort` with the smallest needed command methods,
for example:

```java
void deleteEvent(UUID eventId);
boolean existsOrdersByEventId(UUID eventId);
```

If an existing query port already provides a clear order-existence check, reuse
it instead of adding duplicate repository behavior.

### Persistence Adapter

Implement event deletion in `EventPersistenceAdapter`.

Deletion must handle the event-details relationship correctly. Prefer using the
current JPA mapping if it already cascades details deletion safely. If not,
delete dependent details explicitly before deleting the event.

Do not rely on database foreign-key failure as the normal way to detect existing
orders. The service should reject events with orders before attempting deletion.

## Web Design

Add delete behavior to the owned-events management flow.

Expected UI behavior:

1. Draft owned events show a delete action.
2. Published events do not show a delete action.
3. Delete requires explicit user confirmation.
4. During deletion, the event row/card shows a pending state.
5. On success, the owned-event list refreshes.
6. On failure, the page shows an accessible error message.
7. The event detail/edit state should not leave the user on a deleted event.

Recommended wording:

- Button label: `Delete`
- Confirmation title: `Delete draft event?`
- Confirmation body: `This removes the draft event from your event list. Published events cannot be deleted.`
- Confirm action: `Delete event`
- Cancel action: `Keep event`

## Tests First Plan

Write failing tests before implementation.

### Java Service Tests

Add or update `EventServiceTest`:

1. Owner can delete a draft event.
2. Owner cannot delete a published event.
3. Owner cannot delete a cancelled event, if `CANCELLED` exists in the enum.
4. Non-owner cannot delete another user's draft event.
5. Event with existing orders cannot be deleted.
6. Missing event produces the existing not-found behavior.

### Java Controller Tests

Add or update `EventControllerIntegrationTest`:

1. `MANAGER` owner can delete own draft event and receives `204`.
2. `ADMIN` owner can delete own draft event and receives `204`.
3. `CUSTOMER` receives `403`.
4. Unauthenticated user receives `401`.
5. Non-owner manager/admin receives `403`.
6. Published owned event receives `409`.
7. Event with orders receives `409`.

### Persistence Tests

Add or update `EventPersistenceAdapterTest` only if the new delete behavior is
not already covered by integration tests.

Focus:

- Deleting an event removes its details row.
- Deleting one event does not affect another event.
- Order-existence checks detect orders for the event.

### Web Unit Tests

Add or update My Events tests:

1. Draft events render a delete action.
2. Published events do not render a delete action.
3. Clicking delete opens confirmation.
4. Confirming delete calls the generated API client wrapper.
5. Successful delete refreshes/invalidate owned events.
6. Failed delete displays an accessible error.

### Browser Tests

Add or update Playwright coverage:

1. Manager opens "My events" and deletes a draft event.
2. Deleted draft event disappears after the successful response.
3. Published event does not expose a delete action.

## Validation Commands

Run the smallest relevant validations first:

```sh
make generate
make test-api
make test-web
```

If the implementation remains cross-module, run the full validation before
pushing:

```sh
make test
```

## Critical Architecture Notes

- Keep contract changes first because generated Java/web code depends on the
  OpenAPI source.
- Keep event deletion as an event-management command, not an order command.
- Keep owner checks inside `EventService`/`EventAccessGuard`, not only in
  `@PreAuthorize`.
- Do not grant admins global delete access in this ticket.
- Do not hard-delete published events. Published-event removal should be a
  separate cancellation/status workflow.
- Do not add storage-object cleanup to this ticket unless deletion of draft
  event media becomes a strict product requirement.
- Do not use client-provided ownership fields.

## Acceptance Criteria

1. `DELETE /events/{eventId}` exists in the OpenAPI source and generated clients.
2. The endpoint requires session authentication.
3. The endpoint is restricted to `MANAGER` and `ADMIN`.
4. The application service enforces owner-only deletion.
5. Owned `DRAFT` events can be deleted.
6. Owned `PUBLISHED` events cannot be deleted.
7. Events with existing orders cannot be deleted.
8. Non-owner users cannot delete events.
9. The web "My events" page exposes delete only for draft events.
10. Successful web deletion refreshes the owned-event list.
11. Backend and frontend tests cover success and rejection paths.
12. Existing event creation, edit, publish, unpublish, public browsing, and order
    workflows keep working.
