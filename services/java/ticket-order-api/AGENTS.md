# AGENTS.md

Agent guidance for the `ticket-order-api` service.

## Scope

This file applies to `services/java/ticket-order-api`.

## Development

- Commit messages for this module must follow `TICKET_SERVICE#123 - short description`.
- Update this module's `CHANGELOG.md` before every commit that changes this module.
- Also update the root `CHANGELOG.md` under the `ticket-order-api` module section.
- Use Java 25 and Spring Boot 4.1.
- Follow hexagonal architecture:
  - `domain/model` for framework-free domain objects
  - `application/port/in` for inbound use case ports
  - `application/service` for use case implementations
  - `adapter/in/web` for HTTP controllers
  - `adapter/in/security` for security framework entrypoints
  - `adapter/out/*` for driven adapters such as persistence, messaging, or external clients
  - `infrastructure/config` for Spring, CORS, security, and runtime configuration
- Controllers must call application ports, not own business behavior.
- Controllers must not read `SecurityContextHolder` directly. Use a shared web adapter helper
  for current authenticated user resolution.
- Use Spring Security method annotations such as `@PreAuthorize` for coarse role checks at the
  HTTP adapter boundary.
- Keep ownership and business-state authorization checks inside application services.
- Application services may depend on domain objects and ports.
- Domain code must not depend on Spring or web framework APIs.
- Do not use Java primitives for fields in domain records, application command/query records, or
  JPA entity classes. Use boxed types such as `Integer`, `Long`, and `Boolean` so nullability is
  explicit at model boundaries. Primitive method return values and local variables are acceptable
  for simple counts, predicates, and calculations.
- Prefer static imports for static constants and helper methods when they make code clearer and
  avoid repeated qualifiers, for example `TRUE.equals(value)` with
  `import static java.lang.Boolean.TRUE;`.
- Place field and local-variable annotations on their own line above the declaration. Do not
  inline annotations with class members, for example use `@Autowired` on the line before
  `private MockMvc mockMvc;`.
- Use MapStruct for application, contract, and persistence object mappings when the conversion is
  mostly field-to-field or combines source objects with supplied values. Keep manual construction
  only for framework configuration, simple value objects, test fixtures, or behavior that is clearer
  than a mapper.
- Use YAML for Spring configuration files. Prefer `application.yml` over `application.properties`.
- Keep configuration values externalized in `application.yml` unless a task requires another config source.
- Generate Spring API interfaces and models from `contracts/openapi/ticket-order-api/openapi.yml`.
- Do not edit generated OpenAPI sources by hand.
- Treat warnings from generated OpenAPI sources as contract or generator-configuration issues.
  Do not patch generated files under `target`.
- API surface changes must update `contracts/openapi/ticket-order-api/openapi.yml` before generated code or implementation.
- Database schema changes for this service must be implemented through `database/java/migrations/ticket-order-db-migrations`.

## Test

- Do not run Java tests after every individual Java change.
- Run Java tests before pushing a branch to remote, before creating a PR, or when the user explicitly asks.
- Run from the repository root:

```sh
make test-api
```

- For contract generation changes, run from the repository root:

```sh
make generate-api-contracts
```

- Add focused unit tests for domain and application service behavior.
- Add web or integration tests for controller, CORS, and security behavior.
- Keep tests aligned with package ownership:
  - controller tests under `adapter/in/web`
  - service tests under `application/service`
  - configuration tests under `infrastructure/config`
- For controller integration tests, prefer `@SpringBootTest` with `@AutoConfigureMockMvc`
  and keep tests separated per controller or web concern.
- Controller integration tests should exercise HTTP, security, request/response mapping,
  and controller-to-port wiring. They should not set up data through `JdbcTemplate` or
  depend on migration/table details.
- When controller tests need application state, import a test configuration that overrides
  the existing application port bean with a test implementation of the same port. Keep
  reusable test fixtures static and reset mutable test state in `@BeforeEach`.
- Use direct service/domain unit tests for business branching that does not require the
  web/security filter chain.
- Before pushing Java model changes, scan `domain/model`, `application/port/in`, and
  `adapter/out/persistence` for primitive fields and confirm any remaining primitive hits are
  method signatures, local variables, or generated code.

## Deploy

- Build the API image from the repository root:

```sh
make docker-build-api
```

- Run with Compose from the repository root:

```sh
make compose-up
```

- The API listens on port `8080`.
- Keep runtime ports and service names synchronized with `docker-compose.yml`.

## Next Iterations

- Add outbound ports under `application/port/out` before introducing persistence, messaging, or external HTTP clients.
- Add outbound adapters under `adapter/out/*` for databases, queues, or third-party APIs.
- Keep DTOs in adapter packages and map them to application/domain types.
- Expand CORS and security deliberately as real authentication requirements appear.
