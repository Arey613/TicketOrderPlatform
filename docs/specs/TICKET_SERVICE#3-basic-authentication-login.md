# TICKET_SERVICE#3 - Basic Authentication Login

## Goal

Enable HTTP Basic Authentication in the Java Ticket Order API. Clients authenticate by sending a login and password pair. For the first implementation, the login is the user's email address.

## Context

The Java service already has a user model backed by persistence. Authentication must use the existing user data instead of introducing a separate login store.

Current user fields expected by this feature:

- `id`
- `email`
- `password_hash`
- `role`
- `enabled`
- `created_at`
- `updated_at`

For this feature:

```text
login = email
```

The persistence model uses `ticket_transactional.t_users` as the transactional base table. Query-oriented database views may expose the user concept as `users`, but authentication and user persistence must read from and write to the writable base table because they need `password_hash` and must support user inserts. This spec does not introduce a dedicated `login` column.

## JPA Mapping Decision

Use one JPA user entity for this feature:

```text
UserEntity -> ticket_transactional.t_users
```

This entity supports authentication lookup and user persistence. It includes `password_hash` because Spring Security needs the encoded password during Basic Authentication.

Do not map the authentication/persistence entity to the `users` view. Even if the view is simple enough for PostgreSQL to accept some writes, the service should not rely on view write-through behavior for core user persistence.

Do not add a second Java user entity for the `users` view in this feature. A future CQRS read model may introduce a separate read-only entity or projection, for example:

```text
UserReadEntity -> ticket_transactional.users
```

That read-side type should be added only when there is a real query use case that benefits from the view and should not include `password_hash`.

## Scope

This feature includes:

- HTTP Basic Authentication support.
- Login/password authentication.
- Using email as the login value.
- Validating the submitted password against `password_hash`.
- Rejecting disabled users.
- Loading user roles into Spring Security.
- Protecting API endpoints by default.
- Replacing the `/hello` smoke endpoint with Spring Boot Actuator health.
- Keeping `GET /actuator/health` public.
- Regular `@SpringBootTest` integration tests.

This feature does not include:

- JWT authentication.
- Refresh tokens.
- Session-based login.
- Frontend login form.
- User registration.
- Password reset.
- Dedicated `login` column.
- Role-restricted authorization beyond loading the user's role into the security context.

## Authentication Contract

Clients authenticate with the standard Basic Auth header:

```http
Authorization: Basic base64(login:password)
```

Because `login = email`, decoded credentials should look like:

```text
user@example.com:secret
```

Example request:

```http
GET /some-protected-endpoint
Authorization: Basic dXNlckBleGFtcGxlLmNvbTpzZWNyZXQ=
```

## Behavior

When a request contains Basic Auth credentials, the service must:

1. Read the Basic Auth username value as `login`.
2. Treat `login` as the user email.
3. Load the user by email.
4. Compare the submitted password with the stored `password_hash`.
5. Reject authentication if the user does not exist.
6. Reject authentication if the password is invalid.
7. Reject authentication if the user is disabled.
8. On success, expose the authenticated principal through Spring Security.
9. On success, expose the user role as a Spring Security authority.

## Password Handling

Stored passwords should use Spring Security's encoded password format.

Preferred format:

```text
{bcrypt}<encoded-password>
```

Tests may use:

```text
{noop}secret
```

The implementation should support Spring Security's delegating password encoder so existing and future encoding formats can be handled consistently.

Recommended encoder:

```java
PasswordEncoderFactories.createDelegatingPasswordEncoder()
```

## Authorization Rules

Public endpoints:

- `GET /actuator/health`

Authenticated endpoints:

- All business API endpoints require authentication by default.

The `/hello` endpoint must no longer be the public smoke endpoint. It should be removed or stop being part of the public API contract as part of the implementation.

## Response Rules

```text
200 OK
The request succeeds when valid credentials are provided and the endpoint permits the user.

401 Unauthorized
Returned when credentials are missing, login/email is unknown, password is invalid, or the user is disabled.

403 Forbidden
Reserved for future role-based authorization when an authenticated user lacks permission.
```

## Integration Test Requirement

Use regular Spring Boot integration tests.

Suggested test style:

```java
@SpringBootTest
@AutoConfigureMockMvc
class BasicAuthenticationIntegrationTest {
}
```

The test suite should verify:

1. `GET /actuator/health` works without authentication.
2. A protected endpoint returns `401 Unauthorized` when no credentials are provided.
3. A protected endpoint returns `401 Unauthorized` when the login/email does not exist.
4. A protected endpoint returns `401 Unauthorized` when the password is wrong.
5. A protected endpoint returns `401 Unauthorized` when the user is disabled.
6. A protected endpoint succeeds when login/email and password are valid.
7. The authenticated principal contains the expected email.
8. The authenticated authorities contain the expected user role.

## Implementation Notes

The likely implementation should:

- Add Spring Boot Actuator if it is not already present.
- Expose the health endpoint.
- Update `SecurityConfig` to enable HTTP Basic Authentication.
- Permit unauthenticated access to `GET /actuator/health`.
- Require authentication by default for all other endpoints.
- Keep CSRF disabled for stateless API behavior.
- Add or expose a `PasswordEncoder` bean.
- Keep `DomainUserDetailsService` as the Spring Security adapter.
- Continue loading users through the application/user repository port.
- Avoid adding a new `login` column for now.
- Remove `/hello` from the public API contract and tests.

## Done Criteria

This feature is done when:

- Basic Auth is enabled.
- Email is accepted as the login value.
- Valid enabled users can authenticate.
- Invalid credentials return `401 Unauthorized`.
- Disabled users return `401 Unauthorized`.
- `GET /actuator/health` is public.
- Protected endpoints require authentication.
- `/hello` is no longer used as the health/smoke endpoint.
- `@SpringBootTest` integration coverage proves the behavior.
- `make test-api` passes.
