# BUGFIX#2 - Auth Schema and Role-Aware UI Corrections

## Goal

Correct authentication-related database boundaries and frontend authenticated-state behavior found during review.

This fix must ensure:

- The application has two explicitly configured datasources.
- The primary datasource stores operational data before the event occurrence.
- Analytical database objects live in the analytical schema, not `public`.
- Password hashes are stored only in the transactional database.
- Login and registration actions are hidden after successful authentication.
- Authenticated navigation and visible actions match the user's role.

## Context

The platform currently has separate transactional and analytical migration streams:

```text
ticket_transactional
ticket_analytical
```

Authentication writes and login validation are operational behavior. They must use the writable transactional user table because password hashes are required for authentication and must never be exposed through analytical read models.

The Java API currently behaves as an operational service first. Before an event occurs, the system must keep the source-of-truth records in the primary transactional datasource: users, authentication state references, events, event details, and event orders. Analytical access is separate and must not become the write path for operational behavior.

The web app currently stores the authenticated user returned by login or registration and shows the current email and role in the header. The home page still receives login and registration callbacks independently from authentication state, so unauthenticated-only actions can remain visible after login.

## Scope

This bugfix includes:

- Adding or correcting application datasource configuration so the API has two datasource definitions.
- Marking the operational datasource as primary.
- Routing pre-event operational writes and authentication reads through the primary datasource.
- Keeping analytical reads on the analytical datasource.
- Verifying every analytical view is created under `ticket_analytical`.
- Removing or preventing any analytical view definition under `public`.
- Ensuring `password_hash` exists only on `ticket_transactional.t_users`.
- Ensuring analytical user views do not expose `password_hash`.
- Ensuring Java authentication and registration persistence use `ticket_transactional.t_users`.
- Hiding login and register buttons from authenticated users.
- Displaying role-appropriate navigation and primary actions for `ADMIN`, `MANAGER`, and regular user accounts.
- Adding focused tests for database schema boundaries and authenticated UI state.

This bugfix does not include:

- Adding new roles beyond the roles already modeled by the backend contract and domain.
- Moving operational writes to the analytical datasource.
- Designing post-event archival, warehouse loading, or ETL jobs.
- Implementing a full admin console.
- Implementing a full manager event-management UI.
- Implementing a full user order dashboard if the backing API workflow is not available yet.
- Changing login, logout, registration, or event-management API contracts unless a mismatch is discovered.

## Datasource Rules

The Java API must define two datasource concepts:

```text
primary datasource -> operational transactional database access
analytical datasource -> read-side analytical database access
```

The primary datasource must be the default datasource used by existing transactional repositories unless a repository explicitly opts into another datasource.

The primary datasource stores all source-of-truth records before an event occurrence, including:

- users
- password hashes
- event definitions
- event details
- event publication status
- event orders and reservations

The analytical datasource is for read-side projections and reporting-oriented access. It must not be used for login, registration, password matching, event creation, event updates, event publishing, or order reservation writes.

Configuration must make the two datasources explicit. Environment variables should remain simple for local development, but names must distinguish primary and analytical settings once both are present.

Recommended naming:

```text
SPRING_DATASOURCE_PRIMARY_URL
SPRING_DATASOURCE_PRIMARY_USERNAME
SPRING_DATASOURCE_PRIMARY_PASSWORD

SPRING_DATASOURCE_ANALYTICAL_URL
SPRING_DATASOURCE_ANALYTICAL_USERNAME
SPRING_DATASOURCE_ANALYTICAL_PASSWORD
```

If both datasources point to the same PostgreSQL database in local development, schema qualification must still keep the boundaries clear:

```text
primary datasource writes ticket_transactional
analytical datasource reads ticket_analytical
```

## Database Rules

### Analytical Schema

All analytical objects must be created with explicit `ticket_analytical` qualification.

Valid example:

```sql
CREATE OR REPLACE VIEW ticket_analytical.users AS
SELECT ...
```

Invalid example:

```sql
CREATE OR REPLACE VIEW users AS
SELECT ...
```

The implementation must not rely on the database search path to place analytical views.

### Password Storage

`password_hash` must be stored in:

```text
ticket_transactional.t_users.password_hash
```

No analytical table, view, materialized view, fixture, exported model, frontend model, or API response may expose `password_hash`.

The analytical user view may include:

- `id`
- `email`
- `role`
- `enabled`
- `created_at`
- `updated_at`

The analytical user view must not include:

- `password_hash`
- raw `password`
- session identifiers
- CSRF tokens
- authentication cookies

## Backend Rules

Authentication and registration must continue to use the transactional persistence model:

```text
UserEntity -> ticket_transactional.t_users
```

The backend must not map login, password matching, registration, or user persistence to analytical views. Analytical views are read-side objects and are not a write or authentication boundary.

The login and registration responses must return only password-free user summaries.

## Frontend Rules

### Authenticated State

After successful login or registration:

1. Store the authenticated user in the existing auth storage mechanism.
2. Close the auth panel.
3. Hide public login and register calls to action.
4. Show authenticated account context.
5. Show only actions that are meaningful for the authenticated user's role.

After logout:

1. Clear the stored authenticated user.
2. Return to unauthenticated navigation.
3. Show public login and register calls to action again.

### Role-Aware Display

The UI must derive visible authenticated actions from the authenticated user's role.

`ADMIN` users should see admin-oriented actions, for example:

- User administration
- Platform operations
- Event oversight

`MANAGER` users should see manager-oriented actions, for example:

- My events
- Create event
- Event orders

Regular user accounts should see customer-oriented actions, for example:

- Browse events
- My orders
- My tickets

If a target workflow is not implemented yet, the UI may show the role-specific entry point as disabled, unavailable, or placeholder navigation. It must not show actions for another role as if they are available.

### Public Actions

The following actions are unauthenticated-only:

- Login
- Register
- Create account

These actions must not be displayed in the header, hero section, or auth-related home page calls to action while `currentUser` is present.

## Test Requirements

Database tests or migration validation must prove:

1. Analytical repeatable migrations create views under `ticket_analytical`.
2. No analytical migration creates objects in `public`.
3. `ticket_analytical.users` does not expose `password_hash`.
4. `ticket_transactional.t_users` contains `password_hash`.
5. Local database setup can expose separate primary and analytical datasource configuration.

Backend tests must prove:

1. Login reads the password hash from the transactional user table.
2. Registration writes the password hash to the transactional user table.
3. Login and registration responses do not contain raw password or password hash fields.
4. Existing operational repositories use the primary datasource by default.
5. Analytical datasource access is not used for authentication or operational writes.

Frontend tests must prove:

1. Unauthenticated users can see login and registration actions.
2. After login, login and registration actions are not visible.
3. After registration, login and registration actions are not visible.
4. After logout, login and registration actions are visible again.
5. `ADMIN`, `MANAGER`, and regular user roles see different role-specific action sets.

## Acceptance Criteria

1. Analytical schema objects are explicitly created in `ticket_analytical`.
2. The API has explicit primary and analytical datasource configuration.
3. Pre-event operational data is stored through the primary datasource.
4. No password hash is exposed outside `ticket_transactional.t_users`.
5. Backend authentication and registration use transactional persistence only.
6. Authenticated users do not see login or register buttons.
7. Role-specific UI actions differ for admin, manager, and regular user roles.
8. The fix is covered by focused migration/backend/frontend tests.
