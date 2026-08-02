# AGENTS.md

Repository-level guidance for agents working on TicketOrderPlatform.

## Scope

This file applies to the whole repository. More specific `AGENTS.md` files inside modules override this file for that module.

## Project Layout

- `services/java/ticket-order-api` - Spring Boot Java API
- `apps/web/ticket-order-web` - React/Vite web app
- `contracts/openapi/ticket-order-api` - OpenAPI contract for the Ticket Service API
- `database/java/migrations/ticket-order-db-migrations` - Flyway database migrations for Java services
- `Makefile` - root development entrypoint
- `docker-compose.yml` - local container stack

## Development

- Create every new work branch from `main` unless the user explicitly asks for a stacked branch.
- When the user says to push a branch, push it and create or open a pull request unless the user explicitly says to push only.
- Every commit message must follow `MODULE_NAME#123 - short description`.
- Pull request titles must be identical to the branch's commit message when the branch has a single commit.
- Ticket numbers are scoped per module. For example, `GLOBAL_CONFIG#1`, `TICKET_SERVICE#1`, and `TICKET_PORTAL#1` can all exist independently.
- Use `GLOBAL_CONFIG` as the module name for repository-wide configuration, guidance, and workflow changes.
- If there is no real ticket number, ask the user for one before committing.
- Update changelogs before every commit using the exact commit message.
- The root `CHANGELOG.md` must include every change grouped by module.
- Each module-level `CHANGELOG.md` must include only changes for that module.
- Keep modules independent. Do not mix Java API and web app changes unless the task requires cross-service behavior.
- Contract-first API changes must update `contracts/openapi/ticket-order-api/openapi.yml` before generated code or implementations.
- Java service database schema changes must be implemented as Flyway migrations in `database/java/migrations/ticket-order-db-migrations`.
- Transactional migrations must stay separate from analytical migrations, and transactional tables that support queries must have CQRS read views.
- Java database migrations must use `ticket_transactional` and `ticket_analytical` schemas, `t_` prefixes for transactional tables, and entity names for analytical CQRS views.
- Prefer root Makefile targets over ad hoc commands.
- Keep shared commands at the root and service-specific commands inside the service README.
- Keep generated output out of git. Do not commit `target`, `dist`, or `node_modules`.
- Add a scoped `AGENTS.md` for every new service or app module.

## Test

- Do not run tests after every individual change.
- Run tests before pushing a branch to remote, before creating a PR, or when the user explicitly asks.
- When testing is needed, run the smallest relevant test target first.
- For Java API changes, run `make test-api`.
- For web changes, run `make test-web`.
- For contract changes, run `make validate-contracts`.
- For database migration changes, run `make package-migrations`; when a database is available, run `make migrate-transactional` and then `make migrate-analytical`.
- For cross-module changes, run `make test`.
- For Docker or Compose changes, run `docker compose config` and `make docker-build` when Docker is available.

## Deploy

- Build service images through the root Makefile.
- Use `make docker-build` for all images.
- Use `make compose-build` to validate Compose image builds.
- Use `make compose-up` for local stack verification and `make compose-down` when finished.

## Next Iterations

- New backend modules should follow the Java API split: domain, application, adapters, infrastructure.
- New frontend modules should follow the web app split: source, build scripts, Dockerfile, service README.
- New services must include development, test, and deploy instructions in their own `AGENTS.md`.
- New API contracts must live under `contracts/openapi/*` and expose generation targets through the root Makefile.
- New database migration modules must be scoped under the owning runtime or service layer and separate transactional migrations from analytical read-model migrations.
- Update root Makefile targets when a new module needs to participate in build, test, Docker, or Compose workflows.
