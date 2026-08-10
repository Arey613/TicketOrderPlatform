# TICKET_SERVICE#6 - Future Considerations

These notes are intentionally out of scope for the current event-management ticket. Keep them as design input for later iterations.

## Seat Map Behavior

The current ticket ignores `t_event_seat`. A future iteration should decide whether seat-map storage and behavior should be added.

Open questions:

- Whether `t_event_seat` should be created for every seated event.
- Whether event ordering should reserve by `seat_id`.
- Whether seat rows need statuses such as `AVAILABLE`, `BLOCKED`, or `RESERVED`.
- Whether reserved status should be stored on seats or derived from `t_event_order`.
- Whether the UI needs a dedicated seat-map endpoint.

## Customer Ownership

The current ticket uses `customer_reference` only as a placeholder.

A future viewer/customer permission model should decide:

- Whether `t_event_order` should reference `t_users(id)` through `customer_id`.
- Whether `customer_reference` remains as a snapshot or is replaced.
- How customers view their own orders.
- Which roles can view customer-level order details.

## Multiple Managers

The current ticket has a flat event-owner model with one `owner_id`.

A future manager model may need:

- Multiple managers for the same event or place.
- A separate manager assignment table.
- Different manager permissions for editing, publishing, and viewing orders.

## Standing Places

Standing/general-admission behavior is intentionally ignored for now.

A future iteration should decide:

- Whether standing events need quantity-based orders.
- Whether `number_of_rows` and `seats_per_row` should use `9999` as a sentinel.
- Whether standing reservations should skip row/place uniqueness.
- How capacity should be protected from overselling under concurrent orders.

## Indexes And Performance

The current ticket keeps indexes minimal.

A future iteration should revisit indexes after access patterns are clearer:

- Event browsing by status and date.
- Manager-owned event listing.
- Order lookup by event.
- Seat-map lookup by event.
- Customer order history once customer ownership exists.
