# TICKET_SERVICE#7 - Backend Observability

## Goal

Add structured backend observability to the Ticket Service API through logs, metrics, and traces.

The service must use:

```text
SLF4J
MDC
OpenTelemetry
Spring Boot Actuator
Micrometer
```

The first implementation should make production behavior diagnosable without exposing secrets, personal data beyond explicit identifiers needed for operations, or generated framework details outside the infrastructure layer.

## Context

The Java API is a Spring Boot service using hexagonal architecture. Observability must support debugging API requests, authentication flows, authorization decisions, persistence failures, and application use case execution.

This feature is technical infrastructure. It must not change public business API behavior, OpenAPI response models, domain rules, or database schemas unless a later implementation task explicitly requires that.

Observability concerns belong at the service edge and infrastructure boundaries:

- HTTP request correlation belongs in web or infrastructure filters.
- Logging uses SLF4J from production code.
- Request-scoped diagnostic context uses MDC.
- Metrics are exposed through Spring Boot Actuator and Micrometer.
- Distributed tracing uses OpenTelemetry instrumentation and exporters.
- Application and domain code can log meaningful business events but must not depend on OpenTelemetry SDK APIs.

## Scope

This feature includes:

- Adding a request correlation ID for every HTTP request.
- Accepting an incoming correlation ID header when present and valid.
- Generating a correlation ID in the backend when the client does not provide one.
- Adding correlation fields to MDC for request-scoped logs.
- Clearing MDC after every request.
- Standardizing backend log fields and message rules.
- Adding Logback JSON logging configuration.
- Keeping logging configuration outside the main Spring application YAML.
- Keeping log structure definitions in separate files.
- Making log output targets configurable by environment.
- Making log file locations configurable by environment.
- Making log rotation and retention configurable by environment.
- Making request logging, masking, correlation handling, and custom metrics explicitly configurable.
- Adding structured request lifecycle logs.
- Adding focused controller and service logs for important visibility events and rejected operations.
- Adding Spring Boot Actuator metrics endpoints for backend runtime and HTTP metrics.
- Adding custom Micrometer metrics for core business operations.
- Adding OpenTelemetry trace propagation for inbound and outbound boundaries.
- Adding OpenTelemetry export configuration through externalized runtime properties.
- Adding tests for MDC cleanup, correlation propagation, public actuator access, and metric registration.

This feature does not include:

- Required frontend correlation implementation. The UI may send `X-Correlation-ID`, but it is optional.
- Frontend observability beyond optionally passing a correlation ID header.
- Log aggregation infrastructure.
- Dashboard creation.
- Alerting rules.
- SLO definitions.
- Business audit logging.
- Database schema changes.
- Persisting logs or traces in the application database.
- Adding vendor-specific observability SDKs directly to domain or application services.
- Sending logs, metrics, or traces to a paid external backend by default.

## Correlation ID

The service must use this HTTP header for correlation:

```text
X-Correlation-ID
```

Rules:

1. If the request contains a valid `X-Correlation-ID`, reuse it.
2. If the request does not contain `X-Correlation-ID`, generate a new `UUID-Date` value in the backend.
3. Add the effective value to the response as `X-Correlation-ID`.
4. Add the effective value to MDC as `correlationId`.
5. Keep the correlation ID stable for the whole request.
6. Clear the MDC value after request completion, including error paths.
7. Treat an invalid incoming `X-Correlation-ID` as a suspicious request.
8. When the incoming value has the wrong format, invalidate the current user session if one exists.
9. After invalidating the session for a wrong-format correlation ID, reject the request and require the user to log in again.
10. Do not include the rejected raw correlation ID value in logs.

Valid incoming values:

- Must match the configured correlation validation pattern.
- By default, must use the `UUID-Date` format.
- By default, the UUID part must be a canonical UUID.
- By default, the date part must use `dd-MM-yyyy`.
- By default, the separator between the UUID and date parts must be `-`.
- By default, must be exactly 47 characters.
- By default, must contain only hexadecimal letters, digits, and `-`.
- Must not contain whitespace or control characters.

Required format:

```text
<uuid>-<dd-MM-yyyy>
```

Example:

```text
018f0f5e-4e7a-7a89-b2f3-5d9d4a0b91c2-13-08-2026
```

The date should represent the day the correlation ID was created. The backend can parse the value by treating the first 36 characters as the canonical UUID, the next character as the separator, and the remaining value as the `dd-MM-yyyy` date. The backend does not need to reject a validly formatted date only because it is not today, but it may log a debug event when the date is outside a reasonable operational window.

When an incoming value is invalid, the service must:

1. Generate a replacement `UUID-Date` correlation ID only for server-side logging and the response header.
2. Add the replacement value to MDC as `correlationId`.
3. Invalidate the current HTTP session if one exists.
4. Clear the Spring Security context for the request.
5. Return an authentication failure response that forces the client to re-login.
6. Log a warn-level `http.correlation.invalid` event without including the rejected raw value.

Recommended response:

```text
401 Unauthorized
```

The frontend is not required to create a correlation ID. Missing correlation IDs are normal and must be handled by the backend filter.

Correlation policy must be externalized with conservative defaults:

```yaml
ticket-order-platform:
  observability:
    correlation:
      header-name: X-Correlation-ID
      validation-pattern: '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}-\d{2}-\d{2}-\d{4}$'
      value-template: '{uuid}-{date:dd-MM-yyyy}'
      invalid-id-action: invalidate-session
      generate-when-missing: true
```

Rules:

1. `header-name` may be changed for deployment integration needs, but the default must remain `X-Correlation-ID`.
2. `validation-pattern` controls incoming correlation ID validation and defaults to the `UUID-dd-MM-yyyy` regex.
3. `value-template` controls backend-generated correlation IDs and defaults to `<UUID>-<dd-MM-yyyy>`.
4. Supported generation tokens are `{uuid}`, `{date:<java-date-pattern>}`, and `{random-hex:<length>}`.
5. Do not use SpEL for correlation ID generation.
6. The configured `value-template` should generate values accepted by the configured `validation-pattern`.
7. The default validation path must still reject invalid UUIDs and invalid `dd-MM-yyyy` dates.
8. Custom validation patterns are allowed for deployment integration, but they must be configured in application properties, not read directly from environment variables in code.
9. `invalid-id-action` defaults to `invalidate-session`.
10. `generate-when-missing` defaults to `true` because frontend correlation is optional.
11. If `generate-when-missing` is set to `false`, a missing correlation ID may be rejected as a hardened deployment mode.
12. Backend-generated correlation ID dates must use the service's shared `Supplier<Instant>` time source instead of introducing an observability-specific `Clock` bean.

## MDC Fields

One backend MDC filter must run once per HTTP request and populate these MDC fields when the value is known:

```text
correlationId
requestMethod
requestPath
authenticatedUserId
authenticatedUserRole
```

Rules:

1. `correlationId`, `requestMethod`, and `requestPath` are available for all HTTP requests.
2. `authenticatedUserId` and `authenticatedUserRole` are added only when the authenticated principal is known.
3. Raw passwords, password hashes, session IDs, CSRF tokens, cookies, and authorization headers must never be written to MDC.
4. MDC must be cleared in a `finally` block or equivalent guaranteed cleanup path.
5. Async execution must not silently lose required correlation context for application tasks introduced by this feature.
6. The MDC filter owns correlation ID creation and validation.
7. Controllers must not manually create or validate correlation IDs.

## Logging

Production code must log through SLF4J.

Allowed:

```java
private static final Logger log = LoggerFactory.getLogger(CurrentClass.class);
```

Lombok `@Slf4j` is preferred for class loggers in this module unless a named logger is required for independent routing, such as security or telemetry logs.

Log levels:

```text
ERROR - every exception and any failure that prevents completing the operation
WARN  - overall operational visibility, rejected operations, invalid state, and suspicious input
INFO  - system calls, service startup, service shutdown, and successful infrastructure lifecycle events
DEBUG - request details and implementation diagnostics useful in local development
TRACE - disabled by default and reserved for deep local troubleshooting
```

Controller and service logs:

1. Controllers may log request entry and completion only through the common request logging infrastructure.
2. Controllers must not duplicate the same request lifecycle log emitted by the MDC/request filter.
3. Controllers should log WARN for rejected requests, invalid input that reaches controller handling, authorization denials, and invalid correlation handling.
4. Application services should log WARN for business rejections, invalid state transitions, duplicate-order attempts, and denied ownership operations.
5. Application services should not log INFO for normal business calls by default.
6. INFO is reserved for system calls and infrastructure lifecycle events, for example startup, shutdown, health integration initialization, metrics exporter startup, or successful external system handshake.
7. Any unexpected caught exception that is logged must be logged at ERROR level.
8. Every unexpected exception log must include the exception object.
9. Expected authentication failures such as bad credentials may be logged as WARN security events without logging the exception object or stack trace.
10. The same exception must not be logged repeatedly at multiple layers.
11. If a lower-level component already logs the concrete technical failure, the upper layer should either propagate the exception or log only a non-duplicated boundary event without the same stack trace.
12. For database failures, do not repeat SQL driver, connection pool, or persistence adapter exception logs in application services unless the service adds a distinct business context that is not available in the lower-level log.
13. When distinct business context is required, log one ERROR event at the application boundary with a stable event name and avoid duplicating the lower-level technical message verbatim.
14. WARN is the default level for overall visibility into expected but important runtime conditions.

Required request completion log fields:

```text
correlationId
method
path
status
durationMs
authenticated
authenticatedUserId
```

Rules:

1. Do not log raw passwords.
2. Do not log password hashes.
3. Do not log full cookies.
4. Do not log CSRF tokens.
5. Do not log session IDs.
6. Do not log authorization headers.
7. Do not log full request or response bodies by default.
8. Use parameterized log messages instead of string concatenation.
9. Prefer stable event names in messages for logs that may be searched later.
10. Exception logs should include the exception object once, at the boundary where the failure is handled.
11. Exception logs must not repeat the same stack trace already emitted by a lower-level dependency or adapter.

Suggested stable event names:

```text
http.request.completed
auth.login.succeeded
auth.login.failed
auth.logout.completed
event.created
event.updated
event.published
event.unpublished
event.order.created
event.order.rejected
```

## Logback JSON Output

The service must provide a Logback configuration for structured JSON logs.

The logging configuration must live outside the main Spring application configuration. Prefer a dedicated YAML descriptor for logging configuration, for example:

```text
logback-spring.yml
```

If Logback requires XML for a specific appender or encoder, use `logback-spring.xml` as the active runtime configuration and keep a dedicated YAML descriptor for stream structure and project-level documentation. The active Logback runtime configuration and the structure definitions must remain separate from `application.yml`.

Required logical log streams:

```text
application
telemetry
security
```

Rules:

1. Application logs contain request lifecycle, use case, and service behavior logs.
2. Telemetry logs contain observability pipeline and OpenTelemetry export diagnostics.
3. Security logs contain authentication, logout, authorization rejection, and invalid correlation events.
4. Each stream must be configurable independently.
5. Each stream must support console output.
6. Each stream must support file output.
7. Output targets must be configurable by environment as `console`, `file`, or both.
8. File locations must be configurable by environment.
9. JSON logs must include MDC fields when present.
10. JSON logs must not include sensitive values.
11. Local development may default to console output.
12. Production should support file output without code changes.
13. Log structure definitions must live in files separate from the logging runtime configuration.
14. Application, telemetry, and security log structures must have separate definition files.
15. Logback configuration must reference those structure definitions instead of duplicating the full structure inline.

Required JSON fields:

```text
timestamp
level
logger
thread
message
event
service
environment
correlationId
traceId
spanId
requestMethod
requestPath
status
durationMs
authenticatedUserId
authenticatedUserRole
exception
```

Fields with unknown values may be omitted or set to `null`, but the JSON structure must stay stable enough for log aggregation.

Required log structure definition files:

```text
application-log-structure.yml
telemetry-log-structure.yml
security-log-structure.yml
```

The final file names may follow the module's resource naming conventions, but the structure definitions must stay separate from each other and from the main Logback runtime configuration.

Each structure definition must declare:

- Field names.
- Required or optional status.
- Source of the value, for example MDC, OpenTelemetry context, logger event, or application-provided field.
- Masking behavior for sensitive or risky fields.
- Default value behavior for missing optional fields.

Example structure definition shape:

```yaml
fields:
  - name: correlationId
    required: true
    source: mdc
    mdcKey: correlationId
    masking: none
  - name: message
    required: true
    source: logEvent
    masking: sensitive-patterns
```

Required configurable values:

```text
ticket-order-platform.observability.logging.enabled
ticket-order-platform.observability.logging.request-logging-enabled
ticket-order-platform.observability.logging.application.targets
ticket-order-platform.observability.logging.application.file
ticket-order-platform.observability.logging.telemetry.targets
ticket-order-platform.observability.logging.telemetry.file
ticket-order-platform.observability.logging.security.targets
ticket-order-platform.observability.logging.security.file
ticket-order-platform.observability.logging.masking.enabled
ticket-order-platform.observability.logging.masking.additional-fields
ticket-order-platform.observability.logging.rolling.max-file-size
ticket-order-platform.observability.logging.rolling.max-history-days
ticket-order-platform.observability.logging.rolling.total-size-cap
```

Example target values:

```text
console
file
console,file
```

The exact property names may be adjusted to fit Spring Boot and Logback conventions, but the behavior must remain configurable without rebuilding the application.

## Sensitive Data Masking

Sensitive data must be hidden before it reaches logs.

Values that must never appear in clear text:

```text
password
passwordHash
password_hash
csrfToken
XSRF-TOKEN
JSESSIONID
Cookie
Set-Cookie
Authorization
sessionId
accessToken
refreshToken
```

Rules:

1. Prefer not logging sensitive fields at all.
2. When a value must be represented, write a fixed mask such as `[REDACTED]`.
3. Masking must apply to structured JSON fields and formatted exception/request metadata.
4. Do not rely on developers remembering to mask values at every call site.
5. Request and response bodies remain disabled in logs by default.
6. If body logging is ever enabled for local debugging, masking must still apply.
7. Default sensitive fields must be defined in code.
8. Configuration may add more sensitive field names, but must not remove the core protected fields listed above.

## Metrics

Metrics must use Spring Boot Actuator and Micrometer.

Required actuator endpoints:

```text
GET /actuator/health
GET /actuator/metrics
GET /actuator/prometheus
```

Access rules:

1. `GET /actuator/health` remains public.
2. `GET /actuator/metrics` must not be public in production.
3. `GET /actuator/prometheus` must not be public in production.
4. Non-health actuator endpoints may be public in local development only when explicitly configured.

Required standard metrics:

- JVM memory and CPU metrics.
- Process uptime metrics.
- HTTP server request metrics.
- Spring Security metrics when available through supported instrumentation.
- Database connection pool metrics when persistence is enabled.

Required custom metrics:

```text
ticket.auth.login.attempts
ticket.auth.login.success
ticket.auth.login.failure
ticket.events.created
ticket.events.published
ticket.events.unpublished
ticket.event_orders.created
ticket.event_orders.rejected
```

Metric tags must stay low-cardinality.

Allowed tags:

```text
result
reason
role
event_status
```

Forbidden tags:

```text
user_id
email
session_id
correlation_id
event_id
event_name
request_body
```

Rules:

1. Do not create metrics with unbounded tag values.
2. Do not expose personal data in metric names or tags.
3. Counter metrics should be incremented only after the operation outcome is known.
4. Timer metrics should wrap meaningful work units, not every small private method.
5. Reuse Spring Boot default HTTP request metrics instead of creating duplicate request counters.
6. Authentication metrics must be recorded through an infrastructure aspect at the login use-case boundary, not through direct controller calls.
7. Custom metrics must be controlled through external configuration:

```yaml
ticket-order-platform:
  observability:
    metrics:
      enabled: true
      auth-metrics-enabled: true
```

## OpenTelemetry

OpenTelemetry must provide trace propagation and export configuration.

Required propagation support:

```text
traceparent
tracestate
baggage
```

Rules:

1. Incoming W3C trace context must be extracted when present.
2. Outgoing HTTP calls introduced later must inject W3C trace context.
3. Trace IDs and span IDs should appear in logs when supported by the logging instrumentation.
4. Application and domain services should not depend directly on OpenTelemetry SDK types.
5. Instrumentation should use Spring Boot supported auto-configuration where practical.
6. Exporters must be configurable by environment.
7. Export must be disabled or no-op by default for local development unless an endpoint is configured.

Externalized configuration must support:

```text
service.name
deployment.environment
otel.exporter.otlp.endpoint
otel.traces.exporter
otel.metrics.exporter
otel.logs.exporter
otel.propagators
```

Recommended default service name:

```text
ticket-order-api
```

## Configuration

Configuration must use YAML.

Spring application and management configuration may stay in the Java API's existing Spring YAML configuration file.

Logging configuration must live outside `application.yml`. If Logback XML is required for runtime appenders and encoders, the active file is:

```text
logback-spring.xml
```

Keep a separate YAML descriptor for logging stream definitions, for example:

```text
logback-spring.yml
```

Log structure definitions must live in separate YAML files, one per logical log stream:

```text
application-log-structure.yml
telemetry-log-structure.yml
security-log-structure.yml
```

Required configurable values:

```text
management.endpoints.web.exposure.include
management.endpoint.health.probes.enabled
management.metrics.tags.application
management.otlp.tracing.endpoint
logging.level.root
logging.level.com.example.ticketplatform
ticket-order-platform.observability.correlation.header-name
ticket-order-platform.observability.correlation.validation-pattern
ticket-order-platform.observability.correlation.value-template
ticket-order-platform.observability.correlation.invalid-id-action
ticket-order-platform.observability.correlation.generate-when-missing
ticket-order-platform.observability.metrics.enabled
ticket-order-platform.observability.metrics.auth-metrics-enabled
ticket-order-platform.observability.logging.enabled
ticket-order-platform.observability.logging.request-logging-enabled
ticket-order-platform.observability.logging.application.targets
ticket-order-platform.observability.logging.application.file
ticket-order-platform.observability.logging.telemetry.targets
ticket-order-platform.observability.logging.telemetry.file
ticket-order-platform.observability.logging.security.targets
ticket-order-platform.observability.logging.security.file
ticket-order-platform.observability.logging.masking.enabled
ticket-order-platform.observability.logging.masking.additional-fields
ticket-order-platform.observability.logging.rolling.max-file-size
ticket-order-platform.observability.logging.rolling.max-history-days
ticket-order-platform.observability.logging.rolling.total-size-cap
```

Profiles should separate local development from production:

- Local development may expose health and selected diagnostics.
- Production must expose health publicly and keep metrics protected by infrastructure or security configuration.
- Production trace export requires explicit OTLP endpoint configuration.

## Architecture Rules

1. Web adapters may read HTTP headers and populate correlation context.
2. Infrastructure configuration may own logging, MDC, Actuator, Micrometer, and OpenTelemetry setup.
3. Application services may log business-relevant events through SLF4J.
4. Domain models must remain framework-free and must not depend on SLF4J, MDC, Micrometer, Spring, or OpenTelemetry.
5. Generated OpenAPI models must not be changed for observability metadata.
6. Correlation ID behavior must be implemented as infrastructure, not repeated in every controller.
7. Security configuration must explicitly define actuator access rules.
8. Time-dependent observability code must reuse the shared `Supplier<Instant>` bean from core configuration when it only needs the current instant.
9. Observability infrastructure must be split into focused subpackages for correlation, logging, and metrics instead of one broad package for all concerns.

## Testing Requirements

Add focused tests for:

- Requests without `X-Correlation-ID` receive a generated `UUID-Date` response header.
- Requests with a valid `X-Correlation-ID` reuse the provided value.
- Requests with a valid UUID but invalid date format are rejected as invalid.
- Requests with an invalid `X-Correlation-ID` receive a generated replacement response header.
- Requests with an invalid `X-Correlation-ID` invalidate the current session.
- Requests with an invalid `X-Correlation-ID` require the user to log in again.
- MDC is cleared after request completion.
- MDC is cleared after exception handling.
- Logback emits valid JSON for application logs.
- Logback can route application logs to console, file, or both through configuration.
- Logback can route telemetry logs separately from application logs.
- Logback configuration is loaded from the dedicated logging YAML file.
- Application, telemetry, and security log structures are loaded from separate definition files.
- Sensitive values are masked or absent from emitted logs.
- Protected business endpoints still require authentication.
- `GET /actuator/health` remains public.
- Non-health actuator endpoints are not accidentally public in the production-like profile.
- Required custom meters are registered when their operations execute.

Tests should prefer Spring Boot integration tests for web/security behavior and focused unit tests for validation logic.

## Delivery Plan

1. Add observability dependencies using the existing build system.
2. Add Spring YAML configuration for Actuator, Micrometer, and OpenTelemetry.
3. Add dedicated Logback YAML configuration for JSON logging.
4. Add separate YAML structure definitions for application, telemetry, and security logs.
5. Add a request-scoped MDC filter that creates, validates, and exposes `UUID-Date` correlation IDs.
6. Add invalid correlation handling that invalidates the session and requires re-login.
7. Add sensitive-data masking for structured logs.
8. Add request completion logging.
9. Add custom metric instrumentation at application operation boundaries.
10. Add OpenTelemetry propagation and exporter configuration.
11. Add focused tests for correlation, session invalidation, actuator access, MDC cleanup, JSON logging, masking, and metrics.
12. Run the smallest relevant Java API validation target.

## Acceptance Criteria

The feature is complete when:

1. Every HTTP response includes `X-Correlation-ID`.
2. Every generated correlation ID follows the `UUID-Date` format.
3. Backend logs include `correlationId` for request-scoped log entries.
4. MDC values do not leak between requests.
5. Missing UI-provided correlation IDs are handled by the backend.
6. Wrong-format correlation IDs invalidate the current session and force re-login.
7. Request completion logs include method, path, status, and duration.
8. Logs are emitted as structured JSON.
9. Logging runtime configuration lives in a dedicated YAML file.
10. Application, telemetry, and security log structures live in separate definition files.
11. Application, telemetry, and security logs can be routed independently.
12. Console and file output targets are configurable.
13. Log file locations are configurable.
14. Log rotation and retention are configurable for file targets.
15. Sensitive values are masked or absent from logs.
16. Additional sensitive field names can be configured without removing core protected fields.
17. Correlation, request logging, and custom metric behavior are externally configurable with conservative defaults.
18. Actuator health remains public.
19. Metrics and Prometheus endpoints are protected outside local development.
20. Required custom Micrometer counters are present after relevant operations run.
21. OpenTelemetry trace context is extracted from inbound requests.
22. OpenTelemetry export is controlled by external configuration.
23. Domain code remains free of logging, metrics, Spring, MDC, and OpenTelemetry dependencies.
