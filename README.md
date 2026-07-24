# TicketOrderPlatform

Boring Ticket Order.

## Structure

This repository is organized as a multi-module, multi-language workspace.

- `services/java/ticket-order-api` - Spring Boot HTTP API
- `apps/web/ticket-order-web` - React/Vite web app

## Build

Use the root Makefile as the common entrypoint:

```sh
make build
make test
make run
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
