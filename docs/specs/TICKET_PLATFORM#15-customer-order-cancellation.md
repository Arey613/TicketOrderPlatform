# TICKET_PLATFORM#15 - Customer Order Cancellation

## Goal

Allow authenticated customers to cancel their own upcoming event orders from the
"My orders" page.

This feature completes the basic customer booking lifecycle:

```text
browse event -> book place -> view order -> cancel order
```

Cancelling an order removes the customer's booking and makes the event place
available again.

## Approved Product Decisions

- Customers can cancel only their own orders.
- `DELETE /orders/mine` is `CUSTOMER`-only.
- The cancellation action appears in the routed "My orders" page.
- Cancellation requires an explicit confirmation dialog.
- Successful cancellation removes the order from the visible list.
- Successful cancellation makes the event place available again.
- The page refreshes the orders list automatically after the backend
  cancellation request succeeds.
- Do not add or keep a manual refresh button for this flow.
- Use the existing web app CSS and component patterns.
- Do not introduce a new visual style from reference mockups.

## Current Context

The platform already supports customer-owned orders:

- Customers can create event orders for published events.
- The backend derives order ownership from the authenticated principal.
- The routed "My orders" page is available at `/orders/mine`.
- `GET /orders/mine` lists upcoming orders owned by the signed-in customer.
- The Java API already has owned event-order deletion behavior through
  `deleteEventOrders`.
- The OpenAPI contract currently exposes event-order deletion as:

```http
DELETE /events/orders
```

That event-scoped delete endpoint is acceptable to keep for event-management
flows, but this customer-facing ticket should expose cancellation through the
customer order resource:

```http
DELETE /orders/mine
```

The "My orders" page currently lists orders, but it does not expose cancellation.
It also has a manual refresh action that should not be carried forward as part
of this ticket's final user experience.

## Scope

This ticket includes:

- Adding a customer-visible cancel action to each order on "My orders".
- Showing the cancel action only for orders returned by `GET /orders/mine`.
- Opening a confirmation dialog before cancellation.
- Adding `DELETE /orders/mine` to the customer order contract.
- Calling the customer order cancellation API after confirmation.
- Passing only the selected `eventOrderId` in the delete request.
- Invalidating/refetching the owned-order query after successful cancellation.
- Relying on backend deletion to make the event place available again.
- Removing the cancelled order from the visible "My orders" list after success.
- Showing a row-local or page-level accessible error when cancellation fails.
- Keeping the order visible when cancellation fails.
- Removing the manual refresh button from "My orders".
- Adding focused backend tests for order deletion behavior without requiring
  full integration-test coverage in this ticket.
- Adding frontend unit and browser tests for the cancellation flow.

This ticket does not include:

- Payment refunds.
- Ticket issuance changes.
- QR codes.
- Ticket transfer.
- Guest checkout.
- Cancelling another user's order.
- Manager/admin order cancellation.
- Bulk cancellation from the UI.
- Bulk cancellation is not expected in the UI, even though the API payload uses
  an `eventOrderIds` list.
- Cancelling past orders.
- Cancelling whole events.
- Published event cancellation.
- A separate order details page.
- A new visual design system.

## Domain Rules

1. A customer can cancel only an order owned by their authenticated user id.
2. The authenticated principal is the only source of customer identity.
3. Request bodies, query parameters, or client-provided user ids must not affect
   ownership checks.
4. An unauthenticated user cannot cancel an order.
5. A customer cannot cancel another customer's order.
6. Managers and admins do not get a customer-order cancellation UI in this
   ticket.
7. Cancelling an order deletes the reservation record for that order.
8. Cancelling an order makes the event place available for booking again.
9. Cancelling one order must not delete unrelated orders.
10. Cancelling one order must not change the event itself.
11. Cancellation must be atomic from the customer's perspective.
12. If cancellation fails, the order remains visible and the user receives an
    accessible error message.
13. Cancellation is a hard delete of the customer order in this ticket.
14. This ticket does not add a persisted `CANCELLED` order state or cancellation
    history.
15. Backend tests must ensure the cancelled order record is deleted from
    persistence.

## API Contract

Add a customer-facing delete operation:

```http
DELETE /orders/mine
```

Operation:

```yaml
operationId: cancelMyOrder
tags:
  - Orders
security:
  - sessionAuth: []
```

Request body:

```json
{
  "eventOrderIds": ["<event-order-id>"]
}
```

Expected responses:

- `204` - The owned order was cancelled.
- `400` - The request is invalid.
- `401` - The user is not authenticated.
- `403` - The authenticated user cannot cancel one or more requested orders.
- `404` - One or more requested orders do not exist.

The request must not accept `customerId`, `userId`, email, role, or any other
client-supplied ownership field.
The endpoint is `CUSTOMER`-only and must not allow one customer to affect
another customer's orders.

Keep the existing event-scoped operation for now:

```http
DELETE /events/orders
```

That endpoint can remain under the event-management API boundary and should not
be used by the customer "My orders" page after this ticket.

## Backend Design

Reuse the existing owned order deletion service behavior if it already satisfies
the domain rules, but expose it through the customer-facing `/orders/mine`
controller boundary.

Implementation should verify that:

1. `DELETE /orders/mine` requires authentication and `CUSTOMER` role access.
2. The service receives the authenticated user's id from `CurrentUserProvider`.
3. `EventService.deleteEventOrders` rejects unowned order ids.
4. Deleting an order removes the reservation row used by availability checks.
5. After deletion, the same row/place can be booked again by a valid customer.

The backend may delegate to the existing command use case internally. The
important architectural boundary is that the web customer flow calls the
customer order resource, not the event-management endpoint.

Do not prescribe a new service or controller dependency shape in this ticket.
Reuse the existing command path where it keeps the implementation simpler and
does not weaken the `/orders/mine` API boundary.

## Web Design

Update:

```text
apps/web/ticket-order-web/src/features/orders/MyOrdersPage.tsx
```

Expected UI behavior:

1. Each order row/card shows a `Cancel order` action.
2. The action uses existing button styles and spacing from the web app.
3. The page does not show a manual `Refresh` button.
4. Clicking `Cancel order` opens a confirmation dialog.
5. The dialog identifies the action as order cancellation.
6. Confirming cancellation disables the relevant controls while the request is
   pending.
7. Cancelling/closing the dialog leaves the order unchanged.
8. Successful cancellation invalidates/refetches `['orders', 'mine']`.
9. The "My orders" list refreshes automatically after the backend request
   succeeds.
10. The cancelled order disappears from the visible order list after refresh.
11. Failed cancellation keeps the order visible.
12. Failed cancellation shows an accessible error message.
13. Pagination state remains stable unless the current page becomes empty after
    the cancellation.
14. If cancellation removes the only order on the last page and the current page
    number is greater than `0`, request the previous page.
15. If cancellation removes the only order on page `0`, remain on page `0` and
    show the empty state.
16. When moving from an emptied page to the previous page, update the pagination
    state first and let the changed query key fetch the previous page instead of
    refetching the now-empty page first.

Recommended wording:

- Button label: `Cancel order`
- Confirmation title: `Cancel order?`
- Confirmation body: `This removes your booking and makes the place available again.`
- Confirm action: `Cancel order`
- Cancel action: `Keep order`
- Success status, if shown briefly: `Order cancelled.`
- Error message: `Could not cancel the order. Try again.`

## Web Client Design

Add an order-cancellation wrapper near the existing order client code.

Recommended location:

```text
apps/web/ticket-order-web/src/api/ordersClient.ts
```

Recommended wrapper:

```ts
export async function cancelMyOrder(eventOrderId: string): Promise<void> {
  // Calls generated cancelMyOrder with a singleton eventOrderIds list.
}
```

Add a mutation hook under:

```text
apps/web/ticket-order-web/src/features/orders
```

The hook should:

1. Call `cancelMyOrder`.
2. Invalidate owned-order queries after success.
3. Avoid broad public event-list invalidation unless the current UI state
   already has a specific affected event-detail query to refresh.
4. Let the page display row-local pending and error state.

## Accessibility

Rules:

1. The confirmation dialog uses `role="dialog"` or an equivalent accessible
   dialog pattern already used in the app.
2. The dialog has an accessible name matching `Cancel order?`.
3. Focus moves into the dialog when it opens.
4. Closing the dialog returns focus to the triggering `Cancel order` button.
5. The pending state is visible and available to assistive technology.
6. Error messages use an accessible live region or alert pattern.
7. Keyboard-only users can open, confirm, and dismiss the dialog.
8. Text must not overflow or overlap on mobile.

## Tests First Plan

Write failing tests before implementation.

### Backend Tests

Integration tests are out of scope for this ticket. Mock-based service tests are
acceptable for the delete behavior:

1. Customer can delete one owned order.
2. Customer cannot delete another customer's order.
3. Cancelling an owned order invokes the repository delete path for the selected
   order id.
4. Deleting an order makes its event place available for a future booking, or at
   minimum proves the reservation delete path is the same path used by
   availability checks.
5. The delete behavior does not require client-supplied customer identity.

### Contract And Controller Tests

Add lightweight contract/controller coverage for the new route:

1. `DELETE /orders/mine` exists in the OpenAPI contract.
2. `DELETE /orders/mine` requires `CUSTOMER` role access.
3. `DELETE /orders/mine` returns `401` for unauthenticated requests.
4. `DELETE /orders/mine` uses authenticated ownership and does not accept
   client-supplied customer identity.

### Web Unit Tests

Add or update `MyOrdersPage` tests:

1. Orders render a `Cancel order` action.
2. The page does not render a manual refresh button.
3. Clicking `Cancel order` opens a confirmation dialog.
4. Choosing `Keep order` closes the dialog without calling the API.
5. Confirming cancellation calls the order-cancellation wrapper with the selected
   `eventOrderId`.
6. Successful cancellation invalidates/refetches the owned-order query.
7. Failed cancellation displays an accessible error and keeps the order visible.
8. Pending cancellation disables the relevant cancel action.
9. Cancelling the only order on the last page requests the previous page.
10. Cancelling the only order on page `0` keeps page `0` selected and shows the
    empty state.
11. The UI calls cancellation with a singleton `eventOrderIds` list; bulk
    cancellation controls are not rendered.

### Browser Tests

Add or update Playwright coverage:

1. Customer opens "My orders" and cancels an order.
2. Confirmation is required before the API request is made.
3. The cancelled order disappears after a successful response.
4. A failed cancellation leaves the order visible and shows an error.
5. Cancelling the only order on the last page navigates to the previous page of
   orders.

## Validation Commands

Run the smallest relevant validations first:

```sh
make test-web
make test-api
```

If implementation touches generated contract code or cross-module behavior, run:

```sh
make generate
make test
```

## Critical Architecture Notes

- Keep cancellation as an owned customer-order command, not as an event-management
  command in the UI.
- Keep ownership enforcement on the backend; the UI is only a convenience layer.
- Do not add client-supplied customer identity.
- Do not add a manual refresh button to compensate for cache updates.
- Do not copy reference mockup styling into the app.
- Prefer existing web CSS classes, layout patterns, and dialog conventions.
- Keep the first implementation limited to cancelling one order at a time.
- Treat cancellation as hard deletion of the order record for this MVP.
- Keep `/events/orders` available for the existing event-management boundary,
  but do not call it from the customer "My orders" UI.

## Acceptance Criteria

1. "My orders" shows a `Cancel order` action for each listed customer order.
2. The page does not show a manual refresh button.
3. Cancelling requires explicit confirmation.
4. Confirming cancellation calls `DELETE /orders/mine`.
5. The request contains only the selected order id, not customer identity.
6. Backend ownership checks prevent cancelling another user's order.
7. Successful cancellation removes the order from "My orders".
8. Successful cancellation makes the event place available again.
9. Failed cancellation keeps the order visible.
10. Failed cancellation shows an accessible error.
11. Existing booking, event browsing, and pagination behavior keep working.
