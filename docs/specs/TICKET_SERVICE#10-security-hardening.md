# TICKET_SERVICE#10 - Security Hardening

## Goal

Address findings from a security review of `ticket-order-api` covering session
handling, login abuse resistance, authorization matcher precision, actuator
exposure, startup configuration safety, and container privilege.

## Context

A manual security review of the authentication, authorization, and
infrastructure-config code identified the following, in priority order:

1. Login and registration build the `SecurityContext` by hand and save it into
   whatever `HttpSession` already exists, without rotating the session ID.
   This bypasses Spring Security's normal session-fixation protection.
2. `POST /auth/login` has no rate limiting or lockout, so credential stuffing
   and password guessing are unmitigated at the application layer.
3. The HTTP authorization matcher `/events/*` is a single-segment wildcard
   that was meant to permit `GET /events/{id}`, but it also matches
   `POST /events/orders` and `DELETE /events/orders`. `createEventOrders` is
   saved by an explicit `@PreAuthorize`; `deleteEventOrders` has none and is
   only "protected" today by an incidental `NoSuchElementException` for
   anonymous callers.
4. `/actuator/metrics` and `/actuator/prometheus` are reachable by any
   authenticated user on the same port as the business API.
5. `application.yml` falls back to known weak local datasource credentials
   (`ticket_order` / `ticket_order`) when the corresponding environment
   variables are unset, with no guard against that fallback being used
   outside local development.
6. The API's `Dockerfile` has no `USER` directive, so the JVM process runs as
   `root` inside the container.

This spec does not change the OpenAPI contract, the web app, or the database
schema.

## Scope

This feature includes:

- Rotating the session ID on successful login/registration.
- An in-process login rate limiter for `POST /auth/login`.
- Tightening the `/events/*` authorization matcher to `GET` only, and adding
  `@PreAuthorize` to `deleteEventOrders`.
- Moving actuator endpoints to a separate management port, decoupled from the
  business API's security filter chain.
- A startup validator that fails fast outside the `local` profile when
  datasource credentials are missing or match the known local defaults.
- Running the API container as a non-root user.
- A `TODO` comment on duplicate-email registration handling (documentation
  only, no behavior change).

This feature does not include:

- Reconciling the OpenAPI contract's `/public/events` and
  `/public/events/{eventId}` operations with the actual implementation
  (tracked separately as future work).
- Implementing structured error responses (`ErrorResponse` schema) for
  duplicate-email registration or any other `AuthController`/`UserController`
  failure path — left as the `TODO` above.
- Distributed/shared rate-limit state (e.g. Redis-backed) — the limiter is
  per-instance; documented as a known limitation.
- Any change to `contracts/openapi/ticket-order-api`, the web app, or
  database migrations.

## Session Fixation Fix

`AuthenticationSessionManager.authenticate(User user)` must rotate the
session ID before the new authenticated `SecurityContext` is persisted:

1. If an `HttpSession` already exists on the current request, change its ID
   in place (equivalent to Spring Security's `changeSessionId` strategy)
   before writing the new `SecurityContext` into it.
2. If no session exists yet, behavior is unchanged — one will be created when
   the context is saved.
3. Apply to both `login()` and `registerUser()` in `AuthController`, since
   both call `authenticate()`.

This must not change the authenticated principal, granted authorities, or the
response shape of `POST /auth/login` / `POST /auth/register`.

## Login Rate Limiting

Add a servlet `Filter` (e.g. `LoginRateLimitFilter`) registered in the
existing security filter chain, scoped to `POST /auth/login` only (all other
paths bypass it, following the same `shouldNotFilter` pattern as
`CsrfCookieFilter`).

Rules:

- Track failed attempts in-memory, keyed independently by client IP and by
  normalized (lower-cased) email/login value.
- Window: 5 failed attempts per 15 minutes per key.
- Once either key exceeds the threshold, further login attempts for that key
  return `429 Too Many Requests` without invoking `LoginUseCase`.
- A successful login clears the counters for both the IP and email keys
  involved in that request.
- Expired entries must be evicted (checked on access, or via a periodic
  sweep) so the tracking map does not grow unbounded.
- Use the shared `Supplier<Instant>` time source from `CoreConfig` for window
  timestamps, not `Instant.now()` directly.
- Log a rate-limit trip at `WARN`, including the limiting key type (IP vs
  email) but never the submitted password.

Known limitation to document in code: state is per-instance. If the API ever
runs multiple replicas without a shared/sticky layer in front, limits are not
shared across instances. Out of scope to fix here.

## Authorization Matcher and Order-Deletion Guard

In `AuthenticationSecurityConfig`:

```java
.requestMatchers("/actuator/health", "/auth/**").permitAll()
.requestMatchers(HttpMethod.GET, "/events/*").permitAll()
```

replacing the current single `.requestMatchers("/actuator/health", "/auth/**",
"/events/*").permitAll()` line. `GET /events/{id}` keeps working exactly as
today; `POST`/`DELETE /events/orders` fall back under
`anyRequest().authenticated()`.

In `EventController`, add `@PreAuthorize("hasRole('CUSTOMER')")` to
`deleteEventOrders`, matching the existing annotation on
`createEventOrders`. Behavior for authorized customers is unchanged; the
ownership check inside `EventService.deleteEventOrders` is unchanged.

## Actuator Network Isolation

Add a separate management port so actuator is not served on the same port as
the business API:

```yaml
management:
  server:
    port: ${MANAGEMENT_SERVER_PORT:9090}
```

Access control for `/actuator/metrics` and `/actuator/prometheus` becomes a
network-reachability property (not publishing/routing port `9090`) rather
than an application-level role check. `/actuator/health` may remain reachable
the same way.

This spec does not add a Compose health check or update any deployment
manifest to target the new port — none currently reference `/actuator/**`.
Document, as a follow-up note in the spec's Implementation Notes, that any
future health-check/probe setup must target the management port.

## Startup Datasource Validator

Add a component (e.g. `DatasourceCredentialsStartupValidator`) implementing
`ApplicationRunner`:

- Runs only when the active Spring profile is not `local`.
- Fails startup (throws `IllegalStateException`, causing the application to
  exit non-zero) if, for either the primary or read-replica datasource:
  - the resolved username or password is blank, or
  - the resolved username or password equals the known local default
    (`ticket_order`).
- Does not log the actual credential values (blank/matches-default checks
  only; failure message must not echo the resolved password).

This is a best-effort fail-fast guard: `ApplicationRunner` executes after the
embedded server has started, so the process exits shortly after boot rather
than refusing to bind the port. That trade-off is acceptable here and should
be noted in the validator's Javadoc/comment rather than solved with a more
invasive `EnvironmentPostProcessor`.

## Container Non-Root User

Update `services/java/ticket-order-api/Dockerfile` to create and run as a
dedicated non-root user:

```dockerfile
FROM eclipse-temurin:25-jre

RUN groupadd --system app && useradd --system --gid app --home /app --shell /usr/sbin/nologin app

WORKDIR /app

COPY services/java/ticket-order-api/target/ticket-order-api-0.0.1-SNAPSHOT.jar app.jar
RUN chown app:app app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

No change to the exposed port, entrypoint arguments, or image tag.

## Duplicate Email TODO

Add a `// TODO:` comment above `AuthController.registerUser` noting that a
duplicate-email registration currently surfaces as an unhandled exception
(raw `500`) instead of `409 Conflict`, and that fixing this requires a
`@RestControllerAdvice` mapping to the contract's existing `ErrorResponse`
schema. No behavior change in this ticket.

## Response Rules

```text
429 Too Many Requests
Returned by POST /auth/login when the per-IP or per-email failure threshold
(5 failures / 15 minutes) has been exceeded. Existing 401 behavior for
individual invalid-credential attempts under the threshold is unchanged.

401 Unauthorized / 403 Forbidden
Unchanged for all other endpoints. DELETE /events/orders now additionally
returns 403 (via @PreAuthorize) for an authenticated non-CUSTOMER caller,
matching POST /events/orders; unauthenticated callers continue to be
rejected before reaching the controller for GET/POST/DELETE under
/events/orders once the matcher change lands.
```

## Test Requirement

Use a TDD approach: write the regular Spring Boot integration/unit tests
first, verify they fail for the missing behavior, then implement the
production code needed to make them pass.

The test suite should verify:

1. The `JSESSIONID` (or equivalent session identifier) changes between a
   pre-login request and the response to a successful `POST /auth/login`.
2. The same session-ID rotation happens on successful `POST
   /auth/register`.
3. 5 consecutive failed `POST /auth/login` attempts for the same email
   within the window cause the 6th attempt to return `429`, even with a
   correct password.
4. 5 consecutive failed attempts from the same IP across different emails
   also trigger `429` for that IP.
5. A successful login resets the failure counters for that email/IP.
6. `DELETE /events/orders` returns `401`/`403` (not `200`/`204`) for an
   unauthenticated or non-`CUSTOMER` caller.
7. `POST /events/orders` and `DELETE /events/orders` are unreachable without
   authentication at the HTTP layer (matcher-level), independent of
   `@PreAuthorize`.
8. `GET /events/{id}` remains reachable without authentication.
9. `GET /actuator/metrics` and `GET /actuator/prometheus` are not served on
   the main application port after the management-port change.
10. The startup validator throws/fails application context startup under a
    non-`local` profile when a datasource password is blank.
11. The startup validator throws/fails application context startup under a
    non-`local` profile when a datasource password equals the known local
    default.
12. The startup validator does not fail under the `local` profile regardless
    of credential values.

## Implementation Notes

The likely implementation should:

- Add `LoginRateLimitFilter` under
  `infrastructure/config/security`, registered in
  `AuthenticationSecurityConfig` the same way `CsrfCookieFilter` is.
- Change `AuthenticationSessionManager.authenticate` to rotate the session ID
  before saving the `SecurityContext`.
- Update the single `requestMatchers(...)` line in
  `AuthenticationSecurityConfig` to split out the `GET`-only `/events/*`
  rule.
- Add `@PreAuthorize("hasRole('CUSTOMER')")` to
  `EventController.deleteEventOrders`.
- Add `management.server.port` to `application.yml` (and the corresponding
  env var default to `docker-compose.yml` only if/when a health check is
  introduced there — not required for this ticket since none exists today).
- Add `DatasourceCredentialsStartupValidator` under `infrastructure/config`.
- Update `services/java/ticket-order-api/Dockerfile` for the non-root user.
- Add the `TODO` comment to `AuthController.registerUser`.
- Follow existing module conventions: field/local-variable annotations on
  their own line, `WARN` logging for security-relevant events, no raw
  credentials in logs, shared `Supplier<Instant>` for timestamps.

## Done Criteria

This feature is done when:

- Session ID rotates on every successful login and registration.
- `POST /auth/login` enforces the 5-per-15-minute failure threshold per IP
  and per email, returning `429` once exceeded, and resets on success.
- `POST`/`DELETE /events/orders` require authentication at the HTTP matcher
  layer, not only via `@PreAuthorize`.
- `DELETE /events/orders` carries `@PreAuthorize("hasRole('CUSTOMER')")`.
- Actuator endpoints are served on a separate management port, not the
  business API port.
- The application refuses to start outside the `local` profile with a blank
  or known-default datasource credential.
- The API container runs as a non-root user.
- A `TODO` documents the duplicate-email registration gap without changing
  its behavior.
- New/updated tests from the Test Requirement section pass.
- `make test-api` passes.
