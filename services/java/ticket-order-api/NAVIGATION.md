# Java API Navigation

Fast local guide for Java-only work. Read this module's `AGENTS.md` first.

## Start Here

| Need | Path |
| --- | --- |
| App entrypoint | `src/main/java/com/example/ticketplatform/api/TicketOrderApiApplication.java` |
| Runtime config | `src/main/resources/application.yml` |
| Tests | `src/test/java/com/example/ticketplatform/api` |
| Module build | `pom.xml` |

Root package: `com.example.ticketplatform.api`

## Layout

| Concern | Path |
| --- | --- |
| HTTP adapters | `src/main/java/com/example/ticketplatform/api/adapter/in/web` |
| Security adapter | `src/main/java/com/example/ticketplatform/api/adapter/in/security` |
| Inbound ports | `src/main/java/com/example/ticketplatform/api/application/port/in` |
| Outbound ports | `src/main/java/com/example/ticketplatform/api/application/port/out` |
| Services | `src/main/java/com/example/ticketplatform/api/application/service` |
| Domain | `src/main/java/com/example/ticketplatform/api/domain/model` |
| Persistence | `src/main/java/com/example/ticketplatform/api/adapter/out/persistence` |
| Spring config | `src/main/java/com/example/ticketplatform/api/infrastructure/config` |

## Search

| Goal | Command |
| --- | --- |
| Files | `rg --files` |
| Java types | `rg -n "class|interface|record|enum" src/main/java` |
| Controllers | `rg -n "@RestController|@.*Mapping" src/main/java` |
| Ports/use cases | `rg -n "UseCase|RepositoryPort|interface .*Port" src/main/java` |
| Persistence/mappers | `rg -n "JpaRepository|@Entity|@Mapper|@Mapping" src/main/java src/test/java` |
| Config/security/CORS | `rg -n "@Configuration|SecurityFilterChain|Cors|application.yml" src/main src/test` |

## Commands

Run from the repository root.

| Need | Command |
| --- | --- |
| Test API | `make test-api` |
| Package API | `make package-api` |
| Run API | `make run-api` |
| Generate contract sources | `make generate-api-contracts` |

Docker requires task-specific permission.

## Rules Of Thumb

- Controllers call application ports.
- Application services own use case behavior.
- Domain stays framework-free.
- Persistence goes behind outbound ports and `adapter/out/*`.
- Do not edit generated OpenAPI sources by hand.
