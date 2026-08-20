# ticket-order-api

Spring Boot HTTP API for TicketOrderPlatform.

## Stack

- Java 25
- Spring Boot 4.1
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL
- Maven

## Architecture

The module follows a hexagonal architecture package layout:

- `domain/model` - framework-free domain objects
- `application/port/in` - inbound use case contracts
- `application/service` - use case implementations
- `adapter/in/web` - HTTP controllers that call application ports
- `adapter/in/security` - Spring Security entrypoints backed by application ports
- `adapter/out/persistence` - JPA persistence adapters that implement outbound ports
- `infrastructure/config` - Spring, CORS, and security configuration

## Endpoints

```text
GET /actuator/health
```

Returns:

```text
Service health details
```

## Commands

From the repository root:

```sh
make test-api
make package-api
make run-api
```

From this module directory:

```sh
mvn test
mvn package
mvn spring-boot:run
```

## OpenAPI Generation

The API contract lives in:

```text
../../../contracts/openapi/ticket-order-api/openapi.yml
```

Generate Spring API interfaces and models from the repository root:

```sh
make generate-api-contracts
```

Generated sources are written under `target/generated-sources/openapi` and are not committed.

## Docker

Build from the repository root:

```sh
docker build -f services/java/ticket-order-api/Dockerfile -t ticket-order-api .
```

Run:

```sh
docker run --rm -p 8080:8080 ticket-order-api
```

## Configuration

CORS defaults are configured in `src/main/resources/application.yml`.

```yaml
ticket-order-platform:
  cors:
    allowed-origins:
      - http://localhost:5173
    allowed-methods:
      - GET
      - POST
      - PUT
      - PATCH
      - DELETE
      - OPTIONS
    allowed-headers:
      - "*"
    allow-credentials: false
    max-age: 3600
```

PostgreSQL datasource defaults align with the root Docker Compose stack:

```yaml
spring:
  datasource:
    primary:
      url: jdbc:postgresql://localhost:5432/ticket_order
      username: ticket_order
      password: ticket_order
    analytical:
      url: jdbc:postgresql://localhost:5432/ticket_order
      username: ticket_order
      password: ticket_order
```

The primary datasource is the default operational datasource. User, event, and order persistence use the `ticket_transactional` schema. The analytical datasource is reserved for read-side schema access.

The user persistence adapter maps the domain `User` to the transactional table:

```text
ticket_transactional.t_users
```
