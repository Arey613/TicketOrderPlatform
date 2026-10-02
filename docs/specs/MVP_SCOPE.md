# MVP Scope

This note tracks what is already implemented versus what is still missing from
an end-to-end MVP perspective. It intentionally distinguishes backend/API
capability from web UI capability, because some operations already exist in the
contract and Java API but are not exposed as complete user workflows in the web
app.

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

### Event management API

- [x] Managers and admins can create events.
- [x] Created events start as `DRAFT`.
- [x] Event owners can update draft events.
- [x] Event owners can patch draft events through `PATCH /events/{eventId}`.
- [x] Event owners can publish draft events.
- [x] Event owners can unpublish published events back to draft.
- [x] Published events can be listed and read publicly.
- [x] Owned events can be listed through the authenticated event list using
  `scope=MINE`.
- [x] Event details expose availability data and booked places.
- [x] Event image upload is supported through `POST /events/{eventId}/image`.
- [x] Event video upload is supported through the presigned upload URL and
  confirmation endpoints.

### Event ordering API

- [x] Customers can create event orders for published events.
- [x] Event-order creation validates duplicate seats inside the request and
  against existing reservations.
- [x] Customers can list their own event orders through `GET /orders/mine`.
- [x] Customers can delete only their own event orders.
- [x] Current-customer order listing is paginated and uses a dedicated
  `MyOrdersResponse`/`MyOrderResponse` contract model.

### Web app

- [x] Anonymous users can browse published events.
- [x] Users can log in and register from the auth panel.
- [x] Customers can inspect an event seat grid and book one selected place.
- [x] Customers can open a dedicated "My orders" page at `/orders/mine`.
- [x] Customer navigation links "My orders" to the routed page.
- [x] Managers and admins can create draft events from the web app.
- [x] Managers and admins can open "My events" at `/events/mine`.
- [x] Managers and admins can edit owned draft events.
- [x] Managers and admins can publish and unpublish owned events.
- [x] Event creation and edit flows can attach event images and videos.

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
- [x] Read-side list queries use the read-replica path where configured, with
  primary fallback for replica failures.
- [x] Object storage integration exists for event media.

## Missing Or Partial Functional Areas

### Customer order management

- [ ] Add cancellation/deletion controls to "My orders".
- [ ] Add a web client wrapper/mutation for the existing
  `DELETE /events/orders` API operation.
- [ ] Refresh "My orders" and event availability after order deletion.
- [ ] Decide whether order deletion should be called "cancel order" in the UI.

### Event media polish

- [ ] Decide whether media replacement/deletion is needed for MVP.
- [ ] Add any missing user-facing progress states for large video uploads.
- [ ] Define production storage lifecycle rules for failed or abandoned uploads.

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

- [ ] Build customer order cancellation first.
- [ ] Add a cancel/delete action to "My orders".
- [ ] Wire the action to the existing owned-order delete API.
- [ ] Refresh "My orders" and event availability after cancellation.
- [ ] Add unit and browser coverage for the cancellation flow.

This is the highest-value next slice because the backend already owns and
authorizes order deletion, while the customer UI currently has no complete
cancellation workflow.

## Later Slices

- [ ] Define and implement the real ticket domain.
- [ ] Add admin user/operations pages.
- [ ] Fix duplicate-email registration to return the documented conflict
  response.
- [ ] Polish event media lifecycle behavior after the core MVP workflows settle.
- [ ] Keep navigation aligned with implemented screens.
