# AGENTS.md

Agent guidance for the `ticket-order-web` app.

## Scope

This file applies to `apps/web/ticket-order-web`.

## Development

- Commit messages for this module must follow `TICKET_PORTAL#123 - short description`.
- Update this module's `CHANGELOG.md` before every commit that changes this module.
- Also update the root `CHANGELOG.md` under the `ticket-order-web` module section.
- Use React 19, Vite 8, and npm.
- Keep UI source under `src`.
- Prefer small components with clear ownership.
- Keep app entrypoint logic in `src/main.jsx`.
- Keep top-level app composition in `src/App.jsx` until the app grows enough to justify routing or feature folders.
- Keep styling readable and local to the module. Avoid committing generated `dist` output.
- Generate API clients from `contracts/openapi/ticket-order-api/openapi.yml`.
- Do not edit generated OpenAPI client files by hand.

## Test

- Install dependencies from the repository root when needed:

```sh
make install-web
```

- Run the web validation target from the repository root:

```sh
make test-web
```

- For API client generation changes, run from the repository root:

```sh
make generate-web-contracts
```

- `npm run test` currently runs a production Vite build. Add a dedicated test runner only when there is real component or behavior coverage to write.

## Deploy

- Build the web image from the repository root:

```sh
make docker-build-web
```

- Run with Compose from the repository root:

```sh
make compose-up
```

- The container serves static files with Nginx on port `80`.
- Compose publishes the web app on `localhost:5173`.
- Keep Nginx fallback behavior when client-side routing is introduced.

## Next Iterations

- Add routing only when the product has multiple real views.
- Add API client code in a dedicated module or feature folder once the web app calls the Java API.
- Keep API URLs configurable for local, Compose, and deployed environments.
- Update this file whenever new frontend conventions, tools, or test targets are introduced.
