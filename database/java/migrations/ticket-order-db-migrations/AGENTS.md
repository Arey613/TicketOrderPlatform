# AGENTS.md

Guidance for agents working in `database/java/migrations/ticket-order-db-migrations`.

## Development

- Use `DATABASE_MIGRATIONS` as the commit module name.
- Keep this module scoped to Java service databases; create a separate runtime or service layer for future non-Java database migration modules.
- Keep transactional versioned migrations under `ticket-order-transactional-migrations/src/main/resources/db/migrations`.
- Keep transactional repeatable migrations under `ticket-order-transactional-migrations/src/main/resources/db/repeatable`.
- Keep analytical versioned migrations under `ticket-order-analytical-migrations/src/main/resources/db/migrations`.
- Keep analytical repeatable migrations under `ticket-order-analytical-migrations/src/main/resources/db/repeatable`.
- Keep a separate `CHANGELOG.md` in each schema-specific migration module.
- Package each schema stream as its own Maven jar module.
- Configure Flyway plugin locations and schemas in each child module POM; do not require profiles or migration paths in root commands.
- Define each child module's Flyway schema through the `migration.schema` Maven property.
- Use `ticket_transactional` for transactional schemas and `ticket_analytical` for analytical schemas.
- Prefix transactional table names with `t_`.
- Name CQRS views after the entity/table concept without a view prefix.
- Keep transactional and analytical view definitions in repeatable Flyway migrations using `R__view_name.sql`.
- Every transactional table that supports query behavior must have an analytical CQRS view unless the task explicitly excludes it.
- Java service database schema changes must be implemented in this module.
- Use `V1.0000__create_<schema>_schema.sql` for initial schema creation.
- Use Flyway versions in the `V1.0001__description.sql` pattern for schema DDL after initial schema creation.
- Transactional and analytical migrations are separate version streams; increment versions only within the same migration path/schema.
- Do not mutate an existing versioned Flyway migration after it has been committed; add a new versioned migration instead.
- Repeatable view migrations may be updated when the view definition changes.
- Run transactional migrations before analytical migrations.
- In Docker Compose, model migrations as one-shot services that depend on database health. Java services must depend on successful migration completion before startup.

## Test

- Build the module with `make package-migrations`.
- Validate migrations against a database with `make migrate-transactional` followed by `make migrate-analytical` when database access is available.

## Deploy

- Apply transactional migrations before deploying services that write to the schema.
- Apply analytical migrations after transactional migrations and before enabling read-side consumers.
