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
  - `infrastructure/config` for Spring, CORS, security, and runtime configuration
- Controllers must call application ports, not own business behavior.
- Application services may depend on domain objects and ports.
- Domain code must not depend on Spring or web framework APIs.
- Use YAML for Spring configuration files. Prefer `application.yml` over `application.properties`.
- Keep configuration values externalized in `application.yml` unless a task requires another config source.

## Test

- Do not run Java tests after every individual Java change.
- Run Java tests before pushing a branch to remote, before creating a PR, or when the user explicitly asks.
- Run from the repository root:

```sh
make test-api
```

- Add focused unit tests for domain and application service behavior.
- Add web or integration tests for controller, CORS, and security behavior.
- Keep tests aligned with package ownership:
  - controller tests under `adapter/in/web`
  - service tests under `application/service`
  - configuration tests under `infrastructure/config`

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
