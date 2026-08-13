# AGENTS.md

Repository-level guidance for agents working on TicketOrderPlatform.

## Scope

This file applies to the whole repository. More specific `AGENTS.md` files inside modules override this file for that module.
Use `docs/PROJECT_MAP.md` for fast module routing before scanning broadly.

## Project Layout

- `services/java/ticket-order-api` - Spring Boot Java API
- `apps/web/ticket-order-web` - React/Vite web app
- `contracts/openapi/ticket-order-api` - OpenAPI contract for the Ticket Service API
- `database/java/migrations/ticket-order-db-migrations` - Flyway database migrations for Java services
- `Makefile` - root development entrypoint
- `docker-compose.yml` - local container stack

## Development

- Create every new work branch from the latest `origin/main` unless the user explicitly asks for a stacked branch. Before branching, fetch the remote and update local `main` so the branch starts from the current remote mainline.
- When the user says to push a branch, push it and create or open a pull request unless the user explicitly says to push only.
- For updates to an existing pull request, keep changes local until the user explicitly asks to push, unless the user has already requested a push or PR-update workflow for that turn.
- Do not amend, squash, or otherwise rewrite existing pull request commits during review iteration unless the user explicitly asks; squash only as part of the merge workflow when requested or selected.
- When fixing PR review comments or proving that the current implementation is correct, reply directly under the relevant review comment. If the requested implementation is completed as required, close or resolve the comment where the platform supports it.
- Every commit message must follow `MODULE_NAME#123 - short description`.
- Pull request titles must be identical to the branch's commit message when the branch has a single commit.
- Ticket numbers are scoped per module. For example, `GLOBAL_CONFIG#1`, `TICKET_SERVICE#1`, and `TICKET_PORTAL#1` can all exist independently.
- Use `GLOBAL_CONFIG` as the module name for repository-wide configuration, guidance, and workflow changes.
- If there is no real ticket number, ask the user for one before committing.
- Update changelogs before every commit using the exact commit message.
- The root `CHANGELOG.md` must include every change grouped by module.
- Each module-level `CHANGELOG.md` must include only changes for that module.
- Keep modules independent. Do not mix Java API and web app changes unless the task requires cross-service behavior.
- Follow scoped module guidance for contract, database, API, and web implementation rules.
- Prefer root Makefile targets over ad hoc commands.
- Keep shared commands at the root and service-specific commands inside the service README.
- Before installing dependencies or downloading tools from package registries, ask the user in chat and wait for explicit approval, even if tool escalation is available.
- Keep generated output out of git. Do not commit `target`, `dist`, or `node_modules`.
- Add a scoped `AGENTS.md` for every new service or app module.
- For class fields and local variables inside methods, place annotations on their own line above the variable declaration. This rule does not apply to method, constructor, lambda, or record input parameters.
- When a method, constructor, lambda, or record accepts four or more parameters, format the parameters vertically, one per line.

## Test

- Do not run tests after every individual change.
- Run tests before pushing a branch to remote, before creating a PR, or when the user explicitly asks.
- When testing is needed, run the smallest relevant test target first.
- Use scoped module guidance for module-specific validation commands.
- For cross-module changes, run `make test`.
- For Docker or Compose changes, run `docker compose config` and `make docker-build` when Docker is available.

## Deploy

- Use root Makefile targets for shared Docker and Compose workflows.
- Follow scoped module guidance for module image and runtime details.

## Next Iterations

- New services must include development, test, and deploy instructions in their own `AGENTS.md`.
- New modules should follow the closest existing module's scoped guidance unless the task requires a new convention.
- Update root Makefile targets when a new module needs to participate in build, test, Docker, or Compose workflows.
