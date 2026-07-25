# AGENTS.md

Repository-level guidance for agents working on TicketOrderPlatform.

## Scope

This file applies to the whole repository. More specific `AGENTS.md` files inside modules override this file for that module.

## Project Layout

- `services/java/ticket-order-api` - Spring Boot Java API
- `apps/web/ticket-order-web` - React/Vite web app
- `contracts/openapi/ticket-order-api` - OpenAPI contract for the Ticket Service API
- `Makefile` - root development entrypoint
- `docker-compose.yml` - local container stack

## Development

- Create every new work branch from `main` unless the user explicitly asks for a stacked branch.
- Every commit message must follow `MODULE_NAME#123 - short description`.
- Ticket numbers are scoped per module. For example, `GLOBAL_CONFIG#1`, `TICKET_SERVICE#1`, and `TICKET_PORTAL#1` can all exist independently.
- Use `GLOBAL_CONFIG` as the module name for repository-wide configuration, guidance, and workflow changes.
- If there is no real ticket number, ask the user for one before committing.
- Update changelogs before every commit using the exact commit message.
- The root `CHANGELOG.md` must include every change grouped by module.
- Each module-level `CHANGELOG.md` must include only changes for that module.
- Keep modules independent. Do not mix Java API and web app changes unless the task requires cross-service behavior.
- Contract-first API changes must update `contracts/openapi/ticket-order-api/openapi.yml` before generated code or implementations.
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
- Update root Makefile targets when a new module needs to participate in build, test, Docker, or Compose workflows.
