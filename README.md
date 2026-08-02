# TicketOrderPlatform

Boring Ticket Order.

## Structure

This repository is organized as a multi-module, multi-language workspace.

- `services/java/ticket-order-api` - Spring Boot HTTP API
- `apps/web/ticket-order-web` - React/Vite web app
- `contracts/openapi/ticket-order-api` - OpenAPI contract for the Ticket Service API
- `database/java/migrations/ticket-order-db-migrations` - Flyway database migrations for Java services

Each module has its own README with local setup and architecture notes.

## Build

Use the root Makefile as the common entrypoint:

```sh
make build
make test
make run
```

Package database migrations:

```sh
make package-migrations
```

Validate and generate from OpenAPI contracts:

```sh
make validate-contracts
make generate
```

The API exposes:

```text
GET /hello
```

The web app can be started with:

```sh
make install-web
make run-web
```

## Docker

Build both images:

```sh
make docker-build
```

Run the full stack with Docker Compose:

```sh
make compose-up
```

Compose starts PostgreSQL, runs migration services after PostgreSQL is healthy, and starts the API after migrations complete successfully.

Stop the stack:

```sh
make compose-down
```
