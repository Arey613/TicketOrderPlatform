# TICKET_SERVICE#8 - Read Replica CQRS Datasource Split

## Goal

Separate Ticket Service API database access into explicit write-side and read-side datasources.

The service must use:

```text
primary datasource
read-replica datasource
```

The primary datasource is the transactional source of truth. The read-replica datasource is the preferred datasource for API query behavior that does not require immediate write-after-read consistency from the primary database.

The API must not own analytical database behavior in this ticket.

## Context

The Java API currently has explicit primary and analytical datasource configuration. That naming is misleading for the API boundary.

For the API, CQRS means separating command persistence from query persistence:

```text
commands -> primary datasource
queries  -> read-replica datasource
```

Analytical storage, reporting projections, and ETL processing are separate concerns and will be owned by a separate future service. This ticket only defines API runtime access to the transactional primary database and its read replica.

## Scope

This feature includes:

- Renaming the API-side secondary datasource concept from analytical to read replica.
- Keeping the primary datasource as the source of truth for transactional writes.
- Routing API command/write repositories through the primary datasource.
- Routing eligible API query/read repositories through the read-replica datasource.
- Falling back to the primary datasource when the read-replica connection cannot be opened.
- Keeping login, registration, password matching, session-related persistence, and authorization-sensitive reads on the primary datasource.
- Updating API configuration names, environment variable names, tests, and service documentation to use read-replica terminology.
- Removing or renaming API runtime references to analytical terminology when they are not required by this service.
- Preserving current public HTTP API behavior.

This feature does not include:

- ETL jobs.
- Analytical service creation.
- Analytical database design.
- Reporting models.
- Reporting APIs.
- Event streaming or change-data-capture infrastructure.
- Database replication provisioning.
- Database schema changes.
- Analytical migration module changes.
- OpenAPI contract changes.
- Frontend changes.
- Moving authentication to the read replica.

## Datasource Model

The API must define two datasource roles:

```text
primary datasource -> transactional writes and consistency-sensitive reads
read-replica datasource -> API query reads
```

Both datasource roles point to the same database structure. The read replica must expose the same transactional schema shape as the primary database, but it should be configured as read-only at the database/user/connection level where the deployment supports that.

The primary datasource is mandatory. The read-replica datasource is also configured explicitly, but the application must be able to continue serving eligible read queries from the primary datasource when the read replica is unavailable.

When datasource intent is unknown or cannot be classified safely, the API must use the primary datasource.

The API must not expose an application datasource named analytical.

## Configuration

Spring configuration should use:

```yaml
spring:
  datasource:
    primary:
      url: jdbc:postgresql://localhost:5432/ticket_order
      username: ticket_order
      password: ticket_order
    read-replica:
      url: jdbc:postgresql://localhost:5432/ticket_order
      username: ticket_order
      password: ticket_order
```

Runtime environment variables should use:

```text
SPRING_DATASOURCE_PRIMARY_URL
SPRING_DATASOURCE_PRIMARY_USERNAME
SPRING_DATASOURCE_PRIMARY_PASSWORD

SPRING_DATASOURCE_READ_REPLICA_URL
SPRING_DATASOURCE_READ_REPLICA_USERNAME
SPRING_DATASOURCE_READ_REPLICA_PASSWORD
```

Local development may point both datasource roles at the same PostgreSQL database. Production-like deployments should point the read-replica role at a database replica when one exists.

Old API configuration names that use analytical terminology must be removed from the API runtime configuration. Do not keep `SPRING_DATASOURCE_ANALYTICAL_*` compatibility variables in this service.

## Command Routing

The following behavior must use the primary datasource:

1. User registration.
2. Login and password matching.
3. Session-related user loading where stale user state could weaken access control.
4. Event creation.
5. Event updates.
6. Event publish and unpublish operations.
7. Event order creation and reservation writes.
8. Any operation that mutates transactional state.
9. Any read that is part of validating a write command.
10. Any persistence operation whose datasource intent is unknown or cannot be classified safely.

Command handlers and write persistence adapters must not depend on the read-replica datasource.

## Query Routing

All API repository reads should use the read-replica datasource by default, except login/authentication functionality and reads that are part of write-command validation.

Eligible API read behavior includes:

1. Published event browsing.
2. Event detail reads that are not part of a write command.
3. User-facing ticket/order reads after the order has been committed.
4. Manager event list/detail reads for display screens where replica lag is acceptable.
5. User profile reads that are not used for login, credential validation, or authorization-sensitive decisions.
6. Other read-only use cases that are not part of login functionality or write-command validation.

Query handlers must not perform writes through the read-replica datasource.

Manager/admin reads that support display, filtering, or inspection can use the read replica. Ownership checks, permission checks, and command precondition reads must use the primary datasource when they guard a mutation.

Strong read-after-write consistency for query screens is not part of this ticket. Such cases should be handled by a later cache or consistency strategy instead of widening primary read routing.

## Authentication And Authorization

Authentication remains primary-bound.

The API must use the primary datasource for:

1. Password hash lookup.
2. Credential validation.
3. Login success state.
4. Registration uniqueness checks.
5. User role and enabled-state checks when used for access control.

The reason is consistency and security. A stale read replica must not allow disabled users, stale roles, or recently changed credentials to affect authentication or authorization decisions.

Read visibility, schema ownership expectations, and data-access security must be equivalent between the primary datasource and read-replica datasource. The read replica should use stricter write privileges, but it must not expose data that the primary datasource would not expose to the API.

## Fallback Behavior

Eligible read queries should try the read-replica datasource first.

If the read-replica connection cannot be opened or the read replica becomes unavailable because of a connection/resource problem, the read query must use the primary datasource instead.

Fallback must be implemented at the read-query execution boundary, not as a blanket replacement for the read-replica datasource bean.

Fallback rules:

1. Fallback applies only to read queries.
2. Fallback is triggered by connection acquisition failure, connection loss, pool exhaustion caused by replica unavailability, or equivalent Spring/Hibernate datasource resource failures.
3. Fallback must not handle SQL grammar errors, constraint errors, missing tables, invalid mappings, malformed queries, permission errors, or business data problems.
4. Fallback must not retry writes on the primary after attempting the read replica, because writes must never target the read replica.
5. The system should log and meter read-replica fallback so operational degradation is visible.
6. The client response shape must not change only because fallback occurred.
7. Unknown datasource intent is not a fallback case; it must route directly to primary.

Fallback is an availability mechanism, not a consistency guarantee.

## Repository Boundary

The implementation should make repository intent explicit:

```text
write repositories -> primary datasource
read repositories  -> read-replica datasource with primary fallback
```

Adapters and repository classes should be separated by functionality, not only by database technology. Existing persistence adapters that combine command and query behavior must be split when they support both write-side and read-side use cases.

The split should follow the existing hexagonal boundary:

```text
application port -> persistence adapter -> datasource-specific repository
```

Recommended direction:

```text
event command port -> event command persistence adapter -> primary event repositories
event query port   -> event query persistence adapter   -> read-replica event repositories
user command port  -> user command persistence adapter  -> primary user repositories
user auth port     -> user auth persistence adapter     -> primary user repositories
user query port    -> user query persistence adapter    -> read-replica user repositories when replica-safe
```

Do not keep one broad persistence adapter or repository as the long-term boundary when it mixes command writes, command precondition reads, authentication reads, and display queries.

Do not pass generated OpenAPI models into persistence adapters.

## Runtime Read Routing

Read-side runtime datasource selection should be implemented behind infrastructure-owned routing, not inside application services.

The recommended implementation shape is:

```text
read query adapter
  -> read execution/fallback component
    -> try read-replica repository operation
    -> if replica connection/resource failure, run equivalent primary repository operation
```

The application service should call a query port and should not decide whether the read replica is available. The persistence adapter owns that decision because datasource availability is infrastructure behavior.

The fallback implementation must not call both databases during normal successful reads. The primary read operation is executed only after the read-replica operation fails with a fallback-eligible connection/resource problem.

Avoid broad factory methods that return a mutable current repository to application code. A factory that leaks datasource choice upward makes command/query routing harder to reason about and easier to misuse. If a factory is used, keep it private to the persistence adapter and expose only intent-based methods such as `findPublishedEvents`.

The fallback component should classify exceptions narrowly:

```text
fallback allowed    -> connection/resource unavailable
fallback disallowed -> SQL grammar, mapping, permission, constraint, and domain/data errors
```

The fallback component must be unit tested independently from concrete repository implementations.

One acceptable implementation pattern is to pass the fallback component two lazy operations:

```text
execute(replicaReadOperation, primaryReadOperation)
```

The component must execute the primary operation only when the replica operation fails with a fallback-eligible connection/resource failure.

## Transaction Strategy

Current service-level `@Transactional(readOnly = true)` behavior must be changed for query use cases because it binds to the default primary transaction manager before the read adapter can choose the read replica.

Implementation must use an explicit transaction strategy:

1. Command services keep primary-bound `@Transactional` behavior.
2. Authentication services use primary-bound transactions.
3. Query application services should not open a default primary transaction before calling read-replica adapters.
4. Read-query adapters open a read-only transaction with the read-replica transaction manager for the first attempt.
5. If the replica attempt fails with a fallback-eligible connection/resource failure, the read-query adapter opens a separate read-only transaction with the primary transaction manager and executes the equivalent primary read.
6. A read-only query must not accidentally bind to the primary datasource unless fallback was selected.
7. The same query must not run against both datasources during normal successful replica reads.
8. Unknown datasource intent must use the primary transaction manager.

Do not rely on one generic default transaction manager for both command and query paths once two datasource roles are active.

Recommended implementation:

```text
command service method
  -> @Transactional(primaryTransactionManager)
  -> command port
  -> primary adapter/repository

query service method
  -> no default primary transaction
  -> query port
  -> query adapter
    -> read executor opens readReplicaTransactionManager read-only transaction
    -> on eligible connection/resource failure, opens primaryTransactionManager read-only transaction
```

If method annotations are used for query transactions, they must name the read-replica transaction manager explicitly and must still allow fallback to start a separate primary read transaction. Prefer keeping fallback transaction demarcation inside the read execution component so application services remain datasource-agnostic.

## Analytical Boundary

The Ticket Service API must not own analytical database access for this ticket.

The following are out of scope for the API:

1. Analytical datasource naming.
2. Analytical schema access.
3. ETL logic.
4. Reporting projections.
5. Reporting tables.
6. Analytical-service orchestration.

A separate future service may own analytical ingestion and reporting. That service may read from transactional data and write to analytical storage, but this ticket does not design or implement it.

If an API reference to analytical terminology only exists because of the previous API datasource naming, rename it to read-replica terminology or remove it. Keep analytical references only in historical specs, changelog history, or database-migration modules that are outside the API runtime scope.

Existing analytical migration modules remain unchanged in this ticket. They are reserved for another functional area and are not part of the API read/write datasource split.

## Observability

Read-replica fallback should be visible to operators.

Minimum observability expectations:

1. Log one warning when a read query falls back from read replica to primary because the replica connection cannot be opened.
2. Do not log credentials, JDBC URLs with embedded credentials, session cookies, CSRF tokens, or authorization headers.
3. Avoid duplicate exception stack traces.
4. Add a metric for read-replica fallback count.

## Testing

Implementation should include focused tests for:

1. Primary datasource remains the default write datasource.
2. Read-replica datasource is configured with `spring.datasource.read-replica`.
3. API no longer exposes an analytical datasource bean or API runtime configuration as the main secondary datasource concept.
4. Eligible read repositories use the read-replica datasource.
5. Write repositories use the primary datasource.
6. Authentication reads use the primary datasource.
7. A read-replica connection acquisition failure falls back to the primary datasource for read queries.
8. SQL/query failures after a read-replica connection is opened are not converted into primary fallback.
9. Fallback does not change HTTP response models.
10. Command and query persistence adapters are separated where a current adapter mixes primary writes with replica-safe reads.
11. Read transaction manager usage is explicit and does not accidentally route read-replica queries through the default primary transaction manager.
12. Existing analytical migration modules are not renamed or structurally changed.

## Acceptance Criteria

1. API configuration uses `primary` and `read-replica` datasource names.
2. API runtime code no longer uses analytical terminology for its secondary datasource.
3. Writes and write-validation reads use the primary datasource.
4. Eligible read-only query use cases use the read-replica datasource.
5. Read-replica connection failure falls back to the primary datasource.
6. Authentication and authorization-sensitive reads remain on the primary datasource.
7. ETL and analytical service behavior are not implemented.
8. Public API contracts remain unchanged.
9. Persistence adapters and repositories are separated by command, query, and authentication responsibilities where those responsibilities require different datasource routing.
10. The read replica uses the same database structure as primary and is treated as read-only.
11. Fallback is implemented through a tested read execution policy that handles connection/resource failures but not SQL/query errors.
