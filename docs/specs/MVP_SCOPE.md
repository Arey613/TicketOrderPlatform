# MVP Scope

This note tracks what is already implemented versus what is still missing from an
end-to-end product perspective. Use the checkboxes to keep future implementation
work visible.

## Current Functional Surface

### Authentication and users

- [x] Users can register.
- [x] Users can log in and log out using server-side session authentication.
- [x] Password hashing happens at the HTTP boundary before data enters the
  application layer.
- [x] Authenticated users have one of these roles: `ADMIN`, `MANAGER`,
  `CUSTOMER`.
- [x] The web app stores/restores the authenticated user locally and clears it
  after logout or session expiry.

### Event management backend

- [x] Managers and admins can create events.
- [x] Created events start as `DRAFT`.
- [x] Event owners can update draft events.
- [x] Event owners can publish draft events.
- [x] Event owners can unpublish published events back to draft.
- [x] Published events can be listed and read publicly.
- [x] Owned events can be listed through the authenticated event list using
  `scope=MINE`.

### Event ordering

- [x] Customers can create event orders for published events.
- [x] Event-order creation validates duplicate seats inside the request and
  against existing reservations.
- [x] Customers can list their own event orders.
- [x] Customers can delete only their own event orders.
- [x] Event details expose availability data and booked places.

### Web app

- [x] Anonymous users can browse published events.
- [x] Users can log in and register from the auth panel.
- [x] Customers can inspect an event seat grid and book one selected place.
- [x] Customers can view their event orders.
- [x] Managers and admins can create an event from the web app.

### Platform and infrastructure

- [x] The API follows a hexagonal structure with web adapters, application
  ports, services, domain models, and persistence adapters.
- [x] The OpenAPI contract is the source of truth for generated Java/web
  contract types.
- [x] PostgreSQL persistence is split between transactional and analytical
  schemas.
- [x] Flyway migrations exist for both schemas.
- [x] Observability, CSRF handling, CORS, login rate limiting, actuator access
  rules, and sensitive logging protections exist.

## Missing Or Partial Functional Areas

### Manager event management

- [ ] Add a "My events" page for `MANAGER` and `ADMIN`.
- [ ] List owned events using the existing authenticated event list with
  `scope=MINE`.
- [ ] Add a draft-event edit route.
- [ ] Add a draft-event edit form.
- [ ] Add a web client wrapper/mutation for `PATCH /events/{eventId}`.
- [ ] Restrict edit actions in the UI to draft events.
- [ ] Add publish controls for draft events.
- [ ] Add unpublish controls for published events.
- [ ] Refresh owned-event list and event details after edit, publish, or
  unpublish actions.

### Customer order management

- [ ] Add cancellation/deletion controls to "My orders".
- [ ] Add a web client wrapper/mutation for `DELETE /events/orders`.
- [ ] Refresh "My orders" and event availability after order deletion.
- [ ] Decide whether order deletion should be called "cancel order" in the UI.

### Ticket domain

- [ ] Define the real ticket domain separately from event orders/reservations.
- [ ] Add ticket entity/model.
- [ ] Add ticket issuance rules.
- [ ] Add ticket status.
- [ ] Add ticket lookup for the authenticated customer.
- [ ] Add a real "My tickets" screen.
- [ ] Replace current ticket wording that is only aspirational.

### Navigation cleanup

- [ ] Remove or implement anonymous `Prices` navigation.
- [ ] Remove or implement anonymous `Help` navigation.
- [ ] Remove or implement customer `My tickets` navigation.
- [ ] Remove or implement admin `Users` navigation.
- [ ] Remove or implement admin `Operations` navigation.
- [ ] Revisit navigation after each new screen is implemented so the UI only
  links to real workflows.

### Admin and operations

- [ ] Add admin user-management screen.
- [ ] Add role-management flow.
- [ ] Add enable/disable user flow.
- [ ] Add operations dashboard.
- [ ] Define which admin actions belong in the MVP and which belong after MVP.

### Backend correctness

- [ ] Fix duplicate-email registration so it returns the documented conflict
  response instead of an unhandled `500`.
- [ ] Review bulk order lookup strategy if delete batches grow beyond small
  bounded lists.

## Recommended Next Implementation Slice

- [ ] Build manager event management first.
- [ ] Implement "My events" page.
- [ ] Implement draft-event edit flow.
- [ ] Implement publish/unpublish actions.
- [ ] Remove or replace dead navigation labels touched by the flow.

This is the highest-value next slice because it completes the existing backend
event lifecycle in the UI without introducing a new domain concept.

## Later Slices

- [ ] Add customer order cancellation from "My orders".
- [ ] Define and implement the real ticket domain.
- [ ] Add admin user/operations pages.
- [ ] Fix duplicate-email registration to return the documented conflict
  response.
- [ ] Keep navigation aligned with implemented screens.
