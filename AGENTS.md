# AGENTS.md

Repository-level guidance for agents working on TicketOrderPlatform.

## Scope

This file applies to the whole repository. More specific `AGENTS.md` files inside modules override this file for that module.

## Project Layout

- `services/java/ticket-order-api` - Spring Boot Java API
- `apps/web/ticket-order-web` - React/Vite web app
- `Makefile` - root development entrypoint
- `docker-compose.yml` - local container stack

## Development

- Create every new work branch from `main` unless the user explicitly asks for a stacked branch.
- Every commit message must follow `moduleNameChange#123 - short description`.
- `moduleNameChange#123` means the module or scope name, a concise change label, and the related ticket number.
- Use `GLOBAL_CONGIG` as the module name for repository-wide configuration, guidance, and workflow changes.
- If there is no real ticket number, ask the user for one before committing.
- Update changelogs before every commit using the exact commit message.
- The root `CHANGELOG.md` must include every change grouped by module.
- Each module-level `CHANGELOG.md` must include only changes for that module.
- Keep modules independent. Do not mix Java API and web app changes unless the task requires cross-service behavior.
- Prefer root Makefile targets over ad hoc commands.
- Keep shared commands at the root and service-specific commands inside the service README.
- Keep generated output out of git. Do not commit `target`, `dist`, or `node_modules`.
- Add a scoped `AGENTS.md` for every new service or app module.

## Test

- Run the smallest relevant test target first.
- For Java API changes, run `make test-api`.
- For web changes, run `make test-web`.
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
- Update root Makefile targets when a new module needs to participate in build, test, Docker, or Compose workflows.
