# ticket-order-db-migrations

Flyway migration module for TicketOrderPlatform Java service database changes.

## Structure

- `ticket-order-transactional-migrations` - transactional write-model schema migrations packaged as a jar
- `ticket-order-analytical-migrations` - analytical read-model and CQRS view migrations packaged as a jar

Transactional migrations must run before analytical migrations because analytical views can depend on transactional tables.
Transactional and analytical migrations use separate version sequences because they are applied from separate paths into separate schemas.
Each schema-specific module has its own `CHANGELOG.md`.

## Migration Types

- Initial schema creation uses `V1.0000__create_<schema>_schema.sql`.
- Versioned DDL migrations after schema creation use `V1.0001__description.sql`.
- Transactional and analytical CQRS views use repeatable migrations such as `R__app_users.sql`.
- Repeatable view migrations are reapplied by Flyway when their checksum changes.

## Build

Package the migrations from the repository root:

```sh
make package-migrations
```

Build migration runner images through Docker Compose from the repository root:

```sh
make compose-build
```

## Migrate

Set the Flyway connection environment variables:

```sh
export FLYWAY_URL=jdbc:postgresql://localhost:5432/ticket_order
export FLYWAY_USER=ticket_order
export FLYWAY_PASSWORD=ticket_order
```

Run transactional migrations first:

```sh
make migrate-transactional
```

Run analytical migrations after transactional migrations:

```sh
make migrate-analytical
```

Each migration jar configures its own Flyway plugin location and schema, so migration commands do not pass profiles or migration paths.

## Docker Compose

The root Compose stack runs both migration services after PostgreSQL is healthy. The API waits for both migration services to complete successfully before it starts.
