# TICKET_SERVICE#12 - Admin User Management

## Goal

Give authenticated `ADMIN` users a backend API for inspecting platform users and managing two fields on existing customer and manager user accounts.

The admin flow must let an admin inspect users, find a specific user, change a `CUSTOMER` or `MANAGER` user's role, and enable or disable a `CUSTOMER` or `MANAGER` user account. For this ticket, the complete editable state contains only `role` and `enabled`; the backend validation can be extended later if the role model or admin-management rules grow.

## Context

The Ticket Service API already has user persistence, session authentication, and role loading through Spring Security authorities. The current role model includes:

```text
CUSTOMER
MANAGER
ADMIN
```

The current public and authenticated user API is intentionally small. It supports user registration and `GET /users/{userId}`. There is no dedicated admin user-management surface yet.

For this ticket, admin accounts are visible in admin read endpoints but are not editable. `ADMIN` users are actors in this workflow, not mutation targets. Admin account lifecycle and admin-to-admin management should be designed separately.

Admin user management must use the existing transactional user source of truth:

```text
ticket_transactional.t_users
```

Password hashes remain internal authentication data. Admin endpoints must not expose, accept, reset, or modify passwords, `password_hash`, raw passwords, session identifiers, CSRF tokens, or authentication cookies.

## Scope

This feature includes:

- Admin-only listing of users.
- Admin-only detail lookup for users.
- Admin-only role updates for manageable users.
- Admin-only enabled/disabled status updates for manageable users.
- Filtering users by role and enabled status.
- Text search by email.
- Page-based pagination using the shared pagination model from `TICKET_SERVICE#9`.
- Sorting user lists by supported user fields.
- OpenAPI contract updates for the admin endpoints.
- Reusing existing contract schemas where the response shape is the same.
- A dedicated admin user update request schema in the OpenAPI YAML.
- Java API implementation behind application ports.
- Persistence updates against the transactional user table.
- Backend service validation of which fields are allowed to change.
- Revoking active sessions when a user is disabled.
- Optimistic locking for admin updates when the current persistence stack can support it cleanly.
- Integration tests for authorization, listing, target-scope, and mutation behavior.

This feature does not include:

- Frontend admin screens.
- Password reset.
- Password change.
- Password viewing.
- Password-hash viewing.
- User deletion.
- Bulk user actions.
- User creation by admins.
- Editing `ADMIN` accounts.
- Changing any user to `ADMIN`.
- Changing an `ADMIN` account to another role.
- Audit-log persistence.
- Email notification when a role or enabled status changes.
- General session management for other users, except the disable-driven session revocation required by this ticket.
- Changing the login, logout, or registration contract.
- Exposing password hashes or other authentication secrets.
- Introducing a new structured error response body.

## Domain Rules

A user has:

- `id`
- `email`
- `role`
- `enabled`
- `createdAt`
- `updatedAt`

Rules:

1. Only authenticated users with role `ADMIN` can use the admin user-management endpoints.
2. Admin responses must use password-free user summaries.
3. Admin read endpoints may return users whose role is `CUSTOMER`, `MANAGER`, or `ADMIN`.
4. Manageable users are users whose current role is `CUSTOMER` or `MANAGER`.
5. Users whose current role is `ADMIN` are visible but not editable in this ticket.
6. A manageable user's new role must be accepted only when backend service validation allows it.
7. For this ticket, admin endpoints must not assign the `ADMIN` role.
8. Changing a user's role replaces the previous role.
9. Changing a user's enabled status replaces the previous status.
10. Disabling a user prevents that user from authenticating in future login attempts.
11. Disabling a user must revoke or invalidate that user's active authenticated sessions so the disabled user cannot continue using an existing session.
12. Every successful `PUT /admin/manage/users/{userId}` must set `updatedAt` to the current application time, even when the submitted `role` and `enabled` values equal the existing values.
13. Updating a user must not change `id`, `email`, `password_hash`, password credentials, or `createdAt`.

## API Contract

Add admin user endpoints under:

```text
/admin/manage/users
```

These endpoints are separate from the existing `/users/{userId}` path so admin capabilities are easy to protect and audit. The `manage` segment makes the route explicit that this is an administrative management surface, not the regular user profile API.

### List Users

```http
GET /admin/manage/users
```

Query parameters:

- `page` - zero-based page number, default `0`.
- `size` - page size, using the same limits as the shared pagination model.
- `role` - optional user role filter, using roles supported by the current backend role model.
- `enabled` - optional boolean enabled-status filter.
- `search` - optional case-insensitive email search text.
- `sort` - optional single sort expression, for example `email,asc` or `createdAt,desc`.

Behavior:

1. Require `ADMIN`.
2. Return a paginated list of password-free user summaries.
3. Sort users by the requested supported sort field and direction.
4. Apply filters before pagination.
5. Match `search` against email only.
6. Include `ADMIN` users in read results so admins can inspect platform accounts.
7. Default sort is `createdAt,desc`.
8. Always apply `id,asc` as a final tie-breaker for stable pagination.
9. Reject unsupported sort fields with `400 Bad Request`.
10. Accept only one client-provided sort expression in this ticket. Multiple client-provided sort expressions must return `400 Bad Request`.

Initial supported sort fields:

- `createdAt`
- `updatedAt`
- `email`
- `role`
- `enabled`

Successful response:

```text
200 OK
```

### Get Admin User Detail

```http
GET /admin/manage/users/{userId}
```

Behavior:

1. Require `ADMIN`.
2. Return the password-free user summary for the requested user.
3. Return `404 Not Found` when the user does not exist.

Successful response:

```text
200 OK
```

### Update User

```http
PUT /admin/manage/users/{userId}
Content-Type: application/json
```

Request body:

```json
{
  "role": "MANAGER",
  "enabled": true
}
```

Rules:

1. Require `ADMIN`.
2. Accept a complete editable-state payload for this feature.
3. `role` is required.
4. `enabled` is required.
5. Missing or null `role` or `enabled` must return `400 Bad Request`.
6. `ADMIN` must not be accepted as an update role in this ticket.
7. Return the updated password-free user summary.
8. Return `403 Forbidden` when the target user is an `ADMIN`, because admin accounts are visible but not editable through this endpoint.

This `PUT` replaces the user's admin-manageable state for this endpoint. It does not replace the whole user entity: immutable fields, identity fields, timestamps except `updatedAt`, credentials, and authentication/session data remain outside this operation.

Successful response:

```text
200 OK
```

## Response Rules

```text
200 OK
The admin request succeeded and returns a password-free user summary or page.

400 Bad Request
The request body or query parameters are malformed, required update fields are missing or null, an unsupported sort field is requested, multiple sort expressions are requested, an update payload contains an unsupported field, or basic request parsing fails.

401 Unauthorized
The caller is not authenticated.

403 Forbidden
The caller is authenticated but is not an ADMIN, or the authenticated admin requested a field/value change that is not allowed by this endpoint.

404 Not Found
The target user does not exist.
```

Returning `403 Forbidden` for target `ADMIN` mutations keeps admin accounts visible for inspection while making them non-editable through this feature.

This ticket defines status-code behavior only. A dedicated structured error response body is out of scope and should be handled by a separate future ticket.

## Authorization Rules

All admin user-management endpoints must be restricted with method-level authorization:

```java
@PreAuthorize("hasRole('ADMIN')")
```

HTTP security configuration must still require authentication by default. Controller-level or method-level checks are the source of truth for role-specific authorization.

Non-admin authenticated users must not reach application mutation behavior for these endpoints.

## Persistence

This ticket does not require a database schema migration. It uses the existing transactional users table:

```text
ticket_transactional.t_users
```

Required persistence capabilities:

- List users with optional role, enabled, and email-search filters.
- Sort users by supported sort fields.
- Count users for pagination metadata.
- Load a user by id for admin detail and mutation.
- Update a user's role and enabled status.
- Store the admin update time in `updatedAt` using the current application time.
- Apply optimistic locking to admin updates when the current user persistence mapping can support it without a broad schema redesign.

List and detail persistence may return target users whose role is `ADMIN`. Mutation persistence must not update target users whose role is `ADMIN`.

Command validation and mutation reads must use the primary transactional datasource. User listing is display-oriented and may use the read-replica path if the repository pattern already supports it, but the implementation must not use replica reads for mutation preconditions.

## Backend Design

Add application-level ports instead of putting admin behavior directly in the controller.

Suggested query use case:

```java
PageResult<User> listUsers(AdminUserListQuery query);

User getAdminUser(UUID userId);
```

Suggested command use case:

```java
User updateUser(UUID actorUserId, UUID targetUserId, UpdateUserCommand command);
```

The command service must:

1. Load the actor user.
2. Verify the actor is still an enabled admin.
3. Load the target user.
4. Return not found when the target user does not exist.
5. Throw the project's not-allowed exception when the target user currently has role `ADMIN`.
6. Validate that a requested new role is allowed by the current backend business rule.
7. Apply the role and enabled changes.
8. Persist the updated user.
9. Set `updatedAt` to the current application time for every successful update.
10. If `enabled` changes from `true` to `false`, revoke or invalidate the target user's active sessions.
11. Return the updated password-free user summary to the adapter.

The controller must obtain the current authenticated user through the existing current-user mechanism instead of trusting a user id from the request body.

## Backend Update Validation

The OpenAPI contract may reuse shared user schemas where the shape is the same, but the backend service is responsible for validating the actual requested changes before persistence.

The complete editable state for this feature contains exactly these user fields:

- `role`
- `enabled`

The service must reject any update command that attempts to change immutable, credential, or out-of-scope fields, including:

- `id`
- `email`
- `password`
- `passwordHash`
- `password_hash`
- `createdAt`
- `updatedAt`
- session identifiers
- CSRF tokens
- authentication cookies

The OpenAPI YAML documents the required update payload fields for request-shape validation. If the update payload is missing `role` or `enabled`, contains null for either field, or contains an odd, unknown, immutable, credential, or out-of-scope field, the request must fail with `400 Bad Request` before persistence.

The backend service remains the business-rule authority for whether a well-formed requested field/value change is allowed. If backend update validation detects a well-formed requested value that this endpoint is not allowed to apply, the service must throw the project's not-allowed exception. The HTTP adapter must map that exception to `403 Forbidden`.

Role values are contract/domain values, not a duplicated admin-screen enum. Read filters should accept roles supported by the current backend role model. Mutation validation remains in the backend service and database constraints, so future role additions can be supported by updating business rules instead of redesigning the endpoint.

For this ticket, the backend service must reject `ADMIN` as a requested target role for this endpoint by throwing the project's not-allowed exception. If the role list changes later, the backend service and database constraints must handle any consistency rules for the new roles. Only role changes allowed by the current admin-management business rule may be persisted.

## Session Revocation

When an admin changes a manageable user's `enabled` value from `true` to `false`, the backend must revoke or invalidate active authenticated sessions for the target user.

The implementation should use the existing session mechanism if it supports user-session lookup or invalidation. If the current session implementation cannot invalidate sessions by user without a larger redesign, this ticket must add the smallest service-owned session invalidation capability needed for disabled users and document any remaining limitations in code comments and tests.

Re-enabling a user does not recreate sessions. The user must authenticate again.

## Optimistic Locking

Admin updates should use optimistic locking when the current persistence stack can support it cleanly.

Preferred behavior:

1. The update command verifies that the row being updated has not changed since it was loaded for the update operation.
2. Concurrent conflicting admin updates fail instead of silently overwriting each other.
3. The HTTP response for an optimistic locking conflict should be `409 Conflict` if the project already has a conflict mapping; otherwise document the current fallback behavior in the implementation notes.

This ticket should not introduce a broad schema redesign only for optimistic locking. If the existing user table or entity mapping lacks a version column and adding one is outside the intended scope, use the safest lightweight option available, such as an `updatedAt` compare-and-update condition, and leave a follow-up note for a dedicated version column if needed.

## OpenAPI Schema Changes

Reuse schemas for:

- `UserResponse` for single-user responses when the existing schema remains password-free.
- The shared paginated response pattern from `TICKET_SERVICE#9` for user list responses.

Add schemas only when the existing reusable shape is not sufficient, for example:

- `UpdateUserRequest`.

`UpdateUserRequest` fields:

- `role` - required `UserRole`; backend validation restricts accepted values to the current admin-management business rule.
- `enabled` - required boolean.

Do not add `password`, `passwordHash`, raw password, password reset, or session-related fields to any admin user schema.

`UpdateUserRequest` must be defined as a dedicated YAML schema for this endpoint instead of reusing a full user response object as an update payload. This keeps read-only fields out of the update request while still allowing the response schema to be shared. The request is complete for the editable state in this ticket: both `role` and `enabled` must be present.

## Test Requirement

Use a TDD approach for implementation: write the relevant tests first, verify they fail for the missing behavior, then implement production code.

Backend tests must verify:

1. Anonymous callers receive `401` for admin endpoints.
2. `CUSTOMER` callers receive `403` for admin endpoints.
3. `MANAGER` callers receive `403` for admin endpoints.
4. `ADMIN` callers can list users.
5. User listing supports pagination metadata.
6. User listing can filter by role.
7. User listing can filter by enabled status.
8. User listing can search by email.
9. User listing includes `ADMIN` accounts.
10. User listing supports sorting by supported fields.
11. Unsupported sort fields return `400`.
12. Multiple client-provided sort expressions return `400`.
13. Missing users return `404`.
14. `ADMIN` callers can get any user by id.
15. `ADMIN` detail lookup can return target `ADMIN` accounts.
16. `ADMIN` callers can change a manageable user's role according to the current backend business rule.
17. `ADMIN` callers cannot assign the `ADMIN` role in this ticket.
18. `ADMIN` callers can enable a manageable user.
19. `ADMIN` callers can disable a manageable user.
20. `ADMIN` update returns `403` for target `ADMIN` accounts.
21. Update payloads missing `role` or `enabled` return `400`.
22. Update payloads with null `role` or `enabled` return `400`.
23. Update payloads with unsupported fields return `400`.
24. Responses never include password hashes, raw passwords, or session data.
25. Every successful `PUT` sets `updatedAt` to the current application time.
26. Disabling a user revokes or invalidates that user's active sessions.
27. Mutations use optimistic locking or a documented lightweight concurrency guard where feasible.
28. Mutations do not modify password fields.

Contract validation must verify the OpenAPI bundle after the contract changes are added.

## Acceptance Criteria

1. The OpenAPI contract documents `GET /admin/manage/users`, `GET /admin/manage/users/{userId}`, and `PUT /admin/manage/users/{userId}`.
2. The Java API enforces `ADMIN` authorization for every admin user-management endpoint.
3. Admins can inspect users with pagination, sorting, and filters, including `ADMIN` accounts.
4. Admins can update role and enabled status for `CUSTOMER` and `MANAGER` users.
5. The API exposes `ADMIN` accounts through read endpoints but does not mutate them through this feature.
6. Passwords, password hashes, and authentication secrets are never returned or modified by admin endpoints.
7. Focused backend tests cover success paths, authorization failures, validation failures, and target-scope guardrails.

## Implementation Plan

1. Update the OpenAPI source under `contracts/openapi/ticket-order-api` with admin user paths and schemas.
2. Validate the OpenAPI bundle.
3. Generate Java API contract code.
4. Add admin user query and command application ports.
5. Add persistence capabilities for filtered user listing and manageable-user role/status updates.
6. Add the admin user controller methods with `@PreAuthorize("hasRole('ADMIN')")`.
7. Add integration and unit tests for the required behavior.
8. Run the smallest relevant backend and contract validation targets.
9. Update root and module changelogs when implementation work lands.
