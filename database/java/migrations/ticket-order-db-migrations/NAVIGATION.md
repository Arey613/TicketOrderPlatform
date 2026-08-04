# Database Migration Navigation

Fast local guide for Java service migration work. Read this module's `AGENTS.md` first.

## Start Here

| Need | Path |
| --- | --- |
| Parent build | `pom.xml` |
| Transactional module | `ticket-order-transactional-migrations` |
| Analytical module | `ticket-order-analytical-migrations` |
| Migration runner image | `Dockerfile` |

## Layout

| Concern | Path |
| --- | --- |
| Transactional versioned SQL | `ticket-order-transactional-migrations/src/main/resources/db/migrations` |
| Transactional repeatable SQL | `ticket-order-transactional-migrations/src/main/resources/db/repeatable` |
| Analytical versioned SQL | `ticket-order-analytical-migrations/src/main/resources/db/migrations` |
| Analytical repeatable SQL | `ticket-order-analytical-migrations/src/main/resources/db/repeatable` |

## Search

| Goal | Command |
| --- | --- |
| Files | `rg --files` |
| Versioned migrations | `rg --files -g 'V*.sql' ticket-order-*-migrations/src/main/resources/db/migrations` |
| Repeatable migrations | `rg --files -g 'R__*.sql' ticket-order-*-migrations/src/main/resources/db/repeatable` |
| DDL | `rg -n "CREATE|ALTER|DROP|TABLE|VIEW|INDEX|SCHEMA" ticket-order-*-migrations/src/main/resources/db` |
| Flyway config | `rg -n "flyway|migration.schema|locations|schemas" pom.xml ticket-order-*-migrations/pom.xml` |

## Commands

Run from the repository root.

| Need | Command |
| --- | --- |
| Package migrations | `make package-migrations` |
| Run transactional migrations | `make migrate-transactional` |
| Run analytical migrations | `make migrate-analytical` |

Docker and Compose require task-specific permission.

## Rules Of Thumb

- Transactional schema is `ticket_transactional`; analytical schema is `ticket_analytical`.
- Transactional tables use `t_` prefixes.
- Add new versioned migrations instead of editing committed versioned migrations.
- Repeatable views may be updated.
- Run transactional migrations before analytical migrations.
