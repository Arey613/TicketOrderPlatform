# Project Map

Fast global navigation guide for Codex and contributors. Use this only to choose the right module or validation path; module-specific navigation lives inside each module.

## Repository Shape

| Area | Path | Purpose |
| --- | --- | --- |
| Root workflow | `Makefile` | Common build, test, generation, migration, Docker, and Compose entrypoints. Prefer these targets over ad hoc commands. |
| Repository guidance | `AGENTS.md` | Global agent rules for module boundaries, commits, changelogs, contracts, migrations, and validation. |
| Java API | `services/java/ticket-order-api` | Spring Boot Ticket Service API using hexagonal architecture. See `services/java/ticket-order-api/NAVIGATION.md`. |
| Web app | `apps/web/ticket-order-web` | React/Vite frontend. See `apps/web/ticket-order-web/NAVIGATION.md`. |
| API contract | `contracts/openapi/ticket-order-api` | OpenAPI source of truth for generated API/server/client code. See `contracts/openapi/ticket-order-api/NAVIGATION.md`. |
| Java DB migrations | `database/java/migrations/ticket-order-db-migrations` | Flyway migration modules for Java service schemas. See `database/java/migrations/ticket-order-db-migrations/NAVIGATION.md`. |
| Documentation | `docs` | Cross-repository notes and navigation docs. |

## First Files To Read

When working in a module, read the closest scoped `AGENTS.md` first:

| Task type | Start here |
| --- | --- |
| Any repository-wide change | `AGENTS.md` |
| Java API behavior | `services/java/ticket-order-api/AGENTS.md`, then `services/java/ticket-order-api/NAVIGATION.md` |
| Frontend behavior | `apps/web/ticket-order-web/AGENTS.md`, then `apps/web/ticket-order-web/NAVIGATION.md` |
| Contract-first API change | `contracts/openapi/ticket-order-api/AGENTS.md`, then `contracts/openapi/ticket-order-api/NAVIGATION.md` |
| Database schema or Flyway change | `database/java/migrations/ticket-order-db-migrations/AGENTS.md`, then `database/java/migrations/ticket-order-db-migrations/NAVIGATION.md` |

## Common Commands

Run from repository root unless noted.

| Need | Command |
| --- | --- |
| All tests | `make test` |
| Java API tests | `make test-api` |
| Web validation | `make test-web` |
| Package migrations | `make package-migrations` |
| Validate OpenAPI | `make validate-contracts` |
| Generate API and web contract code | `make generate` |
| Generate Java API contract code | `make generate-api-contracts` |
| Generate web API client | `make generate-web-contracts` |
| Run Java API | `make run-api` |
| Install web dependencies | `make install-web` |
| Run web dev server | `make run-web` |
| Apply transactional migrations | `make migrate-transactional` |
| Apply analytical migrations | `make migrate-analytical` |

Docker and Compose targets exist in the root `Makefile`, but require task-specific permission before use.

## Change Routing

| Change | Usual path |
| --- | --- |
| New or changed API endpoint | Update `contracts/openapi/ticket-order-api/openapi.yml`, generate code, then update API/web consumers. |
| New Java API use case | Work in `services/java/ticket-order-api`; use its local navigation file. |
| New persistence behavior | Add/extend API outbound ports and adapters, then add migrations if schema changes. |
| New schema object | Add Flyway migration in the appropriate transactional or analytical child module. |
| New read/query model | Add transactional support if needed, then add or update analytical repeatable CQRS view. |
| Frontend UI change | Work in `apps/web/ticket-order-web`; use its local navigation file. |
| Cross-module behavior | Validate contract, API, web, and migrations with the smallest relevant Make targets first. |
