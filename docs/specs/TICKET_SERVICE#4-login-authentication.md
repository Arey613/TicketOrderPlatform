# TICKET_SERVICE#4 - Login Authentication

## Goal

Enable login/password authentication in the Java Ticket Order API. Clients authenticate by sending a login and password pair. For the first implementation, the login is the user's email address.

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

This entity supports authentication lookup and user persistence. It includes `password_hash` because Spring Security needs the encoded password during login.

Do not map the authentication/persistence entity to the `users` view. Even if the view is simple enough for PostgreSQL to accept some writes, the service should not rely on view write-through behavior for core user persistence.

Do not add a second Java user entity for the `users` view in this feature. A future CQRS read model may introduce a separate read-only entity or projection, for example:

```text
UserReadEntity -> ticket_transactional.users
```

That read-side type should be added only when there is a real query use case that benefits from the view and should not include `password_hash`.

## Scope

This feature includes:

- Login/password authentication support.
- Using email as the login value.
- Validating the submitted password against `password_hash`.
- Rejecting disabled users.
- Loading user roles into Spring Security.
- Public login endpoint.
- Logout endpoint that invalidates the current authentication artifact.
- Protecting API endpoints by default.
- Replacing the `/hello` smoke endpoint with Spring Boot Actuator health.
- Keeping `GET /actuator/health` public.
- Regular `@SpringBootTest` integration tests separated by controller.

This feature does not include:

- Refresh tokens.
- Frontend login form.
- User registration.
- Password reset.
- Dedicated `login` column.
- Role-restricted authorization beyond loading the user's role into the security context.

## Authentication Contract

Clients first bootstrap CSRF protection through a public endpoint:

```http
GET /auth/csrf
```

The response returns `204 No Content` and sets an `XSRF-TOKEN` cookie. Clients authenticate through a public login endpoint:

```http
POST /auth/login
Content-Type: application/json
Cookie: XSRF-TOKEN=<csrf-token>
X-XSRF-TOKEN: <csrf-token>
```

Request body:

```json
{
  "login": "user@example.com",
  "password": "secret"
}
```

Because `login = email`, the service treats `login` as the user email address.

Successful login creates a server-side authenticated session for the MVP and returns the authenticated user summary. Controllers and application use cases must not depend on password hashing or session implementation details.

Because the MVP uses browser-compatible server-side sessions, unsafe HTTP methods must be protected by CSRF. Clients obtain the CSRF token from `GET /auth/csrf`, read the `XSRF-TOKEN` cookie, and echo the same value in the `X-XSRF-TOKEN` header for login and logout requests.

Clients logout through:

```http
POST /auth/logout
Cookie: XSRF-TOKEN=<csrf-token>
X-XSRF-TOKEN: <csrf-token>
```

Logout invalidates the current server-side session through Spring Security logout support.

## Behavior

When a request calls `POST /auth/login`, the service must:

1. Read `login` from the request body.
2. Treat `login` as the user email.
3. Load the user by email.
4. Compare the submitted password with the stored `password_hash`.
5. Reject authentication if the user does not exist.
6. Reject authentication if the password is invalid.
7. Reject authentication if the user is disabled.
8. On success, expose the authenticated principal through Spring Security.
9. On success, expose the user role as a Spring Security authority.
10. On success, establish the authenticated server-side session.

When a request calls `POST /auth/logout`, the service must:

1. Accept the request without requiring role-specific authorization.
2. Invalidate the current server-side session when one is present.
3. Return success even if the client is already effectively logged out.

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

Password hashing and password matching must be isolated behind a replaceable application-level service or port. Spring Security's encoder is an implementation detail of the current adapter/configuration, not a dependency that should leak into domain behavior or controller logic.

The MVP implementation may use Spring Security's delegating password encoder so existing and future encoding formats can be handled consistently.

Recommended encoder:

```java
PasswordEncoderFactories.createDelegatingPasswordEncoder()
```

## Authorization Rules

Public endpoints:

- `GET /actuator/health`
- `GET /auth/csrf`
- `POST /auth/login`
- `POST /auth/logout`

Authenticated endpoints:

- All business API endpoints require authentication by default.

The `/hello` endpoint must no longer be the public smoke endpoint. It should be removed or stop being part of the public API contract as part of the implementation.

## Response Rules

```text
200 OK
The request succeeds when valid credentials are provided and the endpoint permits the user.

204 No Content
Logout succeeds when the current server-side session is invalidated or when the client is already effectively logged out.

401 Unauthorized
Returned when protected endpoint credentials are missing or invalid, or when login credentials are unknown, invalid, or disabled.

403 Forbidden
Reserved for future role-based authorization when an authenticated user lacks permission.
```

## Integration Test Requirement

Use a TDD approach: write the regular Spring Boot integration tests first, verify they fail for the missing behavior, then implement the production code needed to make them pass.

Integration tests must be separated by controller.

Suggested test style:

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {
}

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorHealthIntegrationTest {
}

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {
}
```

The test suite should verify:

1. `GET /actuator/health` works without authentication.
2. `GET /auth/csrf` works without authentication and creates an `XSRF-TOKEN` cookie.
3. `POST /auth/login` is public and is not protected by authentication or role rules.
4. `POST /auth/login` requires a valid CSRF token.
5. `POST /auth/login` returns `401 Unauthorized` when the login/email does not exist.
6. `POST /auth/login` returns `401 Unauthorized` when the password is wrong.
7. `POST /auth/login` returns `401 Unauthorized` when the user is disabled.
8. `POST /auth/login` succeeds when login/email and password are valid.
9. `POST /auth/logout` is callable without role-specific authorization and invalidates the current server-side session.
10. A protected controller endpoint returns `401 Unauthorized` when no credentials/authentication artifact is provided.
11. A protected controller endpoint succeeds when the current authentication artifact is valid.
12. The authenticated principal contains the expected email.
13. The authenticated authorities contain the expected user role.

## Implementation Notes

The likely implementation should:

- Add Spring Boot Actuator if it is not already present.
- Expose the health endpoint.
- Add login and logout endpoints under a dedicated authentication controller.
- Permit unauthenticated access to `GET /actuator/health`.
- Permit unauthenticated access to `GET /auth/csrf`.
- Permit unauthenticated access to `POST /auth/login`.
- Permit unauthenticated access to `POST /auth/logout`.
- Require authentication by default for all other endpoints.
- Use Spring Security server-side session authentication for the MVP.
- Use CSRF protection for unsafe methods because the MVP uses server-side browser sessions.
- Add or expose a replaceable password hashing/matching service.
- Keep `DomainUserDetailsService` as the Spring Security adapter.
- Continue loading users through the application/user repository port.
- Avoid adding a new `login` column for now.
- Remove `/hello` from the public API contract and tests.

## Done Criteria

This feature is done when:

- Login/password authentication is enabled.
- Email is accepted as the login value.
- Valid enabled users can authenticate.
- Invalid credentials return `401 Unauthorized`.
- Disabled users return `401 Unauthorized`.
- `GET /actuator/health` is public.
- `GET /auth/csrf` is public and creates an `XSRF-TOKEN` cookie.
- `POST /auth/login` is public.
- `POST /auth/logout` invalidates the current server-side session.
- Password hashing/matching is isolated behind a replaceable service or port.
- Protected endpoints require authentication.
- `/hello` is no longer used as the health/smoke endpoint.
- Controller-separated `@SpringBootTest` integration coverage proves the behavior.
- `make test-api` passes.
