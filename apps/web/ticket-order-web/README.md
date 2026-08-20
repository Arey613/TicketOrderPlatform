# ticket-order-web

React and Vite web module for TicketOrderPlatform.

## Stack

- React 19
- Vite 8
- npm

## Structure

```text
src/App.tsx       Main application composition
src/main.tsx      React entrypoint
src/index.css     Tailwind imports and global defaults
src/api           Handwritten API wrappers around generated clients
src/features      Feature-owned UI components
tests/unit        Vitest unit and component tests
tests/e2e         Playwright browser tests
tests/support     Shared test setup, data, and helpers
vite.config.ts    Vite and Vitest configuration
```

## Commands

From the repository root:

```sh
make install-web
make generate-web-contracts
make test-web
make package-web
make run-web
```

From this module directory:

```sh
npm install
npm run generate:api
npm run test
npm run build
npm run dev
```

## OpenAPI Generation

The API contract lives in:

```text
../../../contracts/openapi/ticket-order-api/openapi.yml
```

Generate the TypeScript API client from the repository root:

```sh
make generate-web-contracts
```

Generated client files are written under `src/generated/api` and are not committed.
Run generation before starting or building from a fresh checkout.

## Docker

Build from the repository root:

```sh
docker build -f apps/web/ticket-order-web/Dockerfile -t ticket-order-web .
```

Run:

```sh
docker run --rm -p 5173:80 ticket-order-web
```

## Local Development

The Vite development server runs on:

```text
http://localhost:5173
```

The web app calls the Java API at `http://localhost:8080` by default. Override it with:

```sh
VITE_TICKET_API_BASE_URL=http://localhost:8080 npm run dev
```

The Java API CORS configuration allows this origin with credentials for session auth.
